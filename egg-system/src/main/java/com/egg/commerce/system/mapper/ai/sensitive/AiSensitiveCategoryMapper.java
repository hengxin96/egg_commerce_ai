package com.egg.commerce.system.mapper.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import java.util.List;

public interface AiSensitiveCategoryMapper
{
    AiSensitiveCategory selectAiSensitiveCategoryById(Long id);

    List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query);

    int insertAiSensitiveCategory(AiSensitiveCategory category);

    int updateAiSensitiveCategory(AiSensitiveCategory category);

    int countAiSensitiveWordsByCategoryIds(Long[] ids);

    int deleteAiSensitiveCategoryByIds(Long[] ids);
}
