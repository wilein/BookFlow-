"use strict";
const common_vendor = require("../common/vendor.js");
const ENV_CONFIG = {
  development: {
    API_BASE_URL: "http://10.212.211.152:8080",
    IMAGE_BASE_URL: "http://10.212.211.152:8080"
  },
  trial: {
    API_BASE_URL: "",
    IMAGE_BASE_URL: ""
  },
  production: {
    API_BASE_URL: "",
    IMAGE_BASE_URL: ""
  }
};
function trimBaseUrl(value) {
  return String(value || "").trim().replace(/\/+$/, "");
}
function getMiniProgramEnv() {
  var _a;
  try {
    const accountInfo = common_vendor.index.getAccountInfoSync && common_vendor.index.getAccountInfoSync();
    return ((_a = accountInfo == null ? void 0 : accountInfo.miniProgram) == null ? void 0 : _a.envVersion) || "";
  } catch (error) {
    return "";
  }
}
function getRuntimeEnv() {
  const miniEnv = getMiniProgramEnv();
  if (miniEnv === "trial")
    return "trial";
  if (miniEnv === "release")
    return "production";
  if (typeof process !== "undefined" && process.env && false) {
    return "production";
  }
  return "development";
}
function getBuildEnvValue(key) {
  if (typeof process === "undefined" || !process.env)
    return "";
  return process.env[`VUE_APP_${key}`] || process.env[`UNI_APP_${key}`] || "";
}
function resolveConfigValue(key) {
  const storageValue = common_vendor.index.getStorageSync(`BOOKFLOW_${key}`);
  const buildValue = getBuildEnvValue(key);
  const envConfig = ENV_CONFIG[getRuntimeEnv()] || ENV_CONFIG.development;
  return trimBaseUrl(storageValue || buildValue || envConfig[key]);
}
const API_BASE_URL = resolveConfigValue("API_BASE_URL");
const IMAGE_BASE_URL = resolveConfigValue("IMAGE_BASE_URL") || API_BASE_URL;
function normalizeImageUrl(value) {
  const url = String(value || "").trim();
  if (!url)
    return "";
  const localUploadMatch = url.match(/^https?:\/\/(?:127\.0\.0\.1|localhost|0\.0\.0\.0)(?::\d+)?(\/uploads\/.*)$/i);
  if (localUploadMatch && IMAGE_BASE_URL) {
    return `${IMAGE_BASE_URL}${localUploadMatch[1]}`;
  }
  if (/^(https?:)?\/\//i.test(url) || url.startsWith("data:") || url.startsWith("blob:")) {
    return url;
  }
  if (url.startsWith("/static/") || url.startsWith("static/")) {
    return url.startsWith("/") ? url : `/${url}`;
  }
  const baseUrl = IMAGE_BASE_URL;
  if (!baseUrl)
    return url;
  return `${baseUrl}${url.startsWith("/") ? "" : "/"}${url}`;
}
exports.API_BASE_URL = API_BASE_URL;
exports.normalizeImageUrl = normalizeImageUrl;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/config.js.map
