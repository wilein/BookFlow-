<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">订单详情</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content" v-if="detail">
      <view class="card">
        <view class="head-row">
          <text class="order-no">订单号：{{ detail.orderNo }}</text>
          <text class="status">{{ detail.statusLabel }}</text>
        </view>
        <view class="book-row">
          <image class="cover" :src="detail.bookCover || '/static/logo.png'" mode="aspectFill"></image>
          <view class="book-main">
            <text class="title">{{ detail.bookTitle }}</text>
            <text class="meta">卖家：{{ detail.sellerName }}</text>
            <text class="meta">买家：{{ detail.buyerName }}</text>
            <text class="meta">创建时间：{{ detail.createTime || '-' }}</text>
            <text class="amount">¥{{ detail.totalAmount }}</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section-title">收货信息</text>
        <text class="line">{{ detail.receiverName || '未填写' }} {{ detail.receiverPhone || '' }}</text>
        <text class="line">{{ detail.receiverAddress || '未填写' }}</text>
      </view>

      <view class="card">
        <text class="section-title">买家留言</text>
        <text class="line">{{ detail.buyerMessage || '暂无留言' }}</text>
      </view>

      <view class="card" v-if="issues.length">
        <text class="section-title">订单问题记录</text>
        <view v-for="item in issues" :key="item.id" class="issue-item">
          <view class="issue-head">
            <text class="issue-type">{{ item.typeLabel }}</text>
            <text class="issue-status">{{ item.statusLabel }}</text>
          </view>
          <text class="line">{{ item.content }}</text>
          <text class="issue-meta">{{ item.creatorName }} · {{ item.createTime }}</text>
          <view v-if="item.replyContent" class="reply-box">
            <text class="reply-title">回复</text>
            <text class="line">{{ item.replyContent }}</text>
            <text class="issue-meta">{{ item.replyUserName || '对方' }} · {{ item.replyTime }}</text>
          </view>
          <view v-if="item.canReply" class="issue-action" @click="goIssueReply(item)">去回复</view>
        </view>
      </view>

      <view class="action-grid">
        <view class="action-card" @click="goIssueCreate('question')">订单疑问</view>
        <view class="action-card" @click="goIssueCreate('after_sale')">售后处理</view>
        <view class="action-card" @click="goOrderReport">举报订单</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, getOrderIssues } from '../../utils/api/order';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      orderId: '',
      role: 'buyer',
      detail: null,
      issues: []
    };
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.orderId = options.orderId || '';
    this.role = options.role === 'seller' ? 'seller' : 'buyer';
  },
  onShow() {
    this.fetchDetail();
    this.fetchIssues();
  },
  methods: {
    async fetchDetail() {
      if (!this.orderId) return;
      try {
        this.detail = await getOrderDetail(this.orderId);
      } catch (error) {
        console.error('getOrderDetail failed', error);
      }
    },
    async fetchIssues() {
      if (!this.orderId) return;
      try {
        this.issues = (await getOrderIssues(this.orderId)) || [];
      } catch (error) {
        console.error('getOrderIssues failed', error);
      }
    },
    goIssueCreate(type) {
      const query = [
        'mode=order-issue-create',
        `orderId=${encodeURIComponent(this.orderId)}`,
        `issueType=${encodeURIComponent(type)}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join('&');
      uni.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goIssueReply(issue) {
      const query = [
        'mode=order-issue-reply',
        `orderId=${encodeURIComponent(this.orderId)}`,
        `issueId=${encodeURIComponent(issue.id || '')}`,
        `title=${encodeURIComponent(issue.typeLabel || '订单问题')}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join('&');
      uni.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goOrderReport() {
      const query = [
        'mode=order-report',
        `orderId=${encodeURIComponent(this.orderId)}`,
        `title=${encodeURIComponent(this.detail?.orderNo || '')}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join('&');
      uni.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.navigateTo({ url: `/pages/my/orders?role=${this.role}` }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 16rpx 20rpx 40rpx; }
.card { margin-bottom: 16rpx; background: #ffffff; border-radius: 22rpx; padding: 20rpx; }
.head-row { display: flex; justify-content: space-between; gap: 12rpx; }
.order-no { font-size: 22rpx; color: #7b8ea1; }
.status { font-size: 24rpx; color: #1f5eff; font-weight: 700; }
.book-row { margin-top: 16rpx; display: flex; gap: 16rpx; }
.cover { width: 136rpx; height: 164rpx; border-radius: 16rpx; background: #edf2f8; }
.book-main { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.title { font-size: 30rpx; color: #243548; font-weight: 700; line-height: 1.5; }
.meta, .line { font-size: 24rpx; color: #5a6f86; line-height: 1.7; }
.amount { margin-top: 8rpx; font-size: 34rpx; color: #f59e0b; font-weight: 700; }
.section-title { display: block; margin-bottom: 12rpx; font-size: 28rpx; color: #243548; font-weight: 700; }
.issue-item { padding: 16rpx 0; border-top: 1rpx solid #edf1f5; }
.issue-item:first-of-type { border-top: none; padding-top: 0; }
.issue-head { display: flex; justify-content: space-between; gap: 12rpx; margin-bottom: 8rpx; }
.issue-type { font-size: 24rpx; color: #1f5eff; font-weight: 700; }
.issue-status { font-size: 22rpx; color: #7b8ea1; }
.issue-meta { display: block; margin-top: 8rpx; font-size: 22rpx; color: #8ea0b4; }
.reply-box { margin-top: 12rpx; border-radius: 16rpx; background: #f5f8fc; padding: 14rpx; }
.reply-title { display: block; margin-bottom: 8rpx; font-size: 22rpx; color: #2d3d52; font-weight: 700; }
.issue-action { margin-top: 12rpx; width: 164rpx; height: 60rpx; border-radius: 14rpx; background: #1f5eff; color: #ffffff; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.action-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; }
.action-card { height: 88rpx; border-radius: 18rpx; background: #ffffff; color: #173b75; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; text-align: center; padding: 0 12rpx; }
</style>
