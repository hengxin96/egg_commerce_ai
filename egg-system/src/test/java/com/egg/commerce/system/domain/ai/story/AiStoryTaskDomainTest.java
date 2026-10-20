package com.egg.commerce.system.domain.ai.story;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 剧情任务领域对象测试，锁定前端表单字段和后端任务字段的命名契约。
 */
class AiStoryTaskDomainTest
{
    /**
     * 验证创建任务请求能够完整承载用户输入的剧情参数。
     */
    @Test
    void storyTaskRequestKeepsUserInputParameters()
    {
        AiStoryTaskRequest request = new AiStoryTaskRequest();
        request.setStoryTheme("轮流赡养老人的兄弟俩因为1万块钱起争执不管老人了");
        request.setProductInfo("三全手工鲜肉馄饨，皮薄馅大，汤鲜味美");
        request.setTargetPlatform("抖音");
        request.setTargetAudience("中老年人群");
        request.setStoryStyle("家庭伦理");
        request.setStoryDuration("60秒");

        assertEquals("轮流赡养老人的兄弟俩因为1万块钱起争执不管老人了", request.getStoryTheme());
        assertEquals("三全手工鲜肉馄饨，皮薄馅大，汤鲜味美", request.getProductInfo());
        assertEquals("抖音", request.getTargetPlatform());
        assertEquals("中老年人群", request.getTargetAudience());
        assertEquals("家庭伦理", request.getStoryStyle());
        assertEquals("60秒", request.getStoryDuration());
    }

    /**
     * 验证任务实体能够同时保存当前节点、阶段产物和失败原因。
     */
    @Test
    void storyTaskKeepsProgressOutputsAndFailureReason()
    {
        AiStoryTask task = new AiStoryTask();
        task.setCurrentNode("分镜生成");
        task.setStatus("failed");
        task.setStoryOutput("剧情内容");
        task.setScriptOutput("剧本内容");
        task.setFailureReason("分镜生成失败");

        assertEquals("分镜生成", task.getCurrentNode());
        assertEquals("failed", task.getStatus());
        assertEquals("剧情内容", task.getStoryOutput());
        assertEquals("剧本内容", task.getScriptOutput());
        assertEquals("分镜生成失败", task.getFailureReason());
    }
}
