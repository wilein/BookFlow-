<template>
  <view class="page">
    <view class="fixed-header" :style="{ paddingTop: statusBarHeight + 'px', paddingRight: capsulePaddingRight + 'px' }">
      <view class="logo-wrap">
        <image class="logo-img" src="/static/logo.png" mode="aspectFit"></image>
        <text class="logo-text">薪传</text>
      </view>
      <text class="slogan">快速查找教材与参考书</text>
    </view>
    <view :style="{ height: fixedHeaderHeight + 'px' }"></view>

    <view class="search-section">
      <view class="search-bar">
        <input class="search-input" v-model="keyword" placeholder="搜索书名、作者、分类..." @confirm="handleSearch" />
        <view class="search-btn" @click="handleSearch">
          <image class="search-icon" src="/static/search.png" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <view class="history-hot-section">
      <view class="block-header">
        <text class="block-title">搜索历史</text>
        <text class="clear-btn" @click="clearHistory">清空</text>
      </view>
      <view class="tag-list">
        <view class="tag-item" v-for="item in searchHistory" :key="item" @click="useKeyword(item)">{{ item }}</view>
      </view>
    </view>

    <view class="main-content">
      <scroll-view class="category-sidebar" scroll-y>
        <view
          class="category-item"
          v-for="item in categories"
          :key="item.id"
          :class="{ active: selectedCategoryId === item.id }"
          @click="selectCategory(item.id)"
        >
          <text class="category-name">{{ item.name }}</text>
        </view>
      </scroll-view>

      <scroll-view class="book-list" scroll-y>
        <view v-if="loading" class="status-block">加载中...</view>
        <view v-else-if="books.length === 0" class="status-block">暂无书籍</view>
        <view class="book-item" v-for="book in books" :key="book.id" @click="goToBookDetail(book)">
          <image class="book-cover" :src="book.cover || '/static/cover_placeholder.png'" mode="aspectFill"></image>
          <view class="book-info">
            <text class="book-title">{{ book.title }}</text>
            <text class="book-author">{{ book.author }}</text>
            <view class="book-meta">
              <text class="price">￥{{ book.price }}</text>
              <text class="annotations">{{ book.annotationCount || book.annotations || 0 }} 条批注</text>
            </view>
            <text class="tag">{{ book.categoryName || book.category }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { getBooksByCategory } from '../../utils/api/book';
import { buildBookQueryFromListItem } from '../../utils/book-detail';

export default {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      keyword: '',
      loading: false,
      categories: [],
      selectedCategoryId: '',
      categoryBooksMap: {},
      books: [],
      searchHistory: ['Java 编程思想', '算法导论', '设计模式', '数据结构']
    };
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule =
      typeof uni.getMenuButtonBoundingClientRect === 'function'
        ? uni.getMenuButtonBoundingClientRect()
        : null;
    if (capsule) {
      const screenWidth = uni.getSystemInfoSync().windowWidth;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
      this.fixedHeaderHeight = capsule.top + capsule.height + 58;
    } else {
      this.capsulePaddingRight = 100;
      this.fixedHeaderHeight = this.statusBarHeight + 92;
    }
    this.fetchCategoryData();
  },
  methods: {
    async fetchCategoryData() {
      this.loading = true;
      try {
        const data = await getBooksByCategory();
        const categoryNames = Object.keys(data || {});
        this.categories = categoryNames.map((name) => ({ id: name, name }));
        this.categoryBooksMap = data || {};
        if (this.categories.length > 0) {
          this.selectedCategoryId = this.categories[0].id;
          this.books = this.categoryBooksMap[this.selectedCategoryId] || [];
        }
      } catch (error) {
        console.error('fetchCategoryData failed', error);
      } finally {
        this.loading = false;
      }
    },
    selectCategory(categoryId) {
      this.selectedCategoryId = categoryId;
      this.books = this.categoryBooksMap[categoryId] || [];
    },
    handleSearch() {
      if (!this.keyword.trim()) {
        uni.showToast({ title: '请输入关键词', icon: 'none' });
        return;
      }
      if (!this.searchHistory.includes(this.keyword.trim())) {
        this.searchHistory.unshift(this.keyword.trim());
        this.searchHistory = this.searchHistory.slice(0, 10);
      }
      uni.navigateTo({
        url: `/pages/search/search?keyword=${encodeURIComponent(this.keyword.trim())}`
      });
    },
    useKeyword(keyword) {
      this.keyword = keyword;
      this.handleSearch();
    },
    clearHistory() {
      this.searchHistory = [];
    },
    goToBookDetail(book) {
      const query = buildBookQueryFromListItem(book);
      uni.navigateTo({ url: `/pages/books/detail?${query}` });
    }
  }
};
</script>

<style scoped>
.page {
  height: 100vh;
  background: #f8f9fc;
  display: flex;
  flex-direction: column;
}

.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding-left: 32rpx;
  padding-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.02);
  z-index: 100;
  box-sizing: border-box;
}

.logo-wrap {
  display: flex;
  align-items: center;
}

.logo-img {
  width: 48rpx;
  height: 48rpx;
  margin-right: 12rpx;
}

.logo-text {
  font-size: 44rpx;
  font-weight: 700;
  color: #2c3e50;
}

.slogan {
  margin-top: 8rpx;
  display: block;
  font-size: 24rpx;
  color: #7f8c8d;
}

.search-section,
.history-hot-section {
  background: #ffffff;
  padding: 18rpx 24rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #f0f3f7;
  border-radius: 60rpx;
  height: 72rpx;
  padding: 0 8rpx 0 24rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.search-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #3498db;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-icon {
  width: 36rpx;
  height: 36rpx;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.block-title,
.clear-btn {
  font-size: 24rpx;
  color: #5d7186;
}

.tag-list {
  margin-top: 12rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag-item {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #f3f7fb;
  color: #4b5f76;
  font-size: 22rpx;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-sidebar {
  width: 200rpx;
  background: #ffffff;
  border-right: 1rpx solid #e9eef5;
}

.category-item {
  padding: 30rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #4b5f76;
}

.category-item.active {
  background: #eaf3fb;
  color: #2f4f75;
  font-weight: 700;
}

.book-list {
  flex: 1;
  padding: 20rpx;
}

.status-block {
  padding: 50rpx 0;
  text-align: center;
  color: #7f8c8d;
}

.book-item {
  display: flex;
  gap: 20rpx;
  background: #ffffff;
  border-radius: 18rpx;
  padding: 16rpx;
  margin-bottom: 14rpx;
}

.book-cover {
  width: 130rpx;
  height: 170rpx;
  border-radius: 14rpx;
  background: #edf2f8;
}

.book-info {
  flex: 1;
}

.book-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #2c3e50;
}

.book-author {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #7f8c8d;
}

.book-meta {
  margin-top: 10rpx;
  display: flex;
  gap: 16rpx;
}

.price {
  color: #e67e22;
  font-size: 24rpx;
  font-weight: 600;
}

.annotations {
  color: #3498db;
  font-size: 22rpx;
}

.tag {
  display: inline-block;
  margin-top: 10rpx;
  padding: 6rpx 14rpx;
  border-radius: 24rpx;
  background: #e8f6f3;
  color: #16a085;
  font-size: 22rpx;
}
</style>
