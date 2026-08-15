package github.comioko.articlepilot.agent.config;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类
 * 提供 Agent 相关的全局配置和共享组件
 *
 * @author AI Passage Creator
 */
@Configuration
@Getter
public class AgentConfig {

    /**
     * Agent 最大迭代次数（Self-Critique 修订上限）
     */
    @Value("${article.agent.max-iterations:3}")
    private int maxIterations;

    /**
     * 是否启用 Self-Critique 反思循环
     */
    @Value("${article.agent.critique.enabled:true}")
    private boolean critiqueEnabled;

    /**
     * Self-Critique 评分阈值：评分 &lt; 阈值触发修订
     */
    @Value("${article.agent.critique.threshold:7}")
    private int critiqueThreshold;

    /**
     * 是否启用 Dynamic Tool Routing（PR 3）。
     * <p>
     * true: ImageAnalyzerAgent 让 LLM 通过 ChatClient + defaultToolCallbacks 调用 generateImage 工具
     *      （演示级：LLM 真的会调工具，但 final-response 的 JSON 格式需要更精细的 prompt）
     * <p>
     * false: 沿用 JSON 文本 prompt（PR 1 行为，确保 pipeline 稳定输出 imageRequirements）
     */
    @Value("${article.agent.tool-routing.enabled:true}")
    private boolean toolRoutingEnabled;

    /**
     * 是否启用 Editor-in-Chief 主编动态路由（supervisor pattern）
     * <p>
     * true: 主编节点审阅完整文章，决策 finish / revise_outline / revise_content / revise_images
     *      通过 addConditionalEdges 让 StateGraph 走 LLM 控制的动态路径
     * <p>
     * false: 跳过主编节点，content_merger 直接到 END（PR 1/2 行为）
     */
    @Value("${article.agent.editor.enabled:true}")
    private boolean editorEnabled;

    /**
     * 主编最大迭代次数（防 LLM 互搏死循环）
     */
    @Value("${article.agent.editor.max-iterations:2}")
    private int maxEditorIterations;

    /**
     * 提供内存状态保存器（单例）
     * 用于 Agent 对话记忆管理
     */
    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }
}
