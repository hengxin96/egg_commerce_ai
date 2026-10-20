package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
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
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/** Implements the first-version AI requirement rewrite workflow for product images. */
@Service
public class AiProductRequirementRewriteServiceImpl implements AiProductRequirementRewriteService
{
    private static final String TEMPLATE_NAME = "AI帮写优化提示词";
    private static final String TEMPLATE_CATEGORY = "AI优化提示词";
    private static final String STAGE = "AI帮写";
    private static final String DOUYIN_PLATFORM_RULES = "抖音商品主图应突出商品主体和核心卖点，画面简洁清晰，避免夸大宣传、虚假承诺和违规敏感内容。";
    private static final String GENERIC_PLATFORM_RULES = "商品主图应突出商品主体和核心卖点，保持画面清晰可信，避免夸大宣传、虚假承诺和违规敏感内容。";

    private final AiPromptTemplateMapper promptTemplateMapper;
    private final AiTextGenerationService textGenerationService;

    public AiProductRequirementRewriteServiceImpl(AiProductAssetMapper assetMapper,
            AiPromptTemplateMapper promptTemplateMapper, AiTextGenerationService textGenerationService)
    {
        this.promptTemplateMapper = promptTemplateMapper;
        this.textGenerationService = textGenerationService;
    }

    @Override
    public AiRequirementRewriteResult rewrite(Long userId, AiRequirementRewriteRequest request)
    {
        return rewriteInternal(userId, request, null);
    }

    @Override
    public AiRequirementRewriteResult rewriteStream(Long userId, AiRequirementRewriteRequest request, Consumer<String> onDelta)
    {
        return rewriteInternal(userId, request, onDelta);
    }

    private AiRequirementRewriteResult rewriteInternal(Long userId, AiRequirementRewriteRequest request,
            Consumer<String> onDelta)
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
        generationRequest.setPrompt(renderPrompt(template, variables(request)));
        OptimizedRequirementDeltaEmitter deltaEmitter = new OptimizedRequirementDeltaEmitter(onDelta);
        AiTextGenerationResult generationResult = onDelta == null
                ? textGenerationService.generate(generationRequest)
                : textGenerationService.generateStream(generationRequest, deltaEmitter::accept);
        AiRequirementRewriteResult result = parseResult(generationResult == null ? null : generationResult.getContent());
        deltaEmitter.emitRemaining(result.getOptimizedRequirement());
        return result;
    }

    private Map<String, String> variables(AiRequirementRewriteRequest request)
    {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("targetPlatform", StringUtils.defaultString(request.getTargetPlatform()));
        variables.put("userRequirement", request.getUserRequirement());
        // AI帮写只优化用户当前输入的要求；系统生成参数留给后续生图规划模板处理。
        variables.put("platformRules", platformRules(request.getTargetPlatform()));
        return variables;
    }

    private String platformRules(String targetPlatform)
    {
        if (StringUtils.contains(StringUtils.defaultString(targetPlatform), "抖音"))
        {
            return DOUYIN_PLATFORM_RULES;
        }
        return GENERIC_PLATFORM_RULES;
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

    private static class OptimizedRequirementDeltaEmitter
    {
        private static final String FIELD_NAME = "\"optimizedRequirement\"";

        private final Consumer<String> onDelta;
        private final StringBuilder buffer = new StringBuilder();
        private int emittedLength;

        private OptimizedRequirementDeltaEmitter(Consumer<String> onDelta)
        {
            this.onDelta = onDelta;
        }

        private void accept(String rawDelta)
        {
            if (onDelta == null || StringUtils.isEmpty(rawDelta))
            {
                return;
            }
            buffer.append(rawDelta);
            String current = extractOptimizedRequirement(buffer.toString());
            emitNewText(current);
        }

        private void emitRemaining(String finalValue)
        {
            emitNewText(StringUtils.defaultString(finalValue));
        }

        private void emitNewText(String current)
        {
            if (onDelta == null || current == null || current.length() <= emittedLength)
            {
                return;
            }
            onDelta.accept(current.substring(emittedLength));
            emittedLength = current.length();
        }

        private static String extractOptimizedRequirement(String rawJson)
        {
            int keyIndex = rawJson.indexOf(FIELD_NAME);
            if (keyIndex < 0)
            {
                return "";
            }
            int colonIndex = rawJson.indexOf(':', keyIndex + FIELD_NAME.length());
            if (colonIndex < 0)
            {
                return "";
            }
            int valueStart = colonIndex + 1;
            while (valueStart < rawJson.length() && Character.isWhitespace(rawJson.charAt(valueStart)))
            {
                valueStart++;
            }
            if (valueStart >= rawJson.length() || rawJson.charAt(valueStart) != '"')
            {
                return "";
            }
            return decodeJsonStringPrefix(rawJson, valueStart + 1);
        }

        private static String decodeJsonStringPrefix(String rawJson, int start)
        {
            StringBuilder value = new StringBuilder();
            for (int index = start; index < rawJson.length(); index++)
            {
                char current = rawJson.charAt(index);
                if (current == '"')
                {
                    return value.toString();
                }
                if (current != '\\')
                {
                    value.append(current);
                    continue;
                }
                if (index + 1 >= rawJson.length())
                {
                    return value.toString();
                }
                char escaped = rawJson.charAt(++index);
                if (escaped == 'u')
                {
                    if (index + 4 >= rawJson.length())
                    {
                        return value.toString();
                    }
                    value.append((char) Integer.parseInt(rawJson.substring(index + 1, index + 5), 16));
                    index += 4;
                    continue;
                }
                value.append(switch (escaped)
                {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> escaped;
                });
            }
            return value.toString();
        }
    }
}
