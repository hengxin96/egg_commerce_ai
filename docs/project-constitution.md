# AI 电商视觉内容生成工作台 Constitution

## Core Principles

### I. Picset-Style Visual Generation First

This project is an AI e-commerce visual content generation platform, not a generic chatbot, prompt-only tool, or FastReal-style channel distribution system.

The first product priority is the front-stage user workflow for product image generation:

- Product image upload and visual understanding
- Product main image generation
- Detail page image set generation
- Scene image and selling-point image generation
- Style replication from reference images
- Basic batch refinement
- Asset preview, saving, versioning, download, and export

Text generation, SEO copy, and product descriptions are supporting capabilities. They must serve the visual generation workflow and must not displace image generation as the product core.

### II. Real Image Output Is Required

The first version must generate real images. A feature that only produces image ideas, prompts, or planning text does not satisfy the MVP.

Every image generation workflow must:

- Preserve product identity, including color, structure, logo, quantity, and key physical details
- Store generated images in object storage instead of relying on temporary model URLs
- Record model name, prompt version, generation parameters, task ID, user ID, product ID, and audit status
- Support preview, regeneration, download, and asset-library reuse
- Provide clear failure reasons and retry behavior

Any model or prompt change that may alter product truthfulness must be treated as a product-risk change and reviewed against this constitution.

### III. Agent Chat And RAG Are First-Phase Requirements

The first version must include an e-commerce intelligent chat experience. Users can ask questions about e-commerce operations, product content, platform rules, visual strategy, title and selling-point writing, detail page structure, and compliance risks.

The chat agent must be able to use RAG knowledge sources. It should:

- Retrieve platform rules, brand rules, audit rules, product documents, and e-commerce operation knowledge
- Answer with practical guidance instead of generic conversation
- Use current product context when available
- Save chat sessions and messages
- Record retrieval sources when RAG is used
- Suggest next actions such as generating product images, refining images, replicating style, or improving copy

Coze or an equivalent agent workflow may be used for chat and content-oriented orchestration, but Java backend services remain responsible for data persistence, permissions, task state, file storage, and model gateway control.

### IV. Front Stage And Back Stage Must Stay Separate

The system has two distinct product surfaces:

- Front-stage user workspace: redesigned around a Picset-like visual generation experience
- Back-stage admin console: keeps the existing Egg admin style and is used for configuration and management

The front stage should prioritize clarity, speed, and repeated creative operations. It should provide entry points such as full-category product image generation, style replication, batch refinement, intelligent chat, product library, asset history, and task center.

The back stage should not be redesigned as a marketing-style or Picset-style interface. It remains a stable management surface for model providers, model parameters, prompt templates, RAG knowledge bases, platform rules, audit rules, system parameters, task logs, and model call logs.

### V. Phase-One Simplicity And User Equality

The first version must keep the business model simple.

Phase one explicitly does not include:

- Points, billing, package plans, payment, or recharge flows
- Tenant isolation
- Team spaces
- Role differences between normal front-stage users
- Real video generation
- Advanced universal canvas

A2A is now a phase-one terminal feature: it may be developed only after the visual-generation MVP, intelligent chat/RAG loop, asset workflow, and export flow are usable. A2A must not introduce points, payments, tenant/team architecture, or a microservice split in phase one.

Front-stage users are equal after login and can access the same generation capabilities. Backend admin permissions can continue using the existing Egg permission system.

### VI. Traceability, Reviewability, And Reuse

All AI outputs must be traceable and reusable. Generated content is a product asset, not a transient model response.

The system must preserve:

- Product source data and uploaded source images
- Reference images used for style replication
- Generated images and edited images
- Prompts, prompt versions, model names, model parameters, and task logs
- RAG retrieval sources for chat and generation when applicable
- Audit reports, risk reasons, and suggested fixes
- Asset versions and final-selection state

High-risk outputs must not be silently exported as final assets. The user must be able to review, edit, regenerate, approve, reject, download, and export assets.

### VII. Clear Code Comments

All code must include simple, useful comments that explain what the code does and why it exists when the intent is not immediately obvious.

AI agents and Codex sessions must add maintenance comments by default when generating or changing code. This applies in current threads and future new threads because agent entry rules point back to this constitution.

Comments should:

- Explain business purpose, workflow intent, important assumptions, and non-obvious decisions
- Mark boundaries between controller, service, model gateway, RAG, storage, task, and frontend responsibilities when helpful
- Clarify AI prompts, model parameters, task states, file storage paths, and audit decisions
- Cover generated or modified controller, service, service implementation, domain/entity, mapper, API wrapper, task, storage, RAG, model gateway, and frontend workflow code when those files contain business intent or integration behavior
- Stay short and readable for future maintainers and future AI agents

Comments should not restate obvious syntax. For example, avoid comments like "set name" above `setName(...)`. Prefer comments that help someone debug or safely modify the feature later.

## Product Boundaries

### Phase-One Must-Have Scope

The first usable version must include:

- Login and authenticated front-stage access
- Product library and product image upload
- Object storage file persistence and PostgreSQL metadata persistence
- Redis-backed async task progress for AI generation tasks
- Model gateway for text, vision, image generation, embedding, and Coze or equivalent agent calls
- Intelligent chat with RAG retrieval
- Product visual analysis
- Full-category product image generation
- Detail page image set generation
- Style replication
- Basic batch refinement
- Supporting copy generation
- Audit and risk checking
- Asset library, versioning, preview, download, and export
- Egg-style admin configuration for models, prompts, RAG, platform rules, and audit rules
- A2A integration as the final phase-one delivery item after the core visual-generation and RAG workflows are stable

### Phase-One Non-Goals

The first version must not spend implementation effort on:

- Points, billing, payment, recharge, or package permissions
- Tenant or team architecture
- Microservice split
- Production-grade video generation
- Advanced universal canvas
- Full BI reporting
- Ad platform data integration

These can be reconsidered only after the visual-generation MVP and intelligent chat/RAG loop are usable.

### Technical Direction

The preferred first-phase architecture is:

- Modular Spring Boot monolith
- Existing Egg backend/admin foundation
- Front-stage user workspace built separately from the admin experience
- PostgreSQL for business data
- Redis for login/session support, task queue/progress, rate limiting, duplicate-submit prevention, and short-lived chat context
- Object storage for original images, generated images, edited images, and export files
- Model gateway for all model and agent calls
- RAG using pgvector or another approved vector store

Microservices are not a first-phase goal. Code should be modular enough to split later, but deployment should remain simple until the product loop is proven.

## Development Workflow

### Constitution Check

Before implementing a feature or accepting a requirement change, check it against this constitution:

1. Does it strengthen Picset-style visual generation or required chat/RAG capability?
2. Does it preserve the separation between Picset-like front stage and Egg-style back stage?
3. Does it avoid phase-one non-goals such as billing, tenants, teams, production video, and advanced canvas work?
4. Does it produce traceable, savable, reviewable, and reusable assets?
5. Does it protect product truthfulness and prevent unreviewed product appearance changes?
6. Does new or changed code include simple comments where they help future maintenance?

If the answer conflicts with any core principle, the requirement must be revised or the constitution must be formally amended before implementation.

### Delivery Order

The preferred first-version delivery order is:

1. Foundation: login, product library, upload, object storage, Redis task center, backend configuration
2. Intelligent chat and RAG: chat sessions, Coze or agent integration, knowledge retrieval, source logging
3. Visual generation loop: product analysis, product image generation, detail image generation, style replication, batch refinement
4. Asset workflow: asset library, versioning, audit, download, export, failure handling
5. Front-stage polish: Picset-style user pages, ergonomic generation forms, previews, history, and task status
6. A2A integration: lightweight first-phase A2A handoff after the core product loop is usable

### Quality Gates

No first-phase feature is complete unless:

- It has a clear user entry point
- It works with authenticated users
- It records the task or chat history
- It stores generated assets or answers where appropriate
- It handles failure with a visible reason
- It respects resource ownership and file access permissions
- It does not expose API keys, object storage credentials, model credentials, or private files to the frontend
- It includes simple comments for non-obvious business logic, AI workflow logic, storage logic, and integration logic

For image generation features, the output must also be previewable, downloadable, and traceable to product, model, prompt, and parameters.

## Governance

This constitution supersedes ordinary feature ideas, informal plans, and implementation shortcuts. Requirement documents, UI designs, backend modules, agent workflows, and prompts must remain consistent with it.

Amendments require:

- A written reason for the change
- The affected principles or sections
- A version bump
- A migration or refactoring note if existing work is affected
- Explicit user approval

Versioning follows semantic intent:

- MAJOR: changes project direction or removes/rewrites a core principle
- MINOR: adds a principle, phase boundary, or major workflow constraint
- PATCH: wording clarifications that do not change project behavior

All future planning, code review, UI design, and AI-agent work must verify constitution compliance before treating a task as ready.

**Version**: 1.2.2 | **Ratified**: 2026-06-27 | **Last Amended**: 2026-07-18
