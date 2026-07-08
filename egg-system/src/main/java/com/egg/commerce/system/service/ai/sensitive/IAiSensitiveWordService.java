package com.egg.commerce.system.service.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import java.util.List;

public interface IAiSensitiveWordService
{
    AiSensitiveWord selectAiSensitiveWordById(Long id);

    List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query);

    int insertAiSensitiveWord(AiSensitiveWord word, Long createdBy);

    int updateAiSensitiveWord(AiSensitiveWord word);

    int deleteAiSensitiveWordByIds(Long[] ids);
}
