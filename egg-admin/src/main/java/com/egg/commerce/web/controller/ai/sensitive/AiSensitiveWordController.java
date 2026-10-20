package com.egg.commerce.web.controller.ai.sensitive;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveWordService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台敏感词条管理接口。
 *
 * <p>敏感词条用于支撑后续 AI 文案、标题、卖点和商品素材审核，帮助模型输出在导出前暴露合规风险。</p>
 */
@RestController
@RequestMapping("/ai/sensitive/word")
public class AiSensitiveWordController extends BaseController
{
    /** 敏感词条服务，负责词条校验、默认状态和持久化。 */
    private final AiSensitiveWordService sensitiveWordService;

    /**
     * 注入敏感词条服务，控制器只处理后台请求入口。
     */
    @Autowired
    public AiSensitiveWordController(AiSensitiveWordService sensitiveWordService)
    {
        this.sensitiveWordService = sensitiveWordService;
    }

    /**
     * 分页查询敏感词条，支持后台按分类、状态、词内容筛选维护。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiSensitiveWord query)
    {
        startPage();
        List<AiSensitiveWord> list = sensitiveWordService.selectAiSensitiveWordList(query);
        return getDataTable(list);
    }

    /**
     * 查询敏感词详情，通常用于编辑页回显分类和状态。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(sensitiveWordService.selectAiSensitiveWordById(id));
    }

    /**
     * 新增敏感词条，并记录创建管理员，便于追踪规则来源。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:add')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiSensitiveWord word)
    {
        return toAjax(sensitiveWordService.insertAiSensitiveWord(word, getUserId()));
    }

    /**
     * 更新敏感词条内容、分类或状态。
     *
     * <p>敏感词变化会影响后续内容审核结果，维护时应确认分类语义正确。</p>
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:edit')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiSensitiveWord word)
    {
        return toAjax(sensitiveWordService.updateAiSensitiveWord(word));
    }

    /**
     * 批量删除敏感词条。删除后对应词不会再参与后续敏感词规则维护。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:word:remove')")
    @Log(title = "AI Sensitive Word", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sensitiveWordService.deleteAiSensitiveWordByIds(ids));
    }
}
