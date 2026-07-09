# Feature Map

This file explains where features live in the repository. It is a navigation map for humans and future AI agents.

Update this file whenever a new feature module, page, API, database table, or important directory is added.

## Repository Overview

| Area | Path | Purpose |
|---|---|---|
| Root agent rules | `AGENTS.md` | Tells Codex/agents to read the project constitution before development. |
| Project constitution | `docs/project-constitution.md` | Highest project-level rules and phase-one boundaries. |
| Change log | `docs/change-log.md` | Plain-language record of project changes. |
| Feature map | `docs/feature-map.md` | This file. Maps features to directories and code areas. |
| Backend application | `egg-admin` | Spring Boot web entry module and backend controllers. |
| Shared backend code | `egg-common` | Common Java utilities, constants, annotations, exceptions, and shared helpers. |
| Backend framework | `egg-framework` | Security, web config, datasource, Redis, and framework integration code. |
| Code generator | `egg-generator` | Existing Egg code generation module. |
| Scheduled tasks | `egg-quartz` | Existing scheduler and job management module. |
| System domain/services | `egg-system` | Existing system users, roles, menus, config, logs, and related services. |
| Admin frontend | `egg-ui` | Existing Egg admin console frontend. Keep this style for backend management. |
| User frontend | `egg-user-web` | Front-stage user workspace. New Picset-style user pages should live here. |
| SQL scripts | `sql` | Database schema, migration, cleanup, and seed scripts. |
| Docker/deploy docs | `docker`, `compose.postgres.yml` | Local and server dependency setup. |

## Existing Backend Modules

### Admin Web Controllers

Path: `egg-admin/src/main/java/com/egg/commerce/web/controller`

| Subpath | Purpose |
|---|---|
| `common` | Common upload/download/captcha style endpoints from the Egg base project. |
| `system` | System user, role, menu, config, dictionary, login, register, and profile controllers. |
| `monitor` | Server, cache, log, online user, and monitoring controllers. |
| `tool` | Code generator and tool-related controllers. |

### System Domain And Services

Path: `egg-system/src/main/java/com/egg/commerce/system`

| Subpath | Purpose |
|---|---|
| `domain` | System entities such as users, roles, config, logs, and online users. |
| `mapper` | MyBatis mapper interfaces for system tables. |
| `service` | System service interfaces and implementations. |

### Framework And Common Code

| Path | Purpose |
|---|---|
| `egg-framework` | Security, web MVC, Redis, config, and infrastructure integration. |
| `egg-common` | Common utilities and shared types used by all backend modules. |

## Existing Frontend Modules


### Current Front-Stage Generator Workspace

| Path | Purpose |
|---|---|
| `egg-user-web/src/components/GeneratorWorkspace.vue` | Shared Picset-style generator workspace for product image, style replication, and story generation setup. Owns local image preview state, six-image limit, drag-and-drop image input, generation parameters, and result placeholder. |
| `egg-user-web/src/pages/ProductImagesPage.vue` | Full-category product image generation entry. |
| `egg-user-web/src/pages/StyleClonePage.vue` | Style replication entry that reuses the shared generator workspace. |
| `egg-user-web/src/pages/StoryGenerationPage.vue` | Story generation entry added next to intelligent chat in the first-version top navigation. |
| `egg-user-web/src/utils/imageUpload.js` | Small upload-selection utility for filtering image files and enforcing the six-image limit. |

Navigation note:

- First-version top navigation currently exposes full-category product images, style replication, intelligent chat, story generation, and task center.
- Apparel sets, product library, asset library, and more tools routes remain in the router but are hidden from the top navigation for later versions.
### Egg Admin Console

Path: `egg-ui/src`

Keep this as the back-stage admin console. Do not redesign it as a Picset-style user experience.

| Subpath | Purpose |
|---|---|
| `api` | Admin frontend API wrappers. |
| `views` | Admin pages for system, monitor, tool, and dashboard screens. |
| `layout` | Admin layout, sidebar, navbar, tags, and shell components. |
| `router` | Admin route definitions. |
| `store` | Vuex/permission/user state. |
| `components` | Shared admin components. |
| `utils` | Admin frontend utilities and request helpers. |

### Front-Stage User Workspace

Path: `egg-user-web/src`

This is where the Picset-style user-facing product should be developed.

| Subpath | Purpose |
|---|---|
| `pages` | User-facing pages such as visual generation, chat, product library, asset history, and task center. |
| `components` | Reusable user-facing UI components. |
| `layout` | Front-stage layout shell, navigation, and page frame. |
| `router` | Front-stage route definitions. |
| `data` | Static/demo data or local page configuration. |
| `styles` | Front-stage visual system and page styles. |

## Planned AI E-Commerce Feature Map

The paths below are the intended locations for new phase-one feature code. If implementation chooses a different path, update this file immediately.

### Product Library

Purpose:

- Store product records, product source images, reference images, category, target platform, and user-provided product details.

Backend planned paths:

- Controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/product`
- Domain: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/product`
- Mapper: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/product`
- Service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/product`
- SQL: `sql`

Frontend planned paths:

- User pages: `egg-user-web/src/pages/product`
- User API wrappers: `egg-user-web/src/api/product` if an API layer is added

### File Storage And Assets

Purpose:

- Upload original product images.
- Store AI-generated images and edited images in MinIO.
- Save metadata such as width, height, MIME type, model, prompt, and object path.

Backend planned paths:

- Storage service: `egg-framework` or `egg-common` depending on shared usage
- Asset controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/asset`
- Asset domain/service/mapper: `egg-system/src/main/java/com/egg/commerce/system/.../ai/asset`

Frontend planned paths:

- Asset library pages: `egg-user-web/src/pages/assets`
- Preview/download components: `egg-user-web/src/components/assets`

### Intelligent Chat With RAG

Purpose:

- Let users ask e-commerce questions.
- Retrieve platform rules, product knowledge, audit rules, and operation knowledge from RAG.
- Save chat sessions and messages.

Backend planned paths:

- Chat controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat`
- Chat domain/service/mapper: `egg-system/src/main/java/com/egg/commerce/system/.../ai/chat`
- RAG service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/rag`
- Knowledge base domain/service/mapper: `egg-system/src/main/java/com/egg/commerce/system/.../ai/knowledge`
- Coze integration: `egg-system/src/main/java/com/egg/commerce/system/service/ai/coze`


Frontend planned paths:

- Chat page: `egg-user-web/src/pages/chat`
- Chat components: `egg-user-web/src/components/chat`
- Admin RAG management: `egg-ui/src/views/ai/knowledge` if added to Egg admin

### Model Gateway

Purpose:

- Provide a single backend entry for text, vision, image generation, embedding, and Coze/agent calls.
- Hide API keys from frontend code.
- Record model call logs and costs for backend analysis only.

Backend planned paths:

- Gateway service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/model`
- Provider configs: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/model`
- Admin controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/model`

Frontend planned paths:

- Admin model config pages: `egg-ui/src/views/ai/model`

### AI Task Center

Purpose:

- Track image generation, style replication, batch refinement, chat, audit, and export tasks.
- Show status, progress, failure reason, retry, and result entry.

Backend planned paths:

- Task controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/task`
- Task domain/service/mapper: `egg-system/src/main/java/com/egg/commerce/system/.../ai/task`
- Redis queue/progress integration: `egg-framework` or task service implementation

Frontend planned paths:

- User task center: `egg-user-web/src/pages/tasks`
- Task status components: `egg-user-web/src/components/tasks`

### Product Visual Analysis

Purpose:

- Analyze product images and product details.
- Extract selling points, target users, usage scenarios, visual constraints, and product identity protection rules.

Backend planned paths:

- Analysis service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/analysis`
- Analysis controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/analysis`

Frontend planned paths:

- Product detail analysis section: `egg-user-web/src/pages/product`
- Generation setup pages: `egg-user-web/src/pages/generate`

### Full-Category Product Image Generation

Purpose:

- Generate product main images, scene images, selling-point images, ad images, and detail page image sets.

Backend planned paths:

- Image generation controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/image`
- Image generation service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/image`
- Prompt templates: database-backed, configured from Egg admin

Frontend planned paths:

- Generation page: `egg-user-web/src/pages/generate`
- Image result components: `egg-user-web/src/components/generate`

### Prompt Template Management

Purpose:

- Maintain reusable Markdown prompt templates for later image generation, RAG, and agent workflows.
- Store prompt bodies as JSONB with `{ "format": "markdown", "body": "..." }`.
- Keep optional variable definitions for dynamic rendering with `{name}` style placeholders.

Backend paths:

- Controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/prompt`
- Domain: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/prompt`
- Mapper: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt`
- Service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/prompt`
- Mapper XML: `egg-system/src/main/resources/mapper/ai/prompt`
- Table: `ai_prompt_templates`

Frontend paths:

- Admin API wrapper: `egg-ui/src/api/ai/promptTemplate.js`
- Admin page: `egg-ui/src/views/ai/promptTemplate/index.vue`

SQL:

- `sql/ai_prompt_template_management.sql`

### Style Replication

Purpose:

- Upload reference images and product images.
- Extract visual style, composition, colors, lighting, and layout.
- Generate product images in the reference style while preserving product identity.

Backend planned paths:

- Style replication service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/style`
- Reference image records: product or asset modules

Frontend planned paths:

- Style replication page: `egg-user-web/src/pages/style-replicate`
- Reference image upload components: `egg-user-web/src/components/style-replicate`

### Batch Refinement

Purpose:

- Batch process product images for white background, background optimization, texture improvement, and basic polish.

Backend planned paths:

- Refinement service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/refinement`
- Refinement controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/refinement`

Frontend planned paths:

- Batch refinement page: `egg-user-web/src/pages/refinement`
- Batch upload/result components: `egg-user-web/src/components/refinement`

### Audit And Risk Checking

Purpose:

- Check text and images for exaggeration, product truthfulness, platform violations, and product appearance changes.

Backend planned paths:

- Audit controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/audit`
- Audit service/domain/mapper: `egg-system/src/main/java/com/egg/commerce/system/.../ai/audit`

Frontend planned paths:

- Audit result components inside generation and asset pages
- Admin audit rule pages: `egg-ui/src/views/ai/audit`

### Export And Download

Purpose:

- Download generated images.
- Export product assets as Markdown, Excel, or ZIP.

Backend planned paths:

- Export controller: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/export`
- Export service: `egg-system/src/main/java/com/egg/commerce/system/service/ai/export`

Frontend planned paths:

- Download actions in `egg-user-web/src/pages/assets`
- Export panels in product detail and asset pages

## Debugging Guide

When debugging a feature, start here:

1. Check the user-facing page in `egg-user-web/src/pages`.
2. Check related reusable components in `egg-user-web/src/components`.
3. Check the API wrapper if one exists.
4. Check the backend controller in `egg-admin/src/main/java/com/egg/commerce/web/controller`.
5. Check business logic in `egg-system/src/main/java/com/egg/commerce/system/service`.
6. Check domain objects and mapper SQL in `egg-system` and `egg-system/src/main/resources/mapper`.
7. Check database migration or seed scripts in `sql`.
8. Check task progress and Redis behavior for async AI tasks.
9. Check MinIO object paths and asset metadata for image/file issues.
10. Check model gateway logs, prompt version, task logs, and RAG retrieval logs for AI issues.

## Maintenance Rules

- Every new feature must add or update its entry in this file.
- Every meaningful code or documentation change must add a short note to `docs/change-log.md`.
- New code should include simple comments for non-obvious business logic, AI workflow logic, storage logic, and integration logic.
- If a feature violates `docs/project-constitution.md`, stop and ask for confirmation before implementing it.
