package github.comioko.articlepilot.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.google.gson.JsonSyntaxException;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章智能体服务（仅保留 AI 修改大纲）
 *
 * @deprecated 该类原本包含完整的过程式 Agent 流水线，
 *             自 Tier 1 改造起被 {@link github.comioko.articlepilot.agent.ArticleAgentOrchestrator}
 *             （StateGraph 编排器）取代。仅 {@link #aiModifyOutline} 仍被
 *             {@code ArticleServiceImpl} 调用，故保留。
 *             后续若把 aiModifyOutline 也接入 StateGraph，本类可彻底移除。
 * @author comioko
 */
@Service
@Slf4j
@Deprecated
public class ArticleAgentService {

    @Resource
    private DashScopeChatModel chatModel;

    /**
     * AI 修改大纲：根据用户反馈调整章节结构。
     *
     * @param mainTitle        主标题
     * @param subTitle         副标题
     * @param currentOutline   当前大纲
     * @param modifySuggestion 用户修改建议
     * @return 修改后的大纲章节列表
     */
    public List<ArticleState.OutlineSection> aiModifyOutline(String mainTitle, String subTitle,
                                                             List<ArticleState.OutlineSection> currentOutline,
                                                             String modifySuggestion) {
        String currentOutlineJson = GsonUtils.toJson(currentOutline);

        String prompt = PromptConstant.AI_MODIFY_OUTLINE_PROMPT
                .replace("{mainTitle}", mainTitle)
                .replace("{subTitle}", subTitle)
                .replace("{currentOutline}", currentOutlineJson)
                .replace("{modifySuggestion}", modifySuggestion);

        String content = callLlm(prompt);
        ArticleState.OutlineResult outlineResult = parseJsonResponse(content, ArticleState.OutlineResult.class, "修改后的大纲");

        log.info("AI修改大纲成功, sectionsCount={}", outlineResult.getSections().size());
        return outlineResult.getSections();
    }

    /**
     * 对用户选中的文章片段进行定向精修。该方法只生成候选文本，不写入数据库。
     */
    public String aiRefineContent(String mainTitle, String style, String selectedText, String instruction) {
        String prompt = PromptConstant.AI_REFINE_CONTENT_PROMPT
                .replace("{mainTitle}", mainTitle == null ? "未命名文章" : mainTitle)
                .replace("{style}", style == null || style.isBlank() ? "保持原文风格" : style)
                .replace("{instruction}", instruction)
                .replace("{selectedText}", selectedText);
        return stripMarkdownFence(callLlm(prompt));
    }

    /**
     * 将成稿改写为指定内容渠道的可发布版本，不写入原文章。
     */
    public String generatePublishPackage(String mainTitle, String content, String channel, String channelGuide) {
        String prompt = PromptConstant.PUBLISH_PACKAGE_PROMPT
                .replace("{mainTitle}", mainTitle == null ? "未命名文章" : mainTitle)
                .replace("{content}", content)
                .replace("{channel}", channel)
                .replace("{channelGuide}", channelGuide);
        return stripMarkdownFence(callLlm(prompt));
    }

    // region helpers（aiModifyOutline 仍依赖）

    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    private <T> T parseJsonResponse(String content, Class<T> clazz, String name) {
        String cleaned = stripMarkdownFence(content);
        try {
            return GsonUtils.fromJson(cleaned, clazz);
        } catch (JsonSyntaxException e) {
            log.error("{}解析失败, content={}", name, cleaned, e);
            throw new RuntimeException(name + "解析失败");
        }
    }

    private String stripMarkdownFence(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        int firstFence = trimmed.indexOf("```");
        if (firstFence < 0) return trimmed;
        int openLineEnd = trimmed.indexOf('\n', firstFence);
        if (openLineEnd > 0) trimmed = trimmed.substring(openLineEnd + 1);
        int lastFence = trimmed.lastIndexOf("```");
        if (lastFence > 0) trimmed = trimmed.substring(0, lastFence);
        return trimmed.trim();
    }

    // endregion
}
