package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.egg.commerce.system.service.ai.product.impl.AiProductImageTaskServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

class AiProductImageTaskServiceImplTest
{
    @Test
    void createTaskRunsAnalysisPlanningGenerationAndPublishesAsset()
    {
        Fixture fixture = new Fixture();

        AiProductImageTask task = fixture.service.createTask(1L, fixture.request());

        assertEquals("completed", fixture.tasks.task.getStatus());
        assertEquals("完成", fixture.tasks.task.getCurrentNode());
        assertTrue(fixture.tasks.task.getAnalysisOutput().contains("sourceImageAnalysis"));
        assertTrue(fixture.tasks.task.getPlanningOutput().contains("positivePrompt"));
        assertTrue(fixture.tasks.task.getGenerationOutput().contains("generated.png"));
        assertEquals("completed", fixture.assets.asset.getStatus());
        assertTrue(fixture.assets.asset.getVisible());
        assertEquals(task.getId(), fixture.assets.asset.getLastTaskId());
        assertEquals(2, fixture.text.calls);
        assertEquals(1, fixture.image.calls);
        assertEquals(1, fixture.storage.uploadGeneratedCalls);
        assertEquals(2, fixture.images.images.size());
        assertEquals("generated", fixture.images.images.get(1).getImageRole());
    }

    @Test
    void generationFailureKeepsAnalysisAndPlanningForRetry()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;

        fixture.service.createTask(1L, fixture.request());

        assertEquals("failed", fixture.tasks.task.getStatus());
        assertEquals("生成中", fixture.tasks.task.getCurrentNode());
        assertNotNull(fixture.tasks.task.getAnalysisOutput());
        assertNotNull(fixture.tasks.task.getPlanningOutput());
        assertEquals("failed", fixture.assets.asset.getStatus());
        assertFalse(fixture.assets.asset.getVisible());
        assertEquals(2, fixture.text.calls);
        assertEquals(0, fixture.storage.uploadGeneratedCalls);
    }

    @Test
    void retryGenerationFailureStartsAtGenerationNode()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;
        fixture.service.createTask(1L, fixture.request());
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
        assertEquals(2, fixture.image.calls);
        assertTrue(fixture.assets.asset.getVisible());
    }

    @Test
    void taskJsonOutputsUseEmptyObjectsWhenCreatedAndRetried()
    {
        Fixture fixture = new Fixture();
        fixture.image.fail = true;

        fixture.service.createTask(1L, fixture.request());

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
        String planningPrompt = fixture.text.prompts.get(1);
        assertTrue(planningPrompt.contains("douyin"));
        assertTrue(planningPrompt.contains("main"));
        assertTrue(planningPrompt.contains("3:4"));
        assertTrue(planningPrompt.contains("2K"));
        assertTrue(planningPrompt.contains("mock-product-image-model"));
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

        TransactionSynchronizationManager.initSynchronization();
        try
        {
            fixture.service.createTask(1L, fixture.request());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            executor.runLastTask();
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

            assertEquals(1, executor.submissions);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(2, executor.submissions);
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

        fixture.service.createTask(1L, fixture.request());

        assertEquals("cancelled", fixture.tasks.task.getStatus());
        assertEquals("用户已终止任务", fixture.tasks.task.getCancelReason());
        assertNotNull(fixture.tasks.task.getCancelledAt());
        assertEquals("cancelled", fixture.assets.asset.getStatus());
        assertFalse(fixture.assets.asset.getVisible());
        assertThrows(ServiceException.class, () -> fixture.service.retryTask(fixture.tasks.task.getId(), 1L));
        assertEquals(0, fixture.storage.uploadGeneratedCalls);
    }

    private static class Fixture
    {
        private final FakeTaskMapper tasks = new FakeTaskMapper();
        private final FakeAssetMapper assets = new FakeAssetMapper();
        private final FakeImageMapper images = new FakeImageMapper();
        private final FakeStorage storage = new FakeStorage();
        private final FakeTextGenerationService text = new FakeTextGenerationService();
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
            service = new DownloadingTaskService(tasks, assets, images, new FakePromptMapper(), storage, text, image,
                    executor, environment());
        }

        private AiProductImageTaskRequest request()
        {
            AiProductImageTaskRequest request = new AiProductImageTaskRequest();
            request.setProductAssetId("asset-1");
            request.setAssetType("main");
            request.setTargetPlatform("douyin");
            request.setAspectRatio("3:4");
            request.setQuality("2K");
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
                ThreadPoolTaskExecutor executor, MockEnvironment environment)
        {
            super(taskMapper, assetMapper, imageMapper, promptMapper, storage, text, image, executor, environment);
        }

        @Override
        protected byte[] downloadGeneratedImage(String url)
        {
            return new byte[] {1, 2, 3};
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
            AiPromptTemplate template = new AiPromptTemplate();
            template.setId("main-template");
            template.setVersion(1);
            template.setContent("{\"body\":\"{userRequirement} {sourceImageAnalysis} {targetPlatform} {productCategory} {modelName} {aspectRatio} {clarity} {imageCount} {platformRules} {categoryRules} {modelAdapterRules}\"}");
            return template;
        }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private int uploadGeneratedCalls;

        @Override public AiStoredObject uploadKnowledgeFile(MultipartFile file) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId) { uploadGeneratedCalls++; AiStoredObject object = new AiStoredObject(); object.setBucket("bucket"); object.setObjectKey("generated/generated.png"); object.setUrl("https://oss.example/generated.png"); object.setMimeType("image/png"); object.setSize((long) bytes.length); return object; }
        @Override public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId) { throw new UnsupportedOperationException(); }
        @Override public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan) { return List.of(); }
        @Override public byte[] downloadObject(String objectKey) { throw new UnsupportedOperationException(); }
        @Override public void deleteObject(String objectKey) { }
    }

    private static class FakeTextGenerationService implements AiTextGenerationService
    {
        private int calls;
        private final List<String> prompts = new ArrayList<>();
        private boolean invalidPlanning;

        @Override
        public AiTextGenerationResult generate(AiTextGenerationRequest request)
        {
            calls++;
            prompts.add(request.getPrompt());
            AiTextGenerationResult result = new AiTextGenerationResult();
            result.setProvider("mock");
            result.setModel("mock-product-image-model");
            result.setContent(calls == 1 ? "{\"sourceImageAnalysis\":\"clean shoe\"}"
                    : invalidPlanning ? "{\"positivePrompt\":\"legacy prompt\"}"
                            : "{\"imagePrompt\":{\"positivePrompt\":\"red shoe product photo\",\"negativePrompt\":\"blurry\"}}");
            return result;
        }
    }

    private static class FakeImageGenerationService implements AiImageGenerationService
    {
        private int calls;
        private boolean fail;
        private Runnable beforeGenerate;

        @Override
        public AiImageGenerationResult generate(AiImageGenerationRequest request)
        {
            calls++;
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
            result.setImageUrls(List.of("https://example.invalid/generated.png"));
            return result;
        }
    }
}
