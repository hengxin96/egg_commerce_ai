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