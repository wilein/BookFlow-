<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ texts.title }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="post-card">
      <view class="post-head">
        <image class="post-avatar" :src="post.avatar || '/static/logo.png'" mode="aspectFill"></image>
        <view class="post-user">
          <text class="post-author">{{ post.author || texts.defaultUser }}</text>
          <text class="post-meta">{{ formatMeta(post.school, post.time) }}</text>
        </view>
        <text class="post-type">{{ getPostTypeLabel(post.type) }}</text>
      </view>
      <text class="post-title">{{ post.title || postTitle }}</text>
      <text class="post-content">{{ post.content || texts.emptyPostContent }}</text>
      <view v-if="post.sharedPath" class="shared-path" @click="openSharedPath(post.sharedPath)">
        <text class="shared-label">{{ texts.sharedPath }}</text>
        <text class="shared-title">{{ post.sharedPath.title }}</text>
        <text class="shared-meta">{{ post.sharedPath.difficulty }} · {{ post.sharedPath.totalDuration }} · {{ post.sharedPath.nodeCount || 0 }}{{ texts.nodeUnit }}</text>
      </view>
      <view class="post-actions">
        <view class="action-item" @click="toggleLike">
          <text class="action-text" :class="{ active: post.liked }">{{ texts.like }} {{ post.likes || 0 }}</text>
        </view>
        <view class="action-item">
          <text class="action-text">{{ texts.reply }} {{ post.comments || comments.length || 0 }}</text>
        </view>
        <view class="action-item" @click="toggleFavorite">
          <text class="action-text" :class="{ active: post.favorited }">{{ texts.favorite }} {{ post.favoriteCount || 0 }}</text>
        </view>
      </view>
    </view>

    <view class="content">
      <view class="reply-head">
        <text class="reply-title">{{ texts.replies }}</text>
        <text class="reply-count">{{ comments.length }}</text>
      </view>
      <view v-if="comments.length" class="list">
        <view v-for="item in comments" :key="item.id" class="comment-card">
          <image class="avatar" :src="item.avatar || '/static/logo.png'" mode="aspectFill"></image>
          <view class="comment-main">
            <view class="comment-head">
              <text class="name">{{ item.nickname || item.displayName || texts.defaultUser }}</text>
              <text class="time">{{ item.createTime }}</text>
            </view>
            <text class="comment-content">{{ item.content }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>

    <view class="bottom-bar">
      <input class="input" v-model="draft" :placeholder="texts.placeholder" />
      <view class="submit-btn" @click="submitComment">{{ texts.send }}</view>
    </view>
  </view>
</template>

<script>
import {
  createPostComment,
  getCommunityPostDetail,
  getPostComments,
  toggleCommunityFavorite,
  toggleCommunityLike
} from '../../utils/api/community';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TEXTS = {
  title: '\u5e16\u5b50\u8be6\u60c5',
  defaultPostTitle: '\u5e16\u5b50\u8be6\u60c5',
  emptyPostContent: '\u6682\u65e0\u6b63\u6587\u5185\u5bb9',
  empty: '\u8fd8\u6ca1\u6709\u56de\u590d',
  placeholder: '\u53d1\u8868\u4f60\u7684\u56de\u590d...',
  send: '\u53d1\u9001',
  needContent: '\u8bf7\u8f93\u5165\u56de\u590d\u5185\u5bb9',
  defaultUser: '\u6821\u56ed\u4e66\u53cb',
  recommend: '\u63a8\u8350',
  review: '\u4e66\u8bc4',
  qa: '\u95ee\u7b54',
  path: '\u5b66\u4e60\u8def\u5f84',
  like: '\u70b9\u8d5e',
  reply: '\u56de\u590d',
  replies: '\u5168\u90e8\u56de\u590d',
  favorite: '\u6536\u85cf',
  sharedPath: '\u5173\u8054\u8def\u5f84',
  nodeUnit: '\u4e2a\u8282\u70b9'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      postId: '',
      postTitle: '',
      post: {},
      comments: [],
      draft: ''
    };
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.postId = options.postId || '';
    this.postTitle = decodeURIComponent(options.postTitle || TEXTS.defaultPostTitle);
    this.post = {
      title: this.postTitle
    };
    this.fetchPostDetail();
    this.fetchComments();
  },
  methods: {
    formatMeta(school, time) {
      return [school, time].filter(Boolean).join(' / ');
    },
    getPostTypeLabel(type) {
      const map = {
        recommend: TEXTS.recommend,
        review: TEXTS.review,
        qa: TEXTS.qa,
        path: TEXTS.path
      };
      return map[type] || TEXTS.recommend;
    },
    async fetchPostDetail() {
      if (!this.postId) return;
      try {
        const data = await getCommunityPostDetail(this.postId);
        this.post = data || this.post;
        this.postTitle = this.post.title || this.postTitle;
      } catch (error) {
        console.error('getCommunityPostDetail failed', error);
      }
    },
    async fetchComments() {
      if (!this.postId) return;
      try {
        this.comments = (await getPostComments(this.postId)) || [];
        this.post = { ...this.post, comments: this.comments.length };
      } catch (error) {
        console.error('getPostComments failed', error);
      }
    },
    async toggleLike() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await toggleCommunityLike(this.postId);
        this.post = {
          ...this.post,
          liked: Boolean(data?.liked),
          likes: Number(data?.likeCount || 0)
        };
      } catch (error) {
        console.error('toggleCommunityLike failed', error);
      }
    },
    async toggleFavorite() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await toggleCommunityFavorite(this.postId);
        this.post = {
          ...this.post,
          favorited: Boolean(data?.favorited),
          favoriteCount: Number(data?.favoriteCount || 0)
        };
      } catch (error) {
        console.error('toggleCommunityFavorite failed', error);
      }
    },
    openSharedPath(path) {
      if (!path || !path.id) return;
      uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(path.id)}` });
    },
    async submitComment() {
      if (!this.draft.trim()) {
        uni.showToast({ title: TEXTS.needContent, icon: 'none' });
        return;
      }
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await createPostComment(this.postId, this.draft);
        this.draft = '';
        if (data && data.commentCount !== undefined) {
          this.post = { ...this.post, comments: Number(data.commentCount || 0) };
        }
        this.fetchComments();
      } catch (error) {
        console.error('createPostComment failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/community/community' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; padding-bottom: calc(120rpx + env(safe-area-inset-bottom)); }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.post-card { margin: 18rpx 20rpx 0; background: #fff; border-radius: 22rpx; padding: 22rpx; }
.post-head { display: flex; align-items: flex-start; gap: 14rpx; }
.post-avatar { width: 76rpx; height: 76rpx; border-radius: 50%; background: #e8edf3; flex-shrink: 0; }
.post-user { flex: 1; min-width: 0; }
.post-author { display: block; font-size: 27rpx; color: #243548; font-weight: 700; }
.post-meta { display: block; margin-top: 6rpx; font-size: 22rpx; color: #7d8fa2; }
.post-type { flex-shrink: 0; padding: 8rpx 14rpx; border-radius: 999rpx; background: #edf3ff; color: #1f5eff; font-size: 21rpx; }
.post-title { display: block; margin-top: 18rpx; font-size: 34rpx; line-height: 1.5; color: #243548; font-weight: 700; }
.post-content { display: block; margin-top: 14rpx; font-size: 26rpx; line-height: 1.8; color: #51667d; }
.shared-path { margin-top: 16rpx; border-radius: 18rpx; background: #f4f7fb; padding: 18rpx; border: 1rpx solid #e6edf5; }
.shared-label { display: block; font-size: 22rpx; color: #1f5eff; font-weight: 700; }
.shared-title { display: block; margin-top: 8rpx; font-size: 27rpx; color: #26384d; font-weight: 700; line-height: 1.4; }
.shared-meta { display: block; margin-top: 8rpx; font-size: 22rpx; color: #76899e; }
.post-actions { display: flex; gap: 26rpx; margin-top: 20rpx; }
.action-text { font-size: 24rpx; color: #6d8095; }
.action-text.active { color: #1f5eff; font-weight: 700; }
.content { padding: 16rpx 20rpx 0; }
.reply-head { margin-bottom: 14rpx; display: flex; align-items: center; justify-content: space-between; }
.reply-title { font-size: 30rpx; color: #243548; font-weight: 700; }
.reply-count { min-width: 44rpx; height: 44rpx; border-radius: 22rpx; background: #e8efff; color: #1f5eff; display: flex; align-items: center; justify-content: center; font-size: 22rpx; font-weight: 700; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.comment-card { display: flex; gap: 14rpx; background: #fff; border-radius: 20rpx; padding: 18rpx; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #e8edf3; flex-shrink: 0; }
.comment-main { flex: 1; }
.comment-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.name { font-size: 26rpx; color: #243548; font-weight: 700; }
.time { font-size: 22rpx; color: #7d8fa2; }
.comment-content { display: block; margin-top: 10rpx; font-size: 24rpx; color: #51667d; line-height: 1.6; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; display: flex; align-items: center; gap: 14rpx; background: rgba(255,255,255,0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.input { flex: 1; height: 80rpx; line-height: 80rpx; border-radius: 18rpx; background: #f1f5fa; padding: 0 20rpx; font-size: 26rpx; color: #2d3d52; }
.submit-btn { width: 132rpx; height: 80rpx; border-radius: 18rpx; background: #1f5eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 700; }
</style>
