package com.egg.commerce.web.controller.ai.story;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.utils.file.FileUtils;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskExportFile;
import com.egg.commerce.system.domain.ai.story.AiStoryTaskRequest;
import com.egg.commerce.system.service.ai.story.AiStoryTaskExportService;
import com.egg.commerce.system.service.ai.story.AiStoryTaskService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台剧情生成任务接口。
 *
 * <p>控制器负责把用户端的开始生成、任务中心、详情查看和文件下载请求转给业务服务，
 * 当前登录用户 ID 统一从安全上下文获取，防止跨用户访问任务内容。</p>
 */
@RestController
@RequestMapping("/ai/story/tasks")
public class AiStoryTaskController extends BaseController
{
    /** 剧情任务服务，负责创建异步任务、查询进度和校验任务归属。 */
    private final AiStoryTaskService storyTaskService;

    /** 剧情任务导出服务，负责生成 Word 或 Markdown 文件内容。 */
    private final AiStoryTaskExportService storyTaskExportService;

    /**
     * 注入剧情任务相关服务。
     */
    public AiStoryTaskController(AiStoryTaskService storyTaskService,
            AiStoryTaskExportService storyTaskExportService)
    {
        this.storyTaskService = storyTaskService;
        this.storyTaskExportService = storyTaskExportService;
    }

    /**
     * 创建剧情生成任务。
     *
     * <p>接口会立即返回任务记录，真正的大模型生成在线程池中异步执行。</p>
     */
    @PostMapping
    public AjaxResult create(@Validated @RequestBody AiStoryTaskRequest request)
    {
        return success(storyTaskService.createTask(getUserId(), request));
    }

    /**
     * 查询当前用户的剧情任务列表。
     */
    @GetMapping
    public AjaxResult list()
    {
        return success(storyTaskService.selectTaskList(getUserId()));
    }

    /**
     * 查询当前用户的单个剧情任务详情。
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id)
    {
        return success(storyTaskService.selectTaskDetail(id, getUserId()));
    }

    /**
     * 重试当前用户的失败剧情任务。
     *
     * <p>重试不会创建新任务，服务层会保留已经成功的上游节点内容，并从失败节点或最近可恢复节点继续生成。</p>
     */
    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable String id)
    {
        return success(storyTaskService.retryTask(id, getUserId()));
    }

    /**
     * 下载当前用户的剧情任务内容。
     *
     * <p>format 支持 docx 和 md，默认 docx；文件名和中文编码由 FileUtils 统一处理。</p>
     */
    @GetMapping("/{id}/export")
    public void export(@PathVariable String id,
            @RequestParam(defaultValue = "docx") String format,
            HttpServletResponse response) throws IOException
    {
        AiStoryTaskExportFile file = storyTaskExportService.exportTask(
                storyTaskService.selectTaskDetail(id, getUserId()), format);
        response.setContentType(file.getContentType());
        response.setContentLength(file.getBytes().length);
        FileUtils.setAttachmentResponseHeader(response, file.getFileName());
        response.getOutputStream().write(file.getBytes());
    }
}
