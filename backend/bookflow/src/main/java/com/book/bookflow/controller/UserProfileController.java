package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.ProfileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final ProfileService profileService;

    public UserProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        return Result.success(profileService.getProfile());
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(profileService.getUserStats());
    }

    @PostMapping("/profile/update")
    public Result<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> payload) {
        return Result.success("\u66f4\u65b0\u6210\u529f", profileService.updateProfile(payload));
    }

    @PostMapping("/profile/verify-student")
    public Result<Map<String, Object>> verifyStudent(@RequestBody Map<String, Object> payload) {
        return Result.success("\u63d0\u4ea4\u6210\u529f", profileService.submitStudentVerify(payload));
    }

    @PostMapping(value = "/profile/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("type") String type) {
        return Result.success("\u4e0a\u4f20\u6210\u529f", profileService.uploadProfileImage(file, type));
    }

    @GetMapping("/profile/student-card/view")
    public ResponseEntity<Resource> viewOwnStudentCard(@RequestParam("url") String url) {
        Path path = profileService.resolveOwnStudentCardImagePath(url);
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

    @GetMapping("/bookshelf")
    public Result<List<Map<String, Object>>> bookshelf(@RequestParam(defaultValue = "selling") String status) {
        return Result.success(profileService.getMyBookshelf(status));
    }

    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders(@RequestParam(defaultValue = "all") String status,
                                                    @RequestParam(defaultValue = "buyer") String role) {
        return Result.success(profileService.getMyOrders(status, role));
    }

    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> favorites(@RequestParam(defaultValue = "book") String type) {
        return Result.success(profileService.getMyFavorites(type));
    }

    @GetMapping("/annotations")
    public Result<List<Map<String, Object>>> annotations() {
        return Result.success(profileService.getMyAnnotations());
    }

    @GetMapping("/paths")
    public Result<List<Map<String, Object>>> paths() {
        return Result.success(profileService.getMyPaths());
    }

    @GetMapping("/address/list")
    public Result<List<Map<String, Object>>> addressList() {
        return Result.success(profileService.getAddressList());
    }

    @PostMapping("/address/save")
    public Result<Map<String, Object>> saveAddress(@RequestBody Map<String, Object> payload) {
        return Result.success("\u4fdd\u5b58\u6210\u529f", profileService.saveAddress(payload));
    }

    @PostMapping("/address/delete")
    public Result<Map<String, Object>> deleteAddress(@RequestBody Map<String, Object> payload) {
        return Result.success("\u5220\u9664\u6210\u529f", profileService.deleteAddress(payload));
    }

    @PostMapping("/address/set-default")
    public Result<Map<String, Object>> setDefaultAddress(@RequestBody Map<String, Object> payload) {
        return Result.success("\u8bbe\u7f6e\u6210\u529f", profileService.setDefaultAddress(payload));
    }

    @GetMapping("/history/list")
    public Result<List<Map<String, Object>>> browseHistory() {
        return Result.success(profileService.getBrowseHistory());
    }

    @PostMapping("/history/record")
    public Result<Map<String, Object>> recordBrowseHistory(@RequestBody Map<String, Object> payload) {
        return Result.success("\u8bb0\u5f55\u6210\u529f", profileService.recordBrowseHistory(payload));
    }

    @PostMapping("/history/delete")
    public Result<Map<String, Object>> deleteBrowseHistory(@RequestBody Map<String, Object> payload) {
        return Result.success("\u5220\u9664\u6210\u529f", profileService.deleteBrowseHistory(payload));
    }

    @PostMapping("/history/clear")
    public Result<Map<String, Object>> clearBrowseHistory() {
        return Result.success("\u6e05\u7a7a\u6210\u529f", profileService.clearBrowseHistory());
    }

    @PostMapping("/feedback/submit")
    public Result<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> payload) {
        return Result.success("\u63d0\u4ea4\u6210\u529f", profileService.submitFeedback(payload));
    }

    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> notifications() {
        return Result.success(profileService.getNotifications());
    }

    @PostMapping("/notification/read")
    public Result<Map<String, Object>> readNotification(@RequestBody Map<String, Object> payload) {
        return Result.success("\u5904\u7406\u6210\u529f", profileService.readNotification(payload));
    }
}
