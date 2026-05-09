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
@RequestMapping("/admin/verify")
public class AdminVerifyController {

    private final AdminService adminService;

    public AdminVerifyController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/list")
    public AdminResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        return AdminResult.success(adminService.listVerifies(pageNo, pageSize, keyword, status));
    }

    @PostMapping("/save")
    public AdminResult<Map<String, Object>> save(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.saveVerify(payload));
    }

    @PostMapping("/audit")
    public AdminResult<Map<String, Object>> audit(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("审核成功", adminService.auditVerify(payload));
    }
}
