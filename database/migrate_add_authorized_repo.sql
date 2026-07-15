-- 增量迁移：新增 GitHub 授权仓库表（不清除已有数据）
-- 执行方式：mysql -u root -proot flowsync_simple < database/migrate_add_authorized_repo.sql

CREATE TABLE IF NOT EXISTS github_authorized_repo (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    user_id         BIGINT       NOT NULL                 COMMENT 'FlowSync 用户 ID',
    owner           VARCHAR(100) NOT NULL                 COMMENT '仓库所有者',
    repo_name       VARCHAR(100) NOT NULL                 COMMENT '仓库名',
    repo_full_name  VARCHAR(200) NOT NULL                 COMMENT 'owner/repo',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_repo (user_id, repo_full_name),
    CONSTRAINT fk_authrepo_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GitHub 授权仓库表';

-- 自动授权：将已绑定的仓库加入授权列表
INSERT IGNORE INTO github_authorized_repo (user_id, owner, repo_name, repo_full_name)
    SELECT ga.user_id, pgr.owner, pgr.repo_name, CONCAT(pgr.owner, '/', pgr.repo_name)
    FROM project_github_repository pgr
    JOIN github_account ga ON ga.github_login = pgr.owner;
