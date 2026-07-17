-- 增量迁移：新增文件审核缓存表
-- 执行方式：mysql -u root -proot flowsync_simple < database/migrate_add_file_cache.sql

CREATE TABLE IF NOT EXISTS file_cache (
    id              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
    project_id      BIGINT        NOT NULL                 COMMENT '所属项目 ID',
    file_path       VARCHAR(500)  NOT NULL                 COMMENT '文件路径',
    content         MEDIUMTEXT    NOT NULL                 COMMENT '文件内容（base64）',
    original_sha    VARCHAR(100)  DEFAULT NULL             COMMENT '原始文件 SHA',
    branch          VARCHAR(100)  DEFAULT 'main'           COMMENT '目标分支',
    submitted_by    BIGINT        NOT NULL                 COMMENT '提交人 ID',
    status          VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    message         VARCHAR(500)  DEFAULT NULL             COMMENT '提交信息',
    reviewed_by     BIGINT        DEFAULT NULL             COMMENT '审核人 ID',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_cache_project  FOREIGN KEY (project_id)  REFERENCES project_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_cache_submitter FOREIGN KEY (submitted_by) REFERENCES sys_user(id),
    CONSTRAINT fk_cache_reviewer  FOREIGN KEY (reviewed_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件审核缓存表';
