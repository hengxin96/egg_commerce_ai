package com.egg.commerce.system.mapper.ai.sensitive;

import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import java.util.List;

/**
 * 敏感词条数据访问接口。
 */
public interface AiSensitiveWordMapper
{
    /** 根据词条 ID 查询单条敏感词。 */
    AiSensitiveWord selectAiSensitiveWordById(Long id);

    /** 按条件查询敏感词列表，通常会联查分类名称用于后台展示。 */
    List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query);

    /** 查询所有启用的敏感词内容，用于用户提问入模前拦截。 */
    List<String> selectEnabledSensitiveWords();

    /** 新增敏感词条。 */
    int insertAiSensitiveWord(AiSensitiveWord word);

    /** 更新敏感词条。 */
    int updateAiSensitiveWord(AiSensitiveWord word);

    /** 批量删除敏感词条。 */
    int deleteAiSensitiveWordByIds(Long[] ids);
}
