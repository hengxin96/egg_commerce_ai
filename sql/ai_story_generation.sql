-- AI story generation migration for PostgreSQL.
-- This script creates the front-stage story generation task table and the internal platform rule table.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS ai_story_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL,
    task_type varchar(30) NOT NULL DEFAULT 'story',
    story_theme text NOT NULL,
    product_info text NOT NULL,
    target_platform varchar(100) NOT NULL,
    target_audience varchar(100) NOT NULL,
    story_style varchar(100) NOT NULL,
    story_duration varchar(100) NOT NULL,
    current_node varchar(50) NOT NULL DEFAULT '输入',
    status varchar(30) NOT NULL DEFAULT 'processing',
    failure_reason text,
    story_prompt_template_id uuid,
    story_prompt_version integer,
    story_output text,
    script_prompt_template_id uuid,
    script_prompt_version integer,
    script_output text,
    storyboard_prompt_template_id uuid,
    storyboard_prompt_version integer,
    storyboard_output text,
    storyboard_check_prompt_template_id uuid,
    storyboard_check_prompt_version integer,
    storyboard_check_output text,
    visual_prompt_template_id uuid,
    visual_prompt_version integer,
    visual_output text,
    debug_prompts jsonb NOT NULL DEFAULT '{}'::jsonb,
    platform_rule_id uuid,
    platform_rule_version varchar(50),
    model_provider varchar(50),
    model_name varchar(120),
    model_params jsonb NOT NULL DEFAULT '{}'::jsonb,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_story_tasks IS '前台剧情生成任务表，按固定节点保存用户输入、每阶段模型输出和任务进度';
COMMENT ON COLUMN ai_story_tasks.id IS '剧情任务主键，使用 UUID 便于前后端传递';
COMMENT ON COLUMN ai_story_tasks.user_id IS '创建任务的前台用户ID，用于任务查询、详情和下载权限隔离';
COMMENT ON COLUMN ai_story_tasks.task_type IS '任务类型，剧情任务固定为 story，为任务中心后续扩展保留';
COMMENT ON COLUMN ai_story_tasks.story_theme IS '用户输入的剧情主题，是剧情生成阶段的核心输入';
COMMENT ON COLUMN ai_story_tasks.product_info IS '用户输入的产品信息，模型生成剧情植入和视觉提示词时必须以此为事实来源';
COMMENT ON COLUMN ai_story_tasks.target_platform IS '目标平台字典值，同时用于匹配平台专属剧情生成提示词';
COMMENT ON COLUMN ai_story_tasks.target_audience IS '目标人群字典值，来自 story_target_audience';
COMMENT ON COLUMN ai_story_tasks.story_style IS '推荐风格字典值，来自 story_style';
COMMENT ON COLUMN ai_story_tasks.story_duration IS '剧情时长字典值，来自 story_duration';
COMMENT ON COLUMN ai_story_tasks.current_node IS '当前流程节点：输入、剧情生成、剧本生成、分镜生成、分镜校验、三视图/场景图、完成';
COMMENT ON COLUMN ai_story_tasks.status IS '任务状态：processing 表示执行中，completed 表示完成，failed 表示失败';
COMMENT ON COLUMN ai_story_tasks.failure_reason IS '失败原因短文本，完整异常栈只写入服务端日志';
COMMENT ON COLUMN ai_story_tasks.story_prompt_template_id IS '剧情生成阶段使用的 ai_prompt_templates 模板ID';
COMMENT ON COLUMN ai_story_tasks.story_prompt_version IS '剧情生成阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_story_tasks.story_output IS '剧情生成阶段的大模型输出内容';
COMMENT ON COLUMN ai_story_tasks.script_prompt_template_id IS '剧本生成阶段使用的 ai_prompt_templates 模板ID';
COMMENT ON COLUMN ai_story_tasks.script_prompt_version IS '剧本生成阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_story_tasks.script_output IS '剧本生成阶段的大模型输出内容';
COMMENT ON COLUMN ai_story_tasks.storyboard_prompt_template_id IS '分镜生成阶段使用的 ai_prompt_templates 模板ID';
COMMENT ON COLUMN ai_story_tasks.storyboard_prompt_version IS '分镜生成阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_story_tasks.storyboard_output IS '分镜生成阶段的大模型输出内容';
COMMENT ON COLUMN ai_story_tasks.storyboard_check_prompt_template_id IS '分镜校验压缩阶段使用的 ai_prompt_templates 模板ID';
COMMENT ON COLUMN ai_story_tasks.storyboard_check_prompt_version IS '分镜校验压缩阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_story_tasks.storyboard_check_output IS '分镜校验压缩后的大模型输出内容';
COMMENT ON COLUMN ai_story_tasks.visual_prompt_template_id IS '三视图/场景图提示词阶段使用的 ai_prompt_templates 模板ID';
COMMENT ON COLUMN ai_story_tasks.visual_prompt_version IS '三视图/场景图提示词阶段使用的提示词模板版本';
COMMENT ON COLUMN ai_story_tasks.visual_output IS '三视图提示词和场景图提示词阶段的大模型输出内容';
COMMENT ON COLUMN ai_story_tasks.debug_prompts IS '调试用JSON，按阶段保存渲染后的完整提示词，便于排查模型输出差异';
COMMENT ON COLUMN ai_story_tasks.platform_rule_id IS '本任务使用的平台画像和规则配置ID';
COMMENT ON COLUMN ai_story_tasks.platform_rule_version IS '本任务使用的平台画像和规则版本';
COMMENT ON COLUMN ai_story_tasks.model_provider IS '执行剧情链路的模型供应商快照，例如 deepseek 或 mock';
COMMENT ON COLUMN ai_story_tasks.model_name IS '执行剧情链路的模型名称快照，例如 deepseek-v4-pro';
COMMENT ON COLUMN ai_story_tasks.model_params IS '模型参数快照，例如 temperature、max_tokens 等';
COMMENT ON COLUMN ai_story_tasks.started_at IS '后台线程开始串行执行剧情任务的时间';
COMMENT ON COLUMN ai_story_tasks.completed_at IS '剧情任务成功完成的时间，失败任务为空';
COMMENT ON COLUMN ai_story_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN ai_story_tasks.updated_at IS '任务最近更新时间';

CREATE INDEX IF NOT EXISTS idx_ai_story_tasks_user_created
    ON ai_story_tasks(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_story_tasks_status
    ON ai_story_tasks(status, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_story_platform_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    platform varchar(100) NOT NULL,
    version varchar(50) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'draft',
    platform_profile text NOT NULL,
    category_rules text NOT NULL,
    source_summary text,
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_story_platform_rules IS '剧情生成平台画像和规则表，用于后端注入平台内容倾向、带货规则和风险底线';
COMMENT ON COLUMN ai_story_platform_rules.id IS '平台规则主键，使用 UUID 便于剧情任务记录规则快照';
COMMENT ON COLUMN ai_story_platform_rules.platform IS '平台名称，需与 story_platform 字典值保持一致';
COMMENT ON COLUMN ai_story_platform_rules.version IS '规则版本号，例如 2026-07-v1';
COMMENT ON COLUMN ai_story_platform_rules.status IS '规则状态：draft 草稿，published 已发布，archived 已归档';
COMMENT ON COLUMN ai_story_platform_rules.platform_profile IS '平台用户画像和内容倾向，用于替换提示词变量 {platformProfile}';
COMMENT ON COLUMN ai_story_platform_rules.category_rules IS '平台内容规则、带货植入规则和风险底线，用于替换提示词变量 {categoryRules}';
COMMENT ON COLUMN ai_story_platform_rules.source_summary IS '规则来源和维护说明，后续平台规则定时更新可继续补充';
COMMENT ON COLUMN ai_story_platform_rules.created_by IS '创建人ID，SQL初始化时可为管理员ID';
COMMENT ON COLUMN ai_story_platform_rules.created_at IS '规则创建时间';
COMMENT ON COLUMN ai_story_platform_rules.updated_at IS '规则最近更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_story_platform_rules_platform_version
    ON ai_story_platform_rules(platform, version);

CREATE INDEX IF NOT EXISTS idx_ai_story_platform_rules_published
    ON ai_story_platform_rules(platform, status, updated_at DESC);

INSERT INTO ai_story_platform_rules (
    platform, version, status, platform_profile, category_rules, source_summary, created_by
)
SELECT '抖音', '2026-07-v1', 'published',
       '内容节奏快，开头需要强钩子，前几秒要让用户知道冲突、悬念或看点。适合强情绪、强反转、强视觉记忆点的短剧情，但不能为了刺激制造低俗、暴力或争议价值观。电商内容可以更直接服务转化，但要避免硬广感、虚假宣传和夸张承诺。',
       '开头直接进入矛盾或结果倒置，少铺垫；每15-20秒设置一个情绪推进点；商品植入应短、准、自然。禁止夸大商品效果、伪造权威认证、诱导冲动购买，避免低俗擦边、极端对立和仇富仇穷。',
       '首版来自 AI剧情-平台画像与规则.md，后续应由后台规则维护或定时更新流程管理。',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM ai_story_platform_rules WHERE platform = '抖音' AND version = '2026-07-v1'
);

INSERT INTO ai_story_platform_rules (
    platform, version, status, platform_profile, category_rules, source_summary, created_by
)
SELECT '快手', '2026-07-v1', 'published',
       '内容更适合真实、接地气、熟人社会、人情味和朴素生活逻辑。角色语言可以更生活化，人物关系可以偏乡镇、家庭、邻里、夫妻、朋友、师徒、摊主顾客。',
       '冲突要直白但人物不能悬浮；多用饭桌、厨房、门口、小店、集市、车间、老家院子等日常场景。商品植入要像熟人推荐或生活顺手使用，避免粗俗辱骂、过度卖惨、低俗猎奇和虚假人设。',
       '首版来自 AI剧情-平台画像与规则.md，后续应由后台规则维护或定时更新流程管理。',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM ai_story_platform_rules WHERE platform = '快手' AND version = '2026-07-v1'
);

INSERT INTO ai_story_platform_rules (
    platform, version, status, platform_profile, category_rules, source_summary, created_by
)
SELECT '视频号', '2026-07-v1', 'published',
       '更适合熟人传播、家庭伦理、正向价值、稳定叙事和中老年用户可理解的情感冲突。30-70岁用户更容易接受直白、单线、善恶分明、结局正向的剧情。',
       '开场30秒内说明谁和谁冲突、因为什么冲突；善恶边界清楚但结局要正向。商品植入控制短，放在温情时刻、饭桌、探望、和解、照顾家人等场景，避免价值观争议和刺激性过强内容。',
       '首版来自 AI剧情-平台画像与规则.md，后续应由后台规则维护或定时更新流程管理。',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM ai_story_platform_rules WHERE platform = '视频号' AND version = '2026-07-v1'
);

INSERT INTO ai_story_platform_rules (
    platform, version, status, platform_profile, category_rules, source_summary, created_by
)
SELECT '小红书', '2026-07-v1', 'published',
       '更重真实体验、生活方式、种草感、审美氛围和个人视角。用户对硬广、夸张卖点和悬浮剧情更敏感，适合生活片段、情绪共鸣和轻种草。',
       '冲突可以更轻，像生活观察、关系微妙变化、使用前后心情变化。商品出现要像真实生活选择，不得伪装真实测评、虚构亲测效果或制造容貌焦虑、消费攀比和炫富。',
       '首版来自 AI剧情-平台画像与规则.md，后续应由后台规则维护或定时更新流程管理。',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM ai_story_platform_rules WHERE platform = '小红书' AND version = '2026-07-v1'
);
