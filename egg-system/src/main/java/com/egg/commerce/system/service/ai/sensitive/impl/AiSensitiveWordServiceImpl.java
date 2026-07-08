package com.egg.commerce.system.service.ai.sensitive.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveWordMapper;
import com.egg.commerce.system.service.ai.sensitive.IAiSensitiveWordService;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class AiSensitiveWordServiceImpl implements IAiSensitiveWordService
{
    private static final String STATUS_ENABLED = "enabled";

    private final AiSensitiveWordMapper sensitiveWordMapper;

    public AiSensitiveWordServiceImpl(AiSensitiveWordMapper sensitiveWordMapper)
    {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public AiSensitiveWord selectAiSensitiveWordById(Long id)
    {
        return sensitiveWordMapper.selectAiSensitiveWordById(id);
    }

    @Override
    public List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query)
    {
        return sensitiveWordMapper.selectAiSensitiveWordList(query);
    }

    @Override
    public int insertAiSensitiveWord(AiSensitiveWord word, Long createdBy)
    {
        validateWord(word);
        Date now = new Date();
        // Words are also editable from small admin forms, so missing status should land in the enabled state.
        word.setStatus(StringUtils.defaultIfBlank(word.getStatus(), STATUS_ENABLED));
        word.setCreatedBy(createdBy);
        word.setCreatedAt(now);
        word.setUpdatedAt(now);
        return sensitiveWordMapper.insertAiSensitiveWord(word);
    }

    @Override
    public int updateAiSensitiveWord(AiSensitiveWord word)
    {
        validateWord(word);
        word.setUpdatedAt(new Date());
        return sensitiveWordMapper.updateAiSensitiveWord(word);
    }

    @Override
    public int deleteAiSensitiveWordByIds(Long[] ids)
    {
        return sensitiveWordMapper.deleteAiSensitiveWordByIds(ids);
    }

    private void validateWord(AiSensitiveWord word)
    {
        if (word == null || StringUtils.isBlank(word.getWord()))
        {
            throw new ServiceException("Sensitive word cannot be blank");
        }
    }
}
