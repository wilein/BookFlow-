<template>
  <view class="container">
    <!-- 固定顶部区域（避开胶囊） -->
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
        <view class="search-btn" @click="goToSearch">
          <image class="search-img" src="/static/search.png" mode="aspectFit"></image>
        </view>
      </view>
      <text class="slogan">批注传承·学习路径·知识分享</text>
    </view>

    <!-- 占位视图，高度等于固定顶部高度 -->
    <view :style="{ height: fixedHeaderHeight + 'px' }"></view>

    <!-- 轮播图区域（数据来自 API） -->
    <view class="swiper-section" v-if="bannerList.length > 0">
      <swiper
        class="swiper"
        indicator-dots="true"
        autoplay="true"
        interval="3000"
        duration="500"
        circular="true"
      >
        <swiper-item v-for="(item, index) in bannerList" :key="index">
          <image :src="item.image" class="swiper-image" mode="aspectFill"></image>
        </swiper-item>
      </swiper>
    </view>

    <!-- 四个功能入口（保持不变） -->
    <view class="nav-grid">
      <view
        class="nav-item"
        v-for="(item, index) in navItems"
        :key="index"
        hover-class="nav-hover"
      >
        <text class="nav-icon">{{ item.icon }}</text>
        <text class="nav-text">{{ item.name }}</text>
      </view>
    </view>

    <!-- 热门书籍区块（数据来自 API） -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">热门书籍</text>
        <navigator class="more" url="/pages/books/hot" hover-class="none">更多></navigator>
      </view>
      <view class="book-list" v-if="hotBooks.length > 0">
        <view
          class="book-item"
          v-for="(book, index) in hotBooks"
          :key="index"
          @click="goToBookDetail(book)"
        >
          <image class="book-cover" :src="book.cover || '/static/cover_placeholder.png'" mode="aspectFill"></image>
          <view class="book-info">
            <text class="book-title">{{ book.title }}</text>
            <text class="book-author">{{ book.author }}</text>
            <view class="book-meta">
              <text class="price">￥{{ book.price }}价格</text>
              <text class="annotations">{{ book.annotations }}条批注</text>
            </view>
            <view class="book-tags">
              <text class="tag">{{ book.category }}</text>
            </view>
          </view>
        </view>
      </view>
      <!-- 加载中或空状态可自行添加 -->
    </view>

    <!-- 推荐学习路径区块（仍为静态数据，如需改为 API 可参考修改） -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">推荐学习路径</text>
        <navigator class="more" url="/pages/paths/recommend" hover-class="none">更多></navigator>
      </view>
      <view class="path-list">
        <view
          class="path-item"
          v-for="(path, index) in studyPaths"
          :key="index"
          @click="goToPathDetail(path)"
        >
          <view class="path-info">
            <text class="path-name">{{ path.name }}</text>
            <text class="path-creator">{{ path.creator }}</text>
          </view>
          <view class="path-stats">
            <text class="stat-item">{{ path.bookCount }}本书</text>
            <text class="stat-item">{{ path.learners }}人学习</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 社区动态区块（仍为静态数据） -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">社区动态</text>
        <navigator class="more" url="/pages/community/feed" hover-class="none">更多></navigator>
      </view>
      <view class="dynamic-list">
        <view
          class="dynamic-item"
          v-for="(item, index) in dynamics"
          :key="index"
          @click="goToDynamicDetail(item)"
        >
          <image class="avatar" src="/static/avatar_placeholder.png" mode="aspectFill"></image>
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
      navItems: [
        { name: '分类浏览', icon: '📚' },
        { name: '学习路径', icon: '🗺️' },
        { name: '发布书籍', icon: '📤' },
        { name: '社区动态', icon: '💬' }
      ],
      // 轮播图数据（初始为空，由 API 填充）
      bannerList: [],
      // 热门书籍数据（初始为空，由 API 填充）
      hotBooks: [],
      // 以下为静态数据，可根据需要改为 API 获取
      studyPaths: [
        { name: 'Java后端开发路线', creator: '张三学长创建', bookCount: 8, learners: 234 },
        { name: '前端工程师进阶', creator: '李四学姐创建', bookCount: 12, learners: 456 }
      ],
      dynamics: [
        { username: '王五', time: '2小时前', content: '分享一本宝藏书籍《设计模式》，学长的批注超级详细！' },
        { username: '赵六', time: '5小时前', content: '刚完成了数据结构学习路径，感谢学长推荐！' }
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

    // 调用获取数据的方法
    this.fetchBannerData();
    this.fetchHotBooks();
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
    // 获取轮播图数据
    fetchBannerData() {
      uni.showLoading({ title: '加载中...' });
      uni.request({
        url: 'http://localhost:8080/common/banner/list', // 替换为实际接口地址
        method: 'GET',
        success: (res) => {
          if (res.data.code === "200") {
			  
            this.bannerList = res.data.data; // 假设返回格式 { code:0, data: [{ image: 'url' }] }
          } else {
            uni.showToast({ title: '轮播图加载失败', icon: 'none' });
          }
        },	
        fail: (err) => {
          console.error('banner api error', err);
          uni.showToast({ title: '网络错误', icon: 'none' });
        },
        complete: () => {
          uni.hideLoading();
        }
      });
    },
    // 获取热门书籍数据
    fetchHotBooks() {
      uni.request({
        url: 'http://localhost:8080/book/list', // 替换为实际接口地址
        method: 'GET',
        success: (res) => {
          if (res.data.code === "200") {
			  console.log(res.data.data)
			  console.log(res.data.data)
            this.hotBooks = res.data.data; // 假设返回格式匹配 hotBooks 字段
          } else {
            uni.showToast({ title: '书籍加载失败', icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('hotBooks api error', err);
          uni.showToast({ title: '网络错误', icon: 'none' });
        }
      });
    },
    goToSearch() {
      uni.navigateTo({ url: '/pages/search/search' });
    },
    goToBookDetail(book) {
      uni.navigateTo({
        url: `/pages/books/detail?title=${encodeURIComponent(book.title)}`
      });
    },
    goToPathDetail(path) {
      uni.navigateTo({
        url: `/pages/paths/detail?name=${encodeURIComponent(path.name)}`
      });
    },
    goToDynamicDetail(dynamic) {
      uni.navigateTo({
        url: `/pages/community/detail?username=${encodeURIComponent(dynamic.username)}`
      });
    }
  }
};
</script>

<style scoped>
/* 隐藏右侧滚动条 */
.container::-webkit-scrollbar {
  display: none;
}
.container {
  -ms-overflow-style: none;
  scrollbar-width: none;
  background-color: #f8f9fc;
  min-height: 100vh;
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

.search-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f3f7;
  border-radius: 50%;
}

.search-img {
  width: 36rpx;
  height: 36rpx;
}

.slogan {
  font-size: 28rpx;
  color: #7f8c8d;
  display: block;
}

/* 轮播图区域 */
.swiper-section {
  margin: 32rpx 32rpx 20rpx;
  border-radius: 24rpx;
  overflow: hidden;
}

.swiper {
  width: 100%;
  height: 300rpx;
}

.swiper-image {
  width: 100%;
  height: 100%;
}

/* 四个功能导航网格 */
.nav-grid {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background-color: #ffffff;
  margin: 24rpx 32rpx;
  padding: 20rpx 0;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16rpx 0;
  border-radius: 16rpx;
  transition: background-color 0.2s;
}

.nav-hover {
  background-color: #f0f7ff;
}

.nav-icon {
  font-size: 48rpx;
  margin-bottom: 8rpx;
}

.nav-text {
  font-size: 26rpx;
  color: #34495e;
  font-weight: 500;
}

/* 通用区块样式 */
.section {
  margin: 32rpx 32rpx 40rpx;
  background-color: #ffffff;
  border-radius: 28rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.02);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid #ecf0f1;
}

.section-title {
  font-size: 34rpx;
  font-weight: 650;
  color: #2c3e50;
  position: relative;
  padding-left: 18rpx;
  border-left: 8rpx solid #3498db;
}

.more {
  font-size: 26rpx;
  color: #7f8c8d;
  padding: 8rpx 16rpx;
  background-color: #f8f9fc;
  border-radius: 30rpx;
}

/* 热门书籍列表 */
.book-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.book-item {
  display: flex;
  gap: 24rpx;
  padding: 8rpx 0;
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

/* 学习路径卡片 */
.path-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.path-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f0f3f7;
}

.path-item:last-child {
  border-bottom: none;
}

.path-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.path-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #2c3e50;
}

.path-creator {
  font-size: 26rpx;
  color: #7f8c8d;
}

.path-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
  background-color: #f4f8ff;
  padding: 16rpx 22rpx;
  border-radius: 30rpx;
}

.stat-item {
  font-size: 26rpx;
  color: #2980b9;
  font-weight: 500;
}

/* 社区动态列表 */
.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
}

.dynamic-item {
  display: flex;
  gap: 24rpx;
  padding: 16rpx 0;
  border-bottom: 2rpx solid #f0f3f7;
}

.dynamic-item:last-child {
  border-bottom: none;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: #bdc3c7;
  flex-shrink: 0;
}

.dynamic-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.dynamic-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  flex-wrap: wrap;
}

.username {
  font-size: 30rpx;
  font-weight: 600;
  color: #2c3e50;
}

.time {
  font-size: 24rpx;
  color: #95a5a6;
}

.content {
  font-size: 28rpx;
  color: #34495e;
  line-height: 1.4;
  word-break: break-word;
}

/* 底部安全区域 */
.bottom-safe {
  height: 20rpx;
  background-color: transparent;
}

@media (min-width: 768px) {
  .container {
    max-width: 700px;
    margin: 0 auto;
    box-shadow: 0 0 40rpx rgba(0,0,0,0.05);
  }
}
</style>
