package com.egg.commerce.system.service.ai.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.domain.ai.story.AiStoryPlatformRule;
import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskRequest;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.mapper.ai.story.AiStoryPlatformRuleMapper;
import com.egg.commerce.system.mapper.ai.story.AiStoryTaskMapper;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import com.egg.commerce.system.service.ai.story.impl.AiStoryTaskServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 剧情任务服务测试，重点覆盖固定节点的串行执行和失败状态回写。
 */
class AiStoryTaskServiceImplTest
{
    /**
     * 验证五个节点严格按顺序执行，并且每个节点能拿到上一节点输出继续渲染提示词。
     */
    @Test
    void createTaskRunsStagesSeriallyAndPassesPreviousOutputsForward()
    {
        FakeStoryTaskMapper taskMapper = new FakeStoryTaskMapper();
        FakePromptTemplateMapper promptMapper = new FakePromptTemplateMapper();
        FakePlatformRuleMapper platformRuleMapper = new FakePlatformRuleMapper();
        RecordingTextGenerationService modelService = new RecordingTextGenerationService();
        AiStoryTaskServiceImpl service = new AiStoryTaskServiceImpl(taskMapper, platformRuleMapper, promptMapper,
                modelService, new ImmediateThreadPoolTaskExecutor(), environment());

        AiStoryTask task = service.createTask(99L, request());

        assertEquals("completed", taskMapper.task.getStatus());
        assertEquals("完成", taskMapper.task.getCurrentNode());
        assertEquals("story-output", taskMapper.task.getStoryOutput());
        assertEquals("script-output", taskMapper.task.getScriptOutput());
        assertEquals("storyboard-output", taskMapper.task.getStoryboardOutput());
        assertEquals("checked-storyboard-output", taskMapper.task.getStoryboardCheckOutput());
        assertEquals("visual-output", taskMapper.task.getVisualOutput());
        assertEquals(5, modelService.prompts.size());
        assertTrue(modelService.prompts.get(1).contains("story-output"));
        assertTrue(modelService.prompts.get(2).contains("script-output"));
        assertTrue(modelService.prompts.get(3).contains("storyboard-output"));
        assertTrue(modelService.prompts.get(4).contains("script-output"));
        assertTrue(modelService.prompts.get(4).contains("checked-storyboard-output"));
        assertEquals(task.getId(), taskMapper.task.getId());
    }

    /**
     * 验证中间节点失败时不会清空已经完成的剧情和剧本内容。
     */
    @Test
    void createTaskStopsAtFailedNodeAndKeepsCompletedOutputs()
    {
        FakeStoryTaskMapper taskMapper = new FakeStoryTaskMapper();
        RecordingTextGenerationService modelService = new RecordingTextGenerationService();
        modelService.failureStage = "分镜生成";
        AiStoryTaskServiceImpl service = new AiStoryTaskServiceImpl(taskMapper, new FakePlatformRuleMapper(),
                new FakePromptTemplateMapper(), modelService, new ImmediateThreadPoolTaskExecutor(), environment());

        service.createTask(99L, request());

        assertEquals("failed", taskMapper.task.getStatus());
        assertEquals("分镜生成", taskMapper.task.getCurrentNode());
        assertEquals("story-output", taskMapper.task.getStoryOutput());
        assertEquals("script-output", taskMapper.task.getScriptOutput());
        assertNull(taskMapper.task.getStoryboardOutput());
        assertTrue(taskMapper.task.getFailureReason().contains("分镜生成失败"));
    }

    /**
     * 失败节点的上游内容完整时，重试只从失败节点继续，不重新生成已经成功的剧情和剧本。
     */
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
        assertEquals("checked-storyboard-output", taskMapper.task.getStoryboardCheckOutput());
        assertEquals("visual-output", taskMapper.task.getVisualOutput());
        assertEquals(3, modelService.prompts.size());
        assertTrue(modelService.prompts.get(0).contains("script-output"));
    }

    /**
     * 当前失败节点缺少上游内容时，重试会自动退回到能补齐缺失内容的节点。
     */
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
        assertEquals("checked-storyboard-output", taskMapper.task.getStoryboardCheckOutput());
        assertEquals("visual-output", taskMapper.task.getVisualOutput());
        assertEquals(4, modelService.prompts.size());
        assertTrue(modelService.prompts.get(0).contains("story-output"));
    }

    /**
     * 已完成和生成中的任务不能重试，前端即使误调接口也会得到明确业务提示。
     */
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

    /**
     * 构造用户创建任务请求，模拟前台剧情生成表单提交。
     */
    private AiStoryTaskRequest request()
    {
        AiStoryTaskRequest request = new AiStoryTaskRequest();
        request.setStoryTheme("主题");
        request.setProductInfo("产品");
        request.setTargetPlatform("抖音");
        request.setTargetAudience("中老年人群");
        request.setStoryStyle("家庭伦理");
        request.setStoryDuration("60秒");
        return request;
    }

    /**
     * 构造测试环境变量，覆盖视觉阶段默认模型占位符。
     */
    private MockEnvironment environment()
    {
        return new MockEnvironment()
                .withProperty("egg.ai.model-gateway.story.provider", "mock")
                .withProperty("egg.ai.model-gateway.story.model", "mock-story-model")
                .withProperty("egg.ai.story.aspect-ratio", "9:16")
                .withProperty("egg.ai.story.video-model-name", "视频模型")
                .withProperty("egg.ai.story.image-model-name", "图片模型");
    }

    /**
     * 构造指定状态的既有任务，用于验证重试状态机拒绝非法状态。
     */
    private AiStoryTask storedTaskWithStatus(String status)
    {
        AiStoryTask task = storedFailedTask("剧情生成");
        task.setStatus(status);
        return task;
    }

    /**
     * 构造数据库中已经失败的任务快照，重试测试会在此基础上补充不同上游输出。
     */
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

    private static class ImmediateThreadPoolTaskExecutor extends ThreadPoolTaskExecutor
    {
        /**
         * 立即执行异步任务，让测试可以同步断言完整节点结果。
         */
        @Override
        public Future<?> submit(Runnable task)
        {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class FakeStoryTaskMapper implements AiStoryTaskMapper
    {
        private AiStoryTask task;

        /**
         * 根据测试中保存的任务 ID 返回当前任务。
         */
        @Override
        public AiStoryTask selectAiStoryTaskById(String id)
        {
            return task;
        }

        /**
         * 返回当前测试任务列表。
         */
        @Override
        public List<AiStoryTask> selectAiStoryTaskList(AiStoryTask query)
        {
            return task == null ? Collections.emptyList() : List.of(task);
        }

        /**
         * 捕获新建任务。
         */
        @Override
        public int insertAiStoryTask(AiStoryTask task)
        {
            this.task = task;
            return 1;
        }

        /**
         * 捕获服务每个节点的状态回写。
         */
        @Override
        public int updateAiStoryTask(AiStoryTask task)
        {
            this.task = task;
            return 1;
        }
    }

    private static class FakePlatformRuleMapper implements AiStoryPlatformRuleMapper
    {
        /**
         * 返回发布中的平台画像和规则，供提示词变量替换。
         */
        @Override
        public AiStoryPlatformRule selectPublishedRuleByPlatform(String platform)
        {
            AiStoryPlatformRule rule = new AiStoryPlatformRule();
            rule.setId("9f09eea2-bdc3-4ad7-8a6a-fc07a0161a0a");
            rule.setPlatform(platform);
            rule.setVersion("2026-07-v1");
            rule.setPlatformProfile("平台画像");
            rule.setCategoryRules("平台规则");
            return rule;
        }
    }

    private static class FakePromptTemplateMapper implements AiPromptTemplateMapper
    {
        /**
         * 测试不需要按 ID 查询提示词。
         */
        @Override
        public AiPromptTemplate selectAiPromptTemplateById(String id)
        {
            return null;
        }

        /**
         * 测试不覆盖后台模板列表。
         */
        @Override
        public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
        {
            return Collections.emptyList();
        }

        /**
         * 测试不覆盖模板新增。
         */
        @Override
        public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 1;
        }

        /**
         * 测试不覆盖模板更新。
         */
        @Override
        public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 1;
        }

        /**
         * 测试不覆盖单条删除。
         */
        @Override
        public int deleteAiPromptTemplateById(String id)
        {
            return 1;
        }

        /**
         * 测试不覆盖批量删除。
         */
        @Override
        public int deleteAiPromptTemplateByIds(String[] ids)
        {
            return ids.length;
        }

        /**
         * 按剧情服务传入的阶段分类返回含占位符的发布模板。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateForStoryStage(String category, String platformPrefix,
                List<String> fallbackCategories)
        {
            if ("剧情生成".equals(category))
            {
                return template("story-template", 2,
                        "story {storyTheme} {productInfo} {targetPlatform} {platformProfile} {categoryRules}");
            }
            if ("剧本生成".equals(category) || fallbackCategories.contains("剧情改剧本"))
            {
                return template("script-template", 3, "script {storyText} {targetAudience}");
            }
            if ("分镜生成".equals(category) || fallbackCategories.contains("剧本改分镜"))
            {
                return template("storyboard-template", 4, "storyboard {scriptText} {videoModelName}");
            }
            if ("分镜校验".equals(category))
            {
                return template("check-template", 5, "check {storyboardText} {storyDuration}");
            }
            if ("三视图/场景图".equals(category))
            {
                return template("visual-template", 6, "visual {scriptText} {storyboardText} {imageModelName}");
            }
            return null;
        }

        /**
         * 剧情测试不覆盖通用运行时模板查询。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateByCategory(String category)
        {
            return null;
        }

        /**
         * 剧情测试不覆盖商品图运行时模板查询。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateByNameAndCategory(String name, String category)
        {
            return null;
        }

        /**
         * 构造测试用提示词模板，content 保持和数据库 JSONB 字段一致的 JSON 字符串形态。
         */
        private AiPromptTemplate template(String id, Integer version, String body)
        {
            AiPromptTemplate template = new AiPromptTemplate();
            template.setId(id);
            template.setVersion(version);
            template.setContent("{\"format\":\"markdown\",\"body\":\"" + body + "\"}");
            return template;
        }
    }

    private static class RecordingTextGenerationService implements AiTextGenerationService
    {
        private final List<String> prompts = new ArrayList<>();

        private String failureStage;

        /**
         * 按阶段返回固定输出，并记录提示词以验证上一步内容确实进入下一步。
         */
        @Override
        public AiTextGenerationResult generate(AiTextGenerationRequest request)
        {
            prompts.add(request.getPrompt());
            if (request.getStage().equals(failureStage))
            {
                throw new ServiceException("分镜生成失败");
            }

            AiTextGenerationResult result = new AiTextGenerationResult();
            result.setProvider("mock");
            result.setModel("mock-story-model");
            if ("剧情生成".equals(request.getStage()))
            {
                result.setContent("story-output");
            }
            else if ("剧本生成".equals(request.getStage()))
            {
                result.setContent("script-output");
            }
            else if ("分镜生成".equals(request.getStage()))
            {
                result.setContent("storyboard-output");
            }
            else if ("分镜校验".equals(request.getStage()))
            {
                result.setContent("checked-storyboard-output");
            }
            else
            {
                result.setContent("visual-output");
            }
            return result;
        }
    }
}
