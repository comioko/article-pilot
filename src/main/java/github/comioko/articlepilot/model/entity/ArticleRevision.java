package github.comioko.articlepilot.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章成稿的可恢复快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article_revision", camelToUnderline = false)
public class ArticleRevision implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String taskId;

    private Long userId;

    private Integer revisionNumber;

    private String content;

    private String fullContent;

    private String revisionNote;

    private LocalDateTime createTime;
}
