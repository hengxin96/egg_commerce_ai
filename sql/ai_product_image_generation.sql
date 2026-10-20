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
