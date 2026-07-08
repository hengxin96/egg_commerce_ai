package com.egg.commerce.system.service.ai.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.impl.AiKnowledgeDocumentServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AiKnowledgeDocumentServiceImplTest
{
    @Test
    void uploadCreatesProcessingDocumentAndStartsVectorization()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorization);

        AiKnowledgeDocument document = service.upload(
                new MockMultipartFile("file", "rules.txt", "text/plain", "规则".getBytes()), 100L);

        assertNotNull(document.getId());
        assertEquals("processing", document.getStatus());
        assertEquals("rules.txt", document.getFileName());
        assertEquals("knowledge-bucket", document.getOssBucket());
        assertEquals(document.getId(), vectorization.startedDocumentId);
        assertEquals("rules.txt", vectorization.startedFileName);
        assertTrue(mapper.documents.containsKey(document.getId()));
    }

    @Test
    void uploadRejectsEmptyFile()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorization);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.upload(new MockMultipartFile("file", new byte[0]), 100L));

        assertEquals("知识库文件不能为空", exception.getMessage());
    }

    @Test
    void retryVectorizationResetsExistingDocumentToProcessing()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        FakeStorage storage = new FakeStorage();
        FakeVectorization vectorization = new FakeVectorization();
        AiKnowledgeDocumentServiceImpl service = new AiKnowledgeDocumentServiceImpl(mapper, storage, vectorization);
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

    private static class FakeKnowledgeMapper implements AiKnowledgeDocumentMapper
    {
        private final Map<String, AiKnowledgeDocument> documents = new LinkedHashMap<>();

        @Override
        public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
        {
            return documents.get(id);
        }

        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
        {
            return new ArrayList<>(documents.values());
        }

        @Override
        public int insertAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            documents.put(document.getId(), document);
            return 1;
        }

        @Override
        public int updateAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            documents.put(document.getId(), document);
            return 1;
        }

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
    }

    private static class FakeStorage implements AiObjectStorageService
    {
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

        @Override
        public void deleteObject(String objectKey)
        {
        }
    }

    private static class FakeVectorization implements IAiKnowledgeVectorizationService
    {
        private String startedDocumentId;

        private String startedFileName;

        @Override
        public void startVectorization(String documentId, MultipartFile file)
        {
            startedDocumentId = documentId;
            startedFileName = file.getOriginalFilename();
        }

        @Override
        public void startVectorization(AiKnowledgeDocument document)
        {
            startedDocumentId = document.getId();
            startedFileName = document.getFileName();
        }
    }
}
