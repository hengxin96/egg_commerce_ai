package com.egg.commerce.system.service.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.system.service.ai.rag.impl.AiSimpleDocumentParserService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * 文档解析服务测试，确认纯文本能按顺序切分为 RAG 知识片段。
 */
class AiDocumentParserServiceTest
{
    /**
     * 验证纯文本解析器会按顺序切分非空知识片段。
     */
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

    /**
     * 验证 Word 文档会先提取正文再切片，避免把 docx 的 ZIP 二进制内容发送给 embedding 模型。
     */
    @Test
    void parseAndSplitExtractsDocxTextBeforeChunking() throws IOException
    {
        AiSimpleDocumentParserService parser = new AiSimpleDocumentParserService(100, 10);

        List<AiDocumentChunk> chunks = parser.parseAndSplit("rules.docx", createDocxBytes("危险化学品安全管理要求"));

        String joinedContent = chunks.stream().map(AiDocumentChunk::getContent).collect(Collectors.joining("\n"));
        assertFalse(chunks.isEmpty());
        assertFalse(chunks.get(0).getContent().startsWith("PK"));
        assertTrue(joinedContent.contains("危险化学品安全管理要求"));
    }

    /**
     * 构造最小 docx 测试文档，确保测试覆盖真实 Office 文件解析路径。
     */
    private byte[] createDocxBytes(String text) throws IOException
    {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
