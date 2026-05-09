<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <view class="header-main">
        <text class="seller-name">{{ peerName }}</text>
        <text class="book-name">{{ texts.bookWrapStart }}{{ currentBookTitle }}{{ texts.bookTrading }}</text>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <scroll-view class="chat-body" scroll-y :scroll-into-view="scrollAnchor" scroll-with-animation>
      <view class="trade-card" @click="openOrderDetail">
        <image class="trade-cover" :src="sessionMeta.bookCover || '/static/logo.png'" mode="aspectFill"></image>
        <view class="trade-main">
          <view class="trade-top">
            <text class="trade-role">{{ roleLabel }}</text>
            <text class="trade-status">{{ currentStatusLabel }}</text>
          </view>
          <text class="trade-title">{{ currentBookTitle }}</text>
          <text class="trade-meta">{{ texts.sellerLabel }}{{ sessionMeta.sellerName || sellerName || texts.sellerFallback }}</text>
          <text class="trade-meta">{{ texts.buyerLabel }}{{ sessionMeta.buyerName || texts.buyerFallback }}</text>
          <text class="trade-meta" v-if="sessionMeta.orderNo">{{ texts.orderNoLabel }}{{ sessionMeta.orderNo }}</text>
          <text class="trade-price">{{ texts.currency }}{{ formatPrice(sessionMeta.bookPrice) }}</text>
          <view class="trade-actions" v-if="primaryAction || secondaryAction">
            <view
              v-if="secondaryAction"
              class="action-btn secondary"
              @click.stop="handleTradeAction(secondaryAction.key)"
            >
              {{ secondaryAction.label }}
            </view>
            <view
              v-if="primaryAction"
              class="action-btn primary"
              @click.stop="handleTradeAction(primaryAction.key)"
            >
              {{ primaryAction.label }}
            </view>
          </view>
        </view>
      </view>

      <view class="status-card">
        <text class="status-title">{{ texts.orderStatusTitle }}</text>
        <text class="status-desc">{{ texts.orderStatusDesc }}</text>
      </view>

      <view v-if="loading" class="state">{{ texts.loading }}</view>
      <view v-else-if="errorMessage" class="state">
        <text class="state-text">{{ errorMessage }}</text>
        <view class="retry-btn" @click="retryLoad">{{ texts.retry }}</view>
      </view>
      <view v-else-if="!messages.length" class="state">{{ texts.emptyMessages }}</view>
      <block v-else>
      <block v-for="item in messages" :key="item.id">
        <view v-if="item.mine" class="msg-row mine">
          <view class="msg-main mine">
            <view class="msg-bubble mine">{{ item.content }}</view>
            <text class="msg-time">{{ item.time }}</text>
          </view>
          <image
            class="msg-avatar"
            :src="item.senderAvatar || sessionMeta.selfAvatar || '/static/logo.png'"
            mode="aspectFill"
          ></image>
        </view>
        <view v-else class="msg-row other">
          <image
            class="msg-avatar"
            :src="item.senderAvatar || sessionMeta.avatar || '/static/logo.png'"
            mode="aspectFill"
          ></image>
          <view class="msg-main other">
            <view class="msg-bubble other">{{ item.content }}</view>
            <text class="msg-time">{{ item.time }}</text>
          </view>
        </view>
      </block>
      </block>

      <view id="chat-bottom-anchor"></view>
    </scroll-view>

    <view class="input-bar">
      <input class="input" type="text" :placeholder="texts.inputPlaceholder" v-model="draftMessage" @confirm="submitMessage" />
      <view class="send-btn" @click="submitMessage">{{ texts.send }}</view>
    </view>
  </view>
</template>

<script>
import { getChatMessages, markChatRead, openChatSession, pollChatMessages, sendChatMessage } from '../../utils/api/chat';
import {
  cancelOrder,
  confirmReceipt,
  payOrder,
  shipOrder
} from '../../utils/api/order';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TEXTS = {
  tradeTarget: '\u4ea4\u6613\u5bf9\u8c61',
  bookFallback: '\u4e66\u7c4d',
  sellerFallback: '\u5356\u5bb6',
  buyerFallback: '\u4e70\u5bb6',
  sellerLabel: '\u5356\u5bb6\uff1a',
  buyerLabel: '\u4e70\u5bb6\uff1a',
  orderNoLabel: '\u8ba2\u5355\u53f7\uff1a',
  currency: '\uffe5',
  inputPlaceholder: '\u8f93\u5165\u6d88\u606f...',
  send: '\u53d1\u9001',
  orderStatusTitle: '\u8ba2\u5355\u72b6\u6001',
  orderStatusDesc: '\u4ed8\u6b3e\u3001\u53d1\u8d27\u3001\u6536\u8d27\u90fd\u4f1a\u5728\u9876\u90e8\u5361\u7247\u540c\u6b65\u66f4\u65b0\uff0c\u804a\u5929\u53ea\u4fdd\u7559\u548c\u4ea4\u6613\u76f4\u63a5\u76f8\u5173\u7684\u4fe1\u606f\u3002',
  bookWrapStart: '\u300a',
  bookTrading: '\u300b\u4ea4\u6613\u4e2d',
  sellerRole: '\u6211\u662f\u5356\u5bb6',
  buyerRole: '\u6211\u662f\u4e70\u5bb6',
  ship: '\u53bb\u53d1\u8d27',
  confirm: '\u786e\u8ba4\u6536\u8d27',
  pay: '\u53bb\u4ed8\u6b3e',
  createOrder: '\u53bb\u4e0b\u5355',
  viewOrder: '\u67e5\u770b\u8ba2\u5355',
  cancel: '\u53d6\u6d88\u8ba2\u5355',
  paySuccess: '\u652f\u4ed8\u6210\u529f',
  cancelSuccess: '\u8ba2\u5355\u5df2\u53d6\u6d88',
  shipSuccess: '\u53d1\u8d27\u6210\u529f',
  confirmSuccess: '\u5df2\u786e\u8ba4\u6536\u8d27',
  loading: '\u52a0\u8f7d\u4e2d...',
  retry: '\u91cd\u8bd5',
  emptyMessages: '\u6682\u65e0\u6d88\u606f',
  loadFailed: '\u804a\u5929\u52a0\u8f7d\u5931\u8d25'
};

const POLL_INTERVAL_MS = 3000;

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      sellerName: '',
      bookTitle: '',
      sellerId: '',
      bookId: '',
      sessionId: '',
      draftMessage: '',
      scrollAnchor: '',
      messages: [],
      sessionMeta: {},
      loading: false,
      errorMessage: '',
      pollTimer: null,
      polling: false
    };
  },
  computed: {
    peerName() {
      return this.sessionMeta.name || this.sellerName || this.texts.tradeTarget;
    },
    currentBookTitle() {
      return this.sessionMeta.bookTitle || this.bookTitle || this.texts.bookFallback;
    },
    currentStatusLabel() {
      return this.sessionMeta.orderStatusLabel || this.sessionMeta.bookStatusLabel || '\u6c9f\u901a\u4e2d';
    },
    roleLabel() {
      return this.sessionMeta.role === 'seller' ? this.texts.sellerRole : this.texts.buyerRole;
    },
    primaryAction() {
      if (this.sessionMeta.canShip) return { key: 'ship', label: this.texts.ship };
      if (this.sessionMeta.canConfirm) return { key: 'confirm', label: this.texts.confirm };
      if (this.sessionMeta.canPay) return { key: 'pay', label: this.texts.pay };
      if (this.sessionMeta.canCreateOrder) return { key: 'createOrder', label: this.texts.createOrder };
      if (this.sessionMeta.orderId) return { key: 'viewOrder', label: this.texts.viewOrder };
      return null;
    },
    secondaryAction() {
      if (this.sessionMeta.canCancel) return { key: 'cancel', label: this.texts.cancel };
      return null;
    }
  },
  async onLoad(options) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.sellerName = decodeURIComponent(options.sellerName || '');
    this.bookTitle = decodeURIComponent(options.bookTitle || '');
    this.sellerId = decodeURIComponent(options.sellerId || '');
    this.bookId = decodeURIComponent(options.bookId || '');
    this.sessionId = decodeURIComponent(options.sessionId || '');
    await this.initSession();
  },
  async onShow() {
    if (!this.sessionId) return;
    await this.refreshSessionMeta();
    await this.fetchMessages({ silent: this.messages.length > 0 });
    this.startPolling();
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
  },
  methods: {
    applySession(session = {}) {
      this.sessionMeta = { ...this.sessionMeta, ...session };
      this.sessionId = session.sessionId || session.id || this.sessionId;
      this.sellerName = session.name || this.sellerName;
      this.bookTitle = session.bookTitle || this.bookTitle;
      this.bookId = session.bookId || this.bookId;
      this.sellerId = session.sellerId || this.sellerId;
    },
    async initSession() {
      try {
        const session = await openChatSession({
          sessionId: this.sessionId || undefined,
          sellerId: this.sellerId || undefined,
          bookId: this.bookId || undefined
        });
        this.applySession(session);
        await this.fetchMessages();
        this.startPolling();
      } catch (error) {
        console.error('initSession failed', error);
        this.errorMessage = error?.message || TEXTS.loadFailed;
      }
    },
    async refreshSessionMeta() {
      if (!this.sessionId) return;
      try {
        const session = await openChatSession({ sessionId: this.sessionId });
        this.applySession(session);
      } catch (error) {
        console.error('refreshSessionMeta failed', error);
      }
    },
    async fetchMessages(options = {}) {
      if (!this.sessionId) return;
      const silent = Boolean(options.silent);
      if (!silent) {
        this.loading = true;
        this.errorMessage = '';
      }
      try {
        const data = await getChatMessages(this.sessionId);
        if (Array.isArray(data)) {
          this.messages = data;
          this.errorMessage = '';
          await markChatRead(this.sessionId);
          this.scrollToBottom();
        }
      } catch (error) {
        console.error('fetchMessages failed', error);
        this.errorMessage = error?.message || TEXTS.loadFailed;
      } finally {
        this.loading = false;
      }
    },
    retryLoad() {
      this.initSession();
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/community/community' });
        }
      });
    },
    scrollToBottom() {
      this.scrollAnchor = '';
      this.$nextTick(() => {
        this.scrollAnchor = 'chat-bottom-anchor';
      });
    },
    formatPrice(value) {
      const amount = Number(value || 0);
      if (!Number.isFinite(amount)) return '0.00';
      return amount.toFixed(2);
    },
    openOrderDetail() {
      if (!this.sessionMeta.orderId) return;
      uni.navigateTo({
        url: `/pages/my/order-detail?orderId=${encodeURIComponent(this.sessionMeta.orderId)}`
      });
    },
    async handleTradeAction(action) {
      try {
        if (action === 'viewOrder') {
          this.openOrderDetail();
          return;
        }
        if (action === 'createOrder') {
          const bookId = this.sessionMeta.bookId || this.bookId;
          if (bookId) {
            uni.navigateTo({ url: `/pages/cart/checkout?bookId=${encodeURIComponent(bookId)}` });
          }
          return;
        }
        if (action === 'pay') {
          await payOrder(this.sessionMeta.orderId);
          uni.showToast({ title: this.texts.paySuccess, icon: 'success' });
        }
        if (action === 'cancel') {
          await cancelOrder(this.sessionMeta.orderId);
          uni.showToast({ title: this.texts.cancelSuccess, icon: 'success' });
        }
        if (action === 'ship') {
          await shipOrder(this.sessionMeta.orderId);
          uni.showToast({ title: this.texts.shipSuccess, icon: 'success' });
        }
        if (action === 'confirm') {
          await confirmReceipt(this.sessionMeta.orderId);
          uni.showToast({ title: this.texts.confirmSuccess, icon: 'success' });
        }
        await this.refreshSessionMeta();
      } catch (error) {
        console.error('handleTradeAction failed', error);
      }
    },
    async submitMessage() {
      if (!this.sessionId || !this.draftMessage.trim()) return;
      try {
        const message = await sendChatMessage({
          sessionId: this.sessionId,
          content: this.draftMessage.trim()
        });
        this.messages.push(message);
        this.draftMessage = '';
        this.scrollToBottom();
      } catch (error) {
        console.error('submitMessage failed', error);
      }
    },
    latestMessageId() {
      return this.messages.reduce((maxId, item) => Math.max(maxId, Number(item.id || 0)), 0);
    },
    startPolling() {
      if (!this.sessionId) return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        this.pollMessages();
      }, POLL_INTERVAL_MS);
    },
    stopPolling() {
      if (!this.pollTimer) return;
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    },
    async pollMessages() {
      if (!this.sessionId || this.polling) return;
      this.polling = true;
      try {
        const data = await pollChatMessages(this.sessionId, this.latestMessageId());
        const items = Array.isArray(data?.items) ? data.items : [];
        if (items.length) {
          const seen = new Set(this.messages.map((item) => String(item.id)));
          this.messages = [
            ...this.messages,
            ...items.filter((item) => !seen.has(String(item.id)))
          ];
          this.scrollToBottom();
        }
        if (items.length || Number(data?.unreadCount || 0) > 0) {
          await markChatRead(this.sessionId);
        }
      } catch (error) {
        console.error('pollMessages failed', error);
      } finally {
        this.polling = false;
      }
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eff4fb 0%, #f6f8fb 30%, #f6f8fb 100%);
  display: flex;
  flex-direction: column;
}

.header {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  z-index: 20;
  box-sizing: border-box;
  background: rgba(246, 248, 251, 0.96);
  backdrop-filter: blur(12rpx);
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-left: 20rpx;
  padding-right: 20rpx;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: #eaf0f7;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-icon {
  width: 30rpx;
  height: 30rpx;
}

.header-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.seller-name {
  font-size: 30rpx;
  color: #263442;
  font-weight: 700;
}

.book-name {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #6f7c8e;
}

.chat-body {
  flex: 1;
  padding: 18rpx 20rpx calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.trade-card {
  display: flex;
  gap: 18rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 20rpx;
  box-shadow: 0 16rpx 36rpx rgba(26, 46, 78, 0.08);
  margin-bottom: 18rpx;
}

.trade-cover {
  width: 144rpx;
  height: 184rpx;
  border-radius: 18rpx;
  background: #eaf0f7;
  flex-shrink: 0;
}

.trade-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.trade-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.trade-role {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #edf3ff;
  color: #1f5eff;
  font-size: 20rpx;
  font-weight: 700;
}

.trade-status {
  font-size: 22rpx;
  color: #f59e0b;
  font-weight: 700;
}

.trade-title {
  margin-top: 14rpx;
  font-size: 30rpx;
  line-height: 1.5;
  color: #203347;
  font-weight: 700;
}

.trade-meta {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #6d7f92;
}

.trade-price {
  margin-top: 12rpx;
  font-size: 34rpx;
  color: #f59e0b;
  font-weight: 700;
}

.trade-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  padding-top: 16rpx;
}

.action-btn {
  min-width: 132rpx;
  height: 64rpx;
  padding: 0 20rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.action-btn.primary {
  background: #1f5eff;
  color: #ffffff;
}

.action-btn.secondary {
  background: #eef3f8;
  color: #4c6077;
}

.status-card {
  background: linear-gradient(135deg, #1f5eff 0%, #143a7b 100%);
  color: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.status-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}

.status-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.6;
  opacity: 0.92;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  margin-bottom: 22rpx;
}

.msg-row.other {
  justify-content: flex-start;
  padding-right: 88rpx;
}

.msg-row.mine {
  justify-content: flex-end;
  padding-left: 88rpx;
}

.msg-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #e4ebf4;
  flex-shrink: 0;
}

.msg-main {
  max-width: 68%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.msg-main.mine {
  align-items: flex-end;
}

.msg-main.other {
  align-items: flex-start;
}

.msg-bubble {
  max-width: 100%;
  border-radius: 20rpx;
  padding: 18rpx 20rpx;
  font-size: 26rpx;
  line-height: 1.6;
  word-break: break-all;
  box-shadow: 0 10rpx 20rpx rgba(20, 37, 63, 0.04);
}

.msg-bubble.other {
  background: #ffffff;
  color: #34495e;
}

.msg-bubble.mine {
  background: #dbe9ff;
  color: #234266;
}

.msg-time {
  margin-top: 10rpx;
  font-size: 20rpx;
  color: #91a0b0;
}

.state {
  min-height: 180rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  color: #7d8fa2;
  font-size: 26rpx;
}

.state-text {
  color: #6d8095;
  font-size: 26rpx;
}

.retry-btn {
  min-width: 148rpx;
  height: 62rpx;
  padding: 0 24rpx;
  border-radius: 16rpx;
  background: #1f5eff;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.98);
  padding: 14rpx 18rpx calc(14rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-top: 1rpx solid #e6edf5;
}

.input {
  flex: 1;
  height: 72rpx;
  border-radius: 36rpx;
  background: #f1f5fa;
  padding: 0 24rpx;
  font-size: 24rpx;
}

.send-btn {
  width: 128rpx;
  height: 72rpx;
  border-radius: 36rpx;
  background: #173b75;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
}
</style>
