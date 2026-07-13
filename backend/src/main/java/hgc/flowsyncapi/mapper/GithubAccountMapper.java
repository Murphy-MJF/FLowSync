package hgc.flowsyncapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hgc.flowsyncapi.entity.GithubAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GithubAccountMapper extends BaseMapper<GithubAccount> {
}
