package com.egg.commerce.system.service.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AiKnowledgeDocumentService
{
    List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);

    AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);

    AiKnowledgeDocument upload(MultipartFile file, Long createdBy);

    int deleteAiKnowledgeDocumentByIds(String[] ids);

    void retryVectorization(String id);
}
