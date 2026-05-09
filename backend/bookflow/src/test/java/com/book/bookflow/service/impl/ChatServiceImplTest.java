package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.ChatMessage;
import com.book.bookflow.entity.ChatSession;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.ChatMessageMapper;
import com.book.bookflow.mapper.ChatSessionMapper;
import com.book.bookflow.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private final ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final BookMapper bookMapper = mock(BookMapper.class);
    private final BookOrderMapper bookOrderMapper = mock(BookOrderMapper.class);
    private final ChatServiceImpl service = new ChatServiceImpl(
        chatSessionMapper,
        chatMessageMapper,
        userMapper,
        bookMapper,
        bookOrderMapper
    );

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void markSessionReadOnlyClearsPeerMessages() {
        AuthContext.setUserId(1L);
        ChatSession session = ChatSession.builder()
            .id(7L)
            .buyerId(1L)
            .sellerId(2L)
            .unreadCount(2)
            .isDeleted(0)
            .build();
        when(chatSessionMapper.selectOneById(7L)).thenReturn(session);
        when(chatMessageMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(
            ChatMessage.builder().id(20L).sessionId(7L).senderId(2L).isRead(0).isDeleted(0).build(),
            ChatMessage.builder().id(21L).sessionId(7L).senderId(2L).isRead(0).isDeleted(0).build()
        ));
        when(chatMessageMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

        Map<String, Object> result = service.markSessionRead(7L);

        assertEquals(2, result.get("readCount"));
        assertEquals(0L, result.get("unreadCount"));
        assertEquals(0, session.getUnreadCount());
        verify(chatMessageMapper, times(2)).update(any(ChatMessage.class));
        verify(chatSessionMapper).update(session);
    }
}
