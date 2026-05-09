<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ pageTitle }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="items.length" class="list">
        <view v-for="item in items" :key="item.id" class="card" @click="openResource(item)">
          <view class="icon">{{ item.typeLabel ? item.typeLabel.slice(0, 1) : texts.resourceShort }}</view>
          <view class="card-main">
            <text class="title">{{ item.title || item.name }}</text>
            <text class="type">{{ item.typeLabel || texts.resource }}</text>
            <text class="desc">{{ item.description || texts.emptyDesc }}</text>
            <view class="meta-row">
              <text class="meta">{{ item.fileFormat || texts.file }}</text>
              <text class="meta">{{ texts.download }} {{ item.downloadCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>
  </view>
</template>

<script>
import { getResourceList } from '../../utils/api/resource';

function normalizeId(value) {
  const text = String(value == null ? '' : value).trim();
  if (!text || text === 'undefined' || text === 'null') {
    return undefined;
  }
  return /^\d+$/.test(text) ? text : undefined;
}

const TEXTS = {
  defaultTitle: '\u914d\u5957\u8d44\u6e90',
  resourceShort: '\u8d44',
  resource: '\u8d44\u6e90',
  emptyDesc: '\u6682\u65e0\u8d44\u6e90\u63cf\u8ff0',
  file: '\u6587\u4ef6',
  download: '\u4e0b\u8f7d',
  empty: '\u6682\u65e0\u8d44\u6e90',
  filePending: '\u8d44\u6e90\u6587\u4ef6\u5f85\u8865\u5145',
  copied: '\u8d44\u6e90\u94fe\u63a5\u5df2\u590d\u5236'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      pageTitle: TEXTS.defaultTitle,
      items: []
    };
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.title) {
      this.pageTitle = decodeURIComponent(options.title);
    }
    this.fetchData(options);
  },
  methods: {
    async fetchData(options) {
      try {
        const bookId = normalizeId(options.bookId);
        const pathNodeId = normalizeId(options.pathNodeId);
        this.items =
          (await getResourceList({
            bookId,
            pathNodeId
          })) || [];
      } catch (error) {
        console.error('getResourceList failed', error);
      }
    },
    openResource(item) {
      uni.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(item.id)}` });
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f5f8; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #f3f5f8; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 18rpx 20rpx 40rpx; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { display: flex; gap: 16rpx; background: #ffffff; border-radius: 22rpx; padding: 20rpx; box-shadow: 0 10rpx 24rpx rgba(19, 37, 62, 0.05); }
.icon { width: 92rpx; height: 92rpx; border-radius: 20rpx; background: #e7efff; color: #2d55c7; font-size: 34rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.card-main { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.title { font-size: 30rpx; color: #243548; font-weight: 700; }
.type { font-size: 22rpx; color: #2d55c7; }
.desc { font-size: 23rpx; color: #708399; line-height: 1.6; }
.meta-row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-top: 6rpx; }
.meta { font-size: 22rpx; color: #7d8fa2; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
</style>
