package com.egg.commerce.system.service.ai.rag;

import java.util.List;

public interface AiDocumentParserService
{
    List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content);
}
