<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ selectMode === 'annotation' ? texts.selectForAnnotation : texts.title }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="tab-bar">
      <view v-for="item in tabs" :key="item.key" class="tab-item" :class="{ active: currentTab === item.key }" @click="switchTab(item.key)">
        {{ item.label }}
      </view>
    </view>

    <view class="content">
      <view v-if="books.length" class="list">
        <view v-for="book in books" :key="book.id" class="card" @click="openDetail(book)">
          <image class="cover" :src="book.cover || '/static/logo.png'" mode="aspectFill"></image>
          <view class="card-main">
            <text class="title">{{ book.title }}</text>
            <text class="meta">{{ book.author || texts.unknownAuthor }}</text>
            <view class="tag-row">
              <text class="tag">{{ book.conditionLabel }}</text>
              <text class="tag">{{ book.statusLabel }}</text>
            </view>
            <view class="bottom-row">
              <text class="price">{{ texts.currency }}{{ book.price }}</text>
              <text class="count">{{ texts.annotationCount }} {{ book.annotationCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>
  </view>
</template>

<script>
import { getMyBookshelf } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';
import { buildBookQueryFromListItem } from '../../utils/book-detail';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      selectMode: '',
      currentTab: 'selling',
      texts: {
        title: '\u6211\u7684\u4e66\u67b6',
        selectForAnnotation: '\u9009\u62e9\u6279\u6ce8\u4e66\u7c4d',
        currency: '\u00a5',
        annotationCount: '\u6279\u6ce8',
        empty: '\u6682\u65e0\u4e66\u7c4d',
        unknownAuthor: '\u672a\u77e5\u4f5c\u8005'
      },
      tabs: [
        { key: 'selling', label: '\u5728\u552e' },
        { key: 'sold', label: '\u5df2\u552e' }
      ],
      books: []
    };
  },
  onLoad(options = {}) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.selectMode = options.select || '';
    if (options.status === 'sold') this.currentTab = 'sold';
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.books = await getMyBookshelf(this.currentTab) || [];
      } catch (error) {
        console.error('getMyBookshelf failed', error);
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
    openDetail(book) {
      if (this.selectMode === 'annotation') {
        const params = [
          `bookId=${encodeURIComponent(book.id || '')}`,
          `bookTitle=${encodeURIComponent(book.title || '')}`
        ].join('&');
        uni.navigateTo({ url: `/pages/annotations/list?${params}` });
        return;
      }
      uni.navigateTo({ url: `/pages/books/detail?${buildBookQueryFromListItem(book)}` });
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
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.tab-bar { display: flex; gap: 12rpx; padding: 16rpx 20rpx 0; }
.tab-item { flex: 1; height: 72rpx; border-radius: 18rpx; background: #ffffff; color: #61758b; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.tab-item.active { background: #2d55c7; color: #ffffff; font-weight: 700; }
.content { padding: 16rpx 20rpx calc(32rpx + env(safe-area-inset-bottom)); }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { display: flex; gap: 18rpx; padding: 18rpx; background: #ffffff; border-radius: 20rpx; }
.cover { width: 150rpx; height: 190rpx; border-radius: 16rpx; background: #e8edf3; flex-shrink: 0; }
.card-main { flex: 1; display: flex; flex-direction: column; }
.title { font-size: 30rpx; line-height: 1.4; color: #243548; font-weight: 700; }
.meta { margin-top: 10rpx; font-size: 24rpx; color: #71859b; }
.tag-row { margin-top: 14rpx; display: flex; flex-wrap: wrap; gap: 10rpx; }
.tag { padding: 6rpx 14rpx; border-radius: 999rpx; background: #eef3fb; color: #4d6583; font-size: 22rpx; }
.bottom-row { margin-top: auto; display: flex; align-items: center; justify-content: space-between; }
.price { color: #d05a25; font-size: 34rpx; font-weight: 700; }
.count { color: #7d8fa2; font-size: 22rpx; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
</style>
