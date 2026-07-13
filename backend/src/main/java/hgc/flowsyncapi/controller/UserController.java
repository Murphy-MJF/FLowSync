package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.dto.PasswordUpdateRequest;
import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.service.AuthService;
import hgc.flowsyncapi.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private AuthService authService;

    @GetMapping
    public ApiResponse<List<User>> list() {
        List<User> users = userService.listUsers();
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.ok(users);
    }

    @PostMapping("/update-password")
    public ApiResponse<Void> updatePassword(@RequestBody PasswordUpdateRequest request) {
        boolean ok = authService.updatePassword(request.getUserId(), request.getOldPassword(), request.getNewPassword());
        if (!ok) {
            return ApiResponse.fail("旧密码错误");
        }
        return ApiResponse.ok("密码修改成功", null);
    }
}
