package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.ResourceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/my-list")
    public Result<List<Map<String, Object>>> myList() {
        return Result.success(resourceService.getMyResources());
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long id) {
        return Result.success(resourceService.getResourceDetail(id));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String bookId,
                                                  @RequestParam(required = false) String pathNodeId) {
        return Result.success(resourceService.getResources(parseLong(bookId), parseLong(pathNodeId)));
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", resourceService.uploadResourceFile(file));
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return Result.success("创建成功", resourceService.createResource(payload));
    }

    @PostMapping("/update")
    public Result<Map<String, Object>> update(@RequestBody Map<String, Object> payload) {
        return Result.success("更新成功", resourceService.updateResource(payload));
    }

    @PostMapping("/delete")
    public Result<Map<String, Object>> delete(@RequestBody Map<String, Object> payload) {
        return Result.success("删除成功", resourceService.deleteResource(payload));
    }

    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isBlank() || "undefined".equalsIgnoreCase(text) || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
