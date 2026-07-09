package com.egg.commerce.system.service.ai.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.mapper.ai.knowledge.AiKnowledgeDocumentMapper;
import com.egg.commerce.system.service.ai.knowledge.impl.AiKnowledgeVectorizationServiceImpl;
import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiDocumentParserService;
import com.egg.commerce.system.service.ai.rag.AiVectorRecord;
import com.egg.commerce.system.service.ai.rag.AiVectorStoreService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AiKnowledgeVectorizationServiceImplTest
{
    @Test
    void vectorizationCompletesDocumentAndStoresVectorIds()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId("doc-1");
        document.setFileName("rules.txt");
        mapper.document = document;
        FakeParser parser = new FakeParser();
        FakeVectorStore vectorStore = new FakeVectorStore();
        AiKnowledgeVectorizationServiceImpl service = new AiKnowledgeVectorizationServiceImpl(mapper, parser,
                vectorStore, new ImmediateThreadPoolTaskExecutor());

        service.startVectorization("doc-1",
                new MockMultipartFile("file", "rules.txt", "text/plain", "rules".getBytes()));

        assertEquals("completed", mapper.document.getStatus());
        assertEquals("[\"v1\",\"v2\"]", mapper.document.getVectorIds());
        assertEquals(2, mapper.document.getChunkCount());
        assertNotNull(mapper.document.getCompletedAt());
    }

    @Test
    void vectorizationMarksDocumentFailedWhenParsingFails()
    {
        FakeKnowledgeMapper mapper = new FakeKnowledgeMapper();
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId("doc-1");
        document.setFileName("rules.txt");
        mapper.document = document;
        FakeParser parser = new FakeParser();
        parser.failure = new IllegalStateException("parse failed");
        AiKnowledgeVectorizationServiceImpl service = new AiKnowledgeVectorizationServiceImpl(mapper, parser,
                new FakeVectorStore(), new ImmediateThreadPoolTaskExecutor());
        Logger logger = (Logger) LoggerFactory.getLogger(AiKnowledgeVectorizationServiceImpl.class);
        Level originalLevel = logger.getLevel();

        try
        {
            logger.setLevel(Level.OFF);
            service.startVectorization("doc-1",
                    new MockMultipartFile("file", "rules.txt", "text/plain", "rules".getBytes()));
        }
        finally
        {
            logger.setLevel(originalLevel);
        }

        assertEquals("failed", mapper.document.getStatus());
        assertTrue(mapper.document.getFailureReason().contains("parse failed"));
    }

    private static class ImmediateThreadPoolTaskExecutor extends ThreadPoolTaskExecutor
    {
        @Override
        public Future<?> submit(Runnable task)
        {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class FakeKnowledgeMapper implements AiKnowledgeDocumentMapper
    {
        private AiKnowledgeDocument document;

        @Override
        public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
        {
            return document;
        }

        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentByIds(String[] ids)
        {
            return document == null ? Collections.emptyList() : List.of(document);
        }

        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
        {
            return document == null ? Collections.emptyList() : List.of(document);
        }

        @Override
        public int insertAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            this.document = document;
            return 1;
        }

        @Override
        public int updateAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            this.document = document;
            return 1;
        }

        @Override
        public int deleteAiKnowledgeDocumentByIds(String[] ids)
        {
            document = null;
            return 1;
        }
    }

    private static class FakeParser implements AiDocumentParserService
    {
        private RuntimeException failure;

        @Override
        public List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content)
        {
            if (failure != null)
            {
                throw failure;
            }
            List<AiDocumentChunk> chunks = new ArrayList<>();
            chunks.add(chunk("c1", 0));
            chunks.add(chunk("c2", 1));
            return chunks;
        }

        private AiDocumentChunk chunk(String id, int index)
        {
            AiDocumentChunk chunk = new AiDocumentChunk();
            chunk.setChunkId(id);
            chunk.setChunkIndex(index);
            chunk.setContent("content-" + index);
            return chunk;
        }
    }

    private static class FakeVectorStore implements AiVectorStoreService
    {
        @Override
        public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
        {
            return List.of("v1", "v2");
        }

        @Override
        public List<AiVectorRecord> search(String query, int topK, double minScore)
        {
            return Collections.emptyList();
        }

        @Override
        public void deleteVectors(List<String> vectorIds)
        {
        }
    }
}
