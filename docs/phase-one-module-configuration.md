# Phase-One Business Module Configuration

本文说明第一版必须配置的业务模块、为什么需要配置、在哪里配置、如何启动。

## 1. PostgreSQL + pgvector

为什么需要：

- 保存商品、素材、任务、聊天、Prompt、模型调用日志、审核报告等业务数据。
- pgvector 用于 RAG 知识库向量检索。
- 第一版保持模块化单体，不拆微服务，PostgreSQL 是主业务数据库。

在哪里配置：

- Docker: `compose.postgres.yml`
- 后端数据库: `egg-admin/src/main/resources/application-druid.yml`
- 环境变量: `.env.example`

关键配置：

```text
EGG_DATASOURCE_URL=jdbc:postgresql://localhost:5432/egg
EGG_DATASOURCE_USERNAME=egg
EGG_DATASOURCE_PASSWORD=egg_dev_password
EGG_POSTGRES_PORT=5432
```

## 2. Redis

为什么需要：

- 登录缓存、Token 状态和验证码。
- AI 任务队列和任务进度。
- 生成任务限流、防重复提交、短期聊天上下文。
- 任务最终结果仍必须落 PostgreSQL，Redis 只保存短期状态。

在哪里配置：

- Docker: `compose.postgres.yml`
- 后端 Redis: `egg-admin/src/main/resources/application.yml`
- AI 任务键名: `egg.ai.task.*`

本地默认使用国内镜像前缀，避免 Docker Hub 直连失败：

```text
EGG_REDIS_IMAGE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/redis/redis-stack-server:7.4.0-v8
```

关键配置：

```text
EGG_REDIS_HOST=localhost
EGG_REDIS_PORT=6381
EGG_REDIS_DATABASE=0
EGG_AI_TASK_QUEUE_KEY=ai:task:queue
EGG_AI_DUPLICATE_LOCK_SECONDS=600
```

## 3. MinIO

为什么需要：

- 保存用户上传商品原图、参考图、AI 生成图、编辑图、导出 ZIP/Excel/Markdown。
- AI 模型返回的临时 URL 不能作为长期素材地址，必须转存对象存储。
- 前端不得接触 MinIO AccessKey/SecretKey，只能使用后端签名 URL。

在哪里配置：

- Docker: `compose.postgres.yml`
- 后端对象存储: `egg-admin/src/main/resources/application.yml`
- 环境变量: `.env.example`

本地默认使用国内镜像前缀，避免 Docker Hub 直连失败：

```text
EGG_MINIO_IMAGE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:latest
```

关键配置：

```text
EGG_MINIO_ENDPOINT=http://localhost:9000
EGG_MINIO_PUBLIC_ENDPOINT=http://localhost:9000
EGG_MINIO_BUCKET=egg-commerce-assets
EGG_MINIO_ROOT_USER=eggminio
EGG_MINIO_ROOT_PASSWORD=egg_minio_dev_password
```

## 4. RAG

为什么需要：

- 智能聊天必须检索平台规则、品牌规则、审核规则、商品资料和电商运营知识。
- 审核模块也需要可追踪的规则来源。
- 回答和生成任务必须记录 retrieval sources。

在哪里配置：

- 后端: `egg-admin/src/main/resources/application.yml`
- 配置前缀: `egg.ai.rag`

关键配置：

```text
EGG_RAG_ENABLED=true
EGG_RAG_CHUNK_SIZE=800
EGG_RAG_CHUNK_OVERLAP=120
EGG_RAG_TOP_K=6
EGG_RAG_EMBEDDING_DIMENSION=1536
```

## 5. Model Gateway

为什么需要：

- 统一接入文本模型、视觉理解模型、图片生成模型、Embedding、Coze 和后续 A2A。
- 统一处理重试、超时、模型调用日志、成本估算、失败原因和密钥隔离。
- 业务模块不直接调用外部模型，避免调用逻辑散落。

在哪里配置：

- 后端: `egg-admin/src/main/resources/application.yml`
- 配置前缀: `egg.ai.model-gateway`

本地开发默认：

```text
EGG_AI_MOCK_ENABLED=true
EGG_MODEL_TEXT_PROVIDER=mock
EGG_MODEL_VISION_PROVIDER=mock
EGG_MODEL_IMAGE_PROVIDER=mock
EGG_MODEL_EMBEDDING_PROVIDER=mock
```

生产要求：

```text
EGG_AI_MOCK_ENABLED=false
EGG_MODEL_IMAGE_PROVIDER=<real-provider>
EGG_MODEL_IMAGE_ENDPOINT=<provider-endpoint>
EGG_MODEL_IMAGE_API_KEY=<secret>
```

## 6. Real Image Generation

为什么需要：

- 项目宪法要求第一版必须真实出图，不能只输出 Prompt。
- 生成图必须转存 MinIO，进入素材库，记录模型、Prompt、参数、任务和审核状态。

在哪里配置：

- 后端: `egg-admin/src/main/resources/application.yml`
- 配置前缀: `egg.ai.real-image-generation`

固定要求：

```yaml
required: true
allow-model-temporary-url: false
persist-generated-image: true
require-product-identity-preservation: true
require-manual-review-for-high-risk: true
```

## 7. Coze

为什么需要：

- 第一版智能聊天和内容型 Agent 可先通过 Coze Bot/Workflow 快速落地。
- Java 后端仍负责权限、任务状态、数据持久化、文件转存和模型网关控制。

在哪里配置：

- 后端: `egg-admin/src/main/resources/application.yml`
- 配置前缀: `egg.ai.model-gateway.coze`

关键配置：

```text
EGG_COZE_ENABLED=false
EGG_COZE_ENDPOINT=https://api.coze.cn
EGG_COZE_BOT_ID=
EGG_COZE_WORKFLOW_ID=
EGG_COZE_API_KEY=
```

## 8. A2A

为什么需要：

- 用户已确认 A2A 作为第一阶段最后开发项。
- A2A 不能早于真实出图、RAG、素材、审核和导出闭环。
- 第一版 A2A 应保持轻量 handoff，不引入租户、团队、计费或微服务拆分。

在哪里配置：

- 后端: `egg-admin/src/main/resources/application.yml`
- 配置前缀: `egg.ai.a2a`

默认关闭：

```text
EGG_A2A_ENABLED=false
EGG_A2A_ENDPOINT=
EGG_A2A_API_KEY=
```

## 9. 本地启动依赖

```powershell
cd D:\Code_Workspace\egg_commerce_ai
docker compose -f compose.postgres.yml up -d
```

服务地址：

```text
PostgreSQL: localhost:5432
Redis: localhost:6381
MinIO API: http://localhost:9000
MinIO Console: http://localhost:9001
```

## 10. 第一版开发顺序

1. 登录、商品库、MinIO 上传、Redis 任务中心、后台基础配置。
2. 智能聊天、RAG 检索、Coze/Agent 调用、来源记录。
3. 商品分析、真实图片生成、详情页组图、风格复刻、批量精修。
4. 素材库、版本管理、审核、下载、导出。
5. 前台 Picset 风格页面打磨。
6. A2A 轻量 handoff。
