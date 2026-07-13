package hgc.flowsyncapi.service;

import hgc.flowsyncapi.mapper.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class OverviewService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private ProjectInfoMapper projectInfoMapper;
    @Resource
    private TaskInfoMapper taskInfoMapper;
    @Resource
    private TaskSummaryMapper taskSummaryMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("projectCount", projectInfoMapper.selectCount(null));
        stats.put("taskCount", taskInfoMapper.selectCount(null));
        stats.put("summaryCount", taskSummaryMapper.selectCount(null));
        return stats;
    }
}
