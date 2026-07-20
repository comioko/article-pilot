package github.comioko.articlepilot.mapper;

import com.mybatisflex.core.BaseMapper;
import github.comioko.articlepilot.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
