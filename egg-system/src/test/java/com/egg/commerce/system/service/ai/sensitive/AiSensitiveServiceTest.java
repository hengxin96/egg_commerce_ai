package com.egg.commerce.system.service.ai.sensitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class AiSensitiveServiceTest
{
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

        assertEquals("Sensitive word cannot be blank", exception.getMessage());
    }

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

        assertEquals("Sensitive word cannot be blank", exception.getMessage());
    }

    @Test
    void deleteCategoryRejectsLinkedWords()
    {
        FakeSensitiveCategoryMapper mapper = new FakeSensitiveCategoryMapper();
        mapper.linkedWordCount = 2;
        AiSensitiveCategoryServiceImpl service = new AiSensitiveCategoryServiceImpl(mapper);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteAiSensitiveCategoryByIds(new Long[] { 1L }));

        assertEquals("Sensitive category contains words and cannot be deleted", exception.getMessage());
        assertEquals(0, mapper.deleteCalls);
    }

    private static class FakeSensitiveCategoryMapper implements AiSensitiveCategoryMapper
    {
        private AiSensitiveCategory inserted;

        private int linkedWordCount;

        private int deleteCalls;

        @Override
        public AiSensitiveCategory selectAiSensitiveCategoryById(Long id)
        {
            return null;
        }

        @Override
        public List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query)
        {
            return new ArrayList<>();
        }

        @Override
        public int insertAiSensitiveCategory(AiSensitiveCategory category)
        {
            inserted = category;
            return 1;
        }

        @Override
        public int updateAiSensitiveCategory(AiSensitiveCategory category)
        {
            return 0;
        }

        @Override
        public int countAiSensitiveWordsByCategoryIds(Long[] ids)
        {
            return linkedWordCount;
        }

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

        @Override
        public AiSensitiveWord selectAiSensitiveWordById(Long id)
        {
            return null;
        }

        @Override
        public List<AiSensitiveWord> selectAiSensitiveWordList(AiSensitiveWord query)
        {
            return new ArrayList<>();
        }

        @Override
        public int insertAiSensitiveWord(AiSensitiveWord word)
        {
            inserted = word;
            return 1;
        }

        @Override
        public int updateAiSensitiveWord(AiSensitiveWord word)
        {
            return 0;
        }

        @Override
        public int deleteAiSensitiveWordByIds(Long[] ids)
        {
            return 0;
        }
    }
}
