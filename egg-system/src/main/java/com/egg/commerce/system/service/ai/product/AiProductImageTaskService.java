package com.egg.commerce.system.service.ai.product;

import com.egg.commerce.system.domain.ai.product.AiProductImageTask;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskConfirmRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskReplanRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTaskRequest;
import java.util.List;

/** Service for product-image generation task lifecycle operations. */
public interface AiProductImageTaskService
{
    AiProductImageTask createTask(Long userId, AiProductImageTaskRequest request);

    AiProductImageTask selectTaskDetail(String taskId, Long userId);

    List<AiProductImageTask> selectTaskList(Long userId);

    AiProductImageTask retryTask(String taskId, Long userId);

    AiProductImageTask cancelTask(String taskId, Long userId);

    AiProductImageTask confirmTask(String taskId, Long userId, AiProductImageTaskConfirmRequest request);

    AiProductImageTask replanTask(String taskId, Long userId, AiProductImageTaskReplanRequest request);

    void executeTask(String taskId);
}
