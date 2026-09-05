package github.comioko.articlepilot.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 可直接发布到目标渠道的文章内容。
 */
@Data
@AllArgsConstructor
public class ArticlePublishPackageVO implements Serializable {

    private String channel;
    private String title;
    private String content;
}
