package hgc.flowsyncapi.service;

import hgc.flowsyncapi.entity.User;
import hgc.flowsyncapi.mapper.UserMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

@Service
public class AuthService {
    @Resource
    private UserMapper userMapper;

    public User login(String username, String password) {
        User user = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password)
        );
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null || !user.getPassword().equals(oldPassword)) {
            return false;
        }
        user.setPassword(newPassword);
        userMapper.updateById(user);
        return true;
    }
}
