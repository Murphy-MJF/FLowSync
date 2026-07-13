import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";

const routes = [
  { path: "/login", name: "Login", component: LoginView },
  {
    path: "/",
    component: HomeView,
    redirect: "/overview",
    children: [
      { path: "overview", name: "Overview", component: () => import("../views/OverviewView.vue") },
      { path: "projects", name: "Projects", component: () => import("../views/ProjectView.vue") },
      { path: "task-decompose", name: "TaskDecompose", component: () => import("../views/TaskDecomposeView.vue") },
      { path: "tasks", name: "Tasks", component: () => import("../views/TaskView.vue") },
      { path: "progress", name: "Progress", component: () => import("../views/ProgressView.vue") },
      { path: "summaries", name: "Summaries", component: () => import("../views/SummaryView.vue") },
      { path: "members", name: "Members", component: () => import("../views/MemberView.vue") },
      { path: "profile", name: "Profile", component: () => import("../views/ProfileView.vue") },
    ],
  },
];

const router = createRouter({ history: createWebHistory(), routes });

router.beforeEach((to, from, next) => {
  const user = sessionStorage.getItem("currentUser");
  if (to.path !== "/login" && !user) {
    next("/login");
  } else {
    next();
  }
});

export default router;
