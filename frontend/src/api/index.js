import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const userStr = sessionStorage.getItem("currentUser");
  if (userStr) {
    const user = JSON.parse(userStr);
    config.params = { ...config.params, currentUserId: user.id };
  }
  return config;
});

request.interceptors.response.use(
  (res) => {
    if (res.data && !res.data.success) {
      ElMessage.error(res.data.message || "请求失败");
      return Promise.reject(new Error(res.data.message));
    }
    return res.data;
  },
  () => {
    ElMessage.error("网络错误，请检查后端服务是否启动");
    return Promise.reject(new Error("network error"));
  }
);

export default request;

export const authApi = {
  login(data) { return request.post("/auth/login", data); },
};

export const projectApi = {
  list() { return request.get("/projects"); },
  save(data) { return request.post("/projects", data); },
  delete(id) { return request.delete("/projects/" + id); },
};

export const taskApi = {
  list(projectId) { return request.get("/tasks", { params: { projectId } }); },
  save(data) { return request.post("/tasks", data); },
  delete(id) { return request.delete("/tasks/" + id); },
};

export const taskLogApi = {
  list(taskId) { return request.get("/task-logs", { params: { taskId } }); },
  save(data) { return request.post("/task-logs", data); },
};

export const summaryApi = {
  list() { return request.get("/summaries"); },
  save(data) { return request.post("/summaries", data); },
};

export const overviewApi = {
  get() { return request.get("/overview"); },
};

export const userApi = {
  list() { return request.get("/users"); },
  updatePassword(data) { return request.post("/users/update-password", data); },
};

export const aiApi = {
  taskSuggestion(data) { return request.post("/ai/task-suggestion", data); },
  taskPlan(data) { return request.post("/ai/task-plan", data); },
  importPlan(data) { return request.post("/ai/task-plan/import", data); },
};
