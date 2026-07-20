package github.comioko.articlepilot.model.dto.article;


import github.comioko.articlepilot.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询文章请求
 *
 * @author comioko
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 状态
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
