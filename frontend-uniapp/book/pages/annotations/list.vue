<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }"
    >
      <view class="header-inner">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
        </view>
        <text class="header-title">{{ bookTitle || textMap.defaultTitle }}</text>
        <view v-if="canAddAnnotation" class="add-btn" @click="goToCreate">{{ textMap.add }}</view>
        <view v-else class="header-placeholder"></view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="hero-card">
        <text class="hero-title">{{ heroTitle }}</text>
        <text class="hero-sub">{{ totalAnnotations }} {{ heroSubText }}</text>
      </view>

      <view class="page-nav-card">
        <scroll-view class="page-nav-scroll" scroll-x show-scrollbar="false" enable-flex>
          <view class="page-nav-row">
            <view
              v-for="item in pageNavItems"
              :key="item.page"
              class="page-chip"
              :class="{ active: selectedPage === item.page }"
              @click="selectedPage = item.page"
            >
              <text class="page-chip-title">{{ item.all ? '全部' : textMap.pagePrefix + item.page + textMap.pageSuffix }}</text>
              <text class="page-chip-count">{{ item.count }} {{ textMap.countUnit }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="list-card">
        <view v-if="currentPageAnnotations.length === 0" class="empty-block">
          <text class="empty-text">{{ textMap.empty }}</text>
        </view>
        <view v-else class="annotation-item" v-for="item in currentPageAnnotations" :key="item.id">
          <view class="annotation-top">
            <view class="type-pill" :class="item.type">
              <text class="type-icon">{{ typeMap[item.type].icon }}</text>
              <text class="type-label">{{ typeMap[item.type].label }}</text>
            </view>
            <text class="time">{{ item.createdAt }}</text>
          </view>
          <text v-if="item.positionText" class="position-text">{{ textMap.positionPrefix }}{{ item.positionText }}</text>
          <text class="content-text">{{ item.content }}</text>
          <image v-if="item.imageUrl" class="annotation-image" :src="item.imageUrl" mode="aspectFill" @click="previewAnnotation(item)"></image>
          <view class="annotation-bottom">
            <text class="creator">{{ item.anonymous ? textMap.anonymous : item.nickname }}</text>
            <view class="action-row">
              <text class="view-image-link" @click="previewAnnotation(item)">
                {{ item.imageUrl ? textMap.viewImage : textMap.noImage }}
              </text>
              <view class="like-btn" :class="{ liked: item.liked }" @click="toggleLike(item)">
                <text class="like-icon">{{ item.liked ? '♥' : '♡' }}</text>
                <text class="like-count">{{ item.likeCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view style="height: 40rpx"></view>
    </view>
  </view>
</template>

<script>
import { getAnnotationList, toggleAnnotationLike } from '../../utils/api/annotation';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

function normalizeType(type) {
  if (type === 1 || type === '1' || type === 'highlight') return 'highlight';
  if (type === 2 || type === '2' || type === 'question') return 'question';
  if (type === 3 || type === '3' || type === 'insight') return 'insight';
  return 'highlight';
}

function normalizeAnnotationItem(item, index) {
  return {
    id: item.id || `annotation-${index + 1}`,
    page: Number(item.page || 1),
    type: normalizeType(item.type),
    content: item.content || '',
    positionText: item.positionText || '',
    imageUrl: item.imageUrl || '',
    nickname: item.nickname || item.creatorName || '书友',
    anonymous: Boolean(item.anonymous),
    createdAt: item.createdAt || item.createTime || '',
    likeCount: Number(item.likeCount || 0),
    commentCount: Number(item.commentCount || 0),
    liked: Boolean(item.liked)
  };
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      bookId: '',
      bookTitle: '',
      mineOnly: false,
      canAddAnnotation: false,
      selectedPage: 1,
      annotations: [],
      pageNavItems: [],
      textMap: {
        defaultTitle: '书籍批注',
        add: '添加批注',
        heroTitle: '批注笔记',
        heroSub: '条批注，按页查看',
        pagePrefix: '第 ',
        pageSuffix: ' 页',
        countUnit: '条',
        empty: '当前页暂时无批注',
        positionPrefix: '位置：',
        anonymous: '匿名用户',
        viewImage: '点击查看图片',
        noImage: '此批注无图片展示'
      },
      typeMap: {
        highlight: { label: '重点', icon: '⭐' },
        question: { label: '疑问', icon: '❓' },
        insight: { label: '心得', icon: '💡' }
      }
    };
  },
  computed: {
    heroTitle() {
      return this.mineOnly ? '我的批注笔记' : this.textMap.heroTitle;
    },
    heroSubText() {
      return this.mineOnly ? '条我的批注，按页查看' : this.textMap.heroSub;
    },
    totalAnnotations() {
      return this.annotations.length;
    },
    currentPageAnnotations() {
      if (!this.selectedPage) return this.annotations;
      return this.annotations.filter((item) => item.page === this.selectedPage);
    }
  },
  async onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule =
      typeof uni.getMenuButtonBoundingClientRect === 'function'
        ? uni.getMenuButtonBoundingClientRect()
        : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }

    this.bookId = decodeURIComponent(options.bookId || '');
    this.bookTitle = decodeURIComponent(options.bookTitle || '');
    this.mineOnly = options.mineOnly === '1' || options.mineOnly === 'true';
    await this.fetchAnnotations();
  },
  onShow() {
    this.fetchAnnotations();
  },
  methods: {
    async fetchAnnotations() {
      if (!this.bookId) return;
      try {
        const data = await getAnnotationList(this.bookId, { mineOnly: this.mineOnly });
        this.bookTitle = data.bookTitle || this.bookTitle;
        this.canAddAnnotation = Boolean(data.canAdd);
        this.annotations = Array.isArray(data.annotations)
          ? data.annotations.map((item, index) => normalizeAnnotationItem(item, index))
          : [];
        const pageItems = Array.isArray(data.pageNavItems) && data.pageNavItems.length
          ? data.pageNavItems
          : this.buildPageNavFromAnnotations();
        this.pageNavItems = [{ page: 0, count: this.annotations.length, all: true }, ...pageItems];
        this.selectedPage = 0;
      } catch (error) {
        console.error('fetchAnnotations failed', error);
      }
    },
    buildPageNavFromAnnotations() {
      const counter = {};
      this.annotations.forEach((item) => {
        counter[item.page] = (counter[item.page] || 0) + 1;
      });
      return Object.keys(counter)
        .map((page) => ({ page: Number(page), count: counter[page] }))
        .sort((a, b) => a.page - b.page);
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    goToCreate() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({
        url: `/pages/annotations/create?bookId=${encodeURIComponent(this.bookId)}&bookTitle=${encodeURIComponent(this.bookTitle)}&page=${encodeURIComponent(this.selectedPage || 1)}`
      });
    },
    async toggleLike(item) {
      try {
        const data = await toggleAnnotationLike(item.id);
        item.liked = Boolean(data?.liked);
        item.likeCount = Number(data?.likeCount || 0);
      } catch (error) {
        console.error('toggleAnnotationLike failed', error);
      }
    },
    previewAnnotation(item) {
      if (!item.imageUrl) {
        uni.showToast({ title: this.textMap.noImage, icon: 'none' });
        return;
      }
      uni.previewImage({
        urls: [item.imageUrl],
        current: item.imageUrl
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 40; box-sizing: border-box; padding-left: 20rpx; background: rgba(243, 245, 248, 0.96); backdrop-filter: blur(10px); }
.header-inner { height: 100%; display: flex; align-items: center; gap: 14rpx; padding-bottom: 12rpx; box-sizing: border-box; }
.back-btn { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.add-btn, .header-placeholder { width: 120rpx; height: 72rpx; flex-shrink: 0; }
.add-btn { border-radius: 16rpx; background: #173b75; color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.content { padding: 12rpx 20rpx 0; }
.hero-card { background: linear-gradient(135deg, #1f5eff 0%, #143a7b 100%); border-radius: 24rpx; padding: 24rpx; color: #ffffff; }
.hero-title { display: block; font-size: 34rpx; font-weight: 700; }
.hero-sub { margin-top: 8rpx; display: block; font-size: 24rpx; opacity: 0.92; }
.page-nav-card, .list-card { margin-top: 16rpx; background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.page-nav-scroll { white-space: nowrap; }
.page-nav-row { display: inline-flex; gap: 12rpx; }
.page-chip { min-width: 148rpx; border-radius: 16rpx; background: #f3f7fb; padding: 14rpx 16rpx; }
.page-chip.active { background: #e8efff; }
.page-chip-title { display: block; font-size: 24rpx; color: #173b75; font-weight: 700; }
.page-chip-count { display: block; margin-top: 6rpx; font-size: 22rpx; color: #7b8ea1; }
.empty-block { padding: 48rpx 0; text-align: center; }
.empty-text { color: #91a0b1; font-size: 24rpx; }
.annotation-item { padding: 18rpx 0; border-bottom: 1rpx solid #edf1f5; }
.annotation-item:last-child { border-bottom: none; }
.annotation-top, .annotation-bottom { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.type-pill { display: inline-flex; align-items: center; gap: 6rpx; padding: 8rpx 14rpx; border-radius: 999rpx; background: #f3f7fb; color: #4f6479; font-size: 22rpx; }
.type-pill.highlight { background: #fff5dd; color: #c98800; }
.type-pill.question { background: #eef3ff; color: #3159c7; }
.type-pill.insight { background: #e9f8ed; color: #2d7f54; }
.time, .creator { font-size: 22rpx; color: #7b8ea1; }
.position-text { display: block; margin-top: 10rpx; font-size: 22rpx; color: #6a7e94; }
.content-text { display: block; margin-top: 12rpx; font-size: 26rpx; color: #2b3f53; line-height: 1.7; }
.annotation-image { width: 100%; height: 280rpx; margin-top: 12rpx; border-radius: 18rpx; background: #eef2f8; }
.action-row { display: flex; align-items: center; gap: 18rpx; }
.view-image-link { font-size: 22rpx; color: #1f5eff; }
.like-btn { display: inline-flex; align-items: center; gap: 6rpx; padding: 8rpx 14rpx; border-radius: 999rpx; background: #f4f7fb; color: #7b8ea1; }
.like-btn.liked { background: #ffecef; color: #da4a67; }
.like-icon { font-size: 24rpx; }
.like-count { font-size: 22rpx; }
</style>
