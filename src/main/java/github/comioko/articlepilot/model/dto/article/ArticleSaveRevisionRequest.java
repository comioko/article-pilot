package github.comioko.articlepilot.model.dto.article;

import lombok.Data;

/**
 * 确认精修后保存文章及其版本快照。
 */
@Data
public class ArticleSaveRevisionRequest {

    private String taskId;

    /** 客户端读取的正文指纹，用于检测并发修改。 */
    private String baseFingerprint;

    private String content;

    private String fullContent;

    private String revisionNote;
}
