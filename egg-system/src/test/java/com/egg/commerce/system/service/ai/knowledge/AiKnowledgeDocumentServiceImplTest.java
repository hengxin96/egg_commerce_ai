package com.egg.commerce.system.service.ai.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDownloadUrl;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.impl.AiKnowledgeDocumentServiceImpl;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档服务测试，覆盖上传校验、下载地址、重试和清理流程。
 */
class AiKnowledgeDocumentServiceImplTest
{
    /**
     * 验证单文件上传会创建处理中记录并启动向量化。
     */
    @Test
    void uploadCreatesProcessingDocumentAndStartsVectorization()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);

        AiKnowledgeDocument document = service.upload(
                new MockMultipartFile("file", "rules.txt", "text/plain", "规则".getBytes()), 100L);

        assertNotNull(document.getId());
        assertEquals("processing", document.getStatus());
        assertEquals("rules.txt", document.getFileName());
        assertEquals("knowledge-bucket", document.getOssBucket());
        assertEquals("admin", document.getAccessRoles());
        assertEquals(document.getId(), vectorization.startedDocumentId);
        assertEquals("rules.txt", vectorization.startedFileName);
        assertTrue(mapper.documents.containsKey(document.getId()));
    }

    /**
     * 验证批量上传会为每个合法文件创建文档记录。
     */
    @Test
    void uploadBatchCreatesDocumentForEachAcceptedFile()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);

        List<AiKnowledgeDocument> documents = service.uploadBatch(new MultipartFile[] {
                new MockMultipartFile("files", "rules.pdf", "application/pdf", "pdf".getBytes()),
                new MockMultipartFile("files", "guide.md", "text/markdown", "markdown".getBytes())
        }, 100L);

        assertEquals(2, documents.size());
        assertEquals(2, mapper.documents.size());
        assertEquals(List.of("admin", "admin"), documents.stream().map(AiKnowledgeDocument::getAccessRoles).toList());
        assertEquals(List.of("rules.pdf", "guide.md"), vectorization.startedFileNames);
    }

    /**
     * 验证批量修改知识库访问角色时会标准化角色并始终保留 admin。
     */
    @Test
    void updateAccessRolesNormalizesRolesAndAlwaysKeepsAdmin()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        AiKnowledgeDocument first = new AiKnowledgeDocument();
        first.setId("doc-1");
        first.setAccessRoles("admin");
        AiKnowledgeDocument second = new AiKnowledgeDocument();
        second.setId("doc-2");
        second.setAccessRoles("admin");
        mapper.documents.put(first.getId(), first);
        mapper.documents.put(second.getId(), second);

        int updated = service.updateAccessRoles(new String[] { "doc-1", "doc-2" }, List.of(" common ", "admin"));

        assertEquals(2, updated);
        assertEquals("admin,common", mapper.documents.get("doc-1").getAccessRoles());
        assertEquals("admin,common", mapper.documents.get("doc-2").getAccessRoles());
    }

    /**
     * 验证普通角色只能检索已完成且授权给 common 的知识库文档。
     */
    @Test
    void selectAccessibleCompletedDocumentIdsFiltersByCommonRoleAndCompletedStatus()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        mapper.documents.put("admin-doc", document("admin-doc", "completed", "admin"));
        mapper.documents.put("common-doc", document("common-doc", "completed", "admin,common"));
        mapper.documents.put("processing-common-doc", document("processing-common-doc", "processing", "admin,common"));

        List<String> accessibleIds = service.selectAccessibleCompletedDocumentIds(List.of("common"));

        assertEquals(List.of("common-doc"), accessibleIds);
    }

    /**
     * 验证 admin 角色可以检索所有已完成知识库文档。
     */
    @Test
    void selectAccessibleCompletedDocumentIdsReturnsAllCompletedDocumentsForAdminRole()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        mapper.documents.put("admin-doc", document("admin-doc", "completed", "admin"));
        mapper.documents.put("common-doc", document("common-doc", "completed", "admin,common"));
        mapper.documents.put("failed-doc", document("failed-doc", "failed", "admin,common"));

        List<String> accessibleIds = service.selectAccessibleCompletedDocumentIds(List.of("admin"));

        assertEquals(List.of("admin-doc", "common-doc"), accessibleIds);
    }

    /**
     * 验证空文件会在进入存储前被拒绝。
     */
    @Test
    void uploadRejectsEmptyFile()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.upload(new MockMultipartFile("file", new byte[0]), 100L));

        assertEquals("知识库文件不能为空", exception.getMessage());
    }

    /**
     * 验证不支持的知识库文件扩展名会被拒绝。
     */
    @Test
    void uploadRejectsUnsupportedKnowledgeFileExtension()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.upload(new MockMultipartFile("file", "video.mp4", "video/mp4", "video".getBytes()),
                        100L));

        assertEquals("仅支持上传 pdf、doc、docx、md、xls、xlsx、txt 格式的知识库文件", exception.getMessage());
        assertTrue(mapper.documents.isEmpty());
    }

    /**
     * 验证超过 100MB 的知识库文件会被拒绝。
     */
    @Test
    void uploadRejectsKnowledgeFileLargerThan100Mb()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);

        MultipartFile oversizedFile = new SizedMultipartFile("huge.pdf", "application/pdf", 100L * 1024L * 1024L + 1L);
        ServiceException exception = assertThrows(ServiceException.class, () -> service.upload(oversizedFile, 100L));

        assertEquals("知识库文件不能超过 100MB", exception.getMessage());
        assertTrue(mapper.documents.isEmpty());
    }

    /**
     * 验证下载地址查询只返回文档 ID、文件名和 OSS 地址。
     */
    @Test
    void selectDownloadUrlsReturnsStoredOssUrlsForExistingDocuments()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        AiKnowledgeDocument existing = new AiKnowledgeDocument();
        existing.setId("doc-1");
        existing.setFileName("rules.pdf");
        existing.setOssUrl("https://example.com/knowledge/rules.pdf");
        mapper.documents.put(existing.getId(), existing);

        List<AiKnowledgeDownloadUrl> urls = service.selectDownloadUrls(new String[] { "doc-1" });

        assertEquals(1, urls.size());
        assertEquals("doc-1", urls.get(0).getId());
        assertEquals("rules.pdf", urls.get(0).getFileName());
        assertEquals("https://example.com/knowledge/rules.pdf", urls.get(0).getUrl());
    }

    /**
     * 验证重试向量化会把失败文档重置为处理中状态。
     */
    @Test
    void retryVectorizationResetsExistingDocumentToProcessing()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        AiKnowledgeDocument existing = new AiKnowledgeDocument();
        existing.setId("doc-1");
        existing.setFileName("rules.txt");
        existing.setOssUrl("https://example.com/knowledge/rules.txt");
        existing.setStatus("failed");
        existing.setFailureReason("parse error");
        mapper.documents.put(existing.getId(), existing);

        service.retryVectorization("doc-1");

        assertEquals("processing", existing.getStatus());
        assertNull(existing.getFailureReason());
        assertEquals("doc-1", vectorization.startedDocumentId);
        assertEquals("rules.txt", vectorization.startedFileName);
    }

    /**
     * 验证删除文档前会先清理向量和 OSS 源对象。
     */
    @Test
    void deleteRemovesVectorsAndOssObjectBeforeMetadata()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorStore vectorStore = new FakeVectorStore();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorStore,
                vectorization);
        AiKnowledgeDocument existing = new AiKnowledgeDocument();
        existing.setId("doc-1");
        existing.setOssObjectKey("knowledge/rules.txt");
        existing.setVectorIds("[\"v1\",\"v2\"]");
        mapper.documents.put(existing.getId(), existing);

        int deleted = service.deleteAiKnowledgeDocumentByIds(new String[] { "doc-1" });

        assertEquals(1, deleted);
        assertEquals(List.of("v1", "v2"), vectorStore.deletedVectorIds);
        assertEquals(List.of("knowledge/rules.txt"), storage.deletedObjectKeys);
        assertNull(mapper.documents.get("doc-1"));
    }

    private static class FakeKnowledgeMapper implements AiKnowledgeDocumentMapper
    {
        private final Map<String, AiKnowledgeDocument> documents = new LinkedHashMap<>();

        /**
         * 从内存 Map 中按 ID 返回文档。
         */
        @Override
        public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
        {
            return documents.get(id);
        }

        /**
         * 按 ID 数组返回存在的文档列表。
         */
        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentByIds(String[] ids)
        {
            List<AiKnowledgeDocument> selected = new ArrayList<>();
            for (String id : ids)
            {
                AiKnowledgeDocument document = documents.get(id);
                if (document != null)
                {
                    selected.add(document);
                }
            }
            return selected;
        }

        /**
         * 返回所有内存文档，测试中不额外模拟查询条件。
         */
        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
        {
            return new ArrayList<>(documents.values());
        }

        /**
         * 捕获新插入的知识库文档记录。
         */
        @Override
        public int insertAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            documents.put(document.getId(), document);
            return 1;
        }

        /**
         * 用最新文档对象覆盖内存记录。
         */
        @Override
        public int updateAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            documents.put(document.getId(), document);
            return 1;
        }

        /**
         * 批量更新内存文档的访问角色。
         */
        @Override
        public int updateAiKnowledgeDocumentAccessRoles(String[] ids, String accessRoles)
        {
            int count = 0;
            for (String id : ids)
            {
                AiKnowledgeDocument document = documents.get(id);
                if (document != null)
                {
                    document.setAccessRoles(accessRoles);
                    count++;
                }
            }
            return count;
        }

        /**
         * 返回所有已完成文档 ID。
         */
        @Override
        public List<String> selectCompletedDocumentIds()
        {
            return documents.values().stream()
                    .filter(document -> "completed".equals(document.getStatus()))
                    .map(AiKnowledgeDocument::getId)
                    .toList();
        }

        /**
         * 按访问角色返回已完成文档 ID。
         */
        @Override
        public List<String> selectCompletedDocumentIdsByAccessRoles(List<String> roleKeys)
        {
            return documents.values().stream()
                    .filter(document -> "completed".equals(document.getStatus()))
                    .filter(document -> hasAnyRole(document.getAccessRoles(), roleKeys))
                    .map(AiKnowledgeDocument::getId)
                    .toList();
        }

        /**
         * 从内存 Map 删除指定文档并返回删除数量。
         */
        @Override
        public int deleteAiKnowledgeDocumentByIds(String[] ids)
        {
            int count = 0;
            for (String id : ids)
            {
                if (documents.remove(id) != null)
                {
                    count++;
                }
            }
            return count;
        }

        private boolean hasAnyRole(String accessRoles, List<String> roleKeys)
        {
            if (accessRoles == null || roleKeys == null)
            {
                return false;
            }
            for (String accessRole : accessRoles.split(","))
            {
                for (String roleKey : roleKeys)
                {
                    if (accessRole.trim().equals(roleKey))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private final List<String> deletedObjectKeys = new ArrayList<>();

        /**
         * 返回固定 OSS 元数据，避免测试访问真实对象存储。
         */
        @Override
        public AiStoredObject uploadKnowledgeFile(MultipartFile file)
        {
            AiStoredObject storedObject = new AiStoredObject();
            storedObject.setBucket("knowledge-bucket");
            storedObject.setObjectKey("knowledge/rules.txt");
            storedObject.setUrl("https://example.com/knowledge/rules.txt");
            storedObject.setSize(file.getSize());
            storedObject.setMimeType(file.getContentType());
            return storedObject;
        }

        /**
         * 知识库测试不覆盖商品源图上传。
         */
        @Override
        public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId)
        {
            throw new UnsupportedOperationException("知识库测试不覆盖商品源图上传");
        }

        /**
         * 知识库测试不覆盖生成图上传。
         */
        @Override
        public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId)
        {
            throw new UnsupportedOperationException("知识库测试不覆盖生成图上传");
        }

        /**
         * 知识库测试不覆盖商品图对象复制。
         */
        @Override
        public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId)
        {
            throw new UnsupportedOperationException("知识库测试不覆盖商品图对象复制");
        }

        /**
         * 知识库测试不覆盖商品图对象列表。
         */
        @Override
        public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan)
        {
            return Collections.emptyList();
        }

        /**
         * 知识库测试不覆盖 OSS 对象下载。
         */
        @Override
        public byte[] downloadObject(String objectKey)
        {
            throw new UnsupportedOperationException("知识库测试不覆盖 OSS 对象下载");
        }

        /**
         * 记录服务层请求删除的对象键。
         */
        @Override
        public String presignObjectUrl(String objectKey, long expireSeconds)
        {
            throw new UnsupportedOperationException("knowledge tests do not presign product images");
        }

        @Override
        public void deleteObject(String objectKey)
        {
            deletedObjectKeys.add(objectKey);
        }
    }

    private static class FakeVectorStore implements AiVectorStoreService
    {
        private List<String> deletedVectorIds = new ArrayList<>();

        /**
         * 文档服务测试不覆盖向量写入，返回空 ID 列表。
         */
        @Override
        public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
        {
            return Collections.emptyList();
        }

        /**
         * 文档服务测试不覆盖检索行为。
         */
        @Override
        public List<AiVectorRecord> search(String query, int topK, double minScore)
        {
            return Collections.emptyList();
        }

        /**
         * 记录服务层请求删除的向量 ID。
         */
        @Override
        public void deleteVectors(List<String> vectorIds)
        {
            deletedVectorIds = new ArrayList<>(vectorIds);
        }
    }

    private static class FakeVectorization implements AiKnowledgeVectorizationService
    {
        private String startedDocumentId;

        private String startedFileName;

        private final List<String> startedFileNames = new ArrayList<>();

        /**
         * 捕获从上传文件触发的向量化请求。
         */
        @Override
        public void startVectorization(String documentId, MultipartFile file)
        {
            startedDocumentId = documentId;
            startedFileName = file.getOriginalFilename();
            startedFileNames.add(startedFileName);
        }

        /**
         * 捕获从已有文档重试触发的向量化请求。
         */
        @Override
        public void startVectorization(AiKnowledgeDocument document)
        {
            startedDocumentId = document.getId();
            startedFileName = document.getFileName();
            startedFileNames.add(startedFileName);
        }
    }

    private static AiKnowledgeDocument document(String id, String status, String accessRoles)
    {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId(id);
        document.setStatus(status);
        document.setAccessRoles(accessRoles);
        return document;
    }

    private static class SizedMultipartFile implements MultipartFile
    {
        private final String originalFilename;

        private final String contentType;

        private final long size;

        /**
         * 构造只覆盖大小判断的 MultipartFile 测试桩。
         */
        private SizedMultipartFile(String originalFilename, String contentType, long size)
        {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.size = size;
        }

        @Override
        public String getName()
        {
            return "file";
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
            return false;
        }

        @Override
        public long getSize()
        {
            return size;
        }

        @Override
        public byte[] getBytes()
        {
            return new byte[0];
        }

        @Override
        public InputStream getInputStream()
        {
            return new ByteArrayInputStream(new byte[0]);
        }

        /**
         * 测试不需要真实落盘，保留空实现。
         */
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException
        {
        }
    }
}
