package com.egg.commerce.system.service.ai.prompt;

import java.util.List;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;

/**
 * 提示词模板服务契约。
 *
 * <p>面向后台提示词模板管理，负责模板查询、保存、更新和删除。实现层会校验 JSON 正文、
 * 变量定义和模板状态，避免无效配置进入模型调用流程。</p>
 */
public interface IAiPromptTemplateService
{
    /** 根据模板 ID 查询单条提示词模板。 */
    AiPromptTemplate selectAiPromptTemplateById(String id);

    /** 按条件查询提示词模板列表，供后台分页展示。 */
    List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate);

    /** 新增提示词模板，并补齐默认状态、变量定义和版本号。 */
    int insertAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 更新提示词模板，并重新校验模板正文和变量定义。 */
    int updateAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 批量删除提示词模板。 */
    int deleteAiPromptTemplateByIds(String[] ids);
}
