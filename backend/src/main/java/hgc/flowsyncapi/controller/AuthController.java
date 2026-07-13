package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.dto.LoginRequest;
import hgc.flowsyncapi.dto.PasswordUpdateRequest;
import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return ApiResponse.fail("用户名或密码错误");
        }
        user.setPassword(null);
        return ApiResponse.ok(Map.of("user", user, "token", "simple-token-" + user.getId()));
    }
}
