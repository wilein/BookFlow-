package com.book.bookflow.controller;

import com.book.bookflow.common.Result;

import com.book.bookflow.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/common")
public class CommonControoler {
    @Autowired
    private BannerService bannerService;
    @GetMapping("/banner/list")
    public Result<List<Map<String, String>>> bannerlist() {
        List<Map<String, String>> banners = bannerService.getActiveBanners();
        return Result.success(banners);
    }
}
