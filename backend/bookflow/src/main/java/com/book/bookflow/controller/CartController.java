package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.CartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(cartService.listCartItems());
    }

    @PostMapping("/add")
    public Result<Map<String, Object>> add(@RequestBody Map<String, Object> payload) {
        return Result.success("已加入购物车", cartService.addCartItem(payload));
    }

    @PostMapping("/remove")
    public Result<Map<String, Object>> remove(@RequestBody Map<String, Object> payload) {
        return Result.success("已移除", cartService.removeCartItems(payload));
    }

    @PostMapping("/clear-invalid")
    public Result<Map<String, Object>> clearInvalid() {
        return Result.success("已清理", cartService.clearInvalidItems());
    }
}
