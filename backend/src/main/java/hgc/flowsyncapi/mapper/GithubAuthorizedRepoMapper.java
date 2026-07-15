package hgc.flowsyncapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hgc.flowsyncapi.entity.GithubAuthorizedRepo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GithubAuthorizedRepoMapper extends BaseMapper<GithubAuthorizedRepo> {
}
