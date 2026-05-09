<template>
  <view class="page">
    <view class="hero">
      <swiper class="swiper" circular indicator-dots autoplay interval="3000" duration="500">
        <swiper-item v-for="(image, index) in book.images" :key="index">
          <image class="swiper-image" :src="image" mode="aspectFill"></image>
        </swiper-item>
      </swiper>

      <view class="top-bar" :style="{ paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }">
        <view class="circle-btn" @click="goBack">
          <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
        </view>
        <view class="top-actions">
          <view class="circle-btn" @click="handleShare">
            <text class="icon-text">{{ texts.share }}</text>
          </view>
          <view class="circle-btn" @click="toggleFavoriteAction">
            <text class="icon-text">{{ isFavorite ? texts.favoritedShort : texts.favorite }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content">
      <view class="card info-card">
        <text class="book-title">{{ book.title }}</text>
        <text class="book-author">{{ book.author }}</text>

        <view class="meta-row">
          <text class="meta-label">{{ texts.publisher }}</text>
          <text class="meta-value">{{ book.publisher }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-label">ISBN</text>
          <text class="meta-value">{{ book.isbn }}</text>
        </view>

        <view class="tags">
          <text class="tag">{{ book.conditionLabel }}</text>
          <text class="tag">{{ book.isSold ? texts.sold : texts.onSale }}</text>
          <text class="tag">{{ book.category }}</text>
        </view>

        <view class="price-line">
          <text class="price-symbol">{{ texts.currency }}</text>
          <text class="price-text">{{ priceText }}</text>
        </view>
      </view>

      <view class="card seller-card">
        <view class="seller-left">
          <image class="seller-avatar" :src="book.seller.avatar" mode="aspectFill"></image>
          <view class="seller-info">
            <text class="seller-name">{{ book.seller.name }}</text>
            <view class="seller-stars">
              <text v-for="(star, index) in stars" :key="index" class="star">{{ star ? texts.starFull : texts.starEmpty }}</text>
              <text class="score">{{ book.seller.score.toFixed(1) }}</text>
            </view>
          </view>
        </view>
        <view class="contact-btn" @click="contactSeller">{{ texts.contactSeller }}</view>
      </view>

      <view class="card">
        <text class="section-title">{{ texts.featureTitle }}</text>
        <view class="feature-grid">
          <view class="feature-card" @click="goToAnnotations">
            <text class="feature-title">{{ texts.annotations }}</text>
            <text class="feature-value">{{ book.annotationCount }}</text>
            <text class="feature-desc">{{ texts.annotationUnit }}</text>
          </view>
          <view class="feature-card" @click="goToResources">
            <text class="feature-title">{{ texts.resources }}</text>
            <text class="feature-value">{{ book.resourceCount }}</text>
            <text class="feature-desc">{{ texts.resourceUnit }}</text>
          </view>
        </view>
        <view v-if="book.learningPaths.length" class="path-block">
          <text class="path-title">{{ texts.learningPaths }}</text>
          <view class="path-item" v-for="path in book.learningPaths" :key="path.id" @click="goToPath(path)">
            <view class="path-main">
              <text class="path-name">{{ path.name }}</text>
              <text class="path-intro">{{ path.intro }}</text>
            </view>
            <text class="path-extra">{{ path.learners }}{{ texts.learners }}</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section-title">{{ texts.description }}</text>
        <text class="desc-text">{{ book.description }}</text>
        <text class="remark-title">{{ texts.remark }}</text>
        <text class="remark-text">{{ book.remark }}</text>
      </view>
    </view>

    <view class="bottom-space"></view>
    <view class="action-bar">
      <view class="collect-btn" @click="toggleFavoriteAction">{{ isFavorite ? texts.favorited : texts.favorite }}</view>
      <view class="cart-btn" :class="{ sold: book.isSold }" @click="handleAddCart">{{ texts.addCart }}</view>
      <view class="buy-btn" :class="{ sold: book.isSold }" @click="handleBuy">{{ book.isSold ? texts.sold : texts.buyNow }}</view>
    </view>
  </view>
</template>

<script>
import { buildBookDetail, toPriceText } from '../../utils/book-detail';
import { getBookDetail } from '../../utils/api/book';
import { getAnnotationList } from '../../utils/api/annotation';
import { getFavoriteStatus, toggleFavorite } from '../../utils/api/favorite';
import { addCartItem } from '../../utils/api/cart';
import { recordBrowseHistory } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl, hasValidSession } from '../../utils/auth';

function normalizeId(value) {
  const text = String(value == null ? '' : value).trim();
  return /^\d+$/.test(text) ? text : '';
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerRightSafe: 24,
      book: buildBookDetail(),
      isFavorite: false,
      texts: {
        share: '\u5206\u4eab',
        favorite: '\u6536\u85cf',
        favorited: '\u5df2\u6536\u85cf',
        favoritedShort: '\u5df2\u85cf',
        publisher: '\u51fa\u7248\u793e',
        onSale: '\u5728\u552e',
        sold: '\u5df2\u552e',
        currency: '\u00a5',
        starFull: '\u2605',
        starEmpty: '\u2606',
        contactSeller: '\u8054\u7cfb\u5356\u5bb6',
        featureTitle: '\u7279\u8272\u529f\u80fd',
        annotations: '\u67e5\u770b\u6279\u6ce8',
        resources: '\u914d\u5957\u8d44\u6e90',
        annotationUnit: '\u6761\u6279\u6ce8',
        resourceUnit: '\u4efd\u8d44\u6e90',
        learningPaths: '\u5b66\u4e60\u8def\u5f84',
        learners: '\u4eba\u5728\u5b66',
        description: '\u4e66\u7c4d\u63cf\u8ff0',
        remark: '\u5907\u6ce8',
        buyNow: '\u7acb\u5373\u8d2d\u4e70',
        addCart: '\u52a0\u8d2d',
        addCartSuccess: '\u5df2\u52a0\u5165\u8d2d\u7269\u8f66',
        soldToast: '\u8be5\u4e66\u5df2\u552e',
        shareDev: '\u5206\u4eab\u529f\u80fd\u5f00\u53d1\u4e2d'
      }
    };
  },
  computed: {
    priceText() {
      return toPriceText(this.book.price);
    },
    stars() {
      return [1, 2, 3, 4, 5].map((index) => this.book.seller.score >= index);
    }
  },
  onShow() {
    if (this.book.id) {
      this.fetchBookDetail();
      this.fetchFavoriteState();
    }
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
    } else {
      this.headerRightSafe = 24;
    }
    this.book = buildBookDetail(options);
    this.book.id = normalizeId(this.book.id);
    this.fetchBookDetail(options);
    this.fetchFavoriteState();
  },
  methods: {
    buildRouteUrl(options = {}) {
      const id = encodeURIComponent(this.book.id || options.id || '');
      return id ? `/pages/books/detail?id=${id}` : '/pages/books/detail';
    },
    async recordHistory(options) {
      if (!this.book.id || !hasValidSession()) return;
      try {
        await recordBrowseHistory({
          targetType: 'book',
          targetId: this.book.id,
          title: this.book.title,
          subTitle: this.book.author,
          coverUrl: this.book.cover,
          routeUrl: this.buildRouteUrl(options)
        });
      } catch (error) {
        console.error('record book history failed', error);
      }
    },
    async fetchBookDetail(options = {}) {
      if (!this.book.id) {
        await this.recordHistory(options);
        return;
      }
      try {
        const data = await getBookDetail(this.book.id);
        if (data && Object.keys(data).length) {
          this.book = buildBookDetail(data);
          this.book.id = normalizeId(this.book.id);
        }
      } catch (error) {
        console.error('getBookDetail failed', error);
        await this.recordHistory(options);
        return;
      }
      try {
        const annotationData = await getAnnotationList(this.book.id, { showError: false });
        this.book.annotationCount = Number(annotationData?.total || 0);
      } catch (error) {
        console.error('getAnnotationList failed', error);
      }
      await this.recordHistory(options);
    },
    async fetchFavoriteState() {
      if (!hasValidSession() || !this.book.id) return;
      try {
        const data = await getFavoriteStatus('book', this.book.id);
        this.isFavorite = Boolean(data?.favorited);
      } catch (error) {
        console.error('getFavoriteStatus failed', error);
      }
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    handleShare() {
      uni.showToast({ title: this.texts.shareDev, icon: 'none' });
    },
    async toggleFavoriteAction() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        const data = await toggleFavorite('book', this.book.id);
        this.isFavorite = Boolean(data?.favorited);
      } catch (error) {
        console.error('toggleFavorite failed', error);
      }
    },
    contactSeller() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      const params = [
        `sellerId=${encodeURIComponent(this.book.seller.id || '')}`,
        `sellerName=${encodeURIComponent(this.book.seller.name || '')}`,
        `bookId=${encodeURIComponent(this.book.id || '')}`,
        `bookTitle=${encodeURIComponent(this.book.title || '')}`
      ].join('&');
      uni.navigateTo({ url: `/pages/chat/chat?${params}` });
    },
    async handleAddCart() {
      if (this.book.isSold) {
        uni.showToast({ title: this.texts.soldToast, icon: 'none' });
        return;
      }
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      try {
        await addCartItem(this.book.id);
        uni.showToast({ title: this.texts.addCartSuccess, icon: 'success' });
      } catch (error) {
        console.error('addCartItem failed', error);
      }
    },
    goToAnnotations() {
      const params = [
        `bookId=${encodeURIComponent(this.book.id || '')}`,
        `bookTitle=${encodeURIComponent(this.book.title || '')}`
      ].join('&');
      uni.navigateTo({ url: `/pages/annotations/list?${params}` });
    },
    goToResources() {
      uni.navigateTo({
        url: `/pages/resources/list?bookId=${encodeURIComponent(this.book.id || '')}&title=${encodeURIComponent(this.book.title + this.texts.resources)}`
      });
    },
    goToPath(path) {
      const params = [
        `pathId=${encodeURIComponent(path.id || '')}`,
        `title=${encodeURIComponent(path.name || '')}`,
        `description=${encodeURIComponent(path.intro || '')}`,
        `creator=${encodeURIComponent((this.book.seller && this.book.seller.name) || '\u8def\u5f84\u521b\u5efa\u8005')}`,
        'isCreator=0'
      ].join('&');
      uni.navigateTo({ url: `/pages/path/detail?${params}` });
    },
    handleBuy() {
      if (this.book.isSold) {
        uni.showToast({ title: this.texts.soldToast, icon: 'none' });
        return;
      }
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({ url: `/pages/cart/checkout?bookId=${encodeURIComponent(this.book.id || '')}` });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #f1efe8 0%, #f8f9fc 24%, #f8f9fc 100%); }
.hero { position: relative; }
.swiper { height: 480rpx; }
.swiper-image { width: 100%; height: 100%; }
.top-bar { position: absolute; top: 0; left: 0; right: 0; z-index: 10; padding-left: 24rpx; box-sizing: border-box; padding-bottom: 16rpx; display: flex; justify-content: space-between; align-items: center; }
.top-actions { display: flex; gap: 12rpx; }
.circle-btn { min-width: 64rpx; height: 64rpx; border-radius: 50%; background: rgba(255, 255, 255, 0.9); display: flex; justify-content: center; align-items: center; padding: 0 12rpx; box-sizing: border-box; }
.icon-text { font-size: 22rpx; color: #2b2f3a; }
.back-icon { width: 32rpx; height: 32rpx; }
.content { padding: 20rpx 24rpx 0; }
.card { background: #ffffff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04); margin-bottom: 20rpx; }
.book-title { display: block; font-size: 36rpx; line-height: 1.4; font-weight: 700; color: #263442; }
.book-author { display: block; margin-top: 8rpx; font-size: 26rpx; color: #6f7c8e; }
.meta-row { margin-top: 12rpx; display: flex; justify-content: space-between; gap: 18rpx; }
.meta-label { font-size: 24rpx; color: #7a8797; }
.meta-value { flex: 1; text-align: right; font-size: 24rpx; color: #344759; }
.tags { margin-top: 18rpx; display: flex; flex-wrap: wrap; gap: 12rpx; }
.tag { padding: 6rpx 16rpx; border-radius: 999rpx; background: #eef3fb; font-size: 22rpx; color: #526981; }
.price-line { margin-top: 24rpx; display: flex; align-items: flex-end; color: #d05a25; }
.price-symbol { font-size: 30rpx; font-weight: 700; margin-right: 4rpx; }
.price-text { font-size: 52rpx; line-height: 1; font-weight: 700; }
.seller-card { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; }
.seller-left { display: flex; align-items: center; gap: 18rpx; min-width: 0; }
.seller-avatar { width: 92rpx; height: 92rpx; border-radius: 50%; background: #eef2f7; }
.seller-info { min-width: 0; }
.seller-name { display: block; font-size: 28rpx; color: #243548; font-weight: 700; }
.seller-stars { margin-top: 8rpx; display: flex; align-items: center; gap: 4rpx; }
.star { color: #f2b632; font-size: 22rpx; }
.score { margin-left: 8rpx; font-size: 22rpx; color: #6a7b8f; }
.contact-btn { height: 72rpx; padding: 0 24rpx; border-radius: 18rpx; background: #2f4f75; color: #ffffff; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.section-title { display: block; font-size: 30rpx; color: #263442; font-weight: 700; margin-bottom: 18rpx; }
.feature-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx; }
.feature-card { background: #f4f7fb; border-radius: 20rpx; padding: 20rpx; }
.feature-title { display: block; font-size: 26rpx; color: #3a4f66; font-weight: 700; }
.feature-value { display: block; margin-top: 18rpx; font-size: 40rpx; color: #2d55c7; font-weight: 700; }
.feature-desc { display: block; margin-top: 8rpx; font-size: 22rpx; color: #6f8094; }
.path-block { margin-top: 24rpx; }
.path-title { display: block; font-size: 28rpx; color: #2c4057; font-weight: 700; margin-bottom: 12rpx; }
.path-item { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; padding: 20rpx; border-radius: 18rpx; background: #f7f9fc; margin-top: 12rpx; }
.path-main { flex: 1; min-width: 0; }
.path-name { display: block; font-size: 26rpx; color: #2d3f53; font-weight: 700; }
.path-intro { display: block; margin-top: 8rpx; font-size: 22rpx; color: #74859a; line-height: 1.6; }
.path-extra { flex-shrink: 0; font-size: 22rpx; color: #2d55c7; }
.desc-text, .remark-text { display: block; font-size: 24rpx; color: #566a82; line-height: 1.8; }
.remark-title { display: block; margin-top: 20rpx; margin-bottom: 10rpx; font-size: 26rpx; color: #2c4057; font-weight: 700; }
.bottom-space { height: 140rpx; }
.action-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; display: flex; gap: 16rpx; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e6ebf2; }
.collect-btn, .cart-btn, .buy-btn { height: 84rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 700; }
.collect-btn { width: 170rpx; background: #edf2f8; color: #44596f; flex-shrink: 0; }
.cart-btn { width: 150rpx; background: #fff3e8; color: #c85a3b; flex-shrink: 0; }
.buy-btn { flex: 1; background: #2d55c7; color: #ffffff; }
.buy-btn.sold, .cart-btn.sold { background: #bfc8d6; color: #ffffff; }
</style>
