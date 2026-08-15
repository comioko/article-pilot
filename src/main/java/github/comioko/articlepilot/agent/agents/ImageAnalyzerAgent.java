package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配图需求分析 Agent
 * 分析文章内容，生成配图需求列表，并在正文中插入占位符。
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ImageAnalyzerAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_CONTENT_WITH_PLACEHOLDERS = "contentWithPlaceholders";
    public static final String OUTPUT_IMAGE_REQUIREMENTS = "imageRequirements";

    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        log.info("ImageAnalyzerAgent 开始执行: mainTitle={}, enabledMethods={}",
                ctx.templateVars().get("{mainTitle}"), ctx.enabledImageMethods());

        String prompt = ctx.render(PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String responseContent = response.getResult().getOutput().getText();

        ArticleState.Agent4Result agent4Result = GsonUtils.fromJson(
                responseContent,
                ArticleState.Agent4Result.class
        );

        List<ArticleState.ImageRequirement> validatedRequirements = validateAndFilterImageRequirements(
                agent4Result.getImageRequirements(),
                ctx.enabledImageMethods()
        );

        log.info("ImageAnalyzerAgent 执行完成: 配图需求数量={}, 验证后数量={}, 已在正文中插入占位符",
                agent4Result.getImageRequirements().size(), validatedRequirements.size());

        return Map.of(
                OUTPUT_CONTENT_WITH_PLACEHOLDERS, agent4Result.getContentWithPlaceholders(),
                "content", agent4Result.getContentWithPlaceholders(),
                OUTPUT_IMAGE_REQUIREMENTS, GsonUtils.toJson(validatedRequirements)
        );
    }

    /**
     * 防御性后置校验：把 LLM 选出的 imageSource 限定到 enabled 集合内。
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

    private static String stripMarkdownFence(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        Matcher m = FENCE_PATTERN.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        return trimmed;
    }
}