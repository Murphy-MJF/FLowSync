package hgc.flowsyncapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hgc.flowsyncapi.entity.ProjectGithubRepo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectGithubRepoMapper extends BaseMapper<ProjectGithubRepo> {
}
