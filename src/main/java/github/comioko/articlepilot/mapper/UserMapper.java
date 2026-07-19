package github.comioko.articlepilot.mapper;

import com.mybatisflex.core.BaseMapper;
import github.comioko.articlepilot.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author comioko
 * @version 1.0
 * @className UserMapper
 * @since 1.0
 */

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
