package github.comioko.articlepilot.exception;


import github.comioko.articlepilot.common.BaseResponse;
import github.comioko.articlepilot.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 当项目里出现异常时，统一拦截并返回固定格式的错误响应。
 * @author comioko
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        // 业务预期内的异常（未登录/无权限/参数错）走 warn，避免污染错误日志
        if (e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()
                || e.getCode() == ErrorCode.NO_AUTH_ERROR.getCode()
                || e.getCode() == ErrorCode.PARAMS_ERROR.getCode()) {
            log.warn("BusinessException [{}]: {}", e.getCode(), e.getMessage());
        } else {
            log.error("BusinessException", e);
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}