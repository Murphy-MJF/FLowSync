package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.*;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.*;
import hgc.flowsyncapi.service.GitHubAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class FileCacheController {

    private final FileCacheMapper cacheMapper;
    private final UserMapper userMapper;
    private final ProjectGithubRepoMapper repoMapper;
    private final GitHubAuthService authService;
    private final GitHubApiClient apiClient;
    private final ProjectInfoMapper projectInfoMapper;

    public FileCacheController(FileCacheMapper cacheMapper, UserMapper userMapper,
                                ProjectGithubRepoMapper repoMapper, GitHubAuthService authService,
                                GitHubApiClient apiClient, ProjectInfoMapper projectInfoMapper) {
        this.cacheMapper = cacheMapper;
        this.userMapper = userMapper;
        this.repoMapper = repoMapper;
        this.authService = authService;
        this.apiClient = apiClient;
        this.projectInfoMapper = projectInfoMapper;
    }

    /** 提交文件到审核缓存 */
    @PostMapping("/projects/{projectId}/file-cache")
    public ApiResponse<FileCache> submit(@PathVariable Long projectId,
                                          @RequestBody FileCache fc,
                                          HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        fc.setProjectId(projectId);
        fc.setSubmittedBy(userId);
        fc.setStatus("pending");
        cacheMapper.insert(fc);
        return ApiResponse.ok("已提交审核", fc);
    }

    /** 获取项目的待审核文件列表（负责人查看） */
    @GetMapping("/projects/{projectId}/file-cache")
    public ApiResponse<List<FileCache>> list(@PathVariable Long projectId) {
        List<FileCache> list = cacheMapper.selectList(
                new QueryWrapper<FileCache>().eq("project_id", projectId).orderByDesc("create_time"));
        Map<Long, String> names = new HashMap<>();
        for (FileCache f : list) {
            if (!names.containsKey(f.getSubmittedBy())) {
                User u = userMapper.selectById(f.getSubmittedBy());
                names.put(f.getSubmittedBy(), u != null ? u.getRealName() : "未知");
            }
            f.setSubmitterName(names.get(f.getSubmittedBy()));
        }
        return ApiResponse.ok(list);
    }

    /** 批准并上传到 GitHub */
    @PostMapping("/file-cache/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        FileCache fc = cacheMapper.selectById(id);
        if (fc == null) return ApiResponse.fail("记录不存在");
        if (!"pending".equals(fc.getStatus())) return ApiResponse.fail("已处理");

        ProjectGithubRepo binding = repoMapper.selectOne(
                new QueryWrapper<ProjectGithubRepo>().eq("project_id", fc.getProjectId()));
        if (binding == null) return ApiResponse.fail("项目未绑定仓库");

        // 使用项目负责人的 GitHub token 上传
        ProjectInfo project = projectInfoMapper.selectById(fc.getProjectId());
        String token = authService.getToken(project != null ? project.getOwnerId() : userId);
        if (token == null) return ApiResponse.fail("项目负责人未连接 GitHub");

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", fc.getMessage() != null ? fc.getMessage() : "[FlowSync] Update " + fc.getFilePath());
            body.put("content", fc.getContent());
            body.put("branch", fc.getBranch() != null ? fc.getBranch() : "main");
            if (fc.getOriginalSha() != null) body.put("sha", fc.getOriginalSha());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            new org.springframework.web.client.RestTemplate().exchange(
                    "https://api.github.com/repos/" + binding.getOwner() + "/" + binding.getRepoName()
                            + "/contents/" + fc.getFilePath(),
                    org.springframework.http.HttpMethod.PUT,
                    new org.springframework.http.HttpEntity<>(body, headers), Map.class);

            fc.setStatus("approved");
            fc.setReviewedBy(userId);
            cacheMapper.updateById(fc);
            return ApiResponse.ok("已上传到 GitHub", null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 拒绝 */
    @PostMapping("/file-cache/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        FileCache fc = cacheMapper.selectById(id);
        if (fc == null) return ApiResponse.fail("记录不存在");
        fc.setStatus("rejected");
        fc.setReviewedBy(userId);
        cacheMapper.updateById(fc);
        return ApiResponse.ok("已拒绝", null);
    }
}
