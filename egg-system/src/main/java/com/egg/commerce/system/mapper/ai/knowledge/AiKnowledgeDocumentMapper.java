package com.egg.commerce.system.mapper.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import java.util.List;

public interface AiKnowledgeDocumentMapper
{
    AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);

    List<AiKnowledgeDocument> selectAiKnowledgeDocumentByIds(String[] ids);

    List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);

    int insertAiKnowledgeDocument(AiKnowledgeDocument document);

    int updateAiKnowledgeDocument(AiKnowledgeDocument document);

    int deleteAiKnowledgeDocumentByIds(String[] ids);
}
