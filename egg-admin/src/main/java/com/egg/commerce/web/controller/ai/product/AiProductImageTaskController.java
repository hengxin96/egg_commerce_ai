package com.egg.commerce.web.controller.ai.product;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskConfirmRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskReplanRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskRequest;
import com.egg.commerce.system.service.ai.product.AiProductImageTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Product image generation task APIs for the current front user. */
@RestController
@RequestMapping("/ai/product-image/tasks")
public class AiProductImageTaskController extends BaseController
{
    private final AiProductImageTaskService taskService;

    public AiProductImageTaskController(AiProductImageTaskService taskService)
    {
        this.taskService = taskService;
    }

    @PostMapping
    public AjaxResult create(@Validated @RequestBody AiProductImageTaskRequest request)
    {
        return success(taskService.createTask(getUserId(), request));
    }

    @GetMapping
    public AjaxResult list()
    {
        return success(taskService.selectTaskList(getUserId()));
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id)
    {
        return success(taskService.selectTaskDetail(id, getUserId()));
    }

    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable String id)
    {
        return success(taskService.retryTask(id, getUserId()));
    }

    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable String id)
    {
        return success(taskService.cancelTask(id, getUserId()));
    }

    @PostMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable String id, @RequestBody(required = false) AiProductImageTaskConfirmRequest request)
    {
        return success(taskService.confirmTask(id, getUserId(), request));
    }

    @PostMapping("/{id}/replan")
    public AjaxResult replan(@PathVariable String id, @RequestBody(required = false) AiProductImageTaskReplanRequest request)
    {
        return success(taskService.replanTask(id, getUserId(), request));
    }
}
