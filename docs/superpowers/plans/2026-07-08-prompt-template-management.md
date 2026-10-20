# Prompt Template Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build backend and Egg admin frontend CRUD for database-backed AI prompt templates.

**Architecture:** Follow the existing Egg monolith pattern: controller in `egg-admin`, domain/service/mapper in `egg-system`, MyBatis XML for PostgreSQL access, and `egg-ui` Vue 2 + Element UI for the admin page. Store prompt Markdown as `content` JSONB with `{ "format": "markdown", "body": "..." }`; keep `variables` as optional JSONB.

**Tech Stack:** Java 17, Spring Boot, MyBatis XML, PostgreSQL JSONB, Vue 2, Element UI, Axios request wrappers.

## Global Constraints

- Backend admin only; do not add front-stage `egg-user-web` pages.
- No billing, tenant, team, payment, or video-generation behavior.
- Do not add a Markdown editor dependency; use a large monospace textarea.
- Variable placeholders use single braces such as `{name}`.
- `variables` is optional and manually maintained.
- `version` starts at `1` and increments on every update.
- `status` values are exactly `draft`, `published`, and `archived`.
- Add plain comments only for non-obvious AI workflow, JSONB, or versioning logic.
- Update `docs/change-log.md` and `docs/feature-map.md`.

---

### Task 1: Backend Prompt Template CRUD

**Files:**
- Create: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/prompt/AiPromptTemplate.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt/AiPromptTemplateMapper.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/prompt/IAiPromptTemplateService.java`
- Create: `egg-system/src/main/java/com/egg/commerce/system/service/ai/prompt/impl/AiPromptTemplateServiceImpl.java`
- Create: `egg-system/src/main/resources/mapper/ai/prompt/AiPromptTemplateMapper.xml`
- Create: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/prompt/AiPromptTemplateController.java`

**Interfaces:**
- Produces: REST endpoints under `/ai/promptTemplate`.
- Produces: service methods `selectAiPromptTemplateById`, `selectAiPromptTemplateList`, `insertAiPromptTemplate`, `updateAiPromptTemplate`, and `deleteAiPromptTemplateByIds`.

- [ ] **Step 1: Add the domain object**

Create `AiPromptTemplate` with fields:

```java
private String id;
private String name;
private String description;
private String category;
private String content;
private String variables;
private Integer version;
private String status;
private Long createdBy;
private Date createdAt;
private Date updatedAt;
```

Use validation annotations for required `name` and `content`.

- [ ] **Step 2: Add mapper interface and XML**

Create mapper methods for select by id, list, insert, update, delete by id, and delete by ids. In XML, write JSONB columns with casts:

```sql
CAST(#{content} AS jsonb)
CAST(#{variables} AS jsonb)
```

Use `created_at` and `updated_at` columns directly instead of BaseEntity `createTime`.

- [ ] **Step 3: Add service validation**

In `AiPromptTemplateServiceImpl`, validate:

```java
status in ["draft", "published", "archived"]
content is parseable JSON and contains non-blank body
variables is blank or parseable JSON
```

Use `com.alibaba.fastjson2.JSON` and `JSONObject`. Default blank `variables` to `[]`.

- [ ] **Step 4: Add version handling**

In insert, default `version` to `1`. In update, let SQL set `version = coalesce(version, 1) + 1` so every edit increments server-side.

- [ ] **Step 5: Add controller**

Use existing Egg annotations and permission codes:

```java
@PreAuthorize("@ss.hasPermi('ai:promptTemplate:list')")
@GetMapping("/list")

@PreAuthorize("@ss.hasPermi('ai:promptTemplate:query')")
@GetMapping("/{id}")

@PreAuthorize("@ss.hasPermi('ai:promptTemplate:add')")
@PostMapping

@PreAuthorize("@ss.hasPermi('ai:promptTemplate:edit')")
@PutMapping

@PreAuthorize("@ss.hasPermi('ai:promptTemplate:remove')")
@DeleteMapping("/{ids}")
```

Set `createdBy` from `getUserId()` on add.

- [ ] **Step 6: Compile backend**

Run:

```powershell
mvn -pl egg-admin -am -DskipTests compile
```

Expected: build success, or existing unrelated compile failures documented before continuing.

---

### Task 2: SQL Migration And Menu Permissions

**Files:**
- Create: `sql/ai_prompt_template_management.sql`

**Interfaces:**
- Consumes: backend permission codes from Task 1.
- Produces: PostgreSQL migration and menu script.

- [ ] **Step 1: Add table-alignment SQL**

Write SQL that changes `created_by` to `bigint` and changes `variables` default to `[]::jsonb`:

```sql
ALTER TABLE ai_prompt_templates
  ALTER COLUMN created_by TYPE bigint USING NULL,
  ALTER COLUMN variables SET DEFAULT '[]'::jsonb;
```

- [ ] **Step 2: Add admin menu SQL**

Insert an `AI配置` parent menu and `提示词模板` child route using IDs above the current seed range, plus button permissions:

```sql
ai:promptTemplate:list
ai:promptTemplate:query
ai:promptTemplate:add
ai:promptTemplate:edit
ai:promptTemplate:remove
```

The child route component must be `ai/promptTemplate/index`.

---

### Task 3: Egg Admin Frontend Page

**Files:**
- Create: `egg-ui/src/api/ai/promptTemplate.js`
- Create: `egg-ui/src/views/ai/promptTemplate/index.vue`

**Interfaces:**
- Consumes: REST endpoints from Task 1.
- Produces: list, add, edit, detail, and delete UI in `egg-ui`.

- [ ] **Step 1: Add API wrapper**

Export functions:

```js
listPromptTemplate(query)
getPromptTemplate(id)
addPromptTemplate(data)
updatePromptTemplate(data)
delPromptTemplate(id)
```

- [ ] **Step 2: Add list UI**

Use Element UI patterns from existing Egg pages:

- Search fields: `name`, `category`, `status`.
- Toolbar buttons: add, edit, delete.
- Table columns: name, category, status, version, createdBy, createdAt, updatedAt.
- Row actions: detail, edit, delete.

- [ ] **Step 3: Add edit dialog**

Use a wide dialog with:

- name input
- description textarea
- category input
- status select
- Markdown body textarea
- optional variables JSON textarea

The frontend converts body text to:

```js
content: JSON.stringify({ format: 'markdown', body: this.form.body })
```

The frontend parses details back to `form.body`.

- [ ] **Step 4: Add JSON validation**

Before submit:

- `name` must not be blank.
- `body` must not be blank.
- `variables` may be blank.
- non-blank `variables` must pass `JSON.parse`.

- [ ] **Step 5: Build frontend**

Run from `egg-ui`:

```powershell
npm run build:prod
```

Expected: build success, or dependency/environment failure documented.

---

### Task 4: Documentation And Final Verification

**Files:**
- Modify: `docs/change-log.md`
- Modify: `docs/feature-map.md`

**Interfaces:**
- Consumes: files created by Tasks 1-3.
- Produces: project maintenance docs and verification notes.

- [ ] **Step 1: Update change log**

Add a 2026-07-08 entry explaining the prompt template CRUD, Markdown storage format, backend admin-only scope, and SQL script.

- [ ] **Step 2: Update feature map**

Add a `Prompt Template Management` section mapping:

- backend controller
- domain/service/mapper/XML
- admin frontend API and page
- SQL script

- [ ] **Step 3: Run final status check**

Run:

```powershell
git status --short
```

Expected: only prompt-template implementation files and required docs are changed, alongside any pre-existing unrelated dirty files.

- [ ] **Step 4: Summarize verification**

Report Maven/frontend verification results and any command that could not be run.
