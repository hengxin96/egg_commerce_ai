package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductImageTask;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskConfirmRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskReplanRequest;
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
import com.egg.commerce.system.service.ai.product.AiProductImageAuditRequest;
import com.egg.commerce.system.service.ai.product.AiProductImageAuditService;
import com.egg.commerce.system.service.ai.product.AiProductImageTaskService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates the persisted product-image generation workflow. */
@Service
public class AiProductImageTaskServiceImpl implements AiProductImageTaskService
{
    private static final Logger log = LoggerFactory.getLogger(AiProductImageTaskServiceImpl.class);

    private static final String TASK_TYPE_PRODUCT_IMAGE = "product_image";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_WAITING_CONFIRMATION = "waiting_confirmation";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String DEFAULT_PRODUCT_NAME_PREFIX = "未命名商品-";
    private static final String NODE_INPUT = "输入";
    private static final String NODE_ANALYSIS = "分析中";
    private static final String NODE_PLANNING = "确认规划";
    private static final String NODE_GENERATING = "生成中";
    private static final String NODE_COMPLETE = "完成";
    private static final String TEMPLATE_CATEGORY_IMAGE = "AI生图";
    private static final String TEMPLATE_CATEGORY_REWRITE = "AI优化提示词";
    private static final String TEMPLATE_CATEGORY_VISUAL_ANALYSIS = "AI视觉分析";
    private static final String TEMPLATE_MAIN = "主图系统提示词";
    private static final String TEMPLATE_DETAIL = "详情图系统提示词";
    private static final String TEMPLATE_VISUAL_ANALYSIS = "商品图视觉分析提示词";
    private static final String STATUS_DRAFT_UPLOADED = "draft_uploaded";
    private static final String ASSET_TYPE_MAIN = "main";
    private static final String ASSET_TYPE_DETAIL = "detail";
    private static final String ROLE_SOURCE = "source";
    private static final String ROLE_GENERATED = "generated";
    private static final String ROLE_GENERATED_BASE = "generated_base";
    private static final int REFERENCE_SIGNED_URL_EXPIRE_SECONDS = 1800;
    private static final int IMAGE_GENERATION_RETRY_TIMES = 2;
    private static final int MAIN_PLANNING_RETRY_TIMES = 2;
    private static final String MAIN_PLANNING_INSUFFICIENT_MESSAGE = "规划方案数量不足，请重新规划";
    private static final int GENERATED_IMAGE_DOWNLOAD_MAX_ATTEMPTS = 3;
    private static final int GENERATED_IMAGE_DOWNLOAD_TIMEOUT_SECONDS = 45;
    private static final long GENERATED_IMAGE_DOWNLOAD_RETRY_DELAY_MILLIS = 350L;
    private static final HttpClient GENERATED_IMAGE_DOWNLOAD_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final String[] DETAIL_FALLBACK_SECTIONS = {
            "selling_point", "scene", "detail", "size", "material", "package", "usage", "atmosphere"
    };

    private final AiProductImageTaskMapper taskMapper;
    private final AiProductAssetMapper assetMapper;
    private final AiProductAssetImageMapper imageMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiObjectStorageService objectStorageService;
    private final AiTextGenerationService textGenerationService;
    private final AiImageGenerationService imageGenerationService;
    private final AiVisionUnderstandingService visionUnderstandingService;
    private final AiProductImageCompositionService compositionService;
    private final AiProductImageAuditService auditService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;

    /** Production constructor supplies transactions for worker-side multi-row state changes. */
    @Autowired
    public AiProductImageTaskServiceImpl(AiProductImageTaskMapper taskMapper, AiProductAssetMapper assetMapper,
            AiProductAssetImageMapper imageMapper, AiPromptTemplateMapper promptTemplateMapper,
            AiObjectStorageService objectStorageService, AiTextGenerationService textGenerationService,
            AiImageGenerationService imageGenerationService, AiVisionUnderstandingService visionUnderstandingService,
            AiProductImageCompositionService compositionService, AiProductImageAuditService auditService,
            ThreadPoolTaskExecutor taskExecutor, Environment environment, PlatformTransactionManager transactionManager)
    {
        this(taskMapper, assetMapper, imageMapper, promptTemplateMapper, objectStorageService, textGenerationService,
                imageGenerationService, visionUnderstandingService, compositionService, auditService, taskExecutor, environment,
                new TransactionTemplate(transactionManager));
    }

    public AiProductImageTaskServiceImpl(AiProductImageTaskMapper taskMapper, AiProductAssetMapper assetMapper,
            AiProductAssetImageMapper imageMapper, AiPromptTemplateMapper promptTemplateMapper,
            AiObjectStorageService objectStorageService, AiTextGenerationService textGenerationService,
            AiImageGenerationService imageGenerationService, AiVisionUnderstandingService visionUnderstandingService,
            AiProductImageCompositionService compositionService, AiProductImageAuditService auditService,
            ThreadPoolTaskExecutor taskExecutor, Environment environment)
    {
        this(taskMapper, assetMapper, imageMapper, promptTemplateMapper, objectStorageService, textGenerationService,
                imageGenerationService, visionUnderstandingService, compositionService, auditService, taskExecutor, environment,
                (TransactionTemplate) null);
    }

    private AiProductImageTaskServiceImpl(AiProductImageTaskMapper taskMapper, AiProductAssetMapper assetMapper,
            AiProductAssetImageMapper imageMapper, AiPromptTemplateMapper promptTemplateMapper,
            AiObjectStorageService objectStorageService, AiTextGenerationService textGenerationService,
            AiImageGenerationService imageGenerationService, AiVisionUnderstandingService visionUnderstandingService,
            AiProductImageCompositionService compositionService, AiProductImageAuditService auditService,
            ThreadPoolTaskExecutor taskExecutor, Environment environment, TransactionTemplate transactionTemplate)
    {
        this.taskMapper = taskMapper;
        this.assetMapper = assetMapper;
        this.imageMapper = imageMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.objectStorageService = objectStorageService;
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.visionUnderstandingService = visionUnderstandingService;
        this.compositionService = compositionService;
        this.auditService = auditService;
        this.taskExecutor = taskExecutor;
        this.environment = environment;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional
    public AiProductImageTask createTask(Long userId, AiProductImageTaskRequest request)
    {
        AiProductAsset asset = validateCreateRequest(userId, request);
        Date now = new Date();
        AiProductImageTask task = new AiProductImageTask();
        task.setId(UUID.randomUUID().toString());
        task.setProductAssetId(asset.getId());
        task.setUserId(userId);
        task.setTaskType(TASK_TYPE_PRODUCT_IMAGE);
        task.setAssetType(normalizeAssetType(request.getAssetType()));
        task.setTargetPlatform(StringUtils.trim(request.getTargetPlatform()));
        task.setCurrentNode(NODE_INPUT);
        task.setStatus(STATUS_PROCESSING);
        task.setAnalysisOutput("{}");
        task.setPlanningOutput("{}");
        task.setGenerationOutput("{}");
        task.setDebugPrompts("{}");
        task.setModelProvider(modelProvider());
        task.setModelName(selectedModelForRequest(request));
        task.setModelParams(modelParams(request).toJSONString());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        applyRequestToAsset(asset, request);
        asset.setStatus(STATUS_PROCESSING);
        asset.setVisible(false);
        asset.setLastTaskId(task.getId());
        asset.setUpdatedAt(now);
        updateAsset(asset);
        taskMapper.insertAiProductImageTask(task);
        scheduleTaskExecution(task.getId());
        return task;
    }

    /** Schedules the worker only after the task row is visible to its independent database connection. */
    private void scheduleTaskExecution(String taskId)
    {
        scheduleTaskExecution(() -> executeTask(taskId));
    }

    private void scheduleTaskExecution(String taskId, String startNode)
    {
        scheduleTaskExecution(() -> executeTaskFrom(taskId, startNode));
    }

    private void scheduleTaskExecution(Runnable execution)
    {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            taskExecutor.submit(execution);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                taskExecutor.submit(execution);
            }
        });
    }

    @Override
    public AiProductImageTask selectTaskDetail(String taskId, Long userId)
    {
        return requireOwnedTask(taskId, userId);
    }

    @Override
    public List<AiProductImageTask> selectTaskList(Long userId)
    {
        requireUser(userId);
        AiProductImageTask query = new AiProductImageTask();
        query.setUserId(userId);
        query.setTaskType(TASK_TYPE_PRODUCT_IMAGE);
        return taskMapper.selectAiProductImageTaskList(query);
    }

    @Override
    @Transactional
    public AiProductImageTask retryTask(String taskId, Long userId)
    {
        AiProductImageTask task = requireOwnedTask(taskId, userId);
        if (STATUS_COMPLETED.equals(task.getStatus()))
        {
            throw new ServiceException("当前任务已完成，无需重试");
        }
        if (STATUS_PROCESSING.equals(task.getStatus()))
        {
            throw new ServiceException("任务正在生成中，请稍后");
        }
        if (STATUS_WAITING_CONFIRMATION.equals(task.getStatus()))
        {
            throw new ServiceException("当前规划等待确认，无需重试");
        }
        if (STATUS_CANCELLED.equals(task.getStatus()))
        {
            throw new ServiceException("已终止任务不能重试");
        }
        if (!STATUS_FAILED.equals(task.getStatus()))
        {
            throw new ServiceException("当前任务状态不支持重试");
        }

        String startNode = retryStartNode(task);
        clearFromNode(task, startNode);
        task.setCurrentNode(startNode);
        task.setStatus(STATUS_PROCESSING);
        task.setFailureReason(null);
        task.setCompletedAt(null);
        saveTaskIfStatus(task, STATUS_FAILED);

        AiProductAsset asset = requireOwnedAsset(userId, task.getProductAssetId());
        asset.setStatus(STATUS_PROCESSING);
        asset.setVisible(false);
        asset.setUpdatedAt(new Date());
        updateAsset(asset);
        scheduleTaskExecution(task.getId(), startNode);
        return task;
    }

    @Override
    @Transactional
    public AiProductImageTask cancelTask(String taskId, Long userId)
    {
        AiProductImageTask task = requireOwnedTask(taskId, userId);
        String expectedStatus = task.getStatus();
        if (!STATUS_PROCESSING.equals(expectedStatus) && !STATUS_WAITING_CONFIRMATION.equals(expectedStatus))
        {
            throw new ServiceException("只有生成中或待确认的任务可以终止");
        }
        Date now = new Date();
        task.setStatus(STATUS_CANCELLED);
        task.setCancelReason("用户已终止任务");
        task.setCancelledAt(now);
        task.setUpdatedAt(now);
        saveTaskIfStatus(task, expectedStatus);

        AiProductAsset asset = requireOwnedAsset(userId, task.getProductAssetId());
        asset.setStatus(STATUS_CANCELLED);
        asset.setVisible(false);
        asset.setUpdatedAt(now);
        updateAsset(asset);
        return task;
    }

    @Override
    @Transactional
    public AiProductImageTask confirmTask(String taskId, Long userId, AiProductImageTaskConfirmRequest request)
    {
        AiProductImageTask task = requireOwnedTask(taskId, userId);
        if (!STATUS_WAITING_CONFIRMATION.equals(task.getStatus()))
        {
            throw new ServiceException("当前任务不在待确认规划状态");
        }
        if (request != null && StringUtils.isNotBlank(request.getPlanningOutput()))
        {
            // 用户只能提交结构化规划 JSON；后端会重新校验后再进入生图，避免坏规划直接打到模型。
            task.setPlanningOutput(validPlanningJson(request.getPlanningOutput(), textEnabledForTask(task),
                    imageCountForTask(task), ASSET_TYPE_MAIN.equals(task.getAssetType())));
        }
        task.setStatus(STATUS_PROCESSING);
        task.setCurrentNode(NODE_GENERATING);
        task.setFailureReason(null);
        task.setGenerationOutput("{}");
        saveTaskIfStatus(task, STATUS_WAITING_CONFIRMATION);
        scheduleTaskExecution(task.getId(), NODE_GENERATING);
        return task;
    }

    @Override
    @Transactional
    public AiProductImageTask replanTask(String taskId, Long userId, AiProductImageTaskReplanRequest request)
    {
        AiProductImageTask task = requireOwnedTask(taskId, userId);
        if (!STATUS_WAITING_CONFIRMATION.equals(task.getStatus()))
        {
            throw new ServiceException("只有待确认规划的任务可以重新规划");
        }
        if (request != null && StringUtils.isNotBlank(request.getUserRequirement()))
        {
            AiProductAsset asset = requireOwnedAsset(userId, task.getProductAssetId());
            asset.setUserRequirement(StringUtils.trim(request.getUserRequirement()));
            asset.setUpdatedAt(new Date());
            updateAsset(asset);
        }
        task.setStatus(STATUS_PROCESSING);
        task.setCurrentNode(NODE_PLANNING);
        task.setFailureReason(null);
        task.setPlanningOutput("{}");
        task.setGenerationOutput("{}");
        saveTaskIfStatus(task, STATUS_WAITING_CONFIRMATION);
        scheduleTaskExecution(task.getId(), NODE_PLANNING);
        return task;
    }

    @Override
    public void executeTask(String taskId)
    {
        executeTaskFrom(taskId, NODE_ANALYSIS);
    }

    private void executeTaskFrom(String taskId, String startNode)
    {
        AiProductImageTask task = taskMapper.selectAiProductImageTaskById(taskId);
        if (task == null || STATUS_CANCELLED.equals(task.getStatus()))
        {
            return;
        }
        try
        {
            task.setStatus(STATUS_PROCESSING);
            task.setStartedAt(task.getStartedAt() == null ? new Date() : task.getStartedAt());
            saveTaskIfStatus(task, STATUS_PROCESSING);
            if (NODE_ANALYSIS.equals(startNode))
            {
                runAnalysisStage(task);
            }
            if (NODE_ANALYSIS.equals(startNode) || NODE_PLANNING.equals(startNode))
            {
                runPlanningStage(task);
                waitForUserConfirmation(task);
                return;
            }
            runGenerationStage(task);
            completeTask(task);
        }
        catch (TaskCancelledException ignored)
        {
            // cancelTask persists the final state before the worker notices it.
        }
        catch (Exception e)
        {
            failTask(task, e);
        }
    }

    private void runAnalysisStage(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        beginStage(task, NODE_ANALYSIS);
        String output = analyzeSourceImages(task);
        task.setAnalysisOutput(validJson(output));
        saveTaskIfStatus(task, STATUS_PROCESSING);
        ensureNotCancelled(task.getId());
    }

    private void runPlanningStage(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        beginStage(task, NODE_PLANNING);
        task.setPlanningOutput(generateValidatedPlanning(task).toJSONString());
        saveTaskIfStatus(task, STATUS_PROCESSING);
        ensureNotCancelled(task.getId());
    }

    private JSONObject generateValidatedPlanning(AiProductImageTask task)
    {
        boolean mainImageTask = ASSET_TYPE_MAIN.equals(task.getAssetType());
        int requestedCount = imageCountForTask(task);
        JSONObject lastPlanning = null;
        for (int attempt = 0; attempt <= MAIN_PLANNING_RETRY_TIMES; attempt++)
        {
            ensureNotCancelled(task.getId());
            String planningInput = attempt == 0
                    ? task.getAnalysisOutput()
                    : mainPlanningRetryInstruction(task.getAnalysisOutput(), lastPlanning, requestedCount);
            JSONObject planning = validPlanningObject(generateText(task, planningInput), textEnabledForTask(task));
            if (!mainImageTask || hasEnoughDistinctMainPrompts(planning, requestedCount))
            {
                return planning;
            }
            lastPlanning = planning;
        }
        // Main-image generation needs one distinct plan per requested result, otherwise text and output cannot align.
        throw new ServiceException(MAIN_PLANNING_INSUFFICIENT_MESSAGE);
    }

    private String mainPlanningRetryInstruction(String sourceImageAnalysis, JSONObject previousPlanning,
            int requestedCount)
    {
        JSONObject instruction = new JSONObject(new LinkedHashMap<>());
        instruction.put("instruction", "Regenerate the full main-image planning JSON. Output exactly " + requestedCount
                + " distinct imagePrompts. Each prompt must use a different composition/camera/scene/text layout while preserving product identity.");
        instruction.put("requiredImagePromptCount", requestedCount);
        instruction.put("sourceImageAnalysis", sourceImageAnalysis);
        instruction.put("previousPlanning", previousPlanning);
        instruction.put("failureReason", MAIN_PLANNING_INSUFFICIENT_MESSAGE);
        return instruction.toJSONString();
    }

    private void waitForUserConfirmation(AiProductImageTask task)
    {
        task.setCurrentNode(NODE_PLANNING);
        task.setStatus(STATUS_WAITING_CONFIRMATION);
        task.setUpdatedAt(new Date());
        saveTaskIfStatus(task, STATUS_PROCESSING);
    }

    private void runGenerationStage(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        beginStage(task, NODE_GENERATING);
        JSONObject planning = validatedPlanning(task.getPlanningOutput());
        int requestedCount = imageCountForTask(task);
        boolean textEnabled = textEnabledForTask(task);
        List<JSONObject> prompts = enforceTextPolicy(generationPrompts(task, planning, requestedCount), textEnabled);
        planning.put("imagePrompt", prompts.get(0));
        JSONArray promptArray = new JSONArray();
        promptArray.addAll(prompts);
        planning.put("imagePrompts", promptArray);
        task.setPlanningOutput(planning.toJSONString());
        saveTaskIfStatus(task, STATUS_PROCESSING);

        JSONArray images = new JSONArray();
        int sortOrder = 0;
        for (JSONObject prompt : prompts)
        {
            int promptCount = 1;
            for (String temporaryUrl : generateImagesWithRetry(task, prompt, promptCount))
            {
                ensureNotCancelled(task.getId());
                byte[] baseImageBytes = downloadGeneratedImage(temporaryUrl);
                AiStoredObject baseStored = objectStorageService.uploadGeneratedImage(baseImageBytes,
                        fileExtension(temporaryUrl), task.getUserId());
                AiProductAssetImage baseImage = generatedImage(task, baseStored, sortOrder, ROLE_GENERATED_BASE);
                AiProductImageCompositionResult composition = null;
                AiStoredObject finalStored;
                // When text is disabled, the generated base image is the final deliverable; Java2D is bypassed.
                if (textEnabled)
                {
                    composition = compositionService.compose(baseImageBytes, prompt);
                    finalStored = objectStorageService.uploadGeneratedImage(composition.getImageBytes(),
                            StringUtils.defaultIfBlank(composition.getExtension(), fileExtension(temporaryUrl)),
                            task.getUserId());
                }
                else
                {
                    finalStored = objectStorageService.uploadGeneratedImage(baseImageBytes, fileExtension(temporaryUrl),
                            task.getUserId());
                }
                AiProductAssetImage generated = generatedImage(task, finalStored, sortOrder, ROLE_GENERATED);
                ensureNotCancelled(task.getId());
                imageMapper.insertAiProductAssetImage(baseImage);
                imageMapper.insertAiProductAssetImage(generated);
                images.add(generationImageOutput(baseImage, generated, baseStored, finalStored, temporaryUrl, prompt,
                        composition, sortOrder + 1));
                sortOrder++;
            }
        }
        JSONObject output = new JSONObject(new LinkedHashMap<>());
        output.put("requestedCount", requestedCount);
        output.put("completedCount", images.size());
        output.put("textEnabled", textEnabled);
        output.put("images", images);
        task.setGenerationOutput(auditGenerationOutput(task, planning, output).toJSONString());
        saveTaskIfStatus(task, STATUS_PROCESSING);
        ensureNotCancelled(task.getId());
    }

    private JSONObject auditGenerationOutput(AiProductImageTask task, JSONObject planning, JSONObject output)
    {
        try
        {
            AiProductImageAuditRequest request = new AiProductImageAuditRequest();
            request.setTaskId(task.getId());
            request.setAssetType(task.getAssetType());
            request.setTargetPlatform(task.getTargetPlatform());
            request.setTargetLanguage(targetLanguageForTask(task));
            request.setTextEnabled(textEnabledForTask(task));
            request.setPlanningOutput(planning);
            request.setGenerationOutput(output);
            request.setSourceImageUrls(sourceImageSignedUrls(task));
            request.setFinalImageUrls(finalImageAuditUrls(output.getJSONArray("images")));
            request.setProvider(visionProvider());
            request.setModel(visionModelName());
            request.setHighResolution(visionHighResolutionEnabled());
            return auditService.audit(request);
        }
        catch (Exception e)
        {
            // Audit is informational for the user; generated OSS assets should still be published with a warning.
            return auditFallbackOutput(output, e);
        }
    }

    private JSONObject auditFallbackOutput(JSONObject output, Exception failure)
    {
        output.put("schemaVersion", "product_image_generation_v3");
        JSONArray images = output.getJSONArray("images");
        if (images == null)
        {
            images = new JSONArray();
            output.put("images", images);
        }
        for (int index = 0; index < images.size(); index++)
        {
            JSONObject image = images.getJSONObject(index);
            JSONObject warning = new JSONObject(new LinkedHashMap<>());
            warning.put("status", "warning");
            warning.put("completed", false);
            JSONArray risks = new JSONArray();
            JSONObject risk = new JSONObject(new LinkedHashMap<>());
            risk.put("type", "audit_failed");
            risk.put("severity", "warning");
            risk.put("reason", StringUtils.defaultIfBlank(failure.getMessage(), "Product image audit failed."));
            risks.add(risk);
            warning.put("risks", risks);
            image.put("ruleAudit", warning);
            image.put("visionAudit", warning);
        }
        JSONObject summary = new JSONObject(new LinkedHashMap<>());
        summary.put("status", "warning");
        summary.put("totalCount", images.size());
        summary.put("highRiskCount", 0);
        summary.put("warningCount", images.size());
        summary.put("visionCompletedCount", 0);
        output.put("auditSummary", summary);
        return output;
    }

    private List<String> finalImageAuditUrls(JSONArray images)
    {
        List<String> urls = new ArrayList<>();
        if (images == null)
        {
            return urls;
        }
        for (int index = 0; index < images.size(); index++)
        {
            JSONObject image = images.getJSONObject(index);
            String objectKey = StringUtils.defaultIfBlank(image.getString("finalOssObjectKey"),
                    image.getString("ossObjectKey"));
            if (StringUtils.isNotBlank(objectKey))
            {
                urls.add(objectStorageService.presignObjectUrl(objectKey, REFERENCE_SIGNED_URL_EXPIRE_SECONDS));
                continue;
            }
            urls.add(StringUtils.defaultIfBlank(image.getString("finalOssUrl"), image.getString("ossUrl")));
        }
        return urls;
    }

    private List<String> generateImagesWithRetry(AiProductImageTask task, JSONObject imagePrompt, int expectedCount)
    {
        List<String> urls = new ArrayList<>();
        Exception lastFailure = null;
        for (int attempt = 0; attempt <= IMAGE_GENERATION_RETRY_TIMES && urls.size() < expectedCount; attempt++)
        {
            try
            {
                AiImageGenerationResult result = callImageGeneration(task, imagePrompt, expectedCount - urls.size());
                task.setDashscopeTaskId(result.getTaskId());
                task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
                task.setModelName(StringUtils.defaultIfBlank(result.getModel(), selectedModelForTask(task)));
                for (String url : generatedUrls(result))
                {
                    if (urls.size() >= expectedCount)
                    {
                        break;
                    }
                    urls.add(url);
                }
            }
            catch (Exception e)
            {
                lastFailure = e;
            }
        }
        if (urls.size() < expectedCount)
        {
            if (lastFailure instanceof ServiceException serviceException)
            {
                throw serviceException;
            }
            throw new ServiceException("商品图生成数量不足：期望 " + expectedCount + " 张，实际 " + urls.size() + " 张");
        }
        return urls;
    }

    private AiImageGenerationResult callImageGeneration(AiProductImageTask task, JSONObject imagePrompt, int imageCount)
    {
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage(NODE_GENERATING);
        request.setPositivePrompt(StringUtils.defaultIfBlank(imagePrompt.getString("positivePrompt"),
                imagePrompt.getString("baseImagePrompt")));
        request.setNegativePrompt(imagePrompt.getString("negativePrompt"));
        request.setReferenceImageUrls(sourceImageSignedUrls(task));
        request.setAspectRatio(asset(task).getAspectRatio());
        request.setQuality(asset(task).getQuality());
        request.setImageCount(imageCount);
        request.setSequential(false);
        request.setProvider(modelProvider());
        request.setModel(selectedModelForTask(task));
        ensureNotCancelled(task.getId());
        return imageGenerationService.generate(request);
    }

    private void completeTask(AiProductImageTask task)
    {
        inTransaction(() -> {
            ensureNotCancelled(task.getId());
            Date now = new Date();
            task.setCurrentNode(NODE_COMPLETE);
            task.setStatus(STATUS_COMPLETED);
            task.setCompletedAt(now);
            task.setUpdatedAt(now);
            saveTaskIfStatus(task, STATUS_PROCESSING);
            AiProductAsset asset = asset(task);
            asset.setStatus(STATUS_COMPLETED);
            asset.setVisible(true);
            applyAnalyzedProductNameIfDefault(asset, task);
            asset.setUpdatedAt(now);
            updateAsset(asset);
        });
    }

    private void applyAnalyzedProductNameIfDefault(AiProductAsset asset, AiProductImageTask task)
    {
        if (asset == null || !StringUtils.defaultString(asset.getName()).startsWith(DEFAULT_PRODUCT_NAME_PREFIX))
        {
            return;
        }
        JSONObject analysis = parseJson(task.getAnalysisOutput());
        JSONObject sourceSummary = analysis.getJSONObject("sourceSummary");
        String productNameGuess = sourceSummary == null ? null : sourceSummary.getString("productNameGuess");
        if (StringUtils.isNotBlank(productNameGuess))
        {
            // Only untouched draft names are replaced; user-renamed assets stay authoritative.
            asset.setName(StringUtils.abbreviate(productNameGuess.trim(), 80));
        }
    }

    private void failTask(AiProductImageTask task, Exception e)
    {
        inTransaction(() -> failTaskInTransaction(task, e));
    }

    private void failTaskInTransaction(AiProductImageTask task, Exception e)
    {
        AiProductImageTask current = taskMapper.selectAiProductImageTaskById(task.getId());
        if (current != null && STATUS_CANCELLED.equals(current.getStatus()))
        {
            return;
        }
        // Failure logs keep retry/debug context even when the UI only stores a short failure reason.
        if (e instanceof ServiceException serviceException)
        {
            log.warn("商品图任务业务失败，taskId={}, node={}, reason={}, detail={}", task.getId(), task.getCurrentNode(),
                    e.getMessage(), serviceException.getDetailMessage());
        }
        else
        {
            log.error("商品图任务执行失败，taskId={}, node={}", task.getId(), task.getCurrentNode(), e);
        }
        task.setStatus(STATUS_FAILED);
        task.setFailureReason(StringUtils.abbreviate(StringUtils.defaultIfBlank(e.getMessage(), "商品图任务执行失败"), 500));
        task.setUpdatedAt(new Date());
        if (!trySaveTaskIfStatus(task, STATUS_PROCESSING))
        {
            return;
        }
        AiProductAsset asset = assetMapper.selectAiProductAssetById(task.getProductAssetId());
        if (asset != null)
        {
            asset.setStatus(STATUS_FAILED);
            asset.setVisible(false);
            asset.setUpdatedAt(new Date());
            updateAsset(asset);
        }
    }

    private String analyzeSourceImages(AiProductImageTask task)
    {
        AiVisionUnderstandingRequest request = new AiVisionUnderstandingRequest();
        request.setStage(NODE_ANALYSIS);
        request.setSystemPrompt("你是电商商品图视觉分析助手，只输出合法 JSON，不输出解释。");
        request.setPrompt(visionAnalysisPrompt(task));
        request.setImageUrls(sourceImageSignedUrls(task));
        request.setProvider(visionProvider());
        request.setModel(visionModelName());
        request.setJsonResponse(true);
        request.setHighResolution(visionHighResolutionEnabled());
        AiVisionUnderstandingResult result = visionUnderstandingService.analyze(request);
        task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
        task.setModelName(StringUtils.defaultIfBlank(result.getModel(), visionModelName()));
        return result.getContent();
    }

    private String visionAnalysisPrompt(AiProductImageTask task)
    {
        AiPromptTemplate template = promptTemplateMapper.selectPublishedTemplateByNameAndCategory(TEMPLATE_VISUAL_ANALYSIS,
                TEMPLATE_CATEGORY_VISUAL_ANALYSIS);
        if (template == null)
        {
            throw new ServiceException("未找到已发布的商品图视觉分析提示词模板");
        }
        AiProductAsset asset = asset(task);
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("userRequirement", StringUtils.defaultString(asset.getUserRequirement()));
        variables.put("targetPlatform", StringUtils.defaultString(asset.getTargetPlatform()));
        variables.put("productCategory", StringUtils.defaultString(asset.getAssetType()));
        variables.put("targetLanguage", targetLanguageForTask(task));
        variables.put("textEnabled", String.valueOf(textEnabledForTask(task)));
        variables.put("textPolicy", textPolicy(task));
        variables.put("sourceImageCount", String.valueOf(sourceImageCount(task)));
        return renderPrompt(template, variables);
    }

    private String generateText(AiProductImageTask task, String sourceImageAnalysis)
    {
        AiPromptTemplate template = promptTemplateMapper.selectPublishedTemplateByNameAndCategory(templateName(task),
                TEMPLATE_CATEGORY_IMAGE);
        if (template == null)
        {
            throw new ServiceException("未找到已发布的商品图提示词模板");
        }
        Map<String, String> variables = new LinkedHashMap<>();
        AiProductAsset asset = asset(task);
        variables.put("userRequirement", StringUtils.defaultString(asset.getUserRequirement()));
        variables.put("sourceImageAnalysis", StringUtils.defaultString(sourceImageAnalysis));
        variables.put("targetPlatform", StringUtils.defaultString(asset.getTargetPlatform()));
        variables.put("productCategory", StringUtils.defaultString(asset.getAssetType()));
        variables.put("modelName", selectedModelForTask(task));
        variables.put("selectedModel", selectedModelForTask(task));
        variables.put("aspectRatio", StringUtils.defaultString(asset.getAspectRatio()));
        variables.put("clarity", StringUtils.defaultString(asset.getQuality()));
        variables.put("imageCount", String.valueOf(imageCountForTask(task)));
        variables.put("targetLanguage", targetLanguageForTask(task));
        variables.put("textEnabled", String.valueOf(textEnabledForTask(task)));
        variables.put("textPolicy", textPolicy(task));
        variables.put("compositionPolicy", compositionPolicy(task));
        variables.put("platformRules", "Target platform guidance: " + StringUtils.defaultString(asset.getTargetPlatform()));
        variables.put("categoryRules", "Product category guidance: " + StringUtils.defaultString(asset.getAssetType()));
        variables.put("modelAdapterRules", "Model adapter: " + modelProvider() + "/" + selectedModelForTask(task));
        AiTextGenerationRequest request = new AiTextGenerationRequest();
        request.setStage(task.getCurrentNode());
        request.setPrompt(renderPrompt(template, variables));
        request.setProvider(modelProvider());
        request.setModel(selectedModelForTask(task));
        AiTextGenerationResult result = textGenerationService.generate(request);
        task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
        task.setModelName(StringUtils.defaultIfBlank(result.getModel(), selectedModelForTask(task)));
        return result.getContent();
    }

    private String renderPrompt(AiPromptTemplate template, Map<String, String> variables)
    {
        JSONObject content = parseJson(template.getContent());
        String prompt = content.getString("body");
        if (StringUtils.isBlank(prompt))
        {
            throw new ServiceException("提示词模板正文为空");
        }
        for (Map.Entry<String, String> entry : variables.entrySet())
        {
            prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return prompt;
    }

    private AiProductAsset validateCreateRequest(Long userId, AiProductImageTaskRequest request)
    {
        requireUser(userId);
        if (request == null || StringUtils.isBlank(request.getProductAssetId()))
        {
            throw new ServiceException("商品图任务参数不能为空");
        }
        imageCountForRequest(request);
        AiProductAsset asset = requireOwnedAsset(userId, request.getProductAssetId());
        if (!STATUS_DRAFT_UPLOADED.equals(asset.getStatus()))
        {
            throw new ServiceException("商品草稿状态不支持生成");
        }
        boolean hasSource = false;
        for (AiProductAssetImage image : imageMapper.selectImagesByAssetId(asset.getId(), userId))
        {
            if (ROLE_SOURCE.equals(image.getImageRole()))
            {
                hasSource = true;
                break;
            }
        }
        if (!hasSource)
        {
            throw new ServiceException("请先上传至少一张商品源图");
        }
        return asset;
    }

    private void applyRequestToAsset(AiProductAsset asset, AiProductImageTaskRequest request)
    {
        asset.setAssetType(normalizeAssetType(request.getAssetType()));
        asset.setTargetPlatform(StringUtils.trim(request.getTargetPlatform()));
        asset.setAspectRatio(StringUtils.trim(request.getAspectRatio()));
        asset.setQuality(StringUtils.trim(request.getQuality()));
        asset.setUserRequirement(StringUtils.trim(request.getUserRequirement()));
    }

    private JSONObject modelParams(AiProductImageTaskRequest request)
    {
        JSONObject params = new JSONObject(new LinkedHashMap<>());
        params.put("imageCount", imageCountForRequest(request));
        params.put("referenceImageMode", "all_uploaded_images");
        params.put("primaryReferenceImageIndex", 0);
        params.put("referenceSignedUrlExpireSeconds", 1800);
        params.put("model", selectedModelForRequest(request));
        params.put("selectedModel", selectedModelForRequest(request));
        params.put("targetLanguage", targetLanguageForRequest(request));
        params.put("textEnabled", textEnabledForRequest(request));
        params.put("aspectRatio", StringUtils.defaultString(request.getAspectRatio()));
        params.put("quality", StringUtils.defaultString(request.getQuality()));
        return params;
    }

    private String targetLanguageForRequest(AiProductImageTaskRequest request)
    {
        return StringUtils.defaultIfBlank(request.getTargetLanguage(), "zh-CN");
    }

    private boolean textEnabledForRequest(AiProductImageTaskRequest request)
    {
        return Boolean.TRUE.equals(request.getTextEnabled());
    }

    private String selectedModelForRequest(AiProductImageTaskRequest request)
    {
        return StringUtils.defaultIfBlank(request.getSelectedModel(), modelName());
    }

    private int imageCountForRequest(AiProductImageTaskRequest request)
    {
        return normalizeImageCount(normalizeAssetType(request.getAssetType()), request.getImageCount());
    }

    private int imageCountForTask(AiProductImageTask task)
    {
        try
        {
            JSONObject params = modelParamsForTask(task);
            Integer count = params == null ? null : params.getInteger("imageCount");
            return normalizeImageCount(normalizeAssetType(task.getAssetType()), count);
        }
        catch (JSONException e)
        {
            return normalizeImageCount(normalizeAssetType(task.getAssetType()), null);
        }
    }

    private String targetLanguageForTask(AiProductImageTask task)
    {
        return StringUtils.defaultIfBlank(modelParamsForTask(task).getString("targetLanguage"), "zh-CN");
    }

    private boolean textEnabledForTask(AiProductImageTask task)
    {
        return modelParamsForTask(task).getBooleanValue("textEnabled");
    }

    private String selectedModelForTask(AiProductImageTask task)
    {
        JSONObject params = modelParamsForTask(task);
        String configured = StringUtils.defaultIfBlank(params.getString("selectedModel"), params.getString("model"));
        return StringUtils.defaultIfBlank(configured, StringUtils.defaultIfBlank(task.getModelName(), modelName()));
    }

    private JSONObject modelParamsForTask(AiProductImageTask task)
    {
        try
        {
            JSONObject params = JSON.parseObject(task == null ? null : task.getModelParams());
            return params == null ? new JSONObject(new LinkedHashMap<>()) : params;
        }
        catch (JSONException e)
        {
            return new JSONObject(new LinkedHashMap<>());
        }
    }

    private String textPolicy(AiProductImageTask task)
    {
        if (textEnabledForTask(task))
        {
            return "allow concise in-image text in " + targetLanguageForTask(task)
                    + "; every planned text overlay must match the image style, lighting, material feeling, and composition.";
        }
        return "no in-image text; do not request titles, labels, watermarks, captions, fake characters, or gibberish text inside generated images.";
    }

    private String compositionPolicy(AiProductImageTask task)
    {
        if (textEnabledForTask(task))
        {
            return "textOverlay.enabled must be true only when meaningful copy is planned; include title, subtitle or sellingPoints and style-adaptive typography parameters.";
        }
        return "textOverlay.enabled must be false for every image; backend will skip text composition and return the generated base image as the final image.";
    }

    private int normalizeImageCount(String assetType, Integer requestedCount)
    {
        int count = requestedCount == null ? 1 : requestedCount;
        int max = ASSET_TYPE_DETAIL.equals(assetType) ? 8 : 4;
        if (count < 1 || count > max)
        {
            throw new ServiceException(ASSET_TYPE_DETAIL.equals(assetType)
                    ? "详情图生成数量必须在 1-8 张之间"
                    : "主图生成数量必须在 1-4 张之间");
        }
        return count;
    }

    private String normalizeAssetType(String assetType)
    {
        return ASSET_TYPE_DETAIL.equals(StringUtils.trim(assetType)) ? ASSET_TYPE_DETAIL : ASSET_TYPE_MAIN;
    }

    private String retryStartNode(AiProductImageTask task)
    {
        if (NODE_ANALYSIS.equals(task.getCurrentNode()))
        {
            return NODE_ANALYSIS;
        }
        if (NODE_PLANNING.equals(task.getCurrentNode()))
        {
            return NODE_PLANNING;
        }
        if (NODE_GENERATING.equals(task.getCurrentNode()))
        {
            return NODE_GENERATING;
        }
        throw new ServiceException("当前失败节点不支持重试");
    }

    private void clearFromNode(AiProductImageTask task, String startNode)
    {
        if (NODE_ANALYSIS.equals(startNode))
        {
            task.setAnalysisOutput("{}");
        }
        if (NODE_ANALYSIS.equals(startNode) || NODE_PLANNING.equals(startNode))
        {
            task.setPlanningOutput("{}");
        }
        task.setGenerationOutput("{}");
        task.setDashscopeTaskId(null);
    }

    private void beginStage(AiProductImageTask task, String node)
    {
        task.setCurrentNode(node);
        task.setStatus(STATUS_PROCESSING);
        task.setFailureReason(null);
        saveTaskIfStatus(task, STATUS_PROCESSING);
    }

    private void ensureNotCancelled(String taskId)
    {
        AiProductImageTask task = taskMapper.selectAiProductImageTaskById(taskId);
        if (task == null || !STATUS_PROCESSING.equals(task.getStatus()))
        {
            throw new TaskCancelledException();
        }
    }

    private AiProductAsset asset(AiProductImageTask task)
    {
        AiProductAsset asset = assetMapper.selectAiProductAssetById(task.getProductAssetId());
        if (asset == null)
        {
            throw new ServiceException("商品不存在");
        }
        return asset;
    }

    private AiProductAsset requireOwnedAsset(Long userId, String assetId)
    {
        AiProductAsset asset = assetMapper.selectAiProductAssetById(assetId);
        if (asset == null || !userId.equals(asset.getUserId()))
        {
            throw new ServiceException("商品不存在");
        }
        return asset;
    }

    private AiProductImageTask requireOwnedTask(String taskId, Long userId)
    {
        requireUser(userId);
        AiProductImageTask task = taskMapper.selectAiProductImageTaskById(taskId);
        if (task == null || !userId.equals(task.getUserId()))
        {
            throw new ServiceException("商品图任务不存在");
        }
        return task;
    }

    private void requireUser(Long userId)
    {
        if (userId == null || userId <= 0)
        {
            throw new ServiceException("用户未登录");
        }
    }

    private List<JSONObject> generationPrompts(AiProductImageTask task, JSONObject planning, int requestedCount)
    {
        List<JSONObject> prompts = normalizedImagePrompts(planning);
        if (prompts.isEmpty())
        {
            throw new ServiceException("Product image planning output is missing a valid imagePrompt");
        }
        if (!ASSET_TYPE_DETAIL.equals(task.getAssetType()))
        {
            return ensureMainPromptCount(planning, requestedCount);
        }
        return ensureDetailPromptCount(task, planning, prompts, requestedCount);
    }

    private List<JSONObject> ensureMainPromptCount(JSONObject planning, int requestedCount)
    {
        List<JSONObject> prompts = distinctMainPrompts(planning, requestedCount);
        if (prompts.size() < requestedCount)
        {
            throw new ServiceException(MAIN_PLANNING_INSUFFICIENT_MESSAGE);
        }
        return prompts;
    }

    private boolean hasEnoughDistinctMainPrompts(JSONObject planning, int requestedCount)
    {
        return distinctMainPrompts(planning, requestedCount).size() >= requestedCount;
    }

    private List<JSONObject> distinctMainPrompts(JSONObject planning, int requestedCount)
    {
        List<JSONObject> result = new ArrayList<>();
        List<String> signatures = new ArrayList<>();
        for (JSONObject prompt : normalizedImagePrompts(planning))
        {
            String signature = mainPromptSignature(prompt);
            if (StringUtils.isBlank(signature) || signatures.contains(signature))
            {
                continue;
            }
            signatures.add(signature);
            result.add(prompt);
            if (result.size() >= requestedCount)
            {
                break;
            }
        }
        return result;
    }

    private String mainPromptSignature(JSONObject prompt)
    {
        JSONObject overlay = prompt.getJSONObject("textOverlay");
        return String.join("|",
                StringUtils.defaultString(prompt.getString("positivePrompt")),
                StringUtils.defaultString(prompt.getString("baseImagePrompt")),
                StringUtils.defaultString(prompt.getString("designTarget")),
                StringUtils.defaultString(prompt.getString("title")),
                overlay == null ? "" : overlay.toJSONString()).trim();
    }

    private List<JSONObject> enforceTextPolicy(List<JSONObject> prompts, boolean textEnabled)
    {
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject prompt : prompts)
        {
            JSONObject next = new JSONObject(new LinkedHashMap<>(prompt));
            JSONObject overlay = next.getJSONObject("textOverlay");
            if (overlay == null)
            {
                overlay = new JSONObject(new LinkedHashMap<>());
            }
            overlay.put("enabled", textEnabled);
            if (!textEnabled)
            {
                // The user-facing "no text" mode must survive model planning and plan-editor edits.
                overlay.put("title", "");
                overlay.put("subtitle", "");
                overlay.put("sellingPoints", new JSONArray());
                next.put("negativePrompt", appendPromptInstruction(next.getString("negativePrompt"),
                        "no in-image text, no labels, no captions, no watermark, no random characters"));
            }
            next.put("textOverlay", overlay);
            result.add(next);
        }
        return result;
    }

    private String appendPromptInstruction(String prompt, String instruction)
    {
        if (StringUtils.isBlank(prompt))
        {
            return instruction;
        }
        if (prompt.contains(instruction))
        {
            return prompt;
        }
        return prompt + "; " + instruction;
    }

    private List<JSONObject> ensureDetailPromptCount(AiProductImageTask task, JSONObject planning,
            List<JSONObject> prompts, int requestedCount)
    {
        List<JSONObject> result = new ArrayList<>(prompts);
        for (int round = 0; round < 3 && result.size() < requestedCount; round++)
        {
            JSONObject supplement = parseJson(generateText(task,
                    supplementalPromptInstruction(planning, result, requestedCount - result.size())));
            result.addAll(normalizedImagePrompts(supplement));
        }
        while (result.size() < requestedCount)
        {
            result.add(fallbackDetailPrompt(result.size()));
        }
        return result.subList(0, requestedCount);
    }

    private String supplementalPromptInstruction(JSONObject planning, List<JSONObject> prompts, int missingCount)
    {
        JSONObject input = new JSONObject(new LinkedHashMap<>());
        input.put("instruction", "Complete missing detail image prompts. Output only missing imagePrompts and avoid duplicates.");
        input.put("missingCount", missingCount);
        input.put("existingPlanning", planning);
        input.put("existingPromptCount", prompts.size());
        return input.toJSONString();
    }

    private JSONObject fallbackDetailPrompt(int index)
    {
        String section = DETAIL_FALLBACK_SECTIONS[index % DETAIL_FALLBACK_SECTIONS.length];
        JSONObject prompt = new JSONObject(new LinkedHashMap<>());
        prompt.put("promptId", "fallback_detail_" + (index + 1));
        prompt.put("sectionName", section);
        prompt.put("purpose", section);
        prompt.put("positivePrompt", "Create a " + section
                + " e-commerce detail image. Strictly preserve the exact product subject, color, structure, logo, text, material, shape and quantity from the first reference image. Change only the presentation, composition, scene or annotation style.");
        prompt.put("negativePrompt", "do not alter product identity, color, structure, logo, text, material, shape or quantity; no watermark; no fake claims");
        return prompt;
    }

    private List<JSONObject> normalizedImagePrompts(JSONObject planning)
    {
        List<JSONObject> prompts = new ArrayList<>();
        collectValidImagePrompts(planning.getJSONArray("imagePrompts"), prompts);
        collectValidImagePrompts(planning.getJSONArray("imagePlans"), prompts);
        JSONObject imagePrompt = planning.getJSONObject("imagePrompt");
        if (prompts.isEmpty() && isValidImagePrompt(imagePrompt))
        {
            prompts.add(normalizePromptText(imagePrompt));
        }
        return prompts;
    }

    private void collectValidImagePrompts(JSONArray source, List<JSONObject> prompts)
    {
        if (source == null)
        {
            return;
        }
        for (int index = 0; index < source.size(); index++)
        {
            Object item = source.get(index);
            if (item instanceof JSONObject prompt && isValidImagePrompt(prompt))
            {
                prompts.add(normalizePromptText(prompt));
            }
        }
    }

    private boolean isValidImagePrompt(JSONObject prompt)
    {
        return prompt != null && (StringUtils.isNotBlank(prompt.getString("positivePrompt"))
                || StringUtils.isNotBlank(prompt.getString("baseImagePrompt")));
    }

    private JSONObject normalizePromptText(JSONObject prompt)
    {
        if (StringUtils.isBlank(prompt.getString("positivePrompt")))
        {
            prompt.put("positivePrompt", prompt.getString("baseImagePrompt"));
        }
        return prompt;
    }

    private List<String> sourceImageUrls(AiProductImageTask task)
    {
        List<String> urls = new ArrayList<>();
        for (AiProductAssetImage image : imageMapper.selectImagesByAssetId(task.getProductAssetId(), task.getUserId()))
        {
            if (ROLE_SOURCE.equals(image.getImageRole()) && StringUtils.isNotBlank(image.getOssUrl()))
            {
                urls.add(image.getOssUrl());
            }
        }
        return urls;
    }

    private int sourceImageCount(AiProductImageTask task)
    {
        int count = 0;
        for (AiProductAssetImage image : imageMapper.selectImagesByAssetId(task.getProductAssetId(), task.getUserId()))
        {
            if (ROLE_SOURCE.equals(image.getImageRole()))
            {
                count++;
            }
        }
        return count;
    }

    private String sourceImageSummary(AiProductImageTask task)
    {
        return String.join(",", sourceImageUrls(task));
    }

    private List<String> sourceImageSignedUrls(AiProductImageTask task)
    {
        List<String> urls = new ArrayList<>();
        for (AiProductAssetImage image : imageMapper.selectImagesByAssetId(task.getProductAssetId(), task.getUserId()))
        {
            if (!ROLE_SOURCE.equals(image.getImageRole()))
            {
                continue;
            }
            if (StringUtils.isNotBlank(image.getOssObjectKey()))
            {
                urls.add(objectStorageService.presignObjectUrl(image.getOssObjectKey(),
                        REFERENCE_SIGNED_URL_EXPIRE_SECONDS));
            }
            else if (StringUtils.isNotBlank(image.getOssUrl()))
            {
                urls.add(image.getOssUrl());
            }
        }
        return urls;
    }

    private AiProductAssetImage generatedImage(AiProductImageTask task, AiStoredObject stored, int sortOrder,
            String imageRole)
    {
        AiProductAssetImage image = new AiProductAssetImage();
        Date now = new Date();
        image.setId(UUID.randomUUID().toString());
        image.setProductAssetId(task.getProductAssetId());
        image.setUserId(task.getUserId());
        image.setTaskId(task.getId());
        image.setImageRole(imageRole);
        image.setOssBucket(stored.getBucket());
        image.setOssObjectKey(stored.getObjectKey());
        image.setOssUrl(stored.getUrl());
        image.setMimeType(stored.getMimeType());
        image.setFileSize(stored.getSize());
        image.setSortOrder(sortOrder);
        image.setCreatedAt(now);
        image.setUpdatedAt(now);
        return image;
    }

    private JSONObject generationImageOutput(AiProductAssetImage baseImage, AiProductAssetImage finalImage,
            AiStoredObject baseStored, AiStoredObject finalStored, String temporaryUrl, JSONObject prompt,
            AiProductImageCompositionResult composition, int index)
    {
        JSONObject output = new JSONObject(new LinkedHashMap<>());
        output.put("imageId", finalImage.getId());
        output.put("baseImageId", baseImage.getId());
        output.put("finalImageId", finalImage.getId());
        output.put("promptId", StringUtils.defaultIfBlank(prompt.getString("promptId"), "image_" + index));
        output.put("index", index);
        output.put("baseOssObjectKey", baseStored.getObjectKey());
        output.put("finalOssObjectKey", finalStored.getObjectKey());
        output.put("baseOssUrl", baseStored.getUrl());
        output.put("finalOssUrl", finalStored.getUrl());
        output.put("finalMimeType", finalStored.getMimeType());
        output.put("finalFileSize", finalStored.getSize());
        output.put("ossUrl", finalStored.getUrl());
        output.put("ossObjectKey", finalStored.getObjectKey());
        output.put("temporaryUrl", temporaryUrl);
        JSONObject compositionOutput = new JSONObject(new LinkedHashMap<>());
        compositionOutput.put("applied", composition != null && composition.isApplied());
        compositionOutput.put("metadata", composition == null ? null : composition.getMetadata());
        output.put("composition", compositionOutput);
        return output;
    }

    private List<String> generatedUrls(AiImageGenerationResult result)
    {
        if (result == null || result.getImageUrls() == null)
        {
            throw new ServiceException("图片模型未返回生成图片");
        }
        List<String> urls = result.getImageUrls().stream()
                .filter(StringUtils::isNotBlank)
                .toList();
        if (urls.isEmpty())
        {
            throw new ServiceException("图片模型未返回生成图片");
        }
        return urls;
    }

    private String firstGeneratedUrl(AiImageGenerationResult result)
    {
        if (result == null || result.getImageUrls() == null || result.getImageUrls().isEmpty()
                || StringUtils.isBlank(result.getImageUrls().get(0)))
        {
            throw new ServiceException("图片模型未返回生成图片");
        }
        return result.getImageUrls().get(0);
    }

    private String validJson(String content)
    {
        return parseJson(content).toJSONString();
    }

    private String validPlanningJson(String content, boolean textEnabled)
    {
        return validPlanningObject(content, textEnabled).toJSONString();
    }

    private String validPlanningJson(String content, boolean textEnabled, int requestedCount, boolean requireMainCount)
    {
        JSONObject planning = validPlanningObject(content, textEnabled);
        if (requireMainCount && !hasEnoughDistinctMainPrompts(planning, requestedCount))
        {
            throw new ServiceException(MAIN_PLANNING_INSUFFICIENT_MESSAGE);
        }
        return planning.toJSONString();
    }

    private JSONObject validPlanningObject(String content, boolean textEnabled)
    {
        return sanitizePlanningText(applyTextPolicyToPlanning(validatedPlanning(content), textEnabled));
    }

    /** Planning must yield usable prompts before the workflow can enter the image-model stage. */
    private JSONObject validatedPlanning(String content)
    {
        JSONObject planning = parseJson(content);
        JSONObject imagePrompt = normalizedImagePrompt(planning);
        if (imagePrompt == null || (StringUtils.isBlank(imagePrompt.getString("positivePrompt"))
                && StringUtils.isBlank(imagePrompt.getString("baseImagePrompt"))))
        {
            throw new ServiceException("Product image planning output is missing a valid imagePrompt");
        }
        if (StringUtils.isBlank(imagePrompt.getString("positivePrompt")))
        {
            imagePrompt.put("positivePrompt", imagePrompt.getString("baseImagePrompt"));
        }
        planning.put("imagePrompt", imagePrompt);
        return planning;
    }

    private JSONObject applyTextPolicyToPlanning(JSONObject planning, boolean textEnabled)
    {
        List<JSONObject> prompts = enforceTextPolicy(normalizedImagePrompts(planning), textEnabled);
        if (!prompts.isEmpty())
        {
            planning.put("imagePrompt", prompts.get(0));
            JSONArray promptArray = new JSONArray();
            promptArray.addAll(prompts);
            planning.put("imagePrompts", promptArray);
        }
        return planning;
    }

    private JSONObject sanitizePlanningText(JSONObject planning)
    {
        sanitizeJsonObject(planning);
        return planning;
    }

    private void sanitizeJsonObject(JSONObject object)
    {
        for (Map.Entry<String, Object> entry : object.entrySet())
        {
            entry.setValue(sanitizeJsonValue(entry.getValue()));
        }
    }

    private Object sanitizeJsonValue(Object value)
    {
        if (value instanceof String text)
        {
            return cleanGeneratedText(text);
        }
        if (value instanceof JSONObject object)
        {
            sanitizeJsonObject(object);
            return object;
        }
        if (value instanceof JSONArray array)
        {
            for (int index = 0; index < array.size(); index++)
            {
                array.set(index, sanitizeJsonValue(array.get(index)));
            }
            return array;
        }
        return value;
    }

    private String cleanGeneratedText(String value)
    {
        return value
                .replaceAll("([、，。；：！？,.!?])\\1+", "$1")
                .replaceAll("(?m)^\\s*[、，。；：！？,.!?]+\\s*$", "")
                .replaceAll("([\\p{L}\\p{N}])、\\s*$", "$1")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private JSONObject normalizedImagePrompt(JSONObject planning)
    {
        // Some detail prompt templates keep the legacy compatibility field as `{}` and place real plans in arrays.
        List<JSONObject> prompts = normalizedImagePrompts(planning);
        if (!prompts.isEmpty())
        {
            return prompts.get(0);
        }
        JSONObject imagePrompt = planning.getJSONObject("imagePrompt");
        if (isValidImagePrompt(imagePrompt))
        {
            return normalizePromptText(imagePrompt);
        }
        return null;
    }

    private JSONObject parseJson(String content)
    {
        try
        {
            JSONObject json = JSON.parseObject(content);
            if (json == null)
            {
                throw new JSONException("empty json");
            }
            return json;
        }
        catch (JSONException e)
        {
            throw new ServiceException("商品图规划输出不是合法JSON");
        }
    }

    private void saveTaskIfStatus(AiProductImageTask task, String expectedStatus)
    {
        task.setUpdatedAt(new Date());
        if (!trySaveTaskIfStatus(task, expectedStatus))
        {
            throw new TaskCancelledException();
        }
    }

    private boolean trySaveTaskIfStatus(AiProductImageTask task, String expectedStatus)
    {
        normalizeJsonFields(task);
        return taskMapper.updateAiProductImageTaskIfStatus(task, expectedStatus) == 1;
    }

    /** JSONB workflow columns are non-null by schema and must retain valid empty values on retry. */
    private void normalizeJsonFields(AiProductImageTask task)
    {
        task.setAnalysisOutput(StringUtils.defaultIfBlank(task.getAnalysisOutput(), "{}"));
        task.setPlanningOutput(StringUtils.defaultIfBlank(task.getPlanningOutput(), "{}"));
        task.setGenerationOutput(StringUtils.defaultIfBlank(task.getGenerationOutput(), "{}"));
        task.setDebugPrompts(StringUtils.defaultIfBlank(task.getDebugPrompts(), "{}"));
        task.setModelParams(StringUtils.defaultIfBlank(task.getModelParams(), "{}"));
    }

    private void updateAsset(AiProductAsset asset)
    {
        if (assetMapper.updateAiProductAsset(asset) != 1)
        {
            throw new ServiceException("Product asset state update failed");
        }
    }

    private void inTransaction(Runnable action)
    {
        if (transactionTemplate == null)
        {
            action.run();
            return;
        }
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private String templateName(AiProductImageTask task)
    {
        return "detail".equals(task.getAssetType()) ? TEMPLATE_DETAIL : TEMPLATE_MAIN;
    }

    private String modelProvider()
    {
        return environment.getProperty("egg.ai.model-gateway.product-image.provider", "dashscope");
    }

    private String modelName()
    {
        return environment.getProperty("egg.ai.model-gateway.product-image.model",
                environment.getProperty("egg.ai.model-gateway.image.model", "wan2.7-image-pro"));
    }

    private String visionProvider()
    {
        return environment.getProperty("egg.ai.model-gateway.vision.provider", "dashscope");
    }

    private String visionModelName()
    {
        return environment.getProperty("egg.ai.model-gateway.vision.model", "qwen3.7-plus");
    }

    private boolean visionHighResolutionEnabled()
    {
        return environment.getProperty("egg.ai.model-gateway.vision.high-resolution", Boolean.class, Boolean.TRUE);
    }

    private String fileExtension(String url)
    {
        int dot = url.lastIndexOf('.');
        if (dot < 0 || dot == url.length() - 1 || url.substring(dot + 1).contains("/"))
        {
            return "png";
        }
        return url.substring(dot + 1).split("[?#]", 2)[0];
    }

    protected byte[] downloadGeneratedImage(String url)
    {
        if (StringUtils.isBlank(url))
        {
            throw new ServiceException("下载生成图片失败：模型未返回图片地址");
        }

        Exception lastFailure = null;
        for (int attempt = 1; attempt <= GENERATED_IMAGE_DOWNLOAD_MAX_ATTEMPTS; attempt++)
        {
            try
            {
                return downloadGeneratedImageOnce(url);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw downloadGeneratedImageException(url, "调用被中断", e);
            }
            catch (Exception e)
            {
                lastFailure = e;
                if (attempt < GENERATED_IMAGE_DOWNLOAD_MAX_ATTEMPTS)
                {
                    sleepBeforeGeneratedImageDownloadRetry(attempt, url);
                }
            }
        }
        throw downloadGeneratedImageException(url, downloadFailureMessage(url, lastFailure), lastFailure);
    }

    private byte[] downloadGeneratedImageOnce(String url) throws Exception
    {
        URI uri = new URI(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(generatedImageDownloadTimeoutSeconds()))
                .header("User-Agent", "EggCommerceAI/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = GENERATED_IMAGE_DOWNLOAD_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300)
        {
            throw new IOException("HTTP " + statusCode);
        }
        byte[] body = response.body();
        if (body == null || body.length == 0)
        {
            throw new IOException("响应内容为空");
        }
        return body;
    }

    private int generatedImageDownloadTimeoutSeconds()
    {
        return environment.getProperty("egg.ai.product-image.generated-download-timeout-seconds", Integer.class,
                GENERATED_IMAGE_DOWNLOAD_TIMEOUT_SECONDS);
    }

    private void sleepBeforeGeneratedImageDownloadRetry(int attempt, String url)
    {
        try
        {
            Thread.sleep(GENERATED_IMAGE_DOWNLOAD_RETRY_DELAY_MILLIS * attempt);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw downloadGeneratedImageException(url, "调用被中断", e);
        }
    }

    private ServiceException downloadGeneratedImageException(String url, String reason, Exception cause)
    {
        // DashScope returns short-lived OSS URLs; keep only host/status in user-visible state and put no signed query in DB.
        String message = "下载生成图片失败：" + reason;
        ServiceException exception = new ServiceException(StringUtils.abbreviate(message, 500));
        exception.setDetailMessage("source=" + generatedImageSource(url) + ", cause=" + downloadFailureCause(cause));
        return exception;
    }

    private String downloadFailureMessage(String url, Exception cause)
    {
        return generatedImageSource(url) + " - " + downloadFailureCause(cause);
    }

    private String downloadFailureCause(Exception cause)
    {
        if (cause == null)
        {
            return "未知错误";
        }
        if (cause instanceof URISyntaxException || cause instanceof IllegalArgumentException)
        {
            return "图片地址格式不正确";
        }
        return StringUtils.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName());
    }

    private String generatedImageSource(String url)
    {
        try
        {
            return StringUtils.defaultIfBlank(new URI(url).getHost(), "未知图片源");
        }
        catch (Exception ignored)
        {
            return "图片地址格式不正确";
        }
    }

    private static class TaskCancelledException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
    }
}
