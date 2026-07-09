package com.egg.commerce.system.service.ai.prompt;

import java.util.List;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;

/**
 * Service contract for AI prompt template management.
 */
public interface IAiPromptTemplateService
{
    public AiPromptTemplate selectAiPromptTemplateById(String id);

    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate);

    public int insertAiPromptTemplate(AiPromptTemplate promptTemplate);

    public int updateAiPromptTemplate(AiPromptTemplate promptTemplate);

    public int deleteAiPromptTemplateByIds(String[] ids);
}
