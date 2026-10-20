package com.egg.commerce.system.service.ai.story;

import com.egg.commerce.system.domain.ai.story.AiStoryTask;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskExportFile;

/**
 * 剧情任务导出服务接口。
 *
 * <p>该接口把任务内容导出为用户可下载文件，当前支持 Word 和 Markdown 两种格式。</p>
 */
public interface AiStoryTaskExportService
{
    /** 按指定格式导出剧情任务内容。 */
    AiStoryTaskExportFile exportTask(AiStoryTask task, String format);
}
