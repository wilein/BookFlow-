"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_request = require("./request.js");
const utils_auth = require("../auth.js");
const utils_config = require("../config.js");
function getMyResources() {
  return utils_api_request.get("/resource/my-list");
}
function getResourceDetail(id) {
  return utils_api_request.get("/resource/detail", { id });
}
function getResourceList(params = {}) {
  return utils_api_request.get("/resource/list", params);
}
function createResource(data) {
  return utils_api_request.post("/resource/create", data);
}
function updateResource(data) {
  return utils_api_request.post("/resource/update", data);
}
function uploadResourceFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    common_vendor.index.uploadFile({
      url: `${utils_config.API_BASE_URL}/resource/upload-file`,
      filePath,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        utils_auth.syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === "string" ? JSON.parse(res.data) : res.data || {};
        } catch (error) {
          common_vendor.index.showToast({ title: "资源上传失败", icon: "none" });
          reject(error);
          return;
        }
        if (String(body.code) === "200") {
          resolve(body.data || {});
          return;
        }
        const message = body.message || "资源上传失败";
        if (String(body.code) === "401") {
          utils_auth.clearSession();
          utils_auth.redirectToLogin();
        }
        common_vendor.index.showToast({ title: message, icon: "none" });
        reject(new Error(message));
      },
      fail: (error) => {
        common_vendor.index.showToast({ title: "资源上传失败", icon: "none" });
        reject(error);
      }
    });
  });
}
exports.createResource = createResource;
exports.getMyResources = getMyResources;
exports.getResourceDetail = getResourceDetail;
exports.getResourceList = getResourceList;
exports.updateResource = updateResource;
exports.uploadResourceFile = uploadResourceFile;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/resource.js.map
