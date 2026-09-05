package github.comioko.articlepilot.mapper;

import com.mybatisflex.core.BaseMapper;
import github.comioko.articlepilot.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT * FROM article WHERE taskId = #{taskId} AND isDelete = 0 FOR UPDATE")
    Article selectForEditing(@Param("taskId") String taskId);

}
