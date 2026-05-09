<template>
  <view class="page">
    <view class="fixed-header" :style="{ paddingTop: statusBarHeight + 'px', paddingRight: capsulePaddingRight + 'px' }">
      <view class="header-row">
        <view class="logo-wrap">
          <image class="logo-img" src="/static/logo.png" mode="aspectFit"></image>
          <text class="logo-text">{{ textMap.appName }}</text>
        </view>
        <view class="search-btn" @click="goToSearch">
          <image class="search-img" src="/static/search.png" mode="aspectFit"></image>
        </view>
      </view>
      <text class="slogan">{{ textMap.slogan }}</text>
    </view>
    <view :style="{ height: fixedHeaderHeight + 'px' }"></view>

    <view v-if="currentPath" class="current-path-card" @click="goToPathDetail(currentPath)">
      <view class="current-path-top">
        <text class="current-path-label">当前学习路径</text>
        <text class="current-path-percent">{{ currentPath.progressPercent || 0 }}%</text>
      </view>
      <text class="current-path-title">{{ currentPath.title }}</text>
      <text class="current-path-meta">{{ currentPath.completedCount || 0 }}/{{ currentPath.nodeCount || 0 }} 个节点 · {{ currentPath.lastLearnTime || '刚刚学习' }}</text>
      <view class="current-progress-track">
        <view class="current-progress-fill" :style="{ width: (currentPath.progressPercent || 0) + '%' }"></view>
      </view>
    </view>

    <view class="swiper-section" v-if="bannerList.length > 0">
      <swiper class="swiper" circular indicator-dots autoplay interval="3000" duration="500">
        <swiper-item v-for="(item, index) in bannerList" :key="index">
          <image :src="item.image" class="swiper-image" mode="aspectFill"></image>
        </swiper-item>
      </swiper>
    </view>

    <view class="nav-grid">
      <view class="nav-item" v-for="item in navItems" :key="item.name" @click="handleNav(item)">
        <text class="nav-icon">{{ item.icon }}</text>
        <text class="nav-text">{{ item.name }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ textMap.hotBooks }}</text>
        <text class="more" @click="goToSearch">{{ textMap.more }}</text>
      </view>
      <scroll-view class="hot-tabs-scroll" scroll-x show-scrollbar="false" enable-flex>
        <view class="hot-tabs-row">
          <view
            v-for="tab in hotCategoryTabs"
            :key="tab"
            class="hot-tab"
            :class="{ active: selectedHotCategory === tab }"
            @click="selectedHotCategory = tab"
          >
            {{ tab }}
          </view>
        </view>
      </scroll-view>
      <scroll-view v-if="displayHotBooks.length > 0" class="hot-books-scroll" scroll-x show-scrollbar="false" enable-flex>
        <view class="hot-books-row">
          <view class="hot-book-card" v-for="book in displayHotBooks" :key="book._uniqueKey" @click="goToBookDetail(book)">
            <image class="hot-book-cover" :src="book.cover || '/static/cover_placeholder.png'" mode="aspectFill"></image>
            <view class="hot-book-main">
              <text class="hot-book-title">{{ book.title }}</text>
              <text class="hot-book-author">{{ book.author }}</text>
              <view class="hot-book-meta">
                <text class="hot-book-price">{{ formatPrice(book.price) }}</text>
                <text class="hot-book-annotation">{{ getAnnotationText(book) }}</text>
              </view>
              <text class="hot-book-tag">{{ book.categoryName || book.category || textMap.unknownCategory }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
      <view v-else class="empty-block">
        <text class="empty-text">{{ textMap.noHotBooks }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ textMap.recommendPaths }}</text>
        <text class="more" @click="goToPathList">{{ textMap.more }}</text>
      </view>
      <view v-if="studyPaths.length" class="path-list">
        <view class="path-item" v-for="path in studyPaths" :key="path.id || path.name" @click="goToPathDetail(path)">
          <view>
            <text class="path-name">{{ path.title || path.name }}</text>
            <text class="path-creator">{{ path.creator }} · {{ path.category || textMap.unknownCategory }}</text>
          </view>
          <view class="path-stats">
            <text>{{ path.nodeCount || path.bookCount || 0 }}{{ textMap.nodeUnit }}</text>
            <text>{{ path.learnerCount || path.learners || 0 }}{{ textMap.learnersUnit }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-block">
        <text class="empty-text">{{ textMap.noPaths }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ textMap.communityFeed }}</text>
        <text class="more" @click="goToCommunity">{{ textMap.more }}</text>
      </view>
      <view class="dynamic-list">
        <view class="dynamic-item" v-for="item in dynamics" :key="item.username + item.time" @click="goToCommunity">
          <image class="avatar" src="/static/logo.png" mode="aspectFill"></image>
          <view class="dynamic-content">
            <view class="dynamic-header">
              <text class="username">{{ item.username }}</text>
              <text class="time">{{ item.time }}</text>
            </view>
            <text class="content">{{ item.content }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import { getBannerList } from '../../utils/api/common';
import { getBookList, getBooksByCategory } from '../../utils/api/book';
import { getCurrentLearningPath, getPathList } from '../../utils/api/path';
import { hasValidSession } from '../../utils/auth';
import { buildBookQueryFromListItem, toPriceText } from '../../utils/book-detail';

function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function createKey(book, index) {
  if (book && book.id !== undefined && book.id !== null && book.id !== '') {
    return String(book.id);
  }
  return `${book?.title || 'book'}-${book?.author || 'author'}-${index}`;
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      bannerList: [],
      hotCategoryTabs: ['\u5168\u90e8'],
      selectedHotCategory: '\u5168\u90e8',
      hotBookMap: {
        '\u5168\u90e8': []
      },
      currentPath: null,
      textMap: {
        appName: '\u85aa\u4f20',
        slogan: '\u6279\u6ce8\u4f20\u627f \u00b7 \u5b66\u4e60\u8def\u5f84 \u00b7 \u77e5\u8bc6\u5206\u4eab',
        hotBooks: '\u70ed\u95e8\u4e66\u7c4d',
        recommendPaths: '\u63a8\u8350\u5b66\u4e60\u8def\u5f84',
        communityFeed: '\u793e\u533a\u52a8\u6001',
        more: '\u66f4\u591a >',
        unknownCategory: '\u672a\u5206\u7c7b',
        noHotBooks: '\u6682\u65e0\u70ed\u95e8\u4e66\u7c4d',
        noPaths: '\u6682\u65e0\u63a8\u8350\u8def\u5f84',
        nodeUnit: '\u4e2a\u8282\u70b9',
        learnersUnit: '\u4eba\u5728\u5b66'
      },
      navItems: [
        { name: '\u5206\u7c7b\u6d4f\u89c8', icon: '\u4e66', action: 'category' },
        { name: '\u5b66\u4e60\u8def\u5f84', icon: '\u8def', action: 'path' },
        { name: '\u53d1\u5e03\u4e66\u7c4d', icon: '\u53d1', action: 'publish' },
        { name: '\u793e\u533a\u52a8\u6001', icon: '\u52a8', action: 'community' }
      ],
      studyPaths: [],
      dynamics: [
        { username: '\u738b\u540c\u5b66', time: '2\u5c0f\u65f6\u524d', content: '\u5206\u4eab\u4e86\u4e00\u672c\u5e26\u6709\u8be6\u7ec6\u6279\u6ce8\u7684\u8bbe\u8ba1\u6a21\u5f0f\u6559\u6750\u3002' },
        { username: '\u8d75\u540c\u5b66', time: '5\u5c0f\u65f6\u524d', content: '\u521a\u5b8c\u6210\u6570\u636e\u7ed3\u6784\u5b66\u4e60\u8def\u5f84\uff0c\u6536\u83b7\u5f88\u5927\u3002' }
      ]
    };
  },
  computed: {
    displayHotBooks() {
      return this.hotBookMap[this.selectedHotCategory] || [];
    }
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule =
      typeof uni.getMenuButtonBoundingClientRect === 'function'
        ? uni.getMenuButtonBoundingClientRect()
        : null;
    if (capsule) {
      const screenWidth = systemInfo.windowWidth || 375;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
      this.fixedHeaderHeight = capsule.top + capsule.height + 58;
    } else {
      this.capsulePaddingRight = 100;
      this.fixedHeaderHeight = this.statusBarHeight + 92;
    }
    this.fetchData();
  },
  onShow() {
    this.fetchCurrentPath();
  },
  methods: {
    async fetchData() {
      const [banners, grouped, books] = await Promise.all([
        getBannerList().catch((error) => {
          console.error('fetchBannerList failed', error);
          return [];
        }),
        getBooksByCategory().catch((error) => {
          console.error('fetchBooksByCategory failed', error);
          return {};
        }),
        getBookList().catch((error) => {
          console.error('fetchBookList failed', error);
          return [];
        })
      ]);
      this.bannerList = Array.isArray(banners) ? banners : [];
      const groupedBooks = grouped || {};
      const fallbackBooks = Array.isArray(books) ? books.map((book, index) => this.normalizeBook(book, index)) : [];
      this.buildHotBooks(groupedBooks, fallbackBooks);
      this.fetchPathData();
      this.fetchCurrentPath();
    },
    async fetchPathData() {
      try {
        const paths = await getPathList({ category: '', keyword: '' });
        this.studyPaths = Array.isArray(paths) ? paths.slice(0, 4) : [];
      } catch (error) {
        console.error('fetchPathData failed', error);
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
    normalizeBook(book, index, categoryName = '') {
      const key = createKey(book, index);
      return {
        ...book,
        id: book.id !== undefined && book.id !== null && book.id !== '' ? String(book.id) : key,
        _uniqueKey: `${key}-${index}`,
        price: toNumber(book.price, 0),
        annotationCount: toNumber(book.annotationCount ?? book.annotations, 0),
        categoryName: book.categoryName || categoryName || book.category || this.textMap.unknownCategory
      };
    },
    buildHotBooks(grouped, fallbackBooks) {
      const groupedEntries = Object.keys(grouped || {}).map((name, categoryIndex) => {
        const books = Array.isArray(grouped[name]) ? grouped[name] : [];
        return {
          name,
          books: books.map((book, index) => this.normalizeBook(book, categoryIndex * 20 + index, name))
        };
      });

      const sortedEntries = groupedEntries.sort((a, b) => b.books.length - a.books.length);
      const topEntries = sortedEntries.slice(0, 3);
      const seen = new Set();
      const allBooks = [];

      groupedEntries.forEach((entry) => {
        entry.books.forEach((book) => {
          const key = createKey(book, allBooks.length);
          if (seen.has(key)) return;
          seen.add(key);
          allBooks.push(book);
        });
      });

      if (!allBooks.length && Array.isArray(fallbackBooks)) {
        fallbackBooks.forEach((book, index) => {
          const key = createKey(book, index);
          if (seen.has(key)) return;
          seen.add(key);
          allBooks.push(book);
        });
      }

      const tabs = ['\u5168\u90e8', ...topEntries.map((item) => item.name)];
      const bookMap = {
        '\u5168\u90e8': allBooks.slice(0, 5)
      };

      topEntries.forEach((entry) => {
        bookMap[entry.name] = entry.books.slice(0, 5);
      });

      if (!bookMap['\u5168\u90e8'].length && Array.isArray(fallbackBooks)) {
        bookMap['\u5168\u90e8'] = fallbackBooks.slice(0, 5);
      }

      this.hotCategoryTabs = tabs.length ? tabs : ['\u5168\u90e8'];
      this.hotBookMap = bookMap;
      this.selectedHotCategory = this.hotCategoryTabs.includes(this.selectedHotCategory)
        ? this.selectedHotCategory
        : '\u5168\u90e8';
    },
    formatPrice(price) {
      return '\uffe5' + toPriceText(price);
    },
    getAnnotationText(book) {
      return `${book.annotationCount || 0}\u6761\u6279\u6ce8`;
    },
    goToSearch() {
      uni.navigateTo({ url: '/pages/search/search' });
    },
    goToCommunity() {
      uni.switchTab({ url: '/pages/community/community' });
    },
    handleNav(item) {
      if (item.action === 'category') {
        uni.switchTab({ url: '/pages/category/category' });
        return;
      }
      if (item.action === 'publish') {
        uni.switchTab({ url: '/pages/publish/create' });
        return;
      }
      if (item.action === 'community') {
        this.goToCommunity();
        return;
      }
      this.goToPathList();
    },
    goToBookDetail(book) {
      const query = buildBookQueryFromListItem(book);
      uni.navigateTo({ url: `/pages/books/detail?${query}` });
    },
    goToPathDetail(path) {
      const id = path.pathId || path.id;
      if (id) {
        uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
        return;
      }
      uni.navigateTo({
        url: `/pages/path/detail?title=${encodeURIComponent(path.name)}&creator=${encodeURIComponent(path.creator)}`
      });
    },
    goToPathList() {
      uni.navigateTo({ url: '/pages/path/list' });
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 240rpx, #eef3fb 100%);
}

.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #143a7b 0%, #1f5eff 58%, #13b8a6 100%);
  padding-left: 32rpx;
  padding-bottom: 28rpx;
  box-shadow: 0 18rpx 46rpx rgba(31, 94, 255, 0.22);
  z-index: 100;
  box-sizing: border-box;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-wrap {
  display: flex;
  align-items: center;
}

.logo-img {
  width: 48rpx;
  height: 48rpx;
  margin-right: 12rpx;
  padding: 8rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.16);
  box-sizing: border-box;
}

.logo-text {
  font-size: 44rpx;
  font-weight: 700;
  color: #ffffff;
}

.search-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-img {
  width: 36rpx;
  height: 36rpx;
}

.slogan {
  margin-top: 8rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.82);
  display: block;
}

.current-path-card {
  margin: 28rpx 24rpx 0;
  border-radius: 30rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  color: #ffffff;
  box-shadow: 0 18rpx 44rpx rgba(15, 118, 110, 0.2);
}

.current-path-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.current-path-label {
  font-size: 24rpx;
  opacity: 0.86;
}

.current-path-percent {
  font-size: 30rpx;
  font-weight: 700;
}

.current-path-title {
  display: block;
  margin-top: 12rpx;
  font-size: 34rpx;
  line-height: 1.35;
  font-weight: 700;
}

.current-path-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  opacity: 0.82;
}

.current-progress-track {
  margin-top: 18rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.24);
  overflow: hidden;
}

.current-progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #fef3c7 0%, #f59e0b 100%);
}

.swiper-section {
  margin: 26rpx 24rpx 20rpx;
  border-radius: 30rpx;
  overflow: hidden;
  box-shadow: 0 18rpx 42rpx rgba(23, 32, 51, 0.1);
}

.swiper {
  width: 100%;
  height: 320rpx;
}

.swiper-image {
  width: 100%;
  height: 100%;
}

.nav-grid {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: rgba(255, 255, 255, 0.92);
  margin: 24rpx;
  padding: 18rpx 8rpx;
  border-radius: 30rpx;
  border: 1rpx solid rgba(214, 226, 241, 0.9);
  box-shadow: 0 18rpx 42rpx rgba(23, 32, 51, 0.08);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
}

.nav-icon {
  width: 66rpx;
  height: 66rpx;
  border-radius: 22rpx;
  background: #e8efff;
  color: #1f5eff;
  font-size: 32rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-text {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #334155;
  font-weight: 600;
}

.section {
  margin: 24rpx;
  background: #ffffff;
  border-radius: 30rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(214, 226, 241, 0.9);
  box-shadow: 0 16rpx 40rpx rgba(23, 32, 51, 0.07);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #172033;
}

.more {
  font-size: 24rpx;
  color: #1f5eff;
  font-weight: 700;
}

.hot-tabs-scroll {
  white-space: nowrap;
}

.hot-tabs-row {
  display: inline-flex;
  gap: 14rpx;
  padding-bottom: 6rpx;
}

.hot-tab {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #f4f7fb;
  color: #5f7185;
  font-size: 24rpx;
  white-space: nowrap;
}

.hot-tab.active {
  background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%);
  color: #ffffff;
  box-shadow: 0 10rpx 22rpx rgba(31, 94, 255, 0.18);
}

.hot-books-scroll {
  margin-top: 18rpx;
  white-space: nowrap;
}

.hot-books-row {
  display: inline-flex;
  gap: 20rpx;
  padding-right: 8rpx;
}

.hot-book-card {
  width: 260rpx;
  flex-shrink: 0;
  background: #fbfcff;
  border-radius: 26rpx;
  overflow: hidden;
  border: 1rpx solid #e2eaf5;
  box-shadow: 0 12rpx 26rpx rgba(23, 32, 51, 0.06);
}

.hot-book-cover {
  width: 260rpx;
  height: 320rpx;
  background: #e9edf2;
}

.hot-book-main {
  padding: 18rpx;
}

.hot-book-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #172033;
  line-height: 1.4;
}

.hot-book-author,
.hot-book-tag {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #75879a;
}

.hot-book-meta {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.hot-book-price {
  font-size: 30rpx;
  color: #f59e0b;
  font-weight: 700;
}

.hot-book-annotation {
  font-size: 22rpx;
  color: #0f766e;
}

.empty-block {
  padding: 30rpx 0 10rpx;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #92a0b0;
}

.path-list,
.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.dynamic-item {
  display: flex;
  gap: 20rpx;
  padding: 18rpx;
  border-radius: 22rpx;
  background: #f7faff;
}

.path-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #f7faff;
  border: 1rpx solid #e5edf7;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.dynamic-content {
  flex: 1;
}

.path-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2c3e50;
}

.path-creator,
.time {
  margin-top: 6rpx;
  display: block;
  font-size: 24rpx;
  color: #7f8c8d;
}

.path-stats,
.dynamic-header {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.path-stats text {
  padding: 8rpx 12rpx;
  border-radius: 999rpx;
  background: #e8efff;
  color: #1f5eff;
  font-size: 21rpx;
  white-space: nowrap;
}

.username {
  font-size: 28rpx;
  color: #172033;
  font-weight: 600;
}

.content {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #5d6c7a;
  line-height: 1.6;
}

.bottom-safe {
  height: calc(120rpx + env(safe-area-inset-bottom));
}
</style>
