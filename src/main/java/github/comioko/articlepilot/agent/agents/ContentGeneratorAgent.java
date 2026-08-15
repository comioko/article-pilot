package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.context.ArticleContext;
import github.comioko.articlepilot.agent.context.ArticleContextFactory;
import github.comioko.articlepilot.agent.context.StreamHandlerContext;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.enums.SseMessageTypeEnum;
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
 * 正文生成 Agent
 * 根据大纲生成文章正文内容（支持流式输出）
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContentGeneratorAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_CONTENT = "content";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        // Self-Critique Loop：revisionCount > 0 表示本节点是 critic 之后的修订
        int revisionCount = state.value("revisionCount").map(v -> ((Number) v).intValue()).orElse(0);

        // Editor-in-Chief Loop：contentFeedback 非空表示主编决定 revise_content
        String editorFeedback = state.value("contentFeedback").map(Object::toString).orElse("");
        String criticFeedback = state.value("critiqueFeedback").map(Object::toString).orElse("");

        String template;
        String mode;
        if (!editorFeedback.isBlank()) {
            // 主编反馈（结构性，宏观）
            template = buildRevisePrompt(ctx, editorFeedback, "EDITOR");
            mode = "EDITOR_REVISE";
        } else if (revisionCount > 0) {
            // Critic 反馈（细节、打分）
            template = buildRevisePrompt(ctx, criticFeedback, "CRITIC");
            mode = "CRITIC_REVISE #" + revisionCount;
        } else {
            template = ctx.render(PromptConstant.AGENT3_CONTENT_PROMPT) + ctx.styleSuffix();
            mode = "INITIAL";
        }

        log.info("ContentGeneratorAgent 开始执行 [{}]: mainTitle={}", mode, ctx.templateVars().get("{mainTitle}"));

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(template, streamHandler);

        log.info("ContentGeneratorAgent 执行完成: 正文长度={}, mode={}", content.length(), mode);

        // 注：返回 String（Markdown 正文），下游用 Object::toString 兼容
        return Map.of(OUTPUT_CONTENT, content);
    }

    private String buildRevisePrompt(ArticleContext ctx, String feedback, String source) {
        Map<String, String> vars = new LinkedHashMap<>(ctx.templateVars());
        // 两个修订 prompt 都用 {contentFeedback} / {critiqueFeedback} 之一
        if ("EDITOR".equals(source)) {
            vars.put("{contentFeedback}", feedback);
            ArticleContext reviseCtx = new ArticleContext(
                    ctx.taskId(), ctx.topic(), ctx.userDescription(), ctx.style(),
                    ctx.styleFragment(), ctx.enabledImageMethods(),
                    ctx.availableMethodsDescription(), ctx.methodUsageGuide(),
                    ctx.lengthBudget(), vars
            );
            return reviseCtx.render(PromptConstant.AGENT3_EDITOR_REVISION_PROMPT);
        } else {
            vars.put("{critiqueFeedback}", feedback);
            ArticleContext reviseCtx = new ArticleContext(
                    ctx.taskId(), ctx.topic(), ctx.userDescription(), ctx.style(),
                    ctx.styleFragment(), ctx.enabledImageMethods(),
                    ctx.availableMethodsDescription(), ctx.methodUsageGuide(),
                    ctx.lengthBudget(), vars
            );
            return reviseCtx.render(PromptConstant.AGENT3_REVISION_PROMPT);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
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
                            streamHandler.accept(SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix() + chunk);
                        }
                    }
                })
                .doOnError(error -> log.error("ContentGeneratorAgent 流式调用失败", error))
                .blockLast();

        return contentBuilder.toString();
    }
}
