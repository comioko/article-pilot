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

import java.util.Map;
import java.util.function.Consumer;

/**
 * 大纲生成 Agent
 * 根据标题生成文章大纲（支持流式输出）
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

        log.info("OutlineGeneratorAgent 开始执行: mainTitle={}, subTitle={}",
                ctx.templateVars().get("{mainTitle}"), ctx.templateVars().get("{subTitle}"));

        String prompt = ctx.render(PromptConstant.AGENT2_OUTLINE_PROMPT) + ctx.styleSuffix();

        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);

        // 解析为 OutlineResult 验证合法性，但 StateGraph OverAllState 跨节点读对象时类型可能丢失
        // （Jackson 序列化/反序列化导致），统一返回 JSON 字符串，由 orchestrator 解析。
        ArticleState.OutlineResult outlineResult = GsonUtils.fromJson(
                content,
                ArticleState.OutlineResult.class
        );
        log.info("OutlineGeneratorAgent 执行完成: 生成了 {} 个章节",
                outlineResult.getSections().size());

        // 返回 JSON 字符串：StateGraph 跨节点读写时类型稳定性更好。
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
}
