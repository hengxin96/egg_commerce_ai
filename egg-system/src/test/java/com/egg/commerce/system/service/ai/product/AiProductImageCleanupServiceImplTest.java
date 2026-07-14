package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.service.ai.product.impl.AiProductImageCleanupServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class AiProductImageCleanupServiceImplTest
{
    @Test
    void cleanupDeletesExpiredDraftAssetImages()
    {
        Fixture fixture = new Fixture();
        fixture.assets.add(fixture.asset("draft", "draft_uploaded", false));
        fixture.images.add(fixture.image("source", "draft", "ai/product-images/source/draft.png"));

        fixture.service.cleanExpiredDraftsAndOrphans();

        assertEquals(List.of("ai/product-images/source/draft.png"), fixture.storage.deletedObjectKeys);
        assertTrue(fixture.images.images.isEmpty());
        assertFalse(fixture.assets.assets.containsKey("draft"));
    }

    @Test
    void cleanupDeletesOssObjectWhenDatabaseHasNoImageRow()
    {
        Fixture fixture = new Fixture();
        fixture.storage.objects.add(fixture.object("ai/product-images/source/orphan.png"));

        fixture.service.cleanExpiredDraftsAndOrphans();

        assertEquals(List.of("ai/product-images/source/orphan.png"), fixture.storage.deletedObjectKeys);
    }

    @Test
    void cleanupDoesNotDeleteFailedCancelledOrCompletedObjects()
    {
        Fixture fixture = new Fixture();
        fixture.assets.add(fixture.asset("failed", "failed", false));
        fixture.assets.add(fixture.asset("cancelled", "cancelled", false));
        fixture.assets.add(fixture.asset("completed", "completed", true));
        fixture.images.add(fixture.image("failed-image", "failed", "ai/product-images/source/failed.png"));
        fixture.images.add(fixture.image("cancelled-image", "cancelled", "ai/product-images/source/cancelled.png"));
        fixture.images.add(fixture.image("completed-image", "completed", "ai/product-images/generated/completed.png"));
        fixture.storage.objects.add(fixture.object("ai/product-images/source/failed.png"));
        fixture.storage.objects.add(fixture.object("ai/product-images/source/cancelled.png"));
        fixture.storage.objects.add(fixture.object("ai/product-images/generated/completed.png"));

        fixture.service.cleanExpiredDraftsAndOrphans();

        assertTrue(fixture.storage.deletedObjectKeys.isEmpty());
        assertTrue(fixture.assets.assets.containsKey("failed"));
        assertTrue(fixture.assets.assets.containsKey("cancelled"));
        assertTrue(fixture.assets.assets.containsKey("completed"));
        assertEquals(3, fixture.images.images.size());
    }

    private static class Fixture
    {
        private final FakeAssetMapper assets = new FakeAssetMapper();
        private final FakeImageMapper images = new FakeImageMapper();
        private final FakeStorage storage = new FakeStorage();
        private final AiProductImageCleanupService service = new AiProductImageCleanupServiceImpl(assets, images, storage);

        private AiProductAsset asset(String id, String status, boolean visible)
        {
            AiProductAsset asset = new AiProductAsset();
            asset.setId(id);
            asset.setUserId(1L);
            asset.setStatus(status);
            asset.setVisible(visible);
            asset.setUpdatedAt(new Date(0));
            return asset;
        }

        private AiProductAssetImage image(String id, String assetId, String objectKey)
        {
            AiProductAssetImage image = new AiProductAssetImage();
            image.setId(id);
            image.setProductAssetId(assetId);
            image.setUserId(1L);
            image.setOssObjectKey(objectKey);
            return image;
        }

        private AiStoredObjectSummary object(String objectKey)
        {
            AiStoredObjectSummary object = new AiStoredObjectSummary();
            object.setObjectKey(objectKey);
            object.setLastModified(new Date(0));
            return object;
        }
    }

    private static class FakeAssetMapper implements AiProductAssetMapper
    {
        private final Map<String, AiProductAsset> assets = new HashMap<>();

        void add(AiProductAsset asset) { assets.put(asset.getId(), asset); }
        @Override public AiProductAsset selectAiProductAssetById(String id) { return assets.get(id); }
        @Override public AiProductAsset selectCurrentDraft(Long userId) { return null; }
        @Override public List<AiProductAsset> selectExpiredDrafts(Date expireBefore) { return assets.values().stream().filter(asset -> "draft_uploaded".equals(asset.getStatus()) && Boolean.FALSE.equals(asset.getVisible()) && asset.getUpdatedAt().before(expireBefore)).toList(); }
        @Override public List<AiProductAsset> selectVisibleAssetList(AiProductAsset query) { return List.of(); }
        @Override public int insertAiProductAsset(AiProductAsset asset) { add(asset); return 1; }
        @Override public int updateAiProductAsset(AiProductAsset asset) { add(asset); return 1; }
        @Override public int deleteAiProductAssetById(String id, Long userId) { return assets.remove(id) == null ? 0 : 1; }
    }

    private static class FakeImageMapper implements AiProductAssetImageMapper
    {
        private final List<AiProductAssetImage> images = new ArrayList<>();

        void add(AiProductAssetImage image) { images.add(image); }
        @Override public AiProductAssetImage selectAiProductAssetImageById(String id) { return null; }
        @Override public AiProductAssetImage selectImageByObjectKey(String objectKey) { return images.stream().filter(image -> objectKey.equals(image.getOssObjectKey())).findFirst().orElse(null); }
        @Override public List<AiProductAssetImage> selectImagesByAssetId(String assetId, Long userId) { return images.stream().filter(image -> assetId.equals(image.getProductAssetId()) && userId.equals(image.getUserId())).toList(); }
        @Override public List<AiProductAssetImage> selectImagesByTaskId(String taskId, Long userId) { return List.of(); }
        @Override public int insertAiProductAssetImage(AiProductAssetImage image) { add(image); return 1; }
        @Override public int updateAiProductAssetImage(AiProductAssetImage image) { return 1; }
        @Override public int deleteAiProductAssetImageById(String id, Long userId) { return 0; }
        @Override public int deleteImagesByAssetId(String assetId, Long userId) { return images.removeIf(image -> assetId.equals(image.getProductAssetId()) && userId.equals(image.getUserId())) ? 1 : 0; }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private final List<AiStoredObjectSummary> objects = new ArrayList<>();
        private final List<String> deletedObjectKeys = new ArrayList<>();

        @Override public AiStoredObject uploadKnowledgeFile(MultipartFile file) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId) { throw new UnsupportedOperationException(); }
        @Override public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan) { return new ArrayList<>(objects); }
        @Override public byte[] downloadObject(String objectKey) { throw new UnsupportedOperationException(); }
        @Override public void deleteObject(String objectKey) { deletedObjectKeys.add(objectKey); }
    }
}
