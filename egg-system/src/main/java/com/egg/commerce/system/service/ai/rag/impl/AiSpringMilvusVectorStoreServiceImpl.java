package com.egg.commerce.system.service.ai.rag.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI 的 Milvus 向量库实现，负责真实知识切片写入、检索和删除。
 */
@Service
@ConditionalOnProperty(name = "egg.ai.rag.vector-store", havingValue = "milvus")
public class AiSpringMilvusVectorStoreServiceImpl implements AiVectorStoreService
{
    private static final int MAX_EMBEDDING_BATCH_SIZE = 10;

    private static final String METADATA_DOCUMENT_ID = "documentId";

    private static final String METADATA_SOURCE_TITLE = "sourceTitle";

    private static final String METADATA_CHUNK_ID = "chunkId";

    private static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private final VectorStore vectorStore;

    /**
     * 注入 Spring AI 向量库客户端，真实向量存储由配置选择。
     */
    public AiSpringMilvusVectorStoreServiceImpl(VectorStore vectorStore)
    {
        this.vectorStore = vectorStore;
    }

    /**
     * 将知识切片转换为 Spring AI Document 后写入 Milvus。
     */
    @Override
    public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
    {
        if (StringUtils.isBlank(documentId))
        {
            throw new ServiceException("知识库文档标识不能为空");
        }
        if (chunks == null || chunks.isEmpty())
        {
            return List.of();
        }

        List<Document> documents = chunks.stream()
                .filter(chunk -> chunk != null && StringUtils.isNotBlank(chunk.getContent()))
                .map(chunk -> new Document(chunk.getChunkId(), chunk.getContent(), buildMetadata(documentId, fileName, chunk)))
                .toList();

        if (documents.isEmpty())
        {
            return List.of();
        }

        addDocumentsInBatches(documents);
        return documents.stream().map(Document::getId).toList();
    }

    /**
     * DashScope embedding 单次最多处理 10 条文本，写入 Milvus 前按模型限制拆批。
     */
    private void addDocumentsInBatches(List<Document> documents)
    {
        for (int start = 0; start < documents.size(); start += MAX_EMBEDDING_BATCH_SIZE)
        {
            int end = Math.min(start + MAX_EMBEDDING_BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
    }

    /**
     * 按用户问题执行 Milvus 相似度检索，并转换为项目内 RAG 记录。
     */
    @Override
    public List<AiVectorRecord> search(String query, int topK, double minScore)
    {
        if (StringUtils.isBlank(query))
        {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Math.max(topK, 1))
                .similarityThreshold(minScore)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toVectorRecord)
                .toList();
    }

    /**
     * 删除指定向量 ID，清理文档删除后的可检索内容。
     */
    @Override
    public void deleteVectors(List<String> vectorIds)
    {
        List<String> normalizedIds = vectorIds == null ? List.of()
                : vectorIds.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (normalizedIds.isEmpty())
        {
            return;
        }

        try
        {
            vectorStore.delete(normalizedIds);
        }
        catch (UnsupportedOperationException e)
        {
            throw new ServiceException("当前向量存储实现不支持按 ID 删除向量");
        }
        catch (Exception e)
        {
            throw new ServiceException("删除知识向量失败");
        }
    }

    /**
     * 为向量文档构建可回溯到知识库文档和切片的元数据。
     */
    private Map<String, Object> buildMetadata(String documentId, String fileName, AiDocumentChunk chunk)
    {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(METADATA_DOCUMENT_ID, documentId);
        metadata.put(METADATA_SOURCE_TITLE, fileName);
        metadata.put(METADATA_CHUNK_ID, chunk.getChunkId());
        metadata.put(METADATA_CHUNK_INDEX, chunk.getChunkIndex());
        return metadata;
    }

    /**
     * 将 Spring AI 检索结果转换为项目统一的 RAG 来源记录。
     */
    private AiVectorRecord toVectorRecord(Document document)
    {
        Map<String, Object> metadata = document.getMetadata();
        AiVectorRecord record = new AiVectorRecord();
        record.setVectorId(document.getId());
        record.setDocumentId(asString(metadata.get(METADATA_DOCUMENT_ID)));
        record.setChunkId(asString(metadata.get(METADATA_CHUNK_ID)));
        record.setChunkIndex(asInteger(metadata.get(METADATA_CHUNK_INDEX)));
        record.setSourceTitle(asString(metadata.get(METADATA_SOURCE_TITLE)));
        record.setContent(document.getText());
        record.setScore(document.getScore());
        record.setMetadata(metadata);
        return record;
    }

    /**
     * 兼容不同元数据类型，统一转换为字符串。
     */
    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 兼容数字和字符串形式的切片序号。
     */
    private Integer asInteger(Object value)
    {
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        if (value == null)
        {
            return null;
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
