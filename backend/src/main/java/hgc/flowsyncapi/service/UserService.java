package hgc.flowsyncapi.service;

import hgc.flowsyncapi.entity.User;
import java.util.List;

import hgc.flowsyncapi.entity.AiQuotaRequest;

public interface UserService {
    List<User> listUsers();
    User updateProfile(Long userId, String phone, String email, String avatar);
    User changeRole(Long userId, String newRole);

    // AI 额度
    List<AiQuotaRequest> listQuotaRequests();
    void requestQuota(Long userId, int amount);
    void handleQuotaRequest(Long requestId, Long adminId, boolean approved, int amount);
    User setQuota(Long userId, int quota);
    boolean consumeQuota(Long userId);
}
