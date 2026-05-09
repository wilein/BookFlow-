import { get, post } from './request';

export function toggleFavorite(targetType, targetId) {
  return post('/favorite/toggle', { targetType, targetId });
}

export function getFavoriteStatus(targetType, targetId) {
  return get('/favorite/status', { targetType, targetId });
}
