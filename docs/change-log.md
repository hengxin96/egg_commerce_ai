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

### Restored AI RAG And OSS Module Dependencies

Author: Codex

What changed:

- Added the missing OSS, Spring AI RAG, Milvus vector store, and Tika document reader dependencies to the system module.
- Left the DashScope starter out for now because the current version pulls a Spring Boot 3-era auto-configuration class that fails during Spring Boot 4 startup.

Why:

- AI storage and RAG service classes imported these libraries, but `egg-system` did not declare them, so the IDE could not resolve classes such as `com.aliyun.oss.OSS`.
- The backend currently uses the local `AiChatModelService` abstraction, so the DashScope starter is not required to start the application.

Main files:

- `egg-system/pom.xml`
- `docs/change-log.md`

Notes:

- This is a dependency fix only. No feature flow, database schema, front-stage UI, billing, tenant, team, or video logic was changed.

### Added Local RAG Vector Store Startup Fallback

Author: Codex

What changed:

- Changed the local default vector store from eager Milvus auto-configuration to a simple in-memory RAG vector store.
- Made the Milvus vector store service load only when `EGG_AI_VECTOR_STORE=milvus`.
- Added Spring AI vector-store type configuration so local startup does not attempt to connect to `localhost:19530`.

Why:

- The backend should start in the default local mock setup even when Milvus is not running.
- Real Milvus remains available by explicit configuration instead of being required during every local boot.

Main files:

- `egg-admin/src/main/resources/application.yml`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge/impl/AiKnowledgeDocumentServiceImpl.java`
- `docs/change-log.md`

Notes:

- This keeps the RAG service path usable for local development. Production-like Milvus testing still needs a running Milvus service and `EGG_AI_VECTOR_STORE=milvus`.

### Removed Chat Service Startup Dependence On ObjectMapper Bean

Author: Codex

What changed:

- Updated the Spring-injected chat service constructor to create its own JSON mapper for retrieval source metadata serialization.

Why:

- The current backend startup path did not expose a global `ObjectMapper` bean, so chat service construction failed before the application could start.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiChatServiceImpl.java`
- `docs/change-log.md`

Notes:

- This keeps JSON serialization local to the chat service. No API, database, billing, tenant, team, or video logic was changed.

### Marked Document Parser Spring Constructor

Author: Codex

What changed:

- Marked the environment-based document parser constructor as the Spring injection constructor.

Why:

- Spring could not choose between the runtime constructor and the test-friendly chunk-size constructor during application startup.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl/AiSimpleDocumentParserService.java`
- `docs/change-log.md`

Notes:

- This is a startup wiring fix only. No API, database, billing, tenant, team, or video logic was changed.

### Added DashScope EmbeddingModel Bridge For Milvus RAG

Author: Codex

What changed:

- Added a small project-owned DashScope embedding adapter that implements Spring AI `EmbeddingModel`.
- Registered the adapter from `egg.ai.model-gateway.embedding.*` configuration when the embedding provider is `dashscope`.
- Added focused tests for the embedding auto-configuration and DashScope response parsing.

Why:

- Spring AI Milvus vector store requires an `EmbeddingModel` bean during startup.
- The DashScope Spring starter was avoided because the available version pulls incompatible Spring Boot auto-configuration classes for this project.

Main files:

- `egg-framework/src/main/java/com/egg/commerce/framework/ai/model/DashScopeEmbeddingModel.java`
- `egg-framework/src/main/java/com/egg/commerce/framework/config/AiModelGatewayConfig.java`
- `egg-framework/src/test/java/com/egg/commerce/framework`
- `egg-framework/pom.xml`

Notes:

- Local startup still needs `EGG_DASHSCOPE_API_KEY` when real DashScope embedding calls are made.
- This fixes RAG embedding wiring only. No front-stage UI, database schema, billing, tenant, team, or video logic was changed.

### Documented AI Admin Management Code And Injection Points

Author: Codex

What changed:

- Added explicit constructor `@Autowired` markers across the prompt template, knowledge base, and sensitive-word admin code while keeping final dependencies.
- Added maintenance comments to related controllers, service implementations, service contracts, domain entities, and mapper interfaces.
- Normalized a couple of sensitive-word service error messages to Chinese for backend admin users.

Why:

- These backend admin modules are part of the phase-one RAG and compliance management surface, so their dependency wiring and business intent should be easy to understand and maintain.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai`
- `docs/change-log.md`

Notes:

- This is a maintainability-only change. It does not change API paths, database schema, frontend pages, billing, tenant, team, or video logic.

### Strengthened Default Code Comment Rules

Author: Codex

What changed:

- Updated the project constitution's Clear Code Comments rule so Codex and AI agents must add maintenance comments by default when generating or changing code.
- Updated the root agent instructions to point code changes back to the constitution's comment rule.
- Bumped the constitution patch version to `1.2.1`.

Why:

- Future Codex threads should preserve maintainable comments automatically instead of treating comments as an optional per-task request.

Main files:

- `docs/project-constitution.md`
- `AGENTS.md`
- `docs/change-log.md`

Notes:

- No new rule file was created. This is a documentation and agent-guidance change only.

## 2026-07-10

### Upgraded Admin Knowledge Base Upload Page

Author: Codex

What changed:

- Restyled the Egg admin "我的知识库" page around the RAG project's large drag-and-drop upload area, batch toolbar, and table layout.
- Added multi-file knowledge upload with `pdf/doc/docx/md/xls/xlsx/txt` validation and a 100MB per-file limit on both frontend and backend.
- Added backend batch upload and OSS download URL lookup APIs, plus frontend single and batch download actions.
- Added focused service tests for batch upload, file validation, and download URL lookup.

Why:

- The previous admin knowledge upload flow only accepted one file, allowed unsupported formats, and had an awkward upload submission path.
- Knowledge files are first-phase RAG inputs, so admin upload/download needs to be reliable and traceable.

Main files:

- `egg-ui/src/views/ai/knowledge/index.vue`
- `egg-ui/src/api/ai/knowledge.js`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/knowledge/AiKnowledgeDocumentController.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/knowledge`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/knowledge/AiKnowledgeDocumentServiceImplTest.java`

Notes:

- This changes the Egg-style backend admin knowledge page only. No front-stage Picset workspace, billing, tenant, team, payment, or video behavior was changed.

### Fixed Admin Knowledge Route Loading In Development

Author: Codex

What changed:

- Updated the Egg admin dynamic view loader to resolve development routes through an explicit `views/**/*.vue` webpack context.

Why:

- The newly added knowledge page existed on disk, but the running development bundle could still fail to resolve `ai/knowledge/index.vue` from a loose dynamic require context.

Main files:

- `egg-ui/src/store/modules/permission.js`
- `docs/change-log.md`

Notes:

- This fixes backend admin page access only. No backend API, database schema, front-stage workspace, billing, tenant, team, payment, or video behavior was changed.

### Added Chinese Comments To AI Backend Files

Author: Codex

What changed:

- Added concise Chinese maintenance comments to backend `Ai*` Java and mapper XML files that did not already have comments.
- Translated existing English comments in backend `Ai*` files to Chinese.

Why:

- The backend AI modules should follow the constitution's clear-code-comment rule and be easier for future maintainers to scan.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai`
- `egg-framework/src/main/java/com/egg/commerce/framework/config`
- `egg-system/src/main/java/com/egg/commerce/system`
- `egg-system/src/main/resources/mapper/ai`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai`

Notes:

- This is a comments-only maintainability change. No APIs, SQL behavior, database schema, frontend pages, billing, tenant, team, payment, or video behavior was changed.

### Fixed Knowledge Document UUID Binding

Author: Codex

What changed:

- Updated the knowledge document MyBatis mapper to cast string `id` parameters to PostgreSQL `uuid` values for insert, lookup, update, and delete statements.

Why:

- Uploading knowledge files failed because `ai_knowledge_documents.id` is a UUID column while MyBatis bound the Java string ID as a varchar parameter.

Main files:

- `egg-system/src/main/resources/mapper/ai/knowledge/AiKnowledgeDocumentMapper.xml`
- `docs/change-log.md`

Notes:

- This is a database binding fix only. No schema, frontend page, billing, tenant, team, payment, or video behavior was changed.

### Added Method Comments To AI Backend Files

Author: Codex

What changed:

- Added concise Chinese method comments to `Ai*` backend controller, service, mapper, RAG, storage, XML mapper, and focused test files where method comments were missing.
- Kept trivial getter/setter accessors uncommented to avoid comments that only restate syntax.
- Fixed several existing Chinese line comments that had accidentally swallowed the following Java statement onto the same line.
- Updated sensitive-word service tests to assert the current Chinese validation messages.

Why:

- The AI backend workflow should be easier to maintain and should follow the constitution's clear-code-comment rule at method boundaries.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai`
- `egg-framework/src/main/java/com/egg/commerce/framework/config`
- `egg-system/src/main/java/com/egg/commerce/system`
- `egg-system/src/main/resources/mapper/ai`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai`

Notes:

- This is a maintainability-focused comments change. The same-line comment repair restores intended statement execution but does not introduce new API, schema, frontend, billing, tenant, team, payment, or video behavior.

### Fixed Knowledge Vectorization For Office Documents

Author: Codex

What changed:

- Changed knowledge document parsing so `doc/docx/pdf/xls/xlsx` files are extracted with Tika before chunking instead of being treated as UTF-8 binary text.
- Aligned `commons-compress` with Tika 3.x so Office document parsing works at runtime.
- Added `text_type=document` to DashScope embedding requests used for knowledge chunks.
- Added explicit Spring AI Milvus properties for host, database, collection, embedding dimension, and schema initialization.

Why:

- Uploaded Word files were saved to PostgreSQL and OSS, but vectorization failed because raw DOCX bytes were sent to DashScope embedding, producing HTTP 400 `InvalidParameter` and leaving documents in `failed` status with no Milvus rows.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl/AiSimpleDocumentParserService.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/rag/AiDocumentParserServiceTest.java`
- `egg-framework/src/main/java/com/egg/commerce/framework/ai/model/DashScopeEmbeddingModel.java`
- `egg-framework/src/test/java/com/egg/commerce/framework/ai/model/DashScopeEmbeddingModelTest.java`
- `egg-admin/src/main/resources/application.yml`
- `pom.xml`

Notes:

- Failed existing knowledge rows need to be retried from the admin page after restarting the backend.

### Fixed Local Milvus Startup Host Defaults

Author: Codex

What changed:

- Changed the default Milvus host from the unreachable LAN address to `localhost`.
- Made Spring AI Milvus schema initialization opt-in through `EGG_MILVUS_INITIALIZE_SCHEMA`.
- Added Milvus environment examples for the local admin backend setup.

Why:

- Backend restart failed because the app tried to create the Spring AI Milvus client against `192.168.31.78:19530`, which was not reachable on the current machine while `localhost:19530` was reachable.

Main files:

- `egg-admin/src/main/resources/application.yml`
- `.env.example`

Notes:

- Existing knowledge rows can still be retried from the admin page after the backend starts.

### Batched Knowledge Embedding Writes

Author: Codex

What changed:

- Split Milvus knowledge document writes into batches of at most 10 chunks before calling Spring AI vector store.
- Added a focused test that verifies larger documents are written as multiple DashScope-sized batches.

Why:

- DashScope embedding rejects requests with more than 10 text inputs, so documents with more than 10 chunks failed vectorization even though parsing, OSS storage, and PostgreSQL metadata were successful.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag/impl/AiSpringMilvusVectorStoreServiceImpl.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/rag/AiSpringMilvusVectorStoreServiceImplTest.java`

Notes:

- Existing failed knowledge rows can be retried from the admin page after restarting the backend.

### Changed Prompt Template Category To Dictionary Select

Author: Codex

What changed:

- Changed the prompt template admin page category filter, form field, table column, and detail display to use the `prompt_type` dictionary.

Why:

- Prompt template categories are now centrally maintained through the Egg dictionary system instead of being free-form text in the page.

Main files:

- `egg-ui/src/views/ai/promptTemplate/index.vue`
- `docs/change-log.md`

Notes:

- This is an Egg admin UI change only. No table schema, backend API, front-stage workspace, billing, tenant, team, payment, or video behavior was changed.

## 2026-07-11

### Reworked Story Generation Setup Form

Author: Codex

What changed:

- Configured the front-stage story generation page with its own seven-step progress labels.
- Hid the product image upload area, main/detail image tabs, AI helper button, image quality, and generation count controls on the story page.
- Changed the story page form to collect story theme, product information, target audience, recommended style, and story duration while keeping the shared workspace defaults for other pages.
- Added a focused Node test for the story-page workspace configuration.

Why:

- The story generation workflow is text-led and should not look like the product-image generation flow.

Main files:

- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`

Notes:

- This is scoped to the front-stage story generation setup. Product image, style replication, backend, admin, billing, tenant, team, payment, and real video behavior were not changed.

### Connected Story Generation To Async Backend Workflow

Author: Codex

What changed:

- Added a single wide `ai_story_tasks` table for story task inputs, node progress, fixed-stage outputs, prompt template snapshots, platform rule snapshots, model snapshots, and failure reasons.
- Added `ai_story_platform_rules` with published platform profiles/rules for 抖音、快手、视频号、小红书.
- Added the backend story task service that runs 剧情生成 -> 剧本生成 -> 分镜生成 -> 分镜校验 -> 三视图/场景图 serially in the thread pool.
- Added DeepSeek-compatible text generation wiring for this story chain, with local mock mode still available.
- Added Word and Markdown export for story task content.
- Connected the user story generation page and task center to real backend APIs and dictionary values.

Why:

- The front-stage story generation workflow needs to create real asynchronous backend tasks, update page progress by node, preserve each stage output, and let users view/download completed content.

Main files:

- `sql/ai_story_generation.sql`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/story/AiStoryTaskController.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/story`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/story`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/story`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/model`
- `egg-user-web/src/api/story.js`
- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`

Notes:

- The 三视图/场景图 stage currently generates prompt text only; image generation can be connected later.
- Prompt templates are read from `ai_prompt_templates`; story generation uses platform name prefixes such as `抖音-` to locate platform-specific story prompts.
- The current branch was used directly; no new branch was created.
## 2026-07-12

### Added Front-Stage Phone Login And Auto Registration

Author: Codex

What changed:

- Hid the front-stage “邀请有礼” header button and kept the avatar as the login entry.
- Reworked the user login page into a phone-only login/register entry with a first-version forgot-password reset flow.
- Added frontend auth helpers, token storage, route guards for intelligent chat and task center, and action-level login checks for generation buttons.
- Added backend front-stage auth endpoints for phone login-or-register and password reset without changing the admin login/captcha flow.
- Mapped `sys_user.user_type` in the Java entity and MyBatis mapper so front-stage users are saved with `user_type = 03`.

Why:

- Front-stage users need a simple phone-based entry before using protected generation, chat, story, and task-center features.

Main files:

- `egg-user-web/src/pages/LoginPage.vue`
- `egg-user-web/src/api/auth.js`
- `egg-user-web/src/utils/authGuard.js`
- `egg-user-web/src/router/index.js`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/layout/UserShell.vue`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/system/FrontUserAuthController.java`
- `egg-framework/src/main/java/com/egg/commerce/framework/web/service/FrontUserAuthService.java`
- `egg-common/src/main/java/com/egg/commerce/common/core/domain/entity/SysUser.java`
- `egg-system/src/main/resources/mapper/system/SysUserMapper.xml`

Notes:

- Password rules stay aligned with the current system range: 5 to 20 characters.
- Forgot-password reset intentionally has no SMS verification in this first version; this should be hardened later.

### Fixed Story Generation Dictionary Select Defaults

Author: Codex

What changed:

- Updated the shared generator workspace to sync select values after async dictionary options are loaded.
- Updated story generation fallback options so target audience, style, and duration match the current `story_target_audience`, `story_style`, and `story_duration` dictionaries.
- Added dictionary-load fallback logging and made empty filtered dictionary results fall back to valid story options.
- Corrected the user-web environment example to use `VITE_API_BASE_URL`, while the story API wrapper remains compatible with the older `VITE_API_BASE` name.
- Allowed anonymous GET access only to the four story-generation dictionary types needed by the public story page.
- Added a focused frontend source test for async dictionary option synchronization.

Why:

- The story generation page was still showing fallback values for target audience, style, and duration after the real dictionaries should have loaded.
- When dictionary requests fail or return unusable data, the page should not show obsolete placeholder values.

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/api/story.js`
- `egg-user-web/.env.example`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`
- `egg-framework/src/main/java/com/egg/commerce/framework/config/SecurityConfig.java`

Notes:

- Only `story_platform`, `story_target_audience`, `story_style`, and `story_duration` are publicly readable; other system dictionaries still require authentication.

### Fixed User-Web API Proxy Prefix

Author: Codex

What changed:

- Added a Vite development proxy rewrite so frontend requests under `/api` are forwarded to the backend without the `/api` prefix.
- Added a focused Vite config source test to keep the proxy rewrite in place.

Why:

- Story generation submitted `/api/ai/story/tasks`, but the backend controller is mounted at `/ai/story/tasks`, so the backend treated `/api/ai/story/tasks` as a missing static resource.

Main files:

- `egg-user-web/vite.config.js`
- `egg-user-web/vite.config.test.js`

### Fixed Story Generation Model Output And Export Downloads

Author: Codex

What changed:

- Changed the story text-generation bean selection to use `egg.ai.model-gateway.story.provider`, so the story workflow defaults to DeepSeek and only uses mock when story provider is explicitly set to `mock`.
- Marked the DeepSeek text generator runtime constructor with `@Autowired`, so Spring can create the bean after DeepSeek becomes the default story provider.
- Added DeepSeek configuration fallback lookup for raw environment variable names and JVM `-D` properties, with a clearer missing-key error message for IDEA local debugging.
- Updated the local mock text generator so it returns a clear placeholder instead of echoing the full rendered prompt as generated content.
- Upgraded the managed POI/XMLBeans dependencies and declared `poi-ooxml` directly in `egg-system` for readable Word export.
- Hardened the user frontend story download API so backend JSON errors are shown as errors instead of being saved as corrupt `.docx` files.

Why:

- Story stages were displaying prompt text because the mock generator was active and echoed the prompt.
- Enabling DeepSeek by default exposed that the service had two public constructors and no explicit Spring injection constructor.
- IDEA can run EggApplication in a JVM process that has not inherited newly edited Windows system variables, so the DeepSeek key needs explicit runtime-source diagnostics.
- Word export could fail at runtime with a missing XMLBeans class, and the frontend saved that JSON error as a Word file.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/model`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/story/impl/AiStoryTaskServiceImpl.java`
- `egg-user-web/src/api/story.js`
- `pom.xml`
- `egg-system/pom.xml`
- `.env.example`

Notes:

- Runtime environments must set `EGG_MODEL_STORY_PROVIDER=deepseek` and provide `EGG_DEEPSEEK_API_KEY` to generate real content.
- Existing tasks generated while mock was active still contain mock/prompt-like outputs; create a new story task after restarting with the corrected configuration.

### Fixed Front-Stage Stale Token Handling For Story Generation

Author: Codex

What changed:

- Restricted front-stage login detection to the user frontend's own `User-Token` instead of accepting legacy or admin-style token keys.
- Added shared authentication-failure detection for backend `401` responses, including the Egg JSON `code=401` shape.
- Updated story generation requests, polling, and downloads to clear stale local sessions and send users back to login when the backend rejects the token.

Why:

- After a backend or Redis restart, the browser could still hold an old token and the story page looked logged in even though `/ai/story/tasks` was correctly rejected by Spring Security.

Main files:

- `egg-user-web/src/api/auth.js`
- `egg-user-web/src/api/story.js`
- `egg-user-web/src/utils/authGuard.js`
- `egg-user-web/src/pages/StoryGenerationPage.vue`

Notes:

- Users who see this once should log in again; the frontend will now clear the stale token instead of repeatedly showing the raw backend authentication error.

## 2026-07-12

### Updated Story Task Viewing And Result Scrolling

Author: Codex

What changed:

- Renamed task-center download actions from `Word` and `MD` to `下载为Word` and `下载为MD`.
- Changed `查看内容` in the task center to open the generated story content in a dialog, without Word or Markdown download buttons inside the dialog.
- Constrained story-generation result text boxes so long node output scrolls inside a fixed area instead of growing the whole result panel.
- Added focused frontend regression tests for the task-center dialog behavior and story result scrolling.

Why:

- Story task details should be reviewed in a focused popup, and long generation output should remain readable without pushing the page layout longer.

Main files:

- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/pages/TasksPage.test.js`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`
- `egg-user-web/src/styles/main.css`

Notes:

- This is scoped to the front-stage task center and story-generation result display. Backend, admin, billing, tenant, team, payment, and real video behavior were not changed.

## 2026-07-13

### Improved Story Generation Live Progress Display

Author: Codex

What changed:

- Changed the story-generation result title to show an in-progress title with a spinning loading icon while a story task is being created or processed.
- Updated the story page to follow the backend current node immediately, so the top progress stage and selected result panel advance from the input stage to the story-generation stage as soon as the task reaches that node.
- Expanded single-node story result output to fill the available result canvas and keep long content scrollable inside that fixed area.
- Added focused frontend regression tests for generating-state title display, result-panel fill behavior, and backend-node-following behavior.

Why:

- Users need clear live feedback while generation is still running, and the page should reflect backend progress without requiring manual stage selection.

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`
- `egg-user-web/src/styles/main.css`

Notes:

- This is scoped to the front-stage story-generation workspace. Other pages and backend behavior were not changed.

### Added Story Task Retry From Failed Nodes

Author: Codex

What changed:

- Added `POST /ai/story/tasks/{id}/retry` so failed story-generation tasks can continue in the same task row.
- Added backend recovery logic that starts from the failed node, or walks back to the nearest node whose upstream output is available.
- Preserved successful upstream node output while clearing the retry node and dependent downstream node output, prompt snapshots, template snapshots, and failure reason.
- Added a task-center `重试` action only for failed tasks; processing and completed tasks do not show the retry action.
- Added backend and frontend regression tests for retry status rules, node recovery, API calls, and task-center display.

Why:

- Users need to recover from model or network failures without recreating the whole story task or losing already generated upstream content.

Main files:

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/story/AiStoryTaskController.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/story`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/story/AiStoryTaskServiceImplTest.java`
- `egg-user-web/src/api/story.js`
- `egg-user-web/src/api/story.test.js`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/pages/TasksPage.test.js`

Notes:

- Retry reuses the existing `ai_story_tasks` row and does not create retry history records in this iteration.
- Completed tasks return “当前任务已完成，无需重试”; processing tasks return “任务正在生成中，请稍后”.

### Refined Story Generation In-Progress Labels

Author: Codex

What changed:

- Changed the story-generation result title to show `生成结果` when the selected node already has generated content, even if later nodes are still processing.
- Changed the active node's empty result placeholder from `未生成` to `正在生成` while that node is still processing.
- Kept the change scoped to front-stage story-generation result display; backend generation remains non-streaming.

Why:

- Users need to distinguish completed node results from the currently running node without waiting for the full task to finish.

Main files:

- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`

### Refined Story Step Selection State

Author: Codex

What changed:

- Added a separate selected-completed state for story-generation progress steps.
- Changed completed historical steps selected by the user to use a light green tint instead of the primary black state.
- Kept the latest current step in the original black selected/current style.

Why:

- Users need a clearer distinction between reviewing completed historical output and tracking the latest active node.

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/styles/main.css`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`

### Restricted Task Center Downloads To Completed Tasks

Author: Codex

What changed:

- Hid Word and MD download actions for story tasks that are still processing.
- Added a download guard so non-completed tasks cannot trigger exports through the task-center action path.
- Added a focused frontend regression test for the completed-only download rule.

Why:

- In-progress story tasks do not yet have final exportable content, so download actions should appear only after completion.

Main files:

- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/pages/TasksPage.test.js`

### Added Story Task Terminal Notice

Author: Codex

What changed:

- Added a bottom-right in-page notice when a story task completes or fails.
- Completion copy points users to the task center to view the finished task.
- Failure copy points users to the task center to inspect the failure reason or retry.
- Styled the notice with the front-stage platform card language: white surface, light border, 8px radius, restrained shadow, and state color accents.

Why:

- Users need a visible status handoff after background story generation reaches a terminal state.

Main files:

- `egg-user-web/src/pages/StoryGenerationPage.vue`
- `egg-user-web/src/styles/main.css`
- `egg-user-web/src/components/GeneratorWorkspace.story.test.js`

## 2026-07-15

### Added Full-Category Product Image Generation

Author: Codex

What changed:

- Added the first real full-category product image generation workflow: invisible product drafts, immediate OSS upload, AI analysis/planning/generation nodes, DashScope/Tongyi Wanxiang image generation, AI requirement rewrite, cancellation, retry from failed node, and completion notice.
- Added product-library storage for successful generated assets, including uploaded source images, selected parameters, generated images, rename, reuse, download, and delete with OSS cleanup.
- Added task-center support for product image tasks, including node details, failure retry, processing-task cancellation, and generated-image missing-state copy.
- Added scheduled cleanup for stale draft uploads and orphan product-image OSS objects.

Why:

- Users need the product-image feature to create real downloadable ecommerce images, persist successful results by user, and continue generation from saved product data.

Main files:

- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/pages/ProductsPage.vue`
- `egg-user-web/src/pages/ProductDetailPage.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/api/productImage.js`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/image`
- `sql/ai_product_image_generation.sql`

Notes:

- Prompt content is read from the existing prompt-template feature; no duplicate prompt template seed data is added here.
- Manual smoke still requires valid DashScope and Ali OSS credentials plus the product-image SQL migration.

## 2026-07-16

### Added Copy-Ready AI Requirement Rewrite Prompt

Author: Codex

What changed:

- Appended a copy-ready `AI帮写优化提示词` body to the v3 full-category product image prompt document.
- Aligned the prompt with the current rewrite service template name, `AI优化提示词` category, available backend variables, and JSON response shape.
- Avoided obsolete `{assetType}` and `{imageCount}` placeholders because the current backend does not inject them into the rewrite template.

Why:

- The product image quality upgrade changed the generation workflow, but the AI helper rewrite prompt still reflected the older v2 prompt assumptions.

Main files:

- `docs/prompts/AI生图提示词方案v3_全品类商品图质量升级.md`

Notes:

- This is a prompt-document update only. Runtime code was not changed.

### Reformatted Product Image Prompt Template Document

Author: Codex

What changed:

- Rewrote the v3 full-category product image prompt document into two copy-ready backend prompt template bodies.
- Kept `主图系统提示词` and `详情图系统提示词` aligned with the current `AI生图` category, backend variables, and `content.body` storage format.

Why:

- The previous document was a prompt design guide with split snippets, which was hard to paste into the admin prompt-template editor directly.

Main files:

- `docs/prompts/AI生图提示词方案v3_全品类商品图质量升级.md`

Notes:

- This is a documentation/template-format change only. Runtime code was not changed.

### Implemented Product Image Quality Upgrade Phase 1-3

Author: Codex

What changed:

- Upgraded full-category product image generation to run real vision analysis first, stop at a user-confirmable planning step, and continue generation only after confirmation.
- Added source-image ordering and primary-reference selection, plus confirm/replan task APIs.
- Added base/final image preservation: model output is stored as `generated_base`, backend-composed final output is stored as `generated`, and user pages only display/download final images.
- Added a Java2D text-composition service for controlled ecommerce text overlay.
- Added non-blocking product image audit: local rule checks plus vision-model audit write `ruleAudit`, `visionAudit`, and `auditSummary` into `generation_output`.
- Updated product generation, product detail, and task center UI to show final-image audit status badges.
- Aligned DashScope `wan2.7-image-pro` calls with supported parameters by avoiding `negative_prompt`/`prompt_extend` and explicitly setting `watermark=false`.

Why:

- The product image workflow needed competitor-style visual understanding, editable generation planning, controlled text rendering, final-only user display, and a quality feedback loop while still producing real OSS-backed images.

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/model`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/pages/ProductDetailPage.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-user-web/src/components/product/ProductImageAuditBadge.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `docs/prompts/AI生图提示词方案v3_全品类商品图质量升级.md`
- `.env.example`
- `egg-admin/src/main/resources/application.yml`

Notes:

- Audit warnings do not block task completion; the generated final images remain visible with risk status.
- Manual smoke still requires valid DashScope and OSS credentials.

### Planned Product Image Quality Upgrade

Author: Codex

What changed:

- Added a Chinese implementation planning document for upgrading the full-category product image workflow.
- The plan covers true visual understanding with `qwen3.7-plus`, confirmable planning, deterministic text composition, base/final image preservation, and rule plus visual audit.
- Captured Ali DashScope parameter boundaries for `qwen3.7-plus` Chat Completions and `wan2.7-image-pro` image generation.

Why:

- The current product image workflow generates real assets but lacks the competitor-style visual analysis, editable planning, controlled text overlay, and post-generation quality loop needed for stronger ecommerce outputs.

Main files:

- `docs/2026-07-16-product-image-quality-upgrade-plan.zh-CN.md`

Notes:

- This is a planning/documentation change only. Code execution should start after user approval of the plan.

### 调整商品图上传缩略图与 AI 帮写交互

Author: Codex

What changed:

- 将上传缩略图删除按钮从左上角移动到右上角。
- 将缩略图编号改为更小的半透明角标，并与右上角删除按钮的边距对齐。
- 将第一张图的半透明“主参考”标签移动到缩略图底部居中。
- 去掉其余缩略图上的“设为主图”按钮，保留拖动排序决定主参考图。
- 修复 AI 帮写按钮在文本域表单项内点击不稳定的问题：按钮不再嵌套在 `label` 中，点击会直接交给父页面调用后端帮写接口，并补充前端回归测试。

Why:

- 商品图上传区需要减少视觉干扰，让用户通过拖动第一张图来决定主参考图，同时保证 AI 帮写按钮点击后有明确响应。

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/styles/main.css`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`

### 升级全品类商品图文字开关与提示词参数链路

Author: Codex

What changed:

- Added a `图片带文字` yes/no control to the full-category product image workspace, defaulting to no.
- Passed target language, text-enabled mode, selected model, aspect ratio, clarity, and image count into backend task params and prompt variables.
- Made no-text mode force `textOverlay.enabled=false`, skip Java2D text composition, and return generated base-image content as the final image.
- Updated Java2D text composition to read planning `typography` for position, color, shadow, stroke, opacity, and lightweight panel metadata.
- Moved product image visual analysis and visual audit prompts to published backend templates.
- Added 2K DashScope size mapping for supported ratios and created the v4 copy-ready prompt document.

Why:

- Full-category product image generation needs cleaner no-text outputs, style-adaptive text planning when enabled, better prompt control, and more visible clarity/model behavior.

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/model/impl/AiDashScopeImageGenerationServiceImpl.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiJava2DProductImageCompositionServiceImplTest.java`
- `docs/feature-map.md`
- `C:\Users\14350\Desktop\AI生图提示词\AI生图提示词方案v4_全品类商品图质感与文字升级.md`

Notes:

- Backend visual analysis uses `商品图视觉分析提示词` / `AI视觉分析`.
- Backend visual audit uses `商品图视觉审核提示词` / `AI视觉审核`.

### 收窄商品图 AI 帮写上下文

Author: Codex

What changed:

- Changed product-image AI requirement rewrite so it only receives the current user requirement, target platform, and platform rules.
- Removed target language, text-enabled mode, image count, asset type, and product asset context from the frontend rewrite payload.
- Updated the v4 copy-ready prompt document so `AI帮写优化提示词` no longer asks for system generation parameters.

Why:

- AI 帮写 should only make the user's current main-image requirement easier for the model to understand; generation parameters belong to the later system planning prompts.

Main files:

- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductRequirementRewriteServiceImpl.java`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductRequirementRewriteServiceImplTest.java`
- `C:\Users\14350\Desktop\AI生图提示词\AI生图提示词方案v4_全品类商品图质感与文字升级.md`

### 修复商品图临时图片下载失败的脆弱点

Author: Codex

What changed:

- 将商品图任务下载 DashScope 临时图片的逻辑从单次 `URL.openStream()` 改为带超时、HTTP 状态检查和短重试的 `HttpClient` 下载。
- 失败原因现在会保留脱敏后的下载源 host、HTTP 状态或网络异常摘要，避免只显示泛化的“下载生成图片失败”。
- 商品图任务失败时会写入服务端 warning/error 日志，便于按任务 id 和节点继续追踪。
- 添加了本地 HTTP 复现测试，覆盖首次 503、重试后成功的临时下载场景。

Why:

- 实际失败任务的 DashScope 图片生成已经成功，问题出在后端转存前下载临时 OSS 图片时；原代码单次下载且吞掉底层异常，容易把瞬时网络或 OSS 状态抖动变成不可诊断的终态失败。

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java`

### 修复生成图 MIME 元数据缺失导致的审核误报

Author: Codex

What changed:

- 生成图以字节流上传 OSS 时，根据扩展名推导 `image/png`、`image/jpeg`、`image/webp` 等 MIME 类型。
- 上传 OSS 时同步设置对象 `Content-Type`，并把 MIME 写回 `AiStoredObject`，供图片表和 `generation_output.images[].finalMimeType` 持久化。
- 添加存储层回归测试，覆盖生成图扩展名到 MIME 类型的推导。

Why:

- 商品图最终图已经生成成功，但 `uploadGeneratedImage` 之前返回的 `mimeType` 是空，导致本地审核规则把每张最终图标记为 `Final generated image MIME type is missing.`。

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/storage/impl/AiAliOssObjectStorageServiceImpl.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/storage/AiAliOssObjectStorageServiceImplTest.java`

## 2026-07-17

### 调整生成图预览按钮样式

Author: Codex

What changed:

- 将商品图生成结果的预览按钮从白底放大镜改为深色半透明圆形眼睛按钮。
- 商品库详情页共享图库和任务中心单图预览兜底样式保持一致。

Why:

- 预览入口需要更贴近上传图片区块里的悬浮眼睛按钮样式，降低界面风格割裂。

Main files:

- `egg-user-web/src/components/product/SelectableImageGallery.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/pages/ProductDetailPage.test.js`
- `egg-user-web/src/pages/TasksPage.test.js`

### 优化商品图预览和任务中心实时刷新

Author: Codex

What changed:

- 商品库详情页和任务中心「查看内容」里的生成图卡片新增预览按钮，点击后可放大查看图片。
- 任务中心增加静默自动刷新，列表和已打开的任务详情会定时拉取最新状态。
- 顶部主导航隐藏「风格复刻」入口，保留相关页面和路由代码以便后续恢复。

Why:

- 生成图需要更方便地逐张检查细节，任务状态也需要自动更新，避免用户反复手动刷新才看到结果或提示。

Main files:

- `egg-user-web/src/components/product/SelectableImageGallery.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/data/mock.js`
- `egg-user-web/src/layout/UserShell.navigation.test.js`
- `egg-user-web/src/pages/ProductDetailPage.test.js`
- `egg-user-web/src/pages/TasksPage.test.js`

### 打通任务中心商品图规划确认

Author: Codex

What changed:

- 任务中心商品图「查看内容」复用全品类商品图的规划确认编辑器，不再单独展示 AI 分析 JSON。
- 商品图任务在任务中心重试并重新生成规划后，会在右下角提示用户查看并确认规划，直达按钮会打开当前任务详情弹窗。
- 任务中心可直接确认生成或重新规划，重新规划可提交补充要求。
- 商品库新生成的默认未命名商品会用视觉分析 `productNameGuess` 自动命名，用户已重命名的商品不覆盖。
- 商品详情页改为生成图片在上、商品信息在下，并只读展示最新成功任务的整体设计规范。

Why:

- 规划节点失败后从任务中心重试仍需要回到可确认的规划闭环，同时商品库和详情页要展示更有意义的商品名称与设计依据。

Main files:

- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/pages/ProductDetailPage.vue`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product/AiProductAssetDetail.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductAssetServiceImpl.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`

### 优化商品图规划确认和多方案生成

Author: Codex

What changed:

- 将商品图「分析中」和「生成中」右侧输出改为简短状态文案加加载进度条，不再展示中间 JSON。
- 将「确认规划」内容移入右侧输出框，展示 AI 视觉分析、整体设计规范、分图方案和确认按钮。
- 主图多张生成改为必须规划足量且不同的 `imagePrompts`，不足时自动重规划最多 2 次，仍不足则失败并提示 `规划方案数量不足，请重新规划`。
- 生成阶段按每个主图方案单独请求 1 张图，保证分图方案、文字规划和最终图一一对应。
- 清洗规划 JSON 中的重复标点，并生成 v5 多方案规划提示词文档。

Why:

- 避免生成数量为 2 时只有 1 个分图方案、最终图片过度相似、文字规划不可见，以及规划内容把页面撑长的问题。

Main files:

- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java`
- `C:\Users\14350\Desktop\AI生图提示词\AI生图提示词方案v5_全品类商品图多方案规划优化.md`

### 优化商品图任务节点视觉反馈

Author: Codex

What changed:

- 将商品图「分析中」和「生成中」节点的加载进度条固定展示在结果输出框上方。
- 重排「确认规划」节点内容，增加规划摘要、分区编号、紧凑的视觉分析区和更清晰的分图方案编辑区。
- 修复分图方案字段标签挤压和灰底过重的问题，让标题、设计目标、文案、文字位置和融合字效更易读。

Why:

- 问题记录 05-08 显示加载条位置偏下、确认规划内容像粗糙表单，影响用户确认规划和编辑多图文案方案。

Main files:

- `egg-user-web/src/components/GeneratorWorkspace.vue`
- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`

### 修复详情图规划兼容字段误判

Author: Codex

What changed:

- 后端商品图规划解析会跳过空的顶层 `imagePrompt`，继续从 `imagePrompts` 或 `imagePlans` 中读取有效详情图方案。
- 增加回归测试，覆盖详情图提示词返回 `imagePrompt: {}` 但 `imagePrompts` 有效时仍可进入确认规划并生成。
- 生成 v6 详情图提示词文档，要求 `imagePrompt` 必须等于 `imagePrompts[0]`，不再输出空对象。

Why:

- 详情图生成任务在确认规划前失败，原因是提示词兼容字段为空对象，后端旧解析逻辑误判为缺少有效 `imagePrompt`。

Main files:

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/product/impl/AiProductImageTaskServiceImpl.java`
- `egg-system/src/test/java/com/egg/commerce/system/service/ai/product/AiProductImageTaskServiceImplTest.java`
- `C:\Users\14350\Desktop\AI生图提示词\AI生图提示词方案v6_全品类商品图详情规划兼容修复.md`

### 升级对标式商品图规划提示词与确认页面

Author: Codex

What changed:

- 生成 v7 商品图提示词文档，覆盖主图、详情图、AI 帮写、视觉分析和视觉审核 5 个后台模板。
- 将商品图确认规划页升级为对标式层级展示：整体设计规范展示视觉风格、氛围营造、色彩系统、字体系统和视觉语言；分图方案展示产品出现、产品还原、内容要素和只读生图提示词。
- 保留重点字段可编辑，`positivePrompt` 只读展示，避免用户误改真正进入图片模型的核心提示词。

Why:

- 商品图规划节点需要更接近对标产品的大规划 / 小规划结构，同时保持后端生图链路兼容和可确认。

Main files:

- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`
- `C:\Users\14350\Desktop\AI生图提示词\AI生图提示词方案v7_对标式规划节点与全品类商品图提示词升级.md`

### 调整商品图规划确认布局和详情轮询

Author: Codex

What changed:

- 将商品图“确认规划”中的 AI 视觉分析和整体设计规范改为上下排列。
- 调整分图方案卡片：方案编号缩小，标题输入框不再预留左侧空位，图像定位、视觉执行、内容要素、文字内容等区域改为铺满单列。
- 暂时隐藏分图方案里的“只读生图提示词”展示，但保留 `positivePrompt` 数据进入确认生成链路。
- 收紧商品图生成页和任务中心的单任务详情轮询，确认规划、完成、失败等静止状态不再反复查询同一条任务详情。
- 商品详情页的整体设计规范改为和确认规划节点一致的分组明细展示，避免嵌套对象显示为 `[object Object]`。

Why:

- 这些调整让规划确认页更接近对标产品的信息层级，减少右侧空白和后台重复查询噪声，并让商品库详情能正确复盘规划内容。

Main files:

- `egg-user-web/src/components/product/ProductImagePlanEditor.vue`
- `egg-user-web/src/pages/ProductImagesPage.vue`
- `egg-user-web/src/pages/ProductDetailPage.vue`
- `egg-user-web/src/pages/TasksPage.vue`
- `egg-user-web/src/pages/ProductImagesPage.product-image.test.js`
- `egg-user-web/src/pages/ProductDetailPage.test.js`
- `egg-user-web/src/pages/TasksPage.test.js`

### Remove Legacy Storage References From Core Docs And Local Config

Author: Codex

What changed:

- Replaced legacy storage references in the project constitution, README, feature map, and phase-one module configuration with current object-storage wording.
- Updated `phase-one-module-configuration.md` and `.env.example` to use Ali OSS environment variables instead of legacy storage variables.
- Removed the legacy object-storage service from `compose.postgres.yml` so local startup only brings up PostgreSQL and Redis.

Why:

- The project now uses object storage instead of the legacy storage setup, so the core docs and local configuration examples needed to match the current storage setup and stop advertising a removed dependency.

Main files:

- `docs/project-constitution.md`
- `README.md`
- `docs/feature-map.md`
- `docs/phase-one-module-configuration.md`
- `.env.example`
- `compose.postgres.yml`
