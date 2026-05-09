<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">学习路径</text>
      <view class="create-btn" @click="goCreate">创建</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="currentPath" class="current-card" @click="openPath(currentPath)">
        <view class="current-top">
          <text class="current-label">当前学习</text>
          <text class="current-progress">{{ currentPath.progressPercent || 0 }}%</text>
        </view>
        <text class="current-title">{{ currentPath.title }}</text>
        <text class="current-meta">{{ currentPath.creator }} · {{ currentPath.difficulty }} · {{ currentPath.totalDuration }}</text>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: (currentPath.progressPercent || 0) + '%' }"></view>
        </view>
      </view>

      <view class="search-box">
        <image class="search-icon" src="/static/search.png" mode="aspectFit"></image>
        <input
          class="search-input"
          v-model="keyword"
          confirm-type="search"
          placeholder="搜索路径标题、目标或关键知识点"
          @confirm="fetchPaths"
        />
        <view v-if="keyword" class="clear-btn" @click="clearSearch">×</view>
      </view>

      <scroll-view class="category-scroll" scroll-x show-scrollbar="false" enable-flex>
        <view class="category-row">
          <view
            v-for="item in categories"
            :key="item"
            class="category-chip"
            :class="{ active: activeCategory === item }"
            @click="switchCategory(item)"
          >
            {{ item }}
          </view>
        </view>
      </scroll-view>

      <view class="summary-row">
        <text class="summary-text">{{ activeCategory }} · {{ paths.length }} 条路径</text>
        <text class="summary-action" @click="fetchPaths">刷新</text>
      </view>

      <view v-if="paths.length" class="path-list">
        <view v-for="item in paths" :key="item.id" class="path-card" @click="openPath(item)">
          <image v-if="item.coverImage || item.cover" class="path-cover" :src="item.coverImage || item.cover" mode="aspectFill"></image>
          <view class="path-head">
            <view class="path-title-wrap">
              <text class="path-title">{{ item.title }}</text>
              <text class="path-desc">{{ item.description || '暂无路径说明' }}</text>
            </view>
            <view class="category-badge">{{ item.category || '其他' }}</view>
          </view>

          <view class="meta-row">
            <text class="meta">{{ item.creator || '校园同学' }}</text>
            <text class="dot">·</text>
            <text class="meta">{{ item.difficulty || '入门' }}</text>
            <text class="dot">·</text>
            <text class="meta">{{ item.totalDuration || '时长待补充' }}</text>
          </view>

          <view class="stats-row">
            <text class="stat">{{ item.nodeCount || 0 }} 个节点</text>
            <text class="stat">{{ item.learnerCount || item.learners || 0 }} 人在学</text>
            <text v-if="item.started" class="stat active">已学 {{ item.progressPercent || 0 }}%</text>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-title">暂无匹配路径</text>
        <text class="empty-desc">换一个分类或关键词试试</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCurrentLearningPath, getPathList } from '../../utils/api/path';
import { ensureLoggedIn, getCurrentPageUrl, hasValidSession } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: '',
      activeCategory: '全部',
      categories: ['全部', '编程开发', '计算机基础', '考研课程', '设计产品', '语言文学', '其他'],
      paths: [],
      currentPath: null,
      loading: false
    };
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 12;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.keyword = decodeURIComponent(options.keyword || '');
    const category = decodeURIComponent(options.category || '');
    if (this.categories.includes(category)) {
      this.activeCategory = category;
    }
    this.fetchPaths();
  },
  onShow() {
    this.fetchCurrentPath();
  },
  methods: {
    async fetchPaths() {
      this.loading = true;
      try {
        const params = {
          category: this.activeCategory === '全部' ? '' : this.activeCategory,
          keyword: this.keyword.trim()
        };
        this.paths = (await getPathList(params)) || [];
      } catch (error) {
        console.error('fetchPaths failed', error);
      } finally {
        this.loading = false;
      }
    },
    async fetchCurrentPath() {
      if (!hasValidSession()) {
        this.currentPath = null;
        return;
      }
      try {
        this.currentPath = (await getCurrentLearningPath()) || null;
      } catch (error) {
        this.currentPath = null;
      }
    },
    switchCategory(category) {
      if (this.activeCategory === category) return;
      this.activeCategory = category;
      this.fetchPaths();
    },
    clearSearch() {
      this.keyword = '';
      this.fetchPaths();
    },
    openPath(item) {
      const id = item.pathId || item.id;
      if (!id) return;
      uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
    },
    goCreate() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({ url: '/pages/path/create' });
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f5f8; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 30; box-sizing: border-box; padding-left: 20rpx; padding-right: 20rpx; background: #f3f5f8; display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.back-btn { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 32rpx; color: #2d3d52; font-weight: 700; }
.create-btn { width: 94rpx; height: 62rpx; border-radius: 16rpx; background: #2f4f75; color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; }
.content { padding: 18rpx 20rpx calc(40rpx + env(safe-area-inset-bottom)); }
.current-card { border-radius: 22rpx; padding: 22rpx; background: #2f4f75; color: #ffffff; margin-bottom: 16rpx; }
.current-top { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.current-label { font-size: 24rpx; opacity: 0.86; }
.current-progress { font-size: 28rpx; font-weight: 700; }
.current-title { display: block; margin-top: 12rpx; font-size: 34rpx; font-weight: 700; line-height: 1.35; }
.current-meta { display: block; margin-top: 10rpx; font-size: 23rpx; opacity: 0.82; }
.progress-track { margin-top: 18rpx; height: 14rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.24); overflow: hidden; }
.progress-fill { height: 100%; border-radius: 999rpx; background: #5ee09b; }
.search-box { height: 82rpx; border-radius: 18rpx; background: #ffffff; display: flex; align-items: center; gap: 12rpx; padding: 0 18rpx; box-shadow: 0 8rpx 22rpx rgba(28, 46, 70, 0.04); }
.search-icon { width: 34rpx; height: 34rpx; flex-shrink: 0; }
.search-input { flex: 1; height: 82rpx; font-size: 26rpx; color: #2d3d52; }
.clear-btn { width: 44rpx; height: 44rpx; border-radius: 22rpx; background: #eef2f8; color: #728397; display: flex; align-items: center; justify-content: center; font-size: 34rpx; line-height: 1; }
.category-scroll { margin-top: 16rpx; white-space: nowrap; }
.category-row { display: inline-flex; gap: 12rpx; padding-bottom: 4rpx; }
.category-chip { padding: 12rpx 22rpx; border-radius: 999rpx; background: #ffffff; color: #5f7185; font-size: 24rpx; white-space: nowrap; }
.category-chip.active { background: #e8efff; color: #2d55c7; font-weight: 700; }
.summary-row { margin-top: 16rpx; margin-bottom: 12rpx; display: flex; align-items: center; justify-content: space-between; }
.summary-text { font-size: 24rpx; color: #738397; }
.summary-action { font-size: 24rpx; color: #2d55c7; font-weight: 700; }
.path-list { display: flex; flex-direction: column; gap: 16rpx; }
.path-card { background: #ffffff; border-radius: 22rpx; padding: 22rpx; box-shadow: 0 8rpx 22rpx rgba(28, 46, 70, 0.04); }
.path-cover { width: 100%; height: 220rpx; border-radius: 16rpx; background: #eef2f8; margin-bottom: 18rpx; }
.path-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.path-title-wrap { flex: 1; min-width: 0; }
.path-title { display: block; font-size: 31rpx; line-height: 1.4; color: #25384d; font-weight: 700; }
.path-desc { display: block; margin-top: 10rpx; font-size: 24rpx; line-height: 1.65; color: #64778c; }
.category-badge { flex-shrink: 0; padding: 8rpx 14rpx; border-radius: 999rpx; background: #edf3ff; color: #2d55c7; font-size: 21rpx; }
.meta-row, .stats-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10rpx; }
.meta-row { margin-top: 16rpx; }
.stats-row { margin-top: 14rpx; }
.meta, .dot { font-size: 23rpx; color: #7a8da0; }
.stat { padding: 8rpx 14rpx; border-radius: 999rpx; background: #f1f5f9; color: #617389; font-size: 22rpx; }
.stat.active { background: #e9f8ed; color: #2f8a56; font-weight: 700; }
.empty { margin-top: 120rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.empty-title { font-size: 30rpx; color: #60748a; font-weight: 700; }
.empty-desc { font-size: 24rpx; color: #91a0b1; }
</style>
