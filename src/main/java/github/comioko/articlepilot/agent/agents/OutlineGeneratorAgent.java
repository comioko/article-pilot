package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.agent.context.StreamHandlerContext;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.model.enums.SseMessageTypeEnum;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 大纲生成 Agent
 * 根据标题生成文章大纲（支持流式输出），或根据主编反馈修订大纲。
 *
 * <p>三种调用模式：
 * <ol>
 *   <li>Phase 2 上下文（state.value("outline") 为空，state.value("outlineFeedback") 为空）：
 *       初始生成大纲</li>
 *   <li>Phase 3 上下文（state.value("outline") 已预加载，state.value("outlineFeedback") 为空）：
 *       passthrough，沿用现有大纲（用户已编辑过的）</li>
 *   <li>主编回路（state.value("outlineFeedback") 非空）：
 *       修订大纲，使用 OUTLINE_REVISION_PROMPT</li>
 * </ol>
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutlineGeneratorAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_OUTLINE = "outline";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        // 1) 主编反馈 → 修订模式
        String outlineFeedback = state.value("outlineFeedback").map(Object::toString).orElse("");
        if (!outlineFeedback.isBlank()) {
            return reviseOutline(ctx, outlineFeedback);
        }

        // 2) 已有大纲（passthrough）— Phase 3 graph 入口，state 已预加载用户编辑过的 outline
        Object existingOutline = state.value(OUTPUT_OUTLINE).orElse(null);
        if (existingOutline != null && !existingOutline.toString().isBlank()) {
            String existingJson = existingOutline.toString();
            log.info("OutlineGeneratorAgent passthrough：state 中已有大纲 ({})，跳过重新生成",
                    truncate(existingJson, 80));
            return Map.of(OUTPUT_OUTLINE, existingJson);
        }

        // 3) 初始生成
        return generateOutline(ctx);
    }

    private Map<String, Object> generateOutline(ArticleContext ctx) {
        log.info("OutlineGeneratorAgent 开始执行 [INITIAL]: mainTitle={}, subTitle={}",
                ctx.templateVars().get("{mainTitle}"), ctx.templateVars().get("{subTitle}"));

        String prompt = ctx.render(PromptConstant.AGENT2_OUTLINE_PROMPT) + ctx.styleSuffix();

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        ArticleState.OutlineResult outlineResult = GsonUtils.fromJson(
                content,
                ArticleState.OutlineResult.class
        );
        log.info("OutlineGeneratorAgent 执行完成 [INITIAL]: 生成了 {} 个章节",
                outlineResult.getSections().size());

        return Map.of(OUTPUT_OUTLINE, GsonUtils.toJson(outlineResult));
    }

    private Map<String, Object> reviseOutline(ArticleContext ctx, String feedback) {
        log.info("OutlineGeneratorAgent 开始执行 [REVISION by Editor]: feedback={}",
                truncate(feedback, 80));

        // 构造 revision 模板需要的 {outline} 占位符
        Map<String, String> vars = new LinkedHashMap<>(ctx.templateVars());
        vars.put("{outline}", ctx.templateVars().getOrDefault("{outline}", ""));
        vars.put("{outlineFeedback}", feedback);
        ArticleContext reviseCtx = new ArticleContext(
                ctx.taskId(), ctx.topic(), ctx.userDescription(), ctx.style(),
                ctx.styleFragment(), ctx.enabledImageMethods(),
                ctx.availableMethodsDescription(), ctx.methodUsageGuide(),
                ctx.lengthBudget(), vars
        );

        String prompt = reviseCtx.render(PromptConstant.OUTLINE_REVISION_PROMPT);

        // 修订模式不需要流式（小改动）
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        ArticleState.OutlineResult outlineResult = GsonUtils.fromJson(
                stripMarkdownFence(content),
                ArticleState.OutlineResult.class
        );
        log.info("OutlineGeneratorAgent 执行完成 [REVISION]: 修订后 {} 个章节",
                outlineResult.getSections().size());

        return Map.of(OUTPUT_OUTLINE, GsonUtils.toJson(outlineResult));
    }

    /**
     * 调用 LLM（流式输出）
     */
    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler) {
        StringBuilder contentBuilder = new StringBuilder();

        Flux<ChatResponse> streamResponse = chatModel.stream(new Prompt(new UserMessage(prompt)));

        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        contentBuilder.append(chunk);
                        if (streamHandler != null) {
                            streamHandler.accept(SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("OutlineGeneratorAgent 流式调用失败", error))
                .blockLast();

        return contentBuilder.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String stripMarkdownFence(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        int firstFence = trimmed.indexOf("```");
        if (firstFence < 0) return trimmed;
        int openLineEnd = trimmed.indexOf('\n', firstFence);
        if (openLineEnd > 0) trimmed = trimmed.substring(openLineEnd + 1);
        int lastFence = trimmed.lastIndexOf("```");
        if (lastFence > 0) trimmed = trimmed.substring(0, lastFence);
        return trimmed.trim();
    }
}