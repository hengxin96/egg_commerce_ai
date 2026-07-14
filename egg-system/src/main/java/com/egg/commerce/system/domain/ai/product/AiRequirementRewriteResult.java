package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** AI 帮写商品图要求响应。 */
public class AiRequirementRewriteResult implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String optimizedRequirement;
    private String displayTitle;
    private Boolean imagePromptReady;

    public String getOptimizedRequirement() { return optimizedRequirement; }
    public void setOptimizedRequirement(String optimizedRequirement) { this.optimizedRequirement = optimizedRequirement; }
    public String getDisplayTitle() { return displayTitle; }
    public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle; }
    public Boolean getImagePromptReady() { return imagePromptReady; }
    public void setImagePromptReady(Boolean imagePromptReady) { this.imagePromptReady = imagePromptReady; }
}
