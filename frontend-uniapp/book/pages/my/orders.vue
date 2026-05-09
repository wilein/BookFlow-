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

    <view class="role-row">
      <view
        v-for="item in roleTabs"
        :key="item.key"
        class="role-item"
        :class="{ active: currentRole === item.key }"
        @click="switchRole(item.key)"
      >
        {{ item.label }}
      </view>
    </view>

    <scroll-view class="status-scroll" scroll-x>
      <view class="status-row">
        <view
          v-for="item in tabs"
          :key="item.key"
          class="status-item"
          :class="{ active: currentTab === item.key }"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view class="content">
      <view v-if="loading" class="state">{{ texts.loading }}</view>
      <view v-else-if="errorMessage" class="state">
        <text class="state-text">{{ errorMessage }}</text>
        <view class="retry-btn" @click="fetchData">{{ texts.retry }}</view>
      </view>
      <view v-else-if="orders.length" class="list">
        <view v-for="item in orders" :key="item.id" class="card" @click="openDetail(item)">
          <view class="card-head">
            <text class="order-no">{{ texts.orderNo }}{{ item.orderNo }}</text>
            <text class="status">{{ item.statusLabel }}</text>
          </view>
          <view class="book-row">
            <image class="cover" :src="item.bookCover || '/static/logo.png'" mode="aspectFill"></image>
            <view class="book-main">
              <text class="title">{{ item.bookTitle }}</text>
              <text class="meta">{{ currentRole === 'seller' ? texts.buyer : texts.seller }}{{ currentRole === 'seller' ? item.buyerName : item.sellerName }}</text>
              <text class="meta">{{ texts.createTime }}{{ item.createTime }}</text>
            </view>
          </view>
          <view class="info-row">
            <text class="label">{{ texts.receiverInfo }}</text>
            <text class="value">{{ item.receiverName || texts.notFilled }} {{ item.receiverPhone || '' }}</text>
          </view>
          <view class="info-row">
            <text class="label">{{ texts.receiverAddress }}</text>
            <text class="value address">{{ item.receiverAddress || texts.notFilled }}</text>
          </view>
          <view class="footer-row">
            <text class="message">{{ item.buyerMessage || texts.noBuyerMessage }}</text>
            <text class="amount">{{ texts.currency }}{{ item.totalAmount }}</text>
          </view>
          <view v-if="buildActions(item).length" class="action-row">
            <view
              v-for="action in buildActions(item)"
              :key="action.key"
              class="action-btn"
              :class="action.type"
              @click.stop="handleAction(action.key, item)"
            >
              {{ action.label }}
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>
  </view>
</template>

<script>
import { cancelOrder, confirmReceipt, payOrder, shipOrder } from '../../utils/api/order';
import { getMyOrders } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TEXTS = {
  title: '\u6211\u7684\u8ba2\u5355',
  buyerRole: '\u6211\u662f\u4e70\u5bb6',
  sellerRole: '\u6211\u662f\u5356\u5bb6',
  all: '\u5168\u90e8',
  pendingPay: '\u5f85\u4ed8\u6b3e',
  pendingShip: '\u5f85\u53d1\u8d27',
  pendingReceive: '\u5f85\u6536\u8d27',
  finished: '\u5df2\u5b8c\u6210',
  canceled: '\u5df2\u53d6\u6d88',
  refunding: '\u552e\u540e\u4e2d',
  orderNo: '\u8ba2\u5355\u53f7\uff1a',
  seller: '\u5356\u5bb6\uff1a',
  buyer: '\u4e70\u5bb6\uff1a',
  createTime: '\u521b\u5efa\u65f6\u95f4\uff1a',
  receiverInfo: '\u6536\u8d27\u4fe1\u606f',
  receiverAddress: '\u6536\u8d27\u5730\u5740',
  notFilled: '\u672a\u586b\u5199',
  noBuyerMessage: '\u65e0\u4e70\u5bb6\u7559\u8a00',
  empty: '\u6682\u65e0\u8ba2\u5355',
  cancelOrder: '\u53d6\u6d88\u8ba2\u5355',
  pay: '\u53bb\u4ed8\u6b3e',
  confirmReceipt: '\u786e\u8ba4\u6536\u8d27',
  shipOrder: '\u53bb\u53d1\u8d27',
  canceledSuccess: '\u5df2\u53d6\u6d88\u8ba2\u5355',
  paySuccess: '\u652f\u4ed8\u6210\u529f',
  receiveSuccess: '\u5df2\u786e\u8ba4\u6536\u8d27',
  shipSuccess: '\u5df2\u53d1\u8d27',
  loading: '\u52a0\u8f7d\u4e2d...',
  retry: '\u91cd\u8bd5',
  loadFailed: '\u8ba2\u5355\u52a0\u8f7d\u5931\u8d25',
  currency: '\uffe5'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      currentTab: 'all',
      currentRole: 'buyer',
      roleTabs: [
        { key: 'buyer', label: TEXTS.buyerRole },
        { key: 'seller', label: TEXTS.sellerRole }
      ],
      tabs: [
        { key: 'all', label: TEXTS.all },
        { key: '1', label: TEXTS.pendingPay },
        { key: '2', label: TEXTS.pendingShip },
        { key: '3', label: TEXTS.pendingReceive },
        { key: '4', label: TEXTS.finished },
        { key: '5', label: TEXTS.canceled },
        { key: '6', label: TEXTS.refunding }
      ],
      orders: [],
      loading: false,
      errorMessage: ''
    };
  },
  onLoad(options) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.status) this.currentTab = String(options.status);
    if (options.role) this.currentRole = String(options.role) === 'seller' ? 'seller' : 'buyer';
  },
  onShow() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    this.fetchData();
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.errorMessage = '';
      try {
        this.orders = (await getMyOrders(this.currentTab, this.currentRole)) || [];
      } catch (error) {
        console.error('getMyOrders failed', error);
        this.errorMessage = error?.message || TEXTS.loadFailed;
      } finally {
        this.loading = false;
      }
    },
    buildActions(item) {
      const actions = [];
      if (item.canCancel) {
        actions.push({ key: 'cancel', label: TEXTS.cancelOrder, type: 'secondary' });
      }
      if (item.canPay) {
        actions.push({ key: 'pay', label: TEXTS.pay, type: 'primary' });
      }
      if (item.canShip) {
        actions.push({ key: 'ship', label: TEXTS.shipOrder, type: 'primary' });
      }
      if (item.canConfirm) {
        actions.push({ key: 'receive', label: TEXTS.confirmReceipt, type: 'primary' });
      }
      return actions;
    },
    async handleAction(action, item) {
      try {
        if (action === 'cancel') {
          await cancelOrder(item.id);
          uni.showToast({ title: TEXTS.canceledSuccess, icon: 'success' });
        } else if (action === 'pay') {
          await payOrder(item.id);
          uni.showToast({ title: TEXTS.paySuccess, icon: 'success' });
        } else if (action === 'receive') {
          await confirmReceipt(item.id);
          uni.showToast({ title: TEXTS.receiveSuccess, icon: 'success' });
        } else if (action === 'ship') {
          await shipOrder(item.id);
          uni.showToast({ title: TEXTS.shipSuccess, icon: 'success' });
        }
        this.fetchData();
      } catch (error) {
        console.error('handle order action failed', error);
      }
    },
    openDetail(item) {
      uni.navigateTo({ url: `/pages/my/order-detail?orderId=${encodeURIComponent(item.id || '')}&role=${encodeURIComponent(this.currentRole)}` });
    },
    switchTab(tab) {
      if (this.currentTab === tab) return;
      this.currentTab = tab;
      this.fetchData();
    },
    switchRole(role) {
      if (this.currentRole === role) return;
      this.currentRole = role;
      this.fetchData();
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
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
.role-row { padding: 16rpx 20rpx 0; display: flex; gap: 12rpx; }
.role-item { flex: 1; height: 72rpx; border-radius: 18rpx; background: #ffffff; color: #60758a; font-size: 26rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.role-item.active { background: #2d55c7; color: #ffffff; }
.status-scroll { white-space: nowrap; }
.status-row { display: inline-flex; gap: 12rpx; padding: 16rpx 20rpx 0; }
.status-item { padding: 16rpx 24rpx; border-radius: 999rpx; background: #ffffff; color: #60758a; font-size: 24rpx; }
.status-item.active { background: #2d55c7; color: #ffffff; font-weight: 700; }
.content { padding: 16rpx 20rpx calc(32rpx + env(safe-area-inset-bottom)); }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 18rpx; }
.card-head, .info-row, .footer-row { display: flex; align-items: center; justify-content: space-between; gap: 14rpx; }
.order-no { font-size: 22rpx; color: #7b8ea1; }
.status { font-size: 24rpx; color: #2d55c7; font-weight: 700; }
.book-row { margin-top: 14rpx; display: flex; gap: 16rpx; }
.cover { width: 132rpx; height: 156rpx; border-radius: 14rpx; background: #e8edf3; flex-shrink: 0; }
.book-main { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.title { font-size: 28rpx; line-height: 1.5; color: #243548; font-weight: 700; }
.meta { font-size: 22rpx; color: #708399; }
.info-row { margin-top: 12rpx; align-items: flex-start; }
.label { flex-shrink: 0; font-size: 22rpx; color: #7b8ea1; }
.value { flex: 1; text-align: right; font-size: 22rpx; color: #43576e; }
.value.address { line-height: 1.5; }
.footer-row { margin-top: 14rpx; padding-top: 14rpx; border-top: 1rpx solid #edf1f5; }
.message { flex: 1; font-size: 22rpx; color: #7b8ea1; }
.amount { color: #d05a25; font-size: 32rpx; font-weight: 700; }
.action-row { margin-top: 16rpx; display: flex; gap: 12rpx; justify-content: flex-end; flex-wrap: wrap; }
.action-btn { min-width: 156rpx; height: 68rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; }
.action-btn.primary { background: #2d55c7; color: #ffffff; }
.action-btn.secondary { background: #eef2f8; color: #52677f; }
.empty, .state { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
.state { display: flex; flex-direction: column; align-items: center; gap: 20rpx; }
.state-text { display: block; color: #6d8095; font-size: 26rpx; }
.retry-btn { min-width: 156rpx; height: 64rpx; padding: 0 24rpx; border-radius: 16rpx; background: #2d55c7; color: #ffffff; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
