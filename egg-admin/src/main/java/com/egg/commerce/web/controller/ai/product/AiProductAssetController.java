package com.egg.commerce.web.controller.ai.product;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.utils.file.FileUtils;
import com.egg.commerce.system.domain.ai.product.AiProductAssetDownload;
import com.egg.commerce.system.domain.ai.product.AiProductImageDownloadRequest;
import com.egg.commerce.system.domain.ai.product.AiProductImageOrderRequest;
import com.egg.commerce.system.domain.ai.product.AiProductAssetRenameRequest;
import com.egg.commerce.system.service.ai.product.AiProductAssetService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    public AiProductAssetController(AiProductAssetService assetService)
    {
        this.assetService = assetService;
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

    @PutMapping("/draft/images/order")
    public AjaxResult updateDraftImageOrder(@RequestBody AiProductImageOrderRequest request)
    {
        return success(assetService.updateDraftImageOrder(getUserId(), request));
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
        AiProductAssetDownload download = assetService.downloadGeneratedImage(getUserId(), id);
        writeDownload(response, download);
    }

    @GetMapping("/images/{imageId}/download")
    public void downloadImage(@PathVariable String imageId, HttpServletResponse response) throws IOException
    {
        AiProductAssetDownload download = assetService.downloadGeneratedImageByImageId(getUserId(), imageId);
        writeDownload(response, download);
    }

    @PostMapping("/images/download-zip")
    public void downloadImagesZip(@RequestBody AiProductImageDownloadRequest request,
            HttpServletResponse response) throws IOException
    {
        AiProductAssetDownload download = assetService.downloadGeneratedImagesZip(getUserId(),
                request == null ? null : request.getImageIds());
        writeDownload(response, download);
    }

    private void writeDownload(HttpServletResponse response, AiProductAssetDownload download) throws IOException
    {
        response.setContentType(download.getContentType());
        response.setContentLength(download.getBytes().length);
        FileUtils.setAttachmentResponseHeader(response, download.getFileName());
        response.getOutputStream().write(download.getBytes());
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id)
    {
        return success(assetService.deleteAsset(getUserId(), id));
    }
}
