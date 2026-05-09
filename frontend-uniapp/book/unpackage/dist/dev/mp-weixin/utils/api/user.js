"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_request = require("./request.js");
const utils_auth = require("../auth.js");
const utils_config = require("../config.js");
const TEXTS = {
  uploadParseFail: "上传响应解析失败",
  uploadFail: "上传失败"
};
function loginWithWechat(code) {
  return utils_api_request.post(`/user/auth/wechat?code=${encodeURIComponent(code)}`, {}, { withAuth: false, redirectOn401: false }).then((data) => {
    utils_auth.saveSession(data || {});
    return data;
  });
}
function loginWithDev() {
  return utils_api_request.post("/user/auth/dev-login", {}, { withAuth: false, redirectOn401: false }).then((data) => {
    utils_auth.saveSession(data || {});
    return data;
  });
}
function checkAuthSession() {
  return utils_api_request.get("/user/auth/check").then((data) => {
    utils_auth.syncProfileState((data == null ? void 0 : data.userInfo) || {});
    return data;
  });
}
function logoutAuth() {
  return utils_api_request.post("/user/auth/logout");
}
function getUserProfile() {
  return utils_api_request.get("/user/profile").then((data) => {
    utils_auth.syncProfileState(data || {});
    return data;
  });
}
function getUserStats() {
  return utils_api_request.get("/user/stats");
}
function updateUserProfile(data) {
  return utils_api_request.post("/user/profile/update", data).then((result) => {
    utils_auth.syncProfileState(result || {});
    return result;
  });
}
function verifyStudent(data) {
  return utils_api_request.post("/user/profile/verify-student", data).then((result) => {
    utils_auth.syncProfileState(result || {});
    return result;
  });
}
function uploadProfileImage(filePath, type) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    common_vendor.index.uploadFile({
      url: `${utils_config.API_BASE_URL}/user/profile/upload-image`,
      filePath,
      name: "file",
      formData: { type },
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
function downloadStudentCardImage(url) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    if (!url || !token) {
      reject(new Error("学生证图片不可用"));
      return;
    }
    common_vendor.index.downloadFile({
      url: `${utils_config.API_BASE_URL}/user/profile/student-card/view?url=${encodeURIComponent(url)}`,
      header: { Authorization: `Bearer ${token}` },
      success: (res) => {
        utils_auth.syncSessionFromHeaders(res.header || {});
        if (res.statusCode === 200 && res.tempFilePath) {
          resolve(res.tempFilePath);
          return;
        }
        reject(new Error("学生证图片加载失败"));
      },
      fail: reject
    });
  });
}
function getMyBookshelf(status = "selling") {
  return utils_api_request.get("/user/bookshelf", { status });
}
function getMyOrders(status = "all", role = "buyer") {
  return utils_api_request.get("/user/orders", { status, role });
}
function getMyFavorites(type = "book") {
  return utils_api_request.get("/user/favorites", { type });
}
function getMyAnnotations() {
  return utils_api_request.get("/user/annotations");
}
function getMyPaths() {
  return utils_api_request.get("/user/paths");
}
function getAddressList() {
  return utils_api_request.get("/user/address/list");
}
function saveAddress(data) {
  return utils_api_request.post("/user/address/save", data);
}
function deleteAddress(id) {
  return utils_api_request.post("/user/address/delete", { id });
}
function setDefaultAddress(id) {
  return utils_api_request.post("/user/address/set-default", { id });
}
function getBrowseHistory() {
  return utils_api_request.get("/user/history/list");
}
function recordBrowseHistory(data) {
  return utils_api_request.post("/user/history/record", data, { showError: false });
}
function deleteBrowseHistory(id) {
  return utils_api_request.post("/user/history/delete", { id });
}
function clearBrowseHistory() {
  return utils_api_request.post("/user/history/clear", {});
}
function submitFeedback(data) {
  return utils_api_request.post("/user/feedback/submit", data);
}
function getNotifications() {
  return utils_api_request.get("/user/notifications");
}
function readNotification(id) {
  return utils_api_request.post("/user/notification/read", { id });
}
exports.checkAuthSession = checkAuthSession;
exports.clearBrowseHistory = clearBrowseHistory;
exports.deleteAddress = deleteAddress;
exports.deleteBrowseHistory = deleteBrowseHistory;
exports.downloadStudentCardImage = downloadStudentCardImage;
exports.getAddressList = getAddressList;
exports.getBrowseHistory = getBrowseHistory;
exports.getMyAnnotations = getMyAnnotations;
exports.getMyBookshelf = getMyBookshelf;
exports.getMyFavorites = getMyFavorites;
exports.getMyOrders = getMyOrders;
exports.getMyPaths = getMyPaths;
exports.getNotifications = getNotifications;
exports.getUserProfile = getUserProfile;
exports.getUserStats = getUserStats;
exports.loginWithDev = loginWithDev;
exports.loginWithWechat = loginWithWechat;
exports.logoutAuth = logoutAuth;
exports.readNotification = readNotification;
exports.recordBrowseHistory = recordBrowseHistory;
exports.saveAddress = saveAddress;
exports.setDefaultAddress = setDefaultAddress;
exports.submitFeedback = submitFeedback;
exports.updateUserProfile = updateUserProfile;
exports.uploadProfileImage = uploadProfileImage;
exports.verifyStudent = verifyStudent;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/user.js.map
