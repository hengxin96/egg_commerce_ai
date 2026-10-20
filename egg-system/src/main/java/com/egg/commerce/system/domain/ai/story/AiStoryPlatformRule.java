package com.egg.commerce.system.domain.ai.story;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 剧情生成平台画像和规则实体，对应 ai_story_platform_rules 表。
 *
 * <p>该配置由后台维护或 SQL 初始化，普通用户只选择平台。剧情生成时后端把已发布规则注入
 * `{platformProfile}` 和 `{categoryRules}`，用于约束模型符合平台内容生态和带货合规要求。</p>
 */
public class AiStoryPlatformRule implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 平台规则主键。 */
    private String id;

    /** 平台名称，需与 story_platform 字典值保持一致。 */
    private String platform;

    /** 规则版本，用于追踪任务当时使用的平台规则快照。 */
    private String version;

    /** 规则状态：draft、published、archived。 */
    private String status;

    /** 平台用户画像和内容倾向，用于替换 {platformProfile}。 */
    private String platformProfile;

    /** 平台内容规则、带货植入规则和风险底线，用于替换 {categoryRules}。 */
    private String categoryRules;

    /** 规则来源和维护说明。 */
    private String sourceSummary;

    /** 创建人 ID，首版 SQL 初始化时可为空或为管理员 ID。 */
    private Long createdBy;

    /** 规则创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 规则最近更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getPlatform()
    {
        return platform;
    }

    public void setPlatform(String platform)
    {
        this.platform = platform;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPlatformProfile()
    {
        return platformProfile;
    }

    public void setPlatformProfile(String platformProfile)
    {
        this.platformProfile = platformProfile;
    }

    public String getCategoryRules()
    {
        return categoryRules;
    }

    public void setCategoryRules(String categoryRules)
    {
        this.categoryRules = categoryRules;
    }

    public String getSourceSummary()
    {
        return sourceSummary;
    }

    public void setSourceSummary(String sourceSummary)
    {
        this.sourceSummary = sourceSummary;
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
