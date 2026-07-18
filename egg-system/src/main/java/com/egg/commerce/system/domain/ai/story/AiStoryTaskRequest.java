package com.egg.commerce.system.domain.ai.story;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;

/**
 * 剧情生成任务创建请求。
 *
 * <p>该对象只承载前台用户可输入或可选择的参数，平台画像、平台规则、模型名称等内部参数由后端补齐，
 * 避免把内部提示词规则暴露给普通用户。</p>
 */
public class AiStoryTaskRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 剧情主题，来自前台用户的核心创作输入。 */
    private String storyTheme;

    /** 产品信息，模型生成剧情植入和视觉提示词时必须以此为事实来源。 */
    private String productInfo;

    /** 目标平台，来自 story_platform 字典值，并用于匹配平台专属剧情提示词。 */
    private String targetPlatform;

    /** 目标人群，来自 story_target_audience 字典值。 */
    private String targetAudience;

    /** 推荐风格，来自 story_style 字典值。 */
    private String storyStyle;

    /** 剧情时长，来自 story_duration 字典值。 */
    private String storyDuration;

    @NotBlank(message = "剧情主题不能为空")
    public String getStoryTheme()
    {
        return storyTheme;
    }

    public void setStoryTheme(String storyTheme)
    {
        this.storyTheme = storyTheme;
    }

    @NotBlank(message = "产品信息不能为空")
    public String getProductInfo()
    {
        return productInfo;
    }

    public void setProductInfo(String productInfo)
    {
        this.productInfo = productInfo;
    }

    @NotBlank(message = "目标平台不能为空")
    public String getTargetPlatform()
    {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform)
    {
        this.targetPlatform = targetPlatform;
    }

    @NotBlank(message = "目标人群不能为空")
    public String getTargetAudience()
    {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience)
    {
        this.targetAudience = targetAudience;
    }

    @NotBlank(message = "推荐风格不能为空")
    public String getStoryStyle()
    {
        return storyStyle;
    }

    public void setStoryStyle(String storyStyle)
    {
        this.storyStyle = storyStyle;
    }

    @NotBlank(message = "剧情时长不能为空")
    public String getStoryDuration()
    {
        return storyDuration;
    }

    public void setStoryDuration(String storyDuration)
    {
        this.storyDuration = storyDuration;
    }
}
