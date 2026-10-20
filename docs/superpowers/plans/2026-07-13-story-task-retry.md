# 剧情任务重试功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在任务中心为失败的剧情生成任务增加“重试”能力，让后端在原任务上从失败节点或最近可恢复节点继续生成。

**Architecture:** 新增 `POST /ai/story/tasks/{id}/retry` 接口，服务层校验任务状态和归属，计算真实重试起点，清理重试起点及其下游节点数据，再复用异步生成链路从指定节点执行到完成。前端仅在失败任务操作列展示“重试”，生成中和已完成任务不展示。

**Tech Stack:** Spring Boot + MyBatis + PostgreSQL 宽表 `ai_story_tasks`；Vue 3 + Element Plus；Java JUnit 5；Node `node:test` 静态前端测试。

## Global Constraints

- 只在当前分支 `dev-01` 修改，不创建新分支。
- 不新建剧情任务记录，重试复用原 `ai_story_tasks` 行。
- 只清理重试起点及其后续节点，保留更早已成功节点。
- `processing` 任务前端不显示重试，后端也拒绝重试。
- `completed` 任务后端返回“当前任务已完成，无需重试”。
- 本次不新增重试历史表，不改视频/图片生成，不改积分、支付、租户、团队、权限模型。
- 修改业务逻辑、AI 工作流、接口和存储相关代码时保留简短维护注释。
- 实现完成后更新 `docs/change-log.md` 和 `docs/feature-map.md`。

---

### Task 1: 后端重试行为测试

**Files:**
- Modify: `egg-system/src/test/java/com/egg/commerce/system/service/ai/story/AiStoryTaskServiceImplTest.java`

**Interfaces:**
- Consumes: existing `AiStoryTaskServiceImpl`, `FakeStoryTaskMapper`, `RecordingTextGenerationService`
- Produces: failing tests for `retryTask(String taskId, Long userId)`

- [ ] **Step 1: 写失败测试**

Add these focused tests to `AiStoryTaskServiceImplTest`:

```java
@Test
void retryFailedStoryboardTaskStartsAtStoryboardAndKeepsStoryAndScript()
{
    FakeStoryTaskMapper taskMapper = new FakeStoryTaskMapper();
    RecordingTextGenerationService modelService = new RecordingTextGenerationService();
    modelService.failureStage = "分镜生成";
    AiStoryTaskServiceImpl service = new AiStoryTaskServiceImpl(taskMapper, new FakePlatformRuleMapper(),
            new FakePromptTemplateMapper(), modelService, new ImmediateThreadPoolTaskExecutor(), environment());

    service.createTask(99L, request());
    assertEquals("failed", taskMapper.task.getStatus());
    assertEquals("story-output", taskMapper.task.getStoryOutput());
    assertEquals("script-output", taskMapper.task.getScriptOutput());

    modelService.failureStage = null;
    modelService.prompts.clear();
    service.retryTask(taskMapper.task.getId(), 99L);

    assertEquals("completed", taskMapper.task.getStatus());
    assertEquals("完成", taskMapper.task.getCurrentNode());
    assertEquals("story-output", taskMapper.task.getStoryOutput());
    assertEquals("script-output", taskMapper.task.getScriptOutput());
    assertEquals("storyboard-output", taskMapper.task.getStoryboardOutput());
    assertEquals(3, modelService.prompts.size());
    assertTrue(modelService.prompts.get(0).contains("script-output"));
}

@Test
void retryWalksBackWhenPreviousOutputIsMissing()
{
    FakeStoryTaskMapper taskMapper = new FakeStoryTaskMapper();
    RecordingTextGenerationService modelService = new RecordingTextGenerationService();
    AiStoryTask task = storedFailedTask("分镜生成");
    task.setStoryOutput("story-output");
    task.setScriptOutput(null);
    taskMapper.task = task;
    AiStoryTaskServiceImpl service = new AiStoryTaskServiceImpl(taskMapper, new FakePlatformRuleMapper(),
            new FakePromptTemplateMapper(), modelService, new ImmediateThreadPoolTaskExecutor(), environment());

    service.retryTask(task.getId(), 99L);

    assertEquals("completed", taskMapper.task.getStatus());
    assertEquals("story-output", taskMapper.task.getStoryOutput());
    assertEquals("script-output", taskMapper.task.getScriptOutput());
    assertEquals("storyboard-output", taskMapper.task.getStoryboardOutput());
    assertEquals(4, modelService.prompts.size());
    assertTrue(modelService.prompts.get(0).contains("story-output"));
}

@Test
void retryRejectsCompletedAndProcessingTasks()
{
    FakeStoryTaskMapper taskMapper = new FakeStoryTaskMapper();
    AiStoryTaskServiceImpl service = new AiStoryTaskServiceImpl(taskMapper, new FakePlatformRuleMapper(),
            new FakePromptTemplateMapper(), new RecordingTextGenerationService(),
            new ImmediateThreadPoolTaskExecutor(), environment());

    taskMapper.task = storedTaskWithStatus("completed");
    ServiceException completed = assertThrows(ServiceException.class,
            () -> service.retryTask(taskMapper.task.getId(), 99L));
    assertTrue(completed.getMessage().contains("当前任务已完成，无需重试"));

    taskMapper.task = storedTaskWithStatus("processing");
    ServiceException processing = assertThrows(ServiceException.class,
            () -> service.retryTask(taskMapper.task.getId(), 99L));
    assertTrue(processing.getMessage().contains("任务正在生成中，请稍后"));
}
```

Add helper methods:

```java
private AiStoryTask storedTaskWithStatus(String status)
{
    AiStoryTask task = storedFailedTask("剧情生成");
    task.setStatus(status);
    return task;
}

private AiStoryTask storedFailedTask(String currentNode)
{
    AiStoryTask task = new AiStoryTask();
    task.setId("7a64d19c-8c1a-4bd2-8e21-5d1a135ca8e7");
    task.setUserId(99L);
    task.setTaskType("story");
    task.setStoryTheme("主题");
    task.setProductInfo("产品");
    task.setTargetPlatform("抖音");
    task.setTargetAudience("中老年人群");
    task.setStoryStyle("家庭伦理");
    task.setStoryDuration("60秒");
    task.setCurrentNode(currentNode);
    task.setStatus("failed");
    task.setFailureReason("失败原因");
    task.setDebugPrompts("{}");
    task.setModelParams("{}");
    return task;
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
mvn -pl egg-system -Dtest=AiStoryTaskServiceImplTest test
```

Expected: compilation fails because `retryTask` is not defined.

### Task 2: 后端重试服务与接口

**Files:**
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/story/AiStoryTaskService.java`
- Modify: `egg-system/src/main/java/com/egg/commerce/system/service/ai/story/impl/AiStoryTaskServiceImpl.java`
- Modify: `egg-admin/src/main/java/com/egg/commerce/web/controller/ai/story/AiStoryTaskController.java`

**Interfaces:**
- Consumes: Task 1 tests
- Produces:
  - `AiStoryTask retryTask(String taskId, Long userId)`
  - `POST /ai/story/tasks/{id}/retry`
  - internal `executeTaskFrom(String taskId, String startNode)`

- [ ] **Step 1: 给 service 接口增加重试方法**

Add to `AiStoryTaskService`:

```java
/** 重试失败的剧情任务，在原任务上从失败节点或最近可恢复节点继续生成。 */
AiStoryTask retryTask(String taskId, Long userId);
```

- [ ] **Step 2: 实现状态校验和异步提交**

In `AiStoryTaskServiceImpl`, implement:

```java
@Override
public AiStoryTask retryTask(String taskId, Long userId)
{
    AiStoryTask task = requireOwnedTask(taskId, userId);
    if (STATUS_COMPLETED.equals(task.getStatus()))
    {
        throw new ServiceException("当前任务已完成，无需重试");
    }
    if (STATUS_PROCESSING.equals(task.getStatus()))
    {
        throw new ServiceException("任务正在生成中，请稍后");
    }
    if (!STATUS_FAILED.equals(task.getStatus()))
    {
        throw new ServiceException("当前任务状态不支持重试");
    }

    String retryNode = resolveRetryStartNode(task);
    clearFromNode(task, retryNode);
    task.setCurrentNode(retryNode);
    task.setStatus(STATUS_PROCESSING);
    task.setFailureReason(null);
    task.setCompletedAt(null);
    saveProgress(task);

    // 重试复用原任务记录，只从可恢复节点继续跑，避免覆盖前序已确认内容。
    threadPoolTaskExecutor.submit(() -> executeTaskFrom(task.getId(), retryNode));
    return task;
}
```

- [ ] **Step 3: 拆分执行入口**

Change `executeTask` to:

```java
@Override
public void executeTask(String taskId)
{
    executeTaskFrom(taskId, NODE_STORY);
}
```

Add runner:

```java
private void executeTaskFrom(String taskId, String startNode)
{
    AiStoryTask task = storyTaskMapper.selectAiStoryTaskById(taskId);
    if (task == null)
    {
        log.warn("剧情生成任务不存在，taskId={}", taskId);
        return;
    }

    try
    {
        Date now = new Date();
        task.setStatus(STATUS_PROCESSING);
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        storyTaskMapper.updateAiStoryTask(task);

        AiStoryPlatformRule platformRule = requirePlatformRule(task);
        rememberPlatformRule(task, platformRule);
        Map<String, String> baseVariables = buildBaseVariables(task, platformRule);

        if (shouldRunStage(startNode, NODE_STORY))
        {
            runStoryStage(task, baseVariables);
        }
        if (shouldRunStage(startNode, NODE_SCRIPT))
        {
            runScriptStage(task, baseVariables);
        }
        if (shouldRunStage(startNode, NODE_STORYBOARD))
        {
            runStoryboardStage(task, baseVariables);
        }
        if (shouldRunStage(startNode, NODE_STORYBOARD_CHECK))
        {
            runStoryboardCheckStage(task, baseVariables);
        }
        if (shouldRunStage(startNode, NODE_VISUAL))
        {
            runVisualStage(task, baseVariables);
        }

        completeTask(task);
    }
    catch (Exception e)
    {
        failTask(task, e);
    }
}
```

- [ ] **Step 4: 增加节点顺序、回退和清理工具方法**

Add helpers:

```java
private boolean shouldRunStage(String startNode, String stageNode)
{
    return stageOrder(startNode) <= stageOrder(stageNode);
}

private int stageOrder(String node)
{
    if (NODE_STORY.equals(node)) return 1;
    if (NODE_SCRIPT.equals(node)) return 2;
    if (NODE_STORYBOARD.equals(node)) return 3;
    if (NODE_STORYBOARD_CHECK.equals(node)) return 4;
    if (NODE_VISUAL.equals(node)) return 5;
    return 1;
}

private String resolveRetryStartNode(AiStoryTask task)
{
    String node = normalizeRetryNode(task.getCurrentNode());
    while (true)
    {
        String missingProducer = missingDependencyProducer(task, node);
        if (missingProducer == null)
        {
            return node;
        }
        node = missingProducer;
    }
}

private String normalizeRetryNode(String node)
{
    if (NODE_SCRIPT.equals(node) || NODE_STORYBOARD.equals(node)
            || NODE_STORYBOARD_CHECK.equals(node) || NODE_VISUAL.equals(node))
    {
        return node;
    }
    return NODE_STORY;
}

private String missingDependencyProducer(AiStoryTask task, String node)
{
    if (NODE_SCRIPT.equals(node) && StringUtils.isBlank(task.getStoryOutput()))
    {
        return NODE_STORY;
    }
    if (NODE_STORYBOARD.equals(node) && StringUtils.isBlank(task.getScriptOutput()))
    {
        return StringUtils.isBlank(task.getStoryOutput()) ? NODE_STORY : NODE_SCRIPT;
    }
    if (NODE_STORYBOARD_CHECK.equals(node) && StringUtils.isBlank(task.getStoryboardOutput()))
    {
        return StringUtils.isBlank(task.getScriptOutput()) ? NODE_SCRIPT : NODE_STORYBOARD;
    }
    if (NODE_VISUAL.equals(node))
    {
        if (StringUtils.isBlank(task.getStoryboardCheckOutput()))
        {
            return StringUtils.isBlank(task.getStoryboardOutput()) ? NODE_STORYBOARD : NODE_STORYBOARD_CHECK;
        }
        if (StringUtils.isBlank(task.getScriptOutput()))
        {
            return StringUtils.isBlank(task.getStoryOutput()) ? NODE_STORY : NODE_SCRIPT;
        }
    }
    return null;
}
```

Add `clearFromNode` with one branch per stage, clearing output/template/version and removing prompt key from `debugPrompts`.

- [ ] **Step 5: 增加 controller endpoint**

Add to `AiStoryTaskController`:

```java
/** 重试当前用户的失败剧情任务，后端会从失败节点或最近可恢复节点继续执行。 */
@PostMapping("/{id}/retry")
public AjaxResult retry(@PathVariable String id)
{
    return success(storyTaskService.retryTask(id, getUserId()));
}
```

- [ ] **Step 6: 运行后端测试确认通过**

Run:

```powershell
mvn -pl egg-system -Dtest=AiStoryTaskServiceImplTest test
```

Expected: `BUILD SUCCESS`.

### Task 3: 前端 API 与任务中心重试测试

**Files:**
- Modify: `egg-user-web/src/api/story.test.js`
- Modify: `egg-user-web/src/pages/TasksPage.test.js`

**Interfaces:**
- Consumes: backend endpoint path from Task 2
- Produces: failing tests for `retryStoryTask` and task-center retry UI

- [ ] **Step 1: 写 API 失败测试**

Add to `story.test.js`:

```js
import { retryStoryTask } from './story.js'

test('retryStoryTask posts to the story retry endpoint', async () => {
  const previousFetch = globalThis.fetch
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/ai/story/tasks/task-1/retry')
    assert.equal(options.method, 'POST')
    return new Response(JSON.stringify({ code: 200, data: { id: 'task-1', status: 'processing' } }), {
      status: 200,
      headers: { 'content-type': 'application/json' }
    })
  }

  try {
    const task = await retryStoryTask('task-1')
    assert.equal(task.status, 'processing')
  } finally {
    globalThis.fetch = previousFetch
  }
})
```

- [ ] **Step 2: 写任务中心静态测试**

Extend `TasksPage.test.js`:

```js
test('task center shows retry only for failed story tasks', () => {
  const page = readSource('./TasksPage.vue')

  assert.match(page, /retryStoryTask/)
  assert.match(page, /row\.status === 'failed'/)
  assert.match(page, /重试/)
  assert.match(page, /retryTask\(row\)/)
  assert.match(page, /retryingTaskIds/)
})
```

- [ ] **Step 3: 运行前端测试确认失败**

Run:

```powershell
node --test src\api\story.test.js src\pages\TasksPage.test.js
```

Expected: fails because `retryStoryTask` and retry UI are not implemented.

### Task 4: 前端 API 与任务中心实现

**Files:**
- Modify: `egg-user-web/src/api/story.js`
- Modify: `egg-user-web/src/pages/TasksPage.vue`

**Interfaces:**
- Consumes: Task 3 tests
- Produces:
  - `retryStoryTask(taskId)`
  - task-center retry action for failed rows only

- [ ] **Step 1: 实现 API 封装**

Add to `story.js`:

```js
export function retryStoryTask(taskId) {
  return request(`/ai/story/tasks/${encodeURIComponent(taskId)}/retry`, {
    method: 'POST'
  })
}
```

- [ ] **Step 2: 修改任务中心 imports and state**

Change import:

```js
import { downloadStoryTask, getStoryTask, listStoryTasks, retryStoryTask } from '@/api/story'
```

Add state:

```js
const retryingTaskIds = ref({})
```

- [ ] **Step 3: 增加重试 handler**

Add:

```js
const retryTask = async (task) => {
  retryingTaskIds.value = { ...retryingTaskIds.value, [task.id]: true }
  try {
    await retryStoryTask(task.id)
    ElMessage.success('已开始重试')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '重试失败')
  } finally {
    const next = { ...retryingTaskIds.value }
    delete next[task.id]
    retryingTaskIds.value = next
  }
}
```

- [ ] **Step 4: 增加操作列按钮**

Add before view/download buttons:

```vue
<el-button
  v-if="row.status === 'failed'"
  link
  type="primary"
  :loading="Boolean(retryingTaskIds[row.id])"
  @click="retryTask(row)"
>
  重试
</el-button>
```

- [ ] **Step 5: 运行前端测试确认通过**

Run:

```powershell
node --test src\api\story.test.js src\pages\TasksPage.test.js
```

Expected: all tests pass.

### Task 5: 文档与全量验证

**Files:**
- Modify: `docs/change-log.md`
- Modify: `docs/feature-map.md`

**Interfaces:**
- Consumes: completed backend/frontend implementation
- Produces: project documentation and verification evidence

- [ ] **Step 1: 更新文档**

Add a `2026-07-13` change-log entry describing:

- failed story tasks can be retried from task center
- backend retry API restores from failed or nearest recoverable node
- retry preserves upstream outputs and clears retry/downstream node outputs
- processing/completed retry requests are rejected
- tests were added

Update feature map Story Generation / Task Center notes to include:

- `POST /ai/story/tasks/{id}/retry`
- task center failed-row retry action
- retry keeps one task row and reuses fixed node output columns

- [ ] **Step 2: 运行后端相关测试**

Run:

```powershell
mvn -pl egg-system -Dtest=AiStoryTaskServiceImplTest,AiStoryTaskExportServiceImplTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: 运行前端相关测试**

Run:

```powershell
node --test src\api\auth.test.js src\api\story.test.js src\components\GeneratorWorkspace.story.test.js src\pages\LoginPage.auth.test.js src\pages\TasksPage.test.js src\utils\imageUpload.test.js
```

Expected: all tests pass.

- [ ] **Step 4: 运行前端构建**

Run:

```powershell
npm run build
```

Expected: build succeeds. Existing Vite warnings about VueUse annotations and chunk size may remain.

- [ ] **Step 5: 最终检查**

Run:

```powershell
git -C D:\Code_Workspace\egg_commerce_ai diff --check -- egg-system/src/main/java/com/egg/commerce/system/service/ai/story/AiStoryTaskService.java egg-system/src/main/java/com/egg/commerce/system/service/ai/story/impl/AiStoryTaskServiceImpl.java egg-admin/src/main/java/com/egg/commerce/web/controller/ai/story/AiStoryTaskController.java egg-system/src/test/java/com/egg/commerce/system/service/ai/story/AiStoryTaskServiceImplTest.java egg-user-web/src/api/story.js egg-user-web/src/api/story.test.js egg-user-web/src/pages/TasksPage.vue egg-user-web/src/pages/TasksPage.test.js docs/change-log.md docs/feature-map.md
```

Expected: no whitespace errors; CRLF warnings are acceptable on Windows.
