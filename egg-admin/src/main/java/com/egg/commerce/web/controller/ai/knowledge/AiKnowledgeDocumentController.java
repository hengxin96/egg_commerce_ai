package com.egg.commerce.web.controller.ai.knowledge;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeAccessRoleUpdateRequest;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeDocumentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台知识库文档接口。
 *
 * <p>控制器只处理 HTTP 入参与权限，OSS 存储、RAG 向量化、清理和校验留在服务层。</p>
 */
@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeDocumentController extends BaseController
{
    private final AiKnowledgeDocumentService knowledgeDocumentService;

    /**
     * 注入知识库文档服务，控制器保持后台权限和 HTTP 边界职责。
     */
    @Autowired
    public AiKnowledgeDocumentController(AiKnowledgeDocumentService knowledgeDocumentService)
    {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    /**
     * 后台知识库页面使用的分页文档列表。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiKnowledgeDocument query)
    {
        startPage();
        List<AiKnowledgeDocument> list = knowledgeDocumentService.selectAiKnowledgeDocumentList(query);
        return getDataTable(list);
    }

    /**
     * 兼容旧页面的单文件上传接口。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:upload')")
    @Log(title = "AI知识库", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        return success(knowledgeDocumentService.upload(file, getUserId()));
    }

    /**
     * 拖拽上传页面使用的批量上传接口。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:upload')")
    @Log(title = "AI知识库", businessType = BusinessType.INSERT)
    @PostMapping("/upload/batch")
    public AjaxResult uploadBatch(@RequestParam("files") MultipartFile[] files)
    {
        return success(knowledgeDocumentService.uploadBatch(files, getUserId()));
    }

    /**
     * 返回所选文档的 OSS 下载地址，前端据此触发下载。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:list')")
    @Log(title = "AI知识库", businessType = BusinessType.EXPORT)
    @GetMapping("/download-urls/{ids}")
    public AjaxResult downloadUrls(@PathVariable String[] ids)
    {
        return success(knowledgeDocumentService.selectDownloadUrls(ids));
    }

    /**
     * 批量设置知识库文档可访问角色，上传后默认 admin，可由后台扩展到 common。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:upload')")
    @Log(title = "AI知识库", businessType = BusinessType.UPDATE)
    @PutMapping("/access-roles")
    public AjaxResult accessRoles(@RequestBody AiKnowledgeAccessRoleUpdateRequest request)
    {
        return toAjax(knowledgeDocumentService.updateAccessRoles(request == null ? null : request.getIds(),
                request == null ? null : request.getAccessRoles()));
    }

    /**
     * 删除文档元数据，并清理关联的 OSS 文件和向量库记录。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(knowledgeDocumentService.deleteAiKnowledgeDocumentByIds(ids));
    }

    /**
     * 重新解析并向量化失败的知识库文档。
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledge:retry')")
    @Log(title = "AI知识库", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable String id)
    {
        knowledgeDocumentService.retryVectorization(id);
        return success();
    }
}
