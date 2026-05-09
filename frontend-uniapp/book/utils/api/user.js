import { API_BASE_URL, get, post } from './request';
import { clearSession, getAuthToken, redirectToLogin, saveSession, syncProfileState, syncSessionFromHeaders } from '../auth';

const TEXTS = {
  uploadParseFail: '上传响应解析失败',
  uploadFail: '上传失败'
};

export function loginWithWechat(code) {
  return post(`/user/auth/wechat?code=${encodeURIComponent(code)}`, {}, { withAuth: false, redirectOn401: false }).then((data) => {
    saveSession(data || {});
    return data;
  });
}

export function loginWithDev() {
  return post('/user/auth/dev-login', {}, { withAuth: false, redirectOn401: false }).then((data) => {
    saveSession(data || {});
    return data;
  });
}

export function checkAuthSession() {
  return get('/user/auth/check').then((data) => {
    syncProfileState(data?.userInfo || {});
    return data;
  });
}

export function logoutAuth() {
  return post('/user/auth/logout');
}

export function getUserProfile() {
  return get('/user/profile').then((data) => {
    syncProfileState(data || {});
    return data;
  });
}

export function getUserStats() {
  return get('/user/stats');
}

export function updateUserProfile(data) {
  return post('/user/profile/update', data).then((result) => {
    syncProfileState(result || {});
    return result;
  });
}

export function verifyStudent(data) {
  return post('/user/profile/verify-student', data).then((result) => {
    syncProfileState(result || {});
    return result;
  });
}

export function uploadProfileImage(filePath, type) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    uni.uploadFile({
      url: `${API_BASE_URL}/user/profile/upload-image`,
      filePath,
      name: 'file',
      formData: { type },
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data || {});
        } catch (error) {
          uni.showToast({ title: TEXTS.uploadParseFail, icon: 'none' });
          reject(error);
          return;
        }

        if (String(body.code) === '200') {
          resolve(body.data || {});
          return;
        }

        const message = body.message || TEXTS.uploadFail;
        if (String(body.code) === '401') {
          clearSession();
          redirectToLogin();
        }
        uni.showToast({ title: message, icon: 'none' });
        reject(new Error(message));
      },
      fail: (error) => {
        uni.showToast({ title: TEXTS.uploadFail, icon: 'none' });
        reject(error);
      }
    });
  });
}

export function downloadStudentCardImage(url) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    if (!url || !token) {
      reject(new Error('学生证图片不可用'));
      return;
    }
    uni.downloadFile({
      url: `${API_BASE_URL}/user/profile/student-card/view?url=${encodeURIComponent(url)}`,
      header: { Authorization: `Bearer ${token}` },
      success: (res) => {
        syncSessionFromHeaders(res.header || {});
        if (res.statusCode === 200 && res.tempFilePath) {
          resolve(res.tempFilePath);
          return;
        }
        reject(new Error('学生证图片加载失败'));
      },
      fail: reject
    });
  });
}

export function getMyBookshelf(status = 'selling') {
  return get('/user/bookshelf', { status });
}

export function getMyOrders(status = 'all', role = 'buyer') {
  return get('/user/orders', { status, role });
}

export function getMyFavorites(type = 'book') {
  return get('/user/favorites', { type });
}

export function getMyAnnotations() {
  return get('/user/annotations');
}

export function getMyPaths() {
  return get('/user/paths');
}

export function getAddressList() {
  return get('/user/address/list');
}

export function saveAddress(data) {
  return post('/user/address/save', data);
}

export function deleteAddress(id) {
  return post('/user/address/delete', { id });
}

export function setDefaultAddress(id) {
  return post('/user/address/set-default', { id });
}

export function getBrowseHistory() {
  return get('/user/history/list');
}

export function recordBrowseHistory(data) {
  return post('/user/history/record', data, { showError: false });
}

export function deleteBrowseHistory(id) {
  return post('/user/history/delete', { id });
}

export function clearBrowseHistory() {
  return post('/user/history/clear', {});
}

export function submitFeedback(data) {
  return post('/user/feedback/submit', data);
}

export function getNotifications() {
  return get('/user/notifications');
}

export function readNotification(id) {
  return post('/user/notification/read', { id });
}
