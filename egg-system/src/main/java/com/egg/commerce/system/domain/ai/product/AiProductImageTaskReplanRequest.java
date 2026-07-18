package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** 用户基于已有视觉分析重新生成规划的请求。 */
public class AiProductImageTaskReplanRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户调整后的自然语言需求；为空时复用商品资产中的原始需求。 */
    private String userRequirement;

    public String getUserRequirement() { return userRequirement; }
    public void setUserRequirement(String userRequirement) { this.userRequirement = userRequirement; }
}
