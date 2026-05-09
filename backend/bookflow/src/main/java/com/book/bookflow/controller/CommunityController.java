package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.CommunityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/feed")
    public Result<List<Map<String, Object>>> feed(@RequestParam(required = false) String type) {
        return Result.success(communityService.getFeed(type));
    }

    @GetMapping("/post/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long postId) {
        return Result.success(communityService.getPostDetail(postId));
    }

    @GetMapping("/activity")
    public Result<List<Map<String, Object>>> activity() {
        return Result.success(communityService.getActivity());
    }

    @PostMapping("/post/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return Result.success("发布成功", communityService.createPost(payload));
    }

    @PostMapping("/post/toggle-like")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Object> payload) {
        return Result.success(communityService.togglePostLike(asLong(payload.get("postId"))));
    }

    @PostMapping("/post/toggle-favorite")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody Map<String, Object> payload) {
        return Result.success(communityService.togglePostFavorite(asLong(payload.get("postId"))));
    }

    @GetMapping("/post/comment/list")
    public Result<List<Map<String, Object>>> comments(@RequestParam Long postId) {
        return Result.success(communityService.getCommentList(postId));
    }

    @PostMapping("/post/comment/create")
    public Result<Map<String, Object>> createComment(@RequestBody Map<String, Object> payload) {
        return Result.success("评论成功", communityService.createComment(payload));
    }

    @PostMapping("/post/report")
    public Result<Map<String, Object>> report(@RequestBody Map<String, Object> payload) {
        return Result.success("举报成功", communityService.reportPost(payload));
    }

    @GetMapping("/my-reports")
    public Result<List<Map<String, Object>>> myReports() {
        return Result.success(communityService.getMyReports());
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
}
