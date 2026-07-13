package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.GithubAccount;
import hgc.flowsyncapi.service.GitHubAuthService;
import hgc.flowsyncapi.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/github")
public class GitHubAuthController {

    private final GitHubAuthService authService;
    private final OperationLogService logService;

    public GitHubAuthController(GitHubAuthService authService, OperationLogService logService) {
        this.authService = authService;
        this.logService = logService;
    }

    /** 获取 GitHub 授权链接（前端传入当前页面 origin 作为 redirect_uri） */
    @GetMapping("/connect")
    public ApiResponse<Map<String, String>> connect(@RequestParam(defaultValue = "http://localhost:8081") String redirect) {
        String url = authService.getAuthorizationUrl(redirect + "/github-callback");
        return ApiResponse.ok(Map.of("url", url));
    }

    /** OAuth 回调 */
    @PostMapping("/callback")
    public ApiResponse<Map<String, String>> callback(@RequestBody Map<String, String> body,
                                                      HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        try {
            String code = body.get("code");
            String login = authService.exchangeAndSaveToken(userId, code);
            logService.log(userId, "GitHub授权", "GitHub", null, "绑定账号: " + login);
            return ApiResponse.ok("已连接 GitHub: " + login, Map.of("login", login));
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 查询连接状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        GithubAccount account = authService.getByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("connected", account != null);
        if (account != null) {
            result.put("login", account.getGithubLogin());
            result.put("githubUserId", account.getGithubUserId());
        }
        return ApiResponse.ok(result);
    }

    /** 解除绑定 */
    @PostMapping("/revoke")
    public ApiResponse<Void> revoke(HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        authService.revoke(userId);
        logService.log(userId, "GitHub解绑", "GitHub", null, "解除 GitHub 绑定");
        return ApiResponse.ok("已解除 GitHub 绑定", null);
    }
}
