package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.*;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.*;
import hgc.flowsyncapi.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class GitHubRepositoryController {

    private final GitHubAuthService authService;
    private final GitHubApiClient apiClient;
    private final ProjectGithubRepoMapper repoMapper;
    private final GithubAccountMapper githubAccountMapper;
    private final UserMapper userMapper;
    private final ProjectInfoService projectInfoService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubRepositoryController(GitHubAuthService authService,
                                       GitHubApiClient apiClient,
                                       ProjectGithubRepoMapper repoMapper,
                                       GithubAccountMapper githubAccountMapper,
                                       UserMapper userMapper,
                                       ProjectInfoService projectInfoService) {
        this.authService = authService;
        this.apiClient = apiClient;
        this.repoMapper = repoMapper;
        this.githubAccountMapper = githubAccountMapper;
        this.userMapper = userMapper;
        this.projectInfoService = projectInfoService;
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

    /** 上传/更新文件到仓库 */
    @PutMapping("/github/repos/{owner}/{repo}/contents")
    public ApiResponse<Map<String, Object>> uploadFile(@PathVariable String owner,
                                                        @PathVariable String repo,
                                                        @RequestBody Map<String, Object> body,
                                                        HttpServletRequest req) {
        try {
            String token = getToken(req);
            String path = body.get("path").toString();
            String content = body.get("content").toString();
            String sha = body.containsKey("sha") && body.get("sha") != null ? body.get("sha").toString() : null;
            String branch = body.getOrDefault("branch", "main").toString();
            String message = body.getOrDefault("message", "[FlowSync] Update " + path).toString();

            // Build GitHub API request
            Map<String, Object> ghBody = new HashMap<>();
            ghBody.put("message", message);
            ghBody.put("content", content);
            ghBody.put("branch", branch);
            if (sha != null) ghBody.put("sha", sha);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ghBody, headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            return ApiResponse.ok("文件已上传", resp.getBody());
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 解绑项目仓库 */
    @DeleteMapping("/projects/{projectId}/github/repository")
    public ApiResponse<Void> unbindRepo(@PathVariable Long projectId) {
        repoMapper.delete(new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId));
        return ApiResponse.ok("已解绑", null);
    }

    /** 项目提交历史（含 FlowSync 用户映射 + 数据隔离） */
    @GetMapping("/projects/{projectId}/github/commits")
    public ApiResponse<List<Map<String, Object>>> projectCommits(@PathVariable Long projectId,
                                                                  HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        List<Long> visibleIds = projectInfoService.listVisibleProjectIds(userId);
        if (!visibleIds.contains(projectId)) return ApiResponse.fail("无权查看该项目");

        ProjectGithubRepo binding = repoMapper.selectOne(
                new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId));
        if (binding == null) return ApiResponse.fail("该项目未绑定仓库");

        try {
            String token = getToken(req);
            List<Map<String, Object>> commits = apiClient.listCommits(token,
                    binding.getOwner(), binding.getRepoName(), binding.getDefaultBranch());
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> c : commits) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sha", ((String)c.get("sha")).substring(0, 7));
                Map<String, Object> commit = (Map<String, Object>) c.get("commit");
                Map<String, Object> author = commit != null ? (Map<String, Object>) commit.get("author") : null;
                item.put("message", commit != null ? commit.get("message") : "");
                item.put("author", author != null ? author.get("name") : "");
                item.put("date", author != null ? author.get("date") : "");

                Map<String, Object> ghAuthor = (Map<String, Object>) c.get("author");
                String ghLogin = ghAuthor != null ? (String) ghAuthor.get("login") : null;
                item.put("githubLogin", ghLogin != null ? ghLogin : "");
                item.put("flowSyncUser", "");

                if (ghLogin != null) {
                    GithubAccount acc = githubAccountMapper.selectOne(
                            new QueryWrapper<GithubAccount>().eq("github_login", ghLogin));
                    if (acc != null) {
                        User u = userMapper.selectById(acc.getUserId());
                        if (u != null) item.put("flowSyncUser", u.getRealName());
                    }
                }
                result.add(item);
            }
            return ApiResponse.ok(result);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
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

    /** 获取文件树 */
    @GetMapping("/github/repos/{owner}/{repo}/tree")
    public ApiResponse<Map<String, Object>> getTree(@PathVariable String owner,
                                                     @PathVariable String repo,
                                                     @RequestParam(defaultValue = "main") String branch,
                                                     HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.getTree(getToken(req), owner, repo, branch));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 获取文件内容 */
    @GetMapping("/github/repos/{owner}/{repo}/contents")
    public ApiResponse<Map<String, Object>> getContents(@PathVariable String owner,
                                                         @PathVariable String repo,
                                                         @RequestParam String path,
                                                         @RequestParam(defaultValue = "main") String branch,
                                                         HttpServletRequest req) {
        try {
            return ApiResponse.ok(apiClient.getContents(getToken(req), owner, repo, path, branch));
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }
}
