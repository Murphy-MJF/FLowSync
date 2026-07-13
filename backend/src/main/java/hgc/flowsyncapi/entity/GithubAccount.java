package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("github_account")
public class GithubAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long githubUserId;
    private String githubLogin;
    private Long installationId;
    private String accessToken;
    private String status;  // active / revoked

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
