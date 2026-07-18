package com.egg.commerce.system.domain.ai.story;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 剧情生成任务实体，对应 ai_story_tasks 宽表。
 *
 * <p>剧情生成的节点固定，因此每个节点的输出和提示词版本快照直接保存在同一条任务记录里。
 * 这样生成页、任务中心和下载接口都可以一次读取完整任务，不需要再聚合阶段表。</p>
 */
public class AiStoryTask implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务主键，数据库使用 uuid，Java 侧用字符串便于前后端传递。 */
    private String id;

    /** 创建任务的前台用户 ID，所有查询和下载都必须校验该字段。 */
    private Long userId;

    /** 任务类型，剧情任务固定为 story，给任务中心后续扩展其他任务类型预留。 */
    private String taskType;

    /** 用户输入的剧情主题。 */
    private String storyTheme;

    /** 用户输入的产品信息，是剧情植入和视觉提示词的商品事实来源。 */
    private String productInfo;

    /** 目标平台字典值，同时用于匹配平台专属剧情生成提示词。 */
    private String targetPlatform;

    /** 目标人群字典值。 */
    private String targetAudience;

    /** 推荐风格字典值。 */
    private String storyStyle;

    /** 剧情时长字典值。 */
    private String storyDuration;

    /** 当前流程节点，用于前端步骤条和失败定位。 */
    private String currentNode;

    /** 任务状态：processing、completed、failed。 */
    private String status;

    /** 失败原因短文本，完整异常栈只写入服务端日志。 */
    private String failureReason;

    /** 剧情生成阶段使用的提示词模板 ID。 */
    private String storyPromptTemplateId;

    /** 剧情生成阶段使用的提示词模板版本。 */
    private Integer storyPromptVersion;

    /** 剧情生成阶段的大模型输出。 */
    private String storyOutput;

    /** 剧本生成阶段使用的提示词模板 ID。 */
    private String scriptPromptTemplateId;

    /** 剧本生成阶段使用的提示词模板版本。 */
    private Integer scriptPromptVersion;

    /** 剧本生成阶段的大模型输出。 */
    private String scriptOutput;

    /** 分镜生成阶段使用的提示词模板 ID。 */
    private String storyboardPromptTemplateId;

    /** 分镜生成阶段使用的提示词模板版本。 */
    private Integer storyboardPromptVersion;

    /** 分镜生成阶段的大模型输出。 */
    private String storyboardOutput;

    /** 分镜校验压缩阶段使用的提示词模板 ID。 */
    private String storyboardCheckPromptTemplateId;

    /** 分镜校验压缩阶段使用的提示词模板版本。 */
    private Integer storyboardCheckPromptVersion;

    /** 分镜校验压缩后的大模型输出。 */
    private String storyboardCheckOutput;

    /** 三视图/场景图提示词阶段使用的提示词模板 ID。 */
    private String visualPromptTemplateId;

    /** 三视图/场景图提示词阶段使用的提示词模板版本。 */
    private Integer visualPromptVersion;

    /** 三视图提示词和场景图提示词输出。 */
    private String visualOutput;

    /** 调试用 JSON，保存各阶段渲染后的完整提示词，避免主表拆出过多 prompt 字段。 */
    private String debugPrompts;

    /** 本任务使用的平台画像/规则记录 ID。 */
    private String platformRuleId;

    /** 本任务使用的平台画像/规则版本。 */
    private String platformRuleVersion;

    /** 模型供应商快照，例如 deepseek 或 mock。 */
    private String modelProvider;

    /** 模型名称快照，例如 deepseek-v4-pro。 */
    private String modelName;

    /** 模型参数快照 JSON，例如 temperature、max_tokens 等。 */
    private String modelParams;

    /** 后台线程开始执行任务的时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;

    /** 任务完成时间，失败任务为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completedAt;

    /** 任务创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 任务最近更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTaskType()
    {
        return taskType;
    }

    public void setTaskType(String taskType)
    {
        this.taskType = taskType;
    }

    public String getStoryTheme()
    {
        return storyTheme;
    }

    public void setStoryTheme(String storyTheme)
    {
        this.storyTheme = storyTheme;
    }

    public String getProductInfo()
    {
        return productInfo;
    }

    public void setProductInfo(String productInfo)
    {
        this.productInfo = productInfo;
    }

    public String getTargetPlatform()
    {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform)
    {
        this.targetPlatform = targetPlatform;
    }

    public String getTargetAudience()
    {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience)
    {
        this.targetAudience = targetAudience;
    }

    public String getStoryStyle()
    {
        return storyStyle;
    }

    public void setStoryStyle(String storyStyle)
    {
        this.storyStyle = storyStyle;
    }

    public String getStoryDuration()
    {
        return storyDuration;
    }

    public void setStoryDuration(String storyDuration)
    {
        this.storyDuration = storyDuration;
    }

    public String getCurrentNode()
    {
        return currentNode;
    }

    public void setCurrentNode(String currentNode)
    {
        this.currentNode = currentNode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getFailureReason()
    {
        return failureReason;
    }

    public void setFailureReason(String failureReason)
    {
        this.failureReason = failureReason;
    }

    public String getStoryPromptTemplateId()
    {
        return storyPromptTemplateId;
    }

    public void setStoryPromptTemplateId(String storyPromptTemplateId)
    {
        this.storyPromptTemplateId = storyPromptTemplateId;
    }

    public Integer getStoryPromptVersion()
    {
        return storyPromptVersion;
    }

    public void setStoryPromptVersion(Integer storyPromptVersion)
    {
        this.storyPromptVersion = storyPromptVersion;
    }

    public String getStoryOutput()
    {
        return storyOutput;
    }

    public void setStoryOutput(String storyOutput)
    {
        this.storyOutput = storyOutput;
    }

    public String getScriptPromptTemplateId()
    {
        return scriptPromptTemplateId;
    }

    public void setScriptPromptTemplateId(String scriptPromptTemplateId)
    {
        this.scriptPromptTemplateId = scriptPromptTemplateId;
    }

    public Integer getScriptPromptVersion()
    {
        return scriptPromptVersion;
    }

    public void setScriptPromptVersion(Integer scriptPromptVersion)
    {
        this.scriptPromptVersion = scriptPromptVersion;
    }

    public String getScriptOutput()
    {
        return scriptOutput;
    }

    public void setScriptOutput(String scriptOutput)
    {
        this.scriptOutput = scriptOutput;
    }

    public String getStoryboardPromptTemplateId()
    {
        return storyboardPromptTemplateId;
    }

    public void setStoryboardPromptTemplateId(String storyboardPromptTemplateId)
    {
        this.storyboardPromptTemplateId = storyboardPromptTemplateId;
    }

    public Integer getStoryboardPromptVersion()
    {
        return storyboardPromptVersion;
    }

    public void setStoryboardPromptVersion(Integer storyboardPromptVersion)
    {
        this.storyboardPromptVersion = storyboardPromptVersion;
    }

    public String getStoryboardOutput()
    {
        return storyboardOutput;
    }

    public void setStoryboardOutput(String storyboardOutput)
    {
        this.storyboardOutput = storyboardOutput;
    }

    public String getStoryboardCheckPromptTemplateId()
    {
        return storyboardCheckPromptTemplateId;
    }

    public void setStoryboardCheckPromptTemplateId(String storyboardCheckPromptTemplateId)
    {
        this.storyboardCheckPromptTemplateId = storyboardCheckPromptTemplateId;
    }

    public Integer getStoryboardCheckPromptVersion()
    {
        return storyboardCheckPromptVersion;
    }

    public void setStoryboardCheckPromptVersion(Integer storyboardCheckPromptVersion)
    {
        this.storyboardCheckPromptVersion = storyboardCheckPromptVersion;
    }

    public String getStoryboardCheckOutput()
    {
        return storyboardCheckOutput;
    }

    public void setStoryboardCheckOutput(String storyboardCheckOutput)
    {
        this.storyboardCheckOutput = storyboardCheckOutput;
    }

    public String getVisualPromptTemplateId()
    {
        return visualPromptTemplateId;
    }

    public void setVisualPromptTemplateId(String visualPromptTemplateId)
    {
        this.visualPromptTemplateId = visualPromptTemplateId;
    }

    public Integer getVisualPromptVersion()
    {
        return visualPromptVersion;
    }

    public void setVisualPromptVersion(Integer visualPromptVersion)
    {
        this.visualPromptVersion = visualPromptVersion;
    }

    public String getVisualOutput()
    {
        return visualOutput;
    }

    public void setVisualOutput(String visualOutput)
    {
        this.visualOutput = visualOutput;
    }

    public String getDebugPrompts()
    {
        return debugPrompts;
    }

    public void setDebugPrompts(String debugPrompts)
    {
        this.debugPrompts = debugPrompts;
    }

    public String getPlatformRuleId()
    {
        return platformRuleId;
    }

    public void setPlatformRuleId(String platformRuleId)
    {
        this.platformRuleId = platformRuleId;
    }

    public String getPlatformRuleVersion()
    {
        return platformRuleVersion;
    }

    public void setPlatformRuleVersion(String platformRuleVersion)
    {
        this.platformRuleVersion = platformRuleVersion;
    }

    public String getModelProvider()
    {
        return modelProvider;
    }

    public void setModelProvider(String modelProvider)
    {
        this.modelProvider = modelProvider;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getModelParams()
    {
        return modelParams;
    }

    public void setModelParams(String modelParams)
    {
        this.modelParams = modelParams;
    }

    public Date getStartedAt()
    {
        return startedAt;
    }

    public void setStartedAt(Date startedAt)
    {
        this.startedAt = startedAt;
    }

    public Date getCompletedAt()
    {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt)
    {
        this.completedAt = completedAt;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
