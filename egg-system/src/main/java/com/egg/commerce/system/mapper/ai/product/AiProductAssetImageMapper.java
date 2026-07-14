package com.egg.commerce.system.mapper.ai.product;

import com.egg.commerce.system.domain.ai.product.AiProductAssetImage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 商品图图片资产数据访问接口，对应 ai_product_asset_images 表。
 */
public interface AiProductAssetImageMapper
{
    /** 根据图片 ID 查询单条图片记录。 */
    AiProductAssetImage selectAiProductAssetImageById(String id);

    /** 根据 OSS object key 查询图片记录，供孤儿 OSS 清理判断引用。 */
    AiProductAssetImage selectImageByObjectKey(String ossObjectKey);

    /** 查询某个商品资产下当前用户的图片列表。 */
    List<AiProductAssetImage> selectImagesByAssetId(@Param("productAssetId") String productAssetId,
            @Param("userId") Long userId);

    /** 查询某个任务下当前用户的生成图片列表。 */
    List<AiProductAssetImage> selectImagesByTaskId(@Param("taskId") String taskId,
            @Param("userId") Long userId);

    /** 新增图片资产记录。 */
    int insertAiProductAssetImage(AiProductAssetImage image);

    /** 更新图片资产元数据或分析角色。 */
    int updateAiProductAssetImage(AiProductAssetImage image);

    /** 按用户隔离删除单张图片记录。 */
    int deleteAiProductAssetImageById(@Param("id") String id, @Param("userId") Long userId);

    /** 按用户隔离删除某个商品资产下的全部图片记录。 */
    int deleteImagesByAssetId(@Param("productAssetId") String productAssetId, @Param("userId") Long userId);
}
