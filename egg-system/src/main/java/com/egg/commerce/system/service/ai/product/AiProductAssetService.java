package com.egg.commerce.system.service.ai.product;

import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDownload;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDetail;
import com.egg.commerce.system.domain.ai.product.AiProductDraftDetail;
import com.egg.commerce.system.domain.ai.product.AiProductImageOrderRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Product image asset, draft, and reuse service. */
public interface AiProductAssetService
{
    AiProductDraftDetail selectCurrentDraft(Long userId);

    AiProductDraftDetail uploadDraftImage(Long userId, MultipartFile file);

    void deleteDraftImage(Long userId, String imageId);

    AiProductDraftDetail updateDraftImageOrder(Long userId, AiProductImageOrderRequest request);

    void clearCurrentDraft(Long userId);

    List<AiProductAsset> selectVisibleAssets(Long userId);

    AiProductAssetDetail selectAssetDetail(Long userId, String assetId);

    AiProductAssetDownload downloadGeneratedImage(Long userId, String assetId);

    AiProductAssetDownload downloadGeneratedImageByImageId(Long userId, String imageId);

    AiProductAssetDownload downloadGeneratedImagesZip(Long userId, List<String> imageIds);

    AiProductAsset renameAsset(Long userId, String assetId, String name);

    AiProductDraftDetail reuseAsset(Long userId, String assetId);

    AiProductAssetDetail deleteAsset(Long userId, String assetId);
}
