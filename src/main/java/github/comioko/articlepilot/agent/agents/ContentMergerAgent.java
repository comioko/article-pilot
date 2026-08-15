package github.comioko.articlepilot.agent.agents;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import github.comioko.articlepilot.agent.tools.ImageGenerationTool;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图文合成 Agent
 * 将配图插入到正文的相应位置
 *
 * @author AI Passage Creator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContentMergerAgent implements NodeAction {

    public static final String INPUT_CONTENT = "content";
    public static final String INPUT_IMAGES = "images";
    public static final String OUTPUT_FULL_CONTENT = "fullContent";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String content = state.value(INPUT_CONTENT)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少正文内容参数"));

        List<ArticleState.ImageResult> images = state.value(INPUT_IMAGES)
                .map(this::parseImagesFromState)
                .orElse(new ArrayList<>());

        log.info("ContentMergerAgent 开始执行: 正文长度={}, 图片数量={}", content.length(), images.size());

        String fullContent = mergeImagesIntoContent(content, images);

        log.info("ContentMergerAgent 执行完成: 完整内容长度={}", fullContent.length());

        return Map.of(OUTPUT_FULL_CONTENT, fullContent);
    }

    /**
     * 从 state value 解析图片列表
     * 兼容 List 对象、JSON 字符串、null/空 等场景
     */
    private List<ArticleState.ImageResult> parseImagesFromState(Object v) {
        if (v == null) {
            return new ArrayList<>();
        }
        if (v instanceof List) {
            List<?> list = (List<?>) v;
            if (list.isEmpty()) {
                return new ArrayList<>();
            }
            if (list.get(0) instanceof ArticleState.ImageResult) {
                @SuppressWarnings("unchecked")
                List<ArticleState.ImageResult> typed = (List<ArticleState.ImageResult>) list;
                return typed;
            }
            return convertToImageResults(list);
        }
        // 兼容跨节点传递时被序列化为 JSON 字符串的情况
        try {
            String json = v.toString();
            if (json.isBlank() || "null".equalsIgnoreCase(json) || "[]".equals(json.trim())) {
                return new ArrayList<>();
            }
            List<ArticleState.ImageResult> parsed = GsonUtils.fromJson(
                    json,
                    new com.google.gson.reflect.TypeToken<List<ArticleState.ImageResult>>() {
                    }
            );
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            log.warn("images 解析失败, raw={}", v, e);
            return new ArrayList<>();
        }
    }

    /**
     * 将配图插入正文（使用占位符替换）
     */
    private String mergeImagesIntoContent(String content, List<ArticleState.ImageResult> images) {
        if (images == null || images.isEmpty()) {
            return content;
        }

        String fullContent = content;
        
        // 遍历所有配图，根据占位符替换为实际图片
        for (ArticleState.ImageResult image : images) {
            String placeholder = image.getPlaceholderId();
            log.info("处理图片: position={}, placeholderId={}, url={}", 
                    image.getPosition(), placeholder, image.getUrl());
            
            if (placeholder != null && !placeholder.isEmpty()) {
                String description = image.getDescription() != null ? image.getDescription() : "配图";
                String imageMarkdown = "![" + description + "](" + image.getUrl() + ")";
                
                if (fullContent.contains(placeholder)) {
                    fullContent = fullContent.replace(placeholder, imageMarkdown);
                    log.info("成功替换占位符: {} -> {}", placeholder, imageMarkdown.substring(0, Math.min(50, imageMarkdown.length())));
                } else {
                    log.warn("正文中未找到占位符: {}", placeholder);
                }
            } else {
                log.warn("图片 position={} 的 placeholderId 为空", image.getPosition());
            }
        }
        
        return fullContent;
    }

    /**
     * 转换列表为 ImageResult 列表
     */
    private List<ArticleState.ImageResult> convertToImageResults(List<?> list) {
        List<ArticleState.ImageResult> results = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ArticleState.ImageResult) {
                results.add((ArticleState.ImageResult) item);
            } else if (item instanceof ImageGenerationTool.ImageGenerationResult) {
                // 从 ImageGenerationTool.ImageGenerationResult 转换
                ImageGenerationTool.ImageGenerationResult genResult = 
                        (ImageGenerationTool.ImageGenerationResult) item;
                if (genResult.isSuccess()) {
                    ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
                    imageResult.setPosition(genResult.getPosition());
                    imageResult.setUrl(genResult.getUrl());
                    imageResult.setMethod(genResult.getMethod());
                    imageResult.setKeywords(genResult.getKeywords());
                    imageResult.setSectionTitle(genResult.getSectionTitle());
                    imageResult.setDescription(genResult.getDescription());
                    imageResult.setPlaceholderId(genResult.getPlaceholderId());
                    results.add(imageResult);
                }
            } else if (item instanceof Map) {
                // 从 Map 转换
                String json = GsonUtils.toJson(item);
                ArticleState.ImageResult imageResult = GsonUtils.fromJson(json, ArticleState.ImageResult.class);
                if (imageResult.getUrl() != null) {
                    results.add(imageResult);
                }
            }
        }
        return results;
    }
}
