package com.egg.commerce.system.mapper.ai.prompt;

import java.util.List;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import org.apache.ibatis.annotations.Param;

/**
 * 提示词模板数据访问接口，对应 ai_prompt_templates 表。
 */
public interface AiPromptTemplateMapper
{
    /** 根据模板 ID 查询单条模板。 */
    AiPromptTemplate selectAiPromptTemplateById(String id);

    /** 按条件查询模板列表。 */
    List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate);

    /** 按模板名称和分类查询最新已发布模板，供商品图主图、详情图和 AI 帮写运行时使用。 */
    AiPromptTemplate selectPublishedTemplateByNameAndCategory(@Param("name") String name,
            @Param("category") String category);

    /** 新增提示词模板。 */
    int insertAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 更新提示词模板。 */
    int updateAiPromptTemplate(AiPromptTemplate promptTemplate);

    /** 根据模板 ID 删除单条模板。 */
    int deleteAiPromptTemplateById(String id);

    /** 根据多个模板 ID 批量删除模板。 */
    int deleteAiPromptTemplateByIds(String[] ids);
}
