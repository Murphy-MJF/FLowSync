# FlowSync 后端

Spring Boot 3.3.5 + MyBatis-Plus 3.5.8 + MySQL 8.x

## 目录结构

```
backend/
├── pom.xml                                    # Maven 配置（Lombok 1.18.38, jjwt 0.12.6, BCrypt）
├── mvnw.cmd                                   # Maven Wrapper（无需预装 Maven）
├── .mvn/wrapper/                              # Wrapper JAR + 配置
├── .env.example                                # DeepSeek API Key 模板
├── .env                                        # 实际 Key（.gitignore 排除）
└── src/main/
    ├── resources/
    │   ├── application.yml                     # 数据源 + DeepSeek 配置（.gitignore 排除）
    │   ├── application.example.yml              # 配置模板（可提交）
    │   └── init-h2.sql                          # H2 内嵌数据库初始化
    └── java/hgc/flowsyncapi/
        ├── FlowsyncApiApplication.java          # 启动类
        ├── config/
        │   ├── CorsConfig.java                 # 跨域
        │   ├── OpenApiConfig.java              # SpringDoc
        │   ├── PasswordConfig.java             # BCrypt Bean
        │   ├── JwtInterceptor.java             # JWT 拦截器
        │   ├── WebMvcConfig.java               # 拦截器注册
        │   ├── MybatisPlusConfig.java          # 分页插件
        │   └── MetaObjectHandlerConfig.java    # createTime 自动填充
        ├── common/
        │   ├── ApiResponse.java                # 统一响应 {success, message, data}
        │   └── JwtUtils.java                   # JWT 生成/解析/验证
        ├── controller/
        │   ├── AuthController.java             # 登录/注册
        │   ├── ProjectController.java          # 项目 CRUD + 批量删除
        │   ├── TaskController.java             # 任务 CRUD + 批量删除 + 数据隔离
        │   ├── TaskLogController.java          # 进度记录
        │   ├── TaskSummaryController.java      # 总结管理
        │   ├── OverviewController.java         # 仪表盘统计
        │   ├── UserController.java             # 用户列表 + 修改密码/资料
        │   ├── AdminController.java            # 邀请码 + 角色升降级 + 项目转让
        │   └── AiController.java               # AI 建议 + 拆解 + 导入
        ├── service/
        │   ├── AuthService.java                # 登录(BCrypt)/注册(邀请码验证)/改密
        │   ├── ProjectInfoService.java         # CRUD + 权限校验 + 项目转让
        │   ├── TaskInfoService.java            # CRUD + 状态更新
        │   ├── TaskLogService.java             # CRUD
        │   ├── TaskSummaryService.java         # CRUD
        │   ├── OverviewService.java            # 统计
        │   ├── UserService.java                # 用户列表 + 改资料 + 改角色
        │   ├── InviteCodeService.java          # 邀请码生成/验证
        │   ├── OperationLogService.java        # 操作日志
        │   ├── QwenService.java                # DeepSeek AI
        │   └── impl/                           # 10 个实现类
        ├── mapper/                             # 7 个 MyBatis-Plus Mapper
        ├── entity/                             # 7 个实体类
        └── dto/                                # 9 个 DTO
```

## 核心依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-web | 3.3.5 | Web 框架 |
| mybatis-plus-spring-boot3-starter | 3.5.8 | ORM + 分页 |
| mysql-connector-j | — | MySQL 驱动 |
| spring-security-crypto | — | BCrypt 密码加密 |
| jjwt-api/impl/jackson | 0.12.6 | JWT Token |
| springdoc-openapi | 2.1.0 | API 文档 |
| lombok | 1.18.38 | 代码简化（JDK 24 兼容） |

## API 接口

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 认证 | `/api/auth/login` | POST | 登录 → `{token, user}` |
| 认证 | `/api/auth/register` | POST | 注册（负责人需邀请码） |
| 项目 | `/api/projects` | GET/POST | 列表(数据隔离)/新建/编辑 |
| 项目 | `/api/projects/{id}` | DELETE | 删除（级联） |
| 项目 | `/api/projects/batch-delete` | POST | 批量删除 |
| 任务 | `/api/tasks` | GET/POST | 列表(管理员全透明)/新建/编辑 |
| 任务 | `/api/tasks/{id}/status` | POST | 更新状态 |
| 任务 | `/api/tasks/{id}` | DELETE | 删除 |
| 任务 | `/api/tasks/batch-delete` | POST | 批量删除 |
| 进度 | `/api/task-logs` | GET/POST | 列表/新增 |
| 总结 | `/api/summaries` | GET/POST | 列表/新增 |
| 概览 | `/api/overview` | GET | 统计 |
| 用户 | `/api/users` | GET | 用户列表 |
| 用户 | `/api/users/update-password` | POST | 修改密码 |
| 用户 | `/api/users/update-profile` | POST | 修改电话/邮箱 |
| 管理 | `/api/admin/invite-code` | POST | 生成邀请码(2min) |
| 管理 | `/api/admin/change-role` | POST | 升降级(降级自动处理项目转让) |
| 管理 | `/api/admin/users` | GET | 管理视图用户列表 |
| 管理 | `/api/admin/transfer-candidates` | GET | 项目接手人候选 |
| AI | `/api/ai/task-suggestion` | POST | 单任务建议 |
| AI | `/api/ai/task-plan` | POST | 任务拆解 |
| AI | `/api/ai/task-plan/import` | POST | 导入拆解结果 |

## 启动

```powershell
cd backend
# 配置 application.yml（复制 application.example.yml → application.yml，填数据库密码）
# 配置 .env（复制 .env.example → .env，填 DeepSeek API Key）
java -Xmx1024m -classpath ".mvn/wrapper/maven-wrapper.jar" "-Dmaven.home=$PWD" "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
```
