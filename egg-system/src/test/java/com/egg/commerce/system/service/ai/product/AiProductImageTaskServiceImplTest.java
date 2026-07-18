package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductImageTask;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskRequest;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductImageTaskMapper;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.service.ai.model.AiImageGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiImageGenerationResult;
import com.egg.commerce.system.service.ai.model.AiImageGenerationService;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingRequest;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingResult;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingService;
import com.egg.commerce.system.service.ai.product.AiProductImageCompositionResult;
import com.egg.commerce.system.service.ai.product.AiProductImageCompositionService;
import com.egg.commerce.system.service.ai.product.AiProductImageAuditService;
import com.egg.commerce.system.service.ai.product.impl.AiProductImageAuditServiceImpl;
import com.egg.commerce.system.service.ai.product.impl.AiProductImageTaskServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

class AiProductImageTaskServiceImplTest
{
    @Test
    void createTaskRunsVisionAnalysisAndStopsForUserConfirmation()
    {
        Fixture fixture = new Fixture();

        fixture.service.createTask(1L, fixture.request());

        assertEquals("waiting_confirmation", fixture.tasks.task.getStatus());
        assertEquals("确认规划", fixture.tasks.task.getCurrentNode());
        assertTrue(fixture.tasks.task.getAnalysisOutput().contains("primary red shoe"));
        assertTrue(fixture.tasks.task.getPlanningOutput().contains("overallDesignSpec"));
        assertEquals(1, fixture.vision.calls);
        assertEquals(1, fixture.text.calls);
        assertEquals(0, fixture.image.calls);
        assertEquals(0, fixture.storage.uploadGeneratedCalls);
        assertEquals("processing", fixture.assets.asset.getStatus());
        assertFalse(fixture.assets.asset.getVisible());
    }

    @Test
    void confirmTaskContinuesGenerationAfterUserApprovedPlanning()
    {
        Fixture fixture = new Fixture();
        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());

        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals("完成", fixture.tasks.task.getCurrentNode());
        assertEquals(1, fixture.image.calls);
        assertEquals(2, fixture.storage.uploadGeneratedCalls);
        assertTrue(fixture.assets.asset.getVisible());
    }

    @Test
    void createTaskRunsAnalysisPlanningGenerationAndPublishesAsset()
    {
        Fixture fixture = new Fixture();

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals("完成", fixture.tasks.task.getCurrentNode());
        assertTrue(fixture.tasks.task.getAnalysisOutput().contains("primary red shoe"));
        assertTrue(fixture.tasks.task.getPlanningOutput().contains("positivePrompt"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("generated.png"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("\"auditSummary\""));
        assertEquals("completed", fixture.assets.asset.getStatus());
        assertTrue(fixture.assets.asset.getVisible());
        assertEquals(task.getId(), fixture.assets.asset.getLastTaskId());
        assertEquals(2, fixture.vision.calls);
        assertEquals(1, fixture.text.calls);
        assertEquals(1, fixture.image.calls);
        assertEquals(2, fixture.storage.uploadGeneratedCalls);
        assertEquals(3, fixture.images.images.size());
        assertEquals("generated_base", fixture.images.images.get(1).getImageRole());
        assertEquals("generated", fixture.images.images.get(2).getImageRole());
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("baseImageId"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("finalImageId"));
    }

    @Test
    void completedTaskRenamesDefaultProductFromVisionAnalysis()
    {
        Fixture fixture = new Fixture();
        fixture.assets.asset.setName("未命名商品-abc12345");

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("primary red shoe", fixture.assets.asset.getName());
    }

    @Test
    void completedTaskKeepsUserEditedProductName()
    {
        Fixture fixture = new Fixture();
        fixture.assets.asset.setName("custom product name");

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("custom product name", fixture.assets.asset.getName());
    }

    @Test
    void generationOutputIncludesRuleAndVisionAudit()
    {
        Fixture fixture = new Fixture();

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        JSONObject output = JSONObject.parseObject(fixture.tasks.task.getGenerationOutput());
        JSONObject image = output.getJSONArray("images").getJSONObject(0);
        assertEquals("product_image_generation_v3", output.getString("schemaVersion"));
        assertEquals("pass", image.getJSONObject("ruleAudit").getString("status"));
        assertEquals(true, image.getJSONObject("visionAudit").getBooleanValue("completed"));
        assertEquals("pass", image.getJSONObject("visionAudit").getString("status"));
        assertEquals("pass", output.getJSONObject("auditSummary").getString("status"));
        assertEquals("product_image_audit", fixture.vision.lastRequest.getStage());
    }

    @Test
    void visionAuditFailureDoesNotBlockTaskCompletion()
    {
        Fixture fixture = new Fixture();
        fixture.vision.failAudit = true;

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        JSONObject output = JSONObject.parseObject(fixture.tasks.task.getGenerationOutput());
        JSONObject visionAudit = output.getJSONArray("images").getJSONObject(0).getJSONObject("visionAudit");
        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals(false, visionAudit.getBooleanValue("completed"));
        assertEquals("warning", visionAudit.getString("status"));
        assertEquals("warning", output.getJSONObject("auditSummary").getString("status"));
    }

    @Test
    void generationFailureKeepsAnalysisAndPlanningForRetry()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("failed", fixture.tasks.task.getStatus());
        assertEquals("生成中", fixture.tasks.task.getCurrentNode());
        assertNotNull(fixture.tasks.task.getAnalysisOutput());
        assertNotNull(fixture.tasks.task.getPlanningOutput());
        assertEquals("failed", fixture.assets.asset.getStatus());
        assertFalse(fixture.assets.asset.getVisible());
        assertEquals(1, fixture.text.calls);
        assertEquals(0, fixture.storage.uploadGeneratedCalls);
    }

    @Test
    void retryGenerationFailureStartsAtGenerationNode()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;
        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);
        String analysis = fixture.tasks.task.getAnalysisOutput();
        String planning = fixture.tasks.task.getPlanningOutput();
        int textCallsBeforeRetry = fixture.text.calls;

        fixture.image.fail = false;
        fixture.service.retryTask(fixture.tasks.task.getId(), 1L);

        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals("完成", fixture.tasks.task.getCurrentNode());
        assertEquals(analysis, fixture.tasks.task.getAnalysisOutput());
        assertEquals(planning, fixture.tasks.task.getPlanningOutput());
        assertEquals(textCallsBeforeRetry, fixture.text.calls);
        assertEquals(4, fixture.image.calls);
        assertTrue(fixture.assets.asset.getVisible());
    }

    @Test
    void taskJsonOutputsUseEmptyObjectsWhenCreatedAndRetried()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertFalse(fixture.tasks.jsonWrites.stream().anyMatch(value -> value == null));
        assertEquals("{}", fixture.tasks.task.getGenerationOutput());

        fixture.service.retryTask(fixture.tasks.task.getId(), 1L);

        assertFalse(fixture.tasks.jsonWrites.stream().anyMatch(value -> value == null));
    }

    @Test
    void planningUsesNestedImagePromptAndRendersRequiredVariables()
    {
        Fixture fixture = new Fixture();

        fixture.service.createTask(1L, fixture.request());

        assertTrue(fixture.tasks.task.getPlanningOutput().contains("imagePrompt"));
        String planningPrompt = fixture.text.prompts.get(0);
        assertTrue(planningPrompt.contains("douyin"));
        assertTrue(planningPrompt.contains("main"));
        assertTrue(planningPrompt.contains("3:4"));
        assertTrue(planningPrompt.contains("2K"));
        assertTrue(planningPrompt.contains("mock-product-image-model"));
        assertTrue(planningPrompt.contains("zh-CN"));
        assertTrue(planningPrompt.contains("textEnabled=false"));
        assertTrue(planningPrompt.contains("no in-image text"));
    }

    @Test
    void createTaskWritesLanguageTextAndSelectedModelIntoModelParamsAndPrompts()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setTargetLanguage("en");
        request.setTextEnabled(true);
        request.setSelectedModel("wan2.7-image-pro");

        fixture.service.createTask(1L, request);

        JSONObject params = JSONObject.parseObject(fixture.tasks.task.getModelParams());
        assertEquals("en", params.getString("targetLanguage"));
        assertEquals(true, params.getBooleanValue("textEnabled"));
        assertEquals("wan2.7-image-pro", params.getString("selectedModel"));
        String planningPrompt = fixture.text.prompts.get(0);
        assertTrue(planningPrompt.contains("en"));
        assertTrue(planningPrompt.contains("textEnabled=true"));
        assertTrue(planningPrompt.contains("wan2.7-image-pro"));
    }

    @Test
    void noTextModeSkipsCompositionAndMarksFinalOutputAsNoText()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setTextEnabled(false);

        AiProductImageTask task = fixture.service.createTask(1L, request);
        fixture.service.confirmTask(task.getId(), 1L, null);

        JSONObject output = JSONObject.parseObject(fixture.tasks.task.getGenerationOutput());
        JSONObject image = output.getJSONArray("images").getJSONObject(0);
        assertEquals(false, output.getBooleanValue("textEnabled"));
        assertEquals(false, image.getJSONObject("composition").getBooleanValue("applied"));
        assertEquals(0, fixture.composition.calls);
    }

    @Test
    void visualAnalysisAndAuditUsePublishedPromptTemplates()
    {
        Fixture fixture = new Fixture();

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertTrue(fixture.prompts.requests.contains("商品图视觉分析提示词|AI视觉分析"));
        assertTrue(fixture.prompts.requests.contains("商品图视觉审核提示词|AI视觉审核"));
        assertTrue(fixture.vision.requests.stream().anyMatch(request -> request.getPrompt().contains("configured analysis")));
        assertTrue(fixture.vision.requests.stream().anyMatch(request -> request.getPrompt().contains("configured audit")));
    }

    @Test
    void createTaskWritesRequestedImageCountIntoModelParamsAndPrompts()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(4);
        fixture.text.mainPromptCounts.add(4);

        AiProductImageTask task = fixture.service.createTask(1L, request);

        assertTrue(fixture.tasks.task.getModelParams().contains("\"imageCount\":4"));
        String planningPrompt = fixture.text.prompts.get(0);
        assertTrue(planningPrompt.contains(" 4 "));
        fixture.service.confirmTask(task.getId(), 1L, null);
        assertEquals(4, fixture.image.calls);
        assertTrue(fixture.image.requests.stream().allMatch(item -> Integer.valueOf(1).equals(item.getImageCount())));
    }

    @Test
    void mainPlanningRetriesUntilRequestedDistinctPromptCount()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(2);
        fixture.text.mainPromptCounts.add(1);
        fixture.text.mainPromptCounts.add(2);

        AiProductImageTask task = fixture.service.createTask(1L, request);

        assertEquals("waiting_confirmation", fixture.tasks.task.getStatus());
        assertEquals(2, fixture.text.calls);
        JSONObject planning = JSONObject.parseObject(fixture.tasks.task.getPlanningOutput());
        assertEquals(2, planning.getJSONArray("imagePrompts").size());

        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals(2, fixture.image.calls);
        assertTrue(fixture.image.requests.stream().allMatch(item -> Integer.valueOf(1).equals(item.getImageCount())));
        assertEquals(List.of("main product photo scheme 1", "main product photo scheme 2"),
                fixture.image.requests.stream().map(AiImageGenerationRequest::getPositivePrompt).toList());
    }

    @Test
    void mainPlanningFailsAfterTwoRetriesWhenPromptCountIsStillInsufficient()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(2);
        fixture.text.mainPromptCounts.add(1);
        fixture.text.mainPromptCounts.add(1);
        fixture.text.mainPromptCounts.add(1);

        fixture.service.createTask(1L, request);

        assertEquals("failed", fixture.tasks.task.getStatus());
        assertEquals("确认规划", fixture.tasks.task.getCurrentNode());
        assertEquals(3, fixture.text.calls);
        assertEquals(0, fixture.image.calls);
        assertTrue(fixture.tasks.task.getFailureReason().contains("规划方案数量不足，请重新规划"));
    }

    @Test
    void planningOutputCleansRepeatedChinesePunctuation()
    {
        Fixture fixture = new Fixture();
        fixture.text.repeatedPunctuationPlanning = true;
        fixture.text.mainPromptCounts.add(1);

        fixture.service.createTask(1L, fixture.request());

        assertFalse(fixture.tasks.task.getPlanningOutput().contains("、、、、、"));
    }

    @Test
    void createTaskRejectsMainImageCountOutsideOneToFour()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(5);

        assertThrows(ServiceException.class, () -> fixture.service.createTask(1L, request));
    }

    @Test
    void createTaskRejectsDetailImageCountOutsideOneToEight()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setAssetType("detail");
        request.setImageCount(9);

        assertThrows(ServiceException.class, () -> fixture.service.createTask(1L, request));
    }

    @Test
    void planningAcceptsFirstImagePromptFromArrayOutput()
    {
        Fixture fixture = new Fixture();
        fixture.text.arrayPlanning = true;

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());

        assertEquals("waiting_confirmation", fixture.tasks.task.getStatus());
        fixture.service.confirmTask(task.getId(), 1L, null);
        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals(1, fixture.image.calls);
        assertEquals("array red shoe product photo", fixture.image.lastRequest.getPositivePrompt());
        assertTrue(fixture.tasks.task.getPlanningOutput().contains("\"imagePrompt\""));
        assertTrue(fixture.tasks.task.getPlanningOutput().contains("\"imagePrompts\""));
    }

    @Test
    void generationUsesPresignedSourceImageUrlsForModelReferences()
    {
        Fixture fixture = new Fixture();

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals(List.of("https://signed.example/source.png"), fixture.image.lastRequest.getReferenceImageUrls());
        assertEquals(4, fixture.storage.presignCalls);
    }

    @Test
    void mainGenerationStoresAllReturnedImagesAndOutputArray()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(3);
        fixture.text.mainPromptCounts.add(3);
        fixture.image.responseBatches.add(List.of("https://example.invalid/generated-1.png"));
        fixture.image.responseBatches.add(List.of("https://example.invalid/generated-2.png"));
        fixture.image.responseBatches.add(List.of("https://example.invalid/generated-3.png"));

        AiProductImageTask task = fixture.service.createTask(1L, request);
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals(3, fixture.image.calls);
        assertTrue(fixture.image.requests.stream().allMatch(item -> Integer.valueOf(1).equals(item.getImageCount())));
        assertEquals(6, fixture.storage.uploadGeneratedCalls);
        assertEquals(7, fixture.images.images.size());
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("\"requestedCount\":3"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("\"completedCount\":3"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("\"images\""));
    }

    @Test
    void mainGenerationRetriesSinglePromptWhenModelReturnsNoImage()
    {
        Fixture fixture = new Fixture();
        AiProductImageTaskRequest request = fixture.request();
        request.setImageCount(1);
        fixture.image.responseBatches.add(List.of());
        fixture.image.responseBatches.add(List.of("https://example.invalid/generated-1.png"));

        AiProductImageTask task = fixture.service.createTask(1L, request);
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals(2, fixture.image.calls);
        assertEquals(1, fixture.image.requests.get(1).getImageCount());
        assertEquals(2, fixture.storage.uploadGeneratedCalls);
    }

    @Test
    void detailGenerationUsesEachPlannedPromptWithSingleImageRequests()
    {
        Fixture fixture = new Fixture();
        fixture.text.detailPromptCount = 3;
        AiProductImageTaskRequest request = fixture.request();
        request.setAssetType("detail");
        request.setImageCount(3);

        AiProductImageTask task = fixture.service.createTask(1L, request);
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals(3, fixture.image.calls);
        assertTrue(fixture.image.requests.stream().allMatch(item -> Integer.valueOf(1).equals(item.getImageCount())));
        assertEquals(List.of("detail product photo 1", "detail product photo 2", "detail product photo 3"),
                fixture.image.requests.stream().map(AiImageGenerationRequest::getPositivePrompt).toList());
        assertEquals(6, fixture.storage.uploadGeneratedCalls);
    }

    @Test
    void detailPlanningUsesImagePromptsWhenCompatibilityImagePromptIsEmpty()
    {
        Fixture fixture = new Fixture();
        fixture.text.detailPromptCount = 2;
        fixture.text.detailEmptyImagePrompt = true;
        AiProductImageTaskRequest request = fixture.request();
        request.setAssetType("detail");
        request.setImageCount(2);

        AiProductImageTask task = fixture.service.createTask(1L, request);

        assertEquals("waiting_confirmation", fixture.tasks.task.getStatus());
        JSONObject planning = JSONObject.parseObject(fixture.tasks.task.getPlanningOutput());
        assertEquals("detail product photo 1", planning.getJSONObject("imagePrompt").getString("positivePrompt"));

        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals(2, fixture.image.calls);
    }

    @Test
    void invalidPlanningOutputFailsBeforeImageGeneration()
    {
        Fixture fixture = new Fixture();
        fixture.text.invalidPlanning = true;

        fixture.service.createTask(1L, fixture.request());

        assertEquals("failed", fixture.tasks.task.getStatus());
        assertEquals("确认规划", fixture.tasks.task.getCurrentNode());
        assertEquals(0, fixture.image.calls);
    }

    @Test
    void createTaskDefersSchedulingUntilAfterCommit()
    {
        RecordingThreadPoolTaskExecutor executor = new RecordingThreadPoolTaskExecutor();
        Fixture fixture = new Fixture(executor);
        TransactionSynchronizationManager.initSynchronization();
        try
        {
            fixture.service.createTask(1L, fixture.request());

            assertEquals(0, executor.submissions);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, executor.submissions);
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void retryTaskDefersSchedulingUntilAfterCommit()
    {
        RecordingThreadPoolTaskExecutor executor = new RecordingThreadPoolTaskExecutor();
        Fixture fixture = new Fixture(executor);
        fixture.image.fail = true;
        AiProductImageTask task = null;

        TransactionSynchronizationManager.initSynchronization();
        try
        {
            task = fixture.service.createTask(1L, fixture.request());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            executor.runLastTask();
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.initSynchronization();
        try
        {
            fixture.service.confirmTask(task.getId(), 1L, null);
            assertEquals(1, executor.submissions);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(2, executor.submissions);
            executor.runLastTask();
            assertEquals("failed", fixture.tasks.task.getStatus());
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }

        fixture.image.fail = false;
        TransactionSynchronizationManager.initSynchronization();
        try
        {
            fixture.service.retryTask(fixture.tasks.task.getId(), 1L);

            assertEquals(2, executor.submissions);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(3, executor.submissions);
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void cancelProcessingTaskPreventsRetryAndVisibility()
    {
        Fixture fixture = new Fixture();
        fixture.image.beforeGenerate = () -> fixture.service.cancelTask(fixture.tasks.task.getId(), 1L);

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());
        fixture.service.confirmTask(task.getId(), 1L, null);

        assertEquals("cancelled", fixture.tasks.task.getStatus());
        assertEquals("用户已终止任务", fixture.tasks.task.getCancelReason());
        assertNotNull(fixture.tasks.task.getCancelledAt());
        assertEquals("cancelled", fixture.assets.asset.getStatus());
        assertFalse(fixture.assets.asset.getVisible());
        assertThrows(ServiceException.class, () -> fixture.service.retryTask(fixture.tasks.task.getId(), 1L));
        assertEquals(0, fixture.storage.uploadGeneratedCalls);
    }

    @Test
    void downloadGeneratedImageRetriesTransientHttpFailures() throws Exception
    {
        Fixture fixture = new Fixture();
        RealDownloadTaskService service = new RealDownloadTaskService(fixture);
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/generated.png", exchange ->
        {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1)
            {
                exchange.sendResponseHeaders(503, -1);
            }
            else
            {
                byte[] body = new byte[] {4, 5, 6};
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try
        {
            byte[] bytes = service.downloadForTest("http://127.0.0.1:" + server.getAddress().getPort() + "/generated.png");

            assertArrayEquals(new byte[] {4, 5, 6}, bytes);
            assertEquals(2, attempts.get());
        }
        finally
        {
            server.stop(0);
        }
    }

    private static class Fixture
    {
        private final FakeTaskMapper tasks = new FakeTaskMapper();
        private final FakeAssetMapper assets = new FakeAssetMapper();
        private final FakeImageMapper images = new FakeImageMapper();
        private final FakeStorage storage = new FakeStorage();
        private final FakePromptMapper prompts = new FakePromptMapper();
        private final FakeTextGenerationService text = new FakeTextGenerationService();
        private final FakeVisionUnderstandingService vision = new FakeVisionUnderstandingService();
        private final FakeCompositionService composition = new FakeCompositionService();
        private final AiProductImageAuditService audit = new AiProductImageAuditServiceImpl(vision, prompts);
        private final FakeImageGenerationService image = new FakeImageGenerationService();
        private final AiProductImageTaskServiceImpl service;

        private Fixture()
        {
            this(new ImmediateThreadPoolTaskExecutor());
        }

        private Fixture(ThreadPoolTaskExecutor executor)
        {
            assets.asset = asset();
            images.images.add(sourceImage());
            service = new DownloadingTaskService(tasks, assets, images, prompts, storage, text, image,
                    vision, composition, audit, executor, environment());
        }

        private AiProductImageTaskRequest request()
        {
            AiProductImageTaskRequest request = new AiProductImageTaskRequest();
            request.setProductAssetId("asset-1");
            request.setAssetType("main");
            request.setTargetPlatform("douyin");
            request.setAspectRatio("3:4");
            request.setQuality("2K");
            request.setTargetLanguage("zh-CN");
            request.setTextEnabled(false);
            request.setSelectedModel("mock-product-image-model");
            request.setUserRequirement("highlight the red shoe");
            return request;
        }

        private AiProductAsset asset()
        {
            AiProductAsset asset = new AiProductAsset();
            asset.setId("asset-1");
            asset.setUserId(1L);
            asset.setName("shoe");
            asset.setStatus("draft_uploaded");
            asset.setVisible(false);
            return asset;
        }

        private AiProductAssetImage sourceImage()
        {
            AiProductAssetImage image = new AiProductAssetImage();
            image.setId("source-1");
            image.setProductAssetId("asset-1");
            image.setUserId(1L);
            image.setImageRole("source");
            image.setOssUrl("https://oss.example/source.png");
            image.setOssObjectKey("source.png");
            return image;
        }

        private MockEnvironment environment()
        {
            return new MockEnvironment()
                    .withProperty("egg.ai.model-gateway.product-image.provider", "mock")
                    .withProperty("egg.ai.model-gateway.product-image.model", "mock-product-image-model");
        }
    }

    private static class DownloadingTaskService extends AiProductImageTaskServiceImpl
    {
        private DownloadingTaskService(AiProductImageTaskMapper taskMapper, AiProductAssetMapper assetMapper,
                AiProductAssetImageMapper imageMapper, AiPromptTemplateMapper promptMapper,
                AiObjectStorageService storage, AiTextGenerationService text, AiImageGenerationService image,
                AiVisionUnderstandingService vision, AiProductImageCompositionService composition,
                AiProductImageAuditService audit, ThreadPoolTaskExecutor executor, MockEnvironment environment)
        {
            super(taskMapper, assetMapper, imageMapper, promptMapper, storage, text, image, vision, composition, audit,
                    executor, environment);
        }

        @Override
        protected byte[] downloadGeneratedImage(String url)
        {
            return new byte[] {1, 2, 3};
        }
    }

    private static class RealDownloadTaskService extends AiProductImageTaskServiceImpl
    {
        private RealDownloadTaskService(Fixture fixture)
        {
            super(fixture.tasks, fixture.assets, fixture.images, fixture.prompts, fixture.storage, fixture.text,
                    fixture.image, fixture.vision, fixture.composition, fixture.audit, new ImmediateThreadPoolTaskExecutor(),
                    fixture.environment());
        }

        private byte[] downloadForTest(String url)
        {
            return super.downloadGeneratedImage(url);
        }
    }

    private static class ImmediateThreadPoolTaskExecutor extends ThreadPoolTaskExecutor
    {
        @Override
        public Future<?> submit(Runnable task)
        {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class RecordingThreadPoolTaskExecutor extends ThreadPoolTaskExecutor
    {
        private int submissions;
        private Runnable lastTask;

        @Override
        public Future<?> submit(Runnable task)
        {
            submissions++;
            lastTask = task;
            return CompletableFuture.completedFuture(null);
        }

        private void runLastTask()
        {
            lastTask.run();
        }
    }

    private static class FakeTaskMapper implements AiProductImageTaskMapper
    {
        private AiProductImageTask task;
        private String persistedStatus;
        private final List<String> jsonWrites = new ArrayList<>();

        @Override public AiProductImageTask selectAiProductImageTaskById(String id) { return task; }
        @Override public List<AiProductImageTask> selectAiProductImageTaskList(AiProductImageTask query) { return task == null ? List.of() : List.of(task); }
        @Override public int insertAiProductImageTask(AiProductImageTask value) { task = value; persistedStatus = value.getStatus(); recordJson(value); return 1; }
        @Override public int updateAiProductImageTask(AiProductImageTask value) { task = value; persistedStatus = value.getStatus(); recordJson(value); return 1; }
        @Override public int updateAiProductImageTaskIfStatus(AiProductImageTask value, String expectedStatus)
        {
            if (!expectedStatus.equals(persistedStatus)) return 0;
            task = value;
            persistedStatus = value.getStatus();
            recordJson(value);
            return 1;
        }
        private void recordJson(AiProductImageTask value)
        {
            jsonWrites.add(value.getAnalysisOutput());
            jsonWrites.add(value.getPlanningOutput());
            jsonWrites.add(value.getGenerationOutput());
            jsonWrites.add(value.getDebugPrompts());
            jsonWrites.add(value.getModelParams());
        }
    }

    private static class FakeAssetMapper implements AiProductAssetMapper
    {
        private AiProductAsset asset;

        @Override public AiProductAsset selectAiProductAssetById(String id) { return asset != null && id.equals(asset.getId()) ? asset : null; }
        @Override public AiProductAsset selectCurrentDraft(Long userId) { return null; }
        @Override public List<AiProductAsset> selectExpiredDrafts(Date expireBefore) { return List.of(); }
        @Override public List<AiProductAsset> selectVisibleAssetList(AiProductAsset query) { return List.of(); }
        @Override public int insertAiProductAsset(AiProductAsset value) { asset = value; return 1; }
        @Override public int updateAiProductAsset(AiProductAsset value) { asset = value; return 1; }
        @Override public int deleteAiProductAssetById(String id, Long userId) { return 0; }
    }

    private static class FakeImageMapper implements AiProductAssetImageMapper
    {
        private final List<AiProductAssetImage> images = new ArrayList<>();

        @Override public AiProductAssetImage selectAiProductAssetImageById(String id) { return null; }
        @Override public AiProductAssetImage selectImageByObjectKey(String objectKey) { return null; }
        @Override public List<AiProductAssetImage> selectImagesByAssetId(String assetId, Long userId) { return images.stream().filter(image -> assetId.equals(image.getProductAssetId()) && userId.equals(image.getUserId())).toList(); }
        @Override public List<AiProductAssetImage> selectImagesByTaskId(String taskId, Long userId) { return images.stream().filter(image -> taskId.equals(image.getTaskId()) && userId.equals(image.getUserId())).toList(); }
        @Override public int insertAiProductAssetImage(AiProductAssetImage image) { images.add(image); return 1; }
        @Override public int updateAiProductAssetImage(AiProductAssetImage image) { return 1; }
        @Override public int deleteAiProductAssetImageById(String id, Long userId) { return 0; }
        @Override public int deleteImagesByAssetId(String assetId, Long userId) { return 0; }
    }

    private static class FakePromptMapper implements AiPromptTemplateMapper
    {
        private final List<String> requests = new ArrayList<>();

        @Override public AiPromptTemplate selectAiPromptTemplateById(String id) { return null; }
        @Override public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate) { return List.of(); }
        @Override public int insertAiPromptTemplate(AiPromptTemplate promptTemplate) { return 1; }
        @Override public int updateAiPromptTemplate(AiPromptTemplate promptTemplate) { return 1; }
        @Override public int deleteAiPromptTemplateById(String id) { return 1; }
        @Override public int deleteAiPromptTemplateByIds(String[] ids) { return ids.length; }
        @Override public AiPromptTemplate selectPublishedTemplateForStoryStage(String category, String platformPrefix, List<String> fallbackCategories) { return null; }
        @Override public AiPromptTemplate selectPublishedTemplateByCategory(String category) { return null; }
        @Override public AiPromptTemplate selectPublishedTemplateByNameAndCategory(String name, String category)
        {
            requests.add(name + "|" + category);
            AiPromptTemplate template = new AiPromptTemplate();
            template.setId(name);
            template.setVersion(1);
            if ("商品图视觉分析提示词".equals(name))
            {
                template.setContent("{\"body\":\"configured analysis {userRequirement} {targetPlatform} {productCategory} {targetLanguage} textEnabled={textEnabled}\"}");
            }
            else if ("商品图视觉审核提示词".equals(name))
            {
                template.setContent("{\"body\":\"configured audit {taskId} {targetPlatform} {targetLanguage} textEnabled={textEnabled} {imagePlan}\"}");
            }
            else
            {
                template.setContent("{\"body\":\"{userRequirement} {sourceImageAnalysis} {targetPlatform} {productCategory} {modelName} {selectedModel} {aspectRatio} {clarity} {imageCount} {targetLanguage} textEnabled={textEnabled} {textPolicy} {compositionPolicy} {platformRules} {categoryRules} {modelAdapterRules}\"}");
            }
            return template;
        }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private int uploadGeneratedCalls;
        private int presignCalls;

        @Override public AiStoredObject uploadKnowledgeFile(MultipartFile file) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId) { uploadGeneratedCalls++; String name = uploadGeneratedCalls == 1 ? "generated" : "generated-" + uploadGeneratedCalls; AiStoredObject object = new AiStoredObject(); object.setBucket("bucket"); object.setObjectKey("generated/" + name + "." + extension); object.setUrl("https://oss.example/" + name + "." + extension); object.setMimeType("image/" + extension); object.setSize((long) bytes.length); return object; }
        @Override public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId) { throw new UnsupportedOperationException(); }
        @Override public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan) { return List.of(); }
        @Override public byte[] downloadObject(String objectKey) { throw new UnsupportedOperationException(); }
        @Override public String presignObjectUrl(String objectKey, long expireSeconds) { presignCalls++; return "https://signed.example/" + objectKey; }
        @Override public void deleteObject(String objectKey) { }
    }

    private static class FakeTextGenerationService implements AiTextGenerationService
    {
        private int calls;
        private final List<String> prompts = new ArrayList<>();
        private boolean invalidPlanning;
        private boolean arrayPlanning;
        private int detailPromptCount;
        private boolean detailEmptyImagePrompt;
        private final List<Integer> mainPromptCounts = new ArrayList<>();
        private boolean repeatedPunctuationPlanning;

        @Override
        public AiTextGenerationResult generate(AiTextGenerationRequest request)
        {
            calls++;
            prompts.add(request.getPrompt());
            AiTextGenerationResult result = new AiTextGenerationResult();
            result.setProvider("mock");
            result.setModel("mock-product-image-model");
            int mainPromptCount = mainPromptCounts.isEmpty() ? 0 : mainPromptCounts.remove(0);
            result.setContent(invalidPlanning ? "{\"legacyPrompt\":\"missing image prompt\"}"
                    : detailPromptCount > 0 ? detailPlanning()
                            : mainPromptCount > 0 ? mainPlanning(mainPromptCount)
                            : arrayPlanning ? "{\"overallDesignSpec\":{\"styleKeywords\":[\"clean\"]},\"imagePrompts\":[{\"positivePrompt\":\"array red shoe product photo\",\"negativePrompt\":\"blurry\"}]}"
                                    : "{\"overallDesignSpec\":{\"styleKeywords\":[\"clean\"]},\"imagePrompt\":{\"positivePrompt\":\"red shoe product photo\",\"negativePrompt\":\"blurry\"}}");
            return result;
        }

        private String mainPlanning(int count)
        {
            JSONArray prompts = new JSONArray();
            for (int index = 1; index <= count; index++)
            {
                JSONObject prompt = new JSONObject();
                prompt.put("promptId", "main_" + index);
                prompt.put("title", "Main scheme " + index);
                prompt.put("designTarget", "Distinct main image composition " + index);
                prompt.put("positivePrompt", "main product photo scheme " + index);
                prompt.put("negativePrompt", "blurry");
                JSONObject textOverlay = new JSONObject();
                textOverlay.put("enabled", true);
                textOverlay.put("title", "Title " + index);
                textOverlay.put("subtitle", "Subtitle " + index);
                textOverlay.put("sellingPoints", List.of("Point " + index));
                prompt.put("textOverlay", textOverlay);
                prompts.add(prompt);
            }
            JSONObject output = new JSONObject();
            JSONObject design = new JSONObject();
            design.put("styleKeywords", List.of(repeatedPunctuationPlanning ? "clean、、、、、" : "clean"));
            output.put("overallDesignSpec", design);
            output.put("imagePrompts", prompts);
            return output.toJSONString();
        }

        private String detailPlanning()
        {
            JSONArray prompts = new JSONArray();
            for (int index = 1; index <= detailPromptCount; index++)
            {
                JSONObject prompt = new JSONObject();
                prompt.put("promptId", "detail_" + index);
                prompt.put("positivePrompt", "detail product photo " + index);
                prompt.put("negativePrompt", "blurry");
                prompts.add(prompt);
            }
            JSONObject output = new JSONObject();
            JSONObject design = new JSONObject();
            design.put("styleKeywords", List.of("detail clean"));
            output.put("overallDesignSpec", design);
            if (detailEmptyImagePrompt)
            {
                output.put("imagePrompt", new JSONObject());
            }
            output.put("imagePrompts", prompts);
            return output.toJSONString();
        }
    }

    private static class FakeVisionUnderstandingService implements AiVisionUnderstandingService
    {
        private int calls;
        private AiVisionUnderstandingRequest lastRequest;
        private final List<AiVisionUnderstandingRequest> requests = new ArrayList<>();
        private boolean failAudit;

        @Override
        public AiVisionUnderstandingResult analyze(AiVisionUnderstandingRequest request)
        {
            calls++;
            lastRequest = request;
            requests.add(request);
            if ("product_image_audit".equals(request.getStage()))
            {
                if (failAudit)
                {
                    throw new ServiceException("vision audit unavailable");
                }
                AiVisionUnderstandingResult result = new AiVisionUnderstandingResult();
                result.setProvider("mock");
                result.setModel("mock-vision-model");
                result.setContent("{\"status\":\"pass\",\"risks\":[],\"recommendations\":[]}");
                return result;
            }
            AiVisionUnderstandingResult result = new AiVisionUnderstandingResult();
            result.setProvider("mock");
            result.setModel("mock-vision-model");
            result.setContent("{\"sourceSummary\":{\"productNameGuess\":\"primary red shoe\",\"identityGuards\":[\"keep red color\"]}}");
            return result;
        }
    }

    private static class FakeCompositionService implements AiProductImageCompositionService
    {
        private int calls;

        @Override
        public AiProductImageCompositionResult compose(byte[] baseImageBytes, JSONObject imagePlan)
        {
            calls++;
            AiProductImageCompositionResult result = new AiProductImageCompositionResult();
            result.setImageBytes(new byte[] {9, 9, 9});
            result.setExtension("png");
            result.setApplied(true);
            JSONObject metadata = new JSONObject();
            metadata.put("template", "test-template");
            result.setMetadata(metadata);
            return result;
        }
    }

    private static class FakeImageGenerationService implements AiImageGenerationService
    {
        private int calls;
        private boolean fail;
        private Runnable beforeGenerate;
        private AiImageGenerationRequest lastRequest;
        private final List<AiImageGenerationRequest> requests = new ArrayList<>();
        private final List<List<String>> responseBatches = new ArrayList<>();

        @Override
        public AiImageGenerationResult generate(AiImageGenerationRequest request)
        {
            calls++;
            lastRequest = request;
            requests.add(request);
            if (beforeGenerate != null)
            {
                beforeGenerate.run();
            }
            if (fail)
            {
                throw new ServiceException("image generation failed");
            }
            AiImageGenerationResult result = new AiImageGenerationResult();
            result.setProvider("mock");
            result.setModel("mock-product-image-model");
            result.setImageUrls(responseBatches.isEmpty() ? defaultGeneratedUrls(request) : responseBatches.remove(0));
            return result;
        }

        private List<String> defaultGeneratedUrls(AiImageGenerationRequest request)
        {
            int count = request.getImageCount() == null ? 1 : request.getImageCount();
            List<String> urls = new ArrayList<>();
            for (int index = 1; index <= count; index++)
            {
                urls.add("https://example.invalid/generated-" + index + ".png");
            }
            return urls;
        }
    }
}
