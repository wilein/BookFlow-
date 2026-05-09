"use strict";
const utils_api_request = require("./request.js");
function getCommunityFeed(type) {
  return utils_api_request.get("/community/feed", type ? { type } : void 0);
}
function getCommunityActivity() {
  return utils_api_request.get("/community/activity");
}
function getCommunityPostDetail(postId) {
  return utils_api_request.get("/community/post/detail", { postId });
}
function createCommunityPost(data) {
  return utils_api_request.post("/community/post/create", data);
}
function toggleCommunityLike(postId) {
  return utils_api_request.post("/community/post/toggle-like", { postId });
}
function toggleCommunityFavorite(postId) {
  return utils_api_request.post("/community/post/toggle-favorite", { postId });
}
function getPostComments(postId) {
  return utils_api_request.get("/community/post/comment/list", { postId });
}
function createPostComment(postId, content) {
  return utils_api_request.post("/community/post/comment/create", { postId, content });
}
function reportCommunityPost(data) {
  return utils_api_request.post("/community/post/report", data);
}
exports.createCommunityPost = createCommunityPost;
exports.createPostComment = createPostComment;
exports.getCommunityActivity = getCommunityActivity;
exports.getCommunityFeed = getCommunityFeed;
exports.getCommunityPostDetail = getCommunityPostDetail;
exports.getPostComments = getPostComments;
exports.reportCommunityPost = reportCommunityPost;
exports.toggleCommunityFavorite = toggleCommunityFavorite;
exports.toggleCommunityLike = toggleCommunityLike;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/community.js.map
