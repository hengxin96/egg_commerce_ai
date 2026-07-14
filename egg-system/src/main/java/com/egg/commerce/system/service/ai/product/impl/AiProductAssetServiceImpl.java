package com.egg.commerce.system.service.ai.product.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDetail;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductDraftDetail;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetImageMapper;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.service.ai.product.AiProductAssetService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private static final String DEFAULT_PRODUCT_NAME_PREFIX = "未命名商品-";

    private final AiProductAssetMapper assetMapper;
    private final AiProductAssetImageMapper imageMapper;
    private final AiObjectStorageService objectStorageService;

    public AiProductAssetServiceImpl(AiProductAssetMapper assetMapper, AiProductAssetImageMapper imageMapper,
            AiObjectStorageService objectStorageService)
    {
        this.assetMapper = assetMapper;
        this.imageMapper = imageMapper;
        this.objectStorageService = objectStorageService;
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
        return detail;
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
