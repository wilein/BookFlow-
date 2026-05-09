package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.common.pay.WechatPayClient;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.Notification;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserAddress;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.CartItemMapper;
import com.book.bookflow.mapper.ContentReportMapper;
import com.book.bookflow.mapper.NotificationMapper;
import com.book.bookflow.mapper.OrderIssueMapper;
import com.book.bookflow.mapper.UserAddressMapper;
import com.book.bookflow.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private final BookOrderMapper bookOrderMapper = mock(BookOrderMapper.class);
    private final BookMapper bookMapper = mock(BookMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserAddressMapper userAddressMapper = mock(UserAddressMapper.class);
    private final CartItemMapper cartItemMapper = mock(CartItemMapper.class);
    private final OrderIssueMapper orderIssueMapper = mock(OrderIssueMapper.class);
    private final ContentReportMapper contentReportMapper = mock(ContentReportMapper.class);
    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final WechatPayClient wechatPayClient = mock(WechatPayClient.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
        bookOrderMapper,
        bookMapper,
        userMapper,
        userAddressMapper,
        cartItemMapper,
        orderIssueMapper,
        contentReportMapper,
        notificationMapper,
        wechatPayClient
    );

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void checkoutCreatesOneOrderPerBook() {
        AuthContext.setUserId(1L);
        when(userAddressMapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(UserAddress.builder()
            .id(8L)
            .userId(1L)
            .receiverName("buyer")
            .receiverPhone("13800000000")
            .province("A")
            .city("B")
            .district("C")
            .detailAddress("D")
            .isDeleted(0)
            .build());
        when(bookMapper.selectOneById(10L)).thenReturn(book(10L, 2L, "Book A"));
        when(bookMapper.selectOneById(11L)).thenReturn(book(11L, 3L, "Book B"));
        when(userMapper.selectOneById(anyLong())).thenAnswer(invocation -> User.builder()
            .id(invocation.getArgument(0))
            .nickname("user-" + invocation.getArgument(0))
            .build());
        when(orderIssueMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of());
        AtomicLong orderId = new AtomicLong(100L);
        doAnswer(invocation -> {
            BookOrder order = invocation.getArgument(0);
            order.setId(orderId.getAndIncrement());
            return 1;
        }).when(bookOrderMapper).insert(any(BookOrder.class));

        Map<String, Object> result = service.checkout(Map.of(
            "addressId", 8L,
            "buyerMessage", "please",
            "items", List.of(Map.of("bookId", 10L), Map.of("bookId", 11L))
        ));

        assertEquals(List.of(100L, 101L), result.get("orderIds"));
        assertEquals(2, ((List<?>) result.get("orders")).size());
        verify(bookOrderMapper, times(2)).insert(any(BookOrder.class));
        verify(bookMapper, times(2)).update(any(Book.class));
        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    private Book book(Long id, Long sellerId, String title) {
        return Book.builder()
            .id(id)
            .userId(sellerId)
            .title(title)
            .price(new BigDecimal("9.90"))
            .status(1)
            .isDeleted(0)
            .build();
    }
}
