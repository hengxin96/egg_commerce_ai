package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingRequest;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingResult;
import com.egg.commerce.system.service.ai.model.AiVisionUnderstandingService;
import com.egg.commerce.system.service.ai.product.AiProductImageAuditRequest;
import com.egg.commerce.system.service.ai.product.AiProductImageAuditService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Default non-blocking audit chain for generated e-commerce product images. */
@Service
public class AiProductImageAuditServiceImpl implements AiProductImageAuditService
{
    public static final String STAGE_PRODUCT_IMAGE_AUDIT = "product_image_audit";

    private static final String TEMPLATE_NAME = "商品图视觉审核提示词";
    private static final String TEMPLATE_CATEGORY = "AI视觉审核";
    private static final String STATUS_PASS = "pass";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_HIGH_RISK = "high_risk";

    private final AiVisionUnderstandingService visionUnderstandingService;
    private final AiPromptTemplateMapper promptTemplateMapper;

    public AiProductImageAuditServiceImpl(AiVisionUnderstandingService visionUnderstandingService)
    {
        this(visionUnderstandingService, null);
    }

    @Autowired
    public AiProductImageAuditServiceImpl(AiVisionUnderstandingService visionUnderstandingService,
            AiPromptTemplateMapper promptTemplateMapper)
    {
        this.visionUnderstandingService = visionUnderstandingService;
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    public JSONObject audit(AiProductImageAuditRequest request)
    {
        JSONObject output = request == null || request.getGenerationOutput() == null
                ? new JSONObject(new LinkedHashMap<>()) : request.getGenerationOutput();
        output.put("schemaVersion", "product_image_generation_v3");
        JSONArray images = output.getJSONArray("images");
        if (images == null)
        {
            images = new JSONArray();
            output.put("images", images);
        }

        JSONArray summaryRisks = new JSONArray();
        addCountRisk(output, images, summaryRisks);
        int highRiskCount = 0;
        int warningCount = summaryRisks.isEmpty() ? 0 : 1;
        int visionCompletedCount = 0;
        for (int index = 0; index < images.size(); index++)
        {
            JSONObject image = images.getJSONObject(index);
            JSONObject ruleAudit = ruleAudit(request, output, image, index);
            JSONObject visionAudit = visionAudit(request, image, index);
            image.put("ruleAudit", ruleAudit);
            image.put("visionAudit", visionAudit);
            if (visionAudit.getBooleanValue("completed"))
            {
                visionCompletedCount++;
            }

            String imageStatus = mergeStatus(ruleAudit.getString("status"), visionAudit.getString("status"));
            if (STATUS_HIGH_RISK.equals(imageStatus))
            {
                highRiskCount++;
            }
            else if (STATUS_WARNING.equals(imageStatus))
            {
                warningCount++;
            }
        }

        JSONObject summary = new JSONObject(new LinkedHashMap<>());
        summary.put("status", highRiskCount > 0 ? STATUS_HIGH_RISK : warningCount > 0 ? STATUS_WARNING : STATUS_PASS);
        summary.put("totalCount", images.size());
        summary.put("highRiskCount", highRiskCount);
        summary.put("warningCount", warningCount);
        summary.put("visionCompletedCount", visionCompletedCount);
        summary.put("risks", summaryRisks);
        output.put("auditSummary", summary);
        return output;
    }

    private void addCountRisk(JSONObject output, JSONArray images, JSONArray risks)
    {
        Integer requestedCount = output.getInteger("requestedCount");
        Integer completedCount = output.getInteger("completedCount");
        if (requestedCount != null && completedCount != null && !requestedCount.equals(completedCount))
        {
            risks.add(risk("count_mismatch", STATUS_WARNING,
                    "Generated image count does not match the requested count."));
        }
        if (completedCount != null && completedCount != images.size())
        {
            risks.add(risk("output_mismatch", STATUS_WARNING,
                    "Generation output count does not match the image audit list."));
        }
    }

    private JSONObject ruleAudit(AiProductImageAuditRequest request, JSONObject output, JSONObject image, int index)
    {
        JSONArray risks = new JSONArray();
        if (StringUtils.isBlank(finalObjectKey(image)))
        {
            risks.add(risk("missing_final_object", STATUS_HIGH_RISK, "Final generated image is missing OSS object key."));
        }
        if (StringUtils.isBlank(finalUrl(image)))
        {
            risks.add(risk("missing_final_url", STATUS_HIGH_RISK, "Final generated image is missing a viewable URL."));
        }
        Long fileSize = image.getLong("finalFileSize");
        if (fileSize == null || fileSize <= 0)
        {
            risks.add(risk("missing_file_size", STATUS_HIGH_RISK, "Final generated image file size is missing."));
        }
        if (StringUtils.isBlank(image.getString("finalMimeType")))
        {
            risks.add(risk("missing_mime_type", STATUS_WARNING, "Final generated image MIME type is missing."));
        }
        JSONObject composition = image.getJSONObject("composition");
        if (composition == null)
        {
            risks.add(risk("missing_composition", STATUS_WARNING, "Text composition metadata is missing."));
        }
        else if (textOverlayExpected(request, index) && !composition.getBooleanValue("applied"))
        {
            risks.add(risk("composition_not_applied", STATUS_WARNING,
                    "The plan requested text overlay but the final image was not composed."));
        }

        JSONObject audit = new JSONObject(new LinkedHashMap<>());
        audit.put("status", statusFromRisks(risks));
        audit.put("risks", risks);
        audit.put("checked", true);
        audit.put("requestedCount", output.getInteger("requestedCount"));
        audit.put("completedCount", output.getInteger("completedCount"));
        return audit;
    }

    private JSONObject visionAudit(AiProductImageAuditRequest request, JSONObject image, int index)
    {
        List<String> finalUrls = request == null ? List.of() : nullToEmpty(request.getFinalImageUrls());
        String finalUrl = index < finalUrls.size() ? finalUrls.get(index) : finalUrl(image);
        if (StringUtils.isBlank(finalUrl))
        {
            return failedVisionAudit("Final image URL is missing before vision audit.");
        }
        List<String> imageUrls = new ArrayList<>(request == null ? List.of() : nullToEmpty(request.getSourceImageUrls()));
        imageUrls.add(finalUrl);
        if (imageUrls.size() < 2)
        {
            return failedVisionAudit("Source image URL is missing before vision audit.");
        }

        try
        {
            AiVisionUnderstandingRequest visionRequest = new AiVisionUnderstandingRequest();
            visionRequest.setStage(STAGE_PRODUCT_IMAGE_AUDIT);
            visionRequest.setSystemPrompt("You are a strict e-commerce product image quality auditor.");
            visionRequest.setPrompt(visionPrompt(request, image, index));
            visionRequest.setImageUrls(imageUrls);
            visionRequest.setProvider(request == null ? null : request.getProvider());
            visionRequest.setModel(request == null ? null : request.getModel());
            visionRequest.setJsonResponse(true);
            visionRequest.setHighResolution(request == null ? Boolean.TRUE : request.getHighResolution());
            AiVisionUnderstandingResult result = visionUnderstandingService.analyze(visionRequest);
            return normalizedVisionAudit(result);
        }
        catch (Exception e)
        {
            return failedVisionAudit(StringUtils.defaultIfBlank(e.getMessage(), "Vision audit failed."));
        }
    }

    private JSONObject normalizedVisionAudit(AiVisionUnderstandingResult result)
    {
        JSONObject parsed = parseObject(result == null ? null : result.getContent());
        JSONArray risks = normalizedRisks(parsed.getJSONArray("risks"));
        String status = normalizeStatus(parsed.getString("status"), statusFromRisks(risks));
        JSONObject audit = new JSONObject(new LinkedHashMap<>());
        audit.put("completed", true);
        audit.put("status", status);
        audit.put("risks", risks);
        audit.put("recommendations", parsed.getJSONArray("recommendations"));
        audit.put("provider", result == null ? null : result.getProvider());
        audit.put("model", result == null ? null : result.getModel());
        return audit;
    }

    private JSONObject failedVisionAudit(String reason)
    {
        JSONArray risks = new JSONArray();
        risks.add(risk("vision_audit_failed", STATUS_WARNING, reason));
        JSONObject audit = new JSONObject(new LinkedHashMap<>());
        audit.put("completed", false);
        audit.put("status", STATUS_WARNING);
        audit.put("risks", risks);
        return audit;
    }

    private String visionPrompt(AiProductImageAuditRequest request, JSONObject image, int index)
    {
        JSONObject context = new JSONObject(new LinkedHashMap<>());
        context.put("taskId", request == null ? null : request.getTaskId());
        context.put("assetType", request == null ? null : request.getAssetType());
        context.put("targetPlatform", request == null ? null : request.getTargetPlatform());
        context.put("targetLanguage", request == null ? null : request.getTargetLanguage());
        context.put("textEnabled", request != null && Boolean.TRUE.equals(request.getTextEnabled()));
        context.put("imageIndex", index + 1);
        context.put("imagePlan", imagePlan(request, index));
        context.put("generatedImage", image);
        if (promptTemplateMapper != null)
        {
            AiPromptTemplate template = promptTemplateMapper.selectPublishedTemplateByNameAndCategory(TEMPLATE_NAME,
                    TEMPLATE_CATEGORY);
            if (template == null)
            {
                throw new ServiceException("未找到已发布的商品图视觉审核提示词模板");
            }
            return renderPrompt(template, context);
        }
        return "Compare the source product images with the final generated e-commerce image. "
                + "Check product identity, color, structure, logo, packaging text, quantity, readable overlay text, "
                + "fake claims, forbidden certifications, watermarks and obvious low-quality artifacts. "
                + "Return JSON only: {\"status\":\"pass|warning|high_risk\",\"risks\":[{\"type\":\"identity|color|logo|text|claim|quality|watermark\",\"severity\":\"warning|high_risk\",\"reason\":\"short reason\"}],\"recommendations\":[\"short suggestion\"]}. "
                + "Context: " + context.toJSONString();
    }

    private JSONObject imagePlan(AiProductImageAuditRequest request, int index)
    {
        JSONObject planning = request == null ? null : request.getPlanningOutput();
        if (planning == null)
        {
            return null;
        }
        JSONArray prompts = planning.getJSONArray("imagePrompts");
        if (prompts != null && index < prompts.size())
        {
            Object item = prompts.get(index);
            return item instanceof JSONObject prompt ? prompt : null;
        }
        return planning.getJSONObject("imagePrompt");
    }

    private boolean textOverlayExpected(AiProductImageAuditRequest request, int index)
    {
        if (request == null || !Boolean.TRUE.equals(request.getTextEnabled()))
        {
            return false;
        }
        JSONObject plan = imagePlan(request, index);
        JSONObject overlay = plan == null ? null : plan.getJSONObject("textOverlay");
        return overlay != null && overlay.getBooleanValue("enabled");
    }

    private String renderPrompt(AiPromptTemplate template, JSONObject context)
    {
        JSONObject content = parseTemplateContent(template == null ? null : template.getContent());
        String prompt = content.getString("body");
        if (StringUtils.isBlank(prompt))
        {
            throw new ServiceException("视觉审核提示词模板正文为空");
        }
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("taskId", context.getString("taskId"));
        variables.put("assetType", context.getString("assetType"));
        variables.put("targetPlatform", context.getString("targetPlatform"));
        variables.put("targetLanguage", StringUtils.defaultIfBlank(context.getString("targetLanguage"), "zh-CN"));
        variables.put("textEnabled", String.valueOf(context.getBooleanValue("textEnabled")));
        variables.put("imageIndex", context.getString("imageIndex"));
        variables.put("imagePlan", StringUtils.defaultString(context.getString("imagePlan")));
        variables.put("generatedImage", StringUtils.defaultString(context.getString("generatedImage")));
        variables.put("context", context.toJSONString());
        for (Map.Entry<String, String> entry : variables.entrySet())
        {
            prompt = prompt.replace("{" + entry.getKey() + "}", StringUtils.defaultString(entry.getValue()));
        }
        return prompt;
    }

    private JSONObject parseTemplateContent(String content)
    {
        try
        {
            JSONObject parsed = JSON.parseObject(content);
            return parsed == null ? new JSONObject(new LinkedHashMap<>()) : parsed;
        }
        catch (JSONException e)
        {
            throw new ServiceException("视觉审核提示词模板内容不是合法JSON");
        }
    }

    private JSONArray normalizedRisks(JSONArray source)
    {
        JSONArray risks = new JSONArray();
        if (source == null)
        {
            return risks;
        }
        for (int index = 0; index < source.size(); index++)
        {
            Object item = source.get(index);
            if (item instanceof JSONObject risk)
            {
                String severity = normalizeStatus(risk.getString("severity"), STATUS_WARNING);
                risks.add(risk(StringUtils.defaultIfBlank(risk.getString("type"), "vision"),
                        severity, StringUtils.defaultIfBlank(risk.getString("reason"), "Vision audit risk.")));
            }
        }
        return risks;
    }

    private JSONObject parseObject(String content)
    {
        try
        {
            JSONObject parsed = JSON.parseObject(content);
            return parsed == null ? new JSONObject(new LinkedHashMap<>()) : parsed;
        }
        catch (JSONException e)
        {
            throw new ServiceException("Vision audit output is not valid JSON.");
        }
    }

    private String finalObjectKey(JSONObject image)
    {
        return StringUtils.defaultIfBlank(image.getString("finalOssObjectKey"), image.getString("ossObjectKey"));
    }

    private String finalUrl(JSONObject image)
    {
        return StringUtils.defaultIfBlank(image.getString("finalOssUrl"), image.getString("ossUrl"));
    }

    private List<String> nullToEmpty(List<String> values)
    {
        return values == null ? List.of() : values.stream().filter(StringUtils::isNotBlank).toList();
    }

    private JSONObject risk(String type, String severity, String reason)
    {
        JSONObject risk = new JSONObject(new LinkedHashMap<>());
        risk.put("type", type);
        risk.put("severity", normalizeStatus(severity, STATUS_WARNING));
        risk.put("reason", reason);
        return risk;
    }

    private String statusFromRisks(JSONArray risks)
    {
        boolean warning = false;
        for (int index = 0; index < risks.size(); index++)
        {
            JSONObject risk = risks.getJSONObject(index);
            if (STATUS_HIGH_RISK.equals(risk.getString("severity")))
            {
                return STATUS_HIGH_RISK;
            }
            if (STATUS_WARNING.equals(risk.getString("severity")))
            {
                warning = true;
            }
        }
        return warning ? STATUS_WARNING : STATUS_PASS;
    }

    private String mergeStatus(String left, String right)
    {
        if (STATUS_HIGH_RISK.equals(left) || STATUS_HIGH_RISK.equals(right))
        {
            return STATUS_HIGH_RISK;
        }
        if (STATUS_WARNING.equals(left) || STATUS_WARNING.equals(right))
        {
            return STATUS_WARNING;
        }
        return STATUS_PASS;
    }

    private String normalizeStatus(String status, String fallback)
    {
        if ("high".equalsIgnoreCase(status) || STATUS_HIGH_RISK.equalsIgnoreCase(status))
        {
            return STATUS_HIGH_RISK;
        }
        if ("warn".equalsIgnoreCase(status) || STATUS_WARNING.equalsIgnoreCase(status))
        {
            return STATUS_WARNING;
        }
        if (STATUS_PASS.equalsIgnoreCase(status))
        {
            return STATUS_PASS;
        }
        return fallback;
    }
}
