package com.egg.commerce.system.service.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.egg.commerce.system.service.ai.rag.impl.AiSpringMilvusVectorStoreServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Milvus 向量库服务测试，覆盖项目层对模型调用限制的保护逻辑。
 */
class AiSpringMilvusVectorStoreServiceImplTest
{
    /**
     * DashScope embedding 单次最多接收 10 条文本，写入 Milvus 前必须拆批。
     */
    @Test
    void addKnowledgeChunksSplitsDocumentsIntoDashScopeSizedBatches()
    {
        VectorStore vectorStore = mock(VectorStore.class);
        AiSpringMilvusVectorStoreServiceImpl service = new AiSpringMilvusVectorStoreServiceImpl(vectorStore);

        List<String> vectorIds = service.addKnowledgeChunks("doc-1", "rules.docx", chunks(23));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(3)).add(batchCaptor.capture());
        List<List<Document>> batches = batchCaptor.getAllValues();

        assertEquals(23, vectorIds.size());
        assertEquals(List.of(10, 10, 3), batches.stream().map(List::size).toList());
        assertTrue(batches.stream().allMatch(batch -> batch.size() <= 10));
    }

    /**
     * 授权文档 ID 必须进入 Milvus filterExpression，避免检索到无权限文档切片。
     */
    @Test
    void searchWithAccessibleDocumentIdsAddsDocumentIdFilter()
    {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        AiSpringMilvusVectorStoreServiceImpl service = new AiSpringMilvusVectorStoreServiceImpl(vectorStore);

        service.search("rules", 5, 0.7, List.of("doc-1", "doc-2", "doc-1", " "));

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        String filter = request.getFilterExpression().toString();
        assertEquals("rules", request.getQuery());
        assertTrue(request.hasFilterExpression());
        assertTrue(filter.contains("documentId"));
        assertTrue(filter.contains("doc-1"));
        assertTrue(filter.contains("doc-2"));
    }

    /**
     * 构造指定数量的知识切片，模拟较长文档解析后的入库输入。
     */
    private List<AiDocumentChunk> chunks(int count)
    {
        List<AiDocumentChunk> chunks = new ArrayList<>();
        for (int index = 0; index < count; index++)
        {
            AiDocumentChunk chunk = new AiDocumentChunk();
            chunk.setChunkId("chunk-" + index);
            chunk.setChunkIndex(index);
            chunk.setContent("content-" + index);
            chunks.add(chunk);
        }
        return chunks;
    }
}
