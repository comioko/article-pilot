package github.comioko.articlepilot.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前用户修改密码请求
 */
@Data
public class PasswordChangeRequest implements Serializable {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
}