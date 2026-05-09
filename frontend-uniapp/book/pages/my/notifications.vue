<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">通知中心</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="notifications.length" class="list">
        <view
          v-for="item in notifications"
          :key="item.id"
          class="card"
          :class="{ unread: !item.isRead }"
          @click="openNotification(item)"
        >
          <view class="card-head">
            <text class="title">{{ item.title }}</text>
            <text class="time">{{ item.createTime }}</text>
          </view>
          <text class="type">{{ item.type }}</text>
          <text class="content-text">{{ item.content }}</text>
        </view>
      </view>
      <view v-else class="empty">暂无通知</view>
    </view>
  </view>
</template>

<script>
import { getNotifications, readNotification } from '../../utils/api/user';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      notifications: []
    };
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  onShow() {
    this.fetchNotifications();
  },
  methods: {
    async fetchNotifications() {
      try {
        this.notifications = (await getNotifications()) || [];
      } catch (error) {
        console.error('getNotifications failed', error);
      }
    },
    async openNotification(item) {
      try {
        if (!item.isRead) {
          await readNotification(item.id);
          item.isRead = true;
        }
      } catch (error) {
        console.error('readNotification failed', error);
      }
      if (item.routeUrl) {
        const url = item.routeUrl.startsWith('/') ? item.routeUrl : `/${item.routeUrl}`;
        if (url.includes('/pages/') && !url.includes('/pages/index/index') && !url.includes('/pages/community/community') && !url.includes('/pages/my/my')) {
          uni.navigateTo({ url });
        } else if (url.includes('/pages/community/community') || url.includes('/pages/index/index') || url.includes('/pages/my/my')) {
          uni.switchTab({ url });
        }
      }
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
.content { padding: 16rpx 20rpx 40rpx; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 20rpx; border-left: 8rpx solid transparent; }
.card.unread { border-left-color: #2d55c7; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.title { flex: 1; font-size: 28rpx; color: #243548; font-weight: 700; }
.time { font-size: 22rpx; color: #8b9bad; }
.type { display: block; margin-top: 10rpx; font-size: 22rpx; color: #2d55c7; }
.content-text { display: block; margin-top: 10rpx; font-size: 24rpx; line-height: 1.6; color: #5a6f86; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
</style>
