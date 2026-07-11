package com.egg.commerce.system.service.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import java.util.List;

/**
 * 敏感词分类服务契约。
 *
 * <p>用于后台维护敏感词分类，并在删除分类前保护仍有关联词条的数据。</p>
 */
public interface AiSensitiveCategoryService
{
    /** 根据分类 ID 查询单条分类。 */
    AiSensitiveCategory selectAiSensitiveCategoryById(Long id);

    /** 按条件查询敏感词分类列表。 */
    List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query);

    /** 新增分类，并记录创建管理员。 */
    int insertAiSensitiveCategory(AiSensitiveCategory category, Long createdBy);

    /** 更新分类名称或状态。 */
    int updateAiSensitiveCategory(AiSensitiveCategory category);

    /** 批量删除分类，分类下存在词条时实现层会阻止删除。 */
    int deleteAiSensitiveCategoryByIds(Long[] ids);
}
