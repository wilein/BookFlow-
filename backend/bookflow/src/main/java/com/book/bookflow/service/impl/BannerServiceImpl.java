package com.book.bookflow.service.impl;

import com.book.bookflow.entity.Banner;
import com.book.bookflow.mapper.BannerMapper;
import com.book.bookflow.service.BannerService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    private final BannerMapper bannerMapper;

    public BannerServiceImpl(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    @Override
    public List<Map<String, String>> getActiveBanners() {
        List<Banner> banners = bannerMapper.selectListByQuery(
            QueryWrapper.create().where("status = 1").orderBy("id desc")
        );
        return banners.stream()
            .map(Banner::getImageUrl)
            .map(this::normalizeImageUrl)
            .filter(url -> !url.isBlank())
            .map(url -> {
                Map<String, String> item = new HashMap<>();
                item.put("image", url);
                return item;
            })
            .collect(Collectors.toList());
    }

    private String normalizeImageUrl(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        if (url.isBlank()) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/static/")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        String baseUrl = imageBaseUrl == null ? "" : imageBaseUrl.trim();
        if (baseUrl.isBlank()) {
            return url;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + url;
    }
}
