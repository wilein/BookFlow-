<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ texts.title }}</text>
      <view class="create-btn" @click="goCreate">创建</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="currentPath" class="current-card" @click="openPath(currentPath)">
        <view class="current-top">
          <text class="current-label">{{ texts.current }}</text>
          <text class="current-percent">{{ currentPath.progressPercent || 0 }}%</text>
        </view>
        <text class="current-title">{{ currentPath.title }}</text>
        <text class="current-meta">{{ currentPath.completedCount || 0 }}/{{ currentPath.nodeCount || 0 }} {{ texts.nodeDone }} · {{ currentPath.lastLearnTime || texts.justNow }}</text>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: (currentPath.progressPercent || 0) + '%' }"></view>
        </view>
      </view>

      <view class="tabs">
        <view
          v-for="tab in tabs"
          :key="tab.value"
          class="tab"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </view>
      </view>

      <view v-if="displayItems.length" class="list">
        <view v-for="item in displayItems" :key="item.id" class="card" @click="openPath(item)">
          <view class="card-head">
            <text class="title">{{ item.title }}</text>
            <text class="status">{{ getStatusText(item) }}</text>
          </view>
          <text class="desc">{{ item.description || texts.emptyDesc }}</text>
          <view v-if="activeTab === 'learning'" class="progress-row">
            <view class="mini-track">
              <view class="mini-fill" :style="{ width: (item.progressPercent || 0) + '%' }"></view>
            </view>
            <text class="progress-text">{{ item.progressPercent || 0 }}%</text>
          </view>
          <view class="footer-row">
            <text class="meta">{{ item.difficulty }} / {{ item.totalDuration }}</text>
            <text class="meta">{{ texts.nodeCount }} {{ item.nodeCount || 0 }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty">
        <text class="empty-title">{{ emptyText }}</text>
        <text class="empty-link" @click="goPathList">{{ texts.goFind }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCurrentLearningPath, getMyLearningPaths } from '../../utils/api/path';
import { getMyPaths } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      activeTab: 'learning',
      currentPath: null,
      learningItems: [],
      createdItems: [],
      tabs: [
        { label: '正在学习', value: 'learning' },
        { label: '我创建的', value: 'created' }
      ],
      texts: {
        title: '我的路径',
        current: '当前学习',
        justNow: '刚刚学习',
        emptyDesc: '暂无路径描述',
        emptyLearning: '还没有开始学习的路径',
        emptyCreated: '还没有创建学习路径',
        goFind: '去路径广场看看',
        nodeCount: '节点',
        nodeDone: '节点完成'
      }
    };
  },
  computed: {
    displayItems() {
      return this.activeTab === 'learning' ? this.learningItems : this.createdItems;
    },
    emptyText() {
      return this.activeTab === 'learning' ? this.texts.emptyLearning : this.texts.emptyCreated;
    }
  },
  onLoad() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 12;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const [learning, current, created] = await Promise.all([
          getMyLearningPaths(),
          getCurrentLearningPath(),
          getMyPaths()
        ]);
        this.learningItems = Array.isArray(learning) ? learning : [];
        this.currentPath = current || null;
        this.createdItems = Array.isArray(created) ? created : [];
      } catch (error) {
        console.error('fetch my paths failed', error);
      }
    },
    getStatusText(item) {
      if (this.activeTab === 'learning') {
        return `${item.progressPercent || 0}%`;
      }
      return item.statusLabel || '';
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    openPath(item) {
      const id = item.pathId || item.id;
      if (!id) return;
      const isCreator = this.activeTab === 'created' || item.isCreator ? '1' : '0';
      uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}&isCreator=${isCreator}` });
    },
    goPathList() {
      uni.navigateTo({ url: '/pages/path/list' });
    },
    goCreate() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({ url: '/pages/path/create' });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.create-btn { width: 88rpx; height: 64rpx; border-radius: 16rpx; background: #173b75; color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; flex-shrink: 0; }
.content { padding: 16rpx 20rpx calc(32rpx + env(safe-area-inset-bottom)); }
.current-card { border-radius: 22rpx; padding: 22rpx; background: #173b75; color: #ffffff; margin-bottom: 16rpx; }
.current-top { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.current-label { font-size: 24rpx; opacity: 0.86; }
.current-percent { font-size: 30rpx; font-weight: 700; }
.current-title { display: block; margin-top: 12rpx; font-size: 34rpx; line-height: 1.35; font-weight: 700; }
.current-meta { display: block; margin-top: 10rpx; font-size: 23rpx; opacity: 0.82; }
.progress-track { margin-top: 18rpx; height: 14rpx; border-radius: 999rpx; background: rgba(255,255,255,0.24); overflow: hidden; }
.progress-fill { height: 100%; border-radius: 999rpx; background: #5ee09b; }
.tabs { display: flex; gap: 12rpx; margin-bottom: 16rpx; }
.tab { flex: 1; height: 72rpx; border-radius: 16rpx; background: #ffffff; color: #63758a; display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: 700; }
.tab.active { background: #e8efff; color: #1f5eff; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.card-head, .footer-row { display: flex; align-items: center; justify-content: space-between; gap: 14rpx; }
.title { flex: 1; font-size: 30rpx; color: #243548; font-weight: 700; line-height: 1.4; }
.status { font-size: 22rpx; color: #1f5eff; flex-shrink: 0; }
.desc { display: block; margin-top: 12rpx; font-size: 24rpx; color: #53677e; line-height: 1.7; }
.progress-row { margin-top: 16rpx; display: flex; align-items: center; gap: 12rpx; }
.mini-track { flex: 1; height: 12rpx; border-radius: 999rpx; background: #eef2f8; overflow: hidden; }
.mini-fill { height: 100%; border-radius: 999rpx; background: #2db86e; }
.progress-text { width: 70rpx; text-align: right; font-size: 22rpx; color: #2f8a56; font-weight: 700; }
.footer-row { margin-top: 16rpx; }
.meta { font-size: 22rpx; color: #7b8ea1; }
.empty { margin-top: 120rpx; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 16rpx; }
.empty-title { color: #7d8fa2; font-size: 28rpx; }
.empty-link { padding: 14rpx 22rpx; border-radius: 999rpx; background: #e8efff; color: #1f5eff; font-size: 24rpx; font-weight: 700; }
</style>
