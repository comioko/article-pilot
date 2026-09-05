package github.comioko.articlepilot.model.vo;

import github.comioko.articlepilot.model.entity.ArticleRevision;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章版本视图。
 */
@Data
public class ArticleRevisionVO implements Serializable {

    private Long id;
    private Integer revisionNumber;
    private String revisionNote;
    private String content;
    private String fullContent;
    private LocalDateTime createTime;

    public static ArticleRevisionVO objToVo(ArticleRevision revision) {
        if (revision == null) {
            return null;
        }
        ArticleRevisionVO vo = new ArticleRevisionVO();
        BeanUtils.copyProperties(revision, vo);
        return vo;
    }
}
