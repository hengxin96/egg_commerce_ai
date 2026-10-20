package com.egg.commerce.system.service.ai.story.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskExportFile;
import com.egg.commerce.system.service.ai.story.AiStoryTaskExportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

/**
 * 剧情任务导出服务实现。
 *
 * <p>导出内容来源于 ai_story_tasks 宽表中的固定节点字段，完成页和任务中心详情页可以复用同一份文件生成逻辑。</p>
 */
@Service
public class AiStoryTaskExportServiceImpl implements AiStoryTaskExportService
{
    /** Markdown 文件内容类型。 */
    private static final String CONTENT_TYPE_MARKDOWN = "text/markdown;charset=UTF-8";

    /** Word docx 文件内容类型。 */
    private static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Markdown 导出格式值。 */
    private static final String FORMAT_MARKDOWN = "md";

    /** Word 导出格式值。 */
    private static final String FORMAT_DOCX = "docx";

    /**
     * 按用户选择的格式导出剧情任务。
     */
    @Override
    public AiStoryTaskExportFile exportTask(AiStoryTask task, String format)
    {
        if (task == null)
        {
            throw new ServiceException("剧情任务不存在");
        }
        String normalizedFormat = StringUtils.lowerCase(StringUtils.defaultIfBlank(format, FORMAT_DOCX));
        if (FORMAT_MARKDOWN.equals(normalizedFormat))
        {
            return markdownFile(task);
        }
        if (FORMAT_DOCX.equals(normalizedFormat))
        {
            return wordFile(task);
        }
        throw new ServiceException("不支持的剧情导出格式");
    }

    /**
     * 生成 Markdown 文件。
     */
    private AiStoryTaskExportFile markdownFile(AiStoryTask task)
    {
        AiStoryTaskExportFile file = new AiStoryTaskExportFile();
        file.setFileName(fileBaseName(task) + ".md");
        file.setContentType(CONTENT_TYPE_MARKDOWN);
        file.setBytes(buildMarkdown(task).getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * 生成 Word docx 文件。
     */
    private AiStoryTaskExportFile wordFile(AiStoryTask task)
    {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            addTitle(document, "剧情生成结果");
            addSection(document, "输入参数", inputLines(task));
            addSection(document, "剧情生成", task.getStoryOutput());
            addSection(document, "剧本生成", task.getScriptOutput());
            addSection(document, "分镜生成", task.getStoryboardOutput());
            addSection(document, "分镜校验", task.getStoryboardCheckOutput());
            addSection(document, "三视图/场景图", task.getVisualOutput());
            document.write(outputStream);

            AiStoryTaskExportFile file = new AiStoryTaskExportFile();
            file.setFileName(fileBaseName(task) + ".docx");
            file.setContentType(CONTENT_TYPE_DOCX);
            file.setBytes(outputStream.toByteArray());
            return file;
        }
        catch (IOException e)
        {
            throw new ServiceException("剧情任务 Word 导出失败");
        }
    }

    /**
     * 构建 Markdown 正文。
     */
    private String buildMarkdown(AiStoryTask task)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("# 剧情生成结果\n\n");
        appendMarkdownSection(builder, "输入参数", inputLines(task));
        appendMarkdownSection(builder, "剧情生成", task.getStoryOutput());
        appendMarkdownSection(builder, "剧本生成", task.getScriptOutput());
        appendMarkdownSection(builder, "分镜生成", task.getStoryboardOutput());
        appendMarkdownSection(builder, "分镜校验", task.getStoryboardCheckOutput());
        appendMarkdownSection(builder, "三视图/场景图", task.getVisualOutput());
        return builder.toString();
    }

    /**
     * 追加 Markdown 章节。
     */
    private void appendMarkdownSection(StringBuilder builder, String title, String content)
    {
        builder.append("## ").append(title).append("\n\n");
        builder.append(StringUtils.defaultIfBlank(content, "未生成")).append("\n\n");
    }

    /**
     * 追加 Word 标题段落。
     */
    private void addTitle(XWPFDocument document, String title)
    {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(18);
        run.setText(title);
    }

    /**
     * 追加 Word 章节标题和正文。
     */
    private void addSection(XWPFDocument document, String title, String content)
    {
        XWPFParagraph titleParagraph = document.createParagraph();
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(13);
        titleRun.setText(title);

        XWPFParagraph bodyParagraph = document.createParagraph();
        XWPFRun bodyRun = bodyParagraph.createRun();
        writeMultilineText(bodyRun, StringUtils.defaultIfBlank(content, "未生成"));
    }

    /**
     * 向 Word run 写入多行文本，保留模型输出中的换行结构。
     */
    private void writeMultilineText(XWPFRun run, String content)
    {
        String[] lines = StringUtils.splitPreserveAllTokens(content, "\n");
        for (int index = 0; index < lines.length; index++)
        {
            if (index > 0)
            {
                run.addBreak();
            }
            run.setText(lines[index]);
        }
    }

    /**
     * 构造输入参数展示文本。
     */
    private String inputLines(AiStoryTask task)
    {
        return String.join("\n", List.of(
                "剧情主题：" + StringUtils.defaultString(task.getStoryTheme()),
                "产品信息：" + StringUtils.defaultString(task.getProductInfo()),
                "目标平台：" + StringUtils.defaultString(task.getTargetPlatform()),
                "目标人群：" + StringUtils.defaultString(task.getTargetAudience()),
                "推荐风格：" + StringUtils.defaultString(task.getStoryStyle()),
                "剧情时长：" + StringUtils.defaultString(task.getStoryDuration())));
    }

    /**
     * 构造下载文件基础名，并移除 Windows 文件名非法字符。
     */
    private String fileBaseName(AiStoryTask task)
    {
        String theme = StringUtils.defaultIfBlank(task.getStoryTheme(), "剧情生成");
        String limitedTheme = StringUtils.abbreviate(theme, 30);
        String sanitizedTheme = limitedTheme.replaceAll("[\\\\/:*?\"<>|]", "_");
        String suffix = StringUtils.isBlank(task.getId()) ? "" : "-" + StringUtils.left(task.getId(), 8);
        return "剧情生成-" + sanitizedTheme + suffix;
    }
}
