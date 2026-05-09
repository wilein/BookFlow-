import { API_BASE_URL, get, post } from './request';
import { clearSession, getAuthToken, redirectToLogin, syncSessionFromHeaders } from '../auth';

const TEXTS = {
  uploadParseFail: '上传响应解析失败',
  uploadFail: '图片上传失败'
};

export function getBookList() {
  return get('/book/list');
}

export function getBooksByCategory() {
  return get('/book/category');
}

export function searchBooks(params = {}) {
  return get('/book/search', params);
}

export function getBookDetail(id) {
  return get('/book/detail', { id });
}

export function publishBook(data) {
  return post('/book/publish', data);
}

export function uploadBookImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    uni.uploadFile({
      url: `${API_BASE_URL}/book/upload-image`,
      filePath,
      name: 'file',
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
