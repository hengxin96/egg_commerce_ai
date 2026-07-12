# Story Task Retry Design

Date: 2026-07-13

## Background

The front-stage task center already shows story-generation tasks, including failed tasks and their current failed node. Users now need a retry action for failed tasks so a task can continue from the failed workflow node without losing already saved upstream content.

The existing story workflow stores fixed node outputs on `ai_story_tasks`:

- story output
- script output
- storyboard output
- storyboard check output
- visual prompt output

The retry feature must respect that storage model. It should not create a new task, should not clear earlier successful node output, and should not introduce a separate branch or broader task-center redesign.

## Confirmed Requirements

- Add a retry action in the task center operation column only for failed story tasks.
- A processing task must not show a clickable retry action in the frontend.
- The backend must also reject retry for processing tasks with a clear "task is generating, please wait" business message.
- A completed task retry request must be rejected with a clear "current task is completed, no retry needed" business message.
- A failed task retry should reuse the same task row and continue the existing workflow.
- If the failed node already has partial output, clear only that node's output and node metadata before regenerating.
- Do not clear earlier successful node content.
- Also clear the failed node's prompt/template snapshot and the task failure reason before retrying.
- If the failed node cannot run because its upstream output is missing, walk backward to the nearest runnable upstream point. For example, if storyboard generation failed but script output is missing, retry should regenerate script first and then continue to storyboard and later nodes.
- The retry should run asynchronously like initial task creation.
- The frontend should refresh task state after starting retry.

## Recommended Approach

Use a dedicated backend retry API:

`POST /ai/story/tasks/{id}/retry`

The controller delegates to `AiStoryTaskService.retryTask(taskId, userId)`. The service validates ownership and status, prepares the failed task for retry, then submits a background execution job that starts from the correct node.

This is preferred over reusing task creation because retry has different status rules and must preserve upstream outputs in the same task row.

## Backend Design

### Service Contract

Add to `AiStoryTaskService`:

```java
AiStoryTask retryTask(String taskId, Long userId);
```

Behavior:

- Validate `taskId` and `userId`.
- Load the task and verify it belongs to the current user.
- If `status=completed`, throw a business exception: current task is completed, no retry needed.
- If `status=processing`, throw a business exception: task is generating, please wait.
- If `status` is not `failed`, reject with a clear unsupported status message.
- Resolve the actual retry start node from `currentNode` and available upstream outputs.
- Clear only the start node output, prompt template id, prompt version, and debug prompt entry.
- Clear `failureReason` and `completedAt`.
- Set `status=processing`, keep `currentNode` at the retry start node, update `updatedAt`, and save.
- Submit an async job that continues from the start node.
- Return the updated task snapshot.

### Retry Start Resolution

The workflow dependencies are:

- story stage depends on user input
- script stage depends on `storyOutput`
- storyboard stage depends on `scriptOutput`
- storyboard check stage depends on `storyboardOutput`
- visual stage depends on `scriptOutput` and `storyboardCheckOutput`

Resolution rule:

1. Start from the task's `currentNode`.
2. If the current node is missing its required upstream output, move to the upstream generating node that can produce that missing output.
3. Repeat until the stage can run from available data.

Examples:

- Current node: storyboard generation, `scriptOutput` exists. Retry starts at storyboard generation.
- Current node: storyboard generation, `scriptOutput` missing, `storyOutput` exists. Retry starts at script generation.
- Current node: storyboard generation, both `scriptOutput` and `storyOutput` missing. Retry starts at story generation.
- Current node: visual prompt generation, `storyboardCheckOutput` missing but `storyboardOutput` exists. Retry starts at storyboard check.

### Execution Flow

Refactor the existing `executeTask(taskId)` implementation so the core runner can execute from a given node:

```java
executeTaskFrom(taskId, startNode)
```

Initial task creation calls it with the first generation stage. Retry calls it with the resolved retry start node.

The runner should:

- reload the task from the database
- set processing state
- load platform rules and base variables
- run only stages from the start node through completion
- preserve outputs from stages before the start node
- mark failed again if a later stage fails

### Node Cleanup

For the resolved start node only:

- clear its output column
- clear its prompt template id column
- clear its prompt version column
- remove the matching key from `debugPrompts`

Do not clear later-stage outputs unless the retry start node is earlier than them and they would become stale. For example, if retry starts at script generation, clear script, storyboard, storyboard check, and visual outputs, because those downstream outputs depend on the regenerated script. If retry starts at storyboard generation, clear storyboard, storyboard check, and visual outputs, but keep story and script.

This preserves only data that remains valid after the retry point.

## Frontend Design

### API

Add to `egg-user-web/src/api/story.js`:

```js
export function retryStoryTask(taskId) {
  return request(`/ai/story/tasks/${encodeURIComponent(taskId)}/retry`, {
    method: 'POST'
  })
}
```

### Task Center

In `TasksPage.vue`:

- Show the retry action only when `row.status === 'failed'`.
- Hide retry for `processing` and `completed`.
- Use a per-task loading map so only the clicked task shows retry loading.
- After successful retry start, show a success message and reload the task list.
- On error, show the backend business message.

## Testing

Backend tests:

- Failed storyboard task with script output retries from storyboard and preserves story/script outputs.
- Failed storyboard task missing script output but with story output retries from script generation.
- Retry clears the retry node metadata and failure reason before generation.
- Completed task retry is rejected.
- Processing task retry is rejected.
- Retry continues through completion when the model succeeds.

Frontend tests:

- Failed tasks show the retry action.
- Processing and completed tasks do not show retry.
- Retry API calls `POST /ai/story/tasks/{id}/retry`.
- Task center imports and calls `retryStoryTask`.

## Scope Boundaries

- No new branch.
- No new task row for retry.
- No retry history table in this iteration.
- No image/video generation changes.
- No billing, tenant, team, or permission model changes.
- Update docs/change-log.md and docs/feature-map.md when implementation is completed.
