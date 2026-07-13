package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hgc.flowsyncapi.entity.ProjectInfo;
import hgc.flowsyncapi.mapper.ProjectInfoMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ProjectInfoService {
    @Resource
    private ProjectInfoMapper projectInfoMapper;

    public List<ProjectInfo> listProjects() {
        return projectInfoMapper.selectList(
            new LambdaQueryWrapper<ProjectInfo>().orderByDesc(ProjectInfo::getId)
        );
    }

    public ProjectInfo getProjectById(Long id) {
        return projectInfoMapper.selectById(id);
    }

    public ProjectInfo saveProject(ProjectInfo project) {
        if (project.getId() == null) {
            projectInfoMapper.insert(project);
        } else {
            projectInfoMapper.updateById(project);
        }
        return project;
    }

    public void deleteProject(Long id) {
        projectInfoMapper.deleteById(id);
    }
}
