package github.comioko.articlepilot.agent.routing;

import com.alibaba.cloud.ai.graph.OverAllState;
import github.comioko.articlepilot.agent.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 主编路由函数（Editor-in-Chief 的 conditional edge 路由器）
 *
 * <p>读 {@code editorDecision}（由 EditorInChiefAgent 写入），按值映射到下一个目标节点：
 *
 * <pre>
 *   finish         → END（任务完成，editorialNote 已写入 state）
 *   revise_outline → outline_generator（大纲需要重做）
 *   revise_content → content_generator（正文需要补/重写）
 *   revise_images  → image_analyzer（配图需要重选）
 * </pre>
 *
 * <p>maxEditorIterations 硬上限防死循环；超限强制 finish。
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EditorRouting {

    public static final String GOTO_FINISH = "finish";
    public static final String GOTO_REVISE_OUTLINE = "revise_outline";
    public static final String GOTO_REVISE_CONTENT = "revise_content";
    public static final String GOTO_REVISE_IMAGES = "revise_images";

    private final AgentConfig agentConfig;

    /**
     * 同步路由函数（被 AsyncEdgeAction.edge_async(...) 适配）。
     *
     * @param state 当前 StateGraph 状态
     * @return 下一目标节点名
     */
    public String route(OverAllState state) {
        if (!agentConfig.isEditorEnabled()) {
            log.debug("Editor 路由：未启用 → finish");
            return GOTO_FINISH;
        }

        String decision = state.value("editorDecision").map(Object::toString).orElse(GOTO_FINISH);
        int iteration = state.value("editorIteration").map(v -> ((Number) v).intValue()).orElse(0);
        int maxIterations = agentConfig.getMaxEditorIterations();

        if (iteration >= maxIterations) {
            log.warn("Editor 路由：已达主编上限 ({} 次)，强制 finish", maxIterations);
            return GOTO_FINISH;
        }

        String target = switch (decision) {
            case "revise_outline" -> GOTO_REVISE_OUTLINE;
            case "revise_content" -> GOTO_REVISE_CONTENT;
            case "revise_images" -> GOTO_REVISE_IMAGES;
            case "finish" -> GOTO_FINISH;
            default -> {
                log.warn("Editor 路由：未知决策 '{}' → finish", decision);
                yield GOTO_FINISH;
            }
        };

        log.info("Editor 路由：decision={}, iteration={}/{}, threshold={} → {}",
                decision, iteration, maxIterations, maxIterations, target);

        return target;
    }
}