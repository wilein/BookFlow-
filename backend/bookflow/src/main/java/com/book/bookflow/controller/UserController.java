package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Value("${app.security.dev-login-enabled:false}")
    private boolean devLoginEnabled;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/wechat")
    public Result<Map<String, Object>> authWechat(
        @RequestParam(value = "code", required = false) String code,
        @RequestBody(required = false) Map<String, Object> payload
    ) {
        String loginCode = code;
        if ((loginCode == null || loginCode.isBlank()) && payload != null) {
            Object value = payload.get("code");
            loginCode = value == null ? "" : String.valueOf(value).trim();
        }
        return Result.success(userService.loginWithWechat(loginCode));
    }

    @PostMapping("/auth/dev-login")
    public Result<Map<String, Object>> devLogin() {
        if (!devLoginEnabled) {
            throw new CustomerException("403", "开发登录未启用");
        }
        return Result.success(userService.loginForDev());
    }

    @GetMapping("/auth/check")
    public Result<Map<String, Object>> check(@RequestHeader("Authorization") String authorization) {
        return Result.success(userService.getUserInfoByToken(AuthContext.getToken() != null ? AuthContext.getToken() : authorization));
    }

    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        userService.logout(AuthContext.getToken() != null ? AuthContext.getToken() : authorization);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authorization) {
        return Result.success(userService.getUserInfoByToken(AuthContext.getToken() != null ? AuthContext.getToken() : authorization));
    }
}
