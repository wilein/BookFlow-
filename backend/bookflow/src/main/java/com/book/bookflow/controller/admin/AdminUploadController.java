package com.book.bookflow.controller.admin;

import com.book.bookflow.common.AdminResult;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.AdminService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/admin/uploads")
public class AdminUploadController {

    private final AdminService adminService;

    public AdminUploadController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping(value = "/student-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminResult<Map<String, Object>> uploadStudentCard(@RequestParam("file") MultipartFile file) {
        return AdminResult.success("上传成功", adminService.uploadStudentCardImage(file));
    }

    @GetMapping("/student-card/view")
    public ResponseEntity<Resource> viewStudentCard(@RequestParam("url") String url) {
        Path path = adminService.resolveStudentCardImagePath(url);
        try {
            String contentType = Files.probeContentType(path);
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.IMAGE_JPEG_VALUE : contentType))
                .body(resource);
        } catch (MalformedURLException exception) {
            throw new CustomerException("404", "学生证图片不存在");
        } catch (Exception exception) {
            throw new CustomerException("500", "学生证图片读取失败");
        }
    }

    @PostMapping(value = "/path-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminResult<Map<String, Object>> uploadPathCover(@RequestParam("file") MultipartFile file) {
        return AdminResult.success("上传成功", adminService.uploadPathCoverImage(file));
    }
}
