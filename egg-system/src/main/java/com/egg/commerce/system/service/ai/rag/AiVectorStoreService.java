package com.egg.commerce.system.service.ai.rag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * RAG 向量库服务接口，隔离本地兜底、Milvus 和未来向量存储实现。
 */
public interface AiVectorStoreService
{
    /** 写入知识库文档切片，并返回向量库生成或复用的向量 ID。 */
    List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks);

    /** 按查询文本检索相似知识片段。 */
    List<AiVectorRecord> search(String query, int topK, double minScore);

    /** 按查询文本和授权文档 ID 白名单检索相似知识片段。 */
    default List<AiVectorRecord> search(String query, int topK, double minScore, List<String> accessibleDocumentIds)
    {
        Set<String> normalizedDocumentIds = normalizeDocumentIds(accessibleDocumentIds);
        if (normalizedDocumentIds.isEmpty())
        {
            return List.of();
        }
        return search(query, topK, minScore).stream()
                .filter(record -> record != null && normalizedDocumentIds.contains(record.getDocumentId()))
                .toList();
    }

    /** 删除指定向量 ID，配合文档删除清理可检索内容。 */
    void deleteVectors(List<String> vectorIds);

    /** 清理授权文档 ID 列表中的空值和重复值。 */
    private Set<String> normalizeDocumentIds(List<String> documentIds)
    {
        Set<String> normalizedDocumentIds = new HashSet<>();
        if (documentIds == null)
        {
            return normalizedDocumentIds;
        }
        for (String documentId : documentIds)
        {
            String normalized = StringUtils.trimToEmpty(documentId);
            if (StringUtils.isNotBlank(normalized))
            {
                normalizedDocumentIds.add(normalized);
            }
        }
        return normalizedDocumentIds;
    }
}
