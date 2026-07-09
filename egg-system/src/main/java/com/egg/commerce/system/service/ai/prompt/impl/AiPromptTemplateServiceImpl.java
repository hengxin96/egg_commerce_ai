package com.egg.commerce.system.service.ai.prompt.impl;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.service.ai.prompt.IAiPromptTemplateService;

/**
 * Business rules for AI prompt template management.
 */
@Service
public class AiPromptTemplateServiceImpl implements IAiPromptTemplateService
{
    private static final String STATUS_DRAFT = "draft";
    private static final String DEFAULT_VARIABLES = "[]";
    private static final Set<String> ALLOWED_STATUSES = Set.of("draft", "published", "archived");

    private final AiPromptTemplateMapper promptTemplateMapper;

    @Autowired
    public AiPromptTemplateServiceImpl(AiPromptTemplateMapper promptTemplateMapper)
    {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    public AiPromptTemplate selectAiPromptTemplateById(String id)
    {
        return promptTemplateMapper.selectAiPromptTemplateById(id);
    }

    @Override
    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
    {
        return promptTemplateMapper.selectAiPromptTemplateList(promptTemplate);
    }

    @Override
    public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
    {
        prepareForSave(promptTemplate, true);
        return promptTemplateMapper.insertAiPromptTemplate(promptTemplate);
    }

    @Override
    public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
    {
        prepareForSave(promptTemplate, false);
        return promptTemplateMapper.updateAiPromptTemplate(promptTemplate);
    }

    @Override
    public int deleteAiPromptTemplateByIds(String[] ids)
    {
        return promptTemplateMapper.deleteAiPromptTemplateByIds(ids);
    }

    private void prepareForSave(AiPromptTemplate promptTemplate, boolean insert)
    {
        if (StringUtils.isEmpty(promptTemplate.getStatus()))
        {
            promptTemplate.setStatus(STATUS_DRAFT);
        }
        if (!ALLOWED_STATUSES.contains(promptTemplate.getStatus()))
        {
            throw new ServiceException("提示词模板状态不正确");
        }

        validateContent(promptTemplate.getContent());
        normalizeVariables(promptTemplate);

        if (insert && promptTemplate.getVersion() == null)
        {
            promptTemplate.setVersion(1);
        }
    }

    private void validateContent(String content)
    {
        JSONObject contentJson;
        try
        {
            contentJson = JSON.parseObject(content);
        }
        catch (JSONException e)
        {
            throw new ServiceException("提示词内容必须是合法JSON");
        }

        // Model workflows consume this body as the final Markdown prompt text.
        if (StringUtils.isBlank(contentJson.getString("body")))
        {
            throw new ServiceException("提示词正文不能为空");
        }
    }

    private void normalizeVariables(AiPromptTemplate promptTemplate)
    {
        if (StringUtils.isEmpty(promptTemplate.getVariables()))
        {
            promptTemplate.setVariables(DEFAULT_VARIABLES);
            return;
        }

        try
        {
            JSON.parse(promptTemplate.getVariables());
        }
        catch (JSONException e)
        {
            throw new ServiceException("变量定义必须是合法JSON");
        }
    }
}
