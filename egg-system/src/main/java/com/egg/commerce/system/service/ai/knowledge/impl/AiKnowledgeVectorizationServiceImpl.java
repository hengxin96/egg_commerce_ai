package com.egg.commerce.system.service.ai.knowledge.impl;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeVectorizationService;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiDocumentParserService;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiKnowledgeVectorizationServiceImpl implements AiKnowledgeVectorizationService
{
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeVectorizationServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    private final AiDocumentParserService documentParserService;

    private final AiVectorStoreService vectorStoreService;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public AiKnowledgeVectorizationServiceImpl(AiKnowledgeDocumentMapper knowledgeDocumentMapper,
            AiDocumentParserService documentParserService,
            AiVectorStoreService vectorStoreService,
            ThreadPoolTaskExecutor threadPoolTaskExecutor)
    {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentParserService = documentParserService;
        this.vectorStoreService = vectorStoreService;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @Override
    public void startVectorization(String documentId, MultipartFile file)
    {
        String fileName = file == null ? null : file.getOriginalFilename();
        byte[] bytes = readMultipartBytes(file);
        threadPoolTaskExecutor.submit(() -> processVectorization(documentId, fileName, bytes));
    }

    @Override
    public void startVectorization(AiKnowledgeDocument document)
    {
        if (document == null)
        {
            return;
        }
        threadPoolTaskExecutor.submit(() -> processVectorization(document.getId(), document.getFileName(),
                downloadBytes(document.getOssUrl())));
    }

    private void processVectorization(String documentId, String fileName, byte[] content)
    {
        try
        {
            List<AiDocumentChunk> chunks = documentParserService.parseAndSplit(fileName, content);
            List<String> vectorIds = vectorStoreService.addKnowledgeChunks(documentId, fileName, chunks);

            AiKnowledgeDocument document = knowledgeDocumentMapper.selectAiKnowledgeDocumentById(documentId);
            if (document == null)
            {
                return;
            }
            document.setStatus("completed");
            document.setFailureReason(null);
            document.setVectorIds(toJson(vectorIds));
            document.setChunkCount(chunks.size());
            document.setCompletedAt(new Date());
            document.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateAiKnowledgeDocument(document);
        }
        catch (Exception e)
        {
            log.error("Failed to vectorize knowledge document {}", documentId, e);
            AiKnowledgeDocument document = knowledgeDocumentMapper.selectAiKnowledgeDocumentById(documentId);
            if (document == null)
            {
                return;
            }
            document.setStatus("failed");
            document.setFailureReason(StringUtils.left(e.getMessage(), 500));
            document.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateAiKnowledgeDocument(document);
        }
    }

    private byte[] readMultipartBytes(MultipartFile file)
    {
        try
        {
            return file == null ? new byte[0] : file.getBytes();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("读取知识库文件失败", e);
        }
    }

    private byte[] downloadBytes(String ossUrl)
    {
        try (InputStream inputStream = new URL(ossUrl).openStream())
        {
            return inputStream.readAllBytes();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("下载知识库源文件失败", e);
        }
    }

    private String toJson(List<String> vectorIds)
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(vectorIds);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("序列化向量标识失败", e);
        }
    }
}
