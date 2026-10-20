package com.egg.commerce.system.mapper.ai.story;

import com.egg.commerce.system.domain.ai.story.AiStoryPlatformRule;

/**
 * 剧情生成平台画像和规则数据访问接口，对应 ai_story_platform_rules 表。
 */
public interface AiStoryPlatformRuleMapper
{
    /** 查询指定平台当前发布中的规则配置，用于替换平台画像和平台规则占位符。 */
    AiStoryPlatformRule selectPublishedRuleByPlatform(String platform);
}
