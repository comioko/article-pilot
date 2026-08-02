package github.comioko.articlepilot.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import github.comioko.articlepilot.model.dto.user.UserQueryRequest;
import github.comioko.articlepilot.model.entity.User;
import github.comioko.articlepilot.model.vo.LoginUserVO;
import github.comioko.articlepilot.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author comioko
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（分页）
     *
     * @param userList 用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return 退出登录是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 当前用户修改个人资料
     *
     * @param currentUser 当前登录用户
     * @param userName    昵称（可空）
     * @param userProfile 简介（可空）
     * @param userAvatar  头像 URL（可空）
     * @return 更新后的脱敏用户信息
     */
    LoginUserVO updateMyProfile(User currentUser, String userName, String userProfile, String userAvatar);

    /**
     * 上传头像到 COS，返回访问 URL
     *
     * @param file 头像文件
     * @return COS 访问 URL
     */
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file);

    /**
     * 当前用户修改密码（校验原密码）
     *
     * @param currentUser 当前登录用户
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(User currentUser, String oldPassword, String newPassword);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptPassword(String userPassword);
}
