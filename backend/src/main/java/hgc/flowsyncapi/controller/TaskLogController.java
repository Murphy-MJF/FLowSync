package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.TaskLog;
import hgc.flowsyncapi.service.TaskLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/task-logs")
public class TaskLogController {
    @Resource
    private TaskLogService taskLogService;

    @GetMapping
    public ApiResponse<List<TaskLog>> list(@RequestParam(required = false) Long taskId) {
        return ApiResponse.ok(taskLogService.listTaskLogs(taskId));
    }

    @PostMapping
    public ApiResponse<TaskLog> save(@RequestBody TaskLog taskLog) {
        return ApiResponse.ok(taskLogService.saveTaskLog(taskLog));
    }
}
