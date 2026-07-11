package com.egg.commerce.system.service.ai.rag;

import java.util.List;

/**
 * RAG 向量库服务接口，隔离本地兜底、Milvus 和未来向量存储实现。
 */
public interface AiVectorStoreService
{
    /** 写入知识库文档切片，并返回向量库生成或复用的向量 ID。 */
    List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks);

    /** 按查询文本检索相似知识片段。 */
    List<AiVectorRecord> search(String query, int topK, double minScore);

    /** 删除指定向量 ID，配合文档删除清理可检索内容。 */
    void deleteVectors(List<String> vectorIds);
}
