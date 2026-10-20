package com.egg.commerce.system.service.ai.product.impl;

import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.service.ai.product.AiProductImageCleanupService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Cleans only abandoned upload drafts; task history and every image still referenced by the database are retained.
 */
@Service
public class AiProductImageCleanupServiceImpl implements AiProductImageCleanupService
{
    private static final long DRAFT_EXPIRE_MILLIS = 12L * 60L * 60L * 1000L;

    private final AiProductAssetMapper assetMapper;
    private final AiProductAssetImageMapper imageMapper;
    private final AiObjectStorageService objectStorageService;

    public AiProductImageCleanupServiceImpl(AiProductAssetMapper assetMapper, AiProductAssetImageMapper imageMapper,
            AiObjectStorageService objectStorageService)
    {
        this.assetMapper = assetMapper;
        this.imageMapper = imageMapper;
        this.objectStorageService = objectStorageService;
    }

    @Override
    public int cleanExpiredDraftsAndOrphans()
    {
        Date expireBefore = new Date(System.currentTimeMillis() - DRAFT_EXPIRE_MILLIS);
        Set<String> deletedObjectKeys = new HashSet<>();
        int deletedCount = cleanExpiredDrafts(expireBefore, deletedObjectKeys);

        for (AiStoredObjectSummary object : objectStorageService.listProductImageObjects(expireBefore))
        {
            String objectKey = object.getObjectKey();
            // A database row protects every task state, including failed and cancelled task history.
            if (!deletedObjectKeys.contains(objectKey) && imageMapper.selectImageByObjectKey(objectKey) == null)
            {
                objectStorageService.deleteObject(objectKey);
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private int cleanExpiredDrafts(Date expireBefore, Set<String> deletedObjectKeys)
    {
        int deletedDrafts = 0;
        List<AiProductAsset> expiredDrafts = assetMapper.selectExpiredDrafts(expireBefore);
        for (AiProductAsset draft : expiredDrafts)
        {
            List<AiProductAssetImage> images = imageMapper.selectImagesByAssetId(draft.getId(), draft.getUserId());
            for (AiProductAssetImage image : images)
            {
                objectStorageService.deleteObject(image.getOssObjectKey());
                deletedObjectKeys.add(image.getOssObjectKey());
            }
            imageMapper.deleteImagesByAssetId(draft.getId(), draft.getUserId());
            assetMapper.deleteAiProductAssetById(draft.getId(), draft.getUserId());
            deletedDrafts++;
        }
        return deletedDrafts;
    }
}
