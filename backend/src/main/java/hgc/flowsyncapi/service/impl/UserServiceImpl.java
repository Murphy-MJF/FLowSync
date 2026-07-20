package hgc.flowsyncapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.entity.AiQuotaRequest;
import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.mapper.AiQuotaRequestMapper;
import hgc.flowsyncapi.mapper.UserMapper;
import hgc.flowsyncapi.service.UserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AiQuotaRequestMapper quotaMapper;

    public UserServiceImpl(UserMapper userMapper, AiQuotaRequestMapper quotaMapper) {
        this.userMapper = userMapper;
        this.quotaMapper = quotaMapper;
    }

    @Override
    public List<User> listUsers() {
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>().orderByAsc("id"));
        for (User u : users) {
            u.setPassword(null);
        }
        return users;
    }

    @Override
    public User updateProfile(Long userId, String phone, String email, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setPhone(phone);
        user.setEmail(email);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User changeRole(Long userId, String newRole) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if ("管理员".equals(user.getRole())) throw new RuntimeException("不能修改管理员角色");
        // 降级为组员时清空 AI 额度
        if ("组员".equals(newRole) && "负责人".equals(user.getRole())) {
            user.setAiQuota(0);
        }
        user.setRole(newRole);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    // ==================== AI 额度 ====================

    @Override
    public List<AiQuotaRequest> listQuotaRequests() {
        List<AiQuotaRequest> list = quotaMapper.selectList(
                new QueryWrapper<AiQuotaRequest>().orderByDesc("create_time"));
        List<User> users = userMapper.selectList(null);
        Map<Long, String> nameMap = new HashMap<>();
        for (User u : users) nameMap.put(u.getId(), u.getRealName());
        for (AiQuotaRequest r : list) {
            r.setUserName(nameMap.getOrDefault(r.getUserId(), "未知"));
            r.setHandlerName(nameMap.getOrDefault(r.getHandledBy(), "—"));
        }
        return list;
    }

    @Override
    public void requestQuota(Long userId, int amount) {
        if (amount <= 0) throw new RuntimeException("申请数量必须大于0");
        AiQuotaRequest req = new AiQuotaRequest();
        req.setUserId(userId);
        req.setRequestedAmount(amount);
        req.setStatus("pending");
        quotaMapper.insert(req);
    }

    @Override
    public void handleQuotaRequest(Long requestId, Long adminId, boolean approved, int amount) {
        AiQuotaRequest req = quotaMapper.selectById(requestId);
        if (req == null) throw new RuntimeException("申请不存在");
        if (!"pending".equals(req.getStatus())) throw new RuntimeException("申请已处理");
        req.setStatus(approved ? "approved" : "denied");
        req.setHandledBy(adminId);
        quotaMapper.updateById(req);
        if (approved && amount > 0) {
            User user = userMapper.selectById(req.getUserId());
            if (user != null) {
                int current = user.getAiQuota() != null ? user.getAiQuota() : 0;
                user.setAiQuota(current + amount);
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public User setQuota(Long userId, int quota) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setAiQuota(quota);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public boolean consumeQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        int quota = user.getAiQuota() != null ? user.getAiQuota() : 0;
        if (quota <= 0) return false;
        user.setAiQuota(quota - 1);
        userMapper.updateById(user);
        return true;
    }
}
