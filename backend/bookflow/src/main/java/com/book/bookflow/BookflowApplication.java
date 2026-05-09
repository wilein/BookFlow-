package com.book.bookflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.book.bookflow.mapper")
@SpringBootApplication

public class BookflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookflowApplication.class, args);
    }

}
