<template>
  <view class="page">
    <view :style="{ height: headerPlaceholderHeight + 'px' }"></view>

    <view class="content">
      <view v-if="!isLoggedIn" class="guest-card">
        <image class="guest-avatar" src="/static/logo.png" mode="aspectFill"></image>
        <text class="guest-title">{{ texts.guestTitle }}</text>
        <text class="guest-desc">{{ texts.guestDesc }}</text>
        <view class="guest-actions">
          <view class="guest-login" @click="goLogin">{{ texts.goLogin }}</view>
          <view class="guest-browse" @click="goBrowse">{{ texts.goBrowse }}</view>
        </view>
      </view>

      <block v-else>
      <view class="hero-card">
        <view class="hero-top">
          <image class="avatar" :src="profile.avatar || '/static/logo.png'" mode="aspectFill"></image>
          <view class="hero-main">
            <view class="name-row">
              <text class="name">{{ profile.nickname }}</text>
              <view class="badge">{{ profile.creditBadge }}</view>
            </view>
            <text class="signature">{{ profile.signature }}</text>
          </view>
        </view>

        <view class="hero-actions">
          <view class="action-chip" @click="goEditProfile">{{ texts.editProfile }}</view>
          <view class="action-chip light" @click="goVerify">{{ texts.verify }}</view>
        </view>

        <view class="hero-stats">
          <view class="stat">
            <text class="stat-num">{{ profile.points }}</text>
            <text class="stat-label">{{ texts.points }}</text>
          </view>
          <view class="divider"></view>
          <view class="stat">
            <text class="stat-num">{{ profile.level }}</text>
            <text class="stat-label">{{ texts.level }}</text>
          </view>
        </view>
      </view>

      <view v-if="profile.profileIncomplete" class="complete-strip" @click="goEditProfile">
        <view class="complete-main">
          <text class="complete-title">{{ texts.incompleteTitle }}</text>
          <text class="complete-desc">{{ texts.incompleteDesc }}</text>
        </view>
        <text class="complete-action">{{ texts.goComplete }}</text>
      </view>

      <view class="verify-strip" :class="{ verified: profile.authStatus === 2, pending: profile.authStatus === 1 }" @click="goVerify">
        <text class="verify-text">{{ texts.verifyStatus }}{{ profile.authLabel }}</text>
        <text class="verify-action">{{ verifyActionText }}</text>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.assetTitle }}</text>
        <view class="asset-grid">
          <view v-for="item in assetItems" :key="item.key" class="asset-item" @click="navigateTo(item.url)">
            <view class="asset-icon">{{ item.icon }}</view>
            <text class="asset-title">{{ item.title }}</text>
            <text class="asset-sub">{{ item.sub }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.knowledgeTitle }}</text>
        <view class="list-card">
          <view v-for="item in knowledgeItems" :key="item.key" class="list-item" @click="navigateTo(item.url)">
            <view class="list-left">
              <text class="list-icon">{{ item.icon }}</text>
              <text class="list-title">{{ item.title }}</text>
            </view>
            <text class="arrow">></text>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.helpTitle }}</text>
        <view class="list-card">
          <view class="list-item" @click="navigateTo('/pages/my/notifications')">
            <view class="list-left">
              <text class="list-icon">{{ texts.iconNotification }}</text>
              <text class="list-title">{{ texts.notifications }}</text>
            </view>
            <text class="arrow">></text>
          </view>
          <view class="list-item" @click="navigateTo('/pages/my/address')">
            <view class="list-left">
              <text class="list-icon">{{ texts.iconAddress }}</text>
              <text class="list-title">{{ texts.address }}</text>
            </view>
            <text class="arrow">></text>
          </view>
          <view class="list-item" @click="goService">
            <view class="list-left">
              <text class="list-icon">{{ texts.iconService }}</text>
              <text class="list-title">{{ texts.service }}</text>
            </view>
            <text class="arrow">></text>
          </view>
          <view class="list-item" @click="goEditProfile">
            <view class="list-left">
              <text class="list-icon">{{ texts.iconSetting }}</text>
              <text class="list-title">{{ texts.setting }}</text>
            </view>
            <text class="arrow">></text>
          </view>
        </view>
      </view>

      <view class="logout-wrap">
        <view class="logout-btn" @click="onLogout">{{ texts.logout }}</view>
      </view>
      </block>
      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, getUserStats, logoutAuth } from '../../utils/api/user';
import { clearSession, ensureLoggedIn, getCurrentPageUrl, hasValidSession } from '../../utils/auth';

export default {
  data() {
    return {
      headerPlaceholderHeight: 0,
      texts: {
        editProfile: '\u5b8c\u5584\u8d44\u6599',
        verify: '\u5b66\u751f\u8ba4\u8bc1',
        points: '\u79ef\u5206',
        level: '\u5b66\u8005\u7b49\u7ea7',
        incompleteTitle: '\u4e2a\u4eba\u4fe1\u606f\u5f85\u5b8c\u5584',
        incompleteDesc: '\u9ed8\u8ba4\u6635\u79f0\u4e3a\u4e66\u53cb\uff0c\u70b9\u51fb\u8865\u5145\u5b66\u6821\u3001\u9662\u7cfb\u3001\u7b80\u4ecb\u548c\u5934\u50cf\u3002',
        goComplete: '\u53bb\u5b8c\u5584',
        verifyStatus: '\u8ba4\u8bc1\u72b6\u6001\uff1a',
        assetTitle: '\u8d44\u4ea7\u7ba1\u7406',
        knowledgeTitle: '\u77e5\u8bc6\u7ba1\u7406',
        helpTitle: '\u8bbe\u7f6e\u4e0e\u5e2e\u52a9',
        notifications: '\u901a\u77e5\u4e2d\u5fc3',
        address: '\u6536\u8d27\u5730\u5740\u7ba1\u7406',
        service: '\u8054\u7cfb\u5ba2\u670d',
        setting: '\u8bbe\u7f6e',
        logout: '\u9000\u51fa\u767b\u5f55',
        iconNotification: '\u901a',
        iconAddress: '\u5730',
        iconService: '\u670d',
        iconSetting: '\u8bbe',
        confirmLogout: '\u9000\u51fa\u767b\u5f55',
        confirmLogoutText: '\u786e\u8ba4\u9000\u51fa\u5f53\u524d\u8d26\u53f7\u5417\uff1f',
        viewVerify: '\u67e5\u770b\u8ba4\u8bc1\u4fe1\u606f',
        pendingVerify: '\u5ba1\u6838\u4e2d',
        tapVerify: '\u70b9\u51fb\u53bb\u8ba4\u8bc1',
        guestTitle: '\u6e38\u5ba2\u6d4f\u89c8\u4e2d',
        guestDesc: '\u767b\u5f55\u540e\u53ef\u4ee5\u53d1\u5e03\u4e66\u7c4d\u3001\u6536\u85cf\u3001\u804a\u5929\u548c\u67e5\u770b\u8ba2\u5355\u3002',
        goLogin: '\u53bb\u767b\u5f55',
        goBrowse: '\u7ee7\u7eed\u901b\u901b'
      },
      isLoggedIn: false,
      profile: {
        avatar: '/static/logo.png',
        nickname: '\u4e66\u53cb',
        signature: '\u4e2a\u4eba\u4fe1\u606f\u5f85\u5b8c\u5584',
        creditBadge: '\u4fe1\u8a89\u826f\u597d',
        authStatus: 0,
        authLabel: '\u672a\u8ba4\u8bc1',
        points: 88,
        level: 'Lv.9',
        profileIncomplete: true
      },
      stats: {
        sellingBooks: 0,
        soldBooks: 0,
        pendingPay: 0,
        pendingShip: 0,
        pendingReceive: 0,
        favorites: 0,
        annotations: 0,
        paths: 0,
        resources: 0
      }
    };
  },
  computed: {
    verifyActionText() {
      if (this.profile.authStatus === 2) return this.texts.viewVerify;
      if (this.profile.authStatus === 1) return this.texts.pendingVerify;
      return this.texts.tapVerify;
    },
    assetItems() {
      return [
        { key: 'bookshelf', icon: '\u4e66', title: '\u6211\u7684\u4e66\u67b6', sub: `\u5728\u552e ${this.stats.sellingBooks} / \u5df2\u552e ${this.stats.soldBooks}`, url: '/pages/my/bookshelf' },
        { key: 'orders', icon: '\u5355', title: '\u6211\u7684\u8ba2\u5355', sub: `\u5f85\u4ed8 ${this.stats.pendingPay} / \u5f85\u53d1 ${this.stats.pendingShip}`, url: '/pages/my/orders' },
        { key: 'cart', icon: '\u8f66', title: '\u8d2d\u7269\u8f66', sub: '\u5408\u5e76\u52fe\u9009\u540e\u5206\u5355\u7ed3\u7b97', url: '/pages/cart/cart' },
        { key: 'favorites', icon: '\u85cf', title: '\u6211\u7684\u6536\u85cf', sub: `${this.stats.favorites} \u6761\u6536\u85cf`, url: '/pages/my/favorites' },
        { key: 'address', icon: '\u5740', title: '\u6536\u8d27\u5730\u5740', sub: '\u7ba1\u7406\u6536\u8d27\u4fe1\u606f', url: '/pages/my/address' },
        { key: 'notifications', icon: '\u901a', title: '\u901a\u77e5\u4e2d\u5fc3', sub: '\u67e5\u770b\u4ea4\u6613\u548c\u793e\u533a\u901a\u77e5', url: '/pages/my/notifications' }
      ];
    },
    knowledgeItems() {
      return [
        { key: 'annotations', icon: '\u6ce8', title: '\u6211\u7684\u6279\u6ce8', url: '/pages/my/annotations' },
        { key: 'paths', icon: '\u5f84', title: '\u6211\u7684\u8def\u5f84', url: '/pages/my/paths' },
        { key: 'resources', icon: '\u8d44', title: '\u6211\u4e0a\u4f20\u7684\u8d44\u6e90', url: '/pages/my/resources' }
      ];
    }
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerPlaceholderHeight = capsule ? capsule.top + capsule.height + 12 : statusBarHeight + 48;
  },
  onShow() {
    this.isLoggedIn = hasValidSession();
    if (!this.isLoggedIn) return;
    this.fetchProfile();
    this.fetchStats();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await getUserProfile();
        if (data) {
          this.profile = { ...this.profile, ...data };
        }
      } catch (error) {
        console.error('fetchProfile failed', error);
      }
    },
    async fetchStats() {
      try {
        const data = await getUserStats();
        if (data) {
          this.stats = { ...this.stats, ...data };
        }
      } catch (error) {
        console.error('fetchStats failed', error);
      }
    },
    navigateTo(url) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      uni.navigateTo({ url });
    },
    goLogin() {
      ensureLoggedIn(getCurrentPageUrl());
    },
    goBrowse() {
      uni.switchTab({ url: '/pages/index/index' });
    },
    goEditProfile() {
      this.navigateTo('/pages/my/edit');
    },
    goVerify() {
      this.navigateTo('/pages/placeholder/verify');
    },
    goService() {
      uni.navigateTo({ url: `/pages/placeholder/feedback?pagePath=${encodeURIComponent('/pages/my/my')}` });
    },
    onLogout() {
      uni.showModal({
        title: this.texts.confirmLogout,
        content: this.texts.confirmLogoutText,
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await logoutAuth();
          } catch (error) {
            console.error('logoutAuth failed', error);
          }
          clearSession();
          uni.reLaunch({ url: '/pages/login/login' });
        }
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 300rpx, #eef3fb 100%); }
.content { box-sizing: border-box; padding: 0 24rpx calc(32rpx + env(safe-area-inset-bottom)); }
.guest-card { background: #ffffff; border-radius: 30rpx; padding: 46rpx 32rpx; display: flex; flex-direction: column; align-items: center; text-align: center; border: 1rpx solid #e2eaf5; box-shadow: 0 18rpx 42rpx rgba(23, 32, 51, 0.08); }
.guest-avatar { width: 116rpx; height: 116rpx; border-radius: 58rpx; background: #eef2f8; }
.guest-title { margin-top: 22rpx; font-size: 36rpx; color: #25374b; font-weight: 700; }
.guest-desc { margin-top: 12rpx; max-width: 620rpx; font-size: 25rpx; line-height: 1.7; color: #6f8094; }
.guest-actions { margin-top: 28rpx; display: flex; gap: 14rpx; width: 100%; }
.guest-login, .guest-browse { flex: 1; height: 76rpx; border-radius: 16rpx; font-size: 26rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.guest-login { background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; }
.guest-browse { background: #ffffff; color: #40566f; border: 1rpx solid #dce6f3; }
.hero-card { background: radial-gradient(circle at 85% 10%, rgba(245, 158, 11, 0.32), transparent 28%), linear-gradient(135deg, #143a7b 0%, #1f5eff 58%, #0f766e 100%); border-radius: 32rpx; padding: 28rpx; color: #ffffff; box-shadow: 0 20rpx 52rpx rgba(31, 94, 255, 0.22); }
.hero-top { display: flex; align-items: center; gap: 18rpx; }
.avatar { width: 112rpx; height: 112rpx; border-radius: 56rpx; background: rgba(255, 255, 255, 0.86); }
.hero-main { flex: 1; }
.name-row { display: flex; align-items: center; gap: 12rpx; flex-wrap: wrap; }
.name { display: block; font-size: 38rpx; font-weight: 700; }
.signature { margin-top: 8rpx; display: block; font-size: 24rpx; opacity: 0.92; }
.badge { padding: 8rpx 14rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.18); font-size: 22rpx; }
.hero-actions { margin-top: 20rpx; display: flex; gap: 12rpx; }
.action-chip { min-width: 150rpx; height: 60rpx; padding: 0 22rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.22); color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.action-chip.light { background: rgba(255, 255, 255, 0.14); }
.hero-stats { margin-top: 22rpx; display: flex; align-items: center; gap: 20rpx; }
.stat-num { display: block; font-size: 40rpx; font-weight: 700; }
.stat-label { display: block; margin-top: 6rpx; font-size: 22rpx; opacity: 0.9; }
.divider { width: 2rpx; height: 54rpx; background: rgba(255, 255, 255, 0.4); }
.complete-strip, .verify-strip { margin-top: 14rpx; border-radius: 14rpx; padding: 18rpx; display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.complete-strip { background: #ffffff; border: 1rpx solid #dce8ff; box-shadow: 0 12rpx 28rpx rgba(23, 32, 51, 0.05); }
.complete-main { flex: 1; }
.complete-title { display: block; font-size: 26rpx; color: #173b75; font-weight: 700; }
.complete-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: #6f84a0; line-height: 1.5; }
.complete-action { font-size: 24rpx; color: #1f5eff; font-weight: 700; white-space: nowrap; }
.verify-strip { background: #fff3e8; color: #9a5a24; }
.verify-strip.pending { background: #fff6d8; color: #8b6a20; }
.verify-strip.verified { background: #e9f8ed; color: #2f7a4f; }
.verify-text, .verify-action { font-size: 24rpx; }
.section { margin-top: 24rpx; }
.section-title { display: block; margin-bottom: 12rpx; font-size: 34rpx; color: #2d3d52; font-weight: 700; }
.asset-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14rpx; }
.asset-item { background: #ffffff; border-radius: 24rpx; min-height: 178rpx; padding: 20rpx 16rpx; border: 1rpx solid #e2eaf5; box-shadow: 0 14rpx 32rpx rgba(23, 32, 51, 0.06); }
.asset-icon { width: 56rpx; height: 56rpx; border-radius: 18rpx; background: linear-gradient(135deg, #e8efff 0%, #e7fbf8 100%); color: #1f5eff; font-size: 26rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.asset-title { margin-top: 12rpx; display: block; font-size: 26rpx; color: #2b3f53; font-weight: 600; }
.asset-sub { margin-top: 8rpx; display: block; font-size: 22rpx; color: #7b8ea1; line-height: 1.4; }
.list-card { background: #ffffff; border-radius: 24rpx; overflow: hidden; border: 1rpx solid #e2eaf5; box-shadow: 0 14rpx 32rpx rgba(23, 32, 51, 0.06); }
.list-item { height: 96rpx; padding: 0 18rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #edf1f5; }
.list-item:last-child { border-bottom: none; }
.list-left { display: flex; align-items: center; gap: 12rpx; }
.list-icon { width: 42rpx; height: 42rpx; border-radius: 10rpx; background: #eef2f8; color: #3e5f86; font-size: 22rpx; display: flex; align-items: center; justify-content: center; }
.list-title { font-size: 28rpx; color: #2b3f53; }
.arrow { font-size: 24rpx; color: #99a6b5; }
.logout-wrap { margin-top: 28rpx; }
.logout-btn { height: 84rpx; border-radius: 20rpx; background: #173b75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.bottom-space { height: 18rpx; }
</style>
