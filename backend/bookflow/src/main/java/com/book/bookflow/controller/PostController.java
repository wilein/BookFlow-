package com.book.bookflow.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
public class PostController {
    @RequestMapping("/list")
    public String list() {
        return "post list";
    }
}
