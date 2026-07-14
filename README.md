# FlowSync - 小组任务协同管理系统

## 技术栈

- **前端**: Vue 3 + Vue Router 4 + Element Plus + Axios (端口 8081)
- **后端**: Spring Boot 3.3.5 + MyBatis-Plus 3.5.8 (端口 8080)
- **数据库**: MySQL 8.x (端口 3306)
- **AI**: 阿里云千问 DashScope SDK (qwen-plus)

## 快速启动

### 前置条件

- JDK 17+
- Node.js 16+
- MySQL 8.x（本地已安装并运行，密码 root）

### 1. 数据库

数据库 `flowsync_simple` 已创建，包含 5 张业务表和测试数据：
- 负责人账号: `leader` / `123456`
- 成员账号: `member1` / `123456`、`member2` / `123456`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 http://localhost:8080

### 3. 启动前端

```bash
cd frontend
npm install
npm run serve
```

前端访问 http://localhost:8081，默认自动代理 /api 请求到后端。

### 4. AI 配置（可选）

在 `application.yml` 中配置阿里云千问 API Key：

```yaml
dashscope:
  api-key: sk-你的api-key
```

不配置也不影响系统基本功能，AI 拆解会降级为兜底方案。

## 项目结构

```
FlowSync/
├── backend/                   # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/hgc/flowsyncapi/
│       ├── common/            # ApiResponse, CORS 配置
│       ├── entity/            # 5 个数据库实体
│       ├── mapper/            # MyBatis-Plus Mapper
│       ├── service/           # 业务逻辑 + AI 服务
│       ├── controller/        # REST 控制器
│       └── dto/               # 数据传输对象
├── frontend/                  # Vue 3 前端
│   ├── package.json
│   ├── vue.config.js
│   └── src/
│       ├── api/               # Axios + API 封装
│       ├── router/            # 路由配置
│       └── views/             # 页面组件
└── README.md
```

## 功能模块

| 功能 | 说明 | 负责人 | 成员 |
|------|------|--------|------|
| 总览 | 统计信息看板 | ✓ | ✓ |
| 项目管理 | 项目 CRUD | ✓ | 仅查看 |
| AI 任务拆解 | 千问大模型自动拆解任务 | ✓ | - |
| 任务管理 | 任务分配与状态更新 | ✓ | 仅更新自己任务状态 |
| 进度跟踪 | 进度记录 | ✓ | ✓ |
| 总结中心 | 阶段/最终总结 | ✓ | ✓ |
| 成员列表 | 查看成员信息 | ✓ | ✓ |
| 个人信息 | 查看资料/修改密码 | ✓ | ✓ |

## 扩展空间（教学用途）

详见需求规格说明书第 9 节，包括：后端权限校验、JWT 认证、密码加密、级联删除、分页等。


123456789change
123456