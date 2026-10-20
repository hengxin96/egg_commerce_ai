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
 * 后台提示词模板管理接口。
 *
 * <p>提示词模板是后续图片生成、RAG 问答和 agent 工作流的可追踪配置来源。Controller 只负责
 * 管理端权限、分页和当前登录人写入，模板内容的 JSON 校验和默认值由 service 层处理。</p>
 */
@RestController
@RequestMapping("/ai/promptTemplate")
public class AiPromptTemplateController extends BaseController
{
    /** 提示词模板服务，负责模板内容校验、状态默认值和数据库写入。 */
    private final IAiPromptTemplateService promptTemplateService;

    /**
     * 注入提示词模板服务，控制器只保留后台权限与请求响应适配。
     */
    @Autowired
    public AiPromptTemplateController(IAiPromptTemplateService promptTemplateService)
    {
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 分页查询提示词模板，供后台按名称、分类、状态等条件维护模型提示词配置。
     */
    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiPromptTemplate promptTemplate)
    {
        startPage();
        List<AiPromptTemplate> list = promptTemplateService.selectAiPromptTemplateList(promptTemplate);
        return getDataTable(list);
    }

    /**
     * 查询单个模板详情，通常用于编辑页回显 Markdown 正文和变量定义。
     */
    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable String id)
    {
        return success(promptTemplateService.selectAiPromptTemplateById(id));
    }

    /**
     * 新增提示词模板。
     *
     * <p>createdBy 使用当前管理员 ID，方便后续追踪模板来源和变更责任。</p>
     */
    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:add')")
    @Log(title = "提示词模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiPromptTemplate promptTemplate)
    {
        promptTemplate.setCreatedBy(getUserId());
        return toAjax(promptTemplateService.insertAiPromptTemplate(promptTemplate));
    }

    /**
     * 更新提示词模板。
     *
     * <p>模板正文会重新校验为合法 JSON，避免错误提示词进入后续模型调用链路。</p>
     */
    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:edit')")
    @Log(title = "提示词模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiPromptTemplate promptTemplate)
    {
        return toAjax(promptTemplateService.updateAiPromptTemplate(promptTemplate));
    }

    /**
     * 批量删除提示词模板。删除前应确认模板未被正在使用的生成流程依赖。
     */
    @PreAuthorize("@ss.hasPermi('ai:promptTemplate:remove')")
    @Log(title = "提示词模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(promptTemplateService.deleteAiPromptTemplateByIds(ids));
    }
}
