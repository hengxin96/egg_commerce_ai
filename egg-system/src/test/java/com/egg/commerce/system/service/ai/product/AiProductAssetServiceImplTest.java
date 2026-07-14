package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDetail;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductDraftDetail;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.service.ai.product.impl.AiProductAssetServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AiProductAssetServiceImplTest
{
    @Test
    void uploadFirstImageCreatesInvisibleDraft()
    {
        Fixture fixture = new Fixture();

        AiProductDraftDetail result = fixture.service.uploadDraftImage(1L,
                new MockMultipartFile("file", "shoe.png", "image/png", new byte[] {1, 2}));

        assertEquals("draft_uploaded", result.getAsset().getStatus());
        assertFalse(result.getAsset().getVisible());
        assertEquals(1, fixture.images.images.size());
        assertEquals("source", fixture.images.images.get(0).getImageRole());
        assertEquals(1, fixture.storage.uploadCalls);
    }

    @Test
    void reuseCompletedAssetCopiesSourceObjectsIntoNewDraft()
    {
        Fixture fixture = new Fixture();
        AiProductAsset original = fixture.asset("original", 1L, "completed", true);
        original.setAssetType("main");
        original.setTargetPlatform("douyin");
        original.setAspectRatio("3:4");
        original.setQuality("2K");
        original.setUserRequirement("bright");
        fixture.assets.insertAiProductAsset(original);
        fixture.images.insertAiProductAssetImage(fixture.image("source-1", "original", 1L, "source", "source/1.png"));

        AiProductDraftDetail result = fixture.service.reuseAsset(1L, "original");

        assertEquals("original", result.getAsset().getSourceProductId());
        assertFalse(result.getAsset().getVisible());
        assertEquals(1, fixture.storage.copyCalls);
        assertEquals("main", result.getAsset().getAssetType());
        assertEquals(1, result.getSourceImages().size());
    }

    @Test
    void deleteVisibleAssetDeletesAllOssObjectsAndRows()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("source", "asset", 1L, "source", "source/a.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("generated", "asset", 1L, "generated", "generated/a.png"));

        AiProductAssetDetail deleted = fixture.service.deleteAsset(1L, "asset");

        assertEquals("asset", deleted.getAsset().getId());
        assertEquals(List.of("source/a.png", "generated/a.png"), fixture.storage.deletedObjectKeys);
        assertTrue(fixture.images.images.isEmpty());
        assertFalse(fixture.assets.assets.containsKey("asset"));
    }

    @Test
    void deleteAssetKeepsDatabaseRowsWhenOssDeletionFails()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("source", "asset", 1L, "source", "source/a.png"));
        fixture.storage.failDelete = true;

        assertThrows(ServiceException.class, () -> fixture.service.deleteAsset(1L, "asset"));

        assertTrue(fixture.assets.assets.containsKey("asset"));
        assertEquals(1, fixture.images.images.size());
    }

    @Test
    void selectVisibleAssetsFiltersToCurrentUserCompletedVisibleAssets()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("visible", 1L, "completed", true));
        fixture.assets.insertAiProductAsset(fixture.asset("hidden", 1L, "completed", false));
        fixture.assets.insertAiProductAsset(fixture.asset("processing", 1L, "processing", true));
        fixture.assets.insertAiProductAsset(fixture.asset("other", 2L, "completed", true));

        List<AiProductAsset> result = fixture.service.selectVisibleAssets(1L);

        assertEquals(List.of("visible"), result.stream().map(AiProductAsset::getId).collect(Collectors.toList()));
    }

    @Test
    void renameRejectsBlankNamesAndOtherUsersAssets()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 2L, "completed", true));

        assertThrows(ServiceException.class, () -> fixture.service.renameAsset(1L, "asset", "name"));
        assertThrows(ServiceException.class, () -> fixture.service.renameAsset(1L, "asset", " "));
    }

    @Test
    void deleteDraftImageImmediatelyDeletesObjectAndRow()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("draft", 1L, "draft_uploaded", false));
        fixture.images.insertAiProductAssetImage(fixture.image("image", "draft", 1L, "source", "source/a.png"));

        fixture.service.deleteDraftImage(1L, "image");

        assertEquals(List.of("source/a.png"), fixture.storage.deletedObjectKeys);
        assertTrue(fixture.images.images.isEmpty());
    }

    private static class Fixture
    {
        private final FakeAssetMapper assets = new FakeAssetMapper();
        private final FakeImageMapper images = new FakeImageMapper();
        private final FakeStorage storage = new FakeStorage();
        private final AiProductAssetServiceImpl service = new AiProductAssetServiceImpl(assets, images, storage);

        private AiProductAsset asset(String id, Long userId, String status, boolean visible)
        {
            AiProductAsset asset = new AiProductAsset();
            asset.setId(id);
            asset.setUserId(userId);
            asset.setName("product");
            asset.setStatus(status);
            asset.setVisible(visible);
            return asset;
        }

        private AiProductAssetImage image(String id, String assetId, Long userId, String role, String key)
        {
            AiProductAssetImage image = new AiProductAssetImage();
            image.setId(id);
            image.setProductAssetId(assetId);
            image.setUserId(userId);
            image.setImageRole(role);
            image.setOssBucket("bucket");
            image.setOssObjectKey(key);
            image.setOssUrl("https://oss/" + key);
            image.setMimeType("image/png");
            image.setFileSize(1L);
            image.setSortOrder(0);
            return image;
        }
    }

    private static class FakeAssetMapper implements AiProductAssetMapper
    {
        private final Map<String, AiProductAsset> assets = new HashMap<>();

        @Override public AiProductAsset selectAiProductAssetById(String id) { return assets.get(id); }
        @Override public AiProductAsset selectCurrentDraft(Long userId) { return assets.values().stream().filter(asset -> userId.equals(asset.getUserId()) && "draft_uploaded".equals(asset.getStatus()) && Boolean.FALSE.equals(asset.getVisible())).findFirst().orElse(null); }
        @Override public List<AiProductAsset> selectVisibleAssetList(AiProductAsset query) { return assets.values().stream().filter(asset -> query.getUserId().equals(asset.getUserId()) && Boolean.TRUE.equals(asset.getVisible()) && "completed".equals(asset.getStatus())).collect(Collectors.toList()); }
        @Override public int insertAiProductAsset(AiProductAsset asset) { assets.put(asset.getId(), asset); return 1; }
        @Override public int updateAiProductAsset(AiProductAsset asset) { assets.put(asset.getId(), asset); return 1; }
        @Override public int deleteAiProductAssetById(String id, Long userId) { return assets.remove(id) != null ? 1 : 0; }
    }

    private static class FakeImageMapper implements AiProductAssetImageMapper
    {
        private final List<AiProductAssetImage> images = new ArrayList<>();

        @Override public AiProductAssetImage selectAiProductAssetImageById(String id) { return images.stream().filter(image -> id.equals(image.getId())).findFirst().orElse(null); }
        @Override public AiProductAssetImage selectImageByObjectKey(String objectKey) { return images.stream().filter(image -> objectKey.equals(image.getOssObjectKey())).findFirst().orElse(null); }
        @Override public List<AiProductAssetImage> selectImagesByAssetId(String assetId, Long userId) { return images.stream().filter(image -> assetId.equals(image.getProductAssetId()) && userId.equals(image.getUserId())).collect(Collectors.toList()); }
        @Override public List<AiProductAssetImage> selectImagesByTaskId(String taskId, Long userId) { return List.of(); }
        @Override public int insertAiProductAssetImage(AiProductAssetImage image) { images.add(image); return 1; }
        @Override public int updateAiProductAssetImage(AiProductAssetImage image) { return 1; }
        @Override public int deleteAiProductAssetImageById(String id, Long userId) { return images.removeIf(image -> id.equals(image.getId()) && userId.equals(image.getUserId())) ? 1 : 0; }
        @Override public int deleteImagesByAssetId(String assetId, Long userId) { return images.removeIf(image -> assetId.equals(image.getProductAssetId()) && userId.equals(image.getUserId())) ? 1 : 0; }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private int uploadCalls;
        private int copyCalls;
        private boolean failDelete;
        private final List<String> deletedObjectKeys = new ArrayList<>();

        @Override public AiStoredObject uploadKnowledgeFile(MultipartFile file) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId) { uploadCalls++; return stored("source/uploaded.png"); }
        @Override public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId) { copyCalls++; return stored("copy/" + sourceObjectKey); }
        @Override public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan) { return List.of(); }
        @Override public byte[] downloadObject(String objectKey) { throw new UnsupportedOperationException(); }
        @Override public void deleteObject(String objectKey) { if (failDelete) { throw new IllegalStateException("oss unavailable"); } deletedObjectKeys.add(objectKey); }

        private AiStoredObject stored(String key)
        {
            AiStoredObject object = new AiStoredObject();
            object.setBucket("bucket"); object.setObjectKey(key); object.setUrl("https://oss/" + key); object.setMimeType("image/png"); object.setSize(2L);
            return object;
        }
    }
}
