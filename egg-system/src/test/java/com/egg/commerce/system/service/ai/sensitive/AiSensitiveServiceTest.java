package com.egg.commerce.system.service.ai.sensitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveWord;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveCategoryMapper;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveWordMapper;
import com.egg.commerce.system.service.ai.sensitive.impl.AiSensitiveCategoryServiceImpl;
import com.egg.commerce.system.service.ai.sensitive.impl.AiSensitiveWordServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 敏感词后台服务测试，覆盖默认状态、必填校验和分类删除保护。
 */
class AiSensitiveServiceTest
{
    /**
     * 验证新增敏感词分类默认启用并保存创建人。
     */
    @Test
    void createCategoryDefaultsStatusToEnabled()
    {
        FakeSensitiveCategoryMapper mapper = new FakeSensitiveCategoryMapper();
        AiSensitiveCategoryServiceImpl service = new AiSensitiveCategoryServiceImpl(mapper);

        AiSensitiveCategory category = new AiSensitiveCategory();
        category.setCategoryName("Sensitive Terms");

        service.insertAiSensitiveCategory(category, 100L);

        assertEquals("enabled", mapper.inserted.getStatus());
        assertEquals(100L, mapper.inserted.getCreatedBy());
    }

    /**
     * 验证新增敏感词条默认启用并保存创建人。
     */
    @Test
    void createWordDefaultsStatusToEnabled()
    {
        FakeSensitiveWordMapper mapper = new FakeSensitiveWordMapper();
        AiSensitiveWordServiceImpl service = new AiSensitiveWordServiceImpl(mapper);

        AiSensitiveWord word = new AiSensitiveWord();
        word.setWord("forbidden");
        word.setCategoryId(9L);

        service.insertAiSensitiveWord(word, 200L);

        assertEquals("enabled", mapper.inserted.getStatus());
        assertEquals(200L, mapper.inserted.getCreatedBy());
    }

    /**
     * 验证新增敏感词条时空词条会被拒绝。
     */
    @Test
    void createWordRejectsBlankWord()
    {
        FakeSensitiveWordMapper mapper = new FakeSensitiveWordMapper();
        AiSensitiveWordServiceImpl service = new AiSensitiveWordServiceImpl(mapper);
        AiSensitiveWord word = new AiSensitiveWord();
        word.setCategoryId(9L);
        word.setWord("   ");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.insertAiSensitiveWord(word, 200L));

        assertEquals("敏感词不能为空", exception.getMessage());
    }

    /**
     * 验证更新敏感词条时空词条会被拒绝。
     */
    @Test
    void updateWordRejectsBlankWord()
    {
        FakeSensitiveWordMapper mapper = new FakeSensitiveWordMapper();
        AiSensitiveWordServiceImpl service = new AiSensitiveWordServiceImpl(mapper);
        AiSensitiveWord word = new AiSensitiveWord();
        word.setCategoryId(9L);
        word.setWord("");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateAiSensitiveWord(word));

        assertEquals("敏感词不能为空", exception.getMessage());
    }

    /**
     * 验证用户提问命中已启用敏感词时只返回布尔结果，不暴露具体词条。
     */
    @Test
    void containsEnabledSensitiveWordDetectsEnabledWords()
    {
        FakeSensitiveWordMapper mapper = new FakeSensitiveWordMapper();
        mapper.enabledWords.add("forbidden");
        AiSensitiveWordServiceImpl service = new AiSensitiveWordServiceImpl(mapper);

        assertTrue(service.containsEnabledSensitiveWord("This question has forbidden content."));
        assertFalse(service.containsEnabledSensitiveWord("This question is clean."));
    }

    /**
     * 验证存在关联词条的分类不能删除。
     */
    @Test
    void deleteCategoryRejectsLinkedWords()
    {
        FakeSensitiveCategoryMapper mapper = new FakeSensitiveCategoryMapper();
        mapper.linkedWordCount = 2;
        AiSensitiveCategoryServiceImpl service = new AiSensitiveCategoryServiceImpl(mapper);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteAiSensitiveCategoryByIds(new Long[] { 1L }));

        assertEquals("敏感词分类下存在词条，不能删除", exception.getMessage());
        assertEquals(0, mapper.deleteCalls);
    }

    private static class FakeSensitiveCategoryMapper implements AiSensitiveCategoryMapper
    {
        private AiSensitiveCategory inserted;

        private int linkedWordCount;

        private int deleteCalls;

        /**
         * 分类测试不需要按 ID 查询，返回空结果。
         */
        @Override
        public AiSensitiveCategory selectAiSensitiveCategoryById(Long id)
        {
            return null;
        }

        /**
         * 分类测试不覆盖列表查询，返回空列表。
         */
        @Override
        public List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query)
        {
            return new ArrayList<>();
        }

        /**
         * 捕获新增分类对象。
         */
        @Override
        public int insertAiSensitiveCategory(AiSensitiveCategory category)
        {
            inserted = category;
            return 1;
        }

        /**
         * 分类测试不覆盖更新行为。
         */
        @Override
        public int updateAiSensitiveCategory(AiSensitiveCategory category)
        {
            return 0;
        }

        /**
         * 返回预置关联词条数，用于删除保护断言。
         */
        @Override
        public int countAiSensitiveWordsByCategoryIds(Long[] ids)
        {
            return linkedWordCount;
        }

        /**
         * 记录删除调用次数。
         */
        @Override
        public int deleteAiSensitiveCategoryByIds(Long[] ids)
        {
            deleteCalls++;
            return 0;
        }
    }

    private static class FakeSensitiveWordMapper implements AiSensitiveWordMapper
    {
        private AiSensitiveWord inserted;

        private final List<String> enabledWords = new ArrayList<>();

        /**
         * 词条测试不需要按 ID 查询，返回空结果。
         */
        @Override
        public AiSensitiveWord selectAiSensitiveWordById(Long id)
        {
            return null;
        }

        /**
         * 词条测试不覆盖列表查询，返回空列表。
         */
        @Override
        public List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query)
        {
            return new ArrayList<>();
        }

        /**
         * 捕获新增词条对象。
         */
        @Override
        public int insertAiSensitiveWord(AiSensitiveWord word)
        {
            inserted = word;
            return 1;
        }

        /**
         * 词条测试不覆盖更新落库行为。
         */
        @Override
        public int updateAiSensitiveWord(AiSensitiveWord word)
        {
            return 0;
        }

        /**
         * 返回预置启用词条，用于敏感内容命中判断测试。
         */
        @Override
        public List<String> selectEnabledSensitiveWords()
        {
            return enabledWords;
        }

        /**
         * 词条测试不覆盖删除行为。
         */
        @Override
        public int deleteAiSensitiveWordByIds(Long[] ids)
        {
            return 0;
        }
    }
}
