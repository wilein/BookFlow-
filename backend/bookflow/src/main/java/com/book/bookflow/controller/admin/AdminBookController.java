package com.book.bookflow.controller.admin;

import com.book.bookflow.common.AdminResult;
import com.book.bookflow.service.AdminService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/books")
public class AdminBookController {

    private final AdminService adminService;

    public AdminBookController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        return AdminResult.success(adminService.listBooks(pageNo, pageSize, keyword, status));
    }

    @PostMapping("/save")
    public AdminResult<Map<String, Object>> save(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.saveBook(payload));
    }

    @PostMapping("/change-status")
    public AdminResult<Map<String, Object>> changeStatus(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("处理成功", adminService.changeBookStatus(payload));
    }

    @PostMapping("/delete")
    public AdminResult<Map<String, Object>> delete(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("删除成功", adminService.deleteBook(payload));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminResult<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return AdminResult.success("上传成功", adminService.uploadBookImage(file));
    }

    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminResult<Map<String, Object>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        return AdminResult.success("上传成功", adminService.uploadBookImages(files));
    }

    @PostMapping("/images")
    public AdminResult<Map<String, Object>> updateImages(@RequestBody Map<String, Object> payload) {
        return AdminResult.success("保存成功", adminService.updateBookImages(payload));
    }
}
