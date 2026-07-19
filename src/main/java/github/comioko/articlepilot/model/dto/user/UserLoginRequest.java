package github.comioko.articlepilot.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author comioko
 * @version 1.0
 * @className UserLoginRequest
 * @since 1.0
 */
@Data
public class UserLoginRequest implements Serializable {
    private String userAccount;
    private String userPassword;
}
