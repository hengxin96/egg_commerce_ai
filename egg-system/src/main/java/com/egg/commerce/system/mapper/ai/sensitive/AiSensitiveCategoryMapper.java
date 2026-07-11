package com.egg.commerce.system.mapper.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import java.util.List;

/**
 * 敏感词分类数据访问接口。
 */
public interface AiSensitiveCategoryMapper
{
    /** 根据分类 ID 查询单条分类。 */
    AiSensitiveCategory selectAiSensitiveCategoryById(Long id);

    /** 按条件查询分类列表。 */
    List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query);

    /** 新增敏感词分类。 */
    int insertAiSensitiveCategory(AiSensitiveCategory category);

    /** 更新敏感词分类。 */
    int updateAiSensitiveCategory(AiSensitiveCategory category);

    /** 统计指定分类下的词条数量，用于删除前保护。 */
    int countAiSensitiveWordsByCategoryIds(Long[] ids);

    /** 批量删除敏感词分类。 */
    int deleteAiSensitiveCategoryByIds(Long[] ids);
}
