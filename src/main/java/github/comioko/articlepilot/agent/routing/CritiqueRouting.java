package github.comioko.articlepilot.agent.routing;

import com.alibaba.cloud.ai.graph.OverAllState;
import github.comioko.articlepilot.agent.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Self-Critique 路由函数：content_critic 跑完后，决定下一步走向。
 *
 * <p>规则：
 * <ul>
 *   <li>若关闭批判功能（{@code critiqueEnabled=false}）→ 直接 accept</li>
 *   <li>若评分 &lt; 阈值 且 已修订次数 &lt; 上限 → revise（回到 content_generator）</li>
 *   <li>否则 → accept（继续到 image_analyzer）</li>
 * </ul>
 *
 * <p>由 {@code addConditionalEdges("content_critic", AsyncCommandAction.of(this::route), mapping)}
 * 调用，签名 {@code BiFunction<OverAllState, RunnableConfig, CompletableFuture<Command>>} 简化版。
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CritiqueRouting {

    /** 路由目标常量——与 addConditionalEdges 的 mapping key 对应 */
    public static final String GOTO_REVISE = "revise";
    public static final String GOTO_ACCEPT = "accept";

    private final AgentConfig agentConfig;

    /**
     * 简化的路由函数（被 AsyncCommandAction.of(...) 适配为 BiFunction）。
     *
     * @param state 当前 StateGraph 状态
     * @return "revise" 或 "accept"
     */
    public String route(OverAllState state) {
        boolean enabled = agentConfig.isCritiqueEnabled();
        if (!enabled) {
            log.debug("Critique 路由: 关闭 → accept");
            return GOTO_ACCEPT;
        }

        int score = state.value("critiqueScore").map(v -> ((Number) v).intValue()).orElse(7);
        int rev = state.value("revisionCount").map(v -> ((Number) v).intValue()).orElse(0);
        int threshold = agentConfig.getCritiqueThreshold();
        int maxIterations = agentConfig.getMaxIterations();

        boolean needsRevision = score < threshold && rev < maxIterations;
        String decision = needsRevision ? GOTO_REVISE : GOTO_ACCEPT;

        if (rev >= maxIterations) {
            log.warn("Critique 路由: 已达修订上限 ({} 次)，强制接受当前正文", maxIterations);
        }

        log.info("Critique 路由: score={}/7, revision={}/{}, threshold={} → {}",
                score, rev, maxIterations, threshold, decision);

        return decision;
    }
}
