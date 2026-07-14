package com.egg.commerce.system.service.ai.product;

import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteResult;

/** Rewrites a product-image requirement into a model-ready draft. */
public interface AiProductRequirementRewriteService
{
    AiRequirementRewriteResult rewrite(Long userId, AiRequirementRewriteRequest request);
}
