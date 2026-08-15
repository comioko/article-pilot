package github.comioko.articlepilot.agent.config;

import github.comioko.articlepilot.agent.tools.ImageGenerationTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具工厂：把 {@code ImageGenerationTool} 上所有 {@code @Tool} 注解的方法包装成
 * Spring AI 的 {@link ToolCallback}。
 *
 * <p><b>为什么不做成 {@code @Bean}：</b> 把 ToolCallback 暴露为全局 Bean 会触发
 * Spring AI 的 {@code toolCallbackResolver} 自动发现它，进而触发循环依赖：
 * <pre>
 * agentToolsConfig → imageGenerationTool → imageServiceStrategy → svgDiagramService
 *               ↓                                                                       ↑
 *               └─────── dashScopeChatModel ←── toolCallingManager ←── toolCallbackResolver
 * </pre>
 * 解决方式：把工具构造逻辑做成无状态的工厂方法，由 Agent 在调用时主动构造 callback，
 * 不暴露为全局 Bean，避免触发 Spring AI 的 toolCallbackResolver 自动装配链。
 *
 * <p><b>Tool 名 → imageSource 映射：</b>PR 3 升级把单个 {@code generate_image} tool 拆成 6 个专用 tool，
 * 每个 tool 名称本身就编码了 imageSource。本类维护此映射，Agent 在解析 tool calls 时据此
 * 把 tool name 转回 ImageMethodEnum。
 *
 * @author AI Passage Creator
 */
@Component
public class AgentToolsConfig {

    /**
     * Tool 名 → imageSource 映射。
     *
     * <p>当 LLM 调用工具时，agent 通过此映射把 tool name 转回 imageSource 写入 imageRequirements。
     * 老的 {@code generate_image} tool 走 imageSource 参数路径，不在本表。
     */
    public static final Map<String, String> TOOL_NAME_TO_IMAGE_SOURCE = Map.of(
            "use_pexels_photo",       "PEXELS",
            "generate_ai_image",      "NANO_BANANA",
            "render_mermaid_diagram", "MERMAID",
            "fetch_icon",             "ICONIFY",
            "search_emoji_pack",      "EMOJI_PACK",
            "generate_svg_diagram",   "SVG_DIAGRAM"
    );

    /**
     * 构造 {@code ImageGenerationTool} 上所有 {@code @Tool} 注解方法的 callback。
     *
     * <p>PR 3 完善版：返回 {@code ToolCallback[]} 而非单个，让 ChatClient 注册所有可用工具。
     * 当前工具数 = 7（1 个老的 generate_image + 6 个专用 tool）。
     *
     * @param imageGenerationTool 提供 @Tool 注解方法的对象
     * @return ToolCallback 数组（所有 @Tool 方法各对应一个 callback）
     */
    public ToolCallback[] buildAllImageSourceToolCallbacks(ImageGenerationTool imageGenerationTool) {
        ToolCallback[] callbacks = ToolCallbacks.from(imageGenerationTool);
        if (callbacks.length == 0) {
            throw new IllegalStateException(
                    "ImageGenerationTool 没有 @Tool 注解的方法，请检查 @Tool/@ToolParam 注解");
        }
        return callbacks;
    }
}
