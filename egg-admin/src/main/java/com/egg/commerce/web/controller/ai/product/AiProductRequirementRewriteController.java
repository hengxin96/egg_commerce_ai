package com.egg.commerce.web.controller.ai.product;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteResult;
import com.egg.commerce.system.service.ai.product.AiProductRequirementRewriteService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter rewriteStream(@RequestBody AiRequirementRewriteRequest request)
    {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = getUserId();
        CompletableFuture.runAsync(() -> {
            try
            {
                AiRequirementRewriteResult result = requirementRewriteService.rewriteStream(userId, request,
                        delta -> sendDelta(emitter, delta));
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            }
            catch (Exception e)
            {
                sendError(emitter, e);
            }
        });
        return emitter;
    }

    private void sendDelta(SseEmitter emitter, String delta)
    {
        try
        {
            emitter.send(SseEmitter.event().name("delta").data(delta));
        }
        catch (IOException e)
        {
            throw new ServiceException("AI帮写流式输出失败：" + e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, Exception e)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(AjaxResult.error(e.getMessage())));
            emitter.complete();
        }
        catch (IOException ioException)
        {
            emitter.completeWithError(ioException);
        }
    }
}
