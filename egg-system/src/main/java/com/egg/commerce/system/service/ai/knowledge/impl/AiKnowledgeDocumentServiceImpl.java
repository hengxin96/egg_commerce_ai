package com.egg.commerce.system.service.ai.knowledge.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDownloadUrl;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeDocumentService;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeVectorizationService;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台“我的知识库”页面的知识文档服务。
 *
 * <p>该服务负责 OSS 存储、元数据持久化、异步 RAG 向量化、下载地址查询和清理等文档生命周期。
 * 解析与向量写入委托给 {@link AiKnowledgeVectorizationService}，保持存储和 RAG 实现可替换。</p>
 */
@Service
public class AiKnowledgeDocumentServiceImpl implements AiKnowledgeDocumentService
{
    private static final String STATUS_PROCESSING = "processing";

    /** 后台上传先完成文件校验，避免不支持的文件进入 RAG 流程。 */
    private static final long MAX_KNOWLEDGE_FILE_SIZE = 100L * 1024L * 1024L;

    private static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of("pdf", "doc", "docx", "md", "xls", "xlsx",
            "txt");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    private final AiObjectStorageService objectStorageService;

    private final AiVectorStoreService vectorStoreService;

    private final AiKnowledgeVectorizationService vectorizationService;

    @Value("${egg.ai.rag.vector-store}")
    private String vectorStoreName;

    /**
     * 注入文档元数据、对象存储、向量库和异步向量化服务。
     */
    @Autowired
    public AiKnowledgeDocumentServiceImpl(AiKnowledgeDocumentMapper knowledgeDocumentMapper,
            AiObjectStorageService objectStorageService,
            AiVectorStoreService vectorStoreService,
            AiKnowledgeVectorizationService vectorizationService)
    {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.objectStorageService = objectStorageService;
        this.vectorStoreService = vectorStoreService;
        this.vectorizationService = vectorizationService;
    }

    /**
     * 查询后台知识库文档列表，具体筛选条件由 mapper XML 维护。
     */
    @Override
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
    {
        return knowledgeDocumentMapper.selectAiKnowledgeDocumentList(query);
    }

    /**
     * 按 ID 查询知识库文档元数据。
     */
    @Override
    public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
    {
        return knowledgeDocumentMapper.selectAiKnowledgeDocumentById(id);
    }

    /**
     * 上传单个知识库文件，保存 OSS 与数据库元数据后触发异步向量化。
     */
    @Override
    public AiKnowledgeDocument upload(MultipartFile file, Long createdBy)
    {
        validateKnowledgeFile(file);

        AiStoredObject storedObject = objectStorageService.uploadKnowledgeFile(file);
        AiKnowledgeDocument document = buildDocument(file, storedObject, createdBy);
        knowledgeDocumentMapper.insertAiKnowledgeDocument(document);

        // 保留与请求无关的文件副本，因为向量化会在 HTTP 请求回收后继续执行。
        vectorizationService.startVectorization(document.getId(), createVectorizationFile(file));
        return document;
    }

    /**
     * 批量上传知识库文件，先整体校验再逐个进入单文件上传流程。
     */
    @Override
    public List<AiKnowledgeDocument> uploadBatch(MultipartFile[] files, Long createdBy)
    {
        if (files == null || files.length == 0)
        {
            throw new ServiceException("请选择要上传的知识库文件");
        }
        for (MultipartFile file : files)
        {
            validateKnowledgeFile(file);
        }

        List<AiKnowledgeDocument> documents = new ArrayList<>();
        for (MultipartFile file : files)
        {
            documents.add(upload(file, createdBy));
        }
        return documents;
    }

    /**
     * 查询选中文档的下载地址，仅返回前端下载所需的窄视图。
     */
    @Override
    public List<AiKnowledgeDownloadUrl> selectDownloadUrls(String[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return Collections.emptyList();
        }
        List<AiKnowledgeDocument> documents = knowledgeDocumentMapper.selectAiKnowledgeDocumentByIds(ids);
        List<AiKnowledgeDownloadUrl> urls = new ArrayList<>();
        for (AiKnowledgeDocument document : documents)
        {
            if (StringUtils.isBlank(document.getOssUrl()))
            {
                throw new ServiceException("知识库文件缺少 OSS 下载地址");
            }
            AiKnowledgeDownloadUrl url = new AiKnowledgeDownloadUrl();
            url.setId(document.getId());
            url.setFileName(document.getFileName());
            url.setUrl(document.getOssUrl());
            urls.add(url);
        }
        return urls;
    }

    /**
     * 删除知识库文档前先清理向量库和 OSS，避免残留可检索内容或源文件。
     */
    @Override
    public int deleteAiKnowledgeDocumentByIds(String[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        List<AiKnowledgeDocument> documents = knowledgeDocumentMapper.selectAiKnowledgeDocumentByIds(ids);
        for (AiKnowledgeDocument document : documents)
        {
            cleanupKnowledgeArtifacts(document);
        }
        return knowledgeDocumentMapper.deleteAiKnowledgeDocumentByIds(ids);
    }

    /**
     * 将失败文档重置为处理中状态，并基于已保存的 OSS 地址重新触发向量化。
     */
    @Override
    public void retryVectorization(String id)
    {
        AiKnowledgeDocument document = knowledgeDocumentMapper.selectAiKnowledgeDocumentById(id);
        if (document == null)
        {
            throw new ServiceException("知识库文档不存在");
        }
        if (StringUtils.isBlank(document.getOssUrl()))
        {
            throw new ServiceException("知识库文档缺少可重试的源文件地址");
        }

        document.setStatus(STATUS_PROCESSING);
        document.setFailureReason(null);
        document.setCompletedAt(null);
        document.setUpdatedAt(new Date());
        document.setChunkCount(0);
        document.setVectorIds("[]");
        knowledgeDocumentMapper.updateAiKnowledgeDocument(document);
        vectorizationService.startVectorization(document);
    }

    /**
     * OSS 上传后构建文档元数据记录；向量 ID 和最终状态由异步向量化回填。
     */
    private AiKnowledgeDocument buildDocument(MultipartFile file, AiStoredObject storedObject, Long createdBy)
    {
        Date now = new Date();
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId(UUID.randomUUID().toString());
        document.setFileName(file.getOriginalFilename());
        document.setFileExt(resolveExtension(file.getOriginalFilename()));
        document.setMimeType(StringUtils.defaultIfBlank(file.getContentType(), storedObject.getMimeType()));
        document.setFileSize(storedObject.getSize());
        document.setOssBucket(storedObject.getBucket());
        document.setOssObjectKey(storedObject.getObjectKey());
        document.setOssUrl(storedObject.getUrl());
        document.setVectorStore(StringUtils.defaultIfBlank(vectorStoreName, "local"));
        document.setVectorIds("[]");
        document.setChunkCount(0);
        document.setStatus(STATUS_PROCESSING);
        document.setCreatedBy(createdBy);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }

    /**
     * 校验知识库文件格式和大小，防止无效内容进入 OSS 与 RAG 流程。
     */
    private void validateKnowledgeFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("知识库文件不能为空");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (StringUtils.isBlank(extension) || !SUPPORTED_FILE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT)))
        {
            throw new ServiceException("仅支持上传 pdf、doc、docx、md、xls、xlsx、txt 格式的知识库文件");
        }
        if (file.getSize() > MAX_KNOWLEDGE_FILE_SIZE)
        {
            throw new ServiceException("知识库文件不能超过 100MB");
        }
    }

    /**
     * 创建请求无关的内存文件副本，确保异步任务不依赖 servlet 临时文件。
     */
    private MultipartFile createVectorizationFile(MultipartFile file)
    {
        try
        {
            return new AiInMemoryMultipartFile(file.getName(), file.getOriginalFilename(), file.getContentType(),
                    file.getBytes());
        }
        catch (IOException e)
        {
            throw new ServiceException("读取知识库文件失败");
        }
    }

    /**
     * 从文件名解析小写扩展名，用于格式校验和元数据保存。
     */
    private String resolveExtension(String fileName)
    {
        String extension = StringUtils.substringAfterLast(StringUtils.defaultString(fileName), ".");
        return StringUtils.isBlank(extension) ? null : extension.toLowerCase(Locale.ROOT);
    }

    /**
     * 清理单个知识库文档关联的向量记录和源文件对象。
     */
    private void cleanupKnowledgeArtifacts(AiKnowledgeDocument document)
    {
        if (document == null)
        {
            return;
        }
        List<String> vectorIds = parseVectorIds(document.getVectorIds());
        if (!vectorIds.isEmpty())
        {
            // 向量库没有数据库外键，删除文档时必须显式清理可检索切片。
            vectorStoreService.deleteVectors(vectorIds);
        }
        if (StringUtils.isNotBlank(document.getOssObjectKey()))
        {
            // 文档记录不再引用源文件后，同时移除 OSS 源对象。
            objectStorageService.deleteObject(document.getOssObjectKey());
        }
    }

    /**
     * 解析数据库中保存的向量 ID JSON，供删除流程逐条清理向量库。
     */
    private List<String> parseVectorIds(String vectorIds)
    {
        if (StringUtils.isBlank(vectorIds) || "[]".equals(vectorIds.trim()))
        {
            return Collections.emptyList();
        }
        try
        {
            return OBJECT_MAPPER.readValue(vectorIds, new TypeReference<List<String>>()
            {
            });
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("知识库向量标识格式不正确");
        }
    }

    /**
     * 用于异步向量化的不可变内存文件副本。
     */
    private static class AiInMemoryMultipartFile implements MultipartFile
    {
        private final String name;

        private final String originalFilename;

        private final String contentType;

        private final byte[] content;

        /**
         * 保存 multipart 文件的不可变字节副本，避免外部数组后续被修改。
         */
        private AiInMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content)
        {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content.clone();
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getOriginalFilename()
        {
            return originalFilename;
        }

        @Override
        public String getContentType()
        {
            return contentType;
        }

        @Override
        public boolean isEmpty()
        {
            return content.length == 0;
        }

        @Override
        public long getSize()
        {
            return content.length;
        }

        @Override
        public byte[] getBytes()
        {
            return content.clone();
        }

        @Override
        public InputStream getInputStream()
        {
            return new ByteArrayInputStream(content);
        }

        /**
         * 将内存文件内容写入目标文件，满足 MultipartFile 合约。
         */
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException
        {
            FileCopyUtils.copy(content, dest);
        }
    }
}
