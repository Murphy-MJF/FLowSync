package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hgc.flowsyncapi.entity.TaskSummary;
import hgc.flowsyncapi.mapper.TaskSummaryMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TaskSummaryService {
    @Resource
    private TaskSummaryMapper taskSummaryMapper;

    public List<TaskSummary> listSummaries() {
        return taskSummaryMapper.selectList(
            new LambdaQueryWrapper<TaskSummary>().orderByDesc(TaskSummary::getId)
        );
    }

    public TaskSummary saveSummary(TaskSummary summary) {
        taskSummaryMapper.insert(summary);
        return summary;
    }
}
