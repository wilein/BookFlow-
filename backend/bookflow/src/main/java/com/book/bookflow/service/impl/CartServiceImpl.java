package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.CartItem;
import com.book.bookflow.entity.User;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.CartItemMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.service.CartService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CartServiceImpl implements CartService {

    private static final String DEFAULT_AVATAR = "/static/logo.png";
    private static final String DEFAULT_NICKNAME = "书友";

    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    public CartServiceImpl(CartItemMapper cartItemMapper, BookMapper bookMapper, UserMapper userMapper) {
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> listCartItems() {
        Long currentUserId = AuthContext.requireUserId();
        List<CartItem> items = cartItemMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("id desc")
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (CartItem item : items) {
            result.add(toCartItem(item, currentUserId));
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> addCartItem(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long bookId = asLong(payload.get("bookId"));
        Book book = requireBook(bookId);
        if (Objects.equals(book.getUserId(), currentUserId)) {
            throw new CustomerException("400", "不能购买自己发布的书籍");
        }
        CartItem existing = cartItemMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("book_id = ?", bookId)
                .and("is_deleted = 0")
                .limit(1)
        );
        if (existing != null) {
            return toCartItem(existing, currentUserId);
        }
        LocalDateTime now = LocalDateTime.now();
        CartItem item = CartItem.builder()
            .userId(currentUserId)
            .bookId(bookId)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        cartItemMapper.insert(item);
        return toCartItem(item, currentUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> removeCartItems(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        List<Long> ids = asLongList(payload.get("cartItemIds"));
        if (ids.isEmpty()) {
            Long id = asLong(payload.get("cartItemId"));
            if (id != null) {
                ids = List.of(id);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        for (Long id : ids) {
            CartItem item = cartItemMapper.selectOneById(id);
            if (item != null && Objects.equals(item.getUserId(), currentUserId) && !isDeleted(item.getIsDeleted())) {
                item.setIsDeleted(1);
                item.setUpdateTime(now);
                cartItemMapper.update(item);
                removed++;
            }
        }
        return Map.of("removed", removed);
    }

    @Override
    @Transactional
    public Map<String, Object> clearInvalidItems() {
        Long currentUserId = AuthContext.requireUserId();
        List<CartItem> items = cartItemMapper.selectListByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0")
        );
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        for (CartItem item : items) {
            Book book = item.getBookId() == null ? null : bookMapper.selectOneById(item.getBookId());
            if (book == null || isDeleted(book.getIsDeleted()) || !Objects.equals(book.getStatus(), 1)) {
                item.setIsDeleted(1);
                item.setUpdateTime(now);
                cartItemMapper.update(item);
                removed++;
            }
        }
        return Map.of("removed", removed);
    }

    private Map<String, Object> toCartItem(CartItem item, Long currentUserId) {
        Book book = item.getBookId() == null ? null : bookMapper.selectOneById(item.getBookId());
        User seller = book == null || book.getUserId() == null ? null : userMapper.selectOneById(book.getUserId());
        boolean available = book != null && !isDeleted(book.getIsDeleted()) && Objects.equals(book.getStatus(), 1)
            && !Objects.equals(book.getUserId(), currentUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("cartItemId", item.getId());
        result.put("bookId", item.getBookId());
        result.put("bookTitle", book == null ? "书籍已下架" : defaultString(book.getTitle(), "未命名书籍"));
        result.put("bookCover", book == null ? DEFAULT_AVATAR : resolveBookCover(book));
        result.put("price", book == null || book.getPrice() == null ? BigDecimal.ZERO : book.getPrice());
        result.put("sellerId", book == null ? null : book.getUserId());
        result.put("sellerName", seller == null ? DEFAULT_NICKNAME : defaultString(seller.getNickname(), DEFAULT_NICKNAME));
        result.put("available", available);
        result.put("invalidReason", available ? "" : "书籍不可购买");
        result.put("createTime", item.getCreateTime());
        return result;
    }

    private Book requireBook(Long bookId) {
        if (bookId == null) {
            throw new CustomerException("400", "书籍参数错误");
        }
        Book book = bookMapper.selectOneById(bookId);
        if (book == null || isDeleted(book.getIsDeleted())) {
            throw new CustomerException("404", "书籍不存在");
        }
        if (!Objects.equals(book.getStatus(), 1)) {
            throw new CustomerException("400", "该书暂不可购买");
        }
        return book;
    }

    private List<Long> asLongList(Object value) {
        if (value instanceof List<?> list) {
            List<Long> result = new ArrayList<>();
            for (Object item : list) {
                Long id = asLong(item);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }
        return new ArrayList<>();
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

    private String resolveBookCover(Book book) {
        String images = defaultString(book.getCoverImages(), "");
        if (!images.isBlank()) {
            String[] parts = images.replace("[", "").replace("]", "").replace("\"", "").split(",");
            for (String part : parts) {
                String item = part.trim();
                if (!item.isBlank()) {
                    return item;
                }
            }
        }
        return DEFAULT_AVATAR;
    }

    private boolean isDeleted(Integer isDeleted) {
        return isDeleted != null && isDeleted == 1;
    }

    private String defaultString(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
