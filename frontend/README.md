# FlowSync 前端

Vue 3 + Element Plus + Axios

## 目录结构

```
frontend/
├── package.json               # npm 依赖
├── vue.config.js              # devServer 代理 + LAN/ngrok 配置
├── public/index.html
└── src/
    ├── main.js                # 入口（Vue3 + ElementPlus + Router）
    ├── App.vue                # 根组件
    ├── router/index.js        # SPA 路由 → HomeView
    ├── api/index.js           # Axios + JWT Bearer 拦截器 + 401 处理 + 全部 API
    ├── views/
    │   └── HomeView.vue       # 主容器：登录/注册 + 侧边菜单 + 面板动态切换
    └── components/
        ├── DashboardPanel.vue    # 仪表盘 — 4 统计卡片
        ├── ProjectPanel.vue      # 项目管理 — 表格 + CRUD 弹窗 + 多选批量删除
        ├── AiTaskPlanPanel.vue   # AI 任务拆解 — 选项目→填目标→AI拆解→调整→导入
        ├── TaskPanel.vue         # 任务管理 — 表格 + CRUD + 项目筛选 + 多选批量删除
        ├── TaskLogPanel.vue      # 进度更新 — 进度条 + 滑块 + 新增弹窗
        ├── SummaryPanel.vue      # 总结管理 — 列表 + 新增弹窗
        ├── MemberListPanel.vue   # 成员列表 — 含注册时间
        ├── AdminPanel.vue        # 系统管理 — 邀请码 + 角色升降级 + 项目转让弹窗
        └── ProfilePanel.vue      # 个人信息 — 浮窗编辑电话/邮箱/密码
```

## 核心依赖

- `vue` 3.x + `vue-router` 4.x
- `element-plus` — UI 组件库
- `axios` — HTTP 客户端（Bearer Token 拦截器）

## 关键配置

`vue.config.js`：

```js
devServer: {
  host: '0.0.0.0',           // LAN/ngrok 访问
  port: 8081,
  allowedHosts: 'all',
  client: {
    webSocketURL: 'auto://0.0.0.0:0/ws'  // 自动适配 ws/wss
  },
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

## 认证流程

```
登录/注册 → 后端返回 {token, user}
  → sessionStorage 存 token + user
  → Axios 拦截器自动附加 Authorization: Bearer <token>
  → 后端 JwtInterceptor 解析 → 注入 userId + role
  → 401 自动清除存储 + 跳转登录页
```

## 权限控制

| 角色 | 可见菜单 |
|------|----------|
| 管理员 | 全部菜单 + 系统管理 |
| 负责人 | 全部业务菜单 + AI + CRUD 按钮 |
| 组员 | 控制台/项目/任务/进度/总结/成员/个人信息 |

## 启动

```powershell
cd frontend
npm install
npm run serve
# → http://localhost:8081
```
