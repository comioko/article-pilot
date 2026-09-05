package github.comioko.articlepilot.model.dto.article;

import lombok.Data;

/**
 * 生成指定渠道发布稿的请求。
 */
@Data
public class ArticlePublishPackageRequest {

    private String taskId;

    /** WECHAT 或 XIAOHONGSHU */
    private String channel;
}
