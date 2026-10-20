package com.egg.commerce.system.service.ai.rag.impl;

import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 本地内存向量库兜底实现，用于开发环境和无 Milvus 时的 RAG 闭环。
 */
@Service
@ConditionalOnProperty(name = "egg.ai.rag.vector-store", havingValue = "local", matchIfMissing = true)
public class AiLocalVectorStoreServiceImpl implements AiVectorStoreService
{
    private static final String METADATA_DOCUMENT_ID = "documentId";

    private static final String METADATA_SOURCE_TITLE = "sourceTitle";

    private static final String METADATA_CHUNK_ID = "chunkId";

    private static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private final Map<String, AiVectorRecord> records = new ConcurrentHashMap<>();

    /**
     * 将知识切片写入本地内存 Map，返回后续清理需要的向量 ID。
     */
    @Override
    public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
    {
        if (StringUtils.isBlank(documentId) || chunks == null || chunks.isEmpty())
        {
            return List.of();
        }

        return chunks.stream()
                .filter(chunk -> chunk != null && StringUtils.isNotBlank(chunk.getContent()))
                .map(chunk -> saveChunk(documentId, fileName, chunk))
                .toList();
    }

    /**
     * 使用轻量文本匹配模拟向量检索，保障本地开发环境不依赖 Milvus。
     */
    @Override
    public List<AiVectorRecord> search(String query, int topK, double minScore)
    {
        if (StringUtils.isBlank(query))
        {
            return List.of();
        }

        int limit = Math.max(topK, 1);
        return records.values().stream()
                .map(record -> copyWithScore(record, score(query, record.getContent())))
                .filter(record -> record.getScore() >= minScore)
                .sorted(Comparator.comparing(AiVectorRecord::getScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 本地兜底检索也应用文档 ID 白名单，保持开发环境与 Milvus 权限语义一致。
     */
    @Override
    public List<AiVectorRecord> search(String query, int topK, double minScore, List<String> accessibleDocumentIds)
    {
        Set<String> documentIds = normalizeDocumentIds(accessibleDocumentIds);
        if (documentIds.isEmpty())
        {
            return List.of();
        }
        return search(query, topK, minScore).stream()
                .filter(record -> documentIds.contains(record.getDocumentId()))
                .toList();
    }

    /**
     * 从本地内存索引删除指定向量，配合知识库文档删除流程。
     */
    @Override
    public void deleteVectors(List<String> vectorIds)
    {
        if (vectorIds == null || vectorIds.isEmpty())
        {
            return;
        }
        vectorIds.stream().filter(StringUtils::isNotBlank).forEach(records::remove);
    }

    /**
     * 把单个文档切片转换为可检索记录并保存到内存索引。
     */
    private String saveChunk(String documentId, String fileName, AiDocumentChunk chunk)
    {
        String vectorId = StringUtils.defaultIfBlank(chunk.getChunkId(), UUID.randomUUID().toString());
        AiVectorRecord record = new AiVectorRecord();
        record.setVectorId(vectorId);
        record.setDocumentId(documentId);
        record.setChunkId(chunk.getChunkId());
        record.setChunkIndex(chunk.getChunkIndex());
        record.setSourceTitle(fileName);
        record.setContent(chunk.getContent());
        record.setMetadata(Map.of(
                METADATA_DOCUMENT_ID, documentId,
                METADATA_SOURCE_TITLE, StringUtils.defaultString(fileName),
                METADATA_CHUNK_ID, StringUtils.defaultString(chunk.getChunkId()),
                METADATA_CHUNK_INDEX, chunk.getChunkIndex() == null ? -1 : chunk.getChunkIndex()));
        records.put(vectorId, record);
        return vectorId;
    }

    /**
     * 为检索结果创建副本并写入本次查询的匹配分数。
     */
    private AiVectorRecord copyWithScore(AiVectorRecord source, double score)
    {
        AiVectorRecord copy = new AiVectorRecord();
        copy.setVectorId(source.getVectorId());
        copy.setDocumentId(source.getDocumentId());
        copy.setChunkId(source.getChunkId());
        copy.setChunkIndex(source.getChunkIndex());
        copy.setSourceTitle(source.getSourceTitle());
        copy.setContent(source.getContent());
        copy.setMetadata(source.getMetadata());
        copy.setScore(score);
        return copy;
    }

    /**
     * 计算简单字符命中率，作为本地兜底检索的相似度分数。
     */
    private double score(String query, String content)
    {
        String normalizedQuery = StringUtils.lowerCase(StringUtils.trim(query));
        String normalizedContent = StringUtils.lowerCase(StringUtils.defaultString(content));
        if (StringUtils.isBlank(normalizedQuery) || StringUtils.isBlank(normalizedContent))
        {
            return 0;
        }
        if (normalizedContent.contains(normalizedQuery))
        {
            return 1;
        }

        long matchedCharacters = normalizedQuery.chars()
                .distinct()
                .filter(ch -> normalizedContent.indexOf(ch) >= 0)
                .count();
        return matchedCharacters / (double) normalizedQuery.chars().distinct().count();
    }

    /**
     * 清理授权文档 ID 列表中的空值和重复值。
     */
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
