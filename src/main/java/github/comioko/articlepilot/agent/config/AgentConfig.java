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
     * 提供内存状态保存器（单例）
     * 用于 Agent 对话记忆管理
     */
    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }
}
