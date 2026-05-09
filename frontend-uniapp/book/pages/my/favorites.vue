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

    <view class="tab-bar">
      <view v-for="item in tabs" :key="item.key" class="tab-item" :class="{ active: currentTab === item.key }" @click="switchTab(item.key)">
        {{ item.label }}
      </view>
    </view>

    <view class="content">
      <view v-if="items.length" class="list">
        <view v-for="item in items" :key="item.id" class="card" @click="openItem(item)">
          <image v-if="currentTab === 'book'" class="cover" :src="item.cover || '/static/logo.png'" mode="aspectFill"></image>
          <view class="card-main">
            <text class="title">{{ item.title }}</text>
            <text v-if="currentTab === 'book'" class="meta">{{ item.author }} / {{ item.conditionLabel }}</text>
            <text v-else class="meta">{{ texts.creator }}{{ item.creator }}</text>
            <text class="desc">{{ item.description || (currentTab === 'book' ? texts.bookDescFallback : texts.pathDescFallback) }}</text>
            <view class="footer-row">
              <text v-if="currentTab === 'book'" class="price">{{ texts.currency }}{{ item.price }}</text>
              <text v-else class="path-info">{{ item.difficulty }} / {{ item.totalDuration }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>
  </view>
</template>

<script>
import { getMyFavorites } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';
import { buildBookQueryFromListItem } from '../../utils/book-detail';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      currentTab: 'book',
      texts: {
        title: '\u6211\u7684\u6536\u85cf',
        creator: '\u521b\u5efa\u8005\uff1a',
        bookDescFallback: '\u6682\u65e0\u63cf\u8ff0',
        pathDescFallback: '\u6682\u65e0\u8def\u5f84\u63cf\u8ff0',
        empty: '\u6682\u65e0\u6536\u85cf',
        currency: '\u00a5'
      },
      tabs: [
        { key: 'book', label: '\u4e66\u7c4d' },
        { key: 'path', label: '\u8def\u5f84' }
      ],
      items: []
    };
  },
  onLoad(options) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.type === 'path') this.currentTab = 'path';
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.items = await getMyFavorites(this.currentTab) || [];
      } catch (error) {
        console.error('getMyFavorites failed', error);
      }
    },
    switchTab(tab) {
      if (this.currentTab === tab) return;
      this.currentTab = tab;
      this.fetchData();
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    openItem(item) {
      if (this.currentTab === 'book') {
        uni.navigateTo({ url: `/pages/books/detail?${buildBookQueryFromListItem(item)}` });
        return;
      }
      const params = [
        `pathId=${encodeURIComponent(item.id || '')}`,
        `title=${encodeURIComponent(item.title || '')}`,
        `creator=${encodeURIComponent(item.creator || '')}`,
        `difficulty=${encodeURIComponent(item.difficulty || '')}`,
        `totalDuration=${encodeURIComponent(item.totalDuration || '')}`,
        `description=${encodeURIComponent(item.description || '')}`,
        'isCreator=0'
      ].join('&');
      uni.navigateTo({ url: `/pages/path/detail?${params}` });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.tab-bar { display: flex; gap: 12rpx; padding: 16rpx 20rpx 0; }
.tab-item { flex: 1; height: 72rpx; border-radius: 18rpx; background: #ffffff; color: #61758b; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.tab-item.active { background: #1f5eff; color: #ffffff; font-weight: 700; }
.content { padding: 16rpx 20rpx calc(32rpx + env(safe-area-inset-bottom)); }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { display: flex; gap: 16rpx; background: #ffffff; border-radius: 20rpx; padding: 18rpx; }
.cover { width: 128rpx; height: 164rpx; border-radius: 14rpx; background: #e8edf3; flex-shrink: 0; }
.card-main { flex: 1; display: flex; flex-direction: column; }
.title { font-size: 30rpx; line-height: 1.4; color: #243548; font-weight: 700; }
.meta { margin-top: 8rpx; font-size: 23rpx; color: #71859b; }
.desc { margin-top: 12rpx; font-size: 23rpx; color: #53677e; line-height: 1.6; }
.footer-row { margin-top: auto; display: flex; align-items: center; justify-content: space-between; }
.price { color: #f59e0b; font-size: 32rpx; font-weight: 700; }
.path-info { color: #4c6583; font-size: 22rpx; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
</style>
