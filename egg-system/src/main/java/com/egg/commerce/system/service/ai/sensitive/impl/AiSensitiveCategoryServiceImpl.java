package com.egg.commerce.system.service.ai.sensitive.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveCategoryMapper;
import com.egg.commerce.system.service.ai.sensitive.IAiSensitiveCategoryService;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class AiSensitiveCategoryServiceImpl implements IAiSensitiveCategoryService
{
    private static final String STATUS_ENABLED = "enabled";

    private final AiSensitiveCategoryMapper sensitiveCategoryMapper;

    public AiSensitiveCategoryServiceImpl(AiSensitiveCategoryMapper sensitiveCategoryMapper)
    {
        this.sensitiveCategoryMapper = sensitiveCategoryMapper;
    }

    @Override
    public AiSensitiveCategory selectAiSensitiveCategoryById(Long id)
    {
        return sensitiveCategoryMapper.selectAiSensitiveCategoryById(id);
    }

    @Override
    public List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query)
    {
        return sensitiveCategoryMapper.selectAiSensitiveCategoryList(query);
    }

    @Override
    public int insertAiSensitiveCategory(AiSensitiveCategory category, Long createdBy)
    {
        Date now = new Date();
        // Admin category maintenance can omit status, so the backend applies the SQL default explicitly.
        category.setStatus(StringUtils.defaultIfBlank(category.getStatus(), STATUS_ENABLED));
        category.setCreatedBy(createdBy);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return sensitiveCategoryMapper.insertAiSensitiveCategory(category);
    }

    @Override
    public int updateAiSensitiveCategory(AiSensitiveCategory category)
    {
        category.setUpdatedAt(new Date());
        return sensitiveCategoryMapper.updateAiSensitiveCategory(category);
    }

    @Override
    public int deleteAiSensitiveCategoryByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        // Keep the admin CRUD flow friendly: PostgreSQL would reject linked rows, but with a low-level FK error.
        if (sensitiveCategoryMapper.countAiSensitiveWordsByCategoryIds(ids) > 0)
        {
            throw new ServiceException("Sensitive category contains words and cannot be deleted");
        }
        return sensitiveCategoryMapper.deleteAiSensitiveCategoryByIds(ids);
    }
}
