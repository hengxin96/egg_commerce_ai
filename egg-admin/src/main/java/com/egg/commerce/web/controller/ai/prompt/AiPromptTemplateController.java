package com.egg.commerce.web.controller.ai.prompt;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.service.ai.prompt.IAiPromptTemplateService;

/**
 * AI prompt template management for the backend admin console.
 */
@RestController
@RequestMapping("/ai/promptTemplate")
public class AiPromptTemplateController extends BaseController
{
    @Autowired
    private IAiPromptTemplateService promptTemplateService;

    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiPromptTemplate promptTemplate)
    {
        startPage();
        List<AiPromptTemplate> list = promptTemplateService.selectAiPromptTemplateList(promptTemplate);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable String id)
    {
        return success(promptTemplateService.selectAiPromptTemplateById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:add')")
    @Log(title = "提示词模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiPromptTemplate promptTemplate)
    {
        promptTemplate.setCreatedBy(getUserId());
        return toAjax(promptTemplateService.insertAiPromptTemplate(promptTemplate));
    }

    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:edit')")
    @Log(title = "提示词模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiPromptTemplate promptTemplate)
    {
        return toAjax(promptTemplateService.updateAiPromptTemplate(promptTemplate));
    }

    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:remove')")
    @Log(title = "提示词模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(promptTemplateService.deleteAiPromptTemplateByIds(ids));
    }
}
