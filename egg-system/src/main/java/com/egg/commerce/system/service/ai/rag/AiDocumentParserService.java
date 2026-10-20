package com.egg.commerce.system.service.ai.rag;

import java.util.List;

/**
 * 知识文档解析服务接口，统一文档读取、切片和后续解析器替换入口。
 */
public interface AiDocumentParserService
{
    /** 将知识库源文件解析并切分成后续可向量化的文本片段。 */
    List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content);
}
