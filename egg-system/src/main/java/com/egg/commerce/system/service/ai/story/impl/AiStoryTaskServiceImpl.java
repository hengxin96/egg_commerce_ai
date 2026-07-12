package com.egg.commerce.system.service.ai.story.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.domain.ai.story.AiStoryPlatformRule;
import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskDetail;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskRequest;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.mapper.ai.story.AiStoryPlatformRuleMapper;
import com.egg.commerce.system.mapper.ai.story.AiStoryTaskMapper;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import com.egg.commerce.system.service.ai.story.AiStoryTaskService;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 剧情生成任务服务实现。
 *
 * <p>服务把用户输入、提示词模板、平台规则和文本模型网关串起来，并且保证五个生成节点严格串行：
 * 上一节点成功落库后，下一节点才会使用上一节点输出继续生成。</p>
 */
@Service
public class AiStoryTaskServiceImpl implements AiStoryTaskService
{
    private static final Logger log = LoggerFactory.getLogger(AiStoryTaskServiceImpl.class);

    /** 任务类型固定值，方便任务中心未来复用同一列表入口。 */
    private static final String TASK_TYPE_STORY = "story";

    /** 任务执行中状态。 */
    private static final String STATUS_PROCESSING = "processing";

    /** 任务完成状态。 */
    private static final String STATUS_COMPLETED = "completed";

    /** 任务失败状态。 */
    private static final String STATUS_FAILED = "failed";

    /** 输入节点，任务刚创建时处于该节点。 */
    private static final String NODE_INPUT = "输入";

    /** 剧情生成节点。 */
    private static final String NODE_STORY = "剧情生成";

    /** 剧本生成节点。 */
    private static final String NODE_SCRIPT = "剧本生成";

    /** 分镜生成节点。 */
    private static final String NODE_STORYBOARD = "分镜生成";

    /** 分镜校验压缩节点。 */
    private static final String NODE_STORYBOARD_CHECK = "分镜校验";

    /** 三视图和场景图提示词节点。 */
    private static final String NODE_VISUAL = "三视图/场景图";

    /** 完成节点，前端点击该节点展示全部内容。 */
    private static final String NODE_COMPLETE = "完成";

    /** 剧本阶段旧提示词分类，兼容用户调整 category 前的数据库数据。 */
    private static final String LEGACY_CATEGORY_SCRIPT = "剧情改剧本";

    /** 分镜阶段旧提示词分类，兼容用户调整 category 前的数据库数据。 */
    private static final String LEGACY_CATEGORY_STORYBOARD = "剧本改分镜";

    /** 分镜校验阶段旧提示词分类，兼容用户调整 category 前的数据库数据。 */
    private static final String LEGACY_CATEGORY_STORYBOARD_CHECK = "镜时长校验压缩";

    /** 分镜校验阶段历史拼写，避免旧环境已有数据查询不到模板。 */
    private static final String LEGACY_CATEGORY_STORYBOARD_DURATION_CHECK = "分镜时长校验压缩";

    /** 视觉提示词阶段旧提示词分类，兼容用户调整 category 前的数据库数据。 */
    private static final String LEGACY_CATEGORY_VISUAL = "人物三视图和场景图";

    /** 调试提示词字段的默认 JSON，始终保持对象结构。 */
    private static final String EMPTY_JSON_OBJECT = "{}";

    /** 剧情任务 Mapper，负责 ai_story_tasks 宽表读写。 */
    private final AiStoryTaskMapper storyTaskMapper;

    /** 平台规则 Mapper，负责读取平台画像和平台规则占位符内容。 */
    private final AiStoryPlatformRuleMapper platformRuleMapper;

    /** 提示词模板 Mapper，负责按节点分类和平台前缀定位发布模板。 */
    private final AiPromptTemplateMapper promptTemplateMapper;

    /** 文本生成模型网关，隐藏 DeepSeek、Mock 等供应商差异。 */
    private final AiTextGenerationService textGenerationService;

    /** 通用线程池，用户点击开始生成后任务在后台继续执行。 */
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /** Spring 环境配置，读取模型、比例和默认占位符配置。 */
    private final Environment environment;

    /**
     * 注入剧情任务服务依赖，构造函数显式列出依赖以便测试替换。
     */
    @Autowired
    public AiStoryTaskServiceImpl(AiStoryTaskMapper storyTaskMapper,
            AiStoryPlatformRuleMapper platformRuleMapper,
            AiPromptTemplateMapper promptTemplateMapper,
            AiTextGenerationService textGenerationService,
            ThreadPoolTaskExecutor threadPoolTaskExecutor,
            Environment environment)
    {
        this.storyTaskMapper = storyTaskMapper;
        this.platformRuleMapper = platformRuleMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.textGenerationService = textGenerationService;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.environment = environment;
    }

    /**
     * 创建任务并提交后台线程池。
     *
     * <p>创建接口只负责保存输入和初始节点，随后立即返回任务 ID；后台线程按节点回写进度，用户可以继续新建任务。</p>
     */
    @Override
    public AiStoryTask createTask(Long userId, AiStoryTaskRequest request)
    {
        validateCreateRequest(userId, request);

        Date now = new Date();
        AiStoryTask task = new AiStoryTask();
        task.setId(UUID.randomUUID().toString());
        task.setUserId(userId);
        task.setTaskType(TASK_TYPE_STORY);
        task.setStoryTheme(StringUtils.trim(request.getStoryTheme()));
        task.setProductInfo(StringUtils.trim(request.getProductInfo()));
        task.setTargetPlatform(StringUtils.trim(request.getTargetPlatform()));
        task.setTargetAudience(StringUtils.trim(request.getTargetAudience()));
        task.setStoryStyle(StringUtils.trim(request.getStoryStyle()));
        task.setStoryDuration(StringUtils.trim(request.getStoryDuration()));
        task.setCurrentNode(NODE_INPUT);
        task.setStatus(STATUS_PROCESSING);
        task.setDebugPrompts(EMPTY_JSON_OBJECT);
        task.setModelProvider(modelProvider());
        task.setModelName(modelName());
        task.setModelParams(EMPTY_JSON_OBJECT);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        storyTaskMapper.insertAiStoryTask(task);

        // 异步执行让前端按钮请求快速返回，后台节点进度由查询接口轮询展示。
        threadPoolTaskExecutor.submit(() -> executeTask(task.getId()));
        return task;
    }

    /**
     * 查询当前用户的剧情任务列表。
     */
    @Override
    public List<AiStoryTask> selectTaskList(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        AiStoryTask query = new AiStoryTask();
        query.setUserId(userId);
        query.setTaskType(TASK_TYPE_STORY);
        return storyTaskMapper.selectAiStoryTaskList(query);
    }

    /**
     * 查询当前用户的剧情任务详情。
     */
    @Override
    public AiStoryTaskDetail selectTaskDetail(String taskId, Long userId)
    {
        AiStoryTask task = requireOwnedTask(taskId, userId);
        AiStoryTaskDetail detail = new AiStoryTaskDetail();
        BeanUtils.copyProperties(task, detail);
        return detail;
    }

    /**
     * 重试失败的剧情生成任务。
     *
     * <p>重试复用原任务记录，只清理重试起点及其后续节点，避免破坏用户已经确认可用的上游生成内容。</p>
     */
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

        // 后台从可恢复节点继续串行生成，保留 retryNode 之前已经落库的内容。
        threadPoolTaskExecutor.submit(() -> executeTaskFrom(task.getId(), retryNode));
        return task;
    }

    /**
     * 后台串行执行剧情任务。
     *
     * <p>每个阶段都先设置当前节点，再查模板、渲染提示词、调用模型、保存输出。任何异常都会停在当前节点并保留前序输出。</p>
     */
    @Override
    public void executeTask(String taskId)
    {
        executeTaskFrom(taskId, NODE_STORY);
    }

    /**
     * 从指定节点开始执行剧情任务。
     *
     * <p>首次生成从剧情节点开始；重试则从失败节点或最近可恢复节点开始，后续节点会重新生成以避免依赖旧内容。</p>
     */
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

    /**
     * 执行剧情生成阶段，输出后续剧本阶段的剧情正文。
     */
    private void runStoryStage(AiStoryTask task, Map<String, String> baseVariables)
    {
        StageResult result = generateStage(task, NODE_STORY, NODE_STORY, platformPrefix(task), List.of(), baseVariables);
        task.setStoryPromptTemplateId(result.getTemplate().getId());
        task.setStoryPromptVersion(result.getTemplate().getVersion());
        task.setStoryOutput(result.getContent());
        saveProgress(task);
    }

    /**
     * 执行剧本生成阶段，必须使用剧情生成阶段的输出作为 {storyText}。
     */
    private void runScriptStage(AiStoryTask task, Map<String, String> baseVariables)
    {
        Map<String, String> variables = copyVariables(baseVariables);
        variables.put("storyText", task.getStoryOutput());
        StageResult result = generateStage(task, NODE_SCRIPT, NODE_SCRIPT, null, List.of(LEGACY_CATEGORY_SCRIPT),
                variables);
        task.setScriptPromptTemplateId(result.getTemplate().getId());
        task.setScriptPromptVersion(result.getTemplate().getVersion());
        task.setScriptOutput(result.getContent());
        saveProgress(task);
    }

    /**
     * 执行分镜生成阶段，必须使用剧本生成阶段的输出作为 {scriptText}。
     */
    private void runStoryboardStage(AiStoryTask task, Map<String, String> baseVariables)
    {
        Map<String, String> variables = copyVariables(baseVariables);
        variables.put("scriptText", task.getScriptOutput());
        variables.put("videoModelName", environment.getProperty("egg.ai.story.video-model-name", "视频模型"));
        StageResult result = generateStage(task, NODE_STORYBOARD, NODE_STORYBOARD, null,
                List.of(LEGACY_CATEGORY_STORYBOARD), variables);
        task.setStoryboardPromptTemplateId(result.getTemplate().getId());
        task.setStoryboardPromptVersion(result.getTemplate().getVersion());
        task.setStoryboardOutput(result.getContent());
        saveProgress(task);
    }

    /**
     * 执行分镜校验压缩阶段，必须使用原始分镜作为 {storyboardText}。
     */
    private void runStoryboardCheckStage(AiStoryTask task, Map<String, String> baseVariables)
    {
        Map<String, String> variables = copyVariables(baseVariables);
        variables.put("storyboardText", task.getStoryboardOutput());
        StageResult result = generateStage(task, NODE_STORYBOARD_CHECK, NODE_STORYBOARD_CHECK, null,
                List.of(LEGACY_CATEGORY_STORYBOARD_CHECK, LEGACY_CATEGORY_STORYBOARD_DURATION_CHECK), variables);
        task.setStoryboardCheckPromptTemplateId(result.getTemplate().getId());
        task.setStoryboardCheckPromptVersion(result.getTemplate().getVersion());
        task.setStoryboardCheckOutput(result.getContent());
        saveProgress(task);
    }

    /**
     * 执行三视图/场景图提示词阶段，使用剧本和校验后的分镜共同生成视觉提示词文本。
     */
    private void runVisualStage(AiStoryTask task, Map<String, String> baseVariables)
    {
        Map<String, String> variables = copyVariables(baseVariables);
        variables.put("scriptText", task.getScriptOutput());
        variables.put("storyboardText", task.getStoryboardCheckOutput());
        variables.put("imageModelName", environment.getProperty("egg.ai.story.image-model-name", "图片模型"));
        StageResult result = generateStage(task, NODE_VISUAL, NODE_VISUAL, null, List.of(LEGACY_CATEGORY_VISUAL),
                variables);
        task.setVisualPromptTemplateId(result.getTemplate().getId());
        task.setVisualPromptVersion(result.getTemplate().getVersion());
        task.setVisualOutput(result.getContent());
        saveProgress(task);
    }

    /**
     * 执行单个模型生成阶段的通用流程。
     *
     * <p>该方法会先保存当前节点和渲染后的提示词，再调用模型；如果模型失败，失败处理仍能把当前节点和提示词留在任务记录里。</p>
     */
    private StageResult generateStage(AiStoryTask task, String node, String templateCategory, String platformPrefix,
            List<String> fallbackCategories, Map<String, String> variables)
    {
        task.setCurrentNode(node);
        task.setStatus(STATUS_PROCESSING);
        task.setFailureReason(null);
        saveProgress(task);

        AiPromptTemplate template = promptTemplateMapper.selectPublishedTemplateForStoryStage(templateCategory,
                platformPrefix, fallbackCategories);
        if (template == null)
        {
            throw new ServiceException("未找到已发布的" + node + "提示词模板");
        }

        String prompt = renderPrompt(template, variables);
        rememberPrompt(task, node, prompt);

        AiTextGenerationRequest request = new AiTextGenerationRequest();
        request.setStage(node);
        request.setPrompt(prompt);
        request.setProvider(modelProvider());
        request.setModel(modelName());
        AiTextGenerationResult result = textGenerationService.generate(request);

        task.setModelProvider(StringUtils.defaultIfBlank(result.getProvider(), modelProvider()));
        task.setModelName(StringUtils.defaultIfBlank(result.getModel(), modelName()));
        return new StageResult(template, result.getContent());
    }

    /**
     * 渲染提示词模板正文。
     *
     * <p>模板 content 为 JSONB 字符串，body 字段保存 Markdown 提示词；占位符采用精确字符串替换，不做隐式追加。</p>
     */
    private String renderPrompt(AiPromptTemplate template, Map<String, String> variables)
    {
        JSONObject contentJson;
        try
        {
            contentJson = JSON.parseObject(template.getContent());
        }
        catch (JSONException e)
        {
            throw new ServiceException("提示词模板内容不是合法JSON");
        }

        String prompt = contentJson.getString("body");
        if (StringUtils.isBlank(prompt))
        {
            throw new ServiceException("提示词模板正文为空");
        }
        for (Map.Entry<String, String> entry : variables.entrySet())
        {
            prompt = prompt.replace("{" + entry.getKey() + "}", StringUtils.defaultString(entry.getValue()));
        }
        return prompt;
    }

    /**
     * 构造所有阶段共享的基础占位符变量。
     */
    private Map<String, String> buildBaseVariables(AiStoryTask task, AiStoryPlatformRule platformRule)
    {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("storyTheme", task.getStoryTheme());
        variables.put("productInfo", task.getProductInfo());
        variables.put("targetPlatform", task.getTargetPlatform());
        variables.put("targetAudience", task.getTargetAudience());
        variables.put("storyStyle", task.getStoryStyle());
        variables.put("storyDuration", task.getStoryDuration());
        variables.put("platformProfile", platformRule.getPlatformProfile());
        variables.put("categoryRules", platformRule.getCategoryRules());
        variables.put("aspectRatio", environment.getProperty("egg.ai.story.aspect-ratio", "9:16"));
        return variables;
    }

    /**
     * 复制变量 Map，保证每个节点只追加自己需要的上游输出，不污染其他节点。
     */
    private Map<String, String> copyVariables(Map<String, String> variables)
    {
        return new LinkedHashMap<>(variables);
    }

    /**
     * 判断指定阶段是否应在本次执行中运行。
     */
    private boolean shouldRunStage(String startNode, String stageNode)
    {
        return stageOrder(startNode) <= stageOrder(stageNode);
    }

    /**
     * 固定剧情流程的节点顺序，供重试恢复和下游清理共用。
     */
    private int stageOrder(String node)
    {
        if (NODE_STORY.equals(node))
        {
            return 1;
        }
        if (NODE_SCRIPT.equals(node))
        {
            return 2;
        }
        if (NODE_STORYBOARD.equals(node))
        {
            return 3;
        }
        if (NODE_STORYBOARD_CHECK.equals(node))
        {
            return 4;
        }
        if (NODE_VISUAL.equals(node))
        {
            return 5;
        }
        return 1;
    }

    /**
     * 根据失败节点和已保存的上游输出，计算真正可以恢复执行的节点。
     */
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

    /**
     * 非生成节点或未知节点重试时，从首个生成节点兜底恢复。
     */
    private String normalizeRetryNode(String node)
    {
        if (NODE_SCRIPT.equals(node) || NODE_STORYBOARD.equals(node)
                || NODE_STORYBOARD_CHECK.equals(node) || NODE_VISUAL.equals(node))
        {
            return node;
        }
        return NODE_STORY;
    }

    /**
     * 返回缺失依赖应由哪个上游节点重新生成；没有缺失依赖时返回 null。
     */
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

    /**
     * 清理重试起点及其下游节点，防止旧下游结果和新上游内容混在一起展示。
     */
    private void clearFromNode(AiStoryTask task, String startNode)
    {
        if (shouldRunStage(startNode, NODE_STORY))
        {
            task.setStoryPromptTemplateId(null);
            task.setStoryPromptVersion(null);
            task.setStoryOutput(null);
            clearPrompt(task, NODE_STORY);
        }
        if (shouldRunStage(startNode, NODE_SCRIPT))
        {
            task.setScriptPromptTemplateId(null);
            task.setScriptPromptVersion(null);
            task.setScriptOutput(null);
            clearPrompt(task, NODE_SCRIPT);
        }
        if (shouldRunStage(startNode, NODE_STORYBOARD))
        {
            task.setStoryboardPromptTemplateId(null);
            task.setStoryboardPromptVersion(null);
            task.setStoryboardOutput(null);
            clearPrompt(task, NODE_STORYBOARD);
        }
        if (shouldRunStage(startNode, NODE_STORYBOARD_CHECK))
        {
            task.setStoryboardCheckPromptTemplateId(null);
            task.setStoryboardCheckPromptVersion(null);
            task.setStoryboardCheckOutput(null);
            clearPrompt(task, NODE_STORYBOARD_CHECK);
        }
        if (shouldRunStage(startNode, NODE_VISUAL))
        {
            task.setVisualPromptTemplateId(null);
            task.setVisualPromptVersion(null);
            task.setVisualOutput(null);
            clearPrompt(task, NODE_VISUAL);
        }
    }

    /**
     * 从调试 prompt JSON 中移除指定节点，保留其他节点的排查信息。
     */
    private void clearPrompt(AiStoryTask task, String node)
    {
        JSONObject prompts;
        try
        {
            prompts = JSON.parseObject(StringUtils.defaultIfBlank(task.getDebugPrompts(), EMPTY_JSON_OBJECT));
        }
        catch (JSONException e)
        {
            prompts = new JSONObject();
        }
        prompts.remove(node);
        task.setDebugPrompts(prompts.toJSONString());
    }

    /**
     * 查询并校验目标平台规则。
     */
    private AiStoryPlatformRule requirePlatformRule(AiStoryTask task)
    {
        AiStoryPlatformRule rule = platformRuleMapper.selectPublishedRuleByPlatform(task.getTargetPlatform());
        if (rule == null)
        {
            throw new ServiceException("未配置已发布的平台规则：" + task.getTargetPlatform());
        }
        return rule;
    }

    /**
     * 把平台规则快照记录到任务上，方便后续排查同一提示词在不同规则版本下的输出差异。
     */
    private void rememberPlatformRule(AiStoryTask task, AiStoryPlatformRule platformRule)
    {
        task.setPlatformRuleId(platformRule.getId());
        task.setPlatformRuleVersion(platformRule.getVersion());
        saveProgress(task);
    }

    /**
     * 保存某个节点渲染后的完整提示词，供后台排查和后续复现。
     */
    private void rememberPrompt(AiStoryTask task, String node, String prompt)
    {
        JSONObject prompts;
        try
        {
            prompts = JSON.parseObject(StringUtils.defaultIfBlank(task.getDebugPrompts(), EMPTY_JSON_OBJECT));
        }
        catch (JSONException e)
        {
            prompts = new JSONObject();
        }
        prompts.put(node, prompt);
        task.setDebugPrompts(prompts.toJSONString());
    }

    /**
     * 标记任务完成并写入完成节点。
     */
    private void completeTask(AiStoryTask task)
    {
        Date now = new Date();
        task.setCurrentNode(NODE_COMPLETE);
        task.setStatus(STATUS_COMPLETED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        storyTaskMapper.updateAiStoryTask(task);
    }

    /**
     * 标记任务失败，保留当前节点和所有已完成节点输出。
     */
    private void failTask(AiStoryTask task, Exception e)
    {
        if (e instanceof ServiceException)
        {
            log.warn("剧情生成任务业务失败，taskId={}, node={}, reason={}", task.getId(), task.getCurrentNode(),
                    e.getMessage());
        }
        else
        {
            log.error("剧情生成任务执行失败，taskId={}, node={}", task.getId(), task.getCurrentNode(), e);
        }
        task.setStatus(STATUS_FAILED);
        task.setFailureReason(StringUtils.abbreviate(StringUtils.defaultIfBlank(e.getMessage(), "剧情生成任务执行失败"), 500));
        task.setUpdatedAt(new Date());
        storyTaskMapper.updateAiStoryTask(task);
    }

    /**
     * 保存任务进度并刷新更新时间。
     */
    private void saveProgress(AiStoryTask task)
    {
        task.setUpdatedAt(new Date());
        storyTaskMapper.updateAiStoryTask(task);
    }

    /**
     * 校验新建任务的用户和必填参数。
     */
    private void validateCreateRequest(Long userId, AiStoryTaskRequest request)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (request == null)
        {
            throw new ServiceException("剧情生成参数不能为空");
        }
        if (StringUtils.isBlank(request.getStoryTheme()))
        {
            throw new ServiceException("剧情主题不能为空");
        }
        if (StringUtils.isBlank(request.getProductInfo()))
        {
            throw new ServiceException("产品信息不能为空");
        }
        if (StringUtils.isBlank(request.getTargetPlatform()))
        {
            throw new ServiceException("目标平台不能为空");
        }
        if (StringUtils.isBlank(request.getTargetAudience()))
        {
            throw new ServiceException("目标人群不能为空");
        }
        if (StringUtils.isBlank(request.getStoryStyle()))
        {
            throw new ServiceException("推荐风格不能为空");
        }
        if (StringUtils.isBlank(request.getStoryDuration()))
        {
            throw new ServiceException("剧情时长不能为空");
        }
    }

    /**
     * 校验任务存在且属于当前用户。
     */
    private AiStoryTask requireOwnedTask(String taskId, Long userId)
    {
        if (StringUtils.isBlank(taskId) || userId == null)
        {
            throw new ServiceException("剧情任务不存在");
        }
        AiStoryTask task = storyTaskMapper.selectAiStoryTaskById(taskId);
        if (task == null || !userId.equals(task.getUserId()))
        {
            throw new ServiceException("剧情任务不存在");
        }
        return task;
    }

    /**
     * 剧情生成阶段的平台专属模板名称前缀。
     */
    private String platformPrefix(AiStoryTask task)
    {
        return StringUtils.defaultString(task.getTargetPlatform()) + "-";
    }

    /**
     * 读取剧情任务使用的模型供应商配置。
     */
    private String modelProvider()
    {
        return environment.getProperty("egg.ai.model-gateway.story.provider", "deepseek");
    }

    /**
     * 读取剧情任务使用的模型名称配置。
     */
    private String modelName()
    {
        return environment.getProperty("egg.ai.model-gateway.story.model", "deepseek-v4-pro");
    }

    /**
     * 单阶段生成结果，携带模板快照和模型正文。
     */
    private static class StageResult
    {
        /** 当前阶段实际使用的提示词模板。 */
        private final AiPromptTemplate template;

        /** 当前阶段模型返回正文。 */
        private final String content;

        /**
         * 构造阶段结果。
         */
        StageResult(AiPromptTemplate template, String content)
        {
            this.template = template;
            this.content = content;
        }

        /**
         * 返回当前阶段实际使用的提示词模板。
         */
        AiPromptTemplate getTemplate()
        {
            return template;
        }

        /**
         * 返回当前阶段模型输出正文。
         */
        String getContent()
        {
            return content;
        }
    }
}
