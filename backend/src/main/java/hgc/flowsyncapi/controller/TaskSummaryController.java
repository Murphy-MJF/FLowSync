package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.entity.TaskSummary;
import hgc.flowsyncapi.service.TaskSummaryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/summaries")
public class TaskSummaryController {
    @Resource
    private TaskSummaryService taskSummaryService;

    @GetMapping
    public ApiResponse<List<TaskSummary>> list() {
        return ApiResponse.ok(taskSummaryService.listSummaries());
    }

    @PostMapping
    public ApiResponse<TaskSummary> save(@RequestBody TaskSummary summary) {
        return ApiResponse.ok(taskSummaryService.saveSummary(summary));
    }
}
