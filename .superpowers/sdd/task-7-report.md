# Task 7 Report: RAG Chat Persistence And Service

## Scope

Implemented backend persistence and service APIs for the front-stage RAG chat flow in the `ai-knowledge-sensitive-rag` worktree.

## TDD Record

### RED

Command:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test
```

Result:

- Failed during test compilation because the new `ai.chat` domains, mappers, and service classes did not exist yet.

### GREEN

Command:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test
```

Result:

- Passed with `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.

### RED-GREEN For Review Fixes

RED command:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test
```

RED result:

- Failed in test compile after the new assertions were added because `AiChatMessage` did not expose persisted `sources`, `AiChatRetrievalSource` did not expose `metadata`, and `AiChatServiceImpl` did not yet support metadata serialization or history source attachment.

GREEN command:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test
```

GREEN result:

- Passed with `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.

## Implemented Files

### Backend controller

- `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat/AiChatController.java`

### Chat domains

- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatSession.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatMessage.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatRetrievalSource.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatRequest.java`
- `egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat/AiChatResponse.java`

### Mappers and XML

- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatSessionMapper.java`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatMessageMapper.java`
- `egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat/AiChatRetrievalSourceMapper.java`
- `egg-system/src/main/resources/mapper/ai/chat/AiChatSessionMapper.xml`
- `egg-system/src/main/resources/mapper/ai/chat/AiChatMessageMapper.xml`
- `egg-system/src/main/resources/mapper/ai/chat/AiChatRetrievalSourceMapper.xml`

### Services

- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/AiChatModelService.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/IAiChatService.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiChatServiceImpl.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiMockChatModelServiceImpl.java`
- `egg-system/src/main/java/com/egg/commerce/system/service/ai/chat/impl/AiSimpleChatModelServiceImpl.java`

### Tests

- `egg-system/src/test/java/com/egg/commerce/system/service/ai/chat/AiChatServiceImplTest.java`

### Docs

- `docs/change-log.md`
- `docs/feature-map.md`

## Behavior Summary

- `sendMessage` now validates that the session exists and belongs to the authenticated user before any persistence happens.
- The service stores the user message first, performs vector retrieval through `AiVectorStoreService`, generates the assistant answer through `AiChatModelService`, stores the assistant message with `ragUsed=true`, persists retrieval sources linked to the assistant message id, updates session preview/count/timestamps, and returns `AiChatResponse`.
- Retrieval sources preserve document id, rank, chunk index, source title, snippet, and score from `AiVectorRecord`.
- A mock chat-model implementation is enabled when `egg.ai.mock-enabled=true`.

## Verification

Required commands to run before commit:

```powershell
mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test
mvn "-Dmaven.repo.local=.m2repo" -pl egg-admin -am -DskipTests compile
git diff --check
```

Final verification results:

- `mvn "-Dmaven.repo.local=.m2repo" -pl egg-system -Dtest=AiChatServiceImplTest test` passed with `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
- `mvn "-Dmaven.repo.local=.m2repo" -pl egg-admin -am -DskipTests compile` passed.
- `git diff --check d979a56..HEAD` passed.

### Transaction And Mock Wiring Fix

- Added `@Transactional(rollbackFor = Exception.class)` to `AiChatServiceImpl.sendMessage` so user message, assistant message, retrieval sources, and session updates commit or roll back as one chat answer unit.
- Removed `matchIfMissing = true` from `AiMockChatModelServiceImpl`; mock answers are now selected only when `egg.ai.mock-enabled=true`.
- Set `AiSimpleChatModelServiceImpl` to match when `egg.ai.mock-enabled` is missing, so chat startup still has a non-mock `AiChatModelService` bean by default.

## Notes

- The unit test focuses on the Task 7 persistence contract: ownership validation and full send-message write flow.
