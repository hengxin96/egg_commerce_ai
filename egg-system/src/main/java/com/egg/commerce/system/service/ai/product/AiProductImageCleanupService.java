package com.egg.commerce.system.service.ai.product;

/**
 * Removes abandoned product-image drafts and unreferenced product-image storage objects.
 */
public interface AiProductImageCleanupService
{
    /**
     * Cleans product-image records and storage objects older than the configured draft retention period.
     *
     * @return number of expired drafts and orphan objects deleted
     */
    int cleanExpiredDraftsAndOrphans();
}
