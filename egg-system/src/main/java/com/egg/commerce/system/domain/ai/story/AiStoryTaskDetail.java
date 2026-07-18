package com.egg.commerce.system.domain.ai.story;

/**
 * 剧情任务详情响应对象。
 *
 * <p>当前详情字段与任务实体一致，单独保留响应类型是为了后续增加节点展示状态、
 * 下载权限或前端专用聚合字段时不污染数据库实体。</p>
 */
public class AiStoryTaskDetail extends AiStoryTask
{
    private static final long serialVersionUID = 1L;
}
