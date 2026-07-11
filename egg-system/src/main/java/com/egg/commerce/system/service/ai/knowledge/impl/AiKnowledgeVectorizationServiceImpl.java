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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档向量化服务实现。
 *
 * <p>该服务负责把知识文件解析成文本块，再写入向量库，并把处理结果回写到文档记录。
 * 它运行在后台线程中，避免上传接口长时间阻塞管理员页面。</p>
 */
@Service
public class AiKnowledgeVectorizationServiceImpl implements AiKnowledgeVectorizationService
{
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeVectorizationServiceImpl.class);

    /** 将向量库返回的 ID 列表序列化到数据库 vector_ids 字段。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 文档 Mapper，用于读取和回写向量化状态。 */
    private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 文档解析器，负责把 PDF、Word、文本等文件解析并切成适合 embedding 的块。 */
    private final AiDocumentParserService documentParserService;

    /** 向量库服务，负责把文本块写入当前 RAG 检索库。 */
    private final AiVectorStoreService vectorStoreService;

    /** 后台线程池，避免上传/重试请求阻塞到文档解析和 embedding 完成。 */
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 注入向量化所需的文档元数据、解析器、向量库和异步线程池。
     */
    @Autowired
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

    /**
     * 从上传请求中的文件副本启动异步向量化任务。
     */
    @Override
    public void startVectorization(String documentId, MultipartFile file)
    {
        String fileName = file == null ? null : file.getOriginalFilename();
        // 立即读取请求文件内容，避免异步线程运行时 MultipartFile 临时流已经被容器回收。
        byte[] bytes = readMultipartBytes(file);
        threadPoolTaskExecutor.submit(() -> processVectorization(documentId, fileName, bytes));
    }

    /**
     * 从已保存文档的 OSS 地址重新启动向量化任务，主要用于失败重试。
     */
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

    /**
     * 执行解析、切片、向量入库和文档状态回写的核心流程。
     */
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
            // 向量写入成功后，文档才算可用于 RAG 检索。
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
            // 保留短失败原因给后台页面展示，完整堆栈写日志用于排查解析或向量库问题。
            document.setStatus("failed");
            document.setFailureReason(StringUtils.left(e.getMessage(), 500));
            document.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateAiKnowledgeDocument(document);
        }
    }

    /**
     * 读取上传文件字节，异步任务只持有稳定的 byte[] 输入。
     */
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

    /**
     * 从文档源地址下载字节内容，用于重试向量化已上传文件。
     */
    private byte[] downloadBytes(String ossUrl)
    {
        try (InputStream inputStream = new URL(ossUrl).openStream())
        {
            // 重试时从对象存储下载源文件，保证重新处理的是当初上传的原始内容。
            return inputStream.readAllBytes();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("下载知识库源文件失败", e);
        }
    }

    /**
     * 将向量 ID 列表序列化为 JSON，保存到文档元数据字段。
     */
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
