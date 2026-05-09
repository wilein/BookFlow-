package com.book.bookflow.service;

import java.util.Map;

public interface OrderService {
    Map<String, Object> createOrder(Map<String, Object> payload);

    Map<String, Object> checkout(Map<String, Object> payload);

    Map<String, Object> cancelOrder(Long orderId);

    Map<String, Object> payOrder(Long orderId);

    Map<String, Object> createPaymentPrepay(Long orderId);

    Map<String, Object> payMock(Long orderId);

    Map<String, Object> getPayStatus(Long orderId);

    Map<String, Object> handleWechatPayNotify(Map<String, String> headers, String body);

    Map<String, Object> confirmReceipt(Long orderId);

    Map<String, Object> getOrderDetail(Long orderId);

    Map<String, Object> shipOrder(Map<String, Object> payload);

    Map<String, Object> createIssue(Map<String, Object> payload);

    java.util.List<Map<String, Object>> getIssueList(Long orderId);

    Map<String, Object> replyIssue(Map<String, Object> payload);

    Map<String, Object> reportOrder(Map<String, Object> payload);
}
