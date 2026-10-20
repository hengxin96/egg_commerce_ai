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
 * 提示词模板业务服务实现。
 *
 * <p>后台维护的提示词模板会被后续图片生成、RAG 问答和 agent 工作流复用，因此保存前必须校验
 * 模板状态、正文 JSON 和变量 JSON，避免无效配置进入模型调用链路。</p>
 */
@Service
public class AiPromptTemplateServiceImpl implements IAiPromptTemplateService
{
    /** 新增模板未指定状态时默认保存为草稿，避免未审核模板直接被业务流程使用。 */
    private static final String STATUS_DRAFT = "draft";

    /** 未配置变量时写入空数组，保持数据库 JSONB 字段结构稳定。 */
    private static final String DEFAULT_VARIABLES = "[]";

    /** 后台允许维护的模板状态，和页面状态筛选、后续调用逻辑保持一致。 */
    private static final Set<String> ALLOWED_STATUSES = Set.of("draft", "published", "archived");

    /** 提示词模板 Mapper，负责 ai_prompt_templates 表读写。 */
    private final AiPromptTemplateMapper promptTemplateMapper;

    /**
     * 注入提示词模板 mapper，服务层负责保存前校验和默认值填充。
     */
    @Autowired
    public AiPromptTemplateServiceImpl(AiPromptTemplateMapper promptTemplateMapper)
    {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    /**
     * 按 ID 查询提示词模板详情。
     */
    @Override
    public AiPromptTemplate selectAiPromptTemplateById(String id)
    {
        return promptTemplateMapper.selectAiPromptTemplateById(id);
    }

    /**
     * 查询后台提示词模板列表，筛选条件由 mapper 处理。
     */
    @Override
    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
    {
        return promptTemplateMapper.selectAiPromptTemplateList(promptTemplate);
    }

    /**
     * 按分类读取最新发布模板，供 RAG 聊天等运行时能力使用。
     */
    @Override
    public AiPromptTemplate selectPublishedTemplateByCategory(String category)
    {
        if (StringUtils.isBlank(category))
        {
            return null;
        }
        return promptTemplateMapper.selectPublishedTemplateByCategory(category);
    }

    /**
     * 新增提示词模板前补齐状态、版本和变量默认值。
     */
    @Override
    public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
    {
        // 新增模板时补齐默认状态、变量定义和初始版本号。
        prepareForSave(promptTemplate, true);
        return promptTemplateMapper.insertAiPromptTemplate(promptTemplate);
    }

    /**
     * 更新提示词模板前重新校验正文、变量和状态。
     */
    @Override
    public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
    {
        // 编辑模板也重新校验正文和变量，防止旧模板被改成不可解析格式。
        prepareForSave(promptTemplate, false);
        return promptTemplateMapper.updateAiPromptTemplate(promptTemplate);
    }

    /**
     * 批量删除提示词模板。
     */
    @Override
    public int deleteAiPromptTemplateByIds(String[] ids)
    {
        return promptTemplateMapper.deleteAiPromptTemplateByIds(ids);
    }

    /**
     * 统一处理提示词保存前的字段规范，避免无效模板进入模型工作流。
     */
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

    /**
     * 校验模板正文结构。
     *
     * <p>content 字段以 JSONB 保存，当前约定至少包含 body，body 保存最终给模型使用的 Markdown 文本。</p>
     */
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

        // 模型工作流使用 body 作为最终 Markdown 提示词正文。
        if (StringUtils.isBlank(contentJson.getString("body")))
        {
            throw new ServiceException("提示词正文不能为空");
        }
    }

    /**
     * 规范化变量定义。
     *
     * <p>variables 暂时由后台手工维护为 JSON，后续可扩展为结构化变量编辑器。</p>
     */
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
