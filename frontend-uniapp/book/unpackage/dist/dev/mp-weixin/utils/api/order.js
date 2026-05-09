"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_request = require("./request.js");
function checkoutOrder(data) {
  return utils_api_request.post("/order/checkout", data);
}
function cancelOrder(orderId) {
  return utils_api_request.post("/order/cancel", { orderId });
}
function payOrder(orderId) {
  return createPaymentPrepay(orderId).then((prepay) => {
    if (prepay == null ? void 0 : prepay.mock) {
      return payMockOrder(orderId);
    }
    return requestWechatPayment(prepay);
  });
}
function createPaymentPrepay(orderId) {
  return utils_api_request.post("/order/pay/prepay", { orderId });
}
function payMockOrder(orderId) {
  return utils_api_request.post("/order/pay/mock", { orderId });
}
function requestWechatPayment(prepay = {}) {
  return new Promise((resolve, reject) => {
    if (typeof common_vendor.index.requestPayment !== "function") {
      const error = new Error("当前环境不支持微信支付");
      common_vendor.index.showToast({ title: error.message, icon: "none" });
      reject(error);
      return;
    }
    common_vendor.index.requestPayment({
      provider: "wxpay",
      timeStamp: String(prepay.timeStamp || ""),
      nonceStr: prepay.nonceStr || "",
      package: prepay.package || "",
      signType: prepay.signType || "RSA",
      paySign: prepay.paySign || "",
      success: () => resolve(prepay),
      fail: (error) => {
        var _a;
        const message = ((_a = error == null ? void 0 : error.errMsg) == null ? void 0 : _a.includes("cancel")) ? "支付已取消" : "支付未完成";
        common_vendor.index.showToast({ title: message, icon: "none" });
        reject(error);
      }
    });
  });
}
function confirmReceipt(orderId) {
  return utils_api_request.post("/order/confirm-receipt", { orderId });
}
function shipOrder(orderId) {
  return utils_api_request.post("/order/ship", { orderId });
}
function getOrderDetail(orderId) {
  return utils_api_request.get("/order/detail", { orderId });
}
function createOrderIssue(data) {
  return utils_api_request.post("/order/issue/create", data);
}
function getOrderIssues(orderId) {
  return utils_api_request.get("/order/issue/list", { orderId });
}
function replyOrderIssue(data) {
  return utils_api_request.post("/order/issue/reply", data);
}
function reportOrder(data) {
  return utils_api_request.post("/order/report", data);
}
exports.cancelOrder = cancelOrder;
exports.checkoutOrder = checkoutOrder;
exports.confirmReceipt = confirmReceipt;
exports.createOrderIssue = createOrderIssue;
exports.getOrderDetail = getOrderDetail;
exports.getOrderIssues = getOrderIssues;
exports.payOrder = payOrder;
exports.replyOrderIssue = replyOrderIssue;
exports.reportOrder = reportOrder;
exports.shipOrder = shipOrder;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/order.js.map
