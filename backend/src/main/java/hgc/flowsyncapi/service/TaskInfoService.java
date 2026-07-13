package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hgc.flowsyncapi.entity.TaskInfo;
import hgc.flowsyncapi.mapper.TaskInfoMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class TaskInfoService {
    @Resource
    private TaskInfoMapper taskInfoMapper;

    public List<TaskInfo> listTasks(Long projectId) {
        LambdaQueryWrapper<TaskInfo> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(TaskInfo::getProjectId, projectId);
        }
        wrapper.orderByDesc(TaskInfo::getId);
        return taskInfoMapper.selectList(wrapper);
    }

    public TaskInfo getTaskById(Long id) {
        return taskInfoMapper.selectById(id);
    }

    public TaskInfo saveTask(TaskInfo task) {
        if (task.getId() == null) {
            taskInfoMapper.insert(task);
        } else {
            taskInfoMapper.updateById(task);
        }
        return task;
    }

    public void deleteTask(Long id) {
        taskInfoMapper.deleteById(id);
    }
}
