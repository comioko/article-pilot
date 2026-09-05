package github.comioko.articlepilot.model.dto.article;

import lombok.Data;

/**
 * 文章选中文本的 AI 精修请求。
 */
@Data
public class ArticleAiRefineRequest {

    private String taskId;

    private String selectedText;

    /** 精修预设或用户的自然语言要求 */
    private String instruction;
}
