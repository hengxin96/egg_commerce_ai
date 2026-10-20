package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;
import java.util.Date;

/** 商品图生成任务实体。 */
public class AiProductImageTask implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** 任务主键。 */ private String id;
    /** 商品资产 ID。 */ private String productAssetId;
    /** 前台用户 ID。 */ private Long userId;
    /** 任务类型。 */ private String taskType;
    /** 生成类型。 */ private String assetType;
    /** 目标平台。 */ private String targetPlatform;
    /** 当前节点。 */ private String currentNode;
    /** 任务状态。 */ private String status;
    /** 失败原因。 */ private String failureReason;
    /** 终止原因。 */ private String cancelReason;
    /** 分析输出 JSON。 */ private String analysisOutput;
    /** 规划输出 JSON。 */ private String planningOutput;
    /** 生图输出 JSON。 */ private String generationOutput;
    /** 调试提示词 JSON。 */ private String debugPrompts;
    /** DashScope 任务 ID。 */ private String dashscopeTaskId;
    /** 模型供应商。 */ private String modelProvider;
    /** 模型名称。 */ private String modelName;
    /** 模型参数 JSON。 */ private String modelParams;
    /** 开始时间。 */ private Date startedAt;
    /** 完成时间。 */ private Date completedAt;
    /** 终止时间。 */ private Date cancelledAt;
    /** 创建时间。 */ private Date createdAt;
    /** 更新时间。 */ private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductAssetId() { return productAssetId; }
    public void setProductAssetId(String productAssetId) { this.productAssetId = productAssetId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getAnalysisOutput() { return analysisOutput; }
    public void setAnalysisOutput(String analysisOutput) { this.analysisOutput = analysisOutput; }
    public String getPlanningOutput() { return planningOutput; }
    public void setPlanningOutput(String planningOutput) { this.planningOutput = planningOutput; }
    public String getGenerationOutput() { return generationOutput; }
    public void setGenerationOutput(String generationOutput) { this.generationOutput = generationOutput; }
    public String getDebugPrompts() { return debugPrompts; }
    public void setDebugPrompts(String debugPrompts) { this.debugPrompts = debugPrompts; }
    public String getDashscopeTaskId() { return dashscopeTaskId; }
    public void setDashscopeTaskId(String dashscopeTaskId) { this.dashscopeTaskId = dashscopeTaskId; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelParams() { return modelParams; }
    public void setModelParams(String modelParams) { this.modelParams = modelParams; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
