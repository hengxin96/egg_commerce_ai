package com.egg.commerce.web.controller.ai.chat;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.system.domain.ai.chat.AiChatRequest;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.service.ai.chat.AiChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能聊天后台接口，负责会话、消息和提问入口的 HTTP 适配。
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController
{
    private final AiChatService chatService;

    /**
     * 注入聊天业务服务，控制器只负责登录用户上下文和 HTTP 入参转换。
     */
    public AiChatController(AiChatService chatService)
    {
        this.chatService = chatService;
    }

    /**
     * 查询当前登录用户的聊天会话列表。
     */
    @GetMapping("/session/list")
    public AjaxResult sessions()
    {
        return success(chatService.selectSessionList(getUserId()));
    }

    /**
     * 为当前登录用户创建新的智能聊天会话。
     */
    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody AiChatSession session)
    {
        return success(chatService.createSession(getUserId(), session == null ? null : session.getTitle()));
    }

    /**
     * 查询指定会话的历史消息，并由服务层校验会话归属。
     */
    @GetMapping("/session/{id}/messages")
    public AjaxResult messages(@PathVariable String id)
    {
        return success(chatService.selectMessages(id, getUserId()));
    }

    /**
     * 向指定会话发送用户问题，并返回本轮 RAG 回答结果。
     */
    @PostMapping("/session/{id}/message")
    public AjaxResult send(@PathVariable String id, @RequestBody AiChatRequest request)
    {
        return success(chatService.sendMessage(id, getUserId(), request == null ? null : request.getContent()));
    }
}
