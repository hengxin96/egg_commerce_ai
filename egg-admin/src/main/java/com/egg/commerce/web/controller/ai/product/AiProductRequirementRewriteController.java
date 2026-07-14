package com.egg.commerce.web.controller.ai.product;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.service.ai.product.AiProductRequirementRewriteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Product image requirement rewrite API for the current front user. */
@RestController
@RequestMapping("/ai/product-image/requirement/rewrite")
public class AiProductRequirementRewriteController extends BaseController
{
    private final AiProductRequirementRewriteService requirementRewriteService;

    public AiProductRequirementRewriteController(AiProductRequirementRewriteService requirementRewriteService)
    {
        this.requirementRewriteService = requirementRewriteService;
    }

    @PostMapping
    public AjaxResult rewrite(@RequestBody AiRequirementRewriteRequest request)
    {
        return success(requirementRewriteService.rewrite(getUserId(), request));
    }
}
