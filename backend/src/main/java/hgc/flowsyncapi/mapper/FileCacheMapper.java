package hgc.flowsyncapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hgc.flowsyncapi.entity.FileCache;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileCacheMapper extends BaseMapper<FileCache> {
}
