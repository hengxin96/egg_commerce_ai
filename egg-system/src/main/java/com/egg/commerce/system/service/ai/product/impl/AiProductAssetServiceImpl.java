package com.egg.commerce.system.service.ai.product.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
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
import com.egg.commerce.system.service.ai.product.AiProductAssetService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Implements product asset draft lifecycle and product library access. */
@Service
public class AiProductAssetServiceImpl implements AiProductAssetService
{
    private static final String STATUS_DRAFT_UPLOADED = "draft_uploaded";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String ROLE_SOURCE = "source";
    private static final String ROLE_GENERATED = "generated";
    private static final DateTimeFormatter DOWNLOAD_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_PRODUCT_NAME_PREFIX = "未命名商品-";

    private final AiProductAssetMapper assetMapper;
    private final AiProductAssetImageMapper imageMapper;
    private final AiObjectStorageService objectStorageService;
    private final AiProductImageTaskMapper taskMapper;

    @Autowired
    public AiProductAssetServiceImpl(AiProductAssetMapper assetMapper, AiProductAssetImageMapper imageMapper,
            AiObjectStorageService objectStorageService, AiProductImageTaskMapper taskMapper)
    {
        this.assetMapper = assetMapper;
        this.imageMapper = imageMapper;
        this.objectStorageService = objectStorageService;
        this.taskMapper = taskMapper;
    }

    public AiProductAssetServiceImpl(AiProductAssetMapper assetMapper, AiProductAssetImageMapper imageMapper,
            AiObjectStorageService objectStorageService)
    {
        this(assetMapper, imageMapper, objectStorageService, null);
    }

    @Override
    public AiProductDraftDetail selectCurrentDraft(Long userId)
    {
        requireUserId(userId);
        AiProductAsset draft = assetMapper.selectCurrentDraft(userId);
        return draft == null ? null : draftDetail(draft, userId);
    }

    @Override
    public AiProductDraftDetail uploadDraftImage(Long userId, MultipartFile file)
    {
        requireUserId(userId);
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("图片不能为空");
        }

        AiProductAsset draft = assetMapper.selectCurrentDraft(userId);
        if (draft == null)
        {
            draft = newDraft(userId);
            assetMapper.insertAiProductAsset(draft);
        }
        AiStoredObject object = objectStorageService.uploadProductSourceImage(file, userId);
        AiProductAssetImage image = imageFromObject(draft.getId(), userId, ROLE_SOURCE, object,
                imageMapper.selectImagesByAssetId(draft.getId(), userId).size());
        imageMapper.insertAiProductAssetImage(image);
        return draftDetail(draft, userId);
    }

    @Override
    public void deleteDraftImage(Long userId, String imageId)
    {
        requireUserId(userId);
        requireId(imageId, "图片不存在");
        AiProductAsset draft = requireCurrentDraft(userId);
        AiProductAssetImage image = imageMapper.selectAiProductAssetImageById(imageId);
        if (image == null || !userId.equals(image.getUserId()) || !draft.getId().equals(image.getProductAssetId())
                || !ROLE_SOURCE.equals(image.getImageRole()))
        {
            throw new ServiceException("图片不存在");
        }
        deleteObject(image.getOssObjectKey());
        imageMapper.deleteAiProductAssetImageById(imageId, userId);
    }

    @Override
    public AiProductDraftDetail updateDraftImageOrder(Long userId, AiProductImageOrderRequest request)
    {
        requireUserId(userId);
        AiProductAsset draft = requireCurrentDraft(userId);
        if (request == null || request.getImageIds() == null || request.getImageIds().isEmpty())
        {
            throw new ServiceException("图片排序不能为空");
        }
        if (StringUtils.isBlank(request.getPrimaryImageId()))
        {
            throw new ServiceException("主参考图不能为空");
        }

        List<AiProductAssetImage> sourceImages = filterImages(
                imageMapper.selectImagesByAssetId(draft.getId(), userId), ROLE_SOURCE);
        Map<String, AiProductAssetImage> sourceById = new LinkedHashMap<>();
        for (AiProductAssetImage image : sourceImages)
        {
            sourceById.put(image.getId(), image);
        }

        Set<String> requestedIds = new LinkedHashSet<>(request.getImageIds());
        if (requestedIds.size() != request.getImageIds().size() || !requestedIds.equals(sourceById.keySet())
                || !requestedIds.contains(request.getPrimaryImageId()))
        {
            throw new ServiceException("图片排序必须包含当前草稿的全部源图");
        }

        List<String> orderedIds = new ArrayList<>();
        orderedIds.add(request.getPrimaryImageId());
        for (String imageId : request.getImageIds())
        {
            if (!request.getPrimaryImageId().equals(imageId))
            {
                orderedIds.add(imageId);
            }
        }

        for (int index = 0; index < orderedIds.size(); index++)
        {
            AiProductAssetImage image = sourceById.get(orderedIds.get(index));
            image.setSortOrder(index);
            image.setUpdatedAt(new Date());
            imageMapper.updateAiProductAssetImage(image);
        }
        return draftDetail(draft, userId);
    }

    @Override
    public void clearCurrentDraft(Long userId)
    {
        requireUserId(userId);
        AiProductAsset draft = assetMapper.selectCurrentDraft(userId);
        if (draft == null)
        {
            return;
        }
        List<AiProductAssetImage> images = imageMapper.selectImagesByAssetId(draft.getId(), userId);
        deleteObjects(images);
        imageMapper.deleteImagesByAssetId(draft.getId(), userId);
        assetMapper.deleteAiProductAssetById(draft.getId(), userId);
    }

    @Override
    public List<AiProductAsset> selectVisibleAssets(Long userId)
    {
        requireUserId(userId);
        AiProductAsset query = new AiProductAsset();
        query.setUserId(userId);
        query.setStatus(STATUS_COMPLETED);
        query.setVisible(true);
        return assetMapper.selectVisibleAssetList(query);
    }

    @Override
    public AiProductAssetDetail selectAssetDetail(Long userId, String assetId)
    {
        requireUserId(userId);
        return assetDetail(requireOwnedAsset(userId, assetId), userId);
    }

    @Override
    public AiProductAssetDownload downloadGeneratedImage(Long userId, String assetId)
    {
        // Resolve through the ownership-aware detail path before reading the stored object.
        AiProductAssetDetail detail = selectAssetDetail(userId, assetId);
        List<AiProductAssetImage> generatedImages = detail.getGeneratedImages();
        if (generatedImages == null || generatedImages.isEmpty())
        {
            throw new ServiceException("No generated image is available for download");
        }

        AiProductAssetImage image = generatedImages.get(0);
        if (StringUtils.isBlank(image.getOssObjectKey()))
        {
            throw new ServiceException("Generated image object is unavailable");
        }

        AiProductAssetDownload download = new AiProductAssetDownload();
        download.setBytes(objectStorageService.downloadObject(image.getOssObjectKey()));
        download.setContentType(StringUtils.isBlank(image.getMimeType())
                ? "application/octet-stream" : image.getMimeType());
        download.setFileName(downloadFileName(detail.getAsset(), image));
        return download;
    }

    @Override
    public AiProductAssetDownload downloadGeneratedImageByImageId(Long userId, String imageId)
    {
        AiProductAssetImage image = requireOwnedGeneratedImage(userId, imageId);
        AiProductAssetDownload download = new AiProductAssetDownload();
        download.setBytes(objectStorageService.downloadObject(image.getOssObjectKey()));
        download.setContentType(StringUtils.isBlank(image.getMimeType())
                ? "application/octet-stream" : image.getMimeType());
        download.setFileName(timestampBase() + "-01." + extension(image));
        return download;
    }

    @Override
    public AiProductAssetDownload downloadGeneratedImagesZip(Long userId, List<String> imageIds)
    {
        requireUserId(userId);
        if (imageIds == null || imageIds.isEmpty())
        {
            throw new ServiceException("请选择要下载的生成图");
        }
        String timestamp = timestampBase();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes))
        {
            int index = 1;
            for (String imageId : imageIds)
            {
                AiProductAssetImage image = requireOwnedGeneratedImage(userId, imageId);
                ZipEntry entry = new ZipEntry(timestamp + "-" + String.format("%02d", index) + "."
                        + extension(image));
                zip.putNextEntry(entry);
                zip.write(objectStorageService.downloadObject(image.getOssObjectKey()));
                zip.closeEntry();
                index++;
            }
            zip.finish();
            AiProductAssetDownload download = new AiProductAssetDownload();
            download.setBytes(bytes.toByteArray());
            download.setContentType("application/zip");
            download.setFileName(timestamp + ".zip");
            return download;
        }
        catch (Exception e)
        {
            throw new ServiceException("生成图 ZIP 下载失败");
        }
    }

    @Override
    public AiProductAsset renameAsset(Long userId, String assetId, String name)
    {
        requireUserId(userId);
        if (StringUtils.isBlank(name))
        {
            throw new ServiceException("商品名称不能为空");
        }
        AiProductAsset asset = requireOwnedAsset(userId, assetId);
        asset.setName(name.trim());
        asset.setUpdatedAt(new Date());
        assetMapper.updateAiProductAsset(asset);
        return asset;
    }

    @Override
    public AiProductDraftDetail reuseAsset(Long userId, String assetId)
    {
        requireUserId(userId);
        AiProductAsset source = requireOwnedAsset(userId, assetId);
        if (!STATUS_COMPLETED.equals(source.getStatus()) || !Boolean.TRUE.equals(source.getVisible()))
        {
            throw new ServiceException("商品不存在");
        }
        AiProductAsset draft = newDraft(userId);
        draft.setSourceProductId(source.getId());
        draft.setAssetType(source.getAssetType());
        draft.setTargetPlatform(source.getTargetPlatform());
        draft.setAspectRatio(source.getAspectRatio());
        draft.setQuality(source.getQuality());
        draft.setUserRequirement(source.getUserRequirement());
        assetMapper.insertAiProductAsset(draft);

        List<AiProductAssetImage> sourceImages = imageMapper.selectImagesByAssetId(source.getId(), userId);
        int sortOrder = 0;
        for (AiProductAssetImage sourceImage : sourceImages)
        {
            if (!ROLE_SOURCE.equals(sourceImage.getImageRole()))
            {
                continue;
            }
            AiStoredObject copy = objectStorageService.copyProductImageObject(sourceImage.getOssObjectKey(), userId);
            AiProductAssetImage copiedImage = imageFromObject(draft.getId(), userId, ROLE_SOURCE, copy, sortOrder++);
            copiedImage.setAnalysisRole(sourceImage.getAnalysisRole());
            imageMapper.insertAiProductAssetImage(copiedImage);
        }
        return draftDetail(draft, userId);
    }

    @Override
    public AiProductAssetDetail deleteAsset(Long userId, String assetId)
    {
        requireUserId(userId);
        AiProductAsset asset = requireOwnedAsset(userId, assetId);
        AiProductAssetDetail detail = assetDetail(asset, userId);
        List<AiProductAssetImage> images = imageMapper.selectImagesByAssetId(asset.getId(), userId);
        deleteObjects(images);
        imageMapper.deleteImagesByAssetId(asset.getId(), userId);
        assetMapper.deleteAiProductAssetById(asset.getId(), userId);
        return detail;
    }

    private AiProductAsset newDraft(Long userId)
    {
        AiProductAsset draft = new AiProductAsset();
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        draft.setId(id);
        draft.setUserId(userId);
        draft.setName(DEFAULT_PRODUCT_NAME_PREFIX + id.substring(0, 8));
        draft.setStatus(STATUS_DRAFT_UPLOADED);
        draft.setVisible(false);
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        return draft;
    }

    private AiProductDraftDetail draftDetail(AiProductAsset draft, Long userId)
    {
        AiProductDraftDetail detail = new AiProductDraftDetail();
        detail.setAsset(draft);
        detail.setSourceImages(filterImages(imageMapper.selectImagesByAssetId(draft.getId(), userId), ROLE_SOURCE));
        return detail;
    }

    private AiProductAssetDetail assetDetail(AiProductAsset asset, Long userId)
    {
        List<AiProductAssetImage> images = imageMapper.selectImagesByAssetId(asset.getId(), userId);
        AiProductAssetDetail detail = new AiProductAssetDetail();
        detail.setAsset(asset);
        detail.setSourceImages(filterImages(images, ROLE_SOURCE));
        detail.setGeneratedImages(filterImages(images, ROLE_GENERATED));
        AiProductImageTask latestTask = latestCompletedTask(asset, userId);
        // Product detail pages need both the final audit result and the reviewed planning spec as read-only context.
        detail.setAuditOutput(latestTask == null ? null : latestTask.getGenerationOutput());
        detail.setPlanningOutput(latestTask == null ? null : latestTask.getPlanningOutput());
        return detail;
    }

    private AiProductImageTask latestCompletedTask(AiProductAsset asset, Long userId)
    {
        if (taskMapper == null || asset == null || StringUtils.isBlank(asset.getLastTaskId()))
        {
            return null;
        }
        AiProductImageTask task = taskMapper.selectAiProductImageTaskById(asset.getLastTaskId());
        if (task == null || !userId.equals(task.getUserId()) || !asset.getId().equals(task.getProductAssetId())
                || !STATUS_COMPLETED.equals(task.getStatus()))
        {
            return null;
        }
        return task;
    }

    private List<AiProductAssetImage> filterImages(List<AiProductAssetImage> images, String role)
    {
        List<AiProductAssetImage> matched = new ArrayList<>();
        for (AiProductAssetImage image : images)
        {
            if (role.equals(image.getImageRole()))
            {
                matched.add(image);
            }
        }
        matched.sort(Comparator.comparing(image -> image.getSortOrder() == null ? 0 : image.getSortOrder()));
        return matched;
    }

    private AiProductAssetImage imageFromObject(String assetId, Long userId, String role, AiStoredObject object,
            int sortOrder)
    {
        AiProductAssetImage image = new AiProductAssetImage();
        Date now = new Date();
        image.setId(UUID.randomUUID().toString());
        image.setProductAssetId(assetId);
        image.setUserId(userId);
        image.setImageRole(role);
        image.setOssBucket(object.getBucket());
        image.setOssObjectKey(object.getObjectKey());
        image.setOssUrl(object.getUrl());
        image.setMimeType(object.getMimeType());
        image.setFileSize(object.getSize());
        image.setSortOrder(sortOrder);
        image.setCreatedAt(now);
        image.setUpdatedAt(now);
        return image;
    }

    private String downloadFileName(AiProductAsset asset, AiProductAssetImage image)
    {
        String name = asset == null ? null : asset.getName();
        String safeName = StringUtils.isBlank(name) ? "product-image"
                : name.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+", "_");
        if (safeName.isEmpty())
        {
            safeName = "product-image";
        }
        if (safeName.length() > 100)
        {
            safeName = safeName.substring(0, 100);
        }
        return safeName + "." + extension(image);
    }

    private String extension(AiProductAssetImage image)
    {
        String mimeType = image.getMimeType();
        if ("image/jpeg".equalsIgnoreCase(mimeType))
        {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(mimeType))
        {
            return "png";
        }
        if ("image/webp".equalsIgnoreCase(mimeType))
        {
            return "webp";
        }
        if ("image/gif".equalsIgnoreCase(mimeType))
        {
            return "gif";
        }

        String objectKey = image.getOssObjectKey();
        int index = objectKey == null ? -1 : objectKey.lastIndexOf('.');
        String extension = index < 0 ? "" : objectKey.substring(index + 1);
        return extension.matches("[A-Za-z0-9]{1,10}") ? extension.toLowerCase() : "bin";
    }

    private AiProductAssetImage requireOwnedGeneratedImage(Long userId, String imageId)
    {
        requireUserId(userId);
        requireId(imageId, "图片不存在");
        AiProductAssetImage image = imageMapper.selectAiProductAssetImageById(imageId);
        if (image == null || !userId.equals(image.getUserId()) || !ROLE_GENERATED.equals(image.getImageRole()))
        {
            throw new ServiceException("图片不存在");
        }
        if (StringUtils.isBlank(image.getOssObjectKey()))
        {
            throw new ServiceException("Generated image object is unavailable");
        }
        return image;
    }

    private String timestampBase()
    {
        return LocalDateTime.now().format(DOWNLOAD_TIME_FORMAT);
    }

    private AiProductAsset requireCurrentDraft(Long userId)
    {
        AiProductAsset draft = assetMapper.selectCurrentDraft(userId);
        if (draft == null)
        {
            throw new ServiceException("草稿不存在");
        }
        return draft;
    }

    private AiProductAsset requireOwnedAsset(Long userId, String assetId)
    {
        requireId(assetId, "商品不存在");
        AiProductAsset asset = assetMapper.selectAiProductAssetById(assetId);
        if (asset == null || !userId.equals(asset.getUserId()))
        {
            throw new ServiceException("商品不存在");
        }
        return asset;
    }

    private void deleteObjects(List<AiProductAssetImage> images)
    {
        for (AiProductAssetImage image : images)
        {
            deleteObject(image.getOssObjectKey());
        }
    }

    private void deleteObject(String objectKey)
    {
        try
        {
            objectStorageService.deleteObject(objectKey);
        }
        catch (RuntimeException e)
        {
            throw new ServiceException("删除图片文件失败");
        }
    }

    private void requireUserId(Long userId)
    {
        if (userId == null || userId <= 0)
        {
            throw new ServiceException("用户不存在");
        }
    }

    private void requireId(String id, String message)
    {
        if (StringUtils.isBlank(id))
        {
            throw new ServiceException(message);
        }
    }
}
