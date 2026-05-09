package com.book.bookflow.controller.admin;

import com.book.bookflow.common.AdminResult;
import com.book.bookflow.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/summary")
    public AdminResult<Map<String, Object>> summary() {
        return AdminResult.success(adminService.dashboardSummary());
    }

    @GetMapping("/logs")
    public AdminResult<Map<String, Object>> logs(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String module) {
        return AdminResult.success(adminService.listLogs(pageNo, pageSize, module));
    }
}
