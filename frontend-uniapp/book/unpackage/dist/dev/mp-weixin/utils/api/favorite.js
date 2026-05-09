"use strict";
const utils_api_request = require("./request.js");
function toggleFavorite(targetType, targetId) {
  return utils_api_request.post("/favorite/toggle", { targetType, targetId });
}
function getFavoriteStatus(targetType, targetId) {
  return utils_api_request.get("/favorite/status", { targetType, targetId });
}
exports.getFavoriteStatus = getFavoriteStatus;
exports.toggleFavorite = toggleFavorite;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/favorite.js.map
