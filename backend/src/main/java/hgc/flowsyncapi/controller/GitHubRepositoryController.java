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
    private final TaskInfoService taskInfoService;
    private final GithubAuthorizedRepoMapper authorizedRepoMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubRepositoryController(GitHubAuthService authService,
                                       GitHubApiClient apiClient,
                                       ProjectGithubRepoMapper repoMapper,
                                       GithubAccountMapper githubAccountMapper,
                                       UserMapper userMapper,
                                       ProjectInfoService projectInfoService,
                                       TaskInfoService taskInfoService,
                                       GithubAuthorizedRepoMapper authorizedRepoMapper) {
        this.authService = authService;
        this.apiClient = apiClient;
        this.repoMapper = repoMapper;
        this.githubAccountMapper = githubAccountMapper;
        this.userMapper = userMapper;
        this.projectInfoService = projectInfoService;
        this.taskInfoService = taskInfoService;
        this.authorizedRepoMapper = authorizedRepoMapper;
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
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 列出项目负责人名下可访问的仓库（仅已授权的） */
    @GetMapping("/projects/{projectId}/github/repositories")
    public ApiResponse<List<Map<String, Object>>> listOwnerRepositories(@PathVariable Long projectId,
                                                                         HttpServletRequest req) {
        try {
            ProjectInfo project = projectInfoService.listProjects(
                    AuthController.getCurrentUserId(req)).stream()
                    .filter(p -> p.getId().equals(projectId)).findFirst().orElse(null);
            if (project == null) return ApiResponse.fail("项目不存在");
            String token = authService.getToken(project.getOwnerId());
            if (token == null) return ApiResponse.fail("项目负责人未连接 GitHub");
            List<Map<String, Object>> all = apiClient.listRepositories(token);
            // 获取已授权的仓库列表
            List<GithubAuthorizedRepo> authorized = authorizedRepoMapper.selectList(
                    new QueryWrapper<GithubAuthorizedRepo>().eq("user_id", project.getOwnerId()));
            java.util.Set<String> allowed = new HashSet<>();
            for (GithubAuthorizedRepo a : authorized) allowed.add(a.getOwner() + "/" + a.getRepoName());
            // 过滤：只显示已授权的仓库 + FlowSync 创建的仓库
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> r : all) {
                String fullName = (String) r.get("full_name");
                if (allowed.contains(fullName) || fullName.startsWith("flowsync-")) {
                    filtered.add(r);
                }
            }
            return ApiResponse.ok(filtered);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 授权仓库（添加到可访问列表） */
    @PostMapping("/github/authorize-repo")
    public ApiResponse<Void> authorizeRepo(@RequestBody Map<String, String> body,
                                            HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        String owner = body.get("owner");
        String repo = body.get("repo");
        GithubAuthorizedRepo existing = authorizedRepoMapper.selectOne(
                new QueryWrapper<GithubAuthorizedRepo>().eq("user_id", userId)
                        .eq("owner", owner).eq("repo_name", repo));
        if (existing != null) return ApiResponse.ok("已授权", null);
        GithubAuthorizedRepo ar = new GithubAuthorizedRepo();
        ar.setUserId(userId);
        ar.setOwner(owner);
        ar.setRepoName(repo);
        ar.setRepoFullName(owner + "/" + repo);
        authorizedRepoMapper.insert(ar);
        return ApiResponse.ok("已授权", null);
    }

    /** 取消授权 */
    @DeleteMapping("/github/authorize-repo")
    public ApiResponse<Void> deauthorizeRepo(@RequestBody Map<String, String> body,
                                              HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        authorizedRepoMapper.delete(new QueryWrapper<GithubAuthorizedRepo>()
                .eq("user_id", userId).eq("repo_full_name", body.get("fullName")));
        return ApiResponse.ok("已取消授权", null);
    }

    /** 获取已授权仓库列表 */
    @GetMapping("/github/authorized-repos")
    public ApiResponse<List<GithubAuthorizedRepo>> listAuthorized(HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        return ApiResponse.ok(authorizedRepoMapper.selectList(
                new QueryWrapper<GithubAuthorizedRepo>().eq("user_id", userId)));
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
            repoMapper.delete(new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId).last("limit 1"));

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
                new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId).last("limit 1"));
        return binding != null ? ApiResponse.ok(binding) : ApiResponse.fail("未绑定仓库");
    }

    /** 删除文件 */
    @DeleteMapping("/github/repos/{owner}/{repo}/contents")
    public ApiResponse<Void> deleteFile(@PathVariable String owner,
                                         @PathVariable String repo,
                                         @RequestParam String path,
                                         @RequestParam String sha,
                                         @RequestParam(defaultValue = "main") String branch,
                                         @RequestParam(defaultValue = "[FlowSync] Delete") String message,
                                         HttpServletRequest req) {
        try {
            apiClient.deleteFile(getToken(req), owner, repo, path, sha, branch, message);
            return ApiResponse.ok("已删除", null);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
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
        repoMapper.delete(new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId).last("limit 1"));
        return ApiResponse.ok("已解绑", null);
    }

    /** 创建新仓库并绑定到项目 */
    @PostMapping("/projects/{projectId}/github/create-repo")
    public ApiResponse<ProjectGithubRepo> createAndBind(@PathVariable Long projectId,
                                                         @RequestBody Map<String, String> body,
                                                         HttpServletRequest req) {
        try {
            String token = getToken(req);
            String name = body.getOrDefault("name", "flowsync-project-" + projectId);
            String desc = body.getOrDefault("description", "Created by FlowSync");
            boolean isPrivate = Boolean.parseBoolean(body.getOrDefault("private", "false"));

            Map<String, Object> ghRepo = apiClient.createRepository(token, name, desc, isPrivate);
            String owner = (String) ((Map<String, Object>) ghRepo.get("owner")).get("login");
            String repoName = (String) ghRepo.get("name");

            repoMapper.delete(new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId).last("limit 1"));
            ProjectGithubRepo binding = new ProjectGithubRepo();
            binding.setProjectId(projectId);
            binding.setRepoId(Long.valueOf(ghRepo.get("id").toString()));
            binding.setOwner(owner);
            binding.setRepoName(repoName);
            binding.setDefaultBranch((String) ghRepo.getOrDefault("default_branch", "main"));
            binding.setRepoUrl((String) ghRepo.get("html_url"));
            binding.setSyncStatus("active");
            repoMapper.insert(binding);
            // 自动授权
            GithubAuthorizedRepo ar = new GithubAuthorizedRepo();
            ar.setUserId(AuthController.getCurrentUserId(req));
            ar.setOwner(owner);
            ar.setRepoName(repoName);
            ar.setRepoFullName(owner + "/" + repoName);
            authorizedRepoMapper.insert(ar);
            return ApiResponse.ok("仓库已创建并绑定", binding);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 为任务创建 GitHub Issue + 分支 */
    @PostMapping("/tasks/{taskId}/github/publish")
    public ApiResponse<Map<String, Object>> publishTask(@PathVariable Long taskId,
                                                         HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        try {
            String token = getToken(req);
            TaskInfo task = taskInfoService.getById(taskId);
            if (task == null) return ApiResponse.fail("任务不存在");

            ProjectGithubRepo binding = repoMapper.selectOne(
                    new QueryWrapper<ProjectGithubRepo>().eq("project_id", task.getProjectId()));
            if (binding == null) return ApiResponse.fail("该项目未绑定仓库");

            String owner = binding.getOwner();
            String repo = binding.getRepoName();
            String branchName = "task/" + taskId + "-" + slugify(task.getTitle());
            String issueBody = "## 任务说明\n" + (task.getDescription() != null ? task.getDescription() : "") +
                    "\n\n## 验收标准\n- [ ] 功能实现\n- [ ] 代码通过测试\n\n---\n*Created by FlowSync*";

            Map<String, Object> issue = apiClient.createIssue(token, owner, repo,
                    task.getTitle(), issueBody);
            Map<String, Object> branch = apiClient.createBranch(token, owner, repo, branchName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("issueNumber", issue.get("number"));
            result.put("issueUrl", issue.get("html_url"));
            result.put("branchName", branchName);
            result.put("taskId", taskId);
            // 标记任务已发布
            task.setGithubPublished(true);
            taskInfoService.saveTask(task, userId);
            return ApiResponse.ok("已发布 Issue #" + issue.get("number") + " + 分支 " + branchName, result);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    /** 批量发布任务（AI 拆解导入后调用） */
    @PostMapping("/tasks/batch-publish")
    public ApiResponse<Map<String, Object>> batchPublish(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest req) {
        try {
            String token = getToken(req);
            List<Integer> taskIds = (List<Integer>) body.get("taskIds");
            ProjectGithubRepo binding = repoMapper.selectOne(
                    new QueryWrapper<ProjectGithubRepo>().eq("project_id",
                            Integer.valueOf(body.get("projectId").toString()).longValue()));
            if (binding == null) return ApiResponse.fail("该项目未绑定仓库");

            String owner = binding.getOwner();
            String repo = binding.getRepoName();
            int count = 0;
            for (Integer id : taskIds) {
                TaskInfo task = taskInfoService.getById(id.longValue());
                if (task == null) continue;
                String branchName = "task/" + id + "-" + slugify(task.getTitle());
                try {
                    apiClient.createIssue(token, owner, repo, task.getTitle(),
                            task.getDescription() != null ? task.getDescription() : "");
                    apiClient.createBranch(token, owner, repo, branchName);
                    count++;
                } catch (Exception ignored) {}
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("published", count);
            return ApiResponse.ok("已发布 " + count + " 个任务", result);
        } catch (RuntimeException e) { return ApiResponse.fail(e.getMessage()); }
    }

    private String slugify(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    /** 项目提交历史（含 FlowSync 用户映射 + 数据隔离） */
    @GetMapping("/projects/{projectId}/github/commits")
    public ApiResponse<List<Map<String, Object>>> projectCommits(@PathVariable Long projectId,
                                                                  HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        List<Long> visibleIds = projectInfoService.listVisibleProjectIds(userId);
        if (!visibleIds.contains(projectId)) return ApiResponse.fail("无权查看该项目");

        ProjectGithubRepo binding = repoMapper.selectOne(
                new QueryWrapper<ProjectGithubRepo>().eq("project_id", projectId).last("limit 1"));
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
