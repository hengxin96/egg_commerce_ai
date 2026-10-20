# 全品类商品图真实生图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前分支完成“全品类商品图”首版真实生图闭环：即时上传到阿里云 OSS、不可见商品草稿、异步任务节点、通义万相真实出图、商品库展示、任务中心追踪、AI 帮写、重试/终止和 12 小时清理。

**Architecture:** 后端采用“商品资产记录 + 图片明细 + 商品图任务”的结构；商品库只展示成功资产，任务中心展示所有任务过程。前端复用现有 `GeneratorWorkspace` 的视觉工作台，但把上传从本地预览升级为后端草稿/OSS 上传，并让 `ProductImagesPage` 负责商品图任务状态和节点展示。

**Tech Stack:** Spring Boot Java、MyBatis XML、PostgreSQL SQL、阿里云 OSS SDK、DashScope/通义万相 HTTP API、Vue 3 Composition API、Element Plus、Vite、Node source tests、JUnit 5。

## Global Constraints

- 只在当前分支修改，不创建新分支。
- 不回滚已有未提交改动；只触碰本功能需要的文件。
- 首版必须真实调用阿里 DashScope / 通义万相生成图片，不做 prompt-only。
- 对象存储首版使用阿里云 OSS。
- 首版一次只生成当前选中的类型：主图或详情图。
- 首版每次固定生成 1 张图。
- 首版只实际开放抖音规则，但接口、表结构和配置保留多平台扩展能力。
- 图片比例沿用当前页面默认 `3:4`。
- 用户选择图片后立即上传后端和 OSS。
- 商品库只展示成功记录；失败和终止记录只在任务中心可见。
- 失败任务支持从失败节点续跑；终止任务不可重试。
- AI 帮写纳入首版，结果弹窗展示，用户点击“替换”才回填。
- 提示词已在后台“提示词模板”中维护，SQL 不重复插入模板。
- OSS 清理只扫描 `ai/product-images/` 前缀；12 小时清理未开始分析草稿和数据库无引用的孤儿对象。
- 计划执行时每个任务完成后做一次小提交；提交前只 stage 当前任务文件。

---

## 文件结构总览

### 后端 SQL

- Create: `sql/ai_product_image_generation.sql`  
  创建 `ai_product_assets`、`ai_product_asset_images`、`ai_product_image_tasks`，包含 PostgreSQL 字段注释、索引和必要约束。

### 后端 Domain

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAsset.java`  
  商品资产主表实体，承载草稿、处理中、成功、失败、终止状态。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetImage.java`  
  上传图和生成图实体，保存 OSS bucket/object key/url/角色/排序。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductImageTask.java`  
  商品图任务实体，保存节点输出、任务状态、DashScope task id 和模型快照。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductImageTaskRequest.java`  
  创建商品图任务请求。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetRenameRequest.java`  
  商品重命名请求。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiRequirementRewriteRequest.java`  
  AI 帮写请求。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiRequirementRewriteResult.java`  
  AI 帮写响应。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetDetail.java`  
  商品库详情响应，聚合商品、源图和生成图。

- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductDraftDetail.java`  
  生成页草稿响应，聚合草稿商品和已上传源图。

### 后端 Mapper

- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductAssetMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductAssetImageMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductImageTaskMapper.java`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductAssetMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductAssetImageMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductImageTaskMapper.xml`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt/AiPromptTemplateMapper.java`
- Modify: `egg-system/src/main/resources/mapper/ai/prompt/AiPromptTemplateMapper.xml`

### 后端 Storage

- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiObjectStorageService.java`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiStoredObject.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiStoredObjectSummary.java`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/impl/AiAliOssObjectStorageServiceImpl.java`

### 后端 DashScope 图片网关

- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationResult.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/impl/AiDashScopeImageGenerationServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/impl/AiMockImageGenerationServiceImpl.java`
- Modify: `egg-admin/src/main/resources/application.yml`
- Modify: `.env.example`

### 后端 Service

- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductAssetService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductAssetServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductRequirementRewriteServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageCleanupServiceImpl.java`

### 后端 Scheduled Task

- Create: `egg-admin/src/main/java/com/egg/commerce/web/task/ProductImageCleanupTask.java`

### 后端 Controller

- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductAssetController.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductImageTaskController.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductRequirementRewriteController.java`

### 后端 Tests

- Create: `egg-system/src/test/java/com/egg/commerce/system/domain/ai/product/AiProductImageDomainTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/storage/AiAliOssObjectStorageServiceImplTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/model/AiImageGenerationServiceTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductAssetServiceImplTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteServiceImplTest.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupServiceImplTest.java`

### 前端 API

- Create: `egg-user-web/src/api/productImage.js`

### 前端 Pages / Components

- Modify: `egg-user-web/src/components/GeneratorWorkspace.vue`
- Modify: `egg-user-web/src/pages/ProductImagesPage.vue`
- Modify: `egg-user-web/src/pages/TasksPage.vue`
- Modify: `egg-user-web/src/pages/ProductsPage.vue`
- Modify: `egg-user-web/src/pages/ProductDetailPage.vue`
- Modify: `egg-user-web/src/layout/UserShell.vue`
- Modify: `egg-user-web/src/data/mock.js`
- Modify: `egg-user-web/src/router/index.js` only if route metadata or auth requirements need updates.
- Modify: `egg-user-web/src/styles/main.css`

### 前端 Tests

- Create: `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`
- Create: `egg-user-web/src/pages/ProductsPage.product-assets.test.js`
- Modify: `egg-user-web/src/pages/TasksPage.test.js`
- Modify: `egg-user-web/src/components/GeneratorWorkspace.story.test.js` only if workspace contract changes affect existing story tests.

### Documentation

- Modify: `docs/change-log.md`
- Create or Modify: `docs/feature-map.md` if present; if absent, add a product image section to `docs/change-log.md` only.

---

### Task 1: 数据库脚本和商品图 Domain

**Files:**
- Create: `sql/ai_product_image_generation.sql`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAsset.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetImage.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductImageTask.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductImageTaskRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetRenameRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiRequirementRewriteRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiRequirementRewriteResult.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetDetail.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductDraftDetail.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/domain/ai/product/AiProductImageDomainTest.java`

**Interfaces:**
- Produces: `AiProductAsset` with `id/userId/name/status/visible/assetType/targetPlatform/aspectRatio/quality/userRequirement/sourceProductId/coverImageId/lastTaskId`.
- Produces: `AiProductAssetImage` with `id/productAssetId/userId/taskId/imageRole/ossBucket/ossObjectKey/ossUrl/mimeType/fileSize/width/height/sortOrder/analysisRole`.
- Produces: `AiProductImageTask` with `id/productAssetId/userId/taskType/assetType/targetPlatform/currentNode/status/failureReason/cancelReason/analysisOutput/planningOutput/generationOutput/debugPrompts/dashscopeTaskId/modelProvider/modelName/modelParams`.
- Produces SQL tables used by mapper XML in Task 2.

- [ ] **Step 1: Write the failing domain test**

Create `egg-system/src/test/java/com/egg/commerce/system/domain/ai/product/AiProductImageDomainTest.java`:

```java
package com.egg.commerce.system.domain.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * 商品图领域对象测试，确保前后端和 Mapper 依赖的字段名稳定。
 */
class AiProductImageDomainTest
{
    @Test
    void productAssetKeepsDraftAndVisibilityFields()
    {
        AiProductAsset asset = new AiProductAsset();
        asset.setId("asset-1");
        asset.setUserId(99L);
        asset.setName("未命名商品");
        asset.setStatus("draft_uploaded");
        asset.setVisible(false);
        asset.setAssetType("main");
        asset.setTargetPlatform("抖音");
        asset.setAspectRatio("3:4");
        asset.setQuality("2K");
        asset.setUserRequirement("高端珠宝主图");

        assertEquals("asset-1", asset.getId());
        assertEquals(99L, asset.getUserId());
        assertEquals("draft_uploaded", asset.getStatus());
        assertFalse(asset.getVisible());
        assertEquals("main", asset.getAssetType());
        assertEquals("抖音", asset.getTargetPlatform());
        assertEquals("3:4", asset.getAspectRatio());
        assertEquals("高端珠宝主图", asset.getUserRequirement());
    }

    @Test
    void productImageTaskKeepsNodeOutputsAndProviderSnapshot()
    {
        AiProductImageTask task = new AiProductImageTask();
        task.setId("task-1");
        task.setProductAssetId("asset-1");
        task.setUserId(99L);
        task.setTaskType("product_image");
        task.setCurrentNode("生成中");
        task.setStatus("processing");
        task.setAnalysisOutput("{\"ok\":true}");
        task.setPlanningOutput("{\"imagePrompt\":{\"promptId\":\"main_01\"}}");
        task.setGenerationOutput("{}");
        task.setDashscopeTaskId("dashscope-task-1");
        task.setModelProvider("dashscope");
        task.setModelName("wan2.7-image-pro");

        assertEquals("product_image", task.getTaskType());
        assertEquals("生成中", task.getCurrentNode());
        assertEquals("{\"ok\":true}", task.getAnalysisOutput());
        assertEquals("dashscope-task-1", task.getDashscopeTaskId());
        assertEquals("wan2.7-image-pro", task.getModelName());
    }

    @Test
    void rewriteRequestAndResultKeepCurrentRequirementOnly()
    {
        AiRequirementRewriteRequest request = new AiRequirementRewriteRequest();
        request.setAssetType("detail");
        request.setTargetPlatform("抖音");
        request.setUserRequirement("突出规格和卖点");
        request.setProductAssetId("asset-1");

        AiRequirementRewriteResult result = new AiRequirementRewriteResult();
        result.setOptimizedRequirement("优化后的详情图要求");
        result.setDisplayTitle("优化后的主图要求");
        result.setImagePromptReady(true);

        assertEquals("detail", request.getAssetType());
        assertEquals("突出规格和卖点", request.getUserRequirement());
        assertEquals("优化后的详情图要求", result.getOptimizedRequirement());
        assertEquals(true, result.getImagePromptReady());
    }
}
```

- [ ] **Step 2: Run the domain test and verify it fails**

Run:

```powershell
mvn -pl egg-system -Dtest=AiProductImageDomainTest test
```

Expected: FAIL with compilation errors because `com.egg.commerce.system.domain.ai.product` classes do not exist.

- [ ] **Step 3: Add SQL migration**

Create `sql/ai_product_image_generation.sql`. Use PostgreSQL syntax:

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS ai_product_assets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'draft_uploaded',
    visible boolean NOT NULL DEFAULT false,
    asset_type varchar(30),
    target_platform varchar(100),
    aspect_ratio varchar(30),
    quality varchar(30),
    user_requirement text,
    source_product_id uuid,
    cover_image_id uuid,
    last_task_id uuid,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_product_assets IS '前台全品类商品图商品资产表，承载草稿、处理中、成功、失败和终止记录';
COMMENT ON COLUMN ai_product_assets.id IS '商品资产主键，使用 UUID 便于前后端传递';
COMMENT ON COLUMN ai_product_assets.user_id IS '创建该商品资产的前台用户 ID，所有查询必须按该字段隔离';
COMMENT ON COLUMN ai_product_assets.name IS '商品名称，可由 AI 从要求中提取，也可由用户重命名';
COMMENT ON COLUMN ai_product_assets.status IS '商品资产状态：draft_uploaded、processing、completed、failed、cancelled';
COMMENT ON COLUMN ai_product_assets.visible IS '是否在商品库展示，只有成功完成的记录为 true';
COMMENT ON COLUMN ai_product_assets.asset_type IS '本次生成类型：main 主图，detail 详情图';
COMMENT ON COLUMN ai_product_assets.target_platform IS '目标平台，首版实际使用抖音，字段保留多平台扩展';
COMMENT ON COLUMN ai_product_assets.aspect_ratio IS '生成图片比例，首版默认 3:4';
COMMENT ON COLUMN ai_product_assets.quality IS '生成清晰度，例如 2K';
COMMENT ON COLUMN ai_product_assets.user_requirement IS '用户填写的主图要求或详情图要求';
COMMENT ON COLUMN ai_product_assets.source_product_id IS '继续使用时的来源商品资产 ID';
COMMENT ON COLUMN ai_product_assets.cover_image_id IS '商品库封面图片 ID，通常为生成图';
COMMENT ON COLUMN ai_product_assets.last_task_id IS '最近一次关联的商品图任务 ID';
COMMENT ON COLUMN ai_product_assets.created_at IS '创建时间';
COMMENT ON COLUMN ai_product_assets.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_ai_product_assets_user_visible
    ON ai_product_assets(user_id, visible, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_product_assets_user_status
    ON ai_product_assets(user_id, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_product_asset_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_asset_id uuid NOT NULL,
    user_id bigint NOT NULL,
    task_id uuid,
    image_role varchar(30) NOT NULL,
    oss_bucket varchar(200) NOT NULL,
    oss_object_key varchar(500) NOT NULL,
    oss_url text NOT NULL,
    mime_type varchar(100),
    file_size bigint NOT NULL DEFAULT 0,
    width integer,
    height integer,
    sort_order integer NOT NULL DEFAULT 0,
    analysis_role varchar(100),
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_product_asset_images IS '前台商品图图片资产表，保存用户上传源图和 AI 生成图的 OSS 元数据';
COMMENT ON COLUMN ai_product_asset_images.id IS '图片记录主键';
COMMENT ON COLUMN ai_product_asset_images.product_asset_id IS '所属商品资产 ID';
COMMENT ON COLUMN ai_product_asset_images.user_id IS '图片所属前台用户 ID，用于权限隔离';
COMMENT ON COLUMN ai_product_asset_images.task_id IS '生成图关联任务 ID，源图可为空或在开始分析后绑定';
COMMENT ON COLUMN ai_product_asset_images.image_role IS '图片角色：source 用户上传源图，generated AI 生成图';
COMMENT ON COLUMN ai_product_asset_images.oss_bucket IS 'OSS bucket 名称';
COMMENT ON COLUMN ai_product_asset_images.oss_object_key IS 'OSS object key';
COMMENT ON COLUMN ai_product_asset_images.oss_url IS '图片访问 URL 或后端可代理 URL';
COMMENT ON COLUMN ai_product_asset_images.mime_type IS '图片 MIME 类型';
COMMENT ON COLUMN ai_product_asset_images.file_size IS '图片文件大小，单位字节';
COMMENT ON COLUMN ai_product_asset_images.width IS '图片宽度，首版可为空';
COMMENT ON COLUMN ai_product_asset_images.height IS '图片高度，首版可为空';
COMMENT ON COLUMN ai_product_asset_images.sort_order IS '多图排序';
COMMENT ON COLUMN ai_product_asset_images.analysis_role IS 'AI 自动识别的图片用途，例如正面、侧面、细节';
COMMENT ON COLUMN ai_product_asset_images.created_at IS '创建时间';
COMMENT ON COLUMN ai_product_asset_images.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_product_asset_images_object_key
    ON ai_product_asset_images(oss_object_key);

CREATE INDEX IF NOT EXISTS idx_ai_product_asset_images_asset_role
    ON ai_product_asset_images(product_asset_id, image_role, sort_order);

CREATE TABLE IF NOT EXISTS ai_product_image_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_asset_id uuid NOT NULL,
    user_id bigint NOT NULL,
    task_type varchar(50) NOT NULL DEFAULT 'product_image',
    asset_type varchar(30) NOT NULL,
    target_platform varchar(100) NOT NULL,
    current_node varchar(50) NOT NULL DEFAULT '输入',
    status varchar(30) NOT NULL DEFAULT 'processing',
    failure_reason text,
    cancel_reason text,
    analysis_prompt_template_id uuid,
    analysis_prompt_version integer,
    analysis_output jsonb NOT NULL DEFAULT '{}'::jsonb,
    planning_output jsonb NOT NULL DEFAULT '{}'::jsonb,
    generation_output jsonb NOT NULL DEFAULT '{}'::jsonb,
    debug_prompts jsonb NOT NULL DEFAULT '{}'::jsonb,
    dashscope_task_id varchar(200),
    model_provider varchar(50),
    model_name varchar(120),
    model_params jsonb NOT NULL DEFAULT '{}'::jsonb,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_product_image_tasks IS '前台全品类商品图生成任务表，保存节点进度、节点输出、模型任务 ID 和失败/终止状态';
COMMENT ON COLUMN ai_product_image_tasks.id IS '商品图任务主键';
COMMENT ON COLUMN ai_product_image_tasks.product_asset_id IS '关联商品资产 ID';
COMMENT ON COLUMN ai_product_image_tasks.user_id IS '创建任务的前台用户 ID';
COMMENT ON COLUMN ai_product_image_tasks.task_type IS '任务类型，商品图任务固定为 product_image';
COMMENT ON COLUMN ai_product_image_tasks.asset_type IS '生成类型：main 主图，detail 详情图';
COMMENT ON COLUMN ai_product_image_tasks.target_platform IS '目标平台';
COMMENT ON COLUMN ai_product_image_tasks.current_node IS '当前节点：输入、分析中、确认规划、生成中、完成';
COMMENT ON COLUMN ai_product_image_tasks.status IS '任务状态：processing、completed、failed、cancelled';
COMMENT ON COLUMN ai_product_image_tasks.failure_reason IS '失败原因短文本';
COMMENT ON COLUMN ai_product_image_tasks.cancel_reason IS '终止原因短文本';
COMMENT ON COLUMN ai_product_image_tasks.analysis_prompt_template_id IS '分析/规划阶段使用的提示词模板 ID';
COMMENT ON COLUMN ai_product_image_tasks.analysis_prompt_version IS '分析/规划阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_product_image_tasks.analysis_output IS 'AI 分析结构化输出 JSON';
COMMENT ON COLUMN ai_product_image_tasks.planning_output IS 'AI 规划和生图提示词结构化输出 JSON';
COMMENT ON COLUMN ai_product_image_tasks.generation_output IS '图片生成结果结构化输出 JSON';
COMMENT ON COLUMN ai_product_image_tasks.debug_prompts IS '调试用 JSON，保存渲染提示词和模型请求摘要';
COMMENT ON COLUMN ai_product_image_tasks.dashscope_task_id IS 'DashScope 原始任务 ID，只用于排障和重试';
COMMENT ON COLUMN ai_product_image_tasks.model_provider IS '图片生成模型供应商快照';
COMMENT ON COLUMN ai_product_image_tasks.model_name IS '图片生成模型名称快照';
COMMENT ON COLUMN ai_product_image_tasks.model_params IS '模型参数快照 JSON';
COMMENT ON COLUMN ai_product_image_tasks.started_at IS '任务开始执行时间';
COMMENT ON COLUMN ai_product_image_tasks.completed_at IS '任务成功完成时间';
COMMENT ON COLUMN ai_product_image_tasks.cancelled_at IS '任务终止时间';
COMMENT ON COLUMN ai_product_image_tasks.created_at IS '创建时间';
COMMENT ON COLUMN ai_product_image_tasks.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_ai_product_image_tasks_user_created
    ON ai_product_image_tasks(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_product_image_tasks_status
    ON ai_product_image_tasks(status, updated_at DESC);
```

- [ ] **Step 4: Add domain classes**

Implement POJOs with JavaBean getters/setters and field comments. Use `Boolean getVisible()` instead of primitive `boolean isVisible()` so MyBatis and JSON serialization remain simple. Use `String` for UUID ids, matching story task patterns.

- [ ] **Step 5: Run the domain test and compile**

Run:

```powershell
mvn -pl egg-system -Dtest=AiProductImageDomainTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 1**

```powershell
git add sql/ai_product_image_generation.sql egg-system/src/main/java/com/egg/commerce/system/domain/ai/product egg-system/src/test/java/com/egg/commerce/system/domain/ai/product/AiProductImageDomainTest.java
git commit -m "feat: add product image domain schema"
```

---

### Task 2: 商品资产 Mapper 和提示词查询扩展

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductAssetMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductAssetImageMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product/AiProductImageTaskMapper.java`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductAssetMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductAssetImageMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/product/AiProductImageTaskMapper.xml`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt/AiPromptTemplateMapper.java`
- Modify: `egg-system/src/main/resources/mapper/ai/prompt/AiPromptTemplateMapper.xml`

**Interfaces:**
- Produces: `AiProductAssetMapper.selectCurrentDraft(Long userId)`.
- Produces: `AiProductAssetMapper.selectVisibleAssetList(AiProductAsset query)`.
- Produces: `AiProductAssetImageMapper.selectImagesByAssetId(String productAssetId, Long userId)`.
- Produces: `AiProductImageTaskMapper.selectAiProductImageTaskById(String id)`.
- Produces: `AiPromptTemplateMapper.selectPublishedTemplateByNameAndCategory(String name, String category)`.

- [ ] **Step 1: Add mapper interfaces**

Create mapper methods with comments:

```java
AiProductAsset selectAiProductAssetById(String id);
AiProductAsset selectCurrentDraft(@Param("userId") Long userId);
List<AiProductAsset> selectVisibleAssetList(AiProductAsset query);
int insertAiProductAsset(AiProductAsset asset);
int updateAiProductAsset(AiProductAsset asset);
int deleteAiProductAssetById(@Param("id") String id, @Param("userId") Long userId);
```

```java
AiProductAssetImage selectAiProductAssetImageById(String id);
AiProductAssetImage selectImageByObjectKey(String ossObjectKey);
List<AiProductAssetImage> selectImagesByAssetId(@Param("productAssetId") String productAssetId, @Param("userId") Long userId);
List<AiProductAssetImage> selectImagesByTaskId(@Param("taskId") String taskId, @Param("userId") Long userId);
int insertAiProductAssetImage(AiProductAssetImage image);
int updateAiProductAssetImage(AiProductAssetImage image);
int deleteAiProductAssetImageById(@Param("id") String id, @Param("userId") Long userId);
int deleteImagesByAssetId(@Param("productAssetId") String productAssetId, @Param("userId") Long userId);
```

```java
AiProductImageTask selectAiProductImageTaskById(String id);
List<AiProductImageTask> selectAiProductImageTaskList(AiProductImageTask query);
int insertAiProductImageTask(AiProductImageTask task);
int updateAiProductImageTask(AiProductImageTask task);
```

- [ ] **Step 2: Extend prompt mapper**

Add to `AiPromptTemplateMapper.java`:

```java
/** 按模板名称和分类查询最新已发布模板，供商品图主图、详情图和 AI 帮写运行时使用。 */
AiPromptTemplate selectPublishedTemplateByNameAndCategory(@Param("name") String name,
        @Param("category") String category);
```

Add XML:

```xml
<select id="selectPublishedTemplateByNameAndCategory" resultMap="AiPromptTemplateResult">
    <include refid="selectAiPromptTemplateVo"/>
    where status = 'published'
      and name = #{name}
      and category = #{category}
    order by version desc, updated_at desc, created_at desc
    limit 1
</select>
```

- [ ] **Step 3: Add mapper XML**

Use the story mapper style: define a `resultMap`, a reusable `select...Vo` SQL block, explicit casts for UUID fields, and `debug_prompts::text` style casts for JSONB fields. For JSONB update fields use `cast(#{analysisOutput} as jsonb)`.

- [ ] **Step 4: Compile mapper changes**

Run:

```powershell
mvn -pl egg-system -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product egg-system/src/main/resources/mapper/ai/product egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt/AiPromptTemplateMapper.java egg-system/src/main/resources/mapper/ai/prompt/AiPromptTemplateMapper.xml
git commit -m "feat: add product image mappers"
```

---

### Task 3: 通用 AI 对象存储接口

**Files:**
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiObjectStorageService.java`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiStoredObject.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiStoredObjectSummary.java`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/impl/AiAliOssObjectStorageServiceImpl.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/storage/AiAliOssObjectStorageServiceImplTest.java`

**Interfaces:**
- Produces: `uploadProductSourceImage(MultipartFile file, Long userId)`.
- Produces: `uploadGeneratedImage(byte[] bytes, String extension, Long userId)`.
- Produces: `copyProductImageObject(String sourceObjectKey, Long userId)`.
- Produces: `listProductImageObjects(Date olderThan)`.
- Keeps: `uploadKnowledgeFile(MultipartFile file)` behavior for knowledge module.

- [ ] **Step 1: Write source-level storage tests**

Create a test that verifies method names and object key prefixes exist without calling real OSS:

```java
package com.egg.commerce.system.service.ai.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AiAliOssObjectStorageServiceImplTest
{
    @Test
    void objectStorageInterfaceExposesProductImageMethods()
    {
        assertTrue(hasMethod("uploadProductSourceImage"));
        assertTrue(hasMethod("uploadGeneratedImage"));
        assertTrue(hasMethod("copyProductImageObject"));
        assertTrue(hasMethod("listProductImageObjects"));
        assertTrue(hasMethod("deleteObject"));
    }

    private boolean hasMethod(String name)
    {
        return Arrays.stream(AiObjectStorageService.class.getMethods())
                .map(Method::getName)
                .anyMatch(name::equals);
    }
}
```

- [ ] **Step 2: Run storage test and verify it fails**

```powershell
mvn -pl egg-system -Dtest=AiAliOssObjectStorageServiceImplTest test
```

Expected: FAIL because product image methods do not exist.

- [ ] **Step 3: Extend storage interface**

Add:

```java
AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId);
AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId);
AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId);
List<AiStoredObjectSummary> listProductImageObjects(Date olderThan);
byte[] downloadObject(String objectKey);
```

Import `java.util.Date`, `java.util.List`.

- [ ] **Step 4: Add stored object summary**

Create `AiStoredObjectSummary` with fields:

```java
private String bucket;
private String objectKey;
private String url;
private long size;
private Date lastModified;
```

- [ ] **Step 5: Implement AliOSS methods**

Keep `uploadKnowledgeFile` unchanged. Add product key builders:

```java
private String buildProductSourceObjectKey(String objectPrefix, Long userId, String extension)
{
    return buildProductObjectKey(objectPrefix, "source", userId, extension);
}

private String buildGeneratedObjectKey(String objectPrefix, Long userId, String extension)
{
    return buildProductObjectKey(objectPrefix, "generated", userId, extension);
}

private String buildProductObjectKey(String objectPrefix, String type, Long userId, String extension)
{
    String normalizedPrefix = StringUtils.strip(StringUtils.defaultIfBlank(objectPrefix, "ai"), "/");
    StringBuilder objectKey = new StringBuilder();
    objectKey.append(normalizedPrefix)
            .append("/product-images/")
            .append(type)
            .append("/")
            .append(userId == null ? "anonymous" : userId)
            .append("/")
            .append(DateUtils.datePath())
            .append("/")
            .append(UUID.randomUUID());
    if (StringUtils.isNotBlank(extension))
    {
        objectKey.append(".").append(extension);
    }
    return objectKey.toString();
}
```

For `uploadGeneratedImage`, use `ByteArrayInputStream`. For `copyProductImageObject`, use AliOSS `copyObject` from same bucket to a new generated/source prefix; copy source images as `source`.

- [ ] **Step 6: Run storage test**

```powershell
mvn -pl egg-system -Dtest=AiAliOssObjectStorageServiceImplTest test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 3**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/storage egg-system/src/test/java/com/egg/commerce/system/service/ai/storage/AiAliOssObjectStorageServiceImplTest.java
git commit -m "feat: extend object storage for product images"
```

---

### Task 4: 商品资产 Service、草稿、商品库和复用

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductAssetService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductAssetServiceImpl.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductAssetServiceImplTest.java`

**Interfaces:**
- Produces: `AiProductDraftDetail selectCurrentDraft(Long userId)`.
- Produces: `AiProductDraftDetail uploadDraftImage(Long userId, MultipartFile file)`.
- Produces: `void deleteDraftImage(Long userId, String imageId)`.
- Produces: `void clearCurrentDraft(Long userId)`.
- Produces: `List<AiProductAsset> selectVisibleAssets(Long userId)`.
- Produces: `AiProductAssetDetail selectAssetDetail(Long userId, String assetId)`.
- Produces: `AiProductAsset renameAsset(Long userId, String assetId, String name)`.
- Produces: `AiProductDraftDetail reuseAsset(Long userId, String assetId)`.
- Produces: `AiProductAssetDetail deleteAsset(Long userId, String assetId)`.

- [ ] **Step 1: Write service tests with fake mappers/storage**

Cover:

```java
@Test
void uploadFirstImageCreatesInvisibleDraft()
```

Assertions:

- `asset.status == draft_uploaded`
- `asset.visible == false`
- one source image inserted
- object storage called once

Cover:

```java
@Test
void reuseCompletedAssetCopiesSourceObjectsIntoNewDraft()
```

Assertions:

- new asset has `sourceProductId` = original id
- new asset `visible=false`
- storage `copyProductImageObject` called for source images

Cover:

```java
@Test
void deleteVisibleAssetDeletesAllOssObjectsAndRows()
```

Assertions:

- storage delete called for source and generated object keys
- image rows deleted
- asset row deleted

- [ ] **Step 2: Run service tests and verify they fail**

```powershell
mvn -pl egg-system -Dtest=AiProductAssetServiceImplTest test
```

Expected: FAIL because service does not exist.

- [ ] **Step 3: Implement service constants**

Use constants:

```java
private static final String STATUS_DRAFT_UPLOADED = "draft_uploaded";
private static final String STATUS_PROCESSING = "processing";
private static final String STATUS_COMPLETED = "completed";
private static final String STATUS_FAILED = "failed";
private static final String STATUS_CANCELLED = "cancelled";
private static final String ROLE_SOURCE = "source";
private static final String ROLE_GENERATED = "generated";
private static final String DEFAULT_PRODUCT_NAME_PREFIX = "未命名商品-";
```

- [ ] **Step 4: Implement draft creation**

`uploadDraftImage`:

1. validate `userId` and image file.
2. get current draft by user.
3. create asset if missing.
4. upload image via `uploadProductSourceImage`.
5. insert source image row.
6. return draft detail.

- [ ] **Step 5: Implement visible asset list and detail**

`selectVisibleAssets` queries only current user and visible completed rows. `selectAssetDetail` verifies ownership, loads images, separates `sourceImages` and `generatedImages`.

- [ ] **Step 6: Implement rename**

Reject blank names with `ServiceException("商品名称不能为空")`. Only update owned asset.

- [ ] **Step 7: Implement reuse**

For each source image from original asset:

1. call `copyProductImageObject`.
2. insert new source image row under new draft.
3. copy `assetType/targetPlatform/aspectRatio/quality/userRequirement`.
4. set `sourceProductId`.

- [ ] **Step 8: Implement delete**

Load owned asset images first. Delete OSS objects one by one; then delete image rows and asset row. If OSS delete fails, throw `ServiceException` and do not delete DB rows.

- [ ] **Step 9: Run service tests**

```powershell
mvn -pl egg-system -Dtest=AiProductAssetServiceImplTest test
```

Expected: PASS.

- [ ] **Step 10: Commit Task 4**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductAssetService.java egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductAssetServiceImpl.java egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductAssetServiceImplTest.java
git commit -m "feat: add product asset service"
```

---

### Task 5: DashScope 图片生成网关

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationResult.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/AiImageGenerationService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/impl/AiDashScopeImageGenerationServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/impl/AiMockImageGenerationServiceImpl.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/model/AiImageGenerationServiceTest.java`
- Modify: `egg-admin/src/main/resources/application.yml`
- Modify: `.env.example`

**Interfaces:**
- Produces: `AiImageGenerationService.generate(AiImageGenerationRequest request): AiImageGenerationResult`.
- Consumes: prompt, negative prompt, reference image URLs, model, aspect ratio, quality.
- Produces result with provider/model/taskId/imageUrls/rawResponse.

- [ ] **Step 1: Write gateway tests**

Test request/result POJOs and mock service:

```java
@Test
void mockImageGenerationReturnsDeterministicResult()
{
    AiImageGenerationRequest request = new AiImageGenerationRequest();
    request.setStage("生成中");
    request.setPositivePrompt("clean product image");
    request.setNegativePrompt("watermark");
    request.setReferenceImageUrls(List.of("https://oss/source.png"));
    request.setAspectRatio("3:4");
    request.setQuality("2K");

    AiImageGenerationResult result = new AiMockImageGenerationServiceImpl().generate(request);

    assertEquals("mock", result.getProvider());
    assertEquals("mock-image-model", result.getModel());
    assertEquals("mock-image-task", result.getTaskId());
    assertEquals(1, result.getImageUrls().size());
}
```

- [ ] **Step 2: Run gateway tests and verify failure**

```powershell
mvn -pl egg-system -Dtest=AiImageGenerationServiceTest test
```

Expected: FAIL because image gateway classes do not exist.

- [ ] **Step 3: Implement request/result/interface**

`AiImageGenerationRequest` fields:

```java
private String stage;
private String positivePrompt;
private String negativePrompt;
private List<String> referenceImageUrls;
private String aspectRatio;
private String quality;
private String provider;
private String model;
```

`AiImageGenerationResult` fields:

```java
private String provider;
private String model;
private String taskId;
private List<String> imageUrls;
private String rawResponse;
```

- [ ] **Step 4: Implement mock service**

Only active when `egg.ai.model-gateway.image.provider=mock`. Return a deterministic placeholder URL `https://example.invalid/mock-product-image.png`. This is for tests/local wiring only; final manual verification must use DashScope.

- [ ] **Step 5: Implement DashScope service**

Use JDK `HttpClient` and FastJSON/Jackson already in project. Behavior:

1. Resolve config values from `egg.ai.model-gateway.image.*` and env vars.
2. Throw `ServiceException` if API key is blank.
3. Submit async image task.
4. Poll task result until success/failure/timeout.
5. Return image URLs from response.

Keep provider details inside this class; business task service only calls `AiImageGenerationService`.

- [ ] **Step 6: Add config**

In `application.yml`:

```yaml
image:
  provider: ${EGG_MODEL_IMAGE_PROVIDER:dashscope}
  model: ${EGG_MODEL_IMAGE_NAME:wan2.7-image-pro}
  endpoint: ${EGG_DASHSCOPE_ENDPOINT:https://dashscope.aliyuncs.com}
  api-key: ${EGG_DASHSCOPE_API_KEY:}
```

In `.env.example`:

```env
EGG_MODEL_IMAGE_PROVIDER=dashscope
EGG_MODEL_IMAGE_NAME=wan2.7-image-pro
EGG_DASHSCOPE_ENDPOINT=https://dashscope.aliyuncs.com
EGG_DASHSCOPE_API_KEY=
```

- [ ] **Step 7: Run gateway tests**

```powershell
mvn -pl egg-system -Dtest=AiImageGenerationServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Commit Task 5**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/model egg-system/src/test/java/com/egg/commerce/system/service/ai/model/AiImageGenerationServiceTest.java egg-admin/src/main/resources/application.yml .env.example
git commit -m "feat: add dashscope image generation gateway"
```

---

### Task 6: 商品图任务 Service、节点流转、重试和终止

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java`

**Interfaces:**
- Produces: `AiProductImageTask createTask(Long userId, AiProductImageTaskRequest request)`.
- Produces: `AiProductImageTask selectTaskDetail(String taskId, Long userId)`.
- Produces: `List<AiProductImageTask> selectTaskList(Long userId)`.
- Produces: `AiProductImageTask retryTask(String taskId, Long userId)`.
- Produces: `AiProductImageTask cancelTask(String taskId, Long userId)`.
- Produces: `void executeTask(String taskId)`.

- [ ] **Step 1: Write service tests**

Use fake mappers, fake storage, fake text generation, fake image generation, and immediate executor. Cover:

1. create task moves asset to processing and runs `分析中 -> 确认规划 -> 生成中 -> 完成`.
2. generation failure keeps analysis/planning outputs.
3. retry from generation does not re-run analysis/planning.
4. cancel marks task cancelled and prevents completion.

Test method names:

```java
void createTaskRunsAnalysisPlanningGenerationAndPublishesAsset()
void generationFailureKeepsAnalysisAndPlanningForRetry()
void retryGenerationFailureStartsAtGenerationNode()
void cancelProcessingTaskPreventsRetryAndVisibility()
```

- [ ] **Step 2: Run service tests and verify failure**

```powershell
mvn -pl egg-system -Dtest=AiProductImageTaskServiceImplTest test
```

Expected: FAIL because task service does not exist.

- [ ] **Step 3: Implement task constants**

```java
private static final String TASK_TYPE_PRODUCT_IMAGE = "product_image";
private static final String STATUS_PROCESSING = "processing";
private static final String STATUS_COMPLETED = "completed";
private static final String STATUS_FAILED = "failed";
private static final String STATUS_CANCELLED = "cancelled";
private static final String NODE_INPUT = "输入";
private static final String NODE_ANALYSIS = "分析中";
private static final String NODE_PLANNING = "确认规划";
private static final String NODE_GENERATING = "生成中";
private static final String NODE_COMPLETE = "完成";
private static final String TEMPLATE_CATEGORY_IMAGE = "AI生图";
private static final String TEMPLATE_CATEGORY_REWRITE = "AI优化提示词";
private static final String TEMPLATE_MAIN = "主图系统提示词";
private static final String TEMPLATE_DETAIL = "详情图系统提示词";
```

- [ ] **Step 4: Implement createTask**

Validate:

- user logged in.
- request exists.
- asset belongs to user.
- asset has at least one source image.
- asset is `draft_uploaded`.

Then:

- update asset fields from request.
- set asset `status=processing`, `visible=false`.
- create task `currentNode=输入`, `status=processing`.
- submit async execution.

- [ ] **Step 5: Implement execution**

Execution order:

1. `runAnalysisStage`
2. `runPlanningStage`
3. `runGenerationStage`
4. `completeTask`

At the beginning and end of each stage call `ensureNotCancelled(taskId)`.

- [ ] **Step 6: Implement analysis/planning prompt rendering**

For `assetType=main`, use `主图系统提示词`; for `detail`, use `详情图系统提示词`. Render variables from request, source image summary, platform rules, category rules, model adapter rules. For tests, fake prompt body can contain `{userRequirement}` and `{sourceImageAnalysis}`.

Parse model output as JSON string and save:

- analysis part to `analysis_output`
- planning part to `planning_output`

If model output cannot parse, throw `ServiceException("商品图规划输出不是合法JSON")`.

- [ ] **Step 7: Implement generation**

Read `positivePrompt` and `negativePrompt` from `planning_output`. Call `AiImageGenerationService.generate`. Download returned image URL bytes, save with `uploadGeneratedImage`, insert generated image row, save `generation_output`.

In tests, fake image service returns `https://example.invalid/generated.png` and fake downloader can be a private seam implemented as protected method:

```java
protected byte[] downloadGeneratedImage(String url)
{
    // production uses URL stream; tests subclass and return byte[].
}
```

- [ ] **Step 8: Implement retry**

Reject:

- completed
- processing
- cancelled

Resolve retry node:

- if failed at `分析中`, clear analysis/planning/generation and run from analysis.
- if failed at `确认规划`, keep analysis, clear planning/generation and run from planning.
- if failed at `生成中`, keep analysis/planning, clear generation and run from generation.

- [ ] **Step 9: Implement cancel**

Only processing tasks can cancel. Set:

```java
task.setStatus("cancelled");
task.setCancelReason("用户已终止任务");
task.setCancelledAt(new Date());
asset.setStatus("cancelled");
asset.setVisible(false);
```

Do not delete OSS files.

- [ ] **Step 10: Run service tests**

```powershell
mvn -pl egg-system -Dtest=AiProductImageTaskServiceImplTest test
```

Expected: PASS.

- [ ] **Step 11: Commit Task 6**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskService.java egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java
git commit -m "feat: add product image task workflow"
```

---

### Task 7: AI 帮写 Service

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductRequirementRewriteServiceImpl.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteServiceImplTest.java`

**Interfaces:**
- Produces: `AiRequirementRewriteResult rewrite(Long userId, AiRequirementRewriteRequest request)`.
- Consumes: `AiPromptTemplateMapper.selectPublishedTemplateByNameAndCategory("AI帮写优化提示词", "AI优化提示词")`.
- Consumes: `AiTextGenerationService.generate`.

- [ ] **Step 1: Write rewrite service test**

Fake prompt template body:

```json
{"format":"markdown","body":"rewrite {targetPlatform} {userRequirement}"}
```

Fake text service returns:

```json
{"optimizedRequirement":"优化后的主图要求","displayTitle":"优化后的主图要求","imagePromptReady":true,"missingQuestions":[],"safetyNotes":[]}
```

Assert `optimizedRequirement` is parsed.

- [ ] **Step 2: Run test and verify failure**

```powershell
mvn -pl egg-system -Dtest=AiProductRequirementRewriteServiceImplTest test
```

Expected: FAIL because rewrite service does not exist.

- [ ] **Step 3: Implement rewrite service**

Validate nonblank `userRequirement`. Load published template. Render variables:

- `{targetPlatform}`
- `{productCategory}` as `自动识别`
- `{productName}` from asset name if `productAssetId` exists.
- `{sellingPoints}` empty string.
- `{targetAudience}` empty string.
- `{userRequirement}`
- `{platformRules}` use first version hardcoded summary for 抖音 until RAG is added.

Parse JSON and return `AiRequirementRewriteResult`.

- [ ] **Step 4: Run rewrite test**

```powershell
mvn -pl egg-system -Dtest=AiProductRequirementRewriteServiceImplTest test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 7**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteService.java egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductRequirementRewriteServiceImpl.java egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteServiceImplTest.java
git commit -m "feat: add product image requirement rewrite"
```

---

### Task 8: Controller 接口和下载

**Files:**
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductAssetController.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductImageTaskController.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product/AiProductRequirementRewriteController.java`

**Interfaces:**
- Produces HTTP endpoints under `/ai/product-assets`, `/ai/product-image/tasks`, `/ai/product-image/requirement/rewrite`.
- Consumes services from Tasks 4, 6, 7.

- [ ] **Step 1: Implement asset controller**

Endpoints:

```java
@GetMapping("/draft")
public AjaxResult draft()

@PostMapping("/draft/images")
public AjaxResult uploadDraftImage(@RequestParam("file") MultipartFile file)

@DeleteMapping("/draft/images/{imageId}")
public AjaxResult deleteDraftImage(@PathVariable String imageId)

@DeleteMapping("/draft")
public AjaxResult clearDraft()

@GetMapping
public AjaxResult list()

@GetMapping("/{id}")
public AjaxResult detail(@PathVariable String id)

@PutMapping("/{id}/name")
public AjaxResult rename(@PathVariable String id, @RequestBody AiProductAssetRenameRequest request)

@PostMapping("/{id}/reuse")
public AjaxResult reuse(@PathVariable String id)

@GetMapping("/{id}/download")
public void download(@PathVariable String id, HttpServletResponse response)

@DeleteMapping("/{id}")
public AjaxResult delete(@PathVariable String id)
```

For download, stream object bytes using storage service through product asset service or a dedicated method. Set attachment header with a safe file name.

- [ ] **Step 2: Implement task controller**

Endpoints:

```java
@PostMapping
public AjaxResult create(@Validated @RequestBody AiProductImageTaskRequest request)

@GetMapping
public AjaxResult list()

@GetMapping("/{id}")
public AjaxResult detail(@PathVariable String id)

@PostMapping("/{id}/retry")
public AjaxResult retry(@PathVariable String id)

@PostMapping("/{id}/cancel")
public AjaxResult cancel(@PathVariable String id)
```

- [ ] **Step 3: Implement rewrite controller**

Endpoint:

```java
@PostMapping
public AjaxResult rewrite(@RequestBody AiRequirementRewriteRequest request)
```

- [ ] **Step 4: Compile admin**

```powershell
mvn -pl egg-admin -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit Task 8**

```powershell
git add egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product
git commit -m "feat: expose product image APIs"
```

---

### Task 9: 清理服务和 Spring 定时任务

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageCleanupServiceImpl.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/task/ProductImageCleanupTask.java`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupServiceImplTest.java`

**Interfaces:**
- Produces: `int cleanExpiredDraftsAndOrphans()`.
- Consumes: storage `listProductImageObjects(Date olderThan)`, image mapper `selectImageByObjectKey`, asset mapper current expired drafts.

- [ ] **Step 1: Write cleanup tests**

Cover:

```java
void cleanupDeletesExpiredDraftAssetImages()
void cleanupDeletesOssObjectWhenDatabaseHasNoImageRow()
void cleanupDoesNotDeleteFailedCancelledOrCompletedObjects()
```

- [ ] **Step 2: Run cleanup tests and verify failure**

```powershell
mvn -pl egg-system -Dtest=AiProductImageCleanupServiceImplTest test
```

Expected: FAIL because cleanup service does not exist.

- [ ] **Step 3: Add mapper query for expired drafts**

Add to asset mapper:

```java
List<AiProductAsset> selectExpiredDrafts(@Param("expireBefore") Date expireBefore);
```

SQL:

```xml
where status = 'draft_uploaded'
  and visible = false
  and updated_at &lt; #{expireBefore}
```

- [ ] **Step 4: Implement cleanup service**

Use 12 hours:

```java
private static final long DRAFT_EXPIRE_MILLIS = 12L * 60L * 60L * 1000L;
```

Delete expired drafts and images via storage then DB. For orphan OSS objects, list older-than cutoff and delete when `selectImageByObjectKey` returns null.

- [ ] **Step 5: Add Spring task**

Create `ProductImageCleanupTask`:

```java
@Component("productImageCleanupTask")
public class ProductImageCleanupTask
{
    private final AiProductImageCleanupService cleanupService;

    public ProductImageCleanupTask(AiProductImageCleanupService cleanupService)
    {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${egg.ai.product-image.cleanup-cron:0 30 * * * ?}")
    public void cleanExpiredDraftsAndOrphans()
    {
        cleanupService.cleanExpiredDraftsAndOrphans();
    }
}
```

Ensure scheduling is enabled in the app. If `@EnableScheduling` is not present, add it to the main admin application class or a small config class.

- [ ] **Step 6: Run cleanup tests**

```powershell
mvn -pl egg-system -Dtest=AiProductImageCleanupServiceImplTest test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 9**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupService.java egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageCleanupServiceImpl.java egg-admin/src/main/java/com/egg/commerce/web/task/ProductImageCleanupTask.java egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageCleanupServiceImplTest.java
git commit -m "feat: clean product image drafts and oss orphans"
```

---

### Task 10: 前端 API 封装和上传工作台契约

**Files:**
- Create: `egg-user-web/src/api/productImage.js`
- Modify: `egg-user-web/src/components/GeneratorWorkspace.vue`
- Create: `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`

**Interfaces:**
- Produces frontend API functions: `getProductDraft`, `uploadDraftImage`, `deleteDraftImage`, `clearProductDraft`, `createProductImageTask`, `getProductImageTask`, `listProductImageTasks`, `retryProductImageTask`, `cancelProductImageTask`, `rewriteProductRequirement`, `listProductAssets`, `getProductAsset`, `renameProductAsset`, `reuseProductAsset`, `deleteProductAsset`, `downloadProductAsset`.
- `GeneratorWorkspace` emits upload/remove/generate/rewrite/cancel events or accepts callbacks so `ProductImagesPage` owns backend state.

- [ ] **Step 1: Write source test**

Create `ProductImagesPage.product-image.test.js` with assertions:

```js
test('product image page imports real API wrappers', () => {
  const page = readSource('./ProductImagesPage.vue')
  assert.match(page, /getProductDraft/)
  assert.match(page, /uploadDraftImage/)
  assert.match(page, /createProductImageTask/)
  assert.match(page, /cancelProductImageTask/)
  assert.match(page, /rewriteProductRequirement/)
})
```

Also assert workspace supports uploaded images from parent:

```js
const workspace = readSource('../components/GeneratorWorkspace.vue')
assert.match(workspace, /uploaded-images/)
assert.match(workspace, /@upload-image/)
assert.match(workspace, /@remove-image/)
assert.match(workspace, /@cancel/)
```

- [ ] **Step 2: Run frontend test and verify failure**

```powershell
node egg-user-web/src/pages/ProductImagesPage.product-image.test.js
```

Expected: FAIL.

- [ ] **Step 3: Add API wrapper**

Implement `productImage.js` using the same request/auth style as `story.js`. For uploads, use `FormData` and do not set `Content-Type` manually.

- [ ] **Step 4: Refactor GeneratorWorkspace upload contract**

Add props:

```js
uploadedImages: { type: Array, default: () => [] },
uploading: { type: Boolean, default: false },
showCancelButton: { type: Boolean, default: false }
```

Add emits:

```js
['generate', 'select-step', 'download', 'upload-image', 'remove-image', 'rewrite-requirement', 'cancel']
```

When files are selected, emit files to parent. Keep local object URL fallback only for pages that do not pass `uploadedImages`.

- [ ] **Step 5: Run existing story workspace tests**

```powershell
node egg-user-web/src/components/GeneratorWorkspace.story.test.js
```

Expected: PASS. If tests fail because prop names changed, update tests without weakening story behavior assertions.

- [ ] **Step 6: Run new product image source test**

```powershell
node egg-user-web/src/pages/ProductImagesPage.product-image.test.js
```

Expected: PASS.

- [ ] **Step 7: Commit Task 10**

```powershell
git add egg-user-web/src/api/productImage.js egg-user-web/src/components/GeneratorWorkspace.vue egg-user-web/src/pages/ProductImagesPage.product-image.test.js egg-user-web/src/components/GeneratorWorkspace.story.test.js
git commit -m "feat: add product image frontend api and upload contract"
```

---

### Task 11: 全品类商品图页面真实任务流和 AI 帮写

**Files:**
- Modify: `egg-user-web/src/pages/ProductImagesPage.vue`
- Modify: `egg-user-web/src/styles/main.css`
- Modify: `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`

**Interfaces:**
- Consumes `productImage.js` API.
- Produces page state for draft restore, upload, generate, poll, cancel, rewrite, download, completion notice.

- [ ] **Step 1: Extend source test**

Assert:

```js
assert.match(page, /const PRODUCT_IMAGE_STEPS = \['输入', '分析中', '确认规划', '生成中', '完成'\]/)
assert.match(page, /loadDraft/)
assert.match(page, /handleUploadImage/)
assert.match(page, /handleGenerate/)
assert.match(page, /startPolling/)
assert.match(page, /handleCancelTask/)
assert.match(page, /showCompletionNotice/)
assert.match(page, /rewriteDialogVisible/)
assert.match(page, /handleRewriteRequirement/)
```

- [ ] **Step 2: Implement page state**

Use refs:

```js
const draft = ref(null)
const uploadedImages = ref([])
const currentTask = ref(null)
const selectedStep = ref('输入')
const isUploading = ref(false)
const isCreating = ref(false)
const taskNotice = ref(null)
const rewriteDialogVisible = ref(false)
const rewriteResult = ref(null)
```

- [ ] **Step 3: Implement draft restore**

On mounted:

```js
await loadDraft()
```

Map backend images to workspace images:

```js
{
  id: image.id,
  name: image.analysisRole || image.ossObjectKey,
  url: image.ossUrl,
  uploaded: true
}
```

- [ ] **Step 4: Implement upload/remove**

`handleUploadImage(files)` uploads each file sequentially or with `Promise.allSettled`, then refreshes draft. On any failure, show `ElMessage.error`.

`handleRemoveImage(image)` calls `deleteDraftImage(image.id)` and refreshes draft.

- [ ] **Step 5: Implement generate**

Validate at least one uploaded image. If only one, show warning but continue. Send:

```js
{
  productAssetId: draft.value.id,
  assetType: form.assetType === '主图' ? 'main' : 'detail',
  targetPlatform: form.platform,
  aspectRatio: form.ratio,
  quality: form.quality,
  userRequirement: form.prompt
}
```

- [ ] **Step 6: Implement polling and result sections**

Map backend nodes to steps. For selected step:

- 输入: show source image summary and form params.
- 分析中: show `analysisOutput`.
- 确认规划: show `planningOutput`.
- 生成中/完成: show generated image and `generationOutput`.

- [ ] **Step 7: Implement cancel**

If task processing, call `cancelProductImageTask`, clear page state, show message: `当前任务已终止，如需查看请前往任务中心`.

- [ ] **Step 8: Implement AI rewrite dialog**

Call `rewriteProductRequirement`. Show small dialog with optimized requirement and buttons `替换` and `退出`.

- [ ] **Step 9: Implement completion notice**

When task status becomes `completed`, show bottom-right notice with action `去商品库查看`, route to `/products`.

- [ ] **Step 10: Run product page source test**

```powershell
node egg-user-web/src/pages/ProductImagesPage.product-image.test.js
```

Expected: PASS.

- [ ] **Step 11: Commit Task 11**

```powershell
git add egg-user-web/src/pages/ProductImagesPage.vue egg-user-web/src/styles/main.css egg-user-web/src/pages/ProductImagesPage.product-image.test.js
git commit -m "feat: wire product image generation page"
```

---

### Task 12: 任务中心支持商品图任务

**Files:**
- Modify: `egg-user-web/src/pages/TasksPage.vue`
- Modify: `egg-user-web/src/pages/TasksPage.test.js`
- Modify: `egg-user-web/src/styles/main.css`

**Interfaces:**
- Consumes story task API and product image task API separately.
- Produces combined task list with type-specific actions.

- [ ] **Step 1: Extend task center source tests**

Add assertions:

```js
assert.match(page, /listProductImageTasks/)
assert.match(page, /getProductImageTask/)
assert.match(page, /retryProductImageTask/)
assert.match(page, /cancelProductImageTask/)
assert.match(page, /product_image/)
assert.match(page, /已终止/)
assert.match(page, /图片已删除/)
```

- [ ] **Step 2: Run task test and verify failure**

```powershell
node egg-user-web/src/pages/TasksPage.test.js
```

Expected: FAIL.

- [ ] **Step 3: Load story and product tasks separately**

Use `Promise.allSettled([listStoryTasks(), listProductImageTasks()])`. Normalize rows:

```js
{
  id,
  taskKind: 'story' | 'product_image',
  name,
  typeLabel,
  targetPlatform,
  status,
  currentNode,
  failureReason,
  raw
}
```

- [ ] **Step 4: Add product task detail renderer**

Detail sections:

- 输入摘要
- AI 分析
- 确认规划
- 生成结果

Show image thumbnail when generated image exists. If deleted, show `图片已删除`.

- [ ] **Step 5: Add product actions**

- retry visible only when `status === 'failed'`
- cancel visible only when `status === 'processing'`
- download not in task center for product image; product download belongs to 商品库.

- [ ] **Step 6: Run task test**

```powershell
node egg-user-web/src/pages/TasksPage.test.js
```

Expected: PASS.

- [ ] **Step 7: Commit Task 12**

```powershell
git add egg-user-web/src/pages/TasksPage.vue egg-user-web/src/pages/TasksPage.test.js egg-user-web/src/styles/main.css
git commit -m "feat: show product image tasks in task center"
```

---

### Task 13: 商品库真实数据、详情、导航和继续使用

**Files:**
- Modify: `egg-user-web/src/pages/ProductsPage.vue`
- Modify: `egg-user-web/src/pages/ProductDetailPage.vue`
- Modify: `egg-user-web/src/layout/UserShell.vue`
- Modify: `egg-user-web/src/data/mock.js`
- Create: `egg-user-web/src/pages/ProductsPage.product-assets.test.js`
- Modify: `egg-user-web/src/styles/main.css`

**Interfaces:**
- Consumes product asset APIs.
- Produces product list/detail UI with actions: detail, rename, reuse, download, delete.

- [ ] **Step 1: Write product asset source test**

Assert:

```js
assert.match(page, /listProductAssets/)
assert.match(page, /renameProductAsset/)
assert.match(page, /reuseProductAsset/)
assert.match(page, /downloadProductAsset/)
assert.match(page, /deleteProductAsset/)
assert.match(nav, /商品库/)
assert.doesNotMatch(nav, /素材库/)
```

- [ ] **Step 2: Run product asset test and verify failure**

```powershell
node egg-user-web/src/pages/ProductsPage.product-assets.test.js
```

Expected: FAIL.

- [ ] **Step 3: Implement ProductsPage**

On mounted load visible assets. Table columns:

- 商品
- 类型
- 平台
- 最近生成
- 操作

Actions:

- 详情 routes to `/products/:id`
- 重命名 opens dialog
- 继续使用 calls API then routes to `/generate/product-images`
- 下载 calls file download
- 删除 confirms then calls API

- [ ] **Step 4: Implement ProductDetailPage**

Load detail from API. Show:

- uploaded source images
- generated image
- platform/type/requirement/time
- actions reuse/download/delete

Do not show analysis/planning.

- [ ] **Step 5: Update navigation**

Add 商品库 to primary navigation. Remove/hide 素材库 from visible nav. Keep `/assets` route unchanged.

- [ ] **Step 6: Run product asset test**

```powershell
node egg-user-web/src/pages/ProductsPage.product-assets.test.js
```

Expected: PASS.

- [ ] **Step 7: Commit Task 13**

```powershell
git add egg-user-web/src/pages/ProductsPage.vue egg-user-web/src/pages/ProductDetailPage.vue egg-user-web/src/layout/UserShell.vue egg-user-web/src/data/mock.js egg-user-web/src/pages/ProductsPage.product-assets.test.js egg-user-web/src/styles/main.css
git commit -m "feat: connect product asset library"
```

---

### Task 14: 文档、变更记录和全量验证

**Files:**
- Modify: `docs/change-log.md`
- Modify: `docs/feature-map.md` if file exists.

**Interfaces:**
- Produces maintenance note for product image generation feature.

- [ ] **Step 1: Update change log**

Add a `2026-07-15` entry:

```markdown
## 2026-07-15

- 新增全品类商品图真实生图首版：商品草稿、即时 OSS 上传、商品图任务、通义万相生成、商品库、任务中心、AI 帮写和清理任务。
```

- [ ] **Step 2: Update feature map if present**

If `docs/feature-map.md` exists, add:

```markdown
### 全品类商品图

- 前端入口：`egg-user-web/src/pages/ProductImagesPage.vue`
- 商品库：`egg-user-web/src/pages/ProductsPage.vue`
- 任务中心：`egg-user-web/src/pages/TasksPage.vue`
- 后端接口：`egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product`
- 后端服务：`egg-system/src/main/java/com/egg/commerce/system/service/ai/product`
- 数据表：`ai_product_assets`、`ai_product_asset_images`、`ai_product_image_tasks`
```

- [ ] **Step 3: Run focused backend tests**

```powershell
mvn -pl egg-system -Dtest=AiProductImageDomainTest,AiAliOssObjectStorageServiceImplTest,AiImageGenerationServiceTest,AiProductAssetServiceImplTest,AiProductImageTaskServiceImplTest,AiProductRequirementRewriteServiceImplTest,AiProductImageCleanupServiceImplTest test
```

Expected: PASS.

- [ ] **Step 4: Run backend compile**

```powershell
mvn -pl egg-admin -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Run frontend source tests**

```powershell
node egg-user-web/src/components/GeneratorWorkspace.story.test.js
node egg-user-web/src/pages/ProductImagesPage.product-image.test.js
node egg-user-web/src/pages/TasksPage.test.js
node egg-user-web/src/pages/ProductsPage.product-assets.test.js
```

Expected: PASS.

- [ ] **Step 6: Run frontend build**

```powershell
npm --prefix egg-user-web run build
```

Expected: PASS.

- [ ] **Step 7: Manual smoke with real DashScope**

Prerequisites:

- `EGG_DASHSCOPE_API_KEY` is configured.
- AliOSS credentials are configured.
- SQL `sql/ai_product_image_generation.sql` has been applied.
- Backend and user web are running.

Smoke path:

1. Login as front user.
2. Open `/generate/product-images`.
3. Upload one product image.
4. Confirm image uploads and survives refresh.
5. Click `开始分析`.
6. Watch nodes advance.
7. Confirm generated image appears.
8. Download image.
9. Open 商品库 and confirm record appears.
10. Click 继续使用 and confirm new draft is restored.
11. Open 任务中心 and confirm task nodes are visible.

- [ ] **Step 8: Commit Task 14**

```powershell
git add docs/change-log.md docs/feature-map.md
git commit -m "docs: record product image generation feature"
```

---

## Coverage Self-Review

- 数据底座：Task 1、Task 2 覆盖 SQL、Domain、Mapper。
- 即时上传和草稿恢复：Task 3、Task 4、Task 10、Task 11 覆盖。
- 商品库可见性和用户隔离：Task 4、Task 8、Task 13 覆盖。
- 通义万相真实生图：Task 5、Task 6 覆盖。
- 提示词模板读取：Task 2、Task 6、Task 7 覆盖，且不重复插入模板。
- 任务节点、失败重试、终止：Task 6、Task 8、Task 11、Task 12 覆盖。
- AI 帮写：Task 7、Task 8、Task 11 覆盖。
- OSS 清理：Task 3、Task 9 覆盖。
- 商品库继续使用复制新草稿：Task 4、Task 13 覆盖。
- 任务中心商品图展示：Task 12 覆盖。
- 导航隐藏素材库入口：Task 13 覆盖。
- 验证和文档：Task 14 覆盖。

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-15-product-image-generation.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks, fastest for this much surface area.
2. **Inline Execution** - execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
