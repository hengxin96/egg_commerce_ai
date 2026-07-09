package com.egg.commerce.system.service.ai.prompt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;

class AiPromptTemplateServiceImplTest
{
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

        @Override
        public AiPromptTemplate selectAiPromptTemplateById(String id)
        {
            return null;
        }

        @Override
        public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate promptTemplate)
        {
            return List.of();
        }

        @Override
        public int insertAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            this.inserted = promptTemplate;
            return 1;
        }

        @Override
        public int updateAiPromptTemplate(AiPromptTemplate promptTemplate)
        {
            return 1;
        }

        @Override
        public int deleteAiPromptTemplateById(String id)
        {
            return 1;
        }

        @Override
        public int deleteAiPromptTemplateByIds(String[] ids)
        {
            return ids.length;
        }
    }
}
