-- AI knowledge base, sensitive word, and chat RAG migration for PostgreSQL.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS ai_knowledge_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name varchar(255) NOT NULL,
    file_ext varchar(32),
    mime_type varchar(120),
    file_size bigint NOT NULL DEFAULT 0,
    oss_bucket varchar(255),
    oss_object_key varchar(500),
    oss_url varchar(1000),
    vector_store varchar(50) NOT NULL DEFAULT 'milvus',
    vector_collection varchar(255),
    vector_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    access_roles varchar(255) NOT NULL DEFAULT 'admin',
    chunk_count integer NOT NULL DEFAULT 0,
    retry_count integer NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'processing',
    failure_reason text,
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    completed_at timestamp without time zone
);

ALTER TABLE ai_knowledge_documents
    ADD COLUMN IF NOT EXISTS access_roles varchar(255) NOT NULL DEFAULT 'admin';

CREATE TABLE IF NOT EXISTS ai_sensitive_categories (
    id bigserial PRIMARY KEY,
    category_name varchar(100) NOT NULL,
    description varchar(500),
    status varchar(30) NOT NULL DEFAULT 'enabled',
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_sensitive_words (
    id bigserial PRIMARY KEY,
    category_id bigint NOT NULL REFERENCES ai_sensitive_categories(id),
    word varchar(255) NOT NULL,
    match_type varchar(30) NOT NULL DEFAULT 'contains',
    status varchar(30) NOT NULL DEFAULT 'enabled',
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL,
    title varchar(255) NOT NULL,
    last_message_preview varchar(500),
    status varchar(30) NOT NULL DEFAULT 'active',
    message_count integer NOT NULL DEFAULT 0,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    last_message_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    user_id bigint NOT NULL,
    role varchar(30) NOT NULL,
    content text NOT NULL,
    model_name varchar(120),
    prompt_version varchar(120),
    rag_used boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_chat_retrieval_sources (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id uuid NOT NULL REFERENCES ai_chat_messages(id) ON DELETE CASCADE,
    document_id uuid REFERENCES ai_knowledge_documents(id),
    source_rank integer NOT NULL DEFAULT 1,
    chunk_index integer,
    source_title varchar(255),
    snippet text,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    score numeric(10, 6),
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

ALTER TABLE ai_chat_retrieval_sources
    ADD COLUMN IF NOT EXISTS metadata jsonb NOT NULL DEFAULT '{}'::jsonb;

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_knowledge_documents_object_key
    ON ai_knowledge_documents(oss_bucket, oss_object_key);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_documents_status
    ON ai_knowledge_documents(status);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_documents_access_roles
    ON ai_knowledge_documents(access_roles);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_documents_created_by
    ON ai_knowledge_documents(created_by, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_sensitive_categories_name
    ON ai_sensitive_categories(category_name);

CREATE INDEX IF NOT EXISTS idx_ai_sensitive_categories_status
    ON ai_sensitive_categories(status);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_sensitive_words_word_category
    ON ai_sensitive_words(word, category_id);

CREATE INDEX IF NOT EXISTS idx_ai_sensitive_words_category
    ON ai_sensitive_words(category_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user
    ON ai_chat_sessions(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session
    ON ai_chat_messages(session_id, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_chat_retrieval_sources_message
    ON ai_chat_retrieval_sources(message_id, source_rank);

-- Admin menu: AI配置 / 我的知识库
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2110, '我的知识库', 2100, 2, 'knowledge', 'ai/knowledge/index', '', '',
       1, 0, 'C', '0', '0', 'ai:knowledge:list', 'education',
       'admin', now(), '', NULL, 'AI知识库管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2110);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2111, '知识库列表', 2110, 1, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:knowledge:list', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:knowledge:list');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2112, '知识库上传', 2110, 2, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:knowledge:upload', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:knowledge:upload');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2113, '知识库删除', 2110, 3, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:knowledge:remove', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:knowledge:remove');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2114, '知识库重试', 2110, 4, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:knowledge:retry', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:knowledge:retry');

-- Admin menu: AI配置 / 敏感词管理
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2120, '敏感词管理', 2100, 3, 'sensitive', 'ai/sensitive/index', '', '',
       1, 0, 'C', '0', '0', 'ai:sensitive:word:list', 'dict',
       'admin', now(), '', NULL, 'AI敏感词管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2120);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2121, '敏感词分类列表', 2120, 1, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:category:list', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:category:list');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2122, '敏感词分类新增', 2120, 2, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:category:add', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:category:add');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2123, '敏感词分类编辑', 2120, 3, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:category:edit', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:category:edit');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2124, '敏感词分类删除', 2120, 4, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:category:remove', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:category:remove');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2125, '敏感词列表', 2120, 5, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:word:list', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:word:list');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2126, '敏感词新增', 2120, 6, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:word:add', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:word:add');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2127, '敏感词编辑', 2120, 7, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:word:edit', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:word:edit');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT 2128, '敏感词删除', 2120, 8, '#', '', '', '',
       1, 0, 'F', '0', '0', 'ai:sensitive:word:remove', '#',
       'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:sensitive:word:remove');
