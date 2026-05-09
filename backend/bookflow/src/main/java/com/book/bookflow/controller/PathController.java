package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.PathService;
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
@RequestMapping("/path")
public class PathController {

    private final PathService pathService;

    public PathController(PathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(pathService.getPublicPaths(category, keyword));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long id) {
        return Result.success(pathService.getPathDetail(id));
    }

    @PostMapping("/save-draft")
    public Result<Map<String, Object>> saveDraft(@RequestBody Map<String, Object> payload) {
        return Result.success("保存成功", pathService.saveDraft(payload));
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestBody Map<String, Object> payload) {
        return Result.success("提交成功", pathService.publishPath(payload));
    }

    @PostMapping(value = "/upload-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadCover(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", pathService.uploadPathCover(file));
    }

    @PostMapping("/copy")
    public Result<Map<String, Object>> copy(@RequestBody Map<String, Object> payload) {
        return Result.success("复制成功", pathService.copyPath(asLong(payload.get("pathId"))));
    }

    @PostMapping("/change-status")
    public Result<Map<String, Object>> changeStatus(@RequestBody Map<String, Object> payload) {
        return Result.success("状态已更新", pathService.changePathStatus(payload));
    }

    @PostMapping("/progress/start")
    public Result<Map<String, Object>> start(@RequestBody Map<String, Object> payload) {
        return Result.success(pathService.startLearning(asLong(payload.get("pathId"))));
    }

    @PostMapping("/progress/cancel")
    public Result<Map<String, Object>> cancel(@RequestBody Map<String, Object> payload) {
        return Result.success(pathService.cancelLearning(asLong(payload.get("pathId"))));
    }

    @GetMapping("/progress/my")
    public Result<List<Map<String, Object>>> myLearningPaths() {
        return Result.success(pathService.getMyLearningPaths());
    }

    @GetMapping("/progress/current")
    public Result<Map<String, Object>> currentLearningPath() {
        return Result.success(pathService.getCurrentLearningPath());
    }

    @PostMapping("/progress/complete-node")
    public Result<Map<String, Object>> completeNode(@RequestBody Map<String, Object> payload) {
        return Result.success(pathService.completeNode(payload));
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
}
