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

/**
 * 知识库向量化服务测试，覆盖切片入库成功和解析失败状态回写。
 */
class AiKnowledgeVectorizationServiceImplTest
{
    /**
     * 验证向量化成功后会写回完成状态、向量 ID 和切片数量。
     */
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

    /**
     * 验证解析失败时会把文档标记为失败并保存失败原因。
     */
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
        /**
         * 立即执行异步任务，让测试可以同步断言状态回写。
         */
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

        /**
         * 返回测试当前持有的单个文档。
         */
        @Override
        public AiKnowledgeDocument selectAiKnowledgeDocumentById(String id)
        {
            return document;
        }

        /**
         * 返回单文档列表，满足 mapper 接口测试桩要求。
         */
        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentByIds(String[] ids)
        {
            return document == null ? Collections.emptyList() : List.of(document);
        }

        /**
         * 返回单文档列表，测试中不模拟筛选。
         */
        @Override
        public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query)
        {
            return document == null ? Collections.emptyList() : List.of(document);
        }

        /**
         * 保存测试文档引用。
         */
        @Override
        public int insertAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            this.document = document;
            return 1;
        }

        /**
         * 捕获向量化服务回写后的文档对象。
         */
        @Override
        public int updateAiKnowledgeDocument(AiKnowledgeDocument document)
        {
            this.document = document;
            return 1;
        }

        /**
         * 清空测试文档引用。
         */
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

        /**
         * 返回固定切片，或按测试设置抛出解析异常。
         */
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

        /**
         * 构造带 ID、序号和内容的测试切片。
         */
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
        /**
         * 返回固定向量 ID，模拟向量库写入成功。
         */
        @Override
        public List<String> addKnowledgeChunks(String documentId, String fileName, List<AiDocumentChunk> chunks)
        {
            return List.of("v1", "v2");
        }

        /**
         * 向量化测试不覆盖检索行为。
         */
        @Override
        public List<AiVectorRecord> search(String query, int topK, double minScore)
        {
            return Collections.emptyList();
        }

        /**
         * 向量化测试不覆盖删除行为。
         */
        @Override
        public void deleteVectors(List<String> vectorIds)
        {
        }
    }
}
