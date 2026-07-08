# Prompt Template Management Design

Date: 2026-07-08

Status: Approved for implementation planning

## Goal

Add an Egg admin console feature for maintaining AI prompt templates. The templates will be used later by image generation, RAG, and agent workflows. The feature is visible only to authenticated backend admin users in `egg-ui`; ordinary front-stage users do not see or manage these templates.

This feature supports the project constitution because prompt templates are an admin configuration surface for AI generation and chat/RAG workflows. It does not add billing, tenants, teams, front-stage user permissions, video generation, or a new frontend product surface.

## Scope

The first version includes:

- Backend CRUD APIs for `ai_prompt_templates`.
- Egg admin frontend list and edit UI.
- Markdown prompt body editing with a large monospace textarea.
- Optional manual `variables` JSON editing.
- Menu and permission SQL for the Egg admin sidebar.
- SQL to align `created_by` with the current backend user ID type.
- Documentation updates for the change log and feature map during implementation.

The first version excludes:

- Front-stage user access.
- Prompt rendering preview or test execution.
- Automatic variable extraction from Markdown.
- Markdown editor dependencies, syntax highlighting, line numbers, or split preview.
- Prompt history table or rollback UI.
- Model invocation from this management page.

## Data Model

The existing table remains the source of truth:

```sql
ai_prompt_templates
```

The implementation will add a migration script that changes `created_by` from `uuid` to `bigint`, because the current Egg backend user ID is numeric. New records store the current backend login user ID in `created_by`.

The implementation will also normalize `variables` to default to an empty array:

```json
[]
```

`content` remains `jsonb` and stores Markdown prompt text using this shape:

```json
{
  "format": "markdown",
  "body": "# 角色\n你是一位专业助手。\n用户姓名：{name}"
}
```

`variables` is optional and manually maintained when needed:

```json
[
  {
    "key": "name",
    "label": "用户姓名",
    "type": "text",
    "required": false,
    "defaultValue": ""
  }
]
```

The runtime rendering convention is single-brace placeholders, such as `{name}` and `{productName}`. Rendering itself is outside this feature.

## Version And Status

`version` starts at `1` when a template is created. Any update through the edit API increments `version` by `1`. This keeps prompt changes traceable even before a dedicated version-history table exists.

`status` uses only these values:

- `draft`: draft
- `published`: published
- `archived`: archived

There is no separate enable or disable switch in the first version.

## Backend Design

Use the existing Egg backend style:

- Controller path: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/prompt`
- Domain path: `egg-system/src/main/java/com/egg/commerce/system/domain/ai/prompt`
- Mapper path: `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/prompt`
- Service path: `egg-system/src/main/java/com/egg/commerce/system/service/ai/prompt`
- Mapper XML path: `egg-system/src/main/resources/mapper/ai/prompt`

REST endpoints:

- `GET /ai/promptTemplate/list`: paged list
- `GET /ai/promptTemplate/{id}`: detail
- `POST /ai/promptTemplate`: create
- `PUT /ai/promptTemplate`: update and increment version
- `DELETE /ai/promptTemplate/{ids}`: batch delete

List filters:

- `name`
- `category`
- `status`

Validation:

- `name` is required and max 255 characters.
- `content.body` is required.
- `content` must be valid JSON and saved as JSONB.
- `variables` may be blank, but if present it must be valid JSON and saved as JSONB.
- `status` must be one of `draft`, `published`, or `archived`.

JSONB handling will be simple and explicit. Java domain fields can use strings for `content` and `variables`; MyBatis writes them with PostgreSQL `::jsonb` casts. This avoids adding a global JSONB type handler before the codebase has a broader JSONB pattern.

The controller uses existing permission and operation-log conventions:

- `ai:promptTemplate:list`
- `ai:promptTemplate:query`
- `ai:promptTemplate:add`
- `ai:promptTemplate:edit`
- `ai:promptTemplate:remove`

## Frontend Design

Use the existing Egg admin UI in `egg-ui`, not the Picset-style front-stage UI.

Frontend paths:

- API wrapper: `egg-ui/src/api/ai/promptTemplate.js`
- Page: `egg-ui/src/views/ai/promptTemplate/index.vue`

The list page includes:

- Search fields: name, category, status.
- Table columns: name, category, status, version, createdBy, createdAt, updatedAt.
- Actions: add, edit, delete, detail.

The edit UI includes:

- Template name.
- Description.
- Category as free text.
- Status select with `draft`, `published`, `archived`.
- Markdown prompt body textarea using monospace styling and a large height.
- Optional variables JSON textarea.

The admin user edits only the Markdown body, not the full `content` JSON object. The frontend converts the Markdown body to `{ "format": "markdown", "body": "..." }` before submitting, and converts it back to body text when loading details.

No external Markdown editor package is added in the first version. The UI should remain visually consistent with the Egg admin console.

## SQL Design

Add a SQL script under `sql` for:

- Changing `ai_prompt_templates.created_by` to `bigint`.
- Changing `variables` default to `[]::jsonb`.
- Adding an AI configuration parent menu if it does not already exist.
- Adding the prompt template page menu.
- Adding list, query, add, edit, and remove permission buttons.

The SQL should use stable IDs that do not collide with existing system menu IDs in the current repository scripts.

## Error Handling

Backend APIs return normal Egg `AjaxResult` errors for invalid JSON, missing required content, invalid status, or missing records.

The frontend should show validation messages before submitting where practical, especially for required name/body fields and invalid variables JSON.

## Testing And Verification

Implementation should verify:

- Backend module compiles with Maven.
- Frontend module builds or at least passes the available local frontend check.
- Create, list, detail, update, and delete APIs are wired consistently.
- The frontend can submit Markdown content and optional variables JSON.
- Version increments on update.
- Documentation and SQL files are included.

## Acceptance Criteria

The feature is complete when:

- An authenticated backend user can manage prompt templates in `egg-ui`.
- Created templates store Markdown in `content.body`.
- Optional variables are stored as JSONB without being required.
- Updates increment `version`.
- `created_by` stores the numeric backend user ID after the migration.
- The prompt template page is reachable from the Egg admin menu after running the SQL script.
- No front-stage user page or billing/tenant/team feature is added.
