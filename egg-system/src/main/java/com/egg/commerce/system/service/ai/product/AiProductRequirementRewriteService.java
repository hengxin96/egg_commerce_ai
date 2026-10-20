package com.egg.commerce.system.service.ai.product;

import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteResult;
import java.util.function.Consumer;

/** Rewrites a product-image requirement into a model-ready draft. */
public interface AiProductRequirementRewriteService
{
    AiRequirementRewriteResult rewrite(Long userId, AiRequirementRewriteRequest request);

    AiRequirementRewriteResult rewriteStream(Long userId, AiRequirementRewriteRequest request, Consumer<String> onDelta);
}
