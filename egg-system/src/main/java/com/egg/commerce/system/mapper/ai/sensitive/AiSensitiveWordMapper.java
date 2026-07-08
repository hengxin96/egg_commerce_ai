package com.egg.commerce.system.mapper.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import java.util.List;

public interface AiSensitiveWordMapper
{
    AiSensitiveWord selectAiSensitiveWordById(Long id);

    List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query);

    int insertAiSensitiveWord(AiSensitiveWord word);

    int updateAiSensitiveWord(AiSensitiveWord word);

    int deleteAiSensitiveWordByIds(Long[] ids);
}
