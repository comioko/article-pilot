package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
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
 * 主编 Agent（Editor-in-Chief / Supervisor）
 *
 * <p>位于 Phase 3 StateGraph 的最后一步，持有完整的文章状态（outline + content + imageRequirements），
 * 调 LLM 做最终审阅并输出 JSON 决策：
 *
 * <pre>{@code
 * {
 *   "decision": "finish" | "revise_outline" | "revise_content" | "revise_images",
 *   "editorialNote": "...",        // 仅 decision=finish 时输出
 *   "feedback": "..."               // 仅 decision=revise_* 时输出
 * }
 * }</pre>
 *
 * <p>从 DAG 升级到 LLM 控制的动态图：通过 {@code addConditionalEdges("editor_in_chief", router, mapping)}
 * 让主编自己决定下一步走向——这是 supervisor pattern 在 StateGraph 中的落地。
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EditorInChiefAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_DECISION = "editorDecision";
    public static final String OUTPUT_NOTE = "editorialNote";
    public static final String OUTPUT_OUTLINE_FEEDBACK = "outlineFeedback";
    public static final String OUTPUT_CONTENT_FEEDBACK = "contentFeedback";
    public static final String OUTPUT_IMAGE_FEEDBACK = "imageFeedback";
    public static final String OUTPUT_ITERATION = "editorIteration";

    private static final Pattern FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        int prevIteration = state.value(OUTPUT_ITERATION).map(v -> ((Number) v).intValue()).orElse(0);
        int newIteration = prevIteration + 1;

        String prompt = ctx.render(PromptConstant.EDITOR_REVIEW_PROMPT);

        log.info("EditorInChiefAgent 开始审阅 [iteration={}]: mainTitle={}",
                newIteration, ctx.templateVars().get("{mainTitle}"));

        String response = callLlm(prompt);
        EditorDecision decision = parseDecision(response);

        if (decision == null) {
            // 兜底：解析失败直接 finish，避免卡死
            log.warn("EditorInChiefAgent JSON 解析失败，强制 finish。raw={}", truncate(response, 200));
            decision = new EditorDecision();
            decision.decision = "finish";
            decision.editorialNote = "主编决策解析失败，强制发布。建议人工 review。";
        }

        log.info("EditorInChiefAgent 决策: {} | iteration={}", decision.decision, newIteration);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(OUTPUT_DECISION, decision.decision);
        result.put(OUTPUT_ITERATION, newIteration);

        switch (decision.decision) {
            case "finish":
                result.put(OUTPUT_NOTE, decision.editorialNote == null ? "" : decision.editorialNote);
                break;
            case "revise_outline":
                result.put(OUTPUT_OUTLINE_FEEDBACK, decision.feedback == null ? "" : decision.feedback);
                log.info("EditorInChiefAgent 反馈给大纲: {}", truncate(decision.feedback, 100));
                break;
            case "revise_content":
                result.put(OUTPUT_CONTENT_FEEDBACK, decision.feedback == null ? "" : decision.feedback);
                log.info("EditorInChiefAgent 反馈给正文: {}", truncate(decision.feedback, 100));
                break;
            case "revise_images":
                result.put(OUTPUT_IMAGE_FEEDBACK, decision.feedback == null ? "" : decision.feedback);
                log.info("EditorInChiefAgent 反馈给配图: {}", truncate(decision.feedback, 100));
                break;
            default:
                log.warn("EditorInChiefAgent 未知决策 '{}', 强制 finish", decision.decision);
                result.put(OUTPUT_DECISION, "finish");
                result.put(OUTPUT_NOTE, "主编决策未知，强制发布。");
        }

        return result;
    }

    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    private EditorDecision parseDecision(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = stripMarkdownFence(raw);
        try {
            return GsonUtils.fromJson(cleaned, EditorDecision.class);
        } catch (Exception e) {
            log.debug("EditorDecision 解析失败: {}", e.getMessage());
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

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 主编决策（内部 record）
     */
    public static class EditorDecision {
        /** finish | revise_outline | revise_content | revise_images */
        public String decision;
        /** 仅 finish 时输出：100-200 字编辑笔记 */
        public String editorialNote;
        /** 仅 revise_* 时输出：具体可操作的修改建议 */
        public String feedback;
    }
}