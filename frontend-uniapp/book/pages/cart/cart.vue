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

    <view class="content">
      <view v-if="loading" class="state">{{ texts.loading }}</view>
      <view v-else-if="loadError" class="state">
        <text>{{ texts.loadError }}</text>
        <view class="retry-btn" @click="fetchCart">{{ texts.retry }}</view>
      </view>
      <view v-else-if="items.length" class="list">
        <view v-for="item in items" :key="item.id" class="card" :class="{ invalid: !item.available }">
          <view class="check" :class="{ checked: selectedMap[item.id], disabled: !item.available }" @click="toggleItem(item)">
            <text v-if="selectedMap[item.id]">&#10003;</text>
          </view>
          <image class="cover" :src="item.bookCover || '/static/logo.png'" mode="aspectFill"></image>
          <view class="main">
            <text class="title">{{ item.bookTitle }}</text>
            <text class="seller">{{ texts.seller }}{{ item.sellerName }}</text>
            <text v-if="!item.available" class="invalid-text">{{ item.invalidReason || texts.invalid }}</text>
            <view class="bottom-row">
              <text class="price">{{ texts.currency }}{{ item.price }}</text>
              <text class="remove" @click.stop="removeOne(item)">{{ texts.remove }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="state">{{ texts.empty }}</view>
    </view>

    <view class="bottom-bar">
      <view class="select-all" @click="toggleAll">
        <view class="small-check" :class="{ checked: allSelected }"><text v-if="allSelected">&#10003;</text></view>
        <text>{{ texts.selectAll }}</text>
      </view>
      <view class="summary">
        <text class="count">{{ selectedCount }}{{ texts.selectedUnit }}</text>
        <text class="amount">{{ texts.currency }}{{ totalAmount }}</text>
      </view>
      <view class="checkout-btn" :class="{ disabled: !selectedCount }" @click="goCheckout">{{ texts.checkout }}</view>
    </view>
  </view>
</template>

<script>
import { getCartItems, removeCartItems } from '../../utils/api/cart';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      loading: false,
      loadError: false,
      items: [],
      selectedMap: {},
      texts: {
        title: '\u8d2d\u7269\u8f66',
        loading: '\u52a0\u8f7d\u4e2d...',
        loadError: '\u8d2d\u7269\u8f66\u52a0\u8f7d\u5931\u8d25',
        retry: '\u91cd\u8bd5',
        empty: '\u8d2d\u7269\u8f66\u6682\u65e0\u4e66\u7c4d',
        seller: '\u5356\u5bb6\uff1a',
        invalid: '\u4e66\u7c4d\u4e0d\u53ef\u8d2d\u4e70',
        remove: '\u79fb\u9664',
        selectAll: '\u5168\u9009',
        selectedUnit: ' \u4ef6',
        checkout: '\u53bb\u7ed3\u7b97',
        currency: '\uffe5',
        removeSuccess: '\u5df2\u79fb\u9664'
      }
    };
  },
  computed: {
    availableItems() {
      return this.items.filter((item) => item.available);
    },
    selectedItems() {
      return this.availableItems.filter((item) => this.selectedMap[item.id]);
    },
    selectedCount() {
      return this.selectedItems.length;
    },
    totalAmount() {
      return this.selectedItems.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2);
    },
    allSelected() {
      return this.availableItems.length > 0 && this.availableItems.every((item) => this.selectedMap[item.id]);
    }
  },
  onLoad() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.fetchCart();
  },
  onShow() {
    if (this.headerHeight) this.fetchCart();
  },
  methods: {
    async fetchCart() {
      this.loading = true;
      this.loadError = false;
      try {
        this.items = (await getCartItems()) || [];
        const nextSelected = {};
        this.items.forEach((item) => {
          if (item.available && this.selectedMap[item.id]) {
            nextSelected[item.id] = true;
          }
        });
        this.selectedMap = nextSelected;
      } catch (error) {
        this.loadError = true;
        console.error('getCartItems failed', error);
      } finally {
        this.loading = false;
      }
    },
    toggleItem(item) {
      if (!item.available) return;
      this.selectedMap = { ...this.selectedMap, [item.id]: !this.selectedMap[item.id] };
    },
    toggleAll() {
      if (this.allSelected) {
        this.selectedMap = {};
        return;
      }
      const selected = {};
      this.availableItems.forEach((item) => {
        selected[item.id] = true;
      });
      this.selectedMap = selected;
    },
    async removeOne(item) {
      try {
        await removeCartItems([item.id]);
        uni.showToast({ title: this.texts.removeSuccess, icon: 'success' });
        await this.fetchCart();
      } catch (error) {
        console.error('removeCartItems failed', error);
      }
    },
    goCheckout() {
      if (!this.selectedCount) return;
      const ids = this.selectedItems.map((item) => item.id).join(',');
      uni.navigateTo({ url: `/pages/cart/checkout?cartItemIds=${encodeURIComponent(ids)}` });
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 260rpx, #eef3fb 100%); }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: rgba(255, 255, 255, 0.94); display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #dfe8f4; box-shadow: 0 10rpx 28rpx rgba(23, 32, 51, 0.06); }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 18rpx; background: #e8efff; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 16rpx 20rpx 150rpx; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { display: flex; gap: 16rpx; align-items: center; background: #ffffff; border-radius: 28rpx; padding: 20rpx; border: 1rpx solid #e2eaf5; box-shadow: 0 16rpx 36rpx rgba(23, 32, 51, 0.07); }
.card.invalid { opacity: 0.62; }
.check, .small-check { border-radius: 50%; border: 2rpx solid #c5d0dc; display: flex; align-items: center; justify-content: center; color: #ffffff; }
.check { width: 42rpx; height: 42rpx; flex-shrink: 0; }
.small-check { width: 34rpx; height: 34rpx; }
.check.checked, .small-check.checked { background: #1f5eff; border-color: #1f5eff; }
.check.disabled { background: #eef2f8; }
.cover { width: 128rpx; height: 150rpx; border-radius: 14rpx; background: #e8edf3; flex-shrink: 0; }
.main { flex: 1; min-width: 0; }
.title { display: block; font-size: 28rpx; color: #243548; font-weight: 700; line-height: 1.45; }
.seller, .invalid-text { display: block; margin-top: 8rpx; font-size: 22rpx; color: #75889d; }
.invalid-text { color: #c85a3b; }
.bottom-row { margin-top: 16rpx; display: flex; align-items: center; justify-content: space-between; }
.price { color: #f59e0b; font-size: 30rpx; font-weight: 700; }
.remove { color: #7a8da2; font-size: 24rpx; }
.state { margin-top: 160rpx; display: flex; flex-direction: column; gap: 20rpx; align-items: center; color: #7d8fa2; font-size: 28rpx; }
.retry-btn { height: 64rpx; padding: 0 32rpx; border-radius: 18rpx; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; display: flex; align-items: center; justify-content: center; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.97); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 18rpx; }
.select-all { display: flex; align-items: center; gap: 8rpx; color: #52677f; font-size: 24rpx; }
.summary { flex: 1; display: flex; flex-direction: column; align-items: flex-end; gap: 4rpx; }
.count { color: #7a8da2; font-size: 22rpx; }
.amount { color: #f59e0b; font-size: 30rpx; font-weight: 700; }
.checkout-btn { width: 176rpx; height: 76rpx; border-radius: 20rpx; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: 700; }
.checkout-btn.disabled { background: #bfc8d6; }
</style>
