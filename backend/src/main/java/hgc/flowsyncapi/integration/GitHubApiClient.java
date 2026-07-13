package hgc.flowsyncapi.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
public class GitHubApiClient {

    private static final String GITHUB_API = "https://api.github.com";
    private final RestTemplate rest = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
        } catch (Exception ignored) {}

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    @Value("${github.app-id:}")
    private String appId;

    @Value("${github.client-id:}")
    private String clientId;

    @Value("${github.client-secret:}")
    private String clientSecret;

    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
            && clientSecret != null && !clientSecret.isEmpty();
    }

    // ---- OAuth ----

    public String getAuthorizationUrl(String redirectUri, String state) {
        return "https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri + "&state=" + state + "&scope=repo";
    }

    public Map<String, Object> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code
        );
        HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = rest.postForEntity(
                "https://github.com/login/oauth/access_token", req, Map.class);
        return resp.getBody();
    }

    // ---- User ----

    public Map<String, Object> getUser(String token) {
        return callApi(token, "/user");
    }

    // ---- Repositories ----

    public List<Map<String, Object>> listRepositories(String token) {
        ResponseEntity<List> resp = rest.exchange(
                GITHUB_API + "/user/repos?sort=updated&per_page=50",
                HttpMethod.GET, authRequest(token), List.class);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    public Map<String, Object> getRepository(String token, String owner, String repo) {
        return callApi(token, "/repos/" + owner + "/" + repo);
    }

    // ---- Branches ----

    public List<Map<String, Object>> listBranches(String token, String owner, String repo) {
        ResponseEntity<List> resp = rest.exchange(
                GITHUB_API + "/repos/" + owner + "/" + repo + "/branches",
                HttpMethod.GET, authRequest(token), List.class);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    // ---- Commits ----

    public List<Map<String, Object>> listCommits(String token, String owner, String repo, String branch) {
        String url = GITHUB_API + "/repos/" + owner + "/" + repo + "/commits?sha=" + (branch != null ? branch : "main");
        ResponseEntity<List> resp = rest.exchange(url, HttpMethod.GET, authRequest(token), List.class);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    // ---- Issues ----

    public List<Map<String, Object>> listIssues(String token, String owner, String repo) {
        ResponseEntity<List> resp = rest.exchange(
                GITHUB_API + "/repos/" + owner + "/" + repo + "/issues?state=all&per_page=20",
                HttpMethod.GET, authRequest(token), List.class);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    // ---- Pull Requests ----

    public List<Map<String, Object>> listPullRequests(String token, String owner, String repo) {
        ResponseEntity<List> resp = rest.exchange(
                GITHUB_API + "/repos/" + owner + "/" + repo + "/pulls?state=all&per_page=20",
                HttpMethod.GET, authRequest(token), List.class);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    // ---- Helpers ----

    private Map<String, Object> callApi(String token, String path) {
        ResponseEntity<Map> resp = rest.exchange(
                GITHUB_API + path, HttpMethod.GET, authRequest(token), Map.class);
        return resp.getBody();
    }

    private HttpEntity<Void> authRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
