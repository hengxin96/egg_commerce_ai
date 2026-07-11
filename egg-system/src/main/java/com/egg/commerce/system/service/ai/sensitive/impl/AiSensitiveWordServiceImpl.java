package com.egg.commerce.system.service.ai.sensitive.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveWordMapper;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveWordService;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 敏感词条服务实现。
 *
 * <p>词条是后续 AI 文案审核、商品标题审核和素材风险提示的基础规则数据。这里负责最基础的
 * 空值校验、默认状态补齐和审计字段维护。</p>
 */
@Service
public class AiSensitiveWordServiceImpl implements AiSensitiveWordService
{
    /** 后台新增敏感词未指定状态时默认启用，确保维护后的规则可立即参与审核。 */
    private static final String STATUS_ENABLED = "enabled";

    /** 敏感词条 Mapper，负责词条表读写和分类名称联查。 */
    private final AiSensitiveWordMapper sensitiveWordMapper;

    /**
     * 注入敏感词条 mapper，服务层负责词条校验和审计字段维护。
     */
    @Autowired
    public AiSensitiveWordServiceImpl(AiSensitiveWordMapper sensitiveWordMapper)
    {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    /**
     * 按 ID 查询敏感词条详情。
     */
    @Override
    public AiSensitiveWord selectAiSensitiveWordById(Long id)
    {
        return sensitiveWordMapper.selectAiSensitiveWordById(id);
    }

    /**
     * 查询后台敏感词条列表。
     */
    @Override
    public List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query)
    {
        return sensitiveWordMapper.selectAiSensitiveWordList(query);
    }

    /**
     * 新增敏感词条，保存前校验词条内容并补齐默认状态。
     */
    @Override
    public int insertAiSensitiveWord(AiSensitiveWord word, Long createdBy)
    {
        validateWord(word);
        Date now = new Date();
        // 后台表单可省略状态，服务层统一补默认值，避免空状态进入审核规则数据。
        word.setStatus(StringUtils.defaultIfBlank(word.getStatus(), STATUS_ENABLED));
        word.setCreatedBy(createdBy);
        word.setCreatedAt(now);
        word.setUpdatedAt(now);
        return sensitiveWordMapper.insertAiSensitiveWord(word);
    }

    /**
     * 更新敏感词条，保存前重新校验词条内容。
     */
    @Override
    public int updateAiSensitiveWord(AiSensitiveWord word)
    {
        validateWord(word);
        // 修改词条时只刷新更新时间，创建人仍代表首次录入规则的人。
        word.setUpdatedAt(new Date());
        return sensitiveWordMapper.updateAiSensitiveWord(word);
    }

    /**
     * 批量删除敏感词条。
     */
    @Override
    public int deleteAiSensitiveWordByIds(Long[] ids)
    {
        return sensitiveWordMapper.deleteAiSensitiveWordByIds(ids);
    }

    /**
     * 校验敏感词内容不能为空，避免无效审核规则入库。
     */
    private void validateWord(AiSensitiveWord word)
    {
        if (word == null || StringUtils.isBlank(word.getWord()))
        {
            throw new ServiceException("敏感词不能为空");
        }
    }
}
