package com.egg.commerce.system.service.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import java.util.List;

public interface IAiSensitiveCategoryService
{
    AiSensitiveCategory selectAiSensitiveCategoryById(Long id);

    List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query);

    int insertAiSensitiveCategory(AiSensitiveCategory category, Long createdBy);

    int updateAiSensitiveCategory(AiSensitiveCategory category);

    int deleteAiSensitiveCategoryByIds(Long[] ids);
}
