package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_github_repository")
public class ProjectGithubRepo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long repoId;
    private String owner;
    private String repoName;
    private String defaultBranch;
    private String syncStatus;  // active / error
    private String repoUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
