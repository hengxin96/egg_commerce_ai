package com.egg.commerce.system.mapper.ai.prompt;

import java.util.List;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;

/**
 * Data access for AI prompt templates.
 */
public interface AiPromptTemplateMapper
{
    public AiPromptTemplate selectAiPromptTemplateById(String id);

    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate);

    public int insertAiPromptTemplate(AiPromptTemplate promptTemplate);

    public int updateAiPromptTemplate(AiPromptTemplate promptTemplate);

    public int deleteAiPromptTemplateById(String id);

    public int deleteAiPromptTemplateByIds(String[] ids);
}
