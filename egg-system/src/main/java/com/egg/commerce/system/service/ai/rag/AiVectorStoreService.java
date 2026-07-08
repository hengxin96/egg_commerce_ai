package com.egg.commerce.system.service.ai.rag;

import java.util.List;

public interface AiVectorStoreService
{
    List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks);

    List<AiVectorRecord> search(String query, int topK, double minScore);

    void deleteVectors(List<String> vectorIds);
}
