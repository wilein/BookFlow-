package com.book.bookflow.service;

import java.util.List;
import java.util.Map;

public interface ChatService {
    List<Map<String, Object>> getSessionList();

    Map<String, Object> openSession(Map<String, Object> payload);

    List<Map<String, Object>> getMessageList(Long sessionId);

    Map<String, Object> pollMessages(Long sessionId, Long afterId);

    Map<String, Object> markSessionRead(Long sessionId);

    Map<String, Object> sendMessage(Map<String, Object> payload);
}
