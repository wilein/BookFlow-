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
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Integer authStatus) {
        return AdminResult.success(adminService.listUsers(pageNo, pageSize, keyword, status, authStatus));
    }

    @PostMapping("/save")
    public AdminResult<Map<String, Object>> save(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.saveUser(payload));
    }

    @PostMapping("/change-status")
    public AdminResult<Map<String, Object>> changeStatus(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.changeUserStatus(payload));
    }

    @PostMapping("/credit")
    public AdminResult<Map<String, Object>> credit(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.updateUserCredit(payload));
    }
}
