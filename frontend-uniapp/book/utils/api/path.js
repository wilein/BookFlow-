import { getAuthToken } from '../auth';
import { API_BASE_URL, get, post } from './request';

export function getPathList(params = {}) {
  return get('/path/list', params);
}

export function getPathDetail(id) {
  return get('/path/detail', { id });
}

export function savePathDraft(data) {
  return post('/path/save-draft', data);
}

export function publishPath(data) {
  return post('/path/publish', data);
}

export function uploadPathCover(filePath) {
  return new Promise((resolve, reject) => {
    const token = getAuthToken();
    uni.uploadFile({
      url: `${API_BASE_URL}/path/upload-cover`,
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        let body = res.data || {};
        if (typeof body === 'string') {
          try {
            body = JSON.parse(body);
          } catch (error) {
            reject(error);
            return;
          }
        }
        if (String(body.code) === '200') {
          resolve(body.data || {});
          return;
        }
        reject(new Error(body.message || '封面上传失败'));
      },
      fail: reject
    });
  });
}

export function startPathLearning(pathId) {
  return post('/path/progress/start', { pathId });
}

export function cancelPathLearning(pathId) {
  return post('/path/progress/cancel', { pathId });
}

export function getMyLearningPaths() {
  return get('/path/progress/my');
}

export function getCurrentLearningPath() {
  return get('/path/progress/current', undefined, { showError: false });
}

export function completePathNode(pathId, nodeId, completed = true) {
  return post('/path/progress/complete-node', { pathId, nodeId, completed });
}
