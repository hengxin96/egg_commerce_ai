package com.egg.commerce.system.service.ai.story;

import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskDetail;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskRequest;
import java.util.List;

/**
 * 前台剧情生成任务服务接口。
 *
 * <p>该接口面向用户端页面和任务中心，负责创建异步任务、查询任务进度、读取详情和触发后台串行生成。</p>
 */
public interface AiStoryTaskService
{
    /** 创建剧情生成任务并提交后台线程池异步执行。 */
    AiStoryTask createTask(Long userId, AiStoryTaskRequest request);

    /** 查询当前用户可见的剧情任务列表，供任务中心展示。 */
    List<AiStoryTask> selectTaskList(Long userId);

    /** 查询当前用户的剧情任务详情，包含所有已生成节点内容。 */
    AiStoryTaskDetail selectTaskDetail(String taskId, Long userId);

    /** 重试失败的剧情任务，在原任务上从失败节点或最近可恢复节点继续生成。 */
    AiStoryTask retryTask(String taskId, Long userId);

    /** 后台执行指定剧情任务，按固定节点串行调用模型并回写进度。 */
    void executeTask(String taskId);
}
