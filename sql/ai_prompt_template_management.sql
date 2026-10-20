-- AI prompt template management migration for PostgreSQL.
-- Run this after the ai_prompt_templates table has been created.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'ai_prompt_templates'
          AND column_name = 'created_by'
          AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE ai_prompt_templates
            ALTER COLUMN created_by TYPE bigint USING NULL;
    END IF;
END $$;

UPDATE ai_prompt_templates
SET variables = '[]'::jsonb
WHERE variables IS NULL;

ALTER TABLE ai_prompt_templates
    ALTER COLUMN variables SET DEFAULT '[]'::jsonb;

COMMENT ON COLUMN ai_prompt_templates.created_by IS '创建人ID，保存后台用户表的数字ID';

-- Admin menu: AI配置 / 提示词模板.
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2100, 'AI配置', 0, 4, 'ai', NULL, '', '', 1, 0, 'M', '0', '0', '', 'edit',
       'admin', now(), '', NULL, 'AI业务配置目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2100);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2101, '提示词模板', 2100, 1, 'promptTemplate', 'ai/promptTemplate/index', '', '',
       1, 0, 'C', '0', '0', 'ai:promptTemplate:list', 'edit',
       'admin', now(), '', NULL, '提示词模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2101);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2102, '模板查询', 2101, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'ai:promptTemplate:query', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:promptTemplate:query');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2103, '模板新增', 2101, 2, '#', '', '', '', 1, 0, 'F', '0', '0',
       'ai:promptTemplate:add', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:promptTemplate:add');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2104, '模板修改', 2101, 3, '#', '', '', '', 1, 0, 'F', '0', '0',
       'ai:promptTemplate:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:promptTemplate:edit');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2105, '模板删除', 2101, 4, '#', '', '', '', 1, 0, 'F', '0', '0',
       'ai:promptTemplate:remove', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:promptTemplate:remove');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2106, '模板列表', 2101, 5, '#', '', '', '', 1, 0, 'F', '0', '0',
       'ai:promptTemplate:list', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:promptTemplate:list');
