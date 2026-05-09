import { get, post } from './request';

export function getCommunityFeed(type) {
  return get('/community/feed', type ? { type } : undefined);
}

export function getCommunityActivity() {
  return get('/community/activity');
}

export function getCommunityPostDetail(postId) {
  return get('/community/post/detail', { postId });
}

export function createCommunityPost(data) {
  return post('/community/post/create', data);
}

export function toggleCommunityLike(postId) {
  return post('/community/post/toggle-like', { postId });
}

export function toggleCommunityFavorite(postId) {
  return post('/community/post/toggle-favorite', { postId });
}

export function getPostComments(postId) {
  return get('/community/post/comment/list', { postId });
}

export function createPostComment(postId, content) {
  return post('/community/post/comment/create', { postId, content });
}

export function reportCommunityPost(data) {
  return post('/community/post/report', data);
}
