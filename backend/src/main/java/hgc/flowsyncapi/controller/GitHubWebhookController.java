package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.*;
import hgc.flowsyncapi.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.util.*;

@RestController
@RequestMapping("/api/github")
public class GitHubWebhookController {

    @Value("${github.webhook-secret:}")
    private String webhookSecret;

    private final TaskInfoMapper taskInfoMapper;
    private final TaskLogMapper taskLogMapper;
    private final ProjectGithubRepoMapper repoMapper;

    public GitHubWebhookController(TaskInfoMapper taskInfoMapper,
                                    TaskLogMapper taskLogMapper,
                                    ProjectGithubRepoMapper repoMapper) {
        this.taskInfoMapper = taskInfoMapper;
        this.taskLogMapper = taskLogMapper;
        this.repoMapper = repoMapper;
    }

    @PostMapping("/webhook")
    public ApiResponse<Void> handleWebhook(@RequestBody String payload,
                                            @RequestHeader("X-GitHub-Event") String event,
                                            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
                                            HttpServletRequest request) {
        // 仅处理 push 事件
        if (!"push".equals(event)) return ApiResponse.ok("ignored", null);

        // 签名验证（如配置了 webhook secret）
        if (webhookSecret != null && !webhookSecret.isEmpty() && signature != null) {
            String expected = "sha256=" + hmacSha256(webhookSecret, payload);
            if (!expected.equals(signature)) return ApiResponse.fail("签名验证失败");
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(payload, Map.class);
            String ref = (String) data.get("ref");           // refs/heads/task/3-slug
            Map<String, Object> repo = (Map<String, Object>) data.get("repository");
            String fullName = (String) repo.get("full_name");

            if (ref == null || !ref.contains("task/")) return ApiResponse.ok("非任务分支", null);

            // 解析分支名：task/{taskId}-{slug}
            String branch = ref.replace("refs/heads/", "");
            String taskIdStr = branch.substring(5).split("-")[0];
            Long taskId = Long.valueOf(taskIdStr);

            // 更新任务状态为"进行中"
            TaskInfo task = taskInfoMapper.selectById(taskId);
            if (task != null) {
                if ("未开始".equals(task.getStatus())) {
                    task.setStatus("进行中");
                    taskInfoMapper.updateById(task);

                    // 记录进度
                    TaskLog log = new TaskLog();
                    log.setTaskId(taskId);
                    log.setProgressPercent(10);
                    log.setContent("成员推送代码到分支 " + branch);
                    log.setOperatorId(task.getAssigneeId());
                    taskLogMapper.insert(log);
                }
            }
            return ApiResponse.ok("已更新", null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return ""; }
    }
}
