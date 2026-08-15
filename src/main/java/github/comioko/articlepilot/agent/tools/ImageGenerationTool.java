package github.comioko.articlepilot.agent.tools;


import github.comioko.articlepilot.model.dto.image.ImageRequest;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import github.comioko.articlepilot.service.CosService;
import github.comioko.articlepilot.service.ImageServiceStrategy;
import github.comioko.articlepilot.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图片生成工具集合（PR 3 升级：6 个专用 tool）
 *
 * <p>Spring AI Tool-calling 模式下，每个 {@code @Tool} 注解方法对应一个 LLM 可调用的"工具"。
 * 比起一个通用的 {@code generateImage(imageSource, keywords, prompt, ...)}，
 * 拆成 6 个专用 tool 让 LLM 在选 imageSource 时更精确：
 * <ul>
 *   <li>tool 名称本身就编码了 imageSource（函数名 = imageSource 决定）</li>
 *   <li>每个 tool 的参数表只暴露它真正需要的字段（Pexels 只要 keywords，不需要 prompt）</li>
 *   <li>tool description 描述了"何时使用"，LLM 据此选 tool</li>
 * </ul>
 *
 * <p>这 6 个 tool 都是 <b>decision-only</b>：返回决策 JSON（imageSource + keywords/prompt + position），
 * 不实际生成图片。实际生成由下游 {@link github.comioko.articlepilot.agent.parallel.ParallelImageGenerator}
 * 执行（使用本类的 {@link #generateImageDirect} 方法）。
 *
 * <p>{@link #generateImageDirect}（Java 直调，非 @Tool）是给内部用的真正生成入口。
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
public class ImageGenerationTool {

    @Resource
    private ImageServiceStrategy imageServiceStrategy;

    @Resource
    private CosService cosService;

    // region PR 3 升级：6 个专用 @Tool（LLM 可见）

    @Tool(description = "Use Pexels photo library to source a real photograph. "
            + "Best for: real-world scenes, products, people, nature, technology in use. "
            + "Returns a decision record; the parallel image generator fetches the photo later.")
    public String usePexelsPhoto(
            @ToolParam(description = "English search keywords describing the photo (specific, accurate)") String keywords,
            @ToolParam(description = "Position: 1=cover, 2+=section images") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: use_pexels_photo] position={}, keywords={}", position, keywords);
        return decisionJson("PEXELS", keywords, "", position, sectionTitle);
    }

    @Tool(description = "Use Nano Banana (Gemini) to AI-generate an image from a detailed English prompt. "
            + "Best for: creative illustrations, abstract concepts, infographics, "
            + "imagery that requires specific text rendering or unique style. "
            + "Returns a decision record; the parallel image generator calls Gemini later.")
    public String generateAiImage(
            @ToolParam(description = "Detailed English AI generation prompt (scene, style, colors, details)") String prompt,
            @ToolParam(description = "Position: 1=cover, 2+=section images") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: generate_ai_image] position={}, prompt_len={}", position, prompt == null ? 0 : prompt.length());
        return decisionJson("NANO_BANANA", "", prompt, position, sectionTitle);
    }

    @Tool(description = "Use Mermaid to render a structural diagram (flowchart, sequence, "
            + "architecture, class, gantt, etc). "
            + "Best for: 流程图 / 架构图 / 时序图 / 关系图. "
            + "You provide valid Mermaid source code; the parallel image generator renders it via mermaid-cli.")
    public String renderMermaidDiagram(
            @ToolParam(description = "Valid Mermaid diagram source code (starts with 'flowchart', 'sequenceDiagram', etc.)") String code,
            @ToolParam(description = "Position: 1=cover, 2+=section images") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: render_mermaid_diagram] position={}, code_len={}", position, code == null ? 0 : code.length());
        return decisionJson("MERMAID", "", code, position, sectionTitle);
    }

    @Tool(description = "Use Iconify to fetch an icon. "
            + "Best for: small inline icons like check marks, arrows, stars, hearts. "
            + "Returns a decision record; the parallel image generator fetches the icon later.")
    public String fetchIcon(
            @ToolParam(description = "Icon name in English (e.g. check, arrow, star, heart, info, warning)") String iconName,
            @ToolParam(description = "Position: usually 2+ for inline icons (rarely used for cover)") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: fetch_icon] position={}, iconName={}", position, iconName);
        return decisionJson("ICONIFY", iconName, "", position, sectionTitle);
    }

    @Tool(description = "Use Bing image search to find an emoji pack / meme. "
            + "Best for: 表情包 / 搞笑图片 / 轻松幽默配图. "
            + "You provide Chinese or English keywords; the parallel image generator searches Bing later.")
    public String searchEmojiPack(
            @ToolParam(description = "Search keywords describing the emoji/meme (Chinese or English)") String keywords,
            @ToolParam(description = "Position: usually 2+ for inline emojis") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: search_emoji_pack] position={}, keywords={}", position, keywords);
        return decisionJson("EMOJI_PACK", keywords, "", position, sectionTitle);
    }

    @Tool(description = "Use AI to generate an SVG concept diagram. "
            + "Best for: 概念示意图 / 思维导图样式 / 逻辑关系展示. "
            + "You provide a Chinese description of the diagram; the parallel image generator "
            + "calls LLM to draft SVG markup and renders it.")
    public String generateSvgDiagram(
            @ToolParam(description = "Chinese description of the diagram concept (what it should express)") String requirement,
            @ToolParam(description = "Position: 1=cover, 2+=section images") Integer position,
            @ToolParam(description = "Section title this image belongs to (empty for cover)") String sectionTitle) {
        log.info("[Tool: generate_svg_diagram] position={}, requirement_len={}", position, requirement == null ? 0 : requirement.length());
        return decisionJson("SVG_DIAGRAM", "", requirement, position, sectionTitle);
    }

    /**
     * 构造决策 JSON（不实际生成图片）。
     * 下游 ParallelImageGenerator 拿到这个 JSON 后会用 generateImageDirect 真正生成。
     */
    private String decisionJson(String imageSource, String keywords, String prompt,
                                 Integer position, String sectionTitle) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("imageSource", imageSource);
        decision.put("keywords", keywords == null ? "" : keywords);
        decision.put("prompt", prompt == null ? "" : prompt);
        int pos = position == null ? 1 : position;
        decision.put("position", pos);
        decision.put("sectionTitle", sectionTitle == null ? "" : sectionTitle);
        decision.put("type", pos == 1 ? "cover" : "section");
        decision.put("placeholderId", "");  // agent 自动分配
        return GsonUtils.toJson(decision);
    }

    // endregion

    /**
     * 直接生成图片（Java 直调，不通过 LLM；供 {@code ParallelImageGenerator} 使用）。
     *
     * @param imageSource   图片来源
     * @param keywords      关键词
     * @param prompt        提示词
     * @param position      位置
     * @param type          类型
     * @param sectionTitle  章节标题
     * @param placeholderId 占位符ID
     * @return 图片生成结果
     */
    public ImageGenerationResult generateImageDirect(String imageSource, String keywords, String prompt,
                                                      Integer position, String type, String sectionTitle,
                                                      String placeholderId) {
        try {
            ImageRequest imageRequest = ImageRequest.builder()
                    .keywords(keywords)
                    .prompt(prompt)
                    .position(position)
                    .type(type)
                    .build();

            ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImageAndUpload(imageSource, imageRequest);
            String cosUrl = result.getUrl();
            ImageMethodEnum method = result.getMethod();

            ImageGenerationResult generationResult = new ImageGenerationResult();
            generationResult.setPosition(position);
            generationResult.setUrl(cosUrl);
            generationResult.setMethod(method.getValue());
            generationResult.setKeywords(keywords);
            generationResult.setSectionTitle(sectionTitle);
            generationResult.setDescription(type);
            generationResult.setPlaceholderId(placeholderId);
            generationResult.setSuccess(true);

            return generationResult;

        } catch (Exception e) {
            log.error("图片生成失败: imageSource={}, position={}", imageSource, position, e);

            ImageGenerationResult failResult = new ImageGenerationResult();
            failResult.setPosition(position);
            failResult.setSuccess(false);
            failResult.setError(e.getMessage());
            failResult.setSectionTitle(sectionTitle);
            failResult.setPlaceholderId(placeholderId);

            return failResult;
        }
    }

    /**
     * 图片生成结果
     */
    @Data
    public static class ImageGenerationResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer position;
        private String url;
        private String method;
        private String keywords;
        private String sectionTitle;
        private String description;
        private String placeholderId;
        private boolean success;
        private String error;
    }
}