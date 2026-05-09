import { get, post } from './request';

export function createOrder(data) {
  return post('/order/create', data);
}

export function checkoutOrder(data) {
  return post('/order/checkout', data);
}

export function cancelOrder(orderId) {
  return post('/order/cancel', { orderId });
}

export function payOrder(orderId) {
  return createPaymentPrepay(orderId).then((prepay) => {
    if (prepay?.mock) {
      return payMockOrder(orderId);
    }
    return requestWechatPayment(prepay);
  });
}

export function createPaymentPrepay(orderId) {
  return post('/order/pay/prepay', { orderId });
}

export function getPayStatus(orderId) {
  return get('/order/pay/status', { orderId });
}

export function payMockOrder(orderId) {
  return post('/order/pay/mock', { orderId });
}

function requestWechatPayment(prepay = {}) {
  return new Promise((resolve, reject) => {
    if (typeof uni.requestPayment !== 'function') {
      const error = new Error('\u5f53\u524d\u73af\u5883\u4e0d\u652f\u6301\u5fae\u4fe1\u652f\u4ed8');
      uni.showToast({ title: error.message, icon: 'none' });
      reject(error);
      return;
    }
    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: String(prepay.timeStamp || ''),
      nonceStr: prepay.nonceStr || '',
      package: prepay.package || '',
      signType: prepay.signType || 'RSA',
      paySign: prepay.paySign || '',
      success: () => resolve(prepay),
      fail: (error) => {
        const message = error?.errMsg?.includes('cancel')
          ? '\u652f\u4ed8\u5df2\u53d6\u6d88'
          : '\u652f\u4ed8\u672a\u5b8c\u6210';
        uni.showToast({ title: message, icon: 'none' });
        reject(error);
      }
    });
  });
}

export function confirmReceipt(orderId) {
  return post('/order/confirm-receipt', { orderId });
}

export function shipOrder(orderId) {
  return post('/order/ship', { orderId });
}

export function getOrderDetail(orderId) {
  return get('/order/detail', { orderId });
}

export function createOrderIssue(data) {
  return post('/order/issue/create', data);
}

export function getOrderIssues(orderId) {
  return get('/order/issue/list', { orderId });
}

export function replyOrderIssue(data) {
  return post('/order/issue/reply', data);
}

export function reportOrder(data) {
  return post('/order/report', data);
}
