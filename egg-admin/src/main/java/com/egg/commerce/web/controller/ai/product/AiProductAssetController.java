package com.egg.commerce.web.controller.ai.product;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.file.FileUtils;
import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDetail;
import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import com.egg.commerce.system.domain.ai.product.AiProductAssetRenameRequest;
import com.egg.commerce.system.service.ai.product.AiProductAssetService;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Product image draft and asset APIs for the current front user. */
@RestController
@RequestMapping("/ai/product-assets")
public class AiProductAssetController extends BaseController
{
    private final AiProductAssetService assetService;

    private final AiObjectStorageService objectStorageService;

    public AiProductAssetController(AiProductAssetService assetService,
            AiObjectStorageService objectStorageService)
    {
        this.assetService = assetService;
        this.objectStorageService = objectStorageService;
    }

    @GetMapping("/draft")
    public AjaxResult draft()
    {
        return success(assetService.selectCurrentDraft(getUserId()));
    }

    @PostMapping("/draft/images")
    public AjaxResult uploadDraftImage(@RequestParam("file") MultipartFile file)
    {
        return success(assetService.uploadDraftImage(getUserId(), file));
    }

    @DeleteMapping("/draft/images/{imageId}")
    public AjaxResult deleteDraftImage(@PathVariable String imageId)
    {
        assetService.deleteDraftImage(getUserId(), imageId);
        return success();
    }

    @DeleteMapping("/draft")
    public AjaxResult clearDraft()
    {
        assetService.clearCurrentDraft(getUserId());
        return success();
    }

    @GetMapping
    public AjaxResult list()
    {
        return success(assetService.selectVisibleAssets(getUserId()));
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id)
    {
        return success(assetService.selectAssetDetail(getUserId(), id));
    }

    @PutMapping("/{id}/name")
    public AjaxResult rename(@PathVariable String id, @RequestBody AiProductAssetRenameRequest request)
    {
        return success(assetService.renameAsset(getUserId(), id, request == null ? null : request.getName()));
    }

    @PostMapping("/{id}/reuse")
    public AjaxResult reuse(@PathVariable String id)
    {
        return success(assetService.reuseAsset(getUserId(), id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, HttpServletResponse response) throws IOException
    {
        AiProductAssetDetail detail = assetService.selectAssetDetail(getUserId(), id);
        List<AiProductAssetImage> generatedImages = detail.getGeneratedImages();
        if (generatedImages == null || generatedImages.isEmpty())
        {
            throw new ServiceException("No generated image is available for download");
        }

        AiProductAssetImage image = generatedImages.get(0);
        String objectKey = image.getOssObjectKey();
        if (objectKey == null || objectKey.trim().isEmpty())
        {
            throw new ServiceException("Generated image object is unavailable");
        }

        byte[] bytes = objectStorageService.downloadObject(objectKey);
        String contentType = image.getMimeType();
        response.setContentType(contentType == null || contentType.trim().isEmpty()
                ? "application/octet-stream" : contentType);
        response.setContentLength(bytes.length);
        FileUtils.setAttachmentResponseHeader(response, downloadFileName(detail.getAsset(), image));
        response.getOutputStream().write(bytes);
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id)
    {
        return success(assetService.deleteAsset(getUserId(), id));
    }

    private String downloadFileName(AiProductAsset asset, AiProductAssetImage image)
    {
        String name = asset == null ? null : asset.getName();
        String safeName = name == null ? "product-image" : name.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+", "_");
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
}
