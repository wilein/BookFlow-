package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.CartItem;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.CartItemMapper;
import com.book.bookflow.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceImplTest {

    private final CartItemMapper cartItemMapper = mock(CartItemMapper.class);
    private final BookMapper bookMapper = mock(BookMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final CartServiceImpl service = new CartServiceImpl(cartItemMapper, bookMapper, userMapper);

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void addCartItemReturnsExistingItemWhenDuplicate() {
        AuthContext.setUserId(1L);
        Book book = Book.builder()
            .id(10L)
            .userId(2L)
            .title("Java")
            .price(new BigDecimal("12.30"))
            .status(1)
            .isDeleted(0)
            .build();
        CartItem existing = CartItem.builder()
            .id(99L)
            .userId(1L)
            .bookId(10L)
            .isDeleted(0)
            .build();
        when(bookMapper.selectOneById(10L)).thenReturn(book);
        when(cartItemMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(existing);

        Map<String, Object> result = service.addCartItem(Map.of("bookId", 10L));

        assertEquals(99L, result.get("id"));
        assertEquals(10L, result.get("bookId"));
        verify(cartItemMapper, never()).insert(any(CartItem.class));
    }

    @Test
    void addCartItemRejectsOwnBook() {
        AuthContext.setUserId(1L);
        when(bookMapper.selectOneById(10L)).thenReturn(Book.builder()
            .id(10L)
            .userId(1L)
            .status(1)
            .isDeleted(0)
            .build());

        assertThrows(CustomerException.class, () -> service.addCartItem(Map.of("bookId", 10L)));
    }
}
