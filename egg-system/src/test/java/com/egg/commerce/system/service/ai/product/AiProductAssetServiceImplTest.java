package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDownload;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDetail;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductDraftDetail;
import com.egg.commerce.system.domain.ai.product.AiProductImageOrderRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageTask;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductImageTaskMapper;
import com.egg.commerce.system.service.ai.product.impl.AiProductAssetServiceImpl;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import com.egg.commerce.system.service.ai.storage.AiStoredObjectSummary;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
    void reuseRejectsOwnedAssetsThatAreNotCompletedAndVisible()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("failed", 1L, "failed", false));
        fixture.assets.insertAiProductAsset(fixture.asset("hidden", 1L, "completed", false));

        assertThrows(ServiceException.class, () -> fixture.service.reuseAsset(1L, "failed"));
        assertThrows(ServiceException.class, () -> fixture.service.reuseAsset(1L, "hidden"));
        assertEquals(0, fixture.storage.copyCalls);
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
    void assetDetailReturnsLatestTaskAuditOutput()
    {
        Fixture fixture = new Fixture();
        AiProductAsset asset = fixture.asset("asset", 1L, "completed", true);
        asset.setLastTaskId("task-1");
        fixture.assets.insertAiProductAsset(asset);
        AiProductImageTask task = new AiProductImageTask();
        task.setId("task-1");
        task.setProductAssetId("asset");
        task.setUserId(1L);
        task.setStatus("completed");
        task.setGenerationOutput("{\"auditSummary\":{\"status\":\"warning\"}}");
        fixture.tasks.task = task;

        AiProductAssetDetail detail = fixture.service.selectAssetDetail(1L, "asset");

        assertTrue(detail.getAuditOutput().contains("\"auditSummary\""));
    }

    @Test
    void assetDetailReturnsLatestCompletedTaskPlanningOutput()
    {
        Fixture fixture = new Fixture();
        AiProductAsset asset = fixture.asset("asset", 1L, "completed", true);
        asset.setLastTaskId("task-1");
        fixture.assets.insertAiProductAsset(asset);
        AiProductImageTask task = new AiProductImageTask();
        task.setId("task-1");
        task.setProductAssetId("asset");
        task.setUserId(1L);
        task.setStatus("completed");
        task.setGenerationOutput("{\"auditSummary\":{\"status\":\"pass\"}}");
        task.setPlanningOutput("{\"overallDesignSpec\":{\"lighting\":\"soft window light\"}}");
        fixture.tasks.task = task;

        AiProductAssetDetail detail = fixture.service.selectAssetDetail(1L, "asset");

        assertTrue(detail.getPlanningOutput().contains("\"overallDesignSpec\""));
        assertTrue(detail.getPlanningOutput().contains("soft window light"));
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

    @Test
    void updateDraftImageOrderMovesPrimarySourceImageToFirstPosition()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("draft", 1L, "draft_uploaded", false));
        fixture.images.insertAiProductAssetImage(fixture.image("source-1", "draft", 1L, "source", "source/1.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("source-2", "draft", 1L, "source", "source/2.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("source-3", "draft", 1L, "source", "source/3.png"));
        AiProductImageOrderRequest request = new AiProductImageOrderRequest();
        request.setImageIds(List.of("source-1", "source-2", "source-3"));
        request.setPrimaryImageId("source-2");

        AiProductDraftDetail detail = fixture.service.updateDraftImageOrder(1L, request);

        assertEquals(List.of("source-2", "source-1", "source-3"),
                detail.getSourceImages().stream().map(AiProductAssetImage::getId).collect(Collectors.toList()));
        assertEquals(List.of(0, 1, 2),
                detail.getSourceImages().stream().map(AiProductAssetImage::getSortOrder).collect(Collectors.toList()));
    }

    @Test
    void updateDraftImageOrderRejectsImagesOutsideCurrentDraft()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("draft", 1L, "draft_uploaded", false));
        fixture.images.insertAiProductAssetImage(fixture.image("source-1", "draft", 1L, "source", "source/1.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("other-user", "draft", 2L, "source", "source/other.png"));
        AiProductImageOrderRequest request = new AiProductImageOrderRequest();
        request.setImageIds(List.of("source-1", "other-user"));
        request.setPrimaryImageId("other-user");

        assertThrows(ServiceException.class, () -> fixture.service.updateDraftImageOrder(1L, request));
    }

    @Test
    void downloadGeneratedImageReturnsFirstOwnedGeneratedImage()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("source", "asset", 1L, "source", "source/a.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("generated-1", "asset", 1L, "generated", "generated/a.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("generated-2", "asset", 1L, "generated", "generated/b.png"));

        AiProductAssetDownload download = fixture.service.downloadGeneratedImage(1L, "asset");

        assertEquals("generated/a.png", fixture.storage.downloadedObjectKey);
        assertArrayEquals(new byte[] {7, 8}, download.getBytes());
        assertEquals("image/png", download.getContentType());
        assertEquals("product.png", download.getFileName());
    }

    @Test
    void downloadGeneratedImageByImageIdReturnsOwnedGeneratedImageWithTimeSequenceName()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("generated-1", "asset", 1L, "generated", "generated/a.png"));

        AiProductAssetDownload download = fixture.service.downloadGeneratedImageByImageId(1L, "generated-1");

        assertEquals("generated/a.png", fixture.storage.downloadedObjectKey);
        assertArrayEquals(new byte[] {7, 8}, download.getBytes());
        assertEquals("image/png", download.getContentType());
        assertTrue(download.getFileName().matches("\\d{14}-01\\.png"));
    }

    @Test
    void downloadGeneratedImageByImageIdRejectsSourceAndOtherUsersImages()
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("source", "asset", 1L, "source", "source/a.png"));
        fixture.images.insertAiProductAssetImage(fixture.image("other", "asset", 2L, "generated", "generated/other.png"));

        assertThrows(ServiceException.class, () -> fixture.service.downloadGeneratedImageByImageId(1L, "source"));
        assertThrows(ServiceException.class, () -> fixture.service.downloadGeneratedImageByImageId(1L, "other"));
    }

    @Test
    void downloadGeneratedImagesZipContainsSelectedImagesWithTimeSequenceNames() throws IOException
    {
        Fixture fixture = new Fixture();
        fixture.assets.insertAiProductAsset(fixture.asset("asset", 1L, "completed", true));
        fixture.images.insertAiProductAssetImage(fixture.image("generated-1", "asset", 1L, "generated", "generated/a.png"));
        AiProductAssetImage jpg = fixture.image("generated-2", "asset", 1L, "generated", "generated/b.jpg");
        jpg.setMimeType("image/jpeg");
        fixture.images.insertAiProductAssetImage(jpg);

        AiProductAssetDownload download = fixture.service.downloadGeneratedImagesZip(1L,
                List.of("generated-2", "generated-1"));

        assertEquals("application/zip", download.getContentType());
        assertTrue(download.getFileName().matches("\\d{14}\\.zip"));
        List<String> entries = zipEntries(download.getBytes());
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).matches("\\d{14}-01\\.jpg"));
        assertTrue(entries.get(1).matches("\\d{14}-02\\.png"));
    }

    private List<String> zipEntries(byte[] bytes) throws IOException
    {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes)))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private static class Fixture
    {
        private final FakeAssetMapper assets = new FakeAssetMapper();
        private final FakeImageMapper images = new FakeImageMapper();
        private final FakeTaskMapper tasks = new FakeTaskMapper();
        private final FakeStorage storage = new FakeStorage();
        private final AiProductAssetServiceImpl service = new AiProductAssetServiceImpl(assets, images, storage, tasks);

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
        @Override public List<AiProductAsset> selectExpiredDrafts(Date expireBefore) { return List.of(); }
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

    private static class FakeTaskMapper implements AiProductImageTaskMapper
    {
        private AiProductImageTask task;

        @Override public AiProductImageTask selectAiProductImageTaskById(String id) { return task != null && id.equals(task.getId()) ? task : null; }
        @Override public List<AiProductImageTask> selectAiProductImageTaskList(AiProductImageTask query) { return task == null ? List.of() : List.of(task); }
        @Override public int insertAiProductImageTask(AiProductImageTask task) { this.task = task; return 1; }
        @Override public int updateAiProductImageTask(AiProductImageTask task) { this.task = task; return 1; }
        @Override public int updateAiProductImageTaskIfStatus(AiProductImageTask task, String expectedStatus) { this.task = task; return 1; }
    }

    private static class FakeStorage implements AiObjectStorageService
    {
        private int uploadCalls;
        private int copyCalls;
        private boolean failDelete;
        private String downloadedObjectKey;
        private final List<String> deletedObjectKeys = new ArrayList<>();

        @Override public AiStoredObject uploadKnowledgeFile(MultipartFile file) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId) { uploadCalls++; return stored("source/uploaded.png"); }
        @Override public AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId) { throw new UnsupportedOperationException(); }
        @Override public AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId) { copyCalls++; return stored("copy/" + sourceObjectKey); }
        @Override public List<AiStoredObjectSummary> listProductImageObjects(Date olderThan) { return List.of(); }
        @Override public byte[] downloadObject(String objectKey) { downloadedObjectKey = objectKey; return new byte[] {7, 8}; }
        @Override public String presignObjectUrl(String objectKey, long expireSeconds) { return "https://signed.example/" + objectKey; }
        @Override public void deleteObject(String objectKey) { if (failDelete) { throw new IllegalStateException("oss unavailable"); } deletedObjectKeys.add(objectKey); }

        private AiStoredObject stored(String key)
        {
            AiStoredObject object = new AiStoredObject();
            object.setBucket("bucket"); object.setObjectKey(key); object.setUrl("https://oss/" + key); object.setMimeType("image/png"); object.setSize(2L);
            return object;
        }
    }
}
