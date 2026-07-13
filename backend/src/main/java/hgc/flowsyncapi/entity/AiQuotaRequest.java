package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_quota_request")
public class AiQuotaRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer requestedAmount;
    private String status;        // pending / approved / denied
    private Long handledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 非数据库字段
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String handlerName;
}
