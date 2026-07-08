package com.egg.commerce.web.controller.ai.chat;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.system.domain.ai.chat.AiChatRequest;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.service.ai.chat.IAiChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/chat")
public class AiChatController extends BaseController
{
    private final IAiChatService chatService;

    public AiChatController(IAiChatService chatService)
    {
        this.chatService = chatService;
    }

    @GetMapping("/session/list")
    public AjaxResult sessions()
    {
        return success(chatService.selectSessionList(getUserId()));
    }

    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody AiChatSession session)
    {
        return success(chatService.createSession(getUserId(), session == null ? null : session.getTitle()));
    }

    @GetMapping("/session/{id}/messages")
    public AjaxResult messages(@PathVariable String id)
    {
        return success(chatService.selectMessages(id, getUserId()));
    }

    @PostMapping("/session/{id}/message")
    public AjaxResult send(@PathVariable String id, @RequestBody AiChatRequest request)
    {
        return success(chatService.sendMessage(id, getUserId(), request == null ? null : request.getContent()));
    }
}
