package com.egg.commerce.web.controller.ai.sensitive;

import com.egg.commerce.common.annotation.Log;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.page.TableDataInfo;
import com.egg.commerce.common.enums.BusinessType;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveCategoryService;
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
 * 后台敏感词分类管理接口。
 *
 * <p>分类用于把平台规则、广告法风险、品牌限制等敏感词归档，便于后续审核流程按类别解释风险。</p>
 */
@RestController
@RequestMapping("/ai/sensitive/category")
public class AiSensitiveCategoryController extends BaseController
{
    /** 敏感词分类服务，负责分类默认状态、创建人和删除前依赖校验。 */
    private final AiSensitiveCategoryService sensitiveCategoryService;

    /**
     * 注入敏感词分类服务，控制器负责后台菜单权限和操作日志边界。
     */
    @Autowired
    public AiSensitiveCategoryController(AiSensitiveCategoryService sensitiveCategoryService)
    {
        this.sensitiveCategoryService = sensitiveCategoryService;
    }

    /**
     * 分页查询敏感词分类，用于后台维护分类字典和启停状态。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:query')")
    @GetMapping("/list")
    public TableDataInfo list(AiSensitiveCategory query)
    {
        startPage();
        List<AiSensitiveCategory> list = sensitiveCategoryService.selectAiSensitiveCategoryList(query);
        return getDataTable(list);
    }

    /**
     * 查询分类详情，通常用于编辑页回显。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(sensitiveCategoryService.selectAiSensitiveCategoryById(id));
    }

    /**
     * 新增敏感词分类，并记录创建管理员，方便追踪规则维护来源。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:add')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiSensitiveCategory category)
    {
        return toAjax(sensitiveCategoryService.insertAiSensitiveCategory(category, getUserId()));
    }

    /**
     * 更新敏感词分类名称或状态，影响后台规则维护和后续风险解释展示。
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:edit')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiSensitiveCategory category)
    {
        return toAjax(sensitiveCategoryService.updateAiSensitiveCategory(category));
    }

    /**
     * 删除敏感词分类。
     *
     * <p>如果分类下仍有敏感词，service 会阻止删除，避免规则词失去所属分类。</p>
     */
    @PreAuthorize("@ss.hasPermi('ai:sensitive:category:remove')")
    @Log(title = "AI Sensitive Category", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sensitiveCategoryService.deleteAiSensitiveCategoryByIds(ids));
    }
}
