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
import org.springframework.stereotype.Service;

@Service
public class AiSpringMilvusVectorStoreServiceImpl implements AiVectorStoreService
{
    private static final String METADATA_DOCUMENT_ID = "documentId";

    private static final String METADATA_SOURCE_TITLE = "sourceTitle";

    private static final String METADATA_CHUNK_ID = "chunkId";

    private static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private final VectorStore vectorStore;

    public AiSpringMilvusVectorStoreServiceImpl(VectorStore vectorStore)
    {
        this.vectorStore = vectorStore;
    }

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

        vectorStore.add(documents);
        return documents.stream().map(Document::getId).toList();
    }

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

    private Map<String, Object> buildMetadata(String documentId, String fileName, AiDocumentChunk chunk)
    {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(METADATA_DOCUMENT_ID, documentId);
        metadata.put(METADATA_SOURCE_TITLE, fileName);
        metadata.put(METADATA_CHUNK_ID, chunk.getChunkId());
        metadata.put(METADATA_CHUNK_INDEX, chunk.getChunkIndex());
        return metadata;
    }

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

    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

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
