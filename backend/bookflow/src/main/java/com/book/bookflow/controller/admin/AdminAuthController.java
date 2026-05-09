package com.book.bookflow.controller.admin;

import com.book.bookflow.common.AdminResult;
import com.book.bookflow.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/auth/login")
    public AdminResult<Map<String, Object>> login(@RequestBody Map<String, Object> payload) {
        return AdminResult.success(adminService.login(payload));
    }

    @GetMapping("/auth/info")
    public AdminResult<Map<String, Object>> info() {
        return AdminResult.success(adminService.getAdminInfo());
    }

    @GetMapping("/user/info")
    public AdminResult<Map<String, Object>> userInfo() {
        return AdminResult.success(adminService.getAdminInfo());
    }

    @GetMapping("/auth/codes")
    public AdminResult<List<String>> codes() {
        return AdminResult.success(adminService.getAccessCodes());
    }

    @PostMapping("/auth/refresh")
    public AdminResult<Map<String, Object>> refresh(HttpServletRequest request) {
        return AdminResult.success(adminService.refreshToken(request.getHeader("Authorization")));
    }

    @PostMapping("/auth/logout")
    public AdminResult<Map<String, Object>> logout() {
        return AdminResult.success(Map.of());
    }

    @GetMapping("/menu/all")
    public AdminResult<List<Map<String, Object>>> menus() {
        return AdminResult.success(adminService.getMenus());
    }
}
