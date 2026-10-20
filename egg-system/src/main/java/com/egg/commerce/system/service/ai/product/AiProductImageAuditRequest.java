package com.egg.commerce.system.service.ai.product;

import com.alibaba.fastjson2.JSONObject;
import java.io.Serializable;
import java.util.List;

/** Request snapshot for non-blocking product image quality audit. */
public class AiProductImageAuditRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String taskId;
    private String assetType;
    private String targetPlatform;
    private String targetLanguage;
    private Boolean textEnabled;
    private JSONObject planningOutput;
    private JSONObject generationOutput;
    private List<String> sourceImageUrls;
    private List<String> finalImageUrls;
    private String provider;
    private String model;
    private Boolean highResolution;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    public Boolean getTextEnabled() { return textEnabled; }
    public void setTextEnabled(Boolean textEnabled) { this.textEnabled = textEnabled; }
    public JSONObject getPlanningOutput() { return planningOutput; }
    public void setPlanningOutput(JSONObject planningOutput) { this.planningOutput = planningOutput; }
    public JSONObject getGenerationOutput() { return generationOutput; }
    public void setGenerationOutput(JSONObject generationOutput) { this.generationOutput = generationOutput; }
    public List<String> getSourceImageUrls() { return sourceImageUrls; }
    public void setSourceImageUrls(List<String> sourceImageUrls) { this.sourceImageUrls = sourceImageUrls; }
    public List<String> getFinalImageUrls() { return finalImageUrls; }
    public void setFinalImageUrls(List<String> finalImageUrls) { this.finalImageUrls = finalImageUrls; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Boolean getHighResolution() { return highResolution; }
    public void setHighResolution(Boolean highResolution) { this.highResolution = highResolution; }
}
