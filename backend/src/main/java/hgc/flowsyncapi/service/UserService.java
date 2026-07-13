package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.mapper.UserMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    public List<User> listUsers() {
        return userMapper.selectList(null);
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
