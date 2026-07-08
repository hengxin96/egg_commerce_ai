# Task 6 Report: Sensitive Word Backend

## Scope

- Implement backend CRUD APIs for AI sensitive categories and words.
- Follow the Egg/RuoYi-style Spring Boot monolith patterns already used by the AI knowledge backend.
- Keep the write set scoped to Task 6 backend files plus the required project maintenance docs.

## TDD Record

### RED

Command:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiSensitiveServiceTest test
```

Observed failure:

- `AiSensitiveServiceTest` failed during `testCompile` because the new `ai.sensitive` domain, mapper, and service classes did not exist yet.
- This was the expected red state proving the test covered missing production code instead of already-existing behavior.

### GREEN Plan

- Add minimal sensitive category and word domain classes.
- Add mapper interfaces and MyBatis XML for CRUD queries.
- Add service interfaces and implementations with the required defaults and validation.
- Add category and word controllers with the permissions specified in the task brief.

## Implemented Files

### Backend Controllers

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive/AiSensitiveCategoryController.java`
- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive/AiSensitiveWordController.java`

### Domain Models

- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive/AiSensitiveCategory.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive/AiSensitiveWord.java`

### Mapper Interfaces

- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive/AiSensitiveCategoryMapper.java`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive/AiSensitiveWordMapper.java`

### Services

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/IAiSensitiveCategoryService.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/IAiSensitiveWordService.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/impl/AiSensitiveCategoryServiceImpl.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive/impl/AiSensitiveWordServiceImpl.java`

### MyBatis XML

- `egg-system/src/main/resources/mapper/ai/sensitive/AiSensitiveCategoryMapper.xml`
- `egg-system/src/main/resources/mapper/ai/sensitive/AiSensitiveWordMapper.xml`

### Tests

- `egg-system/src/test/java/com/egg/commerce/system/service/ai/sensitive/AiSensitiveServiceTest.java`

### Maintenance Docs

- `docs/change-log.md`
- `docs/feature-map.md`

## Behavior Notes

- Category inserts default `status` to `enabled` when callers omit it.
- Word inserts default `status` to `enabled` when callers omit it.
- Word create and update both reject blank `word` values with a `ServiceException`.
- Category list/detail uses `ai:sensitive:category:query` because the SQL seed does not define `ai:sensitive:category:list`.
- Word list uses `ai:sensitive:word:list` and word detail uses `ai:sensitive:word:query`.

## Verification Checklist

- [x] Added failing test first.
- [x] `mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiSensitiveServiceTest test`
- [x] `mvn "-Dmaven.repo.local=.m2repo" -pl egg-admin -am -DskipTests compile`
- [x] `git diff --check`
- [x] Commit created with `feat: add ai sensitive word backend`

## Verification Results

### `mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiSensitiveServiceTest test`

- Result: `BUILD SUCCESS`
- Test count: `3`
- Failures: `0`
- Errors: `0`

### `mvn "-Dmaven.repo.local=.m2repo" -pl egg-admin -am -DskipTests compile`

- Result: `BUILD SUCCESS`
- Reactor modules compiled successfully through `egg-admin`

### `git diff --check`

- Result: exit code `0`
- Notes: Git reported only LF-to-CRLF conversion warnings for `docs/change-log.md` and `docs/feature-map.md`, with no whitespace errors.

## Commit

- `30eaaa5 feat: add ai sensitive word backend`

## Task 6 Review Fix

- Updated `AiSensitiveCategoryController.list` to use the standard Egg paginated list pattern with `TableDataInfo`, `startPage()`, and `getDataTable(list)`.
- Added a test assertion that sensitive word updates reject blank input, matching the existing create-path validation.
