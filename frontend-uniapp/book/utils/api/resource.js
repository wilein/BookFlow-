import { API_BASE_URL, get, post } from './request';
import { clearSession, getAuthToken, redirectToLogin, syncSessionFromHeaders } from '../auth';

export function getMyResources() {
  return get('/resource/my-list');
}

export function getResourceDetail(id) {
  return get('/resource/detail', { id });
}

export function getResourceList(params = {}) {
  return get('/resource/list', params);
}

export function createResource(data) {
  return post('/resource/create', data);
}

export function updateResource(data) {
  return post('/resource/update', data);
}

export function uploadResourceFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    uni.uploadFile({
      url: `${API_BASE_URL}/resource/upload-file`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        syncSessionFromHeaders(res.header || {});
        let body = {};
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data || {});
        } catch (error) {
          uni.showToast({ title: '资源上传失败', icon: 'none' });
          reject(error);
          return;
        }
        if (String(body.code) === '200') {
          resolve(body.data || {});
          return;
        }
        const message = body.message || '资源上传失败';
        if (String(body.code) === '401') {
          clearSession();
          redirectToLogin();
        }
        uni.showToast({ title: message, icon: 'none' });
        reject(new Error(message));
      },
      fail: (error) => {
        uni.showToast({ title: '资源上传失败', icon: 'none' });
        reject(error);
      }
    });
  });
}
