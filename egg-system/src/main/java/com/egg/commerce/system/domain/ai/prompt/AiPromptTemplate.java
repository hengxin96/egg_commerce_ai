package com.egg.commerce.system.domain.ai.prompt;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.egg.commerce.common.core.domain.BaseEntity;

/**
 * AI 提示词模板实体，对应 ai_prompt_templates 表。
 *
 * <p>模板用于集中管理可复用提示词，后续图片生成、RAG 问答或 agent 编排可以按分类、状态和版本
 * 选择模板。正文和变量以 JSON 字符串传输，方便 PostgreSQL JSONB 保存结构化内容。</p>
 */
public class AiPromptTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板主键，使用字符串 ID 便于与前端和 SQL 脚本保持一致。 */
    private String id;

    /** 模板名称，后台列表和选择器展示的主要标题。 */
    private String name;

    /** 模板说明，记录使用场景、适用模型或维护注意事项。 */
    private String description;

    /** 模板分类，例如 image-generation、rag-chat、audit，用于后台筛选和后续调用选择。 */
    private String category;

    /** JSON 字符串，保存为 PostgreSQL JSONB，当前约定结构为 { format, body }。 */
    private String content;

    /** 可选 JSON 字符串，描述手工维护的占位变量，例如变量名、默认值和说明。 */
    private String variables;

    /** 模板版本号，新增时默认从 1 开始，便于后续追踪提示词迭代。 */
    private Integer version;

    /** 模板状态，当前支持 draft、published、archived。 */
    private String status;

    /** 创建模板的后台管理员用户 ID。 */
    private Long createdBy;

    /** 模板创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 模板最近更新时间。 */
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

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 255, message = "模板名称不能超过255个字符")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Size(max = 100, message = "模板分类不能超过100个字符")
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    @NotBlank(message = "提示词内容不能为空")
    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getVariables()
    {
        return variables;
    }

    public void setVariables(String variables)
    {
        this.variables = variables;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
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
