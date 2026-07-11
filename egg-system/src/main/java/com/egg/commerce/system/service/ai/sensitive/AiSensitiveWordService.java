package com.egg.commerce.system.service.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import java.util.List;

/**
 * 敏感词条服务契约。
 *
 * <p>用于后台维护敏感词基础规则，后续 AI 文案审核和商品内容风险提示会复用这些数据。</p>
 */
public interface AiSensitiveWordService
{
    /** 根据词条 ID 查询单条敏感词。 */
    AiSensitiveWord selectAiSensitiveWordById(Long id);

    /** 按条件查询敏感词列表。 */
    List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query);

    /** 新增敏感词，并记录创建管理员。 */
    int insertAiSensitiveWord(AiSensitiveWord word, Long createdBy);

    /** 更新敏感词内容、分类或状态。 */
    int updateAiSensitiveWord(AiSensitiveWord word);

    /** 批量删除敏感词。 */
    int deleteAiSensitiveWordByIds(Long[] ids);
}
