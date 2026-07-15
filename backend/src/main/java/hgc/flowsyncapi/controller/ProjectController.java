package hgc.flowsyncapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.*;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.ProjectGithubRepoMapper;
import hgc.flowsyncapi.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectInfoService projectInfoService;
    private final OperationLogService logService;
    private final ProjectGithubRepoMapper repoMapper;
    private final GitHubAuthService githubAuthService;
    private final GitHubApiClient githubApiClient;

    public ProjectController(ProjectInfoService projectInfoService,
                              OperationLogService logService,
                              ProjectGithubRepoMapper repoMapper,
                              GitHubAuthService githubAuthService,
                              GitHubApiClient githubApiClient) {
        this.projectInfoService = projectInfoService;
        this.logService = logService;
        this.repoMapper = repoMapper;
        this.githubAuthService = githubAuthService;
        this.githubApiClient = githubApiClient;
    }

    @GetMapping
    public ApiResponse<List<ProjectInfo>> list(HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        return ApiResponse.ok(projectInfoService.listProjects(userId));
    }

    @PostMapping
    public ApiResponse<ProjectInfo> save(@RequestBody ProjectInfo project,
                                          HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        try {
            ProjectInfo saved = projectInfoService.saveProject(project, userId);
            String action = project.getId() == null ? "创建项目" : "编辑项目";
            logService.log(userId, action, "项目", saved.getId(), "项目：" + saved.getName());
            return ApiResponse.ok(saved);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        try {
            ProjectInfo project = projectInfoService.listProjects(userId).stream()
                    .filter(p -> p.getId().equals(id)).findFirst().orElse(null);
            // 用项目负责人的 GitHub token 删仓库
            if (project != null) {
                String token = githubAuthService.getToken(project.getOwnerId());
                if (token != null) {
                    ProjectGithubRepo binding = repoMapper.selectOne(
                            new QueryWrapper<ProjectGithubRepo>().eq("project_id", id));
                    if (binding != null) {
                        try { githubApiClient.archiveRepository(token, binding.getOwner(), binding.getRepoName()); }
                        catch (Exception e) { logService.log(userId, "归档仓库失败", "项目", id, e.getMessage()); }
                        repoMapper.deleteById(binding.getId());
                    }
                }
            }
            projectInfoService.deleteProject(id, userId);
            logService.log(userId, "删除项目", "项目", id, "删除项目（含GitHub仓库）");
            return ApiResponse.ok(null);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 归档项目（项目完成时调用） */
    @PostMapping("/{id}/archive")
    public ApiResponse<Void> archive(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        try {
            ProjectInfo project = projectInfoService.listProjects(userId).stream()
                    .filter(p -> p.getId().equals(id)).findFirst().orElse(null);
            if (project != null) {
                String token = githubAuthService.getToken(project.getOwnerId());
                if (token != null) {
                    ProjectGithubRepo binding = repoMapper.selectOne(
                            new QueryWrapper<ProjectGithubRepo>().eq("project_id", id));
                    if (binding != null) {
                        githubApiClient.archiveRepository(token, binding.getOwner(), binding.getRepoName());
                        logService.log(userId, "归档仓库", "项目", id, "归档 GitHub 仓库: " + binding.getRepoName());
                    }
                }
            }
            return ApiResponse.ok("项目已归档", null);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Map<String, Integer>> batchDelete(@RequestBody Map<String, List<Long>> body,
                                                          HttpServletRequest request) {
        Long userId = AuthController.getCurrentUserId(request);
        List<Long> ids = body.get("ids");
        int count = 0;
        for (Long id : ids) {
            try {
                projectInfoService.deleteProject(id, userId);
                count++;
            } catch (RuntimeException ignored) {}
        }
        logService.log(userId, "批量删除项目", "项目", null, "删除 " + count + " 个项目");
        return ApiResponse.ok("成功删除 " + count + " 个项目", Map.of("deleted", count));
    }
}
