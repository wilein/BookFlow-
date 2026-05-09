"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_auth = require("../auth.js");
const utils_api_request = require("./request.js");
const utils_config = require("../config.js");
function getPathList(params = {}) {
  return utils_api_request.get("/path/list", params);
}
function getPathDetail(id) {
  return utils_api_request.get("/path/detail", { id });
}
function savePathDraft(data) {
  return utils_api_request.post("/path/save-draft", data);
}
function publishPath(data) {
  return utils_api_request.post("/path/publish", data);
}
function uploadPathCover(filePath) {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getAuthToken();
    common_vendor.index.uploadFile({
      url: `${utils_config.API_BASE_URL}/path/upload-cover`,
      filePath,
      name: "file",
      header: {
        ...token ? { Authorization: `Bearer ${token}` } : {}
      },
      success: (res) => {
        let body = res.data || {};
        if (typeof body === "string") {
          try {
            body = JSON.parse(body);
          } catch (error) {
            reject(error);
            return;
          }
        }
        if (String(body.code) === "200") {
          resolve(body.data || {});
          return;
        }
        reject(new Error(body.message || "封面上传失败"));
      },
      fail: reject
    });
  });
}
function startPathLearning(pathId) {
  return utils_api_request.post("/path/progress/start", { pathId });
}
function cancelPathLearning(pathId) {
  return utils_api_request.post("/path/progress/cancel", { pathId });
}
function getMyLearningPaths() {
  return utils_api_request.get("/path/progress/my");
}
function getCurrentLearningPath() {
  return utils_api_request.get("/path/progress/current", void 0, { showError: false });
}
function completePathNode(pathId, nodeId, completed = true) {
  return utils_api_request.post("/path/progress/complete-node", { pathId, nodeId, completed });
}
exports.cancelPathLearning = cancelPathLearning;
exports.completePathNode = completePathNode;
exports.getCurrentLearningPath = getCurrentLearningPath;
exports.getMyLearningPaths = getMyLearningPaths;
exports.getPathDetail = getPathDetail;
exports.getPathList = getPathList;
exports.publishPath = publishPath;
exports.savePathDraft = savePathDraft;
exports.startPathLearning = startPathLearning;
exports.uploadPathCover = uploadPathCover;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/path.js.map
