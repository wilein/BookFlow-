<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightPadding + 'px' }"
    >
      <view class="header-main">
        <text class="header-title">{{ texts.communitySquare }}</text>
        <text class="header-sub">{{ texts.headerSub }}</text>
      </view>
      <view class="publish-btn" @click="handlePublish">+</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="top-tabs">
      <view
        v-for="item in topTabs"
        :key="item.value"
        class="top-tab"
        :class="{ active: activeTopTab === item.value }"
        @click="switchTopTab(item.value)"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="activeTopTab === 'community'" class="sub-tabs-wrap">
      <scroll-view class="sub-tabs" scroll-x>
        <view class="sub-tabs-row">
          <view
            v-for="item in communityTabs"
            :key="item.value"
            class="sub-tab"
            :class="{ active: activeCommunityTab === item.value }"
            @click="switchCommunityTab(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="content">
      <view v-if="activeTopTab === 'community'" class="feed-list">
        <view v-if="activeCommunityTab === 'path'" class="path-feed">
          <view v-if="pathLoading" class="state">{{ texts.loading }}</view>
          <view v-else-if="pathError" class="state">
            <text class="state-text">{{ pathError }}</text>
            <view class="retry-btn" @click="fetchPublicPaths">{{ texts.retry }}</view>
          </view>
          <view v-else-if="publicPaths.length">
            <view class="path-card" v-for="path in publicPaths" :key="path.id" @click="openLearningPath(path)">
              <image v-if="path.coverImage || path.cover" class="path-card-cover" :src="path.coverImage || path.cover" mode="aspectFill"></image>
              <view class="path-card-head">
                <text class="path-card-title">{{ path.title }}</text>
                <text class="path-card-category">{{ path.category || texts.path }}</text>
              </view>
              <text class="path-card-desc">{{ path.description || texts.emptyPathDesc }}</text>
              <view class="path-card-meta">
                <text>{{ path.creator || texts.defaultUser }}</text>
                <text>{{ path.difficulty || texts.defaultDifficulty }}</text>
                <text>{{ path.nodeCount || 0 }}{{ texts.nodeUnit }}</text>
                <text>{{ path.learnerCount || path.learners || 0 }}{{ texts.learnersUnit }}</text>
              </view>
            </view>
          </view>
          <view v-else class="empty">{{ texts.emptyPath }}</view>
        </view>

        <view v-else-if="feedLoading" class="state">{{ texts.loading }}</view>
        <view v-else-if="feedError" class="state">
          <text class="state-text">{{ feedError }}</text>
          <view class="retry-btn" @click="fetchCommunityFeeds">{{ texts.retry }}</view>
        </view>
        <view v-else-if="filteredCommunityPosts.length">
          <view class="post-card" v-for="post in filteredCommunityPosts" :key="post.id" @click="openPost(post)">
            <view class="post-head">
              <image class="avatar" :src="post.avatar || '/static/logo.png'" mode="aspectFill"></image>
              <view class="post-user">
                <text class="user-name">{{ post.author || texts.defaultUser }}</text>
                <text class="user-meta">{{ formatMeta(post.school, post.time) }}</text>
              </view>
            </view>
            <view class="post-title-row">
              <text class="post-title">{{ post.title }}</text>
              <text class="type-label">{{ getPostTypeLabel(post.type) }}</text>
            </view>
            <text class="post-content">{{ post.content }}</text>
            <view v-if="post.sharedPath" class="shared-path" @click.stop="openSharedPath(post.sharedPath)">
              <image v-if="post.sharedPath.coverImage || post.sharedPath.cover" class="shared-cover" :src="post.sharedPath.coverImage || post.sharedPath.cover" mode="aspectFill"></image>
              <text class="shared-label">{{ texts.sharedPath }}</text>
              <text class="shared-title">{{ post.sharedPath.title }}</text>
              <text class="shared-meta">{{ post.sharedPath.difficulty }} · {{ post.sharedPath.totalDuration }} · {{ post.sharedPath.nodeCount || 0 }}{{ texts.nodeUnit }}</text>
            </view>
            <view class="tags-row" v-if="(post.tags || []).length">
              <text class="tag" v-for="tag in post.tags || []" :key="tag">{{ tag }}</text>
            </view>
            <view class="post-actions">
              <view class="action-item" @click.stop="toggleLike(post)">
                <text class="action-text" :class="{ active: post.liked }">{{ texts.like }} {{ post.likes || 0 }}</text>
              </view>
              <view class="action-item" @click.stop="openComments(post)">
                <text class="action-text">{{ texts.comment }} {{ post.comments || 0 }}</text>
              </view>
              <view class="action-item" @click.stop="toggleFavorite(post)">
                <text class="action-text" :class="{ active: post.favorited }">{{ texts.favorite }} {{ post.favoriteCount || 0 }}</text>
              </view>
              <view class="action-item" @click.stop="reportPost(post)">
                <text class="action-text">{{ texts.report }}</text>
              </view>
            </view>
          </view>
        </view>
        <view v-else class="empty">{{ texts.emptyCommunity }}</view>
      </view>

      <view v-else-if="activeTopTab === 'activity'" class="activity-list">
        <view v-if="activityLoading" class="state">{{ texts.loading }}</view>
        <view v-else-if="activityError" class="state">
          <text class="state-text">{{ activityError }}</text>
          <view class="retry-btn" @click="fetchActivities">{{ texts.retry }}</view>
        </view>
        <block v-else>
        <view class="section-card summary-card">
          <text class="section-title">{{ texts.recentActivity }}</text>
          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-num">{{ communityPosts.length }}</text>
              <text class="summary-label">{{ texts.postCount }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-num">{{ activityList.length }}</text>
              <text class="summary-label">{{ texts.activityCount }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-num">{{ chatSessions.length }}</text>
              <text class="summary-label">{{ texts.sessionCount }}</text>
            </view>
          </view>
        </view>

        <view v-if="activityList.length">
          <view class="activity-card" v-for="item in activityList" :key="item.id">
            <view class="activity-dot"></view>
            <view class="activity-main">
              <text class="activity-title">{{ item.title }}</text>
              <text class="activity-desc">{{ item.desc }}</text>
              <text class="activity-time">{{ item.time }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty">{{ texts.emptyActivity }}</view>
        </block>
      </view>

      <view v-else class="chat-list">
        <view v-if="!isLoggedIn" class="state">
          <text class="state-text">{{ texts.needLoginChat }}</text>
          <view class="retry-btn" @click="goLoginForChat">{{ texts.goLogin }}</view>
        </view>
        <view v-else-if="chatLoading" class="state">{{ texts.loading }}</view>
        <view v-else-if="chatError" class="state">
          <text class="state-text">{{ chatError }}</text>
          <view class="retry-btn" @click="fetchChats">{{ texts.retry }}</view>
        </view>
        <view v-else-if="chatSessions.length">
          <view class="chat-card" v-for="item in chatSessions" :key="item.id" @click="openChat(item)">
            <image class="avatar" :src="item.avatar || '/static/logo.png'" mode="aspectFill"></image>
            <view class="chat-main">
              <view class="chat-top">
                <text class="chat-name">{{ item.name || texts.defaultSeller }}</text>
                <text class="chat-time">{{ item.time }}</text>
              </view>
              <text class="chat-book">{{ texts.aboutBook }}{{ item.bookTitle || texts.unknownBook }}{{ texts.bookSuffix }}</text>
              <text class="chat-preview">{{ item.preview }}</text>
            </view>
            <view v-if="item.unread > 0" class="badge">{{ item.unread }}</view>
          </view>
        </view>
        <view v-else class="empty">{{ texts.emptyChat }}</view>
      </view>

      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { getChatSessions } from '../../utils/api/chat';
import {
  getCommunityActivity,
  getCommunityFeed,
  toggleCommunityFavorite,
  toggleCommunityLike
} from '../../utils/api/community';
import { getPathList } from '../../utils/api/path';
import { ensureLoggedIn, getCurrentPageUrl, hasValidSession } from '../../utils/auth';

const TEXTS = {
  communitySquare: '\u793e\u533a\u5e7f\u573a',
  headerSub: '\u6821\u56ed\u4e66\u7c4d\u4ea4\u6613\u3001\u4ea4\u6d41\u4e0e\u5b66\u4e60\u52a8\u6001',
  community: '\u793e\u533a',
  activity: '\u52a8\u6001',
  chat: '\u804a\u5929',
  recommend: '\u63a8\u8350',
  review: '\u4e66\u8bc4',
  qa: '\u95ee\u7b54',
  path: '\u5b66\u4e60\u8def\u5f84',
  like: '\u70b9\u8d5e',
  comment: '\u56de\u590d',
  favorite: '\u6536\u85cf',
  report: '\u4e3e\u62a5',
  sharedPath: '\u5173\u8054\u8def\u5f84',
  nodeUnit: '\u4e2a\u8282\u70b9',
  learnersUnit: '\u4eba\u5728\u5b66',
  emptyCommunity: '\u6682\u65e0\u793e\u533a\u5185\u5bb9',
  emptyPath: '\u6682\u65e0\u53d1\u5e03\u7684\u5b66\u4e60\u8def\u5f84',
  emptyPathDesc: '\u6682\u65e0\u8def\u5f84\u8bf4\u660e',
  defaultDifficulty: '\u5165\u95e8',
  recentActivity: '\u6700\u8fd1\u52a8\u6001',
  postCount: '\u5e16\u5b50',
  activityCount: '\u52a8\u6001',
  sessionCount: '\u4f1a\u8bdd',
  emptyActivity: '\u6682\u65e0\u52a8\u6001',
  emptyChat: '\u6682\u65e0\u804a\u5929\u8bb0\u5f55',
  aboutBook: '\u5173\u4e8e\u300a',
  bookSuffix: '\u300b',
  unknownBook: '\u672a\u77e5\u4e66\u7c4d',
  defaultUser: '\u6821\u56ed\u4e66\u53cb',
  defaultSeller: '\u4e66\u7c4d\u5356\u5bb6',
  loading: '\u52a0\u8f7d\u4e2d...',
  retry: '\u91cd\u8bd5',
  loadFailed: '\u52a0\u8f7d\u5931\u8d25',
  needLoginChat: '\u767b\u5f55\u540e\u67e5\u770b\u804a\u5929',
  goLogin: '\u53bb\u767b\u5f55'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightPadding: 20,
      activeTopTab: 'community',
      activeCommunityTab: 'recommend',
      topTabs: [
        { label: TEXTS.community, value: 'community' },
        { label: TEXTS.activity, value: 'activity' },
        { label: TEXTS.chat, value: 'chat' }
      ],
      communityTabs: [
        { label: TEXTS.recommend, value: 'recommend' },
        { label: TEXTS.review, value: 'review' },
        { label: TEXTS.qa, value: 'qa' },
        { label: TEXTS.path, value: 'path' }
      ],
      communityPosts: [],
      publicPaths: [],
      activityList: [],
      chatSessions: [],
      isLoggedIn: false,
      feedLoading: false,
      feedError: '',
      pathLoading: false,
      pathError: '',
      activityLoading: false,
      activityError: '',
      chatLoading: false,
      chatError: ''
    };
  },
  computed: {
    filteredCommunityPosts() {
      if (this.activeCommunityTab === 'recommend') return this.communityPosts;
      return this.communityPosts.filter((item) => item.type === this.activeCommunityTab);
    }
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    if (capsule) {
      const screenWidth = systemInfo.windowWidth || 375;
      this.headerRightPadding = Math.max(20, screenWidth - capsule.left + 10);
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightPadding = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.applyInitialTabIntent();
    this.isLoggedIn = hasValidSession();
    this.fetchCommunityFeeds();
    this.fetchActivities();
    if (this.isLoggedIn && this.activeTopTab === 'chat') {
      this.fetchChats();
    }
  },
  onShow() {
    this.applyInitialTabIntent();
    this.isLoggedIn = hasValidSession();
    if (this.activeTopTab === 'community') {
      if (this.activeCommunityTab === 'path') {
        this.fetchPublicPaths();
      } else {
        this.fetchCommunityFeeds();
      }
    } else if (this.activeTopTab === 'chat' && this.isLoggedIn) {
      this.fetchChats();
    }
  },
  methods: {
    formatMeta(school, time) {
      return [school, time].filter(Boolean).join(' / ');
    },
    switchTopTab(tab) {
      if (tab === 'chat' && !ensureLoggedIn(getCurrentPageUrl())) return;
      this.activeTopTab = tab;
      if (tab === 'community') {
        if (this.activeCommunityTab === 'path') {
          this.fetchPublicPaths();
        } else {
          this.fetchCommunityFeeds();
        }
      }
      if (tab === 'activity') this.fetchActivities();
      if (tab === 'chat') this.fetchChats();
    },
    switchCommunityTab(tab) {
      this.activeCommunityTab = tab;
      if (tab === 'path') {
        this.fetchPublicPaths();
      } else {
        this.fetchCommunityFeeds();
      }
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
    applyInitialTabIntent() {
      const initialTab = uni.getStorageSync('communityInitialTab');
      if (!initialTab) return;
      uni.removeStorageSync('communityInitialTab');
      if (['community', 'activity', 'chat'].includes(initialTab)) {
        this.activeTopTab = initialTab;
      }
    },
    handlePublish() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({ url: '/pages/community/create' });
    },
    openPost(post) {
      if (post && post.type === 'path' && post.sharedPath && post.sharedPath.id) {
        this.openSharedPath(post.sharedPath);
        return;
      }
      this.openComments(post);
    },
    openComments(post) {
      uni.navigateTo({
        url: `/pages/community/comments?postId=${encodeURIComponent(post.id || '')}&postTitle=${encodeURIComponent(post.title || '')}`
      });
    },
    openSharedPath(path) {
      if (!path || !path.id) return;
      uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(path.id)}` });
    },
    openLearningPath(path) {
      const id = path.pathId || path.id;
      if (!id) return;
      uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
    },
    openChat(item) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      const params = [
        `sessionId=${encodeURIComponent(item.sessionId || item.id || '')}`,
        `sellerId=${encodeURIComponent(item.sellerId || '')}`,
        `sellerName=${encodeURIComponent(item.name || '')}`,
        `bookId=${encodeURIComponent(item.bookId || '')}`,
        `bookTitle=${encodeURIComponent(item.bookTitle || '')}`
      ].join('&');
      uni.navigateTo({ url: `/pages/chat/chat?${params}` });
    },
    async toggleLike(post) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await toggleCommunityLike(post.id);
        post.liked = Boolean(data?.liked);
        post.likes = Number(data?.likeCount || 0);
      } catch (error) {
        console.error('toggleCommunityLike failed', error);
      }
    },
    async toggleFavorite(post) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await toggleCommunityFavorite(post.id);
        post.favorited = Boolean(data?.favorited);
        post.favoriteCount = Number(data?.favoriteCount || 0);
      } catch (error) {
        console.error('toggleCommunityFavorite failed', error);
      }
    },
    reportPost(post) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      const query = [
        'mode=community-report',
        `postId=${encodeURIComponent(post.id || '')}`,
        `title=${encodeURIComponent(post.title || '')}`,
        `pagePath=${encodeURIComponent('/pages/community/community')}`
      ].join('&');
      uni.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    async fetchCommunityFeeds() {
      this.feedLoading = true;
      this.feedError = '';
      try {
        const type = this.activeCommunityTab === 'recommend' ? '' : this.activeCommunityTab;
        this.communityPosts = (await getCommunityFeed(type)) || [];
      } catch (error) {
        console.error('fetchCommunityFeeds failed', error);
        this.feedError = error?.message || TEXTS.loadFailed;
      } finally {
        this.feedLoading = false;
      }
    },
    async fetchPublicPaths() {
      this.pathLoading = true;
      this.pathError = '';
      try {
        this.publicPaths = (await getPathList({ category: '', keyword: '' })) || [];
      } catch (error) {
        console.error('fetchPublicPaths failed', error);
        this.pathError = error?.message || TEXTS.loadFailed;
      } finally {
        this.pathLoading = false;
      }
    },
    async fetchActivities() {
      this.activityLoading = true;
      this.activityError = '';
      try {
        this.activityList = (await getCommunityActivity()) || [];
      } catch (error) {
        console.error('fetchActivities failed', error);
        this.activityError = error?.message || TEXTS.loadFailed;
      } finally {
        this.activityLoading = false;
      }
    },
    async fetchChats() {
      if (!hasValidSession()) {
        this.isLoggedIn = false;
        this.chatSessions = [];
        return;
      }
      this.isLoggedIn = true;
      this.chatLoading = true;
      this.chatError = '';
      try {
        this.chatSessions = (await getChatSessions()) || [];
      } catch (error) {
        console.error('fetchChats failed', error);
        this.chatError = error?.message || TEXTS.loadFailed;
      } finally {
        this.chatLoading = false;
      }
    },
    goLoginForChat() {
      ensureLoggedIn(getCurrentPageUrl());
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 260rpx, #eef3fb 100%); }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 30; box-sizing: border-box; padding-left: 24rpx; background: linear-gradient(135deg, #143a7b 0%, #1f5eff 62%, #13b8a6 100%); display: flex; align-items: center; justify-content: space-between; box-shadow: 0 18rpx 42rpx rgba(31, 94, 255, 0.18); }
.header-main { display: flex; flex-direction: column; }
.header-title { font-size: 38rpx; color: #ffffff; font-weight: 800; }
.header-sub { margin-top: 6rpx; font-size: 22rpx; color: rgba(255, 255, 255, 0.82); }
.publish-btn { width: 72rpx; height: 72rpx; border-radius: 36rpx; background: rgba(255, 255, 255, 0.18); color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 44rpx; line-height: 1; box-shadow: 0 12rpx 24rpx rgba(8, 25, 61, 0.18); }
.top-tabs { position: sticky; top: 0; z-index: 25; background: rgba(238, 243, 251, 0.96); padding: 12rpx 20rpx 0; display: flex; gap: 14rpx; border-bottom: none; }
.top-tab { position: relative; flex: 1; height: 70rpx; border-radius: 22rpx; background: #ffffff; font-size: 28rpx; color: #63758a; font-weight: 700; display: flex; align-items: center; justify-content: center; box-shadow: 0 10rpx 22rpx rgba(23, 32, 51, 0.05); }
.top-tab.active { color: #ffffff; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); }
.top-tab.active::after { content: none; }
.sub-tabs-wrap { background: rgba(238, 243, 251, 0.96); padding: 0 20rpx; }
.sub-tabs { white-space: nowrap; }
.sub-tabs-row { display: inline-flex; gap: 14rpx; padding: 16rpx 0 8rpx; }
.sub-tab { padding: 12rpx 20rpx; border-radius: 999rpx; background: #ffffff; color: #647588; font-size: 22rpx; border: 1rpx solid #dfe8f4; }
.sub-tab.active { background: #e8efff; color: #1f5eff; font-weight: 700; }
.content { padding: 18rpx 20rpx 0; }
.feed-list, .activity-list, .chat-list { display: flex; flex-direction: column; gap: 16rpx; }
.post-card, .section-card, .activity-card, .chat-card, .path-card { background: #ffffff; border-radius: 28rpx; padding: 22rpx; border: 1rpx solid #e2eaf5; box-shadow: 0 16rpx 36rpx rgba(23, 32, 51, 0.07); }
.path-feed { display: flex; flex-direction: column; gap: 16rpx; }
.path-card { margin-bottom: 16rpx; }
.path-card-cover { width: 100%; height: 220rpx; border-radius: 16rpx; background: #eef2f8; margin-bottom: 16rpx; }
.path-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.path-card-title { flex: 1; min-width: 0; font-size: 32rpx; color: #243548; font-weight: 700; line-height: 1.4; }
.path-card-category { flex-shrink: 0; padding: 8rpx 14rpx; border-radius: 999rpx; background: #e8efff; color: #1f5eff; font-size: 21rpx; }
.path-card-desc { display: block; margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: #5d7086; }
.path-card-meta { margin-top: 16rpx; display: flex; flex-wrap: wrap; gap: 10rpx; }
.path-card-meta text { padding: 8rpx 14rpx; border-radius: 999rpx; background: #f1f5f9; color: #62758a; font-size: 22rpx; }
.shared-cover { width: 100%; height: 180rpx; border-radius: 14rpx; background: #eef2f8; margin-bottom: 12rpx; }
.post-head, .chat-card { display: flex; align-items: flex-start; gap: 16rpx; }
.avatar { width: 82rpx; height: 82rpx; border-radius: 50%; background: #e9edf3; flex-shrink: 0; }
.post-user, .chat-main { flex: 1; min-width: 0; }
.user-name, .chat-name { font-size: 28rpx; color: #243548; font-weight: 700; }
.user-meta, .chat-time, .chat-book { margin-top: 6rpx; font-size: 22rpx; color: #7b8ea1; }
.post-title-row { margin-top: 20rpx; display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.post-title { flex: 1; min-width: 0; display: block; font-size: 34rpx; line-height: 1.45; color: #243548; font-weight: 700; }
.type-label { flex-shrink: 0; padding: 8rpx 14rpx; border-radius: 999rpx; background: #e8efff; color: #1f5eff; font-size: 21rpx; }
.post-content { display: block; margin-top: 16rpx; font-size: 25rpx; line-height: 1.8; color: #51667d; }
.shared-path { margin-top: 16rpx; border-radius: 18rpx; background: #f4f7fb; padding: 18rpx; border: 1rpx solid #e6edf5; }
.shared-label { display: block; font-size: 22rpx; color: #1f5eff; font-weight: 700; }
.shared-title { display: block; margin-top: 8rpx; font-size: 27rpx; color: #26384d; font-weight: 700; line-height: 1.4; }
.shared-meta { display: block; margin-top: 8rpx; font-size: 22rpx; color: #76899e; }
.tags-row { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 18rpx; }
.tag { padding: 8rpx 16rpx; border-radius: 999rpx; background: #e7fbf8; color: #0f766e; font-size: 22rpx; }
.post-actions { display: flex; gap: 26rpx; margin-top: 20rpx; }
.action-item { display: flex; align-items: center; }
.action-text { font-size: 24rpx; color: #6d8095; }
.action-text.active { color: #1f5eff; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; margin-top: 18rpx; }
.summary-item { border-radius: 18rpx; background: #f4f7fb; padding: 20rpx 12rpx; text-align: center; }
.summary-num { display: block; font-size: 36rpx; color: #1f5eff; font-weight: 700; }
.summary-label { display: block; margin-top: 8rpx; font-size: 22rpx; color: #77889b; }
.section-title { font-size: 30rpx; color: #243548; font-weight: 700; }
.activity-card { display: flex; gap: 18rpx; align-items: flex-start; }
.activity-dot { width: 18rpx; height: 18rpx; border-radius: 50%; background: #14b8a6; margin-top: 12rpx; flex-shrink: 0; }
.activity-main { flex: 1; }
.activity-title { display: block; font-size: 28rpx; color: #243548; font-weight: 700; }
.activity-desc { display: block; margin-top: 10rpx; font-size: 24rpx; line-height: 1.7; color: #61758b; }
.activity-time { display: block; margin-top: 14rpx; font-size: 22rpx; color: #8a9bab; }
.chat-top { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.chat-preview { display: block; margin-top: 10rpx; font-size: 24rpx; color: #52677f; line-height: 1.6; }
.badge { min-width: 36rpx; height: 36rpx; padding: 0 10rpx; border-radius: 18rpx; background: #ff5d4d; color: #ffffff; font-size: 22rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-left: 12rpx; }
.empty, .state { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
.state { display: flex; flex-direction: column; align-items: center; gap: 20rpx; }
.state-text { display: block; color: #6d8095; font-size: 26rpx; }
.retry-btn { min-width: 156rpx; height: 64rpx; padding: 0 24rpx; border-radius: 18rpx; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.bottom-space { height: calc(40rpx + env(safe-area-inset-bottom)); }
</style>
