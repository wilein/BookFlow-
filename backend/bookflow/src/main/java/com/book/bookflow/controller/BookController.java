package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.BookService;
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
@RequestMapping("/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getBook() {
        return Result.success(bookService.getBooksList());
    }

    @GetMapping("/category")
    public Result<Map<String, List<Map<String, Object>>>> getBooksByCategory() {
        return Result.success(bookService.getBooksByCategory());
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> searchBooks(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bookService.searchBooks(keyword, category, pageNo, pageSize));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> getBookDetail(@RequestParam(required = false) String id,
                                                     @RequestParam(required = false) String bookId) {
        return Result.success(bookService.getBookDetail(parseBookId(firstNotBlank(id, bookId))));
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publishBook(@RequestBody Map<String, Object> payload) {
        return Result.success("\u53d1\u5e03\u6210\u529f", bookService.publishBook(payload));
    }

    @PostMapping("/update")
    public Result<Map<String, Object>> updateBook(@RequestBody Map<String, Object> payload) {
        return Result.success("\u66f4\u65b0\u6210\u529f", bookService.updateBook(payload));
    }

    @PostMapping("/change-status")
    public Result<Map<String, Object>> changeBookStatus(@RequestBody Map<String, Object> payload) {
        return Result.success("\u72b6\u6001\u5df2\u66f4\u65b0", bookService.changeBookStatus(payload));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success("\u4e0a\u4f20\u6210\u529f", bookService.uploadBookImage(file));
    }

    private Long parseBookId(String id) {
        if (id == null || id.isBlank()) {
            throw new CustomerException("400", "\u4e66\u7c4d\u53c2\u6570\u9519\u8bef");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException exception) {
            throw new CustomerException("400", "\u4e66\u7c4d\u53c2\u6570\u9519\u8bef");
        }
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
