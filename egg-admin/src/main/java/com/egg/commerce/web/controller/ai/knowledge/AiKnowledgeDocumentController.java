package com.egg.commerce.web.controller.ai.knowledge;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.service.ai.knowledge.AiKnowledgeDocumentService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeDocumentController extends BaseController
{
    private final AiKnowledgeDocumentService knowledgeDocumentService;

    public AiKnowledgeDocumentController(AiKnowledgeDocumentService knowledgeDocumentService)
    {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiKnowledgeDocument query)
    {
        startPage();
        List<AiKnowledgeDocument> list = knowledgeDocumentService.selectAiKnowledgeDocumentList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:upload')")
    @Log(title = "AI知识库", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        return success(knowledgeDocumentService.upload(file, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(knowledgeDocumentService.deleteAiKnowledgeDocumentByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledge:retry')")
    @Log(title = "AI知识库", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable String id)
    {
        knowledgeDocumentService.retryVectorization(id);
        return success();
    }
}
