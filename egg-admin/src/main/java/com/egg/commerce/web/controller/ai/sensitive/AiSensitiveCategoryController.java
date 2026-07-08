package com.egg.commerce.web.controller.ai.sensitive;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import com.egg.commerce.system.service.ai.sensitive.IAiSensitiveCategoryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/sensitive/category")
public class AiSensitiveCategoryController extends BaseController
{
    private final IAiSensitiveCategoryService sensitiveCategoryService;

    public AiSensitiveCategoryController(IAiSensitiveCategoryService sensitiveCategoryService)
    {
        this.sensitiveCategoryService = sensitiveCategoryService;
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:query')")
    @GetMapping("/list")
    public TableDataInfo list(AiSensitiveCategory query)
    {
        startPage();
        List<AiSensitiveCategory> list = sensitiveCategoryService.selectAiSensitiveCategoryList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(sensitiveCategoryService.selectAiSensitiveCategoryById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:add')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiSensitiveCategory category)
    {
        return toAjax(sensitiveCategoryService.insertAiSensitiveCategory(category, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:edit')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiSensitiveCategory category)
    {
        return toAjax(sensitiveCategoryService.updateAiSensitiveCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:remove')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sensitiveCategoryService.deleteAiSensitiveCategoryByIds(ids));
    }
}
