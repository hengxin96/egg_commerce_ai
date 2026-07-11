package com.egg.commerce.system.service.ai.sensitive.impl;

import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.sensitive.AiSensitiveCategory;
import com.egg.commerce.system.mapper.ai.sensitive.AiSensitiveCategoryMapper;
import com.egg.commerce.system.service.ai.sensitive.AiSensitiveCategoryService;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 敏感词分类服务实现。
 *
 * <p>分类用于组织不同来源或不同风险类型的敏感词，例如平台规则、广告法风险、品牌禁用词等。
 * 后续审核结果可以借助分类给出更清楚的风险解释。</p>
 */
@Service
public class AiSensitiveCategoryServiceImpl implements AiSensitiveCategoryService
{
    /** 后台新增分类未指定状态时默认启用，便于规则维护后立即生效。 */
    private static final String STATUS_ENABLED = "enabled";

    /** 敏感词分类 Mapper，负责分类表读写以及分类下词条数量查询。 */
    private final AiSensitiveCategoryMapper sensitiveCategoryMapper;

    /**
     * 注入敏感词分类 mapper，服务层负责默认状态和删除保护。
     */
    @Autowired
    public AiSensitiveCategoryServiceImpl(AiSensitiveCategoryMapper sensitiveCategoryMapper)
    {
        this.sensitiveCategoryMapper = sensitiveCategoryMapper;
    }

    /**
     * 按 ID 查询敏感词分类详情。
     */
    @Override
    public AiSensitiveCategory selectAiSensitiveCategoryById(Long id)
    {
        return sensitiveCategoryMapper.selectAiSensitiveCategoryById(id);
    }

    /**
     * 查询后台敏感词分类列表。
     */
    @Override
    public List<AiSensitiveCategory> selectAiSensitiveCategoryList(AiSensitiveCategory query)
    {
        return sensitiveCategoryMapper.selectAiSensitiveCategoryList(query);
    }

    /**
     * 新增敏感词分类，并补齐审计字段和默认启用状态。
     */
    @Override
    public int insertAiSensitiveCategory(AiSensitiveCategory category, Long createdBy)
    {
        Date now = new Date();
        // 后台表单可省略状态，服务层显式补默认值，避免不同数据库默认值行为不一致。
        category.setStatus(StringUtils.defaultIfBlank(category.getStatus(), STATUS_ENABLED));
        category.setCreatedBy(createdBy);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return sensitiveCategoryMapper.insertAiSensitiveCategory(category);
    }

    /**
     * 更新敏感词分类并刷新更新时间。
     */
    @Override
    public int updateAiSensitiveCategory(AiSensitiveCategory category)
    {
        // 分类修改只刷新更新时间；创建人和创建时间保留为首次维护记录。
        category.setUpdatedAt(new Date());
        return sensitiveCategoryMapper.updateAiSensitiveCategory(category);
    }

    /**
     * 删除分类前检查是否仍有词条引用。
     *
     * <p>如果直接依赖数据库外键报错，后台只能看到底层异常；这里提前给出业务化提示。</p>
     */
    @Override
    public int deleteAiSensitiveCategoryByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        // PostgreSQL 外键也会阻止删除，但这里先做业务检查，提示更友好。
        if (sensitiveCategoryMapper.countAiSensitiveWordsByCategoryIds(ids) > 0)
        {
            throw new ServiceException("敏感词分类下存在词条，不能删除");
        }
        return sensitiveCategoryMapper.deleteAiSensitiveCategoryByIds(ids);
    }
}
