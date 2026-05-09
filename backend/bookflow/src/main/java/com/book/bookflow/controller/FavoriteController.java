package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.FavoriteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@RequestBody Map<String, Object> payload) {
        return Result.success(favoriteService.toggleFavorite(payload));
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam String targetType, @RequestParam Long targetId) {
        return Result.success(favoriteService.getFavoriteStatus(targetType, targetId));
    }
}
