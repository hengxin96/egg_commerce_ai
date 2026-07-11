package com.egg.commerce.system.domain.ai.sensitive;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * AI 敏感词条实体。
 *
 * <p>词条是后续文案审核、商品标题审核和生成内容风险提示的基础规则数据。每个词条可归属一个分类，
 * 便于后台筛选和面向用户解释触发原因。</p>
 */
public class AiSensitiveWord
{
    /** 词条主键。 */
    private Long id;

    /** 敏感词内容，例如违禁词、夸大宣传词或平台限制词。 */
    private String word;

    /** 所属敏感词分类 ID。 */
    private Long categoryId;

    /** 所属分类名称，通常由 Mapper 联查后用于后台列表展示。 */
    private String categoryName;

    /** 词条状态，例如 enabled、disabled，用于控制是否参与后续审核。 */
    private String status;

    /** 创建词条的后台管理员用户 ID。 */
    private Long createdBy;

    /** 词条创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 词条最近更新时间。 */
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

    public String getWord()
    {
        return word;
    }

    public void setWord(String word)
    {
        this.word = word;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
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
