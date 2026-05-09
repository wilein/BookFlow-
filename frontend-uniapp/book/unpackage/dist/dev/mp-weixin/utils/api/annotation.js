"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_request = require("./request.js");
const utils_auth = require("../auth.js");
const utils_config = require("../config.js");
function getAnnotationList(bookId, options = {}) {
  const params = { bookId };
  if (options.mineOnly) {
    params.mineOnly = 1;
  }
  return utils_api_request.get("/annotation/list", params, {
    showError: options.showError !== false
  });
}
function createAnnotation(data) {
  return utils_api_request.post("/annotation/create", data);
}
function toggleAnnotationLike(annotationId) {
  return utils_api_request.post("/annotation/toggle-like", { annotationId });
}
function uploadAnnotationImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    common_vendor.index.uploadFile({
      url: `${utils_config.API_BASE_URL}/annotation/upload-image`,
      filePath,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        utils_auth.syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === "string" ? JSON.parse(res.data) : res.data || {};
        } catch (error) {
          common_vendor.index.showToast({ title: "图片上传失败", icon: "none" });
          reject(error);
          return;
        }
        if (String(body.code) === "200") {
          resolve(body.data || {});
          return;
        }
        const message = body.message || "图片上传失败";
        if (String(body.code) === "401") {
          utils_auth.clearSession();
          utils_auth.redirectToLogin();
        }
        common_vendor.index.showToast({ title: message, icon: "none" });
        reject(new Error(message));
      },
      fail: (error) => {
        common_vendor.index.showToast({ title: "图片上传失败", icon: "none" });
        reject(error);
      }
    });
  });
}
exports.createAnnotation = createAnnotation;
exports.getAnnotationList = getAnnotationList;
exports.toggleAnnotationLike = toggleAnnotationLike;
exports.uploadAnnotationImage = uploadAnnotationImage;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/annotation.js.map
