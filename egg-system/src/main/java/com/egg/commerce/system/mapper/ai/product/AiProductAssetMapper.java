package com.egg.commerce.system.mapper.ai.product;

import com.egg.commerce.system.domain.ai.product.AiProductAsset;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 商品图商品资产数据访问接口，对应 ai_product_assets 表。
 */
public interface AiProductAssetMapper
{
    /** 根据商品资产 ID 查询单条记录。 */
    AiProductAsset selectAiProductAssetById(String id);

    /** 查询当前用户未开始分析的最新草稿，用于生成页恢复。 */
    AiProductAsset selectCurrentDraft(@Param("userId") Long userId);

    /** 查询当前用户可见商品资产列表，商品库只展示成功记录。 */
    List<AiProductAsset> selectVisibleAssetList(AiProductAsset query);

    /** 新增商品资产。 */
    int insertAiProductAsset(AiProductAsset asset);

    /** 更新商品资产状态、参数、封面和任务引用。 */
    int updateAiProductAsset(AiProductAsset asset);

    /** 按用户隔离删除商品资产。 */
    int deleteAiProductAssetById(@Param("id") String id, @Param("userId") Long userId);
}
