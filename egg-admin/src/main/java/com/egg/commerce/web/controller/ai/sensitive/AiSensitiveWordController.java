package com.egg.commerce.web.controller.ai.sensitive;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveWordService;
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
@RequestMapping("/ai/sensitive/word")
public class AiSensitiveWordController extends BaseController
{
    private final AiSensitiveWordService sensitiveWordService;

    public AiSensitiveWordController(AiSensitiveWordService sensitiveWordService)
    {
        this.sensitiveWordService = sensitiveWordService;
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiSensitiveWord query)
    {
        startPage();
        List<AiSensitiveWord> list = sensitiveWordService.selectAiSensitiveWordList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(sensitiveWordService.selectAiSensitiveWordById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:add')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiSensitiveWord word)
    {
        return toAjax(sensitiveWordService.insertAiSensitiveWord(word, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:edit')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiSensitiveWord word)
    {
        return toAjax(sensitiveWordService.updateAiSensitiveWord(word));
    }

    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:remove')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sensitiveWordService.deleteAiSensitiveWordByIds(ids));
    }
}
