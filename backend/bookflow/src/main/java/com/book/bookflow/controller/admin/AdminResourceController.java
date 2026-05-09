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
@RequestMapping("/admin/resources")
public class AdminResourceController {

    private final AdminService adminService;

    public AdminResourceController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Integer visibility,
                                                 @RequestParam(required = false) String bindType) {
        return AdminResult.success(adminService.listResources(pageNo, pageSize, keyword, status, visibility, bindType));
    }

    @PostMapping("/save")
    public AdminResult<Map<String, Object>> save(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.saveResource(payload));
    }

    @PostMapping("/change-status")
    public AdminResult<Map<String, Object>> changeStatus(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.changeResourceStatus(payload));
    }
}
