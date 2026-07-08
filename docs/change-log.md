# Project Change Log

This file records code and project-document changes in plain language. It is for humans and future AI agents, not for formal release notes.

Keep each entry short:

- Date
- Author or agent
- What changed
- Why it changed
- Main files or directories touched
- Follow-up notes, if any

## 2026-06-27

### Updated Root README For AI E-Commerce Project

Author: Codex

What changed:

- Replaced the original Egg framework README content with this project's AI e-commerce visual content platform description.
- Removed the original logo/title/badge block, online demo links, demo screenshots, and Egg community group links.
- Replaced the old version-branch section with a project-specific technical architecture section.
- Rewrote the platform introduction and built-in feature list around the Picset-style front stage, Egg-style admin stage, AI generation, intelligent chat, and RAG.

Why:

- The repository README should describe the actual project direction instead of the upstream framework template.
- Future developers and AI agents need the project entry document to match the constitution and current product scope.

Main files:

- `README.md`
- `docs/change-log.md`

Notes:

- No runtime code was changed.

### Added Project Constitution And Agent Entry Rules

Author: Codex

What changed:

- Added a project constitution based on the GitHub Spec Kit constitution template.
- Added a root `AGENTS.md` so future Codex/agent sessions know to read the constitution first.
- Added the original constitution template for reference.

Why:

- The project needs stable rules before large-scale AI, frontend, backend, and database development begins.
- Future threads or different agents should not accidentally drift away from the agreed product direction.

Main files:

- `AGENTS.md`
- `docs/project-constitution.md`
- `docs/project-constitution-template.md`

Notes:

- The constitution states that the first version is Picset-style visual generation first.
- Intelligent chat with RAG is required in phase one.
- Points, billing, payments, tenants, team spaces, production video generation, and advanced canvas are not phase-one priorities.

### Added Maintenance Documentation Requirements

Author: Codex

What changed:

- Updated the project constitution to require simple, useful code comments.
- Added this change log file.
- Added a feature map file that explains where each feature lives in the repository.

Why:

- Future debugging should be easy even after switching Codex threads or changing agents.
- New code should be understandable without forcing a maintainer to rediscover the project structure from scratch.

Main files:

- `docs/project-constitution.md`
- `docs/change-log.md`
- `docs/feature-map.md`

Notes:

- Future code changes should add a short entry here.
- Future feature work should update `docs/feature-map.md` when new modules, pages, APIs, tables, or major directories are added.

## 2026-07-06

### Polished Front-Stage Generator Workspace UI

Author: Codex

What changed:

- Updated the user frontend home page and product-image generation page to open directly into the refined generator workspace.
- Restyled the generator workspace around the reference screenshots: minimal Picset-style header, centered hero copy, compact process steps, refined upload/config panels, and a large generation-result canvas.
- Kept the change scoped to the front-stage user workspace and avoided phase-one non-goal copy such as points, billing, packages, payments, tenants, teams, or real video generation.

Why:

- The front-stage workspace should feel more polished and closer to the requested advanced Picset-style product-generation experience.

Main files:

- `egg-user-web/src/pages/HomePage.vue`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/styles/main.css`

Notes:

- No backend, database, admin-console, billing, tenant, or payment logic was changed.

### Adjusted Front-Stage Workspace Density

Author: Codex

What changed:

- Reduced the desktop density of the generator workspace content so the main creation area reads closer to the user's 80% browser-zoom reference while the top navigation remains at normal 100% scale.
- Tuned desktop-only spacing, typography, panel width, card padding, upload area height, process steps, and result canvas dimensions in the user frontend stylesheet.

Why:

- The previous 100% page scale felt too large and uncomfortable; the normal browser zoom should show the more compact working-area proportion from the reference screenshot.

Main files:

- `egg-user-web/src/styles/main.css`

Notes:

- This was a visual density adjustment only. No feature flow, backend, database, billing, tenant, payment, or admin-console logic was changed.
### Fixed Workspace Density CSS Cascade

Author: Codex

What changed:

- Moved the desktop workspace density override to the final stylesheet cascade layer and scoped it to the generator workbench.
- Further tightened the normal-zoom desktop workspace typography, spacing, cards, upload area, and result canvas so it behaves like the user's 80% reference while keeping the top navigation at native scale.

Why:

- The earlier density styles were written before later base workspace rules, so the browser still rendered the page too large at 100% zoom.

Main files:

- `egg-user-web/src/styles/main.css`
- `docs/change-log.md`

Notes:

- This remains a front-stage visual-only fix. No backend, database, admin-console, billing, tenant, payment, or feature-flow logic was changed.
### Tightened Workspace Density Again

Author: Codex

What changed:

- Reduced the front-stage generator workspace desktop density by another 10% while keeping the top navigation at normal scale.
- Tuned the final stylesheet override for title size, process steps, sidebar width, panel padding, upload area, form controls, button height, and result canvas height.

Why:

- The normal 100% browser view still felt a little too large, so the workspace needed a smaller working proportion.

Main files:

- `egg-user-web/src/styles/main.css`
- `docs/change-log.md`

Notes:

- This is a visual-only front-stage adjustment. No backend, database, admin-console, billing, tenant, payment, or feature-flow logic was changed.

## 2026-07-08

### Adjusted Front-Stage Generator Entry Points And Upload Preview

Author: Codex

What changed:

- Made the user frontend root route open the full-category product image generator by default.
- Hid phase-two top navigation entries for apparel sets, product library, asset library, and more tools.
- Added a front-stage story generation entry and page that reuses the generator workspace style.
- Updated the shared generator workspace upload area to support local image selection, drag-and-drop image input, six-image limit, preview thumbnails, and per-image removal.
- Hid the ad-image tab and generation speed panel from the shared generator workspace.

Why:

- The first-version workspace should focus on the active product image and style replication flows while keeping later modules out of the primary navigation.
- Users need clear local image previews before starting product analysis or generation.

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/pages/HomePage.vue`
- `egg-user-web/src/layout/UserShell.vue`
- `egg-user-web/src/router/index.js`
- `egg-user-web/src/data/mock.js`
- `egg-user-web/src/styles/main.css`
- `egg-user-web/src/utils/imageUpload.js`
- `egg-user-web/src/utils/imageUpload.test.js`

Notes:

- This is a front-stage UI and local-preview behavior change only. Backend upload, storage, task, billing, tenant, payment, admin-console, and real video logic were not changed.
### Added Admin Prompt Template Management

Author: Codex

What changed:

- Added backend CRUD support for `ai_prompt_templates`.
- Added an Egg admin page for maintaining prompt templates with Markdown prompt body editing.
- Stored prompt content as JSONB using `{ "format": "markdown", "body": "..." }`.
- Kept `variables` optional and manually maintained as JSON.
- Added SQL for `created_by` numeric admin user IDs, `variables` defaults, and admin menu permissions.

Why:

- Image generation, RAG, and agent workflows need reusable, traceable prompt templates configured from the backend admin console.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/prompt`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/prompt`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/prompt`
- `egg-system/src/main/resources/mapper/ai/prompt`
- `egg-ui/src/api/ai/promptTemplate.js`
- `egg-ui/src/views/ai/promptTemplate/index.vue`
- `sql/ai_prompt_template_management.sql`

Notes:

- This is a backend admin configuration feature only. It does not expose prompt templates to front-stage users and does not add billing, tenants, teams, or video generation.

### Fixed Admin Dynamic Route Loading For Prompt Templates

Author: Codex

What changed:

- Normalized backend menu component paths to explicit `.vue` files before dynamic route loading in the admin frontend.

Why:

- The prompt template menu stored `ai/promptTemplate/index`, while the development webpack route context needed the exact Vue SFC path to load the newly added page.

Main files:

- `egg-ui/src/store/modules/permission.js`
- `docs/change-log.md`

Notes:

- This fixes backend admin page access only. No database schema, front-stage user page, billing, tenant, team, or video logic was changed.

## 2026-07-09

### Added AI Knowledge Admin Page

Author: Codex

What changed:

- Added an Egg admin API wrapper for AI knowledge documents.
- Added the admin knowledge page with search, manual file upload, status table, retry, delete, and pagination.

Why:

- Admin users need a management screen for uploaded RAG knowledge files and asynchronous vectorization status.

Main files:

- `egg-ui/src/api/ai/knowledge.js`
- `egg-ui/src/views/ai/knowledge/index.vue`
- `docs/feature-map.md`
- `docs/change-log.md`

Notes:

- This is an admin-console page only. It keeps the existing Egg admin style and does not add billing, tenants, teams, A2A, or video features.

### Fixed Persisted RAG Sources In Chat History

Author: Codex

What changed:

- Updated AI chat history responses so assistant messages can return their persisted retrieval sources.
- Added retrieval-source metadata persistence so vector record metadata is stored and replayed from chat history.
- Expanded the Task 7 chat service test coverage for message history source loading and metadata storage.

Why:

- Chat history needs to expose the same traceable RAG evidence that was stored during the original assistant reply.
- Retrieval metadata should not be lost between vector search and persisted source records.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiChatServiceImpl.java`
- `egg-system/src/main/resources/mapper/ai/chat/AiChatRetrievalSourceMapper.xml`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/chat/AiChatServiceImplTest.java`
- `sql/ai_knowledge_sensitive_rag.sql`

Notes:

- This is a backend chat persistence fix only. No billing, tenant, team, A2A, or video scope was added.

## 2026-07-09

### Added AI Sensitive Category And Word Backend CRUD

Author: Codex

What changed:

- Added backend domain, mapper, service, controller, and mapper XML support for `ai_sensitive_categories` and `ai_sensitive_words`.
- Added service tests that lock in default `enabled` status handling and blank-word validation for sensitive word writes.
- Added a friendly backend guard that prevents deleting sensitive word categories while they still contain words.

Why:

- The admin-side sensitive word management flow needs backend CRUD APIs before the RAG moderation and knowledge tooling can reuse this configuration safely.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive`
- `egg-system/src/main/resources/mapper/ai/sensitive`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/sensitive`

Notes:

- Category list/detail uses the existing `ai:sensitive:category:query` permission because the SQL menu set does not define `ai:sensitive:category:list`.

### Added AI Sensitive Word Admin Page

Author: Codex

What changed:

- Added an Egg admin API wrapper for AI sensitive word categories and words.
- Added the sensitive word management page with category and word tabs, search, add, edit, delete, status display, and pagination.

Why:

- Admin users need a backend-style management screen for the sensitive word configuration copied into the AI module.

Main files:

- `egg-ui/src/api/ai/sensitive.js`
- `egg-ui/src/views/ai/sensitive/index.vue`
- `docs/feature-map.md`
- `docs/change-log.md`

Notes:

- This is an admin-console configuration page only. It does not add moderation APIs, billing, tenants, teams, A2A, or video features.

### Planned AI Knowledge, Sensitive Word, And RAG Migration

Author: Codex

What changed:

- Documented the implementation direction for migrating knowledge base, sensitive word management, and RAG chat from `rag-ai`.
- Updated the project direction to use Alibaba Cloud OSS for AI files and images, and Milvus for RAG vectors, with PostgreSQL retained for business metadata.

Why:

- The user confirmed the storage direction change and requested these features under the current AI module structure.

Main files:

- `docs/project-constitution.md`
- `docs/feature-map.md`
- `.env.example`
- `docs/change-log.md`

### Added RAG Chat Persistence Backend

Author: Codex

What changed:

- Added backend chat session, message, and retrieval-source domains, mappers, XML mappings, and service logic for RAG chat persistence.
- Added a front-stage chat controller with session list/create, message history, and send-message endpoints.
- Added a mock chat-model service controlled by `egg.ai.mock-enabled` and a focused unit test for the chat send flow.

Why:

- Phase-one intelligent chat requires saved sessions, saved messages, retrieval-source traceability, and a backend service contract for the front-stage RAG chat page.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat`
- `egg-system/src/main/resources/mapper/ai/chat`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/chat`

Notes:

- The send-message flow now validates session ownership before saving anything, stores both user and assistant messages, and links retrieval sources to the assistant reply record.
