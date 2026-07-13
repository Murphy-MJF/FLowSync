package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.ProjectInfo;
import hgc.flowsyncapi.service.ProjectInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @Resource
    private ProjectInfoService projectInfoService;

    @GetMapping
    public ApiResponse<List<ProjectInfo>> list() {
        return ApiResponse.ok(projectInfoService.listProjects());
    }

    @PostMapping
    public ApiResponse<ProjectInfo> save(@RequestBody ProjectInfo project) {
        return ApiResponse.ok(projectInfoService.saveProject(project));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectInfoService.deleteProject(id);
        return ApiResponse.ok("删除成功", null);
    }
}
