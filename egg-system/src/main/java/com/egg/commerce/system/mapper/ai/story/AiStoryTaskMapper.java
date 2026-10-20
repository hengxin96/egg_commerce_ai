package com.egg.commerce.system.mapper.ai.story;

import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import java.util.List;

/**
 * 剧情生成任务数据访问接口，对应 ai_story_tasks 表。
 */
public interface AiStoryTaskMapper
{
    /** 根据任务 ID 查询单条剧情任务，用于详情、下载和异步执行回读。 */
    AiStoryTask selectAiStoryTaskById(String id);

    /** 按用户、状态等条件查询剧情任务列表，供任务中心展示。 */
    List<AiStoryTask> selectAiStoryTaskList(AiStoryTask query);

    /** 新增剧情任务，保存用户输入和初始节点状态。 */
    int insertAiStoryTask(AiStoryTask task);

    /** 更新剧情任务进度、阶段输出、失败原因和模型快照。 */
    int updateAiStoryTask(AiStoryTask task);
}
