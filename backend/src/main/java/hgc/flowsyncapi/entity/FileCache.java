package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_cache")
public class FileCache {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String filePath;
    private String content;       // base64 encoded
    private String originalSha;
    private String branch;
    private Long submittedBy;
    private String status;        // pending / approved / rejected
    private String message;       // commit message
    private Long reviewedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 非数据库字段
    @TableField(exist = false)
    private String submitterName;
}
