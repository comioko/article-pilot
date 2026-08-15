package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正文质量评审 Agent（Self-Critique Loop 的 Critic 节点）。
 *
 * <p>读取 {@code content} key，按 7 分制对正文打分并给出具体改进建议。
 *
 * <p>输出：
 * <ul>
 *   <li>{@code critiqueScore}      — 评分（0-7）</li>
 *   <li>{@code critiqueFeedback}   — 反馈文本（供 Revision Agent 使用）</li>
 *   <li>{@code revisionCount}      — 已修订次数（+1）</li>
 *   <li>{@code revise}             — 是否需要重写（score < threshold && rev < max）</li>
 * </ul>
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContentCriticAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_SCORE = "critiqueScore";
    public static final String OUTPUT_FEEDBACK = "critiqueFeedback";
    public static final String OUTPUT_REVISION_COUNT = "revisionCount";
    public static final String OUTPUT_REVISE = "revise";

    /** 兼容 LLM 给 JSON 加 ```json 围栏 */
    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        String content = ctx.templateVars().get("{content}");
        if (content == null || content.isBlank()) {
            log.warn("ContentCriticAgent: content 为空，跳过评审");
            return Map.of(
                    OUTPUT_SCORE, 7,
                    OUTPUT_FEEDBACK, "",
                    OUTPUT_REVISION_COUNT, state.value(OUTPUT_REVISION_COUNT).orElse(0),
                    OUTPUT_REVISE, false
            );
        }

        // 把当前 content 注入到 {content} 占位符
        Map<String, String> vars = new LinkedHashMap<>(ctx.templateVars());
        vars.put("{content}", content);
        vars.put("{critiqueFeedback}", "");  // critic 不需要这个
        ArticleContext ctxWithContent = new ArticleContext(
                ctx.taskId(), ctx.topic(), ctx.userDescription(), ctx.style(),
                ctx.styleFragment(), ctx.enabledImageMethods(),
                ctx.availableMethodsDescription(), ctx.methodUsageGuide(),
                ctx.lengthBudget(), vars
        );

        String prompt = ctxWithContent.render(PromptConstant.AGENT3_CRITIC_PROMPT);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String raw = response.getResult().getOutput().getText();
        CriticResult result = parseCriticResult(raw);

        int prevRev = state.value(OUTPUT_REVISION_COUNT).map(v -> ((Number) v).intValue()).orElse(0);
        int newRev = prevRev + 1;

        log.info("ContentCriticAgent 评分: score={}/7, revisionCount={}, feedback={}",
                result.score, newRev, truncate(result.feedback, 80));

        Boolean revise = state.value(OUTPUT_REVISE)
                .map(v -> Boolean.TRUE.equals(v))
                .orElse(false);
        // 由 orchestration 层根据 threshold + max 决定是否再 revise；
        // 这里只输出原始评分 + 反馈，conditional edge 路由逻辑在 orchestration。
        return Map.of(
                OUTPUT_SCORE, result.score,
                OUTPUT_FEEDBACK, result.feedback,
                OUTPUT_REVISION_COUNT, newRev,
                OUTPUT_REVISE, revise
        );
    }

    private CriticResult parseCriticResult(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CriticResult(6, "评审返回为空，视为合格");
        }
        String cleaned = stripMarkdownFence(raw);
        try {
            return GsonUtils.fromJson(cleaned, CriticResult.class);
        } catch (Exception e) {
            log.warn("Critic JSON 解析失败, raw={}", truncate(cleaned, 200), e);
            // 兜底：尝试从文本里提取数字
            int score = 6;
            Matcher m = Pattern.compile("\"score\"\\s*:\\s*(\\d)").matcher(cleaned);
            if (m.find()) {
                try {
                    score = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignore) {
                }
            }
            return new CriticResult(score, cleaned);
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

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * Critic 评分结果（内部 record，避免新建文件）
     */
    public record CriticResult(int score, String feedback) {
    }
}
