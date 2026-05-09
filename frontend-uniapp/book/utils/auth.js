const TOKEN_KEY = 'authToken';
const OPEN_ID_KEY = 'openid';
const TOKEN_EXPIRE_KEY = 'tokenExpireAt';
const PROFILE_INCOMPLETE_KEY = 'profileIncomplete';
const USER_VERIFIED_KEY = 'userVerified';
const USER_INFO_KEY = 'userInfo';
const REDIRECT_KEY = 'loginRedirectUrl';

const LEGACY_TOKEN_KEYS = ['token', 'jwtToken'];
const TABBAR_PAGES = [
  '/pages/index/index',
  '/pages/category/category',
  '/pages/publish/create',
  '/pages/community/community',
  '/pages/my/my'
];

function parseBool(value) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') return value === 'true' || value === '1';
  if (typeof value === 'number') return value === 1;
  return false;
}

function normalizeUrl(url) {
  if (!url) return '';
  return url.startsWith('/') ? url : `/${url}`;
}

function getCurrentPage() {
  const pages = getCurrentPages();
  return pages.length ? pages[pages.length - 1] : null;
}

export function getAuthToken() {
  const token = uni.getStorageSync(TOKEN_KEY);
  if (token) return token;
  for (const key of LEGACY_TOKEN_KEYS) {
    const legacyToken = uni.getStorageSync(key);
    if (legacyToken) {
      uni.setStorageSync(TOKEN_KEY, legacyToken);
      return legacyToken;
    }
  }
  return '';
}

export function getOpenid() {
  return uni.getStorageSync(OPEN_ID_KEY) || '';
}

export function getTokenExpireAt() {
  const expireAt = Number(uni.getStorageSync(TOKEN_EXPIRE_KEY) || 0);
  return Number.isFinite(expireAt) ? expireAt : 0;
}

export function hasValidSession() {
  return Boolean(getAuthToken()) && getTokenExpireAt() > Date.now();
}

export function isProfileIncomplete() {
  return parseBool(uni.getStorageSync(PROFILE_INCOMPLETE_KEY));
}

export function isVerified() {
  return parseBool(uni.getStorageSync(USER_VERIFIED_KEY));
}

export function getUserInfoSnapshot() {
  return uni.getStorageSync(USER_INFO_KEY) || {};
}

export function syncProfileState(profile = {}) {
  const current = getUserInfoSnapshot();
  const next = { ...current, ...profile };
  uni.setStorageSync(USER_INFO_KEY, next);
  if (Object.prototype.hasOwnProperty.call(next, 'profileIncomplete')) {
    uni.setStorageSync(PROFILE_INCOMPLETE_KEY, Boolean(next.profileIncomplete));
  }
  if (Object.prototype.hasOwnProperty.call(next, 'verified')) {
    uni.setStorageSync(USER_VERIFIED_KEY, Boolean(next.verified));
  } else if (Object.prototype.hasOwnProperty.call(next, 'authStatus')) {
    uni.setStorageSync(USER_VERIFIED_KEY, Number(next.authStatus) === 2);
  }
}

export function saveSession(session = {}) {
  const token = session.token || '';
  const openid = session.openid || session.userInfo?.openid || '';
  const expireAt = Number(session.expireAt || 0) || Date.now() + Number(session.expiresIn || 0) * 1000;
  if (token) uni.setStorageSync(TOKEN_KEY, token);
  if (openid) uni.setStorageSync(OPEN_ID_KEY, openid);
  if (expireAt) uni.setStorageSync(TOKEN_EXPIRE_KEY, expireAt);
  for (const key of LEGACY_TOKEN_KEYS) {
    uni.removeStorageSync(key);
  }
  if (session.userInfo) {
    syncProfileState(session.userInfo);
  }
}

export function syncSessionFromHeaders(headers = {}) {
  const token = headers['x-access-token'] || headers['X-Access-Token'];
  const expireAt = headers['x-token-expire-at'] || headers['X-Token-Expire-At'];
  if (!token && !expireAt) return;
  saveSession({
    token: token || getAuthToken(),
    openid: getOpenid(),
    expireAt: Number(expireAt || getTokenExpireAt() || 0)
  });
}

export function clearSession() {
  uni.removeStorageSync(TOKEN_KEY);
  uni.removeStorageSync(OPEN_ID_KEY);
  uni.removeStorageSync(TOKEN_EXPIRE_KEY);
  uni.removeStorageSync(PROFILE_INCOMPLETE_KEY);
  uni.removeStorageSync(USER_VERIFIED_KEY);
  uni.removeStorageSync(USER_INFO_KEY);
  for (const key of LEGACY_TOKEN_KEYS) {
    uni.removeStorageSync(key);
  }
}

export function getCurrentPageUrl() {
  const page = getCurrentPage();
  if (!page || !page.route) return '';
  const baseUrl = normalizeUrl(page.route);
  const options = page.options || {};
  const query = Object.keys(options)
    .filter((key) => options[key] !== undefined && options[key] !== null && options[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
    .join('&');
  return query ? `${baseUrl}?${query}` : baseUrl;
}

export function rememberRedirectUrl(url) {
  const target = normalizeUrl(url);
  if (!target || target === '/pages/login/login') return;
  uni.setStorageSync(REDIRECT_KEY, target);
}

export function consumeRedirectUrl() {
  const url = uni.getStorageSync(REDIRECT_KEY) || '';
  uni.removeStorageSync(REDIRECT_KEY);
  return url;
}

export function redirectToLogin(redirectUrl = '') {
  const currentUrl = normalizeUrl(getCurrentPageUrl());
  if (currentUrl === '/pages/login/login') return;
  rememberRedirectUrl(redirectUrl || currentUrl);
  uni.reLaunch({ url: '/pages/login/login' });
}

export function navigateAfterLogin(targetUrl = '') {
  const finalUrl = normalizeUrl(targetUrl || consumeRedirectUrl() || '/pages/index/index');
  const pagePath = finalUrl.split('?')[0];
  if (TABBAR_PAGES.includes(pagePath)) {
    uni.switchTab({ url: pagePath });
    return;
  }
  uni.reLaunch({ url: finalUrl });
}

export function ensureLoggedIn(redirectUrl = '') {
  if (hasValidSession()) return true;
  clearSession();
  redirectToLogin(redirectUrl);
  return false;
}
