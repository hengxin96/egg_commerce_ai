package com.egg.commerce.system.domain.ai.sensitive;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * AI 敏感词分类实体。
 *
 * <p>分类用于组织不同风险来源的敏感词，例如平台规则、广告法、品牌限制或行业禁用词。
 * 后续审核结果可以通过分类向用户解释风险来源。</p>
 */
public class AiSensitiveCategory
{
    /** 分类主键。 */
    private Long id;

    /** 分类名称，后台维护和风险解释中展示。 */
    private String categoryName;

    /** 分类状态，例如 enabled、disabled，用于控制该分类规则是否参与后续审核。 */
    private String status;

    /** 创建分类的后台管理员用户 ID。 */
    private Long createdBy;

    /** 分类创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 分类最近更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy)
    {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
