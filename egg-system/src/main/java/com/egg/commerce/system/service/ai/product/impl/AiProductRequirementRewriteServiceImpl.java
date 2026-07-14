package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteResult;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import com.egg.commerce.system.service.ai.product.AiProductRequirementRewriteService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Implements the first-version AI requirement rewrite workflow for product images. */
@Service
public class AiProductRequirementRewriteServiceImpl implements AiProductRequirementRewriteService
{
    private static final String TEMPLATE_NAME = "AI帮写优化提示词";
    private static final String TEMPLATE_CATEGORY = "AI优化提示词";
    private static final String STAGE = "AI帮写";
    private static final String PRODUCT_CATEGORY = "自动识别";
    private static final String DOUYIN_PLATFORM_RULES = "抖音商品主图应突出商品主体和核心卖点，画面简洁清晰，避免夸大宣传、虚假承诺和违规敏感内容。";

    private final AiProductAssetMapper assetMapper;
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiTextGenerationService textGenerationService;

    public AiProductRequirementRewriteServiceImpl(AiProductAssetMapper assetMapper,
            AiPromptTemplateMapper promptTemplateMapper, AiTextGenerationService textGenerationService)
    {
        this.assetMapper = assetMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.textGenerationService = textGenerationService;
    }

    @Override
    public AiRequirementRewriteResult rewrite(Long userId, AiRequirementRewriteRequest request)
    {
        requireUserId(userId);
        if (request == null || StringUtils.isBlank(request.getUserRequirement()))
        {
            throw new ServiceException("用户需求不能为空");
        }

        AiPromptTemplate template = promptTemplateMapper.selectPublishedTemplateByNameAndCategory(TEMPLATE_NAME,
                TEMPLATE_CATEGORY);
        if (template == null)
        {
            throw new ServiceException("未找到已发布的AI帮写优化提示词模板");
        }

        AiTextGenerationRequest generationRequest = new AiTextGenerationRequest();
        generationRequest.setStage(STAGE);
        generationRequest.setPrompt(renderPrompt(template, variables(userId, request)));
        AiTextGenerationResult generationResult = textGenerationService.generate(generationRequest);
        return parseResult(generationResult == null ? null : generationResult.getContent());
    }

    private Map<String, String> variables(Long userId, AiRequirementRewriteRequest request)
    {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("targetPlatform", StringUtils.defaultString(request.getTargetPlatform()));
        variables.put("productCategory", PRODUCT_CATEGORY);
        variables.put("productName", productName(userId, request.getProductAssetId()));
        variables.put("sellingPoints", "");
        variables.put("targetAudience", "");
        variables.put("userRequirement", request.getUserRequirement());
        // RAG-backed platform rules are not available yet, so the first version uses the Douyin baseline.
        variables.put("platformRules", DOUYIN_PLATFORM_RULES);
        return variables;
    }

    private String productName(Long userId, String productAssetId)
    {
        if (StringUtils.isBlank(productAssetId))
        {
            return "";
        }
        AiProductAsset asset = assetMapper.selectAiProductAssetById(productAssetId);
        if (asset == null || !userId.equals(asset.getUserId()))
        {
            throw new ServiceException("商品不存在");
        }
        return StringUtils.defaultString(asset.getName());
    }

    private String renderPrompt(AiPromptTemplate template, Map<String, String> variables)
    {
        JSONObject content;
        try
        {
            content = JSON.parseObject(template.getContent());
        }
        catch (JSONException e)
        {
            throw new ServiceException("提示词模板内容不是合法JSON");
        }
        String prompt = content == null ? null : content.getString("body");
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

    private AiRequirementRewriteResult parseResult(String content)
    {
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("AI帮写未返回内容");
        }
        try
        {
            return JSON.parseObject(content, AiRequirementRewriteResult.class);
        }
        catch (JSONException e)
        {
            throw new ServiceException("AI帮写返回内容不是合法JSON");
        }
    }

    private void requireUserId(Long userId)
    {
        if (userId == null || userId <= 0)
        {
            throw new ServiceException("用户不存在");
        }
    }
}
