package com.egg.commerce.system.service.ai.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskDetail;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskExportFile;
import com.egg.commerce.system.service.ai.story.impl.AiStoryTaskExportServiceImpl;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * 剧情任务导出服务测试，验证 Markdown 和 Word 文件都能完整承载固定节点内容。
 */
class AiStoryTaskExportServiceImplTest
{
    /**
     * 验证 Markdown 导出包含输入参数和全部阶段输出。
     */
    @Test
    void exportTaskAsMarkdownIncludesAllStorySections()
    {
        AiStoryTaskExportServiceImpl service = new AiStoryTaskExportServiceImpl();

        AiStoryTaskExportFile file = service.exportTask(task(), "md");

        String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
        assertEquals("text/markdown;charset=UTF-8", file.getContentType());
        assertTrue(file.getFileName().endsWith(".md"));
        assertTrue(markdown.contains("剧情主题"));
        assertTrue(markdown.contains("剧情内容"));
        assertTrue(markdown.contains("剧本内容"));
        assertTrue(markdown.contains("三视图提示词"));
    }

    /**
     * 验证 Word 导出能被 POI 解析，并且能读出关键节点内容。
     */
    @Test
    void exportTaskAsWordCreatesReadableDocx() throws Exception
    {
        AiStoryTaskExportServiceImpl service = new AiStoryTaskExportServiceImpl();

        AiStoryTaskExportFile file = service.exportTask(task(), "docx");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getBytes())))
        {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .collect(Collectors.joining("\n"));
            assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    file.getContentType());
            assertTrue(file.getFileName().endsWith(".docx"));
            assertTrue(text.contains("剧情生成结果"));
            assertTrue(text.contains("校验后分镜"));
            assertTrue(text.contains("三视图提示词"));
        }
    }

    /**
     * 验证未知导出格式会被明确拒绝。
     */
    @Test
    void exportTaskRejectsUnknownFormat()
    {
        AiStoryTaskExportServiceImpl service = new AiStoryTaskExportServiceImpl();

        ServiceException exception = assertThrows(ServiceException.class, () -> service.exportTask(task(), "pdf"));

        assertEquals("不支持的剧情导出格式", exception.getMessage());
    }

    /**
     * 构造包含全部节点内容的剧情任务详情。
     */
    private AiStoryTaskDetail task()
    {
        AiStoryTaskDetail task = new AiStoryTaskDetail();
        task.setId("61a57a0f-a4e2-495d-90be-e29efe1131ad");
        task.setStoryTheme("兄弟因为一万块钱争执");
        task.setProductInfo("三全手工鲜肉馄饨");
        task.setTargetPlatform("抖音");
        task.setTargetAudience("中老年人群");
        task.setStoryStyle("家庭伦理");
        task.setStoryDuration("60秒");
        task.setCurrentNode("完成");
        task.setStatus("completed");
        task.setStoryOutput("剧情内容");
        task.setScriptOutput("剧本内容");
        task.setStoryboardOutput("原始分镜");
        task.setStoryboardCheckOutput("校验后分镜");
        task.setVisualOutput("三视图提示词");
        return task;
    }
}
