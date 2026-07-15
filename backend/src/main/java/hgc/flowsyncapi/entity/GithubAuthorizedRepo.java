package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("github_authorized_repo")
public class GithubAuthorizedRepo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;          // FlowSync 用户 ID（GitHub 账号绑定者）
    private String owner;          // GitHub owner
    private String repoName;       // 仓库名
    private String repoFullName;   // owner/repo

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
