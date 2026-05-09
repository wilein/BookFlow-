"use strict";
const utils_api_request = require("./request.js");
function getBannerList() {
  return utils_api_request.get("/common/banner/list");
}
exports.getBannerList = getBannerList;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/common.js.map
