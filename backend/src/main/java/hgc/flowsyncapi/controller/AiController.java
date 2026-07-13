package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.dto.*;
import hgc.flowsyncapi.entity.TaskInfo;
import hgc.flowsyncapi.service.OperationLogService;
import hgc.flowsyncapi.service.QwenService;
import hgc.flowsyncapi.service.TaskInfoService;
import hgc.flowsyncapi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final QwenService qwenService;
    private final TaskInfoService taskInfoService;
    private final OperationLogService logService;
    private final UserService userService;

    public AiController(QwenService qwenService,
                        TaskInfoService taskInfoService,
                        OperationLogService logService,
                        UserService userService) {
        this.qwenService = qwenService;
        this.taskInfoService = taskInfoService;
        this.logService = logService;
        this.userService = userService;
    }

    /** 单任务 AI 建议 */
    @PostMapping("/task-suggestion")
    public ApiResponse<Map<String, String>> taskSuggestion(@RequestBody AiTaskSuggestionRequest request) {
        String suggestion = qwenService.getTaskSuggestion(request);
        Map<String, String> result = new HashMap<>();
        result.put("suggestion", suggestion);
        return ApiResponse.ok(result);
    }

    /** AI 任务拆解（消耗额度） */
    @PostMapping("/task-plan")
    public ApiResponse<AiTaskPlanResponse> taskPlan(@RequestBody AiTaskPlanRequest request,
                                                     HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        String role = (String) req.getAttribute("currentUserRole");
        // 管理员不限额度
        if (!"管理员".equals(role)) {
            if (!userService.consumeQuota(userId)) {
                return ApiResponse.fail("AI 使用次数不足，请联系管理员申请额度");
            }
        }
        try {
            AiTaskPlanResponse plan = qwenService.generateTaskPlan(request);
            return ApiResponse.ok("AI 拆解完成", plan);
        } catch (Exception e) {
            return ApiResponse.fail("AI 服务异常：" + e.getMessage());
        }
    }

    /** 导入 AI 拆解结果 */
    @PostMapping("/task-plan/import")
    public ApiResponse<Map<String, Object>> importPlan(@RequestBody AiTaskPlanImportRequest request,
                                                        HttpServletRequest req) {
        Long userId = AuthController.getCurrentUserId(req);
        int imported = 0;
        if (request.getItems() != null) {
            for (AiTaskPlanItem item : request.getItems()) {
                TaskInfo task = new TaskInfo();
                task.setProjectId(request.getProjectId());
                task.setTitle(item.getTitle());
                task.setDescription(item.getDescription());
                task.setPriority(item.getPriority() != null ? item.getPriority() : "中");
                task.setAssigneeId(item.getAssigneeId());
                task.setStatus("未开始");
                taskInfoService.saveTask(task, userId);
                imported++;
            }
        }
        logService.log(userId, "AI导入任务", "项目", request.getProjectId(),
                "导入 " + imported + " 个AI拆解任务");
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        return ApiResponse.ok("成功导入 " + imported + " 个任务", result);
    }
}
