package com.egg.commerce.system.service.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

public interface AiKnowledgeVectorizationService
{
    void startVectorization(String documentId, MultipartFile file);

    void startVectorization(AiKnowledgeDocument document);
}
