package com.egg.commerce.system.domain.ai.knowledge;

import java.util.List;

/**
 * 知识库文档批量访问角色更新请求。
 */
public class AiKnowledgeAccessRoleUpdateRequest
{
    /** 要更新权限的知识库文档 ID。 */
    private String[] ids;

    /** 前端选择的可访问角色 key，服务层会自动补充 admin。 */
    private List<String> accessRoles;

    public String[] getIds()
    {
        return ids;
    }

    public void setIds(String[] ids)
    {
        this.ids = ids;
    }

    public List<String> getAccessRoles()
    {
        return accessRoles;
    }

    public void setAccessRoles(List<String> accessRoles)
    {
        this.accessRoles = accessRoles;
    }
}
