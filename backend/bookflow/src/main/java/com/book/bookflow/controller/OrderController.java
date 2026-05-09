package com.book.bookflow.controller;

import com.book.bookflow.common.Result;
import com.book.bookflow.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return Result.success("创建成功", orderService.createOrder(payload));
    }

    @PostMapping("/checkout")
    public Result<Map<String, Object>> checkout(@RequestBody Map<String, Object> payload) {
        return Result.success("结算成功", orderService.checkout(payload));
    }

    @PostMapping("/cancel")
    public Result<Map<String, Object>> cancel(@RequestBody Map<String, Object> payload) {
        return Result.success("取消成功", orderService.cancelOrder(asLong(payload.get("orderId"))));
    }

    @PostMapping("/pay/mock")
    public Result<Map<String, Object>> payMock(@RequestBody Map<String, Object> payload) {
        return Result.success("支付成功", orderService.payMock(asLong(payload.get("orderId"))));
    }

    @PostMapping("/pay/prepay")
    public Result<Map<String, Object>> prepay(@RequestBody Map<String, Object> payload) {
        return Result.success("预支付创建成功", orderService.createPaymentPrepay(asLong(payload.get("orderId"))));
    }

    @PostMapping("/pay")
    public Result<Map<String, Object>> pay(@RequestBody Map<String, Object> payload) {
        return Result.success("预支付创建成功", orderService.payOrder(asLong(payload.get("orderId"))));
    }

    @PostMapping("/pay/notify/wechat")
    public Map<String, Object> wechatNotify(@RequestHeader Map<String, String> headers, @RequestBody String body) {
        return orderService.handleWechatPayNotify(headers, body);
    }

    @GetMapping("/pay/status")
    public Result<Map<String, Object>> payStatus(@RequestParam Long orderId) {
        return Result.success(orderService.getPayStatus(orderId));
    }

    @PostMapping("/ship")
    public Result<Map<String, Object>> ship(@RequestBody Map<String, Object> payload) {
        return Result.success("发货成功", orderService.shipOrder(payload));
    }

    @PostMapping("/confirm-receipt")
    public Result<Map<String, Object>> confirmReceipt(@RequestBody Map<String, Object> payload) {
        return Result.success("确认收货成功", orderService.confirmReceipt(asLong(payload.get("orderId"))));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long orderId) {
        return Result.success(orderService.getOrderDetail(orderId));
    }

    @PostMapping("/issue/create")
    public Result<Map<String, Object>> createIssue(@RequestBody Map<String, Object> payload) {
        return Result.success("提交成功", orderService.createIssue(payload));
    }

    @GetMapping("/issue/list")
    public Result<List<Map<String, Object>>> issueList(@RequestParam Long orderId) {
        return Result.success(orderService.getIssueList(orderId));
    }

    @PostMapping("/issue/reply")
    public Result<Map<String, Object>> replyIssue(@RequestBody Map<String, Object> payload) {
        return Result.success("回复成功", orderService.replyIssue(payload));
    }

    @PostMapping("/report")
    public Result<Map<String, Object>> report(@RequestBody Map<String, Object> payload) {
        return Result.success("举报成功", orderService.reportOrder(payload));
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
