<template>
  <view class="container">
    <!-- 固定顶部区域（仅保留 Logo 和标语） -->
    <view
      class="fixed-header"
      :style="{
        paddingTop: statusBarHeight + 'px',
        paddingRight: capsulePaddingRight + 'px'
      }"
      ref="fixedHeader"
    >
      <view class="header-row">
        <view class="logo-title">
          <image class="logo-img" src="/static/logo.png" mode="aspectFit"></image>
          <text class="logo-text">薪传</text>
        </view>
      </view>
      <text class="slogan">批注传承·学习路径·知识分享</text>
    </view>

    <!-- 占位视图，高度等于固定顶部高度 -->
    <view :style="{ height: fixedHeaderHeight + 'px' }"></view>

    <!-- 搜索条（整合按钮） -->
    <view class="search-section">
      <view class="search-bar">
        <input
          class="search-input"
          type="text"
          placeholder="搜索书名、作者、专业..."
          placeholder-class="placeholder"
          v-model="keyword"
          @confirm="handleSearch"
        />
        <view class="search-btn" @click="handleSearch">
          <image class="search-icon" src="/static/search.png" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <!-- 搜索历史 & 热门搜索 -->
    <view class="history-hot-section">
      <!-- 搜索历史 -->
      <view class="history-block" v-if="searchHistory.length > 0">
        <view class="block-header">
          <text class="block-title">搜索历史</text>
          <text class="clear-btn" @click="clearHistory">清空</text>
        </view>
        <view class="tag-list">
          <view
            class="tag-item"
            v-for="(item, index) in searchHistory"
            :key="index"
            @click="useHistory(item)"
          >
            <text>{{ item }}</text>
          </view>
        </view>
      </view>

      <!-- 热门搜索 -->
      <view class="hot-block">
        <view class="block-header">
          <text class="block-title">热门搜索</text>
        </view>
        <view class="tag-list">
          <view
            class="tag-item hot-tag"
            v-for="(item, index) in hotSearches"
            :key="index"
            @click="useHotSearch(item)"
          >
            <text>{{ item.name }}</text>
            <text class="hot-badge" v-if="item.badge">{{ item.badge }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 主体区域：左侧分类 + 右侧书籍列表 -->
    <view class="main-content">
      <!-- 左侧分类列表 -->
      <scroll-view class="category-sidebar" scroll-y enhanced show-scrollbar="false">
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

      <!-- 右侧书籍列表 -->
      <scroll-view class="book-list" scroll-y enhanced show-scrollbar="false">
        <view v-if="loading" class="loading-tip">加载中...</view>
        <template v-else>
          <view
            class="book-item"
            v-for="book in books"
            :key="book.id"
            @click="goToBookDetail(book)"
          >
            <image class="book-cover" :src="book.cover || '/static/cover_placeholder.png'" mode="aspectFill"></image>
            <view class="book-info">
              <text class="book-title">{{ book.title }}</text>
              <text class="book-author">{{ book.author }}</text>
              <view class="book-meta">
                <text class="price">￥{{ book.price }}</text>
                <text class="annotations">{{ book.annotations }}条批注</text>
              </view>
              <view class="book-tags">
                <text class="tag">{{ book.categoryName || book.category }}</text>
              </view>
            </view>
          </view>
          <view v-if="books.length === 0" class="empty-tip">暂无书籍</view>
        </template>
      </scroll-view>
    </view>

    <!-- 底部留白 -->
    <view class="bottom-safe"></view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      keyword: '',
      categories: [],
      selectedCategoryId: null,
      categoryBooksMap: {}, // 分类名 -> 书籍列表（后端返回，每类最多6条）
      books: [],
      loading: false,
      // 搜索历史（从本地存储读取，示例用静态）
      searchHistory: ['Java编程思想', '算法导论', '设计模式', '数据结构'],
      // 热门搜索（带角标）
      hotSearches: [
        { name: 'Java编程思想', badge: '热' },
        { name: '机器学习实战', badge: '新' },
        { name: '高等数学', badge: '热' },
        { name: '操作系统', badge: '' }
      ]
    };
  },
  onLoad() {
    // 获取状态栏和胶囊信息
    uni.getSystemInfo({
      success: (res) => {
        this.statusBarHeight = res.statusBarHeight;
      }
    });
    const capsule =
      typeof uni.getMenuButtonBoundingClientRect === 'function'
        ? uni.getMenuButtonBoundingClientRect()
        : null;
    if (capsule) {
      const screenWidth = uni.getSystemInfoSync().windowWidth;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
    } else {
      this.capsulePaddingRight = 100;
    }

    // 从后端获取分类及书籍数据
    this.fetchCategoryData();
  },
  onReady() {
    // 获取固定顶部的高度
    uni.createSelectorQuery()
      .in(this)
      .select('.fixed-header')
      .boundingClientRect((rect) => {
        if (rect) {
          this.fixedHeaderHeight = rect.height;
        }
      })
      .exec();
  },
  methods: {
    // 从后端获取分类及书籍数据（每个分类前6条）
    fetchCategoryData() {
      this.loading = true;
      uni.request({
        url: 'http://localhost:8080/book/category',
        method: 'GET',
        success: (res) => {
          if (res.data.code === '200' && res.data.data) {
            const data = res.data.data;
            // 根据返回数据中的分类标签统计出有哪些分类
            const categoryNames = Object.keys(data);
            this.categories = categoryNames.map((name, index) => ({
              id: name,
              name: name
            }));
            this.categoryBooksMap = data; // 分类 -> 书籍列表（最多6条）
            if (this.categories.length > 0) {
              this.selectedCategoryId = this.categories[0].id;
              this.books = data[this.selectedCategoryId] || [];
            }
          } else {
            uni.showToast({ title: '分类数据加载失败', icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('category api error', err);
          uni.showToast({ title: '网络错误', icon: 'none' });
        },
        complete: () => {
          this.loading = false;
        }
      });
    },
    selectCategory(categoryId) {
      if (this.selectedCategoryId === categoryId) return;
      this.selectedCategoryId = categoryId;
      this.books = this.categoryBooksMap[categoryId] || [];
    },
    // 处理搜索
    handleSearch() {
      if (!this.keyword.trim()) {
        uni.showToast({ title: '请输入关键词', icon: 'none' });
        return;
      }
      // 将关键词存入历史（避免重复）
      if (!this.searchHistory.includes(this.keyword)) {
        this.searchHistory.unshift(this.keyword);
        if (this.searchHistory.length > 10) this.searchHistory.pop(); // 限制长度
        // 实际可存入 localStorage
      }
      // 跳转到搜索结果页（示例）
      uni.navigateTo({
        url: `/pages/search/search?keyword=${encodeURIComponent(this.keyword)}&categoryId=${this.selectedCategoryId}`
      });
    },
    // 使用历史搜索词
    useHistory(item) {
      this.keyword = item;
      this.handleSearch();
    },
    // 使用热门搜索
    useHotSearch(item) {
      this.keyword = item.name;
      this.handleSearch();
    },
    // 清空历史
    clearHistory() {
      uni.showModal({
        title: '提示',
        content: '确定清空搜索历史吗？',
        success: (res) => {
          if (res.confirm) {
            this.searchHistory = [];
            // 同时清除本地存储
          }
        }
      });
    },
    goToBookDetail(book) {
      uni.navigateTo({
        url: `/pages/books/detail?id=${book.id}`
      });
    }
  }
};
</script>

<style scoped>
/* 隐藏滚动条 */
.container::-webkit-scrollbar,
.category-sidebar::-webkit-scrollbar,
.book-list::-webkit-scrollbar {
  display: none;
}
.container {
  -ms-overflow-style: none;
  scrollbar-width: none;
  background-color: #f8f9fc;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

/* 固定顶部区域 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  padding-left: 32rpx;
  padding-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.02);
  z-index: 100;
  box-sizing: border-box;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.logo-title {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.logo-img {
  width: 48rpx;
  height: 48rpx;
  margin-right: 12rpx;
}

.logo-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
}

.slogan {
  font-size: 28rpx;
  color: #7f8c8d;
  display: block;
}

/* 搜索条区域 */
.search-section {
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  border-bottom: 2rpx solid #ecf0f1;
}

.search-bar {
  display: flex;
  align-items: center;
  background-color: #f0f3f7;
  border-radius: 60rpx;
  height: 72rpx;
  padding: 0 8rpx 0 24rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #2c3e50;
  height: 100%;
  line-height: 72rpx;
}

.placeholder {
  color: #95a5a6;
  font-size: 28rpx;
}

.search-btn {
  width: 64rpx;
  height: 64rpx;
  background-color: #3498db;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8rpx;
  flex-shrink: 0;
}

.search-icon {
  width: 36rpx;
  height: 36rpx;
  filter: brightness(0) invert(1); /* 使黑色图标变白，如果图标本身是白色可去掉 */
}

/* 历史 & 热门区块 */
.history-hot-section {
  background-color: #ffffff;
  padding: 0 32rpx 20rpx;
  border-bottom: 2rpx solid #ecf0f1;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.block-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2c3e50;
}

.clear-btn {
  font-size: 26rpx;
  color: #95a5a6;
  padding: 8rpx 16rpx;
  background-color: #f8f9fc;
  border-radius: 30rpx;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.tag-item {
  font-size: 26rpx;
  color: #34495e;
  background-color: #f0f3f7;
  padding: 12rpx 28rpx;
  border-radius: 40rpx;
  display: inline-flex;
  align-items: center;
}

.hot-tag {
  background-color: #fff5e6;
  color: #e67e22;
  position: relative;
  padding-right: 48rpx;
}

.hot-badge {
  position: absolute;
  right: 8rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 22rpx;
  color: #ffffff;
  background-color: #e74c3c;
  padding: 4rpx 8rpx;
  border-radius: 20rpx;
  line-height: 1;
}

/* 主体内容 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background-color: #f8f9fc;
}

.category-sidebar {
  width: 200rpx;
  background-color: #ffffff;
  border-right: 2rpx solid #ecf0f1;
  overflow-y: auto;
}

.category-item {
  padding: 32rpx 0;
  text-align: center;
  font-size: 28rpx;
  color: #34495e;
  position: relative;
  transition: background-color 0.2s;
}

.category-item.active {
  background-color: #ebf5fb;
  color: #2980b9;
  font-weight: 500;
}

.category-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 40rpx;
  background-color: #3498db;
  border-radius: 0 6rpx 6rpx 0;
}

.category-name {
  display: block;
  line-height: 1.4;
}

.book-list {
  flex: 1;
  padding: 20rpx 24rpx;
  overflow-y: auto;
}

.loading-tip,
.empty-tip {
  text-align: center;
  padding: 40rpx 0;
  color: #7f8c8d;
  font-size: 28rpx;
}

/* 书籍项样式 */
.book-item {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f0f3f7;
}

.book-item:last-child {
  border-bottom: none;
}

.book-cover {
  width: 130rpx;
  height: 170rpx;
  background-color: #ecf0f1;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.book-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.book-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.3;
}

.book-author {
  font-size: 26rpx;
  color: #7f8c8d;
  margin: 6rpx 0 12rpx;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 12rpx;
}

.price {
  font-size: 26rpx;
  color: #e67e22;
  font-weight: 500;
  background-color: #fef5e7;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
}

.annotations {
  font-size: 24rpx;
  color: #3498db;
  background-color: #ebf5fb;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
}

.book-tags {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.tag {
  font-size: 22rpx;
  color: #16a085;
  background-color: #e8f6f3;
  padding: 4rpx 18rpx;
  border-radius: 30rpx;
  border: 1rpx solid #b3e5dc;
}

.bottom-safe {
  height: 20rpx;
  background-color: transparent;
  flex-shrink: 0;
}
</style>