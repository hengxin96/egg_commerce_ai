package com.egg.commerce.system.service.ai.prompt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;

/**
 * 提示词模板服务测试，覆盖默认字段填充和模板内容校验。
 */
class AiPromptTemplateServiceImplTest
{
    /**
     * 验证新增模板会补齐默认变量、状态和版本。
     */
    @Test
    void insertPromptTemplateDefaultsVariablesStatusAndVersion()
    {
        CapturingMapper mapper = new CapturingMapper();
        AiPromptTemplateServiceImpl service = new AiPromptTemplateServiceImpl(mapper);
        AiPromptTemplate template = new AiPromptTemplate();
        template.setName("商品主图提示词");
        template.setContent("{\"format\":\"markdown\",\"body\":\"# 角色\\n你好，{name}\"}");
        template.setVariables("");

        service.insertAiPromptTemplate(template);

        assertEquals("draft", mapper.inserted.getStatus());
        assertEquals(Integer.valueOf(1), mapper.inserted.getVersion());
        assertEquals("[]", mapper.inserted.getVariables());
    }

    /**
     * 验证缺少 Markdown 正文的提示词模板会被拒绝。
     */
    @Test
    void insertPromptTemplateRejectsContentWithoutMarkdownBody()
    {
        AiPromptTemplateServiceImpl service = new AiPromptTemplateServiceImpl(new CapturingMapper());
        AiPromptTemplate template = new AiPromptTemplate();
        template.setName("缺少正文");
        template.setContent("{\"format\":\"markdown\"}");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.insertAiPromptTemplate(template));

        assertEquals("提示词正文不能为空", exception.getMessage());
    }

    /**
     * 验证未知模板状态不会被保存。
     */
    @Test
    void updatePromptTemplateRejectsUnknownStatus()
    {
        AiPromptTemplateServiceImpl service = new AiPromptTemplateServiceImpl(new CapturingMapper());
        AiPromptTemplate template = new AiPromptTemplate();
        template.setId("2f03b364-6d27-4a45-97ec-0c55185fc001");
        template.setName("状态错误");
        template.setStatus("enabled");
        template.setContent("{\"format\":\"markdown\",\"body\":\"正文\"}");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateAiPromptTemplate(template));

        assertEquals("提示词模板状态不正确", exception.getMessage());
    }

    private static class CapturingMapper implements AiPromptTemplateMapper
    {
        private AiPromptTemplate inserted;

        /**
         * 测试不需要按 ID 查询模板，返回空结果。
         */
        @Override
        public AiPromptTemplate selectAiPromptTemplateById(String id)
        {
            return null;
        }

        /**
         * 测试不覆盖列表查询，返回空列表。
         */
        @Override
        public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
        {
            return List.of();
        }

        /**
         * 提示词模板服务测试不涉及剧情运行时模板定位，返回空结果即可。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateForStoryStage(String category, String platformPrefix,
                List<String> fallbackCategories)
        {
            return null;
        }

        /**
         * 提示词模板服务测试不涉及通用运行时模板定位，返回空结果即可。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateByCategory(String category)
        {
            return null;
        }

        /**
         * 提示词模板服务测试不涉及商品图运行时模板定位，返回空结果即可。
         */
        @Override
        public AiPromptTemplate selectPublishedTemplateByNameAndCategory(String name, String category)
        {
            return null;
        }

        /**
         * 捕获服务层准备保存的新增模板。
         */
        @Override
        public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            this.inserted = promptTemplate;
            return 1;
        }

        /**
         * 更新测试只关心前置校验，返回成功。
         */
        @Override
        public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 1;
        }

        /**
         * 测试不覆盖单条删除，返回成功。
         */
        @Override
        public int deleteAiPromptTemplateById(String id)
        {
            return 1;
        }

        /**
         * 返回传入 ID 数量作为批量删除数量。
         */
        @Override
        public int deleteAiPromptTemplateByIds(String[] ids)
        {
            return ids.length;
        }
    }
}
