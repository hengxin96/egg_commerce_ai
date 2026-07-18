package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** 用户确认规划并进入生成阶段的请求。 */
public class AiProductImageTaskConfirmRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 前台可传回编辑后的规划 JSON；为空时使用任务当前规划。 */
    private String planningOutput;

    public String getPlanningOutput() { return planningOutput; }
    public void setPlanningOutput(String planningOutput) { this.planningOutput = planningOutput; }
}
