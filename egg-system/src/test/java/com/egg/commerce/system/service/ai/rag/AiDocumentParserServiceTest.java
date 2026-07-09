package com.egg.commerce.system.service.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.egg.commerce.system.service.ai.rag.impl.AiSimpleDocumentParserService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiDocumentParserServiceTest
{
    @Test
    void parseAndSplitSplitsPlainTextIntoOrderedChunks()
    {
        AiSimpleDocumentParserService parser = new AiSimpleDocumentParserService(12, 3);

        List<AiDocumentChunk> chunks = parser.parseAndSplit("rules.txt",
                "平台规则要求主图不能夸大功效".getBytes(StandardCharsets.UTF_8));

        assertFalse(chunks.isEmpty());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertFalse(chunks.get(0).getContent().isBlank());
    }
}
