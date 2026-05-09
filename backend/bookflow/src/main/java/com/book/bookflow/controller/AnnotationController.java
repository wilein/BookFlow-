package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.AnnotationService;
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
@RequestMapping("/annotation")
public class AnnotationController {

    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String bookId,
                                            @RequestParam(required = false) String mineOnly) {
        return Result.success(annotationService.getAnnotationList(parseBookId(bookId), parseBoolean(mineOnly)));
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return Result.success("发布成功", annotationService.createAnnotation(payload));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", annotationService.uploadAnnotationImage(file));
    }

    @PostMapping("/toggle-like")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Object> payload) {
        return Result.success(annotationService.toggleLike(asLong(payload.get("annotationId"))));
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

    private Long parseBookId(String value) {
        if (value == null || value.isBlank() || "undefined".equalsIgnoreCase(value.trim())) {
            throw new CustomerException("400", "\u4e66\u7c4d\u53c2\u6570\u9519\u8bef");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new CustomerException("400", "\u4e66\u7c4d\u53c2\u6570\u9519\u8bef");
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null || value.isBlank() || "undefined".equalsIgnoreCase(value.trim())) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized);
    }
}
