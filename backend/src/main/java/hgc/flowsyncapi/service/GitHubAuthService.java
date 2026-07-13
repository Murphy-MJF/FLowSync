package hgc.flowsyncapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hgc.flowsyncapi.entity.GithubAccount;
import hgc.flowsyncapi.integration.GitHubApiClient;
import hgc.flowsyncapi.mapper.GithubAccountMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GitHubAuthService {

    private final GithubAccountMapper mapper;
    private final GitHubApiClient apiClient;

    public GitHubAuthService(GithubAccountMapper mapper, GitHubApiClient apiClient) {
        this.mapper = mapper;
        this.apiClient = apiClient;
    }

    public String getAuthorizationUrl(String redirectUri) {
        String state = java.util.UUID.randomUUID().toString().substring(0, 8);
        return apiClient.getAuthorizationUrl(redirectUri, state);
    }

    public String exchangeAndSaveToken(Long userId, String code) {
        Map<String, Object> tokenResp = apiClient.exchangeCodeForToken(code);
        String accessToken = (String) tokenResp.get("access_token");
        if (accessToken == null) throw new RuntimeException("GitHub 授权失败");

        Map<String, Object> ghUser = apiClient.getUser(accessToken);
        Long ghId = Long.valueOf(ghUser.get("id").toString());
        String login = (String) ghUser.get("login");

        // 删除旧绑定
        mapper.delete(new QueryWrapper<GithubAccount>().eq("user_id", userId));

        GithubAccount account = new GithubAccount();
        account.setUserId(userId);
        account.setGithubUserId(ghId);
        account.setGithubLogin(login);
        account.setAccessToken(accessToken);
        account.setStatus("active");
        mapper.insert(account);
        return login;
    }

    public GithubAccount getByUserId(Long userId) {
        return mapper.selectOne(new QueryWrapper<GithubAccount>().eq("user_id", userId));
    }

    public void revoke(Long userId) {
        mapper.delete(new QueryWrapper<GithubAccount>().eq("user_id", userId));
    }

    public String getToken(Long userId) {
        GithubAccount account = getByUserId(userId);
        return account != null ? account.getAccessToken() : null;
    }
}
