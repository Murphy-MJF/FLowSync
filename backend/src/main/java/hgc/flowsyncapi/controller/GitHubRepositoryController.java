package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.ProjectGithubRepo;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.ProjectGithubRepoMapper;
import hgc.flowsyncapi.service.GitHubAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class GitHubRepositoryController {

    private final GitHubAuthService authService;
    private final GitHubApiClient apiClient;
    private final ProjectGithubRepoMapper repoMapper;

    public GitHubRepositoryController(GitHubAuthService authService,
                                       GitHubApiClient apiClient,
                                       ProjectGithubRepoMapper repoMapper) {
        this.authService = authService;
        this.apiClient = apiClient;
        this.repoMapper = repoMapper;
    }

    private String getToken(HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        String token = authService.getToken(userId);
        if (token == null) throw new RuntimeException("请先连接 GitHub 账号");
        return token;
    }

    /** 列出用户可访问的仓库 */
    @GetMapping("/github/repositories")
    public ApiResponse<List<Map<String, Object>>> listRepositories(HttpServletRequest req) {
        try {
            String token = getToken(req);
            return ApiResponse.ok(apiClient.listRepositories(token));
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 绑定仓库到项目 */
    @PostMapping("/projects/{projectId}/github/repository")
    public ApiResponse<ProjectGithubRepo> bindRepo(@PathVariable Long projectId,
                                                    @RequestBody Map<String, Object> body,
                                                    HttpServletRequest req) {
        try {
            String token = getToken(req);
            String owner = body.get("owner").toString();
            String repo = body.get("repo").toString();
            Map<String, Object> ghRepo = apiClient.getRepository(token, owner, repo);

            // 删除旧绑定
            repoMapper.delete(new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId));

            ProjectGithubRepo binding = new ProjectGithubRepo();
            binding.setProjectId(projectId);
            binding.setRepoId(Long.valueOf(ghRepo.get("id").toString()));
            binding.setOwner(owner);
            binding.setRepoName(repo);
            binding.setDefaultBranch((String) ghRepo.getOrDefault("default_branch", "main"));
            binding.setRepoUrl((String) ghRepo.get("html_url"));
            binding.setSyncStatus("active");
            repoMapper.insert(binding);
            return ApiResponse.ok("仓库已绑定", binding);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 查询项目绑定的仓库 */
    @GetMapping("/projects/{projectId}/github/status")
    public ApiResponse<ProjectGithubRepo> repoStatus(@PathVariable Long projectId) {
        ProjectGithubRepo binding = repoMapper.selectOne(
                new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId));
        return binding != null ? ApiResponse.ok(binding) : ApiResponse.fail("未绑定仓库");
    }

    /** 只读：仓库分支列表 */
    @GetMapping("/github/repos/{owner}/{repo}/branches")
    public ApiResponse<List<Map<String, Object>>> listBranches(@PathVariable String owner,
                                                                @PathVariable String repo,
                                                                HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.listBranches(getToken(req), owner, repo));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 只读：最近提交 */
    @GetMapping("/github/repos/{owner}/{repo}/commits")
    public ApiResponse<List<Map<String, Object>>> listCommits(@PathVariable String owner,
                                                               @PathVariable String repo,
                                                               @RequestParam(defaultValue = "main") String branch,
                                                               HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.listCommits(getToken(req), owner, repo, branch));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 只读：Issue 列表 */
    @GetMapping("/github/repos/{owner}/{repo}/issues")
    public ApiResponse<List<Map<String, Object>>> listIssues(@PathVariable String owner,
                                                              @PathVariable String repo,
                                                              HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.listIssues(getToken(req), owner, repo));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 只读：PR 列表 */
    @GetMapping("/github/repos/{owner}/{repo}/pulls")
    public ApiResponse<List<Map<String, Object>>> listPullRequests(@PathVariable String owner,
                                                                    @PathVariable String repo,
                                                                    HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.listPullRequests(getToken(req), owner, repo));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }
}
