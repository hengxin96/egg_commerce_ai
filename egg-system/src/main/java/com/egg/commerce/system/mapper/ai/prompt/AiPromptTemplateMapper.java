package com.egg.commerce.system.mapper.ai.prompt;

import java.util.List;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;

/**
 * 提示词模板数据访问接口，对应 ai_prompt_templates 表。
 */
public interface AiPromptTemplateMapper
{
    /** 根据模板 ID 查询单条模板。 */
    AiPromptTemplate selectAiPromptTemplateById(String id);

    /** 按条件查询模板列表。 */
    List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate);

    /** 新增提示词模板。 */
    int insertAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 更新提示词模板。 */
    int updateAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 根据模板 ID 删除单条模板。 */
    int deleteAiPromptTemplateById(String id);

    /** 根据多个模板 ID 批量删除模板。 */
    int deleteAiPromptTemplateByIds(String[] ids);
}
