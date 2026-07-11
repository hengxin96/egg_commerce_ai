package com.egg.commerce.system.service.ai.rag.impl;

import com.egg.commerce.system.service.ai.rag.AiDocumentChunk;
import com.egg.commerce.system.service.ai.rag.AiDocumentParserService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AiSimpleDocumentParserService implements AiDocumentParserService
{
    private final int chunkSize;

    private final int chunkOverlap;

    /**
     * Spring 构造函数，从环境变量读取切片大小和重叠长度。
     */
    @Autowired
    public AiSimpleDocumentParserService(Environment environment)
    {
        this(getIntProperty(environment, "egg.ai.rag.chunk-size", 800),
                getIntProperty(environment, "egg.ai.rag.chunk-overlap", 120));
    }

    /**
     * 测试构造函数，允许用小切片参数验证切分行为。
     */
    public AiSimpleDocumentParserService(int chunkSize, int chunkOverlap)
    {
        this.chunkSize = Math.max(chunkSize, 1);
        this.chunkOverlap = Math.max(Math.min(chunkOverlap, this.chunkSize - 1), 0);
    }

    /**
     * 将纯文本内容按配置切分为有序知识片段。
     */
    @Override
    public List<AiDocumentChunk> parseAndSplit(String fileName, byte[] content)
    {
        // 本地测试和纯文本上传使用该兜底解析器，更完整的解析器可以继续复用同一接口。
        String text = extractText(fileName, content);
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

    /**
     * 根据文件类型提取可发送给 embedding 模型的纯文本，Office/PDF 不能直接按二进制字节入库。
     */
    private String extractText(String fileName, byte[] content)
    {
        byte[] safeContent = content == null ? new byte[0] : content;
        String extension = StringUtils.substringAfterLast(StringUtils.defaultString(fileName), ".")
                .toLowerCase(Locale.ROOT);
        if ("txt".equals(extension) || "md".equals(extension))
        {
            return new String(safeContent, StandardCharsets.UTF_8).trim();
        }

        try
        {
            // Tika 负责从 doc/docx/pdf/xls/xlsx 等知识库文件中抽取正文，避免向量模型收到 ZIP 或 Office 二进制内容。
            TikaDocumentReader reader = new TikaDocumentReader(new NamedByteArrayResource(safeContent, fileName));
            return reader.get().stream().map(Document::getText).filter(StringUtils::isNotBlank)
                    .reduce("", (left, right) -> left + "\n" + right).trim();
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("解析知识库源文件失败，请确认文件未损坏且格式受支持", e);
        }
    }

    /**
     * 读取整数配置，缺省时返回本地可运行的默认值。
     */
    private static int getIntProperty(Environment environment, String key, int defaultValue)
    {
        Integer value = environment.getProperty(key, Integer.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 给内存字节补充文件名，帮助 Tika 按扩展名识别 Office/PDF 等文档类型。
     */
    private static class NamedByteArrayResource extends ByteArrayResource
    {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename)
        {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename()
        {
            return filename;
        }
    }
}
