package com.egg.commerce.web.controller.ai.chat;

import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.domain.ai.chat.AiChatRequest;
import com.egg.commerce.system.domain.ai.chat.AiChatResponse;
import com.egg.commerce.system.domain.ai.chat.AiChatSession;
import com.egg.commerce.system.service.ai.chat.AiChatService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     * 物理删除当前登录用户自己的指定会话及其消息、RAG 来源。
     */
    @DeleteMapping("/session/{id}")
    public AjaxResult deleteSession(@PathVariable String id)
    {
        chatService.deleteSession(id, getUserId());
        return success(true);
    }

    /**
     * 向指定会话发送用户问题，并返回本轮 RAG 回答结果。
     */
    @PostMapping("/session/{id}/message")
    public AjaxResult send(@PathVariable String id, @RequestBody AiChatRequest request)
    {
        return success(chatService.sendMessage(id, getUserId(), request == null ? null : request.getContent()));
    }

    /**
     * SSE 发送用户问题。delta 事件返回模型增量文本，done 事件返回完整回答和来源。
     */
    @PostMapping(value = "/session/{id}/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(@PathVariable String id, @RequestBody AiChatRequest request)
    {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = getUserId();
        String content = request == null ? null : request.getContent();
        CompletableFuture.runAsync(() -> {
            try
            {
                AiChatResponse response = chatService.streamMessage(id, userId, content,
                        delta -> sendDelta(emitter, delta));
                emitter.send(SseEmitter.event().name("done").data(response));
                emitter.complete();
            }
            catch (Exception e)
            {
                sendError(emitter, e);
            }
        });
        return emitter;
    }

    private void sendDelta(SseEmitter emitter, String delta)
    {
        try
        {
            emitter.send(SseEmitter.event().name("delta").data(delta));
        }
        catch (IOException e)
        {
            throw new ServiceException("流式输出失败：" + e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, Exception e)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(AjaxResult.error(e.getMessage())));
            emitter.complete();
        }
        catch (IOException ioException)
        {
            emitter.completeWithError(ioException);
        }
    }
}
