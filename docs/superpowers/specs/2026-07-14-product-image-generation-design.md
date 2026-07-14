# 全品类商品图真实生图功能设计

日期：2026-07-14

## 1. 背景

前台用户端已有“全品类商品图”入口，当前页面展示为 Picset 风格的 AI 电商视觉生成工作台：用户上传产品图，选择主图或详情图，填写平台和图片要求，然后点击“开始分析”。页面步骤为：

1. 输入
2. 分析中
3. 确认规划
4. 生成中
5. 完成

现有代码中，该入口还只是静态页面：`egg-user-web/src/pages/ProductImagesPage.vue` 只配置了 `GeneratorWorkspace` 的标题和描述，没有真实接口；`GeneratorWorkspace.vue` 目前只做本地图片预览，点击生成时只把表单字段抛给页面，没有把图片文件、商品草稿 ID 或 OSS 图片记录传出去。

本设计把“全品类商品图”升级为真实可用的端到端功能：用户选择图片后立即上传到阿里云 OSS，后端创建不可见商品草稿；用户点击“开始分析”后创建异步任务，任务自动完成产品分析、规划展示、通义万相真实生图、结果持久化；任务成功后商品记录进入商品库，用户可查看、重命名、继续使用、下载和删除。

## 2. 已确认需求

- 首版必须真实生成图片，不做 prompt-only。
- 图片生成服务首版使用阿里 DashScope / 通义万相。
- 对象存储首版使用后端当前 `application.yml` 中的阿里云 OSS。
- 首版一次只生成当前选中的类型：主图或详情图。
- 首版每次固定生成 1 张图。
- 首版只实际开放抖音规则，但后端接口、表结构和配置要保留其它平台扩展能力。
- 图片比例沿用当前页面默认 `3:4`。
- 商品主体允许基于上传图重构角度，不接受简单抠图贴背景。
- 商品包装、Logo、商标和文字必须尽量保持原样；如果输入不足，系统提示多角度保真可能不足。
- 用户上传 1 张图也允许生成，但提示“多角度保真可能不足”；建议上传 2-3 张不同角度或细节图。
- 上传图片的用途由 AI 自动识别，不要求用户手动标注正面、侧面、细节图。
- 详情图由 AI 自动判断更适合“卖点说明图”还是“规格/细节图”。
- “确认规划”节点只展示 AI 规划内容，不要求用户确认，也不阻塞后续生成。
- 生成图若文字或 Logo 明显变形，首版直接展示，由用户人工筛选，不做自动质检拦截。
- 下载首版只支持下载单张生成图片原文件。
- 任务中心要纳入商品图任务，可查看进度、节点内容、失败原因、重试、终止和结果缩略图。
- 商品库只展示成功记录；任务失败或终止时保留任务和数据，但不进入商品库列表。
- 商品库首版支持查看详情、重命名、继续使用、下载生成图、删除。
- 商品库和素材库概念合并，顶部导航只展示“商品库”；原 `/assets` 素材库页面和路由保留备用，但不展示入口。
- 用户在商品库点击“继续使用”时，复制成一条新的商品草稿记录，并带入上传图、平台、图片类型、要求等参数跳转到全品类商品图页面。
- 商品库详情页不展示 AI 分析/规划，只展示上传图、生成图、平台、类型、要求、时间和操作。
- 任务中心展示上传摘要、AI 分析、规划、生成结果 URL/缩略图。
- 点击“开始分析”后可以在生成页或任务中心终止运行中任务。
- 终止任务只在本系统内标记，不强求调用阿里侧取消接口。
- 终止任务保留上传图、已完成节点内容和已产生文件；不进入商品库，不允许重试。
- 终止按钮只在分析中、确认规划、生成中等运行态显示。
- 失败任务允许重试，重试不是全部推倒重来，而是从失败节点继续。
- 图片生成失败时复用已完成的 AI 分析和规划，只重新调用通义万相生成。
- AI 帮写纳入首版，只优化当前“主图要求/详情图要求”输入框。
- AI 帮写结果弹小窗口展示，用户点击“替换”才回填，不自动覆盖原输入。
- 用户上传图片后立即进入 OSS，并创建不可见商品草稿记录。
- 同一个用户只允许有一条“全品类商品图草稿”；再次进入页面时恢复草稿。
- 如果草稿未开始分析，12 小时后删除草稿记录和全部上传 OSS 文件。
- 用户删除某张已上传产品图时，立即删除数据库图片记录和 OSS 文件。
- 如果任务已经开始分析，用户想上传新图片，需要新开一条商品草稿。
- 生成完成后右下角弹出小弹窗提示“已保存到商品库”，并提供“去商品库查看”入口。
- DashScope 原始任务 ID、模型 provider、model 等只存数据库和日志，不展示给普通用户。
- 提示词已经在后台“提示词模板”功能中维护，内容与交接文档一致，不需要在本功能 SQL 中重复插入模板。

## 3. 产品注意点

### 3.1 商品真实性不是普通生图问题

本功能不是“输入 prompt 生成一张漂亮图”，而是“基于用户上传的真实商品生成可用于电商的商品图”。产品经理层面最重要的验收标准是：生成图能不能让商家相信它属于自己的商品，且不会错误表达商品事实。

重点风险：

- 包装文字、Logo、商标可能被模型重绘错误。
- 单张俯拍图无法可靠推断看不见的侧面结构。
- 珠宝、食品、数码家电等类目有不同的真实性边界。
- 详情图容易诱导模型生成不存在的功效、参数、认证或促销信息。

首版策略：

- 允许单张图生成，但提示保真风险。
- 鼓励上传 2-3 张不同角度或细节图。
- AI 分析节点记录每张源图的用途识别和真实性约束。
- 提示词默认不生成大段中文文字、二维码、水印、联系方式、平台 Logo。
- 图片内文字变形首版不拦截，结果直接展示给用户人工筛选。

### 3.2 商品库是成功资产管理器，任务中心是过程追踪器

商品库只展示成功完成的商品图资产。它服务于用户复用、下载、重命名、删除。

任务中心展示所有任务，包括处理中、失败、终止、成功。它服务于过程追踪、失败原因、节点输出、重试和终止。

这个分工避免商品库被失败记录污染，也避免任务中心承担复杂的资产复用参数管理。

### 3.3 上传即持久化，开始分析只是绑定任务

用户选择图片时就要上传后端和 OSS。这样才能支持：

- 页面刷新后恢复草稿。
- 用户离开页面后不丢图。
- 上传后未开始分析的草稿可定时清理。
- 后续任务只引用已保存 OSS 图片，不依赖浏览器临时文件。

### 3.4 确认规划不是人工确认

页面步骤里的“确认规划”不需要用户点击确认。它只是让用户看到 AI 规划内容，例如：

- 商品识别摘要。
- 商品真实性约束。
- 主图或详情图的生成目的。
- 构图、背景、光线和风格方向。
- 详情图判断为卖点说明图或规格/细节图的理由。

后端写入规划后自动进入生成节点。

## 4. 现有代码参考

### 4.1 前端参考

- `egg-user-web/src/pages/ProductImagesPage.vue`：当前全品类商品图静态入口，需要接入真实任务。
- `egg-user-web/src/components/GeneratorWorkspace.vue`：通用生成工作台，需要支持即时上传、草稿恢复、已上传图片后端记录、删除 OSS 图片、终止按钮、AI 帮写小窗口、生成完成通知。
- `egg-user-web/src/pages/StoryGenerationPage.vue`：剧情生成的节点状态、轮询、下载、任务完成通知可参考。
- `egg-user-web/src/pages/TasksPage.vue`：当前是剧情任务中心，需要升级为多类型任务中心。
- `egg-user-web/src/pages/ProductsPage.vue`：当前商品库 mock 页面，需要接真实商品资产列表。
- `egg-user-web/src/pages/ProductDetailPage.vue`：当前商品详情 mock 页面，需要接真实详情。
- `egg-user-web/src/pages/AssetsPage.vue`：素材库页面保留备用，首版不在导航展示。
- `egg-user-web/src/data/mock.js`：顶部导航目前没有商品库入口，需要把商品库加入主导航或合适入口，并隐藏素材库入口。

### 4.2 后端参考

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/story/AiStoryTaskController.java`：剧情生成任务 Controller，可参考当前用户隔离、创建、列表、详情、重试、导出。
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/story/impl/AiStoryTaskServiceImpl.java`：剧情生成的异步任务、节点推进、失败停留、重试续跑可参考。
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiObjectStorageService.java`：当前对象存储接口只面向知识库文件，需要升级成通用 AI 资产存储。
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/impl/AiAliOssObjectStorageServiceImpl.java`：当前阿里 OSS 上传实现可复用连接配置，但 object key 生成、方法名、删除、列举前缀都要扩展。
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt/AiPromptTemplateMapper.java`：提示词模板运行时查询可扩展，按分类读取已发布模板。
- `egg-framework/src/main/java/com/egg/commerce/framework/config/properties/AiCommerceProperties.java`：已预留 `modelGateway.image`、`modelGateway.vision` 和 `realImageGeneration`，需要补齐实际通义万相调用配置。

## 5. 总体架构

采用“商品资产记录 + 生成任务 + 图片明细”的结构。

核心模块：

1. 商品资产模块：保存用户上传图、生成图、平台、类型、要求、可见状态、复用来源。
2. 图片对象模块：保存每张图片的 OSS bucket、object key、URL、大小、角色和排序。
3. 商品图任务模块：保存任务状态、当前节点、节点输出、失败原因、模型任务 ID 和重试/终止状态。
4. 通义万相网关模块：负责 DashScope 异步任务提交、查询、结果下载和异常解析。
5. 对象存储模块：负责上传产品图、保存生成图、删除图片、列举商品图前缀对象。
6. 提示词运行时模块：读取后台已发布模板，渲染变量，解析严格 JSON 输出。
7. 商品库前端：展示成功商品资产，支持详情、重命名、继续使用、下载、删除。
8. 任务中心前端：展示商品图任务过程，支持查看节点内容、重试、终止。

## 6. 流程设计

### 6.1 进入生成页

1. 前端进入 `/generate/product-images`。
2. 调用 `GET /ai/product-assets/draft`。
3. 后端查找当前用户是否存在未开始分析的草稿商品记录。
4. 如果存在且未超过 12 小时，返回草稿、已上传图片和表单参数。
5. 如果不存在，返回空草稿状态；第一张图片上传时再创建草稿。

同一用户只允许一条全品类商品图草稿。

### 6.2 上传图片

1. 用户选择或拖拽图片。
2. 前端立即调用上传接口。
3. 后端如果没有草稿，创建不可见商品记录，状态为 `draft_uploaded`。
4. 后端把图片上传到阿里 OSS，object key 使用商品图专用前缀，例如 `ai/product-images/source/{userId}/{date}/{uuid}.ext`。
5. 后端新增图片记录，角色为 `source`。
6. 前端用后端返回的图片 URL 展示预览。

删除上传图时：

1. 前端调用 `DELETE /ai/product-assets/draft/images/{imageId}`。
2. 后端校验图片属于当前用户草稿。
3. 删除 OSS 对象。
4. 删除数据库图片记录。
5. 前端移除预览。

如果删除失败，前端提示失败，不要假装删除成功。

### 6.3 开始分析

1. 用户点击“开始分析”。
2. 前端校验至少有 1 张上传图；如果只有 1 张，提示“多角度保真可能不足”，但不阻断。
3. 前端提交商品草稿 ID、当前图片类型、目标平台、比例、清晰度、用户要求。
4. 后端校验草稿属于当前用户且有上传图。
5. 后端更新商品记录为 `processing`，仍不可见。
6. 后端创建商品图任务，状态为 `processing`，当前节点为 `分析中`。
7. 后端异步执行任务。
8. 前端绑定返回的 `taskId` 并轮询。

如果用户重新发起新分析：

- 后端创建新商品草稿和新任务。
- 当前页面只跟踪最新任务。
- 旧任务继续执行，结果在任务中心查看。

### 6.4 分析节点

分析节点负责把上传图和用户要求转成结构化商品理解。

输出内容建议：

```json
{
  "sourceImages": [
    {
      "imageId": "",
      "detectedView": "正面/侧面/俯拍/细节/包装文字",
      "usefulness": "high/medium/low",
      "observations": []
    }
  ],
  "productAnalysis": {
    "productName": "",
    "category": "",
    "visualIdentityRules": [],
    "keySellingPoints": [],
    "targetAudience": [],
    "truthfulnessWarnings": []
  }
}
```

分析节点要保存到 `analysis_output`，供任务中心展示和重试续跑使用。

### 6.5 规划节点

规划节点对应前端“确认规划”。它不等待用户确认。

主图规划输出建议：

```json
{
  "generationPlan": {
    "targetPlatform": "抖音",
    "assetType": "main",
    "aspectRatio": "3:4",
    "imageCount": 1,
    "styleDirection": "",
    "compositionStrategy": "",
    "backgroundStrategy": "",
    "lightingStrategy": ""
  },
  "imagePrompt": {
    "promptId": "main_01",
    "purpose": "商品主图",
    "positivePrompt": "",
    "negativePrompt": "",
    "productTruthRules": [],
    "recommendedModelParams": {}
  },
  "seoTitles": [],
  "sellingPointCopy": {},
  "riskReview": {}
}
```

详情图规划输出建议：

```json
{
  "detailPagePlan": {
    "selectedDetailType": "卖点说明图/规格细节图",
    "selectionReason": "",
    "theme": "",
    "visualStyle": "",
    "sectionOrder": []
  },
  "imagePrompt": {
    "promptId": "detail_01",
    "sectionName": "",
    "purpose": "",
    "positivePrompt": "",
    "negativePrompt": "",
    "productTruthRules": [],
    "copySuggestion": {}
  },
  "seoCopy": {},
  "riskReview": {}
}
```

规划节点写入 `planning_output` 后，自动进入生成节点。

### 6.6 生成节点

1. 后端从 `planning_output` 读取第一条图片提示词。
2. 后端调用通义万相，使用用户上传图作为参考图或编辑输入。
3. 后端保存 DashScope 原始任务 ID 到数据库，不展示给用户。
4. 后端轮询 DashScope 任务结果。
5. 如果用户已终止任务，后端停止后续业务写入，不把任务置为完成。
6. 如果生成成功，后端下载模型返回的临时 URL 图片。
7. 后端上传图片到阿里 OSS，object key 使用商品图专用前缀，例如 `ai/product-images/generated/{userId}/{date}/{uuid}.png`。
8. 后端新增图片记录，角色为 `generated`。
9. 后端写入 `generation_output`，包含生成图记录 ID、OSS URL、模型参数、提示词摘要。

DashScope 结果 URL 有时效，不能直接依赖模型 URL 作为最终资产 URL。必须持久化到自己的 OSS。

### 6.7 完成节点

任务成功后：

1. 更新任务 `status=completed`、`current_node=完成`、`completed_at`。
2. 更新商品记录 `status=completed`、`visible=true`。
3. 设置商品封面为生成图。
4. 如果商品名为空或仍为默认名，优先使用 AI 分析出的商品名；没有商品名则使用“未命名商品-日期”。
5. 前端展示生成图和下载按钮。
6. 右下角弹出提示，提供“去商品库查看”入口。

### 6.8 失败与重试

失败时：

- 任务停在失败节点。
- 写入 `status=failed`。
- 写入 `failure_reason`。
- 保留已完成节点输出。
- 商品记录保持不可见，状态可同步为 `failed`。
- 任务中心可查看失败节点和已完成内容。

重试规则：

- 分析失败：从分析节点重试。
- 规划失败：复用上传图，从规划节点重试。
- 生成失败：复用分析和规划，只重新调用通义万相。
- OSS 保存失败：复用 DashScope 结果信息，如果临时 URL 仍可用则重试保存；不可用则重新调用生成节点。
- 终止任务不允许重试。

### 6.9 终止

终止入口：

- 生成页步骤条后增加“终止”按钮。
- 任务中心运行中任务增加“终止”按钮。

终止行为：

1. 用户点击终止。
2. 后端校验任务属于当前用户且处于运行态。
3. 后端写入 `status=cancelled`、`cancelled_at`、`current_node` 保持终止时所在节点。
4. 后端不删除上传图和已产生文件。
5. 如果异步线程后续收到 DashScope 结果，写入前必须再次检查任务状态；已终止则不覆盖为 completed。
6. 生成页点击终止后，页面表面清空，提示“当前任务已终止，如需查看请前往任务中心”。
7. 终止任务在任务中心展示已完成节点内容和已有图片。
8. 终止任务不进入商品库，不显示重试按钮。

首版不要求调用阿里侧取消接口。

## 7. 数据模型

### 7.1 `ai_product_assets`

商品库主表，也承载草稿、处理中、失败、终止状态。商品库列表只展示成功可见记录。

建议字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | uuid | 商品资产主键 |
| `user_id` | bigint | 前台用户 ID，所有查询必须按该字段隔离 |
| `name` | varchar(200) | 商品名称，可由 AI 提取，也可用户重命名 |
| `status` | varchar(30) | `draft_uploaded`、`processing`、`completed`、`failed`、`cancelled` |
| `visible` | boolean | 是否在商品库展示，只有成功记录为 true |
| `asset_type` | varchar(30) | `main` 或 `detail`，表示本次生成类型 |
| `target_platform` | varchar(100) | 目标平台，首版抖音，保留扩展 |
| `aspect_ratio` | varchar(30) | 首版默认 `3:4` |
| `quality` | varchar(30) | 清晰度，例如 `2K` |
| `user_requirement` | text | 用户填写的主图/详情图要求 |
| `source_product_id` | uuid | 继续使用时的来源商品 ID |
| `cover_image_id` | uuid | 商品库封面图，通常为生成图 |
| `last_task_id` | uuid | 最近一次关联任务 ID |
| `created_at` | timestamp | 创建时间 |
| `updated_at` | timestamp | 更新时间 |

说明：

- 上传第一张图时创建 `draft_uploaded` 商品记录，`visible=false`。
- 点击开始分析后状态变为 `processing`，仍 `visible=false`。
- 任务成功后状态变为 `completed`，`visible=true`。
- 任务失败或终止时记录保留，但 `visible=false`。
- 商品库删除操作删除该商品、关联图片记录和 OSS 对象。
- 商品重命名只允许修改 `name`。

### 7.2 `ai_product_asset_images`

保存用户上传图和生成图。

建议字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | uuid | 图片记录主键 |
| `product_asset_id` | uuid | 所属商品资产 |
| `user_id` | bigint | 前台用户 ID，用于权限隔离 |
| `task_id` | uuid | 生成图可关联任务，源图可为空或绑定开始分析后的任务 |
| `image_role` | varchar(30) | `source` 或 `generated` |
| `oss_bucket` | varchar(200) | OSS bucket |
| `oss_object_key` | varchar(500) | OSS object key |
| `oss_url` | text | 可访问 URL 或后端代理 URL |
| `mime_type` | varchar(100) | MIME 类型 |
| `file_size` | bigint | 文件大小 |
| `width` | integer | 图片宽度，可后续补充 |
| `height` | integer | 图片高度，可后续补充 |
| `sort_order` | integer | 多图排序 |
| `analysis_role` | varchar(100) | AI 识别用途，例如正面、侧面、细节 |
| `created_at` | timestamp | 创建时间 |
| `updated_at` | timestamp | 更新时间 |

说明：

- 删除上传图时，删除对应 OSS 对象和该表记录。
- 商品库删除时，删除该商品下所有图片 OSS 对象和记录。
- 失败、终止任务关联图片不被定时清理。

### 7.3 `ai_product_image_tasks`

商品图任务表。

建议字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | uuid | 任务主键 |
| `product_asset_id` | uuid | 关联商品资产 |
| `user_id` | bigint | 前台用户 ID |
| `task_type` | varchar(50) | 固定 `product_image` |
| `asset_type` | varchar(30) | `main` 或 `detail` |
| `target_platform` | varchar(100) | 目标平台 |
| `current_node` | varchar(50) | 输入、分析中、确认规划、生成中、完成 |
| `status` | varchar(30) | `processing`、`completed`、`failed`、`cancelled` |
| `failure_reason` | text | 失败原因 |
| `cancel_reason` | text | 终止原因，首版可用默认文案 |
| `analysis_prompt_template_id` | uuid | 分析/规划模板 ID |
| `analysis_prompt_version` | integer | 模板版本 |
| `analysis_output` | jsonb | AI 分析结构化输出 |
| `planning_output` | jsonb | AI 规划和生图提示词输出 |
| `generation_output` | jsonb | 生成结果、图片记录 ID、模型响应摘要 |
| `debug_prompts` | jsonb | 渲染后的提示词和模型请求摘要 |
| `dashscope_task_id` | varchar(200) | DashScope 原始任务 ID，只存库和日志 |
| `model_provider` | varchar(50) | 模型供应商，例如 dashscope |
| `model_name` | varchar(120) | 模型名，例如 wan2.7-image-pro |
| `model_params` | jsonb | 模型参数快照 |
| `started_at` | timestamp | 开始时间 |
| `completed_at` | timestamp | 完成时间 |
| `cancelled_at` | timestamp | 终止时间 |
| `created_at` | timestamp | 创建时间 |
| `updated_at` | timestamp | 更新时间 |

说明：

- 节点输出使用 JSONB，便于任务中心展示和重试续跑。
- `debug_prompts` 只用于排障，不直接展示给普通用户。
- 任务中心列表按 `created_at desc`。

## 8. 后端接口设计

### 8.1 草稿与上传接口

#### 获取当前草稿

`GET /ai/product-assets/draft`

返回当前用户未开始分析的草稿、图片列表和表单参数。没有草稿时返回空状态。

#### 上传草稿图片

`POST /ai/product-assets/draft/images`

请求类型：`multipart/form-data`

字段：

- `file`

行为：

- 无草稿时创建草稿。
- 上传到 OSS。
- 新增图片记录。
- 返回商品草稿和图片记录。

#### 删除草稿图片

`DELETE /ai/product-assets/draft/images/{imageId}`

行为：

- 校验图片属于当前用户草稿。
- 删除 OSS 对象。
- 删除图片记录。

#### 清空草稿

`DELETE /ai/product-assets/draft`

行为：

- 删除当前未开始分析草稿。
- 删除关联 OSS 图片。

### 8.2 商品图任务接口

#### 创建任务

`POST /ai/product-image/tasks`

请求体：

```json
{
  "productAssetId": "",
  "assetType": "main",
  "targetPlatform": "抖音",
  "aspectRatio": "3:4",
  "quality": "2K",
  "userRequirement": ""
}
```

返回任务详情。

#### 查询任务详情

`GET /ai/product-image/tasks/{id}`

用于生成页轮询和任务中心详情。

#### 查询任务列表

`GET /ai/product-image/tasks`

用于任务中心。首版可与剧情任务中心并列展示，也可以在统一接口中返回多任务类型。

#### 重试任务

`POST /ai/product-image/tasks/{id}/retry`

仅失败任务可重试，终止任务不可重试。

#### 终止任务

`POST /ai/product-image/tasks/{id}/cancel`

仅运行态可终止。

### 8.3 商品库接口

#### 商品库列表

`GET /ai/product-assets`

只返回当前用户 `status=completed` 且 `visible=true` 的记录。

#### 商品详情

`GET /ai/product-assets/{id}`

展示上传图、生成图、平台、类型、要求、创建时间等。不展示 AI 分析/规划。

#### 重命名

`PUT /ai/product-assets/{id}/name`

请求体：

```json
{
  "name": "新的商品名称"
}
```

#### 继续使用

`POST /ai/product-assets/{id}/reuse`

行为：

- 校验商品属于当前用户且可见。
- 复制商品参数。
- 复制源图 OSS 对象并创建新的源图图片记录。
- 创建新的不可见草稿。
- 返回新草稿 ID 和生成页可恢复数据。

说明：

- 继续使用时复制成一条新的商品记录，不覆盖原商品。
- 首版采用“复制 OSS 对象”的方式，不做引用计数。这样删除原商品时不会影响由“继续使用”创建的新草稿。
- 新草稿通过 `source_product_id` 记录来源商品，便于后续追溯。

#### 下载生成图

`GET /ai/product-assets/{id}/download`

返回单张生成图文件流。后端校验归属，不直接依赖公开 URL。

#### 删除商品

`DELETE /ai/product-assets/{id}`

行为：

- 校验商品属于当前用户。
- 删除商品下所有图片 OSS 对象。
- 删除图片记录。
- 删除商品记录。

首版按硬删除 OSS 实现。任务记录保留文本节点内容；任务中心若遇到已删除的图片记录或 OSS 对象，展示“图片已删除”。

### 8.4 AI 帮写接口

`POST /ai/product-image/requirement/rewrite`

请求体：

```json
{
  "assetType": "main",
  "targetPlatform": "抖音",
  "userRequirement": "",
  "productAssetId": ""
}
```

返回：

```json
{
  "optimizedRequirement": "",
  "displayTitle": "优化后的主图要求",
  "imagePromptReady": true,
  "missingQuestions": [],
  "safetyNotes": []
}
```

前端只展示 `optimizedRequirement`，用户点击“替换”才回填。

## 9. 提示词模板设计

用户已在后台“提示词模板”功能中维护了以下模板，内容与交接文档一致：

- 分类 `AI生图`：
  - `主图系统提示词`
  - `详情图系统提示词`
- 分类 `AI优化提示词`：
  - `AI帮写优化提示词`

因此本功能的 SQL 不需要重复插入这三条模板。

运行时读取规则：

- 主图任务读取已发布的 `主图系统提示词`。
- 详情图任务读取已发布的 `详情图系统提示词`。
- AI 帮写读取已发布的 `AI帮写优化提示词`。
- 若同分类下有多个已发布版本，按版本号或更新时间取最新。

提示词变量：

- `{targetPlatform}`
- `{productCategory}`
- `{modelName}`
- `{aspectRatio}`
- `{clarity}`
- `{imageCount}`
- `{userRequirement}`
- `{platformRules}`
- `{categoryRules}`
- `{modelAdapterRules}`
- `{sourceImageAnalysis}`

首版变量来源：

- `targetPlatform`：前端选择，首版抖音。
- `productCategory`：AI 分析输出；若分析前需要，可默认“自动识别”。
- `modelName`：后端配置，首版通义万相模型。
- `aspectRatio`：首版 `3:4`。
- `clarity`：页面清晰度，首版可保留 `2K`。
- `imageCount`：首版固定 `1`。
- `userRequirement`：用户输入框内容。
- `platformRules`：首版可使用抖音内置规则或知识库摘要；不要硬编码到模板正文。
- `categoryRules`：首版可使用通用规则加重点类目规则，后续接 RAG。
- `modelAdapterRules`：通义万相适配规则。
- `sourceImageAnalysis`：分析节点输出。

AI 输出必须严格 JSON。后端需要做：

- JSON parse。
- 必填字段校验。
- 缺字段时失败停在当前节点。
- 失败原因写入任务。
- 原始输出可写入 `debug_prompts` 或模型日志，便于排查。

## 10. 通义万相对接

首版封装独立网关：

- `AiImageGenerationService`
- `AiImageGenerationRequest`
- `AiImageGenerationResult`
- `AiDashScopeImageGenerationServiceImpl`
- `AiMockImageGenerationServiceImpl` 可选，仅用于本地联调；正式验收必须真实生成。

职责：

1. 构造 DashScope 请求。
2. 提交异步图片生成任务。
3. 保存或返回 DashScope task id。
4. 查询任务状态。
5. 返回模型临时图片 URL。
6. 处理错误码、超时和空结果。

业务服务不要直接拼 DashScope HTTP 请求，避免供应商协议散落在任务流程里。

模型结果 URL 只能作为临时输入，后端必须下载后保存到阿里 OSS。

配置建议：

```yaml
egg:
  ai:
    model-gateway:
      image:
        provider: ${EGG_MODEL_IMAGE_PROVIDER:dashscope}
        model: ${EGG_MODEL_IMAGE_NAME:wan2.7-image-pro}
        endpoint: ${EGG_DASHSCOPE_ENDPOINT:https://dashscope.aliyuncs.com}
        api-key: ${EGG_DASHSCOPE_API_KEY:}
```

## 11. 对象存储设计

当前 `AiObjectStorageService` 只支持知识库文件，需要改成通用 AI 资产存储接口。

建议接口：

```java
AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId);
AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId);
void deleteObject(String objectKey);
List<AiStoredObjectSummary> listProductImageObjects(Date olderThan);
byte[] downloadObject(String objectKey);
```

对象 key 建议：

- 源图：`ai/product-images/source/{userId}/{yyyy/MM/dd}/{uuid}.{ext}`
- 生成图：`ai/product-images/generated/{userId}/{yyyy/MM/dd}/{uuid}.png`

只扫描 `ai/product-images/` 前缀做清理，不碰知识库和其他对象。

## 12. OSS 清理策略

清理两类：

### 12.1 OSS 有但数据库无记录

来源：

- OSS 上传成功但数据库写入失败。
- 生成图保存 OSS 成功但任务更新失败。
- 异常中断导致的对象残留。

策略：

- 扫描 `ai/product-images/` 前缀。
- 对象创建时间超过 12 小时。
- 数据库中不存在相同 `oss_object_key`。
- 删除 OSS 对象。

### 12.2 数据库有草稿，但用户未开始分析

来源：

- 用户上传产品图后离开页面，没有点击“开始分析”。

策略：

- 查询 `status=draft_uploaded` 且 `updated_at` 超过 12 小时的不可见商品记录。
- 删除其全部 OSS 图片。
- 删除图片记录。
- 删除草稿商品记录。

不能清理：

- `processing` 任务关联图片。
- `failed` 任务关联图片。
- `cancelled` 任务关联图片。
- `completed` 商品库图片。

实现方式：

- 项目已有 `egg-quartz` 若依式定时任务模块，但当前 `sql/quartz.sql` 是 MySQL 风格，而项目业务库已按 PostgreSQL 使用。
- 首版使用 Spring 定时 Bean，例如 `productImageCleanupTask.cleanExpiredDraftsAndOrphans()`。
- 后续如果需要接入后台定时任务管理，再补 PostgreSQL 版 Quartz 表和任务白名单配置。

## 13. 前端设计

### 13.1 全品类商品图页面

主要状态：

- 当前草稿商品。
- 已上传图片列表。
- 当前任务。
- 当前选中步骤。
- 是否正在上传。
- 是否正在生成。
- AI 帮写弹窗。
- 完成通知。

进入页面：

- 加载草稿并恢复图片和表单。

上传：

- 选择图片后立即调用后端上传。
- 上传成功后展示后端 URL。
- 删除图片调用后端删除。

开始分析：

- 按钮文案为“开始分析”。
- 单张图时提示保真风险。
- 创建任务并开始轮询。

终止：

- 运行态步骤条后显示“终止”按钮。
- 点击后调用终止接口。
- 成功后清空页面表面状态，并提示去任务中心。

结果：

- `分析中` 展示 AI 分析。
- `确认规划` 展示 AI 规划。
- `生成中` 展示生成状态。
- `完成` 展示生成图和下载按钮。

完成通知：

- 右下角小弹窗提示“已保存到商品库”。
- 提供“去商品库查看”按钮。

### 13.2 AI 帮写

- 点击“AI 帮写”调用后端优化接口。
- 弹窗展示优化后的要求。
- 按钮：替换、退出。
- 替换时写回当前主图/详情图要求输入框。
- 不自动覆盖用户输入。

### 13.3 任务中心

任务中心要从剧情任务专用升级为多类型任务中心。

商品图任务展示：

- 任务名称：商品名或“全品类商品图任务”。
- 类型：主图/详情图。
- 平台：抖音。
- 状态：生成中、成功、失败、已终止。
- 当前节点。
- 进度。
- 失败原因或终止状态。
- 操作：查看内容、重试、终止。

详情弹窗或详情区域展示：

- 上传摘要。
- AI 分析。
- 规划。
- 生成结果 URL/缩略图。
- 已终止任务展示已完成节点和已有图片。

### 13.4 商品库

顶部导航展示“商品库”。

列表展示：

- 商品名。
- 缩略图。
- 类型：主图/详情图。
- 平台。
- 最近生成时间。
- 操作：详情、重命名、继续使用、下载生成图、删除。

详情页展示：

- 上传产品图。
- 生成图。
- 平台。
- 类型。
- 用户要求。
- 创建/完成时间。
- 操作按钮。

不展示 AI 分析/规划。

### 13.5 素材库

`/assets` 页面和路由保留备用，但不在顶部导航展示。

## 14. 权限与用户隔离

所有前台接口都必须使用当前登录用户 ID：

- 草稿查询。
- 图片上传。
- 图片删除。
- 任务创建。
- 任务查询。
- 重试。
- 终止。
- 商品库列表。
- 商品详情。
- 重命名。
- 继续使用。
- 下载。
- 删除。

后端不能信任前端传入的 `userId`。

查询条件必须包含 `user_id`。

删除 OSS 前必须先校验数据库记录归属当前用户。

## 15. 错误处理

### 15.1 上传失败

- OSS 上传失败：前端提示上传失败，不创建图片记录。
- 数据库写入失败但 OSS 成功：记录日志；清理任务 12 小时后删除孤儿对象。若能同步补偿删除更好。

### 15.2 模板缺失

- 未找到已发布主图/详情图/AI 帮写模板时，任务失败或接口返回错误。
- 错误文案明确提示后台发布对应模板。

### 15.3 JSON 解析失败

- 分析/规划输出不是合法 JSON：任务失败，停在当前节点。
- 失败原因写入 `failure_reason`。

### 15.4 DashScope 失败

- 提交失败：生成节点失败，可重试。
- 查询超时：生成节点失败，可重试。
- 返回空图：生成节点失败，可重试。
- 临时 URL 下载失败：生成节点失败，可重试。

### 15.5 OSS 保存生成图失败

- 任务停在生成节点。
- 如果模型 URL 仍有效，重试可直接重试保存。
- 如果模型 URL 过期，重试重新调用生成节点。

### 15.6 用户终止竞态

异步线程在每个节点前后检查任务状态：

- 节点开始前检查是否已取消。
- 调用模型返回后检查是否已取消。
- 写入完成状态前检查是否已取消。

避免用户终止后任务又被后台线程写成 completed。

## 16. 测试范围

### 16.1 后端测试

- 上传第一张图时创建当前用户不可见草稿。
- 多图上传追加到同一草稿。
- 删除草稿图片时删除 OSS 和图片记录。
- 获取草稿可恢复图片和参数。
- 创建任务时校验草稿归属和图片存在。
- 创建任务后商品记录变为 processing 且不可见。
- 分析输出写入任务。
- 规划输出写入任务。
- 生成成功后生成图写入 OSS 和图片表。
- 任务成功后商品记录变为 completed 且 visible=true。
- 失败任务停留失败节点，保留前序输出。
- 生成节点失败重试时复用分析和规划。
- 终止任务写入 cancelled，不允许重试，不进入商品库。
- 终止后异步线程不能覆盖为 completed。
- 商品库列表只返回当前用户成功可见记录。
- 商品重命名只影响当前用户记录。
- 继续使用复制成新草稿，不覆盖原商品。
- 下载生成图校验归属。
- 删除商品删除 OSS 和数据库记录。
- 12 小时清理草稿和 OSS。
- 12 小时清理 OSS 孤儿对象。
- 清理任务不删除 failed/cancelled/completed/processing 关联对象。
- AI 帮写读取已发布模板并返回 `optimizedRequirement`。

### 16.2 前端测试

- 进入页面加载草稿。
- 上传图片立即调用后端。
- 删除图片调用后端并更新预览。
- 单图开始分析显示保真提示但不阻断。
- 点击开始分析创建任务并轮询。
- 步骤条按节点更新。
- 确认规划节点只展示不阻塞。
- 完成后展示生成图、下载按钮和右下角商品库提示。
- 运行中显示终止按钮，完成/失败/终止后隐藏。
- 终止后页面清空并提示去任务中心。
- AI 帮写弹窗展示优化结果，替换按钮回填。
- 任务中心展示商品图任务、节点内容、缩略图、重试、终止。
- 商品库列表展示成功记录和操作按钮。
- 商品库继续使用后跳转全品类商品图并恢复参数。

## 17. 实施建议

建议分阶段实现，但每阶段都保持可验证。

### 阶段一：数据底座和存储

- 新增 SQL 表。
- 新增商品资产、图片、任务 domain/mapper/service。
- 扩展对象存储接口。
- 实现草稿获取、上传、删除、清空。
- 实现商品库列表、详情、重命名、删除。

### 阶段二：任务链路

- 实现商品图任务创建、详情、列表。
- 实现分析/规划模板读取和 JSON 解析。
- 实现异步节点推进。
- 实现失败停留和重试续跑。
- 实现终止。

### 阶段三：通义万相

- 新增 DashScope 图片生成网关。
- 提交任务、查询结果、下载临时 URL。
- 保存生成图到 OSS。
- 完成商品可见化。

### 阶段四：前端生成页

- 上传组件改为即时上传。
- 草稿恢复。
- 开始分析、轮询、节点展示。
- 终止按钮。
- 完成通知。
- 下载生成图。
- AI 帮写弹窗。

### 阶段五：任务中心和商品库

- 任务中心多类型展示。
- 商品图任务详情。
- 商品库真实数据。
- 继续使用复制草稿。
- 导航入口调整，隐藏素材库入口。

### 阶段六：清理任务和收尾

- 12 小时草稿清理。
- OSS 孤儿对象清理。
- 文档、变更记录和测试补齐。

## 18. 不在首版范围

- 不做自动 OCR/Logo 质检拦截。
- 不做平台矩阵的完整规则配置，只实际跑抖音。
- 不做批量 4/8 张生成。
- 不做主图和详情图同时生成。
- 不做真实阿里侧取消任务。
- 不做积分、计费、支付。
- 不做团队空间或多租户协作。
- 不做素材库独立入口。
- 不做商品类目、品牌、规格、卖点等完整商品资料编辑，首版只支持重命名。

## 19. 实施默认决策

为避免后续实现分叉，首版按以下默认决策执行：

1. “继续使用”时复制源图 OSS 对象，创建新商品草稿和新图片记录，不做引用计数。
2. 商品删除时删除 OSS 文件；任务中心历史保留文本节点，图片位置展示“图片已删除”。
3. 任务中心首版由前端分别请求剧情任务和商品图任务后合并展示，减少对已有剧情接口的冲击。
4. 清理任务首版使用 Spring 定时 Bean，后续再接入若依 Quartz 管理后台。

## 20. 设计结论

本功能的核心不是单次图片生成，而是建立“上传即资产化、任务可追踪、失败可续跑、成功可复用、用户数据隔离、OSS 可治理”的商品图生成闭环。

首版应优先保证：

- 用户上传图不会因刷新丢失。
- 真实调用通义万相生成 1 张图。
- 生成结果进入商品库并可下载。
- 失败能从失败节点重试。
- 终止不会错误完成任务。
- 商品库只展示成功资产。
- OSS 不留下长期无人管理的草稿和孤儿对象。

这样第一版虽然只生成单张图，但底座足够支撑后续扩展到批量主图、详情图组、多平台规则、自动质检、批量下载和更多生图模型。
