package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hgc.flowsyncapi.entity.TaskLog;
import hgc.flowsyncapi.mapper.TaskLogMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TaskLogService {
    @Resource
    private TaskLogMapper taskLogMapper;

    public List<TaskLog> listTaskLogs(Long taskId) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(TaskLog::getTaskId, taskId);
        }
        wrapper.orderByDesc(TaskLog::getId);
        return taskLogMapper.selectList(wrapper);
    }

    public TaskLog saveTaskLog(TaskLog taskLog) {
        taskLogMapper.insert(taskLog);
        return taskLog;
    }
}
