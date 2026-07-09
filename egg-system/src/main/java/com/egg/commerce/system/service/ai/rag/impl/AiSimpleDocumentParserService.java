package com.egg.commerce.system.service.ai.rag.impl;

import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiDocumentParserService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AiSimpleDocumentParserService implements AiDocumentParserService
{
    private final int chunkSize;

    private final int chunkOverlap;

    public AiSimpleDocumentParserService(Environment environment)
    {
        this(getIntProperty(environment, "egg.ai.rag.chunk-size", 800),
                getIntProperty(environment, "egg.ai.rag.chunk-overlap", 120));
    }

    public AiSimpleDocumentParserService(int chunkSize, int chunkOverlap)
    {
        this.chunkSize = Math.max(chunkSize, 1);
        this.chunkOverlap = Math.max(Math.min(chunkOverlap, this.chunkSize - 1), 0);
    }

    @Override
    public List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content)
    {
        // This fallback keeps local tests and plain-text uploads working; richer parsing can plug in later behind the same interface.
        String text = new String(content == null ? new byte[0] : content, StandardCharsets.UTF_8).trim();
        if (StringUtils.isBlank(text))
        {
            return List.of();
        }

        List<AiDocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        int cursor = 0;
        while (cursor < text.length())
        {
            int end = Math.min(cursor + chunkSize, text.length());
            AiDocumentChunk chunk = new AiDocumentChunk();
            chunk.setChunkId(UUID.randomUUID().toString());
            chunk.setContent(text.substring(cursor, end));
            chunk.setChunkIndex(index++);
            chunks.add(chunk);
            if (end == text.length())
            {
                break;
            }
            cursor = Math.max(end - chunkOverlap, cursor + 1);
        }
        return chunks;
    }

    private static int getIntProperty(Environment environment, String key, int defaultValue)
    {
        Integer value = environment.getProperty(key, Integer.class);
        return value == null ? defaultValue : value;
    }
}
