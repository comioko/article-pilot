package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.reflect.TypeToken;
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

import java.util.List;
import java.util.Map;

/**
 * 标题生成 Agent
 * 根据选题生成 3-5 个爆款标题方案
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TitleGeneratorAgent implements NodeAction {

    private final DashScopeChatModel chatModel;
    private final ArticleContextFactory contextFactory;

    public static final String OUTPUT_TITLE_OPTIONS = "titleOptions";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ArticleContext ctx = contextFactory.fromOverAllState(state);

        log.info("TitleGeneratorAgent 开始执行: topic={}, style={}", ctx.topic(), ctx.style());

        String prompt = ctx.render(PromptConstant.AGENT1_TITLE_PROMPT) + ctx.styleSuffix();

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        List<ArticleState.TitleOption> titleOptions = GsonUtils.fromJson(
                content,
                new TypeToken<List<ArticleState.TitleOption>>() {
                }
        );

        log.info("TitleGeneratorAgent 执行完成: 生成了 {} 个标题方案", titleOptions.size());

        return Map.of(OUTPUT_TITLE_OPTIONS, titleOptions);
    }
}
