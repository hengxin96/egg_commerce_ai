package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
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
import com.egg.commerce.system.service.ai.product.AiProductImageTaskService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/** Coordinates the persisted product-image generation workflow. */
@Service
public class AiProductImageTaskServiceImpl implements AiProductImageTaskService
{
    private static final String TASK_TYPE_PRODUCT_IMAGE = "product_image";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String NODE_INPUT = "输入";
    private static final String NODE_ANALYSIS = "分析中";
    private static final String NODE_PLANNING = "确认规划";
    private static final String NODE_GENERATING = "生成中";
    private static final String NODE_COMPLETE = "完成";
    private static final String TEMPLATE_CATEGORY_IMAGE = "AI生图";
    private static final String TEMPLATE_CATEGORY_REWRITE = "AI优化提示词";
    private static final String TEMPLATE_MAIN = "主图系统提示词";
    private static final String TEMPLATE_DETAIL = "详情图系统提示词";
    private static final String STATUS_DRAFT_UPLOADED = "draft_uploaded";
    private static final String ROLE_SOURCE = "source";
    private static final String ROLE_GENERATED = "generated";

    private final AiProductImageTaskMapper taskMapper;
    private final AiProductAssetMapper assetMapper;
    private final AiProductAssetImageMapper imageMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiObjectStorageService objectStorageService;
    private final AiTextGenerationService textGenerationService;
    private final AiImageGenerationService imageGenerationService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final Environment environment;

    public AiProductImageTaskServiceImpl(AiProductImageTaskMapper taskMapper, AiProductAssetMapper assetMapper,
            AiProductAssetImageMapper imageMapper, AiPromptTemplateMapper promptTemplateMapper,
            AiObjectStorageService objectStorageService, AiTextGenerationService textGenerationService,
            AiImageGenerationService imageGenerationService, ThreadPoolTaskExecutor taskExecutor,
            Environment environment)
    {
        this.taskMapper = taskMapper;
        this.assetMapper = assetMapper;
        this.imageMapper = imageMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.objectStorageService = objectStorageService;
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.taskExecutor = taskExecutor;
        this.environment = environment;
    }

    @Override
    public AiProductImageTask createTask(Long userId, AiProductImageTaskRequest request)
    {
        AiProductAsset asset = validateCreateRequest(userId, request);
        Date now = new Date();
        AiProductImageTask task = new AiProductImageTask();
        task.setId(UUID.randomUUID().toString());
        task.setProductAssetId(asset.getId());
        task.setUserId(userId);
        task.setTaskType(TASK_TYPE_PRODUCT_IMAGE);
        task.setAssetType(StringUtils.trim(request.getAssetType()));
        task.setTargetPlatform(StringUtils.trim(request.getTargetPlatform()));
        task.setCurrentNode(NODE_INPUT);
        task.setStatus(STATUS_PROCESSING);
        task.setDebugPrompts("{}");
        task.setModelProvider(modelProvider());
        task.setModelName(modelName());
        task.setModelParams("{}");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        applyRequestToAsset(asset, request);
        asset.setStatus(STATUS_PROCESSING);
        asset.setVisible(false);
        asset.setLastTaskId(task.getId());
        asset.setUpdatedAt(now);
        assetMapper.updateAiProductAsset(asset);
        taskMapper.insertAiProductImageTask(task);
        taskExecutor.submit(() -> executeTask(task.getId()));
        return task;
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
        saveTask(task);

        AiProductAsset asset = requireOwnedAsset(userId, task.getProductAssetId());
        asset.setStatus(STATUS_PROCESSING);
        asset.setVisible(false);
        asset.setUpdatedAt(new Date());
        assetMapper.updateAiProductAsset(asset);
        taskExecutor.submit(() -> executeTaskFrom(task.getId(), startNode));
        return task;
    }

    @Override
    public AiProductImageTask cancelTask(String taskId, Long userId)
    {
        AiProductImageTask task = requireOwnedTask(taskId, userId);
        if (!STATUS_PROCESSING.equals(task.getStatus()))
        {
            throw new ServiceException("只有生成中的任务可以终止");
        }
        Date now = new Date();
        task.setStatus(STATUS_CANCELLED);
        task.setCancelReason("用户已终止任务");
        task.setCancelledAt(now);
        task.setUpdatedAt(now);
        taskMapper.updateAiProductImageTask(task);

        AiProductAsset asset = requireOwnedAsset(userId, task.getProductAssetId());
        asset.setStatus(STATUS_CANCELLED);
        asset.setVisible(false);
        asset.setUpdatedAt(now);
        assetMapper.updateAiProductAsset(asset);
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
            saveTask(task);
            if (NODE_ANALYSIS.equals(startNode))
            {
                runAnalysisStage(task);
            }
            if (NODE_ANALYSIS.equals(startNode) || NODE_PLANNING.equals(startNode))
            {
                runPlanningStage(task);
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
        String output = generateText(task, sourceImageSummary(task));
        task.setAnalysisOutput(validJson(output));
        saveTask(task);
        ensureNotCancelled(task.getId());
    }

    private void runPlanningStage(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        beginStage(task, NODE_PLANNING);
        String output = generateText(task, task.getAnalysisOutput());
        task.setPlanningOutput(validJson(output));
        saveTask(task);
        ensureNotCancelled(task.getId());
    }

    private void runGenerationStage(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        beginStage(task, NODE_GENERATING);
        JSONObject planning = parseJson(task.getPlanningOutput());
        AiImageGenerationRequest request = new AiImageGenerationRequest();
        request.setStage(NODE_GENERATING);
        request.setPositivePrompt(planning.getString("positivePrompt"));
        request.setNegativePrompt(planning.getString("negativePrompt"));
        request.setReferenceImageUrls(sourceImageUrls(task));
        request.setAspectRatio(asset(task).getAspectRatio());
        request.setQuality(asset(task).getQuality());
        request.setProvider(modelProvider());
        request.setModel(modelName());
        AiImageGenerationResult result = imageGenerationService.generate(request);
        ensureNotCancelled(task.getId());
        String temporaryUrl = firstGeneratedUrl(result);
        AiStoredObject stored = objectStorageService.uploadGeneratedImage(downloadGeneratedImage(temporaryUrl),
                fileExtension(temporaryUrl), task.getUserId());
        AiProductAssetImage generated = generatedImage(task, stored);
        imageMapper.insertAiProductAssetImage(generated);
        task.setDashscopeTaskId(result.getTaskId());
        task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
        task.setModelName(StringUtils.defaultIfBlank(result.getModel(), modelName()));
        JSONObject output = new JSONObject(new LinkedHashMap<>());
        output.put("temporaryUrl", temporaryUrl);
        output.put("ossUrl", stored.getUrl());
        output.put("ossObjectKey", stored.getObjectKey());
        task.setGenerationOutput(output.toJSONString());
        saveTask(task);
        ensureNotCancelled(task.getId());
    }

    private void completeTask(AiProductImageTask task)
    {
        ensureNotCancelled(task.getId());
        Date now = new Date();
        task.setCurrentNode(NODE_COMPLETE);
        task.setStatus(STATUS_COMPLETED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        taskMapper.updateAiProductImageTask(task);
        AiProductAsset asset = asset(task);
        asset.setStatus(STATUS_COMPLETED);
        asset.setVisible(true);
        asset.setUpdatedAt(now);
        assetMapper.updateAiProductAsset(asset);
    }

    private void failTask(AiProductImageTask task, Exception e)
    {
        AiProductImageTask current = taskMapper.selectAiProductImageTaskById(task.getId());
        if (current != null && STATUS_CANCELLED.equals(current.getStatus()))
        {
            return;
        }
        task.setStatus(STATUS_FAILED);
        task.setFailureReason(StringUtils.abbreviate(StringUtils.defaultIfBlank(e.getMessage(), "商品图任务执行失败"), 500));
        task.setUpdatedAt(new Date());
        taskMapper.updateAiProductImageTask(task);
        AiProductAsset asset = assetMapper.selectAiProductAssetById(task.getProductAssetId());
        if (asset != null)
        {
            asset.setStatus(STATUS_FAILED);
            asset.setVisible(false);
            asset.setUpdatedAt(new Date());
            assetMapper.updateAiProductAsset(asset);
        }
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
        variables.put("platformRules", StringUtils.defaultString(asset.getTargetPlatform()));
        variables.put("categoryRules", StringUtils.defaultString(asset.getAssetType()));
        variables.put("modelAdapterRules", modelProvider() + "/" + modelName());
        AiTextGenerationRequest request = new AiTextGenerationRequest();
        request.setStage(task.getCurrentNode());
        request.setPrompt(renderPrompt(template, variables));
        request.setProvider(modelProvider());
        request.setModel(modelName());
        AiTextGenerationResult result = textGenerationService.generate(request);
        task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
        task.setModelName(StringUtils.defaultIfBlank(result.getModel(), modelName()));
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
        asset.setAssetType(StringUtils.trim(request.getAssetType()));
        asset.setTargetPlatform(StringUtils.trim(request.getTargetPlatform()));
        asset.setAspectRatio(StringUtils.trim(request.getAspectRatio()));
        asset.setQuality(StringUtils.trim(request.getQuality()));
        asset.setUserRequirement(StringUtils.trim(request.getUserRequirement()));
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
            task.setAnalysisOutput(null);
        }
        if (NODE_ANALYSIS.equals(startNode) || NODE_PLANNING.equals(startNode))
        {
            task.setPlanningOutput(null);
        }
        task.setGenerationOutput(null);
        task.setDashscopeTaskId(null);
    }

    private void beginStage(AiProductImageTask task, String node)
    {
        task.setCurrentNode(node);
        task.setStatus(STATUS_PROCESSING);
        task.setFailureReason(null);
        saveTask(task);
    }

    private void ensureNotCancelled(String taskId)
    {
        AiProductImageTask task = taskMapper.selectAiProductImageTaskById(taskId);
        if (task == null || STATUS_CANCELLED.equals(task.getStatus()))
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

    private String sourceImageSummary(AiProductImageTask task)
    {
        return String.join(",", sourceImageUrls(task));
    }

    private AiProductAssetImage generatedImage(AiProductImageTask task, AiStoredObject stored)
    {
        AiProductAssetImage image = new AiProductAssetImage();
        Date now = new Date();
        image.setId(UUID.randomUUID().toString());
        image.setProductAssetId(task.getProductAssetId());
        image.setUserId(task.getUserId());
        image.setTaskId(task.getId());
        image.setImageRole(ROLE_GENERATED);
        image.setOssBucket(stored.getBucket());
        image.setOssObjectKey(stored.getObjectKey());
        image.setOssUrl(stored.getUrl());
        image.setMimeType(stored.getMimeType());
        image.setFileSize(stored.getSize());
        image.setSortOrder(0);
        image.setCreatedAt(now);
        image.setUpdatedAt(now);
        return image;
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

    private void saveTask(AiProductImageTask task)
    {
        task.setUpdatedAt(new Date());
        taskMapper.updateAiProductImageTask(task);
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
        return environment.getProperty("egg.ai.model-gateway.product-image.model", "wanx2.1-t2i-plus");
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
        try (InputStream input = new URL(url).openStream(); ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            input.transferTo(output);
            return output.toByteArray();
        }
        catch (Exception e)
        {
            throw new ServiceException("下载生成图片失败");
        }
    }

    private static class TaskCancelledException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
    }
}
