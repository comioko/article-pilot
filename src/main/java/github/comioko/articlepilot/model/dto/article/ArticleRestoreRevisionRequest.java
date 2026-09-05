package github.comioko.articlepilot.model.dto.article;

import lombok.Data;

/**
 * 恢复文章历史版本请求。
 */
@Data
public class ArticleRestoreRevisionRequest {

    private String taskId;

    /** 客户端读取的正文指纹，用于检测并发修改。 */
    private String baseFingerprint;

    private Long revisionId;
}
