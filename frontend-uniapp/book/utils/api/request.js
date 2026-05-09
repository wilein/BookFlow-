import { clearSession, getAuthToken, redirectToLogin, syncSessionFromHeaders } from '../auth';
import { API_BASE_URL, IMAGE_BASE_URL, normalizeImageUrl } from '../config';

const TEXTS = {
  requestFailed: '\u8bf7\u6c42\u5931\u8d25',
  networkError: '\u7f51\u7edc\u9519\u8bef'
};

export { API_BASE_URL, IMAGE_BASE_URL, normalizeImageUrl };

function request({ url, method = 'GET', data, header = {}, showError = true, withAuth = true, redirectOn401 = true }) {
  return new Promise((resolve, reject) => {
    const token = withAuth ? getAuthToken() : '';
    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data: sanitizeRequestData(data),
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header
      },
      success: (res) => {
        syncSessionFromHeaders(res.header || {});
        const body = res.data || {};
        if (String(body.code) === '200') {
          resolve(normalizeUploadUrls(body.data));
          return;
        }
        const message = body.message || TEXTS.requestFailed;
        if (String(body.code) === '401') {
          clearSession();
          if (redirectOn401) {
            redirectToLogin();
          }
        }
        if (showError) {
          uni.showToast({ title: message, icon: 'none' });
        }
        reject(new Error(message));
      },
      fail: (error) => {
        if (showError) {
          uni.showToast({ title: TEXTS.networkError, icon: 'none' });
        }
        reject(error);
      }
    });
  });
}

export function get(url, data, options = {}) {
  return request({ url, method: 'GET', data, ...options });
}

export function post(url, data, options = {}) {
  return request({ url, method: 'POST', data, ...options });
}

function normalizeUploadUrls(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeUploadUrls);
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).reduce((result, key) => {
      result[key] = normalizeUploadUrls(value[key]);
      return result;
    }, {});
  }
  if (typeof value === 'string') {
    const text = value.trim();
    if (text.startsWith('/uploads/') || text.startsWith('uploads/')) {
      return normalizeImageUrl(text);
    }
  }
  return value;
}

function sanitizeRequestData(value) {
  if (Array.isArray(value)) {
    return value.map(sanitizeRequestData).filter((item) => item !== undefined);
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).reduce((result, key) => {
      const item = sanitizeRequestData(value[key]);
      if (item !== undefined) {
        result[key] = item;
      }
      return result;
    }, {});
  }
  if (value === undefined || value === null) {
    return undefined;
  }
  return value;
}

export default request;
