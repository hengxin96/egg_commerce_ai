# 龙蛋 AI 前台用户工作台

这是独立于 `egg-ui` 后台管理端的 Vue 3 + Element Plus 前台项目。它用于承载 Picset 风格的用户工作台：商品图生成、风格复刻、服装组图、批量精修、智能聊天/RAG、商品库、素材库、任务中心和后续 A2A。

## 为什么单独建前台项目

- 前台需要高频创作、图片预览、任务状态和素材操作，交互节奏不同于 Egg 后台管理端。
- 后台 `egg-ui` 继续保持 Egg 风格，用于模型、Prompt、RAG、规则、日志等配置。
- 前后台分离后，前台可以升级到 Vue 3 / Element Plus，不影响后台稳定性。

## 运行

```powershell
cd D:\Code_Workspace\egg_commerce_ai\egg-user-web
npm.cmd install
npm.cmd run dev
```

默认地址：

```text
http://localhost:5177
```

## 后端代理

开发环境中 `/api` 会代理到：

```text
VITE_API_PROXY_TARGET=http://localhost:8080
```

当前页面使用 mock 数据展示第一版产品体验。后续接入 Java 后端时，保持 `/api/products`、`/api/assets`、`/api/agent-tasks`、`/api/chat`、`/api/knowledge-bases` 等路径即可。
