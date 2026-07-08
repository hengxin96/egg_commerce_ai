package com.egg.commerce.system.service.ai.knowledge.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.IAiKnowledgeDocumentService;
import com.egg.commerce.system.service.ai.knowledge.IAiKnowledgeVectorizationService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiKnowledgeDocumentServiceImpl implements IAiKnowledgeDocumentService
{
    private static final String STATUS_PROCESSING = "processing";

    private final AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    private final AiObjectStorageService objectStorageService;

    private final IAiKnowledgeVectorizationService vectorizationService;

    public AiKnowledgeDocumentServiceImpl(AiKnowledgeDocumentMapper knowledgeDocumentMapper,
            AiObjectStorageService objectStorageService,
            IAiKnowledgeVectorizationService vectorizationService)
    {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.objectStorageService = objectStorageService;
        this.vectorizationService = vectorizationService;
    }

    @Override
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
    {
        return knowledgeDocumentMapper.selectAiKnowledgeDocumentList(query);
    }

    @Override
    public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
    {
        return knowledgeDocumentMapper.selectAiKnowledgeDocumentById(id);
    }

    @Override
    public AiKnowledgeDocument upload(MultipartFile file, Long createdBy)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("知识库文件不能为空");
        }

        AiStoredObject storedObject = objectStorageService.uploadKnowledgeFile(file);
        AiKnowledgeDocument document = buildDocument(file, storedObject, createdBy);
        knowledgeDocumentMapper.insertAiKnowledgeDocument(document);

        // Return after metadata is saved; async vectorization will update status and chunk data later.
        vectorizationService.startVectorization(document.getId(), createVectorizationFile(file));
        return document;
    }

    @Override
    public int deleteAiKnowledgeDocumentByIds(String[] ids)
    {
        return knowledgeDocumentMapper.deleteAiKnowledgeDocumentByIds(ids);
    }

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
        document.setVectorStore("milvus");
        document.setVectorIds("[]");
        document.setChunkCount(0);
        document.setStatus(STATUS_PROCESSING);
        document.setCreatedBy(createdBy);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }

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

    private String resolveExtension(String fileName)
    {
        String extension = StringUtils.substringAfterLast(StringUtils.defaultString(fileName), ".");
        return StringUtils.isBlank(extension) ? null : extension;
    }

    private static class AiInMemoryMultipartFile implements MultipartFile
    {
        private final String name;

        private final String originalFilename;

        private final String contentType;

        private final byte[] content;

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
        public java.io.InputStream getInputStream()
        {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException
        {
            org.springframework.util.FileCopyUtils.copy(content, dest);
        }
    }
}
