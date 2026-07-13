# FlowSync 数据库

MySQL 8.x — 数据库名 `flowsync_simple`，字符集 `utf8mb4`

## 文件

- `init.sql` — 完整的 DDL + 预置数据（含 BCrypt 密码哈希）

## 表结构（7 张）

| 表名 | 说明 | 关键 FK |
|------|------|---------|
| `sys_user` | 用户表 | — |
| `project_info` | 项目表 | owner_id → sys_user |
| `task_info` | 任务表 | project_id → project_info (CASCADE), assignee_id/creator_id → sys_user |
| `task_log` | 进度记录表 | task_id → task_info (CASCADE) |
| `task_summary` | 总结表 | project_id → project_info (CASCADE), task_id → task_info (SET NULL) |
| `operation_log` | 操作日志表 | operator_id → sys_user (SET NULL) |
| `invite_code` | 邀请码表 | created_by → sys_user (SET NULL) |

## 预置用户

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 123456 | 管理员 | 最高权限 |
| leader | 123456 | 负责人 | 项目管理 |
| member1 | 123456 | 组员 | 普通成员 |
| member2 | 123456 | 组员 | 普通成员 |

> 密码使用 BCrypt 哈希存储，`init.sql` 中为预生成的哈希值。

## 执行

```cmd
:: CMD（非 PowerShell）
mysql -u root -p -e "DROP DATABASE IF EXISTS flowsync_simple;"
mysql -u root -p < database\init.sql

:: 验证
mysql -u root -p -e "USE flowsync_simple; SHOW TABLES;"
```

## 外键策略

- 项目 → 任务 → 进度/总结：`ON DELETE CASCADE`（级联删除）
- 总结 ↔ 任务：`ON DELETE SET NULL`（任务删除时总结保留但脱钩）
- 日志/邀请码 ↔ 用户：`ON DELETE SET NULL`（用户删除时保留记录）
