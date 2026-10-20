# AI 知识库、敏感词与 RAG 聊天 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `rag-ai` 中的知识库、敏感词管理和 RAG 聊天能力，按当前项目风格迁移到 `egg_commerce_ai`，并改造项目默认 AI 存储方向为阿里 OSS + Milvus + PostgreSQL 元数据。

**Architecture:** 后端保持模块化 Spring Boot 单体和普通 MyBatis 分层，所有新增业务放在 `ai` 包下并使用 `Ai` 类名前缀。后台使用 Egg Vue 2 + Element UI 管理端风格；前台 `/chat` 使用 `egg-user-web` 的 Vue 3 + Element Plus Picset 风格。外部依赖通过接口隔离：单元测试使用 fake OSS、fake Milvus、fake chat model，真实环境通过 Spring AI、DashScope、阿里 OSS 和 Milvus 配置接入。

**Tech Stack:** Java 17, Spring Boot 4.0.6, MyBatis, PageHelper, PostgreSQL, Vue 2 + Element UI (`egg-ui`), Vue 3 + Element Plus (`egg-user-web`), Spring AI, DashScope, Aliyun OSS SDK, Milvus vector store.

## Global Constraints

- 项目宪法必须改为：阿里 OSS 存文件，Milvus 存向量，PostgreSQL 存业务元数据。
- 不复制 `D:\Code_Workspace\rag-ai` 中的真实 API Key、OSS 密钥、Milvus 地址或其他凭据。
- `ai` 文件夹下的 Java 类名必须使用 `Ai` 前缀。
- 新增数据库表名必须使用 `ai_` 前缀。
- 后台管理端保持 Egg 管理端风格，不改成 Picset 风格。
- 前台 `/chat` 保持当前 Picset 风格。
- 敏感词首版只做后台分类和词库增删改查，不做文本检测接口。
- 知识库上传后必须异步解析、切分、向量化。
- RAG 聊天必须保存会话、消息和检索来源。
- 不引入积分、支付、租户、团队空间、A2A 或真实视频生成。

---

## File Structure

### Documentation

- Modify: `docs/project-constitution.md`  
  Update storage/vector-store principles from object storage/pgvector to Ali OSS/Milvus/PostgreSQL metadata.
- Modify: `docs/change-log.md`  
  Add a short plain-language entry for this implementation.
- Modify: `docs/feature-map.md`  
  Add paths for knowledge, sensitive, chat, OSS/Milvus services, pages, APIs, and SQL.
- Modify: `.env.example`  
  Add environment variable examples with empty or safe development values only.

### Backend Configuration

- Modify: `pom.xml`  
  Add version properties and dependency management for Spring AI if required.
- Modify: `egg-system/pom.xml`  
  Add dependencies used by system services: Spring AI document readers, vector store interfaces, Aliyun OSS SDK if services live here.
- Modify: `egg-admin/pom.xml`  
  Add web-facing dependencies only if controllers need them directly.
- Modify: `egg-admin/src/main/resources/application.yml`  
  Replace commented object-storage/pgvector sample with Ali OSS + Milvus sample.
- Modify: `egg-framework/src/main/java/com/egg/commerce/framework/config/properties/AiCommerceProperties.java`  
  Replace object-storage nested config with Ali OSS config and add Milvus config.

### Backend Infrastructure

- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiObjectStorageService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/AiStoredObject.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/impl/AiAliOssObjectStorageServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/AiDocumentParserService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/AiDocumentChunk.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/AiVectorStoreService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/AiVectorRecord.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl/AiSpringMilvusVectorStoreServiceImpl.java`

### Knowledge Backend

- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/knowledge/AiKnowledgeDocumentController.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/knowledge/AiKnowledgeDocument.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/knowledge/AiKnowledgeDocumentMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge/IAiKnowledgeDocumentService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge/impl/AiKnowledgeDocumentServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge/impl/AiKnowledgeVectorizationService.java`
- Create: `egg-system/src/main/resources/mapper/ai/knowledge/AiKnowledgeDocumentMapper.xml`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/knowledge/AiKnowledgeDocumentServiceImplTest.java`

### Sensitive Backend

- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive/AiSensitiveCategoryController.java`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive/AiSensitiveWordController.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive/AiSensitiveCategory.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive/AiSensitiveWord.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive/AiSensitiveCategoryMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive/AiSensitiveWordMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/IAiSensitiveCategoryService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/IAiSensitiveWordService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/impl/AiSensitiveCategoryServiceImpl.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/impl/AiSensitiveWordServiceImpl.java`
- Create: `egg-system/src/main/resources/mapper/ai/sensitive/AiSensitiveCategoryMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/sensitive/AiSensitiveWordMapper.xml`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/sensitive/AiSensitiveServiceTest.java`

### Chat Backend

- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat/AiChatController.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatSession.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatMessage.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatRetrievalSource.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatRequest.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatResponse.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatSessionMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatMessageMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatRetrievalSourceMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/IAiChatService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiChatServiceImpl.java`
- Create: `egg-system/src/main/resources/mapper/ai/chat/AiChatSessionMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/chat/AiChatMessageMapper.xml`
- Create: `egg-system/src/main/resources/mapper/ai/chat/AiChatRetrievalSourceMapper.xml`
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/chat/AiChatServiceImplTest.java`

### SQL

- Create: `sql/ai_knowledge_sensitive_rag.sql`

### Admin Frontend

- Create: `egg-ui/src/api/ai/knowledge.js`
- Create: `egg-ui/src/views/ai/knowledge/index.vue`
- Create: `egg-ui/src/api/ai/sensitive.js`
- Create: `egg-ui/src/views/ai/sensitive/index.vue`

### User Frontend

- Create: `egg-user-web/src/api/chat.js`
- Modify: `egg-user-web/src/pages/ChatPage.vue`
- Modify: `egg-user-web/src/styles/main.css`

---

## Interfaces

These interfaces are shared across tasks. Keep signatures stable unless a later task explicitly updates all consumers.

```java
package com.egg.commerce.system.service.ai.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AiObjectStorageService
{
    AiStoredObject uploadKnowledgeFile(MultipartFile file);

    void deleteObject(String objectKey);
}
```

```java
package com.egg.commerce.system.service.ai.storage;

public class AiStoredObject
{
    private String bucket;
    private String objectKey;
    private String url;
    private long size;
    private String mimeType;
}
```

```java
package com.egg.commerce.system.service.ai.rag;

import java.util.List;

public interface AiDocumentParserService
{
    List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content);
}
```

```java
package com.egg.commerce.system.service.ai.rag;

public class AiDocumentChunk
{
    private String chunkId;
    private String content;
    private int chunkIndex;
}
```

```java
package com.egg.commerce.system.service.ai.rag;

import java.util.List;

public interface AiVectorStoreService
{
    List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks);

    List<AiVectorRecord> search(String query, int topK, double minScore);

    void deleteVectors(List<String> vectorIds);
}
```

```java
package com.egg.commerce.system.service.ai.rag;

public class AiVectorRecord
{
    private String vectorId;
    private String documentId;
    private String sourceTitle;
    private String content;
    private double score;
}
```

---

## Task 1: 修订宪法、配置说明和导航文档

**Files:**
- Modify: `docs/project-constitution.md`
- Modify: `docs/change-log.md`
- Modify: `docs/feature-map.md`
- Modify: `.env.example`

**Interfaces:**
- Consumes: confirmed design in `docs/superpowers/specs/2026-07-08-ai-knowledge-sensitive-rag-design.md`
- Produces: project-level rules that later tasks must follow

- [ ] **Step 1: Update constitution storage language**

In `docs/project-constitution.md`, replace references that require object storage/pgvector as the default first-phase architecture:

```markdown
- PostgreSQL for business data
- Redis for login/session support, task queue/progress, rate limiting, duplicate-submit prevention, and short-lived chat context
- 阿里 OSS for original images, generated images, edited images, knowledge files, and export files
- Model gateway for all model and agent calls
- RAG using Milvus as the approved vector store, with PostgreSQL retaining business metadata and retrieval trace records
```

Also update image-output requirements to say generated images must be persisted to OSS instead of temporary model URLs:

```markdown
- Store generated images in Alibaba Cloud OSS instead of relying on temporary model URLs
```

- [ ] **Step 2: Update `.env.example`**

Add safe empty examples:

```env
EGG_ALIOSS_ENDPOINT=
EGG_ALIOSS_ACCESS_KEY_ID=
EGG_ALIOSS_ACCESS_KEY_SECRET=
EGG_ALIOSS_BUCKET=
EGG_ALIOSS_OBJECT_PREFIX=ai
EGG_MILVUS_HOST=localhost
EGG_MILVUS_PORT=19530
EGG_MILVUS_USERNAME=
EGG_MILVUS_PASSWORD=
EGG_MILVUS_DATABASE=default
EGG_MILVUS_COLLECTION=ai_knowledge_vectors
EGG_DASHSCOPE_API_KEY=
EGG_RAG_CHUNK_SIZE=800
EGG_RAG_CHUNK_OVERLAP=120
EGG_RAG_TOP_K=6
EGG_RAG_MIN_SCORE=0.62
```

- [ ] **Step 3: Update feature map**

Add sections for:

```markdown
### Knowledge Base Management
Backend paths:
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/knowledge`
- `egg-system/src/main/java/com/egg/commerce/system/.../ai/knowledge`
- Table: `ai_knowledge_documents`

Frontend paths:
- Admin page: `egg-ui/src/views/ai/knowledge`

### Sensitive Word Management
Backend paths:
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive`
- `egg-system/src/main/java/com/egg/commerce/system/.../ai/sensitive`
- Tables: `ai_sensitive_categories`, `ai_sensitive_words`

Frontend paths:
- Admin page: `egg-ui/src/views/ai/sensitive`

### RAG Chat
Backend paths:
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat`
- `egg-system/src/main/java/com/egg/commerce/system/.../ai/chat`
- Tables: `ai_chat_sessions`, `ai_chat_messages`, `ai_chat_retrieval_sources`

Frontend paths:
- User page: `egg-user-web/src/pages/ChatPage.vue`
- User API wrapper: `egg-user-web/src/api/chat.js`
```

- [ ] **Step 4: Update change log**

Append a short entry dated `2026-07-08`:

```markdown
### Planned AI Knowledge, Sensitive Word, And RAG Migration

Author: Codex

What changed:

- Documented the implementation direction for migrating knowledge base, sensitive word management, and RAG chat from `rag-ai`.
- Updated the project direction to use Alibaba Cloud OSS for AI files and Milvus for RAG vectors, with PostgreSQL retained for business metadata.

Why:

- The user confirmed the storage direction change and requested these features under the current AI module structure.

Main files:

- `docs/project-constitution.md`
- `docs/feature-map.md`
- `.env.example`
- `docs/change-log.md`
```

- [ ] **Step 5: Verify docs**

Run:

```powershell
Select-String -Path docs\project-constitution.md,docs\feature-map.md,.env.example -Pattern 'object storage|pgvector|PGVECTOR'
```

Expected: any remaining object-storage/pgvector references are historical notes or explicitly marked as replaced, not active first-phase defaults.

- [ ] **Step 6: Commit**

```powershell
git add docs/project-constitution.md docs/change-log.md docs/feature-map.md .env.example
git commit -m "docs: update ai storage direction"
```

---

## Task 2: Add AI Storage And RAG Configuration

**Files:**
- Modify: `pom.xml`
- Modify: `egg-system/pom.xml`
- Modify: `egg-admin/src/main/resources/application.yml`
- Modify: `egg-framework/src/main/java/com/egg/commerce/framework/config/properties/AiCommerceProperties.java`

**Interfaces:**
- Consumes: environment variable names from Task 1
- Produces: typed config available through `AiCommerceProperties`

- [ ] **Step 1: Add dependency properties**

In root `pom.xml`, add properties:

```xml
<spring-ai.version>2.0.0</spring-ai.version>
<spring-ai-alibaba.version>1.0.0.2</spring-ai-alibaba.version>
<aliyun-sdk-oss.version>3.10.2</aliyun-sdk-oss.version>
```

Add dependency management:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 2: Add system dependencies**

In `egg-system/pom.xml`, add:

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>${aliyun-sdk-oss.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-advisors-vector-store</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-milvus</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

- [ ] **Step 3: Update typed properties**

In `AiCommerceProperties`, replace `Storage.ObjectStorage` with `Storage.AliOss`:

```java
public static class Storage
{
    private String provider = "alioss";
    private AliOss alioss = new AliOss();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public AliOss getAlioss() { return alioss; }
    public void setAlioss(AliOss alioss) { this.alioss = alioss; }
}

public static class AliOss
{
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String objectPrefix = "ai";

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    public String getObjectPrefix() { return objectPrefix; }
    public void setObjectPrefix(String objectPrefix) { this.objectPrefix = objectPrefix; }
}
```

Update `Rag`:

```java
private String vectorStore = "milvus";
private Milvus milvus = new Milvus();

public Milvus getMilvus() { return milvus; }
public void setMilvus(Milvus milvus) { this.milvus = milvus; }

public static class Milvus
{
    private String host = "localhost";
    private int port = 19530;
    private String username;
    private String password;
    private String database = "default";
    private String collection = "ai_knowledge_vectors";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
}
```

- [ ] **Step 4: Update application YAML**

In `egg-admin/src/main/resources/application.yml`, replace the commented `egg.ai` sample with:

```yaml
egg:
  ai:
    mock-enabled: ${EGG_AI_MOCK_ENABLED:true}
    storage:
      provider: alioss
      alioss:
        endpoint: ${EGG_ALIOSS_ENDPOINT:}
        bucket: ${EGG_ALIOSS_BUCKET:}
        access-key-id: ${EGG_ALIOSS_ACCESS_KEY_ID:}
        access-key-secret: ${EGG_ALIOSS_ACCESS_KEY_SECRET:}
        object-prefix: ${EGG_ALIOSS_OBJECT_PREFIX:ai}
    rag:
      enabled: ${EGG_RAG_ENABLED:true}
      vector-store: milvus
      chunk-size: ${EGG_RAG_CHUNK_SIZE:800}
      chunk-overlap: ${EGG_RAG_CHUNK_OVERLAP:120}
      top-k: ${EGG_RAG_TOP_K:6}
      min-score: ${EGG_RAG_MIN_SCORE:0.62}
      embedding-dimension: ${EGG_RAG_EMBEDDING_DIMENSION:1536}
      milvus:
        host: ${EGG_MILVUS_HOST:localhost}
        port: ${EGG_MILVUS_PORT:19530}
        username: ${EGG_MILVUS_USERNAME:}
        password: ${EGG_MILVUS_PASSWORD:}
        database: ${EGG_MILVUS_DATABASE:default}
        collection: ${EGG_MILVUS_COLLECTION:ai_knowledge_vectors}
    model-gateway:
      text:
        provider: ${EGG_MODEL_TEXT_PROVIDER:dashscope}
        model: ${EGG_MODEL_TEXT_NAME:qwen-plus}
        api-key: ${EGG_DASHSCOPE_API_KEY:}
      embedding:
        provider: ${EGG_MODEL_EMBEDDING_PROVIDER:dashscope}
        model: ${EGG_MODEL_EMBEDDING_NAME:text-embedding-v2}
        api-key: ${EGG_DASHSCOPE_API_KEY:}
```

- [ ] **Step 5: Compile**

Run:

```powershell
mvn -pl egg-system -am -DskipTests compile
```

Expected: build reaches `BUILD SUCCESS`. If Spring AI version conflicts with Spring Boot 4, record the dependency conflict and choose the nearest compatible versions before continuing.

- [ ] **Step 6: Commit**

```powershell
git add pom.xml egg-system/pom.xml egg-admin/src/main/resources/application.yml egg-framework/src/main/java/com/egg/commerce/framework/config/properties/AiCommerceProperties.java
git commit -m "chore: configure alioss and milvus ai settings"
```

---

## Task 3: Add SQL Tables And Admin Menus

**Files:**
- Create: `sql/ai_knowledge_sensitive_rag.sql`

**Interfaces:**
- Produces: tables and permissions used by backend and admin UI

- [ ] **Step 1: Write SQL migration**

Create `sql/ai_knowledge_sensitive_rag.sql`:

```sql
-- AI knowledge, sensitive word, and RAG chat migration for PostgreSQL.

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
    chunk_count integer NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'processing',
    failure_reason text,
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    completed_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS ai_sensitive_categories (
    id bigserial PRIMARY KEY,
    category_name varchar(100) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'enabled',
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_sensitive_words (
    id bigserial PRIMARY KEY,
    word varchar(255) NOT NULL,
    category_id bigint REFERENCES ai_sensitive_categories(id),
    status varchar(30) NOT NULL DEFAULT 'enabled',
    created_by bigint,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL,
    title varchar(255) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'active',
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    user_id bigint NOT NULL,
    role varchar(30) NOT NULL,
    content text NOT NULL,
    model_name varchar(120),
    rag_used boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_chat_retrieval_sources (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id uuid NOT NULL REFERENCES ai_chat_messages(id) ON DELETE CASCADE,
    document_id uuid REFERENCES ai_knowledge_documents(id),
    vector_id varchar(255),
    source_title varchar(255),
    snippet text,
    score numeric(10, 6),
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_sensitive_categories_name
    ON ai_sensitive_categories(category_name);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_sensitive_words_word_category
    ON ai_sensitive_words(word, category_id);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_documents_status
    ON ai_knowledge_documents(status);

CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user
    ON ai_chat_sessions(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session
    ON ai_chat_messages(session_id, created_at);
```

- [ ] **Step 2: Add menu inserts**

Append menu SQL under the table definitions. Use IDs above existing prompt template menu IDs if available:

```sql
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
SELECT 2120, '敏感词管理', 2100, 3, 'sensitive', 'ai/sensitive/index', '', '',
       1, 0, 'C', '0', '0', 'ai:sensitive:word:list', 'dict',
       'admin', now(), '', NULL, 'AI敏感词管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2120);
```

Add button permissions for every permission listed in the design spec, using `WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = '...')`.

- [ ] **Step 3: Validate SQL text**

Run:

```powershell
Select-String -Path sql\ai_knowledge_sensitive_rag.sql -Pattern 'ai_knowledge_documents|ai_sensitive_words|ai_chat_sessions|ai:knowledge:list|ai:sensitive:word:list'
```

Expected: each pattern appears.

- [ ] **Step 4: Commit**

```powershell
git add sql/ai_knowledge_sensitive_rag.sql
git commit -m "db: add ai knowledge sensitive rag schema"
```

---

## Task 4: Implement Storage, Document, And Vector Abstractions

**Files:**
- Create infrastructure files listed in Backend Infrastructure
- Create: `egg-system/src/test/java/com/egg/commerce/system/service/ai/rag/AiDocumentParserServiceTest.java`

**Interfaces:**
- Produces: `AiObjectStorageService`, `AiDocumentParserService`, `AiVectorStoreService`
- Consumes: `AiCommerceProperties`

- [ ] **Step 1: Write parser test**

Create `AiDocumentParserServiceTest.java`:

```java
package com.egg.commerce.system.service.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiDocumentParserServiceTest
{
    @Test
    void parseAndSplitSplitsPlainTextIntoOrderedChunks()
    {
        AiSimpleDocumentParserService parser = new AiSimpleDocumentParserService(12, 3);

        List<AiDocumentChunk> chunks = parser.parseAndSplit("rules.txt",
                "平台规则要求主图不能夸大功效".getBytes(StandardCharsets.UTF_8));

        assertFalse(chunks.isEmpty());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertFalse(chunks.get(0).getContent().isBlank());
    }
}
```

- [ ] **Step 2: Add simple parser implementation**

Create `AiSimpleDocumentParserService` in `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl/AiSimpleDocumentParserService.java`. It should:

```java
public class AiSimpleDocumentParserService implements AiDocumentParserService
{
    private final int chunkSize;
    private final int chunkOverlap;

    public AiSimpleDocumentParserService(int chunkSize, int chunkOverlap)
    {
        this.chunkSize = Math.max(chunkSize, 1);
        this.chunkOverlap = Math.max(Math.min(chunkOverlap, this.chunkSize - 1), 0);
    }

    @Override
    public List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content)
    {
        String text = new String(content, StandardCharsets.UTF_8).trim();
        List<AiDocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        int cursor = 0;
        while (cursor < text.length())
        {
            int end = Math.min(cursor + chunkSize, text.length());
            AiDocumentChunk chunk = new AiDocumentChunk();
            chunk.setChunkId(UUID.randomUUID().toString());
            chunk.setContent(text.substring(cursor, end));
            chunk.setChunkIndex(index++);
            chunks.add(chunk);
            if (end == text.length())
            {
                break;
            }
            cursor = Math.max(end - chunkOverlap, cursor + 1);
        }
        return chunks;
    }
}
```

Include a short comment that this fallback is for plain text and local tests; rich document parsing can use Spring AI/Tika behind the same interface.

- [ ] **Step 3: Add storage DTO and interface**

Create `AiStoredObject` with getters/setters for `bucket`, `objectKey`, `url`, `size`, `mimeType`. Create `AiObjectStorageService` exactly as defined in the shared interface section.

- [ ] **Step 4: Add Ali OSS implementation**

Create `AiAliOssObjectStorageServiceImpl`. It should:

```java
@Service
public class AiAliOssObjectStorageServiceImpl implements AiObjectStorageService
{
    private final AiCommerceProperties properties;

    public AiAliOssObjectStorageServiceImpl(AiCommerceProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public AiStoredObject uploadKnowledgeFile(MultipartFile file)
    {
        AiCommerceProperties.AliOss oss = properties.getStorage().getAlioss();
        if (StringUtils.isAnyBlank(oss.getEndpoint(), oss.getBucket(), oss.getAccessKeyId(), oss.getAccessKeySecret()))
        {
            throw new ServiceException("阿里 OSS 配置不完整");
        }
        String ext = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
        String objectKey = oss.getObjectPrefix() + "/knowledge/" + DateUtils.datePath() + "/" + UUID.randomUUID() + (StringUtils.isBlank(ext) ? "" : "." + ext);
        OSS client = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
        try
        {
            client.putObject(oss.getBucket(), objectKey, file.getInputStream());
        }
        catch (IOException e)
        {
            throw new ServiceException("知识库文件上传 OSS 失败");
        }
        finally
        {
            client.shutdown();
        }
        AiStoredObject stored = new AiStoredObject();
        stored.setBucket(oss.getBucket());
        stored.setObjectKey(objectKey);
        stored.setUrl("https://" + oss.getBucket() + "." + oss.getEndpoint() + "/" + objectKey);
        stored.setSize(file.getSize());
        stored.setMimeType(file.getContentType());
        return stored;
    }
}
```

Adapt `DateUtils.datePath()` if it does not exist by using `DateTimeFormatter.ofPattern("yyyy/MM/dd")`.

- [ ] **Step 5: Add vector interface and Spring Milvus implementation shell**

Create `AiVectorStoreService` and `AiVectorRecord`. Implement `AiSpringMilvusVectorStoreServiceImpl` with constructor injection of Spring AI `VectorStore` if available:

```java
@Service
public class AiSpringMilvusVectorStoreServiceImpl implements AiVectorStoreService
{
    private final VectorStore vectorStore;

    public AiSpringMilvusVectorStoreServiceImpl(VectorStore vectorStore)
    {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
    {
        List<Document> documents = chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("sourceTitle", fileName);
            metadata.put("chunkIndex", chunk.getChunkIndex());
            return new Document(chunk.getChunkId(), chunk.getContent(), metadata);
        }).toList();
        vectorStore.add(documents);
        return documents.stream().map(Document::getId).toList();
    }
}
```

Add `search` and `deleteVectors` using Spring AI vector APIs available after dependencies resolve.

- [ ] **Step 6: Run tests**

Run:

```powershell
mvn -pl egg-system -Dtest=AiDocumentParserServiceTest test
```

Expected: parser test passes.

- [ ] **Step 7: Commit**

```powershell
git add egg-system/src/main/java/com/egg/commerce/system/service/ai/storage egg-system/src/main/java/com/egg/commerce/system/service/ai/rag egg-system/src/test/java/com/egg/commerce/system/service/ai/rag
git commit -m "feat: add ai storage and rag abstractions"
```

---

## Task 5: Implement Knowledge Backend And Async Vectorization

**Files:**
- Create all Knowledge Backend files

**Interfaces:**
- Consumes: `AiObjectStorageService`, `AiDocumentParserService`, `AiVectorStoreService`
- Produces: `/ai/knowledge` backend APIs and document status workflow

- [ ] **Step 1: Write service test**

Create `AiKnowledgeDocumentServiceImplTest.java` with fakes for mapper/storage/vectorization:

```java
@Test
void uploadCreatesProcessingDocumentAndStartsVectorization()
{
    FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
    FakeStorage storage = new FakeStorage();
    FakeVectorization vectorization = new FakeVectorization();
    AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorization);

    AiKnowledgeDocument document = service.upload(new MockMultipartFile("file", "rules.txt", "text/plain", "规则".getBytes()), 100L);

    assertEquals("processing", document.getStatus());
    assertEquals("rules.txt", document.getFileName());
    assertEquals(document.getId(), vectorization.startedDocumentId);
}
```

If constructor injection differs, keep the same observable behavior.

- [ ] **Step 2: Add domain**

Create `AiKnowledgeDocument` with fields from `ai_knowledge_documents`: `id`, `fileName`, `fileExt`, `mimeType`, `fileSize`, `ossBucket`, `ossObjectKey`, `ossUrl`, `vectorStore`, `vectorCollection`, `vectorIds`, `chunkCount`, `status`, `failureReason`, `createdBy`, `createdAt`, `updatedAt`, `completedAt`.

- [ ] **Step 3: Add mapper interface and XML**

Mapper methods:

```java
AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);
List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);
int insertAiKnowledgeDocument(AiKnowledgeDocument document);
int updateAiKnowledgeDocument(AiKnowledgeDocument document);
int deleteAiKnowledgeDocumentByIds(String[] ids);
```

XML must map `vector_ids::text as vector_ids` so Java stores it as `String`.

- [ ] **Step 4: Add service**

`IAiKnowledgeDocumentService` methods:

```java
List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);
AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);
AiKnowledgeDocument upload(MultipartFile file, Long createdBy);
int deleteAiKnowledgeDocumentByIds(String[] ids);
void retryVectorization(String id);
```

`AiKnowledgeDocumentServiceImpl.upload` must:

1. Validate non-empty file.
2. Upload to OSS.
3. Insert `processing` metadata row.
4. Call `vectorizationService.startVectorization(document.getId(), file)`.

Add a short comment before the async call explaining that the upload request returns after metadata is saved; vectorization updates status later.

- [ ] **Step 5: Add vectorization service**

`AiKnowledgeVectorizationService.startVectorization(String documentId, MultipartFile file)` should submit work to `threadPoolTaskExecutor`.

The worker should:

1. Read file bytes.
2. Parse and split.
3. Add chunks to Milvus.
4. Update row to `completed`, set `vectorIds`, `chunkCount`, `completedAt`.
5. On exception, update row to `failed` and set `failureReason`.

- [ ] **Step 6: Add controller**

`AiKnowledgeDocumentController`:

```java
@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeDocumentController extends BaseController
{
    @PreAuthorize("@ss.hasPermi('ai:knowledge:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiKnowledgeDocument query) { ... }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) { ... }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) { ... }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:retry')")
    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable String id) { ... }
}
```

- [ ] **Step 7: Run tests and compile**

Run:

```powershell
mvn -pl egg-system -Dtest=AiKnowledgeDocumentServiceImplTest test
mvn -pl egg-admin -am -DskipTests compile
```

Expected: test passes and admin compile succeeds.

- [ ] **Step 8: Commit**

```powershell
git add egg-admin/src/main/java/com/egg/commerce/web/controller/ai/knowledge egg-system/src/main/java/com/egg/commerce/system/domain/ai/knowledge egg-system/src/main/java/com/egg/commerce/system/mapper/ai/knowledge egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge egg-system/src/main/resources/mapper/ai/knowledge egg-system/src/test/java/com/egg/commerce/system/service/ai/knowledge
git commit -m "feat: add ai knowledge backend"
```

---

## Task 6: Implement Sensitive Word Backend

**Files:**
- Create all Sensitive Backend files

**Interfaces:**
- Produces: backend CRUD APIs for sensitive categories and words

- [ ] **Step 1: Write service test**

Create `AiSensitiveServiceTest.java`:

```java
@Test
void createCategoryDefaultsStatusToEnabled()
{
    FakeSensitiveCategoryMapper mapper = new FakeSensitiveCategoryMapper();
    AiSensitiveCategoryServiceImpl service = new AiSensitiveCategoryServiceImpl(mapper);

    AiSensitiveCategory category = new AiSensitiveCategory();
    category.setCategoryName("违禁词");

    service.insertAiSensitiveCategory(category, 100L);

    assertEquals("enabled", mapper.inserted.getStatus());
    assertEquals(100L, mapper.inserted.getCreatedBy());
}
```

- [ ] **Step 2: Add domains**

`AiSensitiveCategory`: `id`, `categoryName`, `status`, `createdBy`, `createdAt`, `updatedAt`.  
`AiSensitiveWord`: `id`, `word`, `categoryId`, `categoryName`, `status`, `createdBy`, `createdAt`, `updatedAt`.

- [ ] **Step 3: Add mappers and XML**

Category mapper:

```java
AiSensitiveCategory selectAiSensitiveCategoryById(Long id);
List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query);
int insertAiSensitiveCategory(AiSensitiveCategory category);
int updateAiSensitiveCategory(AiSensitiveCategory category);
int deleteAiSensitiveCategoryByIds(Long[] ids);
```

Word mapper:

```java
AiSensitiveWord selectAiSensitiveWordById(Long id);
List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query);
int insertAiSensitiveWord(AiSensitiveWord word);
int updateAiSensitiveWord(AiSensitiveWord word);
int deleteAiSensitiveWordByIds(Long[] ids);
```

- [ ] **Step 4: Add services**

Category service should default `status` to `enabled`. Word service should default `status` to `enabled` and require non-blank `word`.

- [ ] **Step 5: Add controllers**

Endpoints:

```text
GET    /ai/sensitive/category/list
GET    /ai/sensitive/category/{id}
POST   /ai/sensitive/category
PUT    /ai/sensitive/category
DELETE /ai/sensitive/category/{ids}
GET    /ai/sensitive/word/list
GET    /ai/sensitive/word/{id}
POST   /ai/sensitive/word
PUT    /ai/sensitive/word
DELETE /ai/sensitive/word/{ids}
```

Use permissions from Task 3.

- [ ] **Step 6: Run tests and compile**

```powershell
mvn -pl egg-system -Dtest=AiSensitiveServiceTest test
mvn -pl egg-admin -am -DskipTests compile
```

Expected: tests pass and compile succeeds.

- [ ] **Step 7: Commit**

```powershell
git add egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive egg-system/src/main/resources/mapper/ai/sensitive egg-system/src/test/java/com/egg/commerce/system/service/ai/sensitive
git commit -m "feat: add ai sensitive word backend"
```

---

## Task 7: Implement RAG Chat Persistence And Service

**Files:**
- Create all Chat Backend files

**Interfaces:**
- Consumes: `AiVectorStoreService`
- Produces: session/message/source APIs for `egg-user-web`

- [ ] **Step 1: Write chat service test**

Create `AiChatServiceImplTest.java`:

```java
@Test
void sendMessageStoresUserAssistantAndSources()
{
    FakeChatSessionMapper sessionMapper = new FakeChatSessionMapper();
    FakeChatMessageMapper messageMapper = new FakeChatMessageMapper();
    FakeSourceMapper sourceMapper = new FakeSourceMapper();
    FakeVectorStore vectorStore = new FakeVectorStore();
    FakeChatModel chatModel = new FakeChatModel("建议突出平台规则。");
    AiChatServiceImpl service = new AiChatServiceImpl(sessionMapper, messageMapper, sourceMapper, vectorStore, chatModel);

    AiChatResponse response = service.sendMessage("session-1", 100L, "主图怎么做？");

    assertEquals("建议突出平台规则。", response.getAnswer());
    assertEquals(2, messageMapper.inserted.size());
    assertEquals(1, sourceMapper.inserted.size());
}
```

- [ ] **Step 2: Add chat domains**

Create:

- `AiChatSession`
- `AiChatMessage`
- `AiChatRetrievalSource`
- `AiChatRequest`
- `AiChatResponse`

`AiChatResponse` fields:

```java
private String sessionId;
private String messageId;
private String answer;
private List<AiChatRetrievalSource> sources;
```

- [ ] **Step 3: Add mapper interfaces and XML**

Create select/insert/update methods for sessions, messages, and sources. `selectAiChatSessionList` must filter by `userId`.

- [ ] **Step 4: Add model gateway interface**

Create:

```java
package com.egg.commerce.system.service.ai.chat;

import java.util.List;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;

public interface AiChatModelService
{
    String generateAnswer(String question, List<AiVectorRecord> sources);
}
```

Add `AiMockChatModelServiceImpl` that returns:

```java
"我已结合知识库资料给出建议。请检查下方来源后再用于正式商品内容。"
```

Use mock implementation when `egg.ai.mock-enabled=true`.

- [ ] **Step 5: Add chat service**

`IAiChatService`:

```java
List<AiChatSession> selectSessionList(Long userId);
AiChatSession createSession(Long userId, String title);
List<AiChatMessage> selectMessages(String sessionId, Long userId);
AiChatResponse sendMessage(String sessionId, Long userId, String content);
```

`sendMessage` must:

1. Validate session belongs to user.
2. Insert user message.
3. Search vector store.
4. Generate answer.
5. Insert assistant message with `ragUsed=true`.
6. Insert retrieval sources with `messageId` = assistant message ID.
7. Return answer and sources.

- [ ] **Step 6: Add controller**

`AiChatController`:

```java
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController
{
    @GetMapping("/session/list")
    public AjaxResult sessions() { ... }

    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody AiChatSession session) { ... }

    @GetMapping("/session/{id}/messages")
    public AjaxResult messages(@PathVariable String id) { ... }

    @PostMapping("/session/{id}/message")
    public AjaxResult send(@PathVariable String id, @RequestBody AiChatRequest request) { ... }
}
```

Use authenticated `getUserId()` for `userId`.

- [ ] **Step 7: Run tests and compile**

```powershell
mvn -pl egg-system -Dtest=AiChatServiceImplTest test
mvn -pl egg-admin -am -DskipTests compile
```

Expected: tests pass and compile succeeds.

- [ ] **Step 8: Commit**

```powershell
git add egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat egg-system/src/main/java/com/egg/commerce/system/service/ai/chat egg-system/src/main/resources/mapper/ai/chat egg-system/src/test/java/com/egg/commerce/system/service/ai/chat
git commit -m "feat: add ai rag chat backend"
```

---

## Task 8: Implement Admin Knowledge Page

**Files:**
- Create: `egg-ui/src/api/ai/knowledge.js`
- Create: `egg-ui/src/views/ai/knowledge/index.vue`

**Interfaces:**
- Consumes: `/ai/knowledge` APIs from Task 5
- Produces: Egg-style admin knowledge management page

- [ ] **Step 1: Add API wrapper**

`egg-ui/src/api/ai/knowledge.js`:

```js
import request from '@/utils/request'

export function listKnowledge(query) {
  return request({ url: '/ai/knowledge/list', method: 'get', params: query })
}

export function uploadKnowledge(data) {
  return request({
    url: '/ai/knowledge/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function delKnowledge(ids) {
  return request({ url: '/ai/knowledge/' + ids, method: 'delete' })
}

export function retryKnowledge(id) {
  return request({ url: '/ai/knowledge/' + id + '/retry', method: 'post' })
}
```

- [ ] **Step 2: Add page**

`index.vue` must include:

- `app-container`
- search form: file name, status
- upload button using `el-upload` with `:auto-upload="false"`
- table columns: file name, file size, status tag, chunk count, failure reason, updated time
- actions: retry for `failed`, delete
- pagination component

Use existing prompt-template page patterns for `right-toolbar`, `pagination`, `$modal`, and `v-hasPermi`.

- [ ] **Step 3: Build admin frontend**

Run:

```powershell
npm run build:stage
```

from `egg-ui`.

Expected: build succeeds.

- [ ] **Step 4: Commit**

```powershell
git add egg-ui/src/api/ai/knowledge.js egg-ui/src/views/ai/knowledge/index.vue
git commit -m "feat: add ai knowledge admin page"
```

---

## Task 9: Implement Admin Sensitive Page

**Files:**
- Create: `egg-ui/src/api/ai/sensitive.js`
- Create: `egg-ui/src/views/ai/sensitive/index.vue`

**Interfaces:**
- Consumes: `/ai/sensitive/category` and `/ai/sensitive/word` APIs from Task 6
- Produces: Egg-style tabbed sensitive management page

- [ ] **Step 1: Add API wrapper**

`egg-ui/src/api/ai/sensitive.js`:

```js
import request from '@/utils/request'

export function listSensitiveCategory(query) {
  return request({ url: '/ai/sensitive/category/list', method: 'get', params: query })
}
export function addSensitiveCategory(data) {
  return request({ url: '/ai/sensitive/category', method: 'post', data })
}
export function updateSensitiveCategory(data) {
  return request({ url: '/ai/sensitive/category', method: 'put', data })
}
export function delSensitiveCategory(ids) {
  return request({ url: '/ai/sensitive/category/' + ids, method: 'delete' })
}
export function listSensitiveWord(query) {
  return request({ url: '/ai/sensitive/word/list', method: 'get', params: query })
}
export function addSensitiveWord(data) {
  return request({ url: '/ai/sensitive/word', method: 'post', data })
}
export function updateSensitiveWord(data) {
  return request({ url: '/ai/sensitive/word', method: 'put', data })
}
export function delSensitiveWord(ids) {
  return request({ url: '/ai/sensitive/word/' + ids, method: 'delete' })
}
```

- [ ] **Step 2: Add page with tabs**

`index.vue` must include:

- `el-tabs` with `category` and `word`
- category table with name/status/updated time/actions
- word table with word/category/status/updated time/actions
- dialogs for add/edit category and add/edit word
- category dropdown in word dialog
- permission directives matching Task 3

- [ ] **Step 3: Build admin frontend**

Run from `egg-ui`:

```powershell
npm run build:stage
```

Expected: build succeeds.

- [ ] **Step 4: Commit**

```powershell
git add egg-ui/src/api/ai/sensitive.js egg-ui/src/views/ai/sensitive/index.vue
git commit -m "feat: add ai sensitive admin page"
```

---

## Task 10: Implement User RAG Chat Page

**Files:**
- Create: `egg-user-web/src/api/chat.js`
- Modify: `egg-user-web/src/pages/ChatPage.vue`
- Modify: `egg-user-web/src/styles/main.css`

**Interfaces:**
- Consumes: `/ai/chat` APIs from Task 7
- Produces: user-facing RAG chat experience

- [ ] **Step 1: Add user API wrapper**

Create `egg-user-web/src/api/chat.js`:

```js
const API_BASE = import.meta.env.VITE_API_BASE || ''

async function request(path, options = {}) {
  const token = localStorage.getItem('Admin-Token') || localStorage.getItem('token') || ''
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  })
  const data = await response.json()
  if (!response.ok || data.code !== 200) {
    throw new Error(data.msg || data.message || '请求失败')
  }
  return data.data
}

export function listChatSessions() {
  return request('/ai/chat/session/list')
}

export function createChatSession(title = '新的智能聊天') {
  return request('/ai/chat/session', {
    method: 'POST',
    body: JSON.stringify({ title })
  })
}

export function listChatMessages(sessionId) {
  return request(`/ai/chat/session/${sessionId}/messages`)
}

export function sendChatMessage(sessionId, content) {
  return request(`/ai/chat/session/${sessionId}/message`, {
    method: 'POST',
    body: JSON.stringify({ content })
  })
}
```

- [ ] **Step 2: Replace mock chat page**

`ChatPage.vue` must:

- load sessions on mount
- create first session if no session exists
- display messages
- send message on button or Enter
- show assistant answer
- render sources under assistant answer
- show loading and error states

State names:

```js
const sessions = ref([])
const activeSessionId = ref('')
const messages = ref([])
const input = ref('')
const loading = ref(false)
const errorMessage = ref('')
```

- [ ] **Step 3: Add styles**

Add or adjust styles in `egg-user-web/src/styles/main.css` for:

```css
.chat-workspace
.chat-session-list
.chat-message
.chat-message.is-user
.chat-source-list
.chat-composer
```

Keep typography compact and avoid nested cards.

- [ ] **Step 4: Build user frontend**

Run from `egg-user-web`:

```powershell
npm run build
```

Expected: build succeeds.

- [ ] **Step 5: Commit**

```powershell
git add egg-user-web/src/api/chat.js egg-user-web/src/pages/ChatPage.vue egg-user-web/src/styles/main.css
git commit -m "feat: connect user chat to rag backend"
```

---

## Task 11: End-To-End Verification And Cleanup

**Files:**
- Modify only files needed to fix integration defects discovered by verification.

**Interfaces:**
- Consumes: all earlier tasks
- Produces: verified implementation ready for user review

- [ ] **Step 1: Run backend compile**

```powershell
mvn -pl egg-admin -am -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run backend focused tests**

```powershell
mvn -pl egg-system -Dtest=AiKnowledgeDocumentServiceImplTest,AiSensitiveServiceTest,AiChatServiceImplTest,AiDocumentParserServiceTest test
```

Expected: all tests pass.

- [ ] **Step 3: Run admin frontend build**

```powershell
npm run build:stage
```

Working directory: `egg-ui`  
Expected: build succeeds.

- [ ] **Step 4: Run user frontend build**

```powershell
npm run build
```

Working directory: `egg-user-web`  
Expected: build succeeds.

- [ ] **Step 5: Inspect for leaked secrets**

Run:

```powershell
Select-String -Path .env.example,egg-admin\src\main\resources\application.yml -Pattern 'LTAI|sk-|access-key-secret: [^$]|api-key: sk' -CaseSensitive:$false
```

Expected: no matches containing real credentials.

- [ ] **Step 6: Final status**

Run:

```powershell
git status --short
```

Expected: only intentional implementation files are modified.

- [ ] **Step 7: Commit verification fixes**

If verification required fixes:

```powershell
git add <fixed-files>
git commit -m "fix: stabilize ai knowledge rag integration"
```

If no fixes were needed, do not create an empty commit.

---

## Self-Review

### Spec Coverage

- 宪法修订：Task 1.
- 阿里 OSS + Milvus + PostgreSQL 元数据配置：Task 2 and Task 3.
- 知识库后台、异步向量化、状态展示：Task 5 and Task 8.
- 敏感词分类与词库管理：Task 6 and Task 9.
- 前台 RAG 聊天、会话、消息、来源：Task 7 and Task 10.
- 不复制真实密钥：Task 1, Task 2, Task 11.
- 文档更新：Task 1.
- 验证：Task 11.

### Placeholder Scan

This plan contains no unresolved placeholder markers. Where dependency versions may conflict, Task 2 requires resolving the concrete Maven conflict before proceeding.

### Type Consistency

Shared infrastructure interfaces are named once in the Interfaces section and reused by knowledge and chat tasks:

- `AiObjectStorageService`
- `AiStoredObject`
- `AiDocumentParserService`
- `AiDocumentChunk`
- `AiVectorStoreService`
- `AiVectorRecord`

Chat tasks consistently use:

- `AiChatSession`
- `AiChatMessage`
- `AiChatRetrievalSource`
- `AiChatRequest`
- `AiChatResponse`
