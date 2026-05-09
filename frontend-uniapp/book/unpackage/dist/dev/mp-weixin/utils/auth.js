"use strict";
const common_vendor = require("../common/vendor.js");
const TOKEN_KEY = "authToken";
const OPEN_ID_KEY = "openid";
const TOKEN_EXPIRE_KEY = "tokenExpireAt";
const PROFILE_INCOMPLETE_KEY = "profileIncomplete";
const USER_VERIFIED_KEY = "userVerified";
const USER_INFO_KEY = "userInfo";
const REDIRECT_KEY = "loginRedirectUrl";
const LEGACY_TOKEN_KEYS = ["token", "jwtToken"];
const TABBAR_PAGES = [
  "/pages/index/index",
  "/pages/category/category",
  "/pages/publish/create",
  "/pages/community/community",
  "/pages/my/my"
];
function parseBool(value) {
  if (typeof value === "boolean")
    return value;
  if (typeof value === "string")
    return value === "true" || value === "1";
  if (typeof value === "number")
    return value === 1;
  return false;
}
function normalizeUrl(url) {
  if (!url)
    return "";
  return url.startsWith("/") ? url : `/${url}`;
}
function getCurrentPage() {
  const pages = getCurrentPages();
  return pages.length ? pages[pages.length - 1] : null;
}
function getAuthToken() {
  const token = common_vendor.index.getStorageSync(TOKEN_KEY);
  if (token)
    return token;
  for (const key of LEGACY_TOKEN_KEYS) {
    const legacyToken = common_vendor.index.getStorageSync(key);
    if (legacyToken) {
      common_vendor.index.setStorageSync(TOKEN_KEY, legacyToken);
      return legacyToken;
    }
  }
  return "";
}
function getOpenid() {
  return common_vendor.index.getStorageSync(OPEN_ID_KEY) || "";
}
function getTokenExpireAt() {
  const expireAt = Number(common_vendor.index.getStorageSync(TOKEN_EXPIRE_KEY) || 0);
  return Number.isFinite(expireAt) ? expireAt : 0;
}
function hasValidSession() {
  return Boolean(getAuthToken()) && getTokenExpireAt() > Date.now();
}
function isVerified() {
  return parseBool(common_vendor.index.getStorageSync(USER_VERIFIED_KEY));
}
function getUserInfoSnapshot() {
  return common_vendor.index.getStorageSync(USER_INFO_KEY) || {};
}
function syncProfileState(profile = {}) {
  const current = getUserInfoSnapshot();
  const next = { ...current, ...profile };
  common_vendor.index.setStorageSync(USER_INFO_KEY, next);
  if (Object.prototype.hasOwnProperty.call(next, "profileIncomplete")) {
    common_vendor.index.setStorageSync(PROFILE_INCOMPLETE_KEY, Boolean(next.profileIncomplete));
  }
  if (Object.prototype.hasOwnProperty.call(next, "verified")) {
    common_vendor.index.setStorageSync(USER_VERIFIED_KEY, Boolean(next.verified));
  } else if (Object.prototype.hasOwnProperty.call(next, "authStatus")) {
    common_vendor.index.setStorageSync(USER_VERIFIED_KEY, Number(next.authStatus) === 2);
  }
}
function saveSession(session = {}) {
  var _a;
  const token = session.token || "";
  const openid = session.openid || ((_a = session.userInfo) == null ? void 0 : _a.openid) || "";
  const expireAt = Number(session.expireAt || 0) || Date.now() + Number(session.expiresIn || 0) * 1e3;
  if (token)
    common_vendor.index.setStorageSync(TOKEN_KEY, token);
  if (openid)
    common_vendor.index.setStorageSync(OPEN_ID_KEY, openid);
  if (expireAt)
    common_vendor.index.setStorageSync(TOKEN_EXPIRE_KEY, expireAt);
  for (const key of LEGACY_TOKEN_KEYS) {
    common_vendor.index.removeStorageSync(key);
  }
  if (session.userInfo) {
    syncProfileState(session.userInfo);
  }
}
function syncSessionFromHeaders(headers = {}) {
  const token = headers["x-access-token"] || headers["X-Access-Token"];
  const expireAt = headers["x-token-expire-at"] || headers["X-Token-Expire-At"];
  if (!token && !expireAt)
    return;
  saveSession({
    token: token || getAuthToken(),
    openid: getOpenid(),
    expireAt: Number(expireAt || getTokenExpireAt() || 0)
  });
}
function clearSession() {
  common_vendor.index.removeStorageSync(TOKEN_KEY);
  common_vendor.index.removeStorageSync(OPEN_ID_KEY);
  common_vendor.index.removeStorageSync(TOKEN_EXPIRE_KEY);
  common_vendor.index.removeStorageSync(PROFILE_INCOMPLETE_KEY);
  common_vendor.index.removeStorageSync(USER_VERIFIED_KEY);
  common_vendor.index.removeStorageSync(USER_INFO_KEY);
  for (const key of LEGACY_TOKEN_KEYS) {
    common_vendor.index.removeStorageSync(key);
  }
}
function getCurrentPageUrl() {
  const page = getCurrentPage();
  if (!page || !page.route)
    return "";
  const baseUrl = normalizeUrl(page.route);
  const options = page.options || {};
  const query = Object.keys(options).filter((key) => options[key] !== void 0 && options[key] !== null && options[key] !== "").map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`).join("&");
  return query ? `${baseUrl}?${query}` : baseUrl;
}
function rememberRedirectUrl(url) {
  const target = normalizeUrl(url);
  if (!target || target === "/pages/login/login")
    return;
  common_vendor.index.setStorageSync(REDIRECT_KEY, target);
}
function consumeRedirectUrl() {
  const url = common_vendor.index.getStorageSync(REDIRECT_KEY) || "";
  common_vendor.index.removeStorageSync(REDIRECT_KEY);
  return url;
}
function redirectToLogin(redirectUrl = "") {
  const currentUrl = normalizeUrl(getCurrentPageUrl());
  if (currentUrl === "/pages/login/login")
    return;
  rememberRedirectUrl(redirectUrl || currentUrl);
  common_vendor.index.reLaunch({ url: "/pages/login/login" });
}
function navigateAfterLogin(targetUrl = "") {
  const finalUrl = normalizeUrl(targetUrl || consumeRedirectUrl() || "/pages/index/index");
  const pagePath = finalUrl.split("?")[0];
  if (TABBAR_PAGES.includes(pagePath)) {
    common_vendor.index.switchTab({ url: pagePath });
    return;
  }
  common_vendor.index.reLaunch({ url: finalUrl });
}
function ensureLoggedIn(redirectUrl = "") {
  if (hasValidSession())
    return true;
  clearSession();
  redirectToLogin(redirectUrl);
  return false;
}
exports.clearSession = clearSession;
exports.ensureLoggedIn = ensureLoggedIn;
exports.getAuthToken = getAuthToken;
exports.getCurrentPageUrl = getCurrentPageUrl;
exports.hasValidSession = hasValidSession;
exports.isVerified = isVerified;
exports.navigateAfterLogin = navigateAfterLogin;
exports.redirectToLogin = redirectToLogin;
exports.saveSession = saveSession;
exports.syncProfileState = syncProfileState;
exports.syncSessionFromHeaders = syncSessionFromHeaders;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/auth.js.map
