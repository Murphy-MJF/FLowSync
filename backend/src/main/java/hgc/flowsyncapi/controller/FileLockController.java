package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.mapper.UserMapper;
import hgc.flowsyncapi.service.FileLockService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/github/file-lock")
public class FileLockController {

    private final FileLockService lockService;
    private final UserMapper userMapper;

    public FileLockController(FileLockService lockService, UserMapper userMapper) {
        this.lockService = lockService;
        this.userMapper = userMapper;
    }

    /** 获取文件锁 */
    @PostMapping("/acquire")
    public ApiResponse<Map<String, Object>> acquire(@RequestBody Map<String, String> body,
                                                     HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        User user = userMapper.selectById(userId);
        String userName = user != null ? user.getRealName() : "未知";

        FileLockService.AcquireResult result = lockService.acquire(
                body.get("owner"), body.get("repo"), body.get("branch"),
                body.get("path"), userId, userName);

        Map<String, Object> resp = new HashMap<>();
        resp.put("acquired", result.acquired);
        if (!result.acquired) {
            resp.put("queuePosition", result.queuePosition);
            resp.put("currentHolder", result.currentHolder);
            return ApiResponse.ok("文件正被 " + result.currentHolder + " 编辑，您排在第 " + result.queuePosition + " 位", resp);
        }
        return ApiResponse.ok("已获取编辑权限", resp);
    }

    /** 释放文件锁 */
    @PostMapping("/release")
    public ApiResponse<Void> release(@RequestBody Map<String, String> body,
                                      HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        FileLockService.WaitingUser next = lockService.release(
                body.get("owner"), body.get("repo"), body.get("branch"),
                body.get("path"), userId);
        if (next != null) {
            // 下一位等待者就绪的提示通过 status 轮询获知
        }
        return ApiResponse.ok("已释放", null);
    }

    /** 查询锁状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam String owner,
                                                    @RequestParam String repo,
                                                    @RequestParam String branch,
                                                    @RequestParam String path) {
        return ApiResponse.ok(lockService.status(owner, repo, branch, path));
    }
}
