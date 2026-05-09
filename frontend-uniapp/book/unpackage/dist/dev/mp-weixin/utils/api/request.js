"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_auth = require("../auth.js");
const utils_config = require("../config.js");
const TEXTS = {
  requestFailed: "请求失败",
  networkError: "网络错误"
};
function request({ url, method = "GET", data, header = {}, showError = true, withAuth = true, redirectOn401 = true }) {
  return new Promise((resolve, reject) => {
    const token = withAuth ? utils_auth.getAuthToken() : "";
    common_vendor.index.request({
      url: `${utils_config.API_BASE_URL}${url}`,
      method,
      data: sanitizeRequestData(data),
      header: {
        "Content-Type": "application/json",
        ...token ? { Authorization: `Bearer ${token}` } : {},
        ...header
      },
      success: (res) => {
        utils_auth.syncSessionFromHeaders(res.header || {});
        const body = res.data || {};
        if (String(body.code) === "200") {
          resolve(normalizeUploadUrls(body.data));
          return;
        }
        const message = body.message || TEXTS.requestFailed;
        if (String(body.code) === "401") {
          utils_auth.clearSession();
          if (redirectOn401) {
            utils_auth.redirectToLogin();
          }
        }
        if (showError) {
          common_vendor.index.showToast({ title: message, icon: "none" });
        }
        reject(new Error(message));
      },
      fail: (error) => {
        if (showError) {
          common_vendor.index.showToast({ title: TEXTS.networkError, icon: "none" });
        }
        reject(error);
      }
    });
  });
}
function get(url, data, options = {}) {
  return request({ url, method: "GET", data, ...options });
}
function post(url, data, options = {}) {
  return request({ url, method: "POST", data, ...options });
}
function normalizeUploadUrls(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeUploadUrls);
  }
  if (value && typeof value === "object") {
    return Object.keys(value).reduce((result, key) => {
      result[key] = normalizeUploadUrls(value[key]);
      return result;
    }, {});
  }
  if (typeof value === "string") {
    const text = value.trim();
    if (text.startsWith("/uploads/") || text.startsWith("uploads/")) {
      return utils_config.normalizeImageUrl(text);
    }
  }
  return value;
}
function sanitizeRequestData(value) {
  if (Array.isArray(value)) {
    return value.map(sanitizeRequestData).filter((item) => item !== void 0);
  }
  if (value && typeof value === "object") {
    return Object.keys(value).reduce((result, key) => {
      const item = sanitizeRequestData(value[key]);
      if (item !== void 0) {
        result[key] = item;
      }
      return result;
    }, {});
  }
  if (value === void 0 || value === null) {
    return void 0;
  }
  return value;
}
exports.get = get;
exports.post = post;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/request.js.map
