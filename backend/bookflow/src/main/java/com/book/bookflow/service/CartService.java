package com.book.bookflow.service;

import java.util.List;
import java.util.Map;

public interface CartService {
    List<Map<String, Object>> listCartItems();

    Map<String, Object> addCartItem(Map<String, Object> payload);

    Map<String, Object> removeCartItems(Map<String, Object> payload);

    Map<String, Object> clearInvalidItems();
}
