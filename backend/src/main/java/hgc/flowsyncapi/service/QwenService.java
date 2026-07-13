package hgc.flowsyncapi.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hgc.flowsyncapi.dto.*;
import hgc.flowsyncapi.entity.User;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QwenService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.model}")
    private String model;

    @Resource
    private UserService userService;

    @Resource
    private TaskInfoService taskInfoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiTaskSuggestionResponse getTaskSuggestion(AiTaskSuggestionRequest request) {
        String systemPrompt = "你是一个简单直接的项目任务助手。请用最容易理解的中文输出，给出：1. 建议拆分的子任务；2. 执行顺序；3. 风险提醒。控制在300字以内。";
        String userPrompt = String.format("项目名称：%s\n任务标题：%s\n任务说明：%s",
                request.getProjectName(), request.getTitle(), request.getDescription());

        String result = callQwen(systemPrompt, userPrompt);
        AiTaskSuggestionResponse response = new AiTaskSuggestionResponse();
        response.setSuggestion(result != null ? result : "无法获取 AI 建议，请稍后重试。");
        return response;
    }

    public AiTaskPlanResponse getTaskPlan(AiTaskPlanRequest request) {
        List<User> users = userService.listUsers();
        String memberList = users.stream()
                .map(u -> u.getId() + " - " + u.getRealName())
                .collect(Collectors.joining("\n"));

        String systemPrompt = "你是一个项目任务拆解助手。请把大任务拆成可以直接执行的小任务。我会给你可选的成员名单，请为每个任务推荐一个最合适的负责人，在assigneeId字段填写该成员的id（必须是名单中已有的id）。重要：每个任务都必须填写assigneeId，不能为空；同一个人可以负责多个任务。只返回严格JSON，不要解释，不要markdown。";
        String userPrompt = String.format("项目名称：%s\n任务目标：%s\n补充说明：%s\n可选成员名单（id - 姓名）：\n%s",
                request.getProjectName(), request.getGoal(),
                request.getDescription() != null ? request.getDescription() : "", memberList);

        String result = callQwen(systemPrompt, userPrompt);
        if (result != null) {
            try {
                AiTaskPlanResponse plan = objectMapper.readValue(result, AiTaskPlanResponse.class);
                if (plan.getItems() != null && !plan.getItems().isEmpty()) {
                    Set<Long> validIds = users.stream().map(User::getId).collect(Collectors.toSet());
                    Long fallbackId = users.isEmpty() ? null : users.get(0).getId();
                    for (AiTaskPlanItem item : plan.getItems()) {
                        if (item.getAssigneeId() == null || !validIds.contains(item.getAssigneeId())) {
                            item.setAssigneeId(fallbackId);
                        }
                    }
                }
                return plan;
            } catch (Exception e) {
                // JSON parse failed, fall through to fallback
            }
        }
        return buildFallbackPlan();
    }

    public void importTaskPlan(AiTaskPlanImportRequest request) {
        if (request.getItems() != null) {
            for (AiTaskPlanItem item : request.getItems()) {
                hgc.flowsyncapi.entity.TaskInfo task = new hgc.flowsyncapi.entity.TaskInfo();
                task.setProjectId(request.getProjectId());
                task.setTitle(item.getTitle());
                task.setDescription(item.getDescription());
                task.setPriority(item.getPriority());
                task.setAssigneeId(item.getAssigneeId());
                task.setCreatorId(request.getCreatorId());
                task.setStatus("未开始");
                taskInfoService.saveTask(task);
            }
        }
    }

    private AiTaskPlanResponse buildFallbackPlan() {
        AiTaskPlanResponse fallback = new AiTaskPlanResponse();
        fallback.setSummary("系统自动生成的兜底任务方案，请根据需要调整后导入。");
        List<AiTaskPlanItem> items = new ArrayList<>();
        String[][] defaults = {{"准备资料", "收集项目所需的各类资料和信息"}, {"执行主体", "按计划执行项目主体工作"}, {"检查总结", "对已完成的工作进行检查和总结"}};
        for (String[] d : defaults) {
            AiTaskPlanItem item = new AiTaskPlanItem();
            item.setTitle(d[0]);
            item.setDescription(d[1]);
            item.setPriority("中");
            item.setSuggestedDays(3);
            items.add(item);
        }
        fallback.setItems(items);
        return fallback;
    }

    private String callQwen(String systemPrompt, String userPrompt) {
        try {
            Message sysMsg = Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build();
            Message userMsg = Message.builder().role(Role.USER.getValue()).content(userPrompt).build();
            Generation gen = new Generation();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(Arrays.asList(sysMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            return null;
        }
    }
}
