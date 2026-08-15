package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.comioko.articlepilot.agent.config.AgentConfig;
import github.comioko.articlepilot.agent.config.AgentToolsConfig;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.agent.tools.ImageGenerationTool;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配图需求分析 Agent（PR 3 完善 + 升级：Dynamic Tool Routing）
 *
 * <p>通过 Spring AI 的 ChatClient 让 LLM 通过 function-calling 协议选 imageSource，
 * 而不是写 JSON 文本。所有配图 source 由 LLM 调工具决定。
 *
 * <h2>三层兜底</h2>
 * <ol>
 *   <li><b>主路径</b>：ChatClient + defaultToolCallbacks，LLM 通过 tool-calling 表达决策，
 *       agent 程序化从 {@code ChatResponse.getResult().getOutput().getToolCalls()}
 *       提取决策 → 构造 {@code imageRequirements}。这是最可靠的方式。</li>
 *   <li><b>次路径</b>：若 LLM 在 final response 中输出了 JSON，
 *       解析 {@code {contentWithPlaceholders, imageRequirements}}。Agent 会优先用这个，
 *       因为内容里的占位符可能更精确。</li>
 *   <li><b>降级</b>：使用旧 JSON 文本 prompt（{@code AGENT4_IMAGE_REQUIREMENTS_PROMPT}），
 *       不注册工具。pipeline 不中断。</li>
 * </ol>
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ImageAnalyzerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;
    private final AgentToolsConfig toolsConfig;
    private final ImageGenerationTool imageGenerationTool;
    private final AgentConfig agentConfig;

    public static final String OUTPUT_CONTENT_WITH_PLACEHOLDERS = "contentWithPlaceholders";
    public static final String OUTPUT_IMAGE_REQUIREMENTS = "imageRequirements";

    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        log.info("ImageAnalyzerAgent 开始执行 [mainTitle={}, enabledMethods={}]",
                ctx.templateVars().get("{mainTitle}"), ctx.enabledImageMethods());

        // Editor-in-Chief 反馈：使用 IMAGE_REVISION_PROMPT 重选配图
        String imageFeedback = state.value("imageFeedback").map(Object::toString).orElse("");
        String prompt;
        if (!imageFeedback.isBlank()) {
            log.info("ImageAnalyzerAgent 收到主编反馈: {}", truncate(imageFeedback, 80));
            prompt = buildRevisionPrompt(ctx, imageFeedback);
        } else {
            prompt = ctx.render(PromptConstant.AGENT4_TOOL_USE_PROMPT);
        }

        ArticleState.Agent4Result result = null;
        if (agentConfig.isToolRoutingEnabled()) {
            log.info("ImageAnalyzerAgent: 启用 Dynamic Tool Routing 主路径");
            try {
                result = invokeWithToolRouting(prompt);
            } catch (Exception e) {
                log.warn("Tool-routing 失败，降级到 JSON 文本路径: {}", e.getMessage());
            }
        }
        if (result == null) {
            // 主编反馈走 JSON 文本路径（用 IMAGE_REVISION_PROMPT），
            // 初始走老 JSON prompt 路径
            result = !imageFeedback.isBlank()
                    ? invokeRevisionPrompt(ctx, imageFeedback)
                    : invokeWithFallbackPrompt(ctx);
        }

        if (result == null || result.getContentWithPlaceholders() == null) {
            result = new ArticleState.Agent4Result();
            result.setContentWithPlaceholders("");
            result.setImageRequirements(new ArrayList<>());
        }

        List<ArticleState.ImageRequirement> validated = validateAndFilterImageRequirements(
                result.getImageRequirements(), ctx.enabledImageMethods());

        log.info("ImageAnalyzerAgent 执行完成: imageRequirements={}, 验证后={}",
                result.getImageRequirements() == null ? 0 : result.getImageRequirements().size(),
                validated.size());

        return Map.of(
                OUTPUT_CONTENT_WITH_PLACEHOLDERS, result.getContentWithPlaceholders(),
                "content", result.getContentWithPlaceholders(),
                OUTPUT_IMAGE_REQUIREMENTS, GsonUtils.toJson(validated)
        );
    }

    /**
     * 主路径：ChatClient + defaultToolCallbacks。
     *
     * <p>LLM 通过 tool-calling 表达决策。我们从 {@code ChatResponse} 同时取
     * <ul>
     *   <li>final response text（用作 contentWithPlaceholders，LLM 应该已经插入了占位符）</li>
     *   <li>tool calls 列表（用来程序化构建 imageRequirements，最可靠）</li>
     * </ul>
     */
    private ArticleState.Agent4Result invokeWithToolRouting(String prompt) {
        ToolCallback[] callbacks = toolsConfig.buildAllImageSourceToolCallbacks(imageGenerationTool);
        log.debug("ImageAnalyzerAgent: 注册 {} 个 tool callback", callbacks.length);
        for (ToolCallback cb : callbacks) {
            ToolDefinition def = cb.getToolDefinition();
            log.debug("  - tool: {} | {}", def.name(), def.description().substring(0, Math.min(60, def.description().length())));
        }

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(callbacks)
                .build();

        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String finalContent = response.getResult().getOutput().getText();
        List<AssistantMessage.ToolCall> toolCalls =
                response.getResult().getOutput().getToolCalls();

        log.info("ImageAnalyzerAgent tool-routing: {} 次 tool 调用, final content 长度={}",
                toolCalls == null ? 0 : toolCalls.size(),
                finalContent == null ? 0 : finalContent.length());

        if (toolCalls != null) {
            for (AssistantMessage.ToolCall tc : toolCalls) {
                log.debug("  tool call: name={}, args={}", tc.name(), tc.arguments());
            }
        }

        // 路径 1：程序化从 tool calls 构建 imageRequirements（最可靠）
        List<ArticleState.ImageRequirement> reqsFromTools = extractRequirementsFromToolCalls(toolCalls);

        // 路径 2：尝试解析 LLM final response 中的 JSON
        ArticleState.Agent4Result parsedFromText = parseAgent4Result(finalContent);

        ArticleState.Agent4Result result = new ArticleState.Agent4Result();
        result.setContentWithPlaceholders(finalContent == null ? "" : finalContent);

        // 优先级：LLM 输出的 JSON（带 placeholder 标记的 content） > 工具调用程序化提取
        if (parsedFromText != null && parsedFromText.getImageRequirements() != null
                && !parsedFromText.getImageRequirements().isEmpty()) {
            log.info("使用 LLM JSON 输出构建 imageRequirements ({} 条)",
                    parsedFromText.getImageRequirements().size());
            result.setImageRequirements(parsedFromText.getImageRequirements());
            // 如果 LLM 输出里有更精确的 contentWithPlaceholders，也用它
            if (parsedFromText.getContentWithPlaceholders() != null
                    && !parsedFromText.getContentWithPlaceholders().isBlank()) {
                result.setContentWithPlaceholders(parsedFromText.getContentWithPlaceholders());
            }
        } else if (!reqsFromTools.isEmpty()) {
            log.info("程序化从 tool calls 提取 imageRequirements ({} 条)", reqsFromTools.size());
            result.setImageRequirements(reqsFromTools);
        } else {
            log.warn("Tool-routing 路径未产生任何 imageRequirements");
            result.setImageRequirements(new ArrayList<>());
        }
        return result;
    }

    /**
     * 降级路径：使用旧的 JSON 文本 prompt（不注册工具）。
     */
    private ArticleState.Agent4Result invokeWithFallbackPrompt(ArticleContext ctx) {
        log.debug("ImageAnalyzerAgent: 使用 JSON 文本路径（fallback）");
        String prompt = ctx.render(PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT);
        String response = chatModel.call(
                new org.springframework.ai.chat.prompt.Prompt(
                        new org.springframework.ai.chat.messages.UserMessage(prompt)
                )
        ).getResult().getOutput().getText();
        return parseAgent4Result(response);
    }

    /**
     * 程序化从 tool calls 构造 imageRequirements。
     *
     * <p>每个 tool call 包含：tool name（→ imageSource）+ arguments JSON（包含 keywords/prompt/position/sectionTitle）。
     * 我们根据 tool name 映射 imageSource，从 args 抽取其他字段，自动分配 placeholderId。
     */
    private List<ArticleState.ImageRequirement> extractRequirementsFromToolCalls(
            List<AssistantMessage.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) return new ArrayList<>();

        List<ArticleState.ImageRequirement> reqs = new ArrayList<>();
        int placeholderIdx = 1;
        for (AssistantMessage.ToolCall call : toolCalls) {
            String toolName = call.name();
            String imageSource = AgentToolsConfig.TOOL_NAME_TO_IMAGE_SOURCE.get(toolName);
            if (imageSource == null) {
                log.warn("未知 tool name: {}, 跳过", toolName);
                continue;
            }

            try {
                JsonNode args = OBJECT_MAPPER.readTree(call.arguments());
                ArticleState.ImageRequirement req = new ArticleState.ImageRequirement();
                Integer position = args.has("position") && args.get("position").isInt()
                        ? args.get("position").asInt() : null;
                String sectionTitle = args.path("sectionTitle").asText("");

                req.setPosition(position != null ? position : reqs.size() + 1);
                req.setImageSource(imageSource);
                req.setSectionTitle(sectionTitle);
                boolean isCover = position != null && position == 1;
                req.setType(isCover ? "cover" : "section");

                // 按 tool name 抽取 keyword / prompt
                switch (toolName) {
                    case "use_pexels_photo":
                    case "search_emoji_pack":
                        req.setKeywords(args.path("keywords").asText(""));
                        break;
                    case "fetch_icon":
                        req.setKeywords(args.path("iconName").asText(""));
                        break;
                    case "generate_ai_image":
                        req.setPrompt(args.path("prompt").asText(""));
                        break;
                    case "render_mermaid_diagram":
                        req.setPrompt(args.path("code").asText(""));
                        break;
                    case "generate_svg_diagram":
                        req.setPrompt(args.path("requirement").asText(""));
                        break;
                    default:
                        // 旧的 generate_image tool 走通用字段
                        req.setKeywords(args.path("keywords").asText(""));
                        req.setPrompt(args.path("prompt").asText(""));
                        if (isCover) {
                            req.setType(args.path("type").asText("cover"));
                        }
                        break;
                }

                // 自动分配 placeholderId
                if (!isCover) {
                    req.setPlaceholderId("{{IMAGE_PLACEHOLDER_" + placeholderIdx++ + "}}");
                }

                reqs.add(req);
            } catch (Exception e) {
                log.warn("解析 tool call args 失败: tool={}, args={}, err={}",
                        toolName, call.arguments(), e.getMessage());
            }
        }
        return reqs;
    }

    /**
     * 主编反馈路径：构造修订 prompt（注入 imageFeedback 到模板）
     */
    private String buildRevisionPrompt(ArticleContext ctx, String feedback) {
        Map<String, String> vars = new java.util.LinkedHashMap<>(ctx.templateVars());
        vars.put("{imageFeedback}", feedback);
        ArticleContext reviseCtx = new ArticleContext(
                ctx.taskId(), ctx.topic(), ctx.userDescription(), ctx.style(),
                ctx.styleFragment(), ctx.enabledImageMethods(),
                ctx.availableMethodsDescription(), ctx.methodUsageGuide(),
                ctx.lengthBudget(), vars
        );
        return reviseCtx.render(PromptConstant.IMAGE_REVISION_PROMPT);
    }

    /**
     * 主编反馈降级路径：直接调 chatModel.call() 用 IMAGE_REVISION_PROMPT
     */
    private ArticleState.Agent4Result invokeRevisionPrompt(ArticleContext ctx, String feedback) {
        String prompt = buildRevisionPrompt(ctx, feedback);
        String response = chatModel.call(
                new Prompt(new org.springframework.ai.chat.messages.UserMessage(prompt))
        ).getResult().getOutput().getText();
        return parseAgent4Result(response);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private ArticleState.Agent4Result parseAgent4Result(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = stripMarkdownFence(raw);
        try {
            return GsonUtils.fromJson(cleaned, ArticleState.Agent4Result.class);
        } catch (Exception e) {
            log.debug("解析 Agent4Result 失败: {}", e.getMessage());
            return null;
        }
    }

    private static String stripMarkdownFence(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        Matcher m = FENCE_PATTERN.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        return trimmed;
    }

    /**
     * 防御性后置校验：把 imageSource 限定到 enabled 集合内。
     */
    private List<ArticleState.ImageRequirement> validateAndFilterImageRequirements(
            List<ArticleState.ImageRequirement> requirements,
            List<ImageMethodEnum> enabledMethods) {

        if (requirements == null || requirements.isEmpty()) return new ArrayList<>();
        if (enabledMethods == null || enabledMethods.isEmpty()) return requirements;

        List<ImageMethodEnum> effective = enabledMethods;
        List<ArticleState.ImageRequirement> validated = new ArrayList<>();
        for (ArticleState.ImageRequirement req : requirements) {
            String src = req.getImageSource();
            boolean ok = false;
            if (src != null) {
                for (ImageMethodEnum m : effective) {
                    if (m.getValue().equals(src)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (ok) {
                validated.add(req);
            } else {
                log.warn("配图需求不符合限制被过滤, position={}, imageSource={}, enabledMethods={}",
                        req.getPosition(), src, effective);
                if (!effective.isEmpty()) {
                    req.setImageSource(effective.get(0).getValue());
                    validated.add(req);
                    log.info("配图需求已替换为允许的方式, position={}, fallback={}",
                            req.getPosition(), effective.get(0).getValue());
                }
            }
        }
        return validated;
    }
}
