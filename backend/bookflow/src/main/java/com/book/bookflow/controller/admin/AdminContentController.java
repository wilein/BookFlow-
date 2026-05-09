package com.book.bookflow.controller.admin;

import com.book.bookflow.common.AdminResult;
import com.book.bookflow.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/content")
public class AdminContentController {

    private final AdminService adminService;

    public AdminContentController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/posts")
    public AdminResult<Map<String, Object>> posts(@RequestParam(defaultValue = "1") Integer pageNo,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer type,
                                                  @RequestParam(required = false) Integer status) {
        return AdminResult.success(adminService.listPosts(pageNo, pageSize, keyword, type, status));
    }

    @PostMapping("/posts/save")
    public AdminResult<Map<String, Object>> savePost(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.savePost(payload));
    }

    @PostMapping("/posts/change-status")
    public AdminResult<Map<String, Object>> changePostStatus(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.changePostStatus(payload));
    }

    @GetMapping("/comments")
    public AdminResult<Map<String, Object>> comments(@RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return AdminResult.success(adminService.listComments(pageNo, pageSize, keyword, status));
    }

    @PostMapping("/comments/change-status")
    public AdminResult<Map<String, Object>> changeCommentStatus(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.changeCommentStatus(payload));
    }
}
