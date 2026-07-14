package com.egg.commerce.web.task;

import com.egg.commerce.system.service.ai.product.AiProductImageCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the bounded product-image retention cleanup on the application scheduler. */
@Component("productImageCleanupTask")
public class ProductImageCleanupTask
{
    private final AiProductImageCleanupService cleanupService;

    public ProductImageCleanupTask(AiProductImageCleanupService cleanupService)
    {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${egg.ai.product-image.cleanup-cron:0 30 * * * ?}")
    public void cleanExpiredDraftsAndOrphans()
    {
        cleanupService.cleanExpiredDraftsAndOrphans();
    }
}
