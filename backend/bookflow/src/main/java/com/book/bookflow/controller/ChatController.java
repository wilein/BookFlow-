package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/session/list")
    public Result<List<Map<String, Object>>> sessionList() {
        return Result.success(chatService.getSessionList());
    }

    @PostMapping("/session/open")
    public Result<Map<String, Object>> openSession(@RequestBody Map<String, Object> payload) {
        return Result.success(chatService.openSession(payload));
    }

    @GetMapping("/message/list")
    public Result<List<Map<String, Object>>> messageList(@RequestParam Long sessionId) {
        return Result.success(chatService.getMessageList(sessionId));
    }

    @GetMapping("/message/poll")
    public Result<Map<String, Object>> pollMessages(@RequestParam Long sessionId,
                                                    @RequestParam(defaultValue = "0") Long afterId) {
        return Result.success(chatService.pollMessages(sessionId, afterId));
    }

    @PostMapping("/message/read")
    public Result<Map<String, Object>> readMessages(@RequestBody Map<String, Object> payload) {
        return Result.success("已读", chatService.markSessionRead(asLong(payload.get("sessionId"))));
    }

    @PostMapping("/message/send")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> payload) {
        return Result.success("发送成功", chatService.sendMessage(payload));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
