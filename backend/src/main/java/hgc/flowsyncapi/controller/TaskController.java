package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.TaskInfo;
import hgc.flowsyncapi.service.TaskInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @Resource
    private TaskInfoService taskInfoService;

    @GetMapping
    public ApiResponse<List<TaskInfo>> list(@RequestParam(required = false) Long projectId) {
        return ApiResponse.ok(taskInfoService.listTasks(projectId));
    }

    @PostMapping
    public ApiResponse<TaskInfo> save(@RequestBody TaskInfo task) {
        return ApiResponse.ok(taskInfoService.saveTask(task));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskInfoService.deleteTask(id);
        return ApiResponse.ok("删除成功", null);
    }
}
