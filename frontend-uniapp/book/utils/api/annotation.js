import { API_BASE_URL, get, post } from './request';
import { clearSession, getAuthToken, redirectToLogin, syncSessionFromHeaders } from '../auth';

export function getAnnotationList(bookId, options = {}) {
  const params = { bookId };
  if (options.mineOnly) {
    params.mineOnly = 1;
  }
  return get('/annotation/list', params, {
    showError: options.showError !== false
  });
}

export function createAnnotation(data) {
  return post('/annotation/create', data);
}

export function toggleAnnotationLike(annotationId) {
  return post('/annotation/toggle-like', { annotationId });
}

export function uploadAnnotationImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    uni.uploadFile({
      url: `${API_BASE_URL}/annotation/upload-image`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data || {});
        } catch (error) {
          uni.showToast({ title: '图片上传失败', icon: 'none' });
          reject(error);
          return;
        }
        if (String(body.code) === '200') {
          resolve(body.data || {});
          return;
        }
        const message = body.message || '图片上传失败';
        if (String(body.code) === '401') {
          clearSession();
          redirectToLogin();
        }
        uni.showToast({ title: message, icon: 'none' });
        reject(new Error(message));
      },
      fail: (error) => {
        uni.showToast({ title: '图片上传失败', icon: 'none' });
        reject(error);
      }
    });
  });
}
