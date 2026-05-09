"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_request = require("./request.js");
const utils_auth = require("../auth.js");
const utils_config = require("../config.js");
const TEXTS = {
  uploadParseFail: "上传响应解析失败",
  uploadFail: "图片上传失败"
};
function getBookList() {
  return utils_api_request.get("/book/list");
}
function getBooksByCategory() {
  return utils_api_request.get("/book/category");
}
function searchBooks(params = {}) {
  return utils_api_request.get("/book/search", params);
}
function getBookDetail(id) {
  return utils_api_request.get("/book/detail", { id });
}
function publishBook(data) {
  return utils_api_request.post("/book/publish", data);
}
function uploadBookImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    common_vendor.index.uploadFile({
      url: `${utils_config.API_BASE_URL}/book/upload-image`,
      filePath,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        utils_auth.syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === "string" ? JSON.parse(res.data) : res.data || {};
        } catch (error) {
          common_vendor.index.showToast({ title: TEXTS.uploadParseFail, icon: "none" });
          reject(error);
          return;
        }
        if (String(body.code) === "200") {
          resolve(body.data || {});
          return;
        }
        const message = body.message || TEXTS.uploadFail;
        if (String(body.code) === "401") {
          utils_auth.clearSession();
          utils_auth.redirectToLogin();
        }
        common_vendor.index.showToast({ title: message, icon: "none" });
        reject(new Error(message));
      },
      fail: (error) => {
        common_vendor.index.showToast({ title: TEXTS.uploadFail, icon: "none" });
        reject(error);
      }
    });
  });
}
exports.getBookDetail = getBookDetail;
exports.getBookList = getBookList;
exports.getBooksByCategory = getBooksByCategory;
exports.publishBook = publishBook;
exports.searchBooks = searchBooks;
exports.uploadBookImage = uploadBookImage;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/book.js.map
