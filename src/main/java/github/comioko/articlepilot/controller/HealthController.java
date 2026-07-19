package github.comioko.articlepilot.controller;

import github.comioko.articlepilot.common.BaseResponse;
import github.comioko.articlepilot.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author comioko
 * @version 1.0
 * @className HealthController
 * @since 1.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    // 访问访问 http://localhost:8567/api/doc.html 能够看到接口文档
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
