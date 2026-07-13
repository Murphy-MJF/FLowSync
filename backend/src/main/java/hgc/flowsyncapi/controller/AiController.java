package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.dto.*;
import hgc.flowsyncapi.service.QwenService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    @Resource
    private QwenService qwenService;

    @PostMapping("/task-suggestion")
    public ApiResponse<AiTaskSuggestionResponse> taskSuggestion(@RequestBody AiTaskSuggestionRequest request) {
        return ApiResponse.ok(qwenService.getTaskSuggestion(request));
    }

    @PostMapping("/task-plan")
    public ApiResponse<AiTaskPlanResponse> taskPlan(@RequestBody AiTaskPlanRequest request) {
        return ApiResponse.ok(qwenService.getTaskPlan(request));
    }

    @PostMapping("/task-plan/import")
    public ApiResponse<Void> importPlan(@RequestBody AiTaskPlanImportRequest request) {
        qwenService.importTaskPlan(request);
        return ApiResponse.ok("导入成功", null);
    }
}
