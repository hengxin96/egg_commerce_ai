package com.egg.commerce.system.domain.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * 商品图领域对象测试，确保前后端和 Mapper 依赖的字段名稳定。
 */
class AiProductImageDomainTest
{
    @Test
    void productAssetKeepsDraftAndVisibilityFields()
    {
        AiProductAsset asset = new AiProductAsset();
        asset.setId("asset-1");
        asset.setUserId(99L);
        asset.setName("未命名商品");
        asset.setStatus("draft_uploaded");
        asset.setVisible(false);
        asset.setAssetType("main");
        asset.setTargetPlatform("抖音");
        asset.setAspectRatio("3:4");
        asset.setQuality("2K");
        asset.setUserRequirement("高端珠宝主图");

        assertEquals("asset-1", asset.getId());
        assertEquals(99L, asset.getUserId());
        assertEquals("draft_uploaded", asset.getStatus());
        assertFalse(asset.getVisible());
        assertEquals("main", asset.getAssetType());
        assertEquals("抖音", asset.getTargetPlatform());
        assertEquals("3:4", asset.getAspectRatio());
        assertEquals("高端珠宝主图", asset.getUserRequirement());
    }

    @Test
    void productImageTaskKeepsNodeOutputsAndProviderSnapshot()
    {
        AiProductImageTask task = new AiProductImageTask();
        task.setId("task-1");
        task.setProductAssetId("asset-1");
        task.setUserId(99L);
        task.setTaskType("product_image");
        task.setCurrentNode("生成中");
        task.setStatus("processing");
        task.setAnalysisOutput("{\"ok\":true}");
        task.setPlanningOutput("{\"imagePrompt\":{\"promptId\":\"main_01\"}}");
        task.setGenerationOutput("{}");
        task.setDashscopeTaskId("dashscope-task-1");
        task.setModelProvider("dashscope");
        task.setModelName("wan2.7-image-pro");

        assertEquals("product_image", task.getTaskType());
        assertEquals("生成中", task.getCurrentNode());
        assertEquals("{\"ok\":true}", task.getAnalysisOutput());
        assertEquals("dashscope-task-1", task.getDashscopeTaskId());
        assertEquals("wan2.7-image-pro", task.getModelName());
    }

    @Test
    void rewriteRequestAndResultKeepCurrentRequirementOnly()
    {
        AiRequirementRewriteRequest request = new AiRequirementRewriteRequest();
        request.setAssetType("detail");
        request.setTargetPlatform("抖音");
        request.setUserRequirement("突出规格和卖点");
        request.setProductAssetId("asset-1");

        AiRequirementRewriteResult result = new AiRequirementRewriteResult();
        result.setOptimizedRequirement("优化后的详情图要求");
        result.setDisplayTitle("优化后的主图要求");
        result.setImagePromptReady(true);

        assertEquals("detail", request.getAssetType());
        assertEquals("突出规格和卖点", request.getUserRequirement());
        assertEquals("优化后的详情图要求", result.getOptimizedRequirement());
        assertEquals(true, result.getImagePromptReady());
    }
}
