package com.egg.commerce.system.mapper.ai.product;

import com.egg.commerce.system.domain.ai.product.AiProductImageTask;
import java.util.List;

/**
 * 商品图生成任务数据访问接口，对应 ai_product_image_tasks 表。
 */
public interface AiProductImageTaskMapper
{
    /** 根据任务 ID 查询单条商品图任务。 */
    AiProductImageTask selectAiProductImageTaskById(String id);

    /** 按用户、状态等条件查询商品图任务列表。 */
    List<AiProductImageTask> selectAiProductImageTaskList(AiProductImageTask query);

    /** 新增商品图任务。 */
    int insertAiProductImageTask(AiProductImageTask task);

    /** 更新商品图任务节点、输出、失败/终止信息和模型快照。 */
    int updateAiProductImageTask(AiProductImageTask task);
}
