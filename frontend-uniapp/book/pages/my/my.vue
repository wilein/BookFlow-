<template>
  <view class="page">
    <!-- 顶部个人信息 -->
    <view class="profile-header">
      <view class="profile-main">
        <image
          class="avatar"
          :src="user.avatarUrl || defaultAvatar"
          mode="aspectFill"
        ></image>
        <view class="profile-text">
          <view class="row">
            <text class="nickname">{{ user.nickname || '未登录用户' }}</text>
            <view class="badge" v-if="user.creditLevelText">
              <text class="badge-text">{{ user.creditLevelText }}</text>
            </view>
          </view>
          <text class="uid" v-if="user.userId">ID: {{ user.userId }}</text>
          <text class="uid" v-else>请先登录以同步云端数据</text>
        </view>
      </view>
      <view class="profile-meta">
        <view class="meta-item">
          <text class="meta-number">{{ user.stats.selling }}</text>
          <text class="meta-label">在售</text>
        </view>
        <view class="meta-item">
          <text class="meta-number">{{ user.stats.sold }}</text>
          <text class="meta-label">已售</text>
        </view>
        <view class="meta-item">
          <text class="meta-number">{{ user.stats.collections }}</text>
          <text class="meta-label">收藏</text>
        </view>
      </view>
    </view>

    <!-- 认证状态条 -->
    <view class="verify-bar" @click="goToVerify">
      <view class="verify-left">
        <text class="verify-status" :class="{ passed: user.isVerified }">
          {{ user.isVerified ? '已完成实名认证' : '未认证，点击去认证' }}
        </text>
        <text class="verify-sub">
          {{ user.isVerified ? '账户更安全，交易更放心' : '认证后可提升信誉等级，增加交易额度' }}
        </text>
      </view>
      <text class="arrow">></text>
    </view>

    <!-- 功能区块：我的书架 / 我的订单 -->
    <view class="card">
      <view class="card-title-row">
        <text class="card-title">我的书架</text>
        <text class="card-sub">管理在售/已售书籍</text>
      </view>
      <view class="grid grid-3">
        <view class="grid-item" @click="goToShelf('selling')">
          <text class="grid-icon">📚</text>
          <text class="grid-text">在售书籍</text>
        </view>
        <view class="grid-item" @click="goToShelf('sold')">
          <text class="grid-icon">✅</text>
          <text class="grid-text">已售书籍</text>
        </view>
        <view class="grid-item" @click="goToShelf('draft')">
          <text class="grid-icon">✏️</text>
          <text class="grid-text">草稿/待发布</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">我的订单</text>
        <text class="card-sub">查看买入 / 卖出订单</text>
      </view>
      <view class="grid grid-4">
        <view class="grid-item" @click="goToOrder('unpaid')">
          <text class="grid-icon">💰</text>
          <text class="grid-text">待付款</text>
        </view>
        <view class="grid-item" @click="goToOrder('undelivered')">
          <text class="grid-icon">📦</text>
          <text class="grid-text">待发货</text>
        </view>
        <view class="grid-item" @click="goToOrder('unreceived')">
          <text class="grid-icon">🚚</text>
          <text class="grid-text">待收货</text>
        </view>
        <view class="grid-item" @click="goToOrder('finished')">
          <text class="grid-icon">✅</text>
          <text class="grid-text">已完成</text>
        </view>
      </view>
    </view>

    <!-- 其它功能网格 -->
    <view class="card">
      <view class="grid grid-3">
        <view class="grid-item" @click="goToCollections">
          <text class="grid-icon">⭐</text>
          <text class="grid-text">我的收藏</text>
        </view>
        <view class="grid-item" @click="goToAnnotations">
          <text class="grid-icon">📝</text>
          <text class="grid-text">我的批注</text>
        </view>
        <view class="grid-item" @click="goToPaths">
          <text class="grid-icon">🗺️</text>
          <text class="grid-text">我创建的路径</text>
        </view>
        <view class="grid-item" @click="goToResources">
          <text class="grid-icon">📤</text>
          <text class="grid-text">我上传的资源</text>
        </view>
        <view class="grid-item" @click="goToAddress">
          <text class="grid-icon">🏠</text>
          <text class="grid-text">收货地址管理</text>
        </view>
        <view class="grid-item" @click="goToSettings">
          <text class="grid-icon">⚙️</text>
          <text class="grid-text">设置</text>
        </view>
      </view>
    </view>

    <!-- 底部：联系客服 / 编辑资料 / 退出登录 -->
    <view class="list-card">
      <view class="list-item" @click="goToEditProfile">
        <text class="list-text">编辑个人资料</text>
        <text class="list-sub">修改昵称、头像等信息</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="contactService">
        <text class="list-text">联系客服</text>
        <text class="list-sub">遇到问题？联系平台客服</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="logout-wrapper">
      <button class="logout-btn" type="warn" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      defaultAvatar: '/static/avatar_placeholder.png',
      user: {
        userId: null,
        nickname: '',
        avatarUrl: '',
        creditLevelText: '新用户',
        isVerified: false,
        stats: {
          selling: 0,
          sold: 0,
          collections: 0
        }
      }
    };
  },
  onLoad() {
    this.loadUserInfo();
  },
  methods: {
    // 从后端 / 登录结果中获取用户数据（参考 /user/auth/wechat 返回的 userInfo）
    loadUserInfo() {
      try {
        // 优先从本地缓存读取（建议在登录成功时把 userInfo 和 token 存入本地）
        const cached = uni.getStorageSync('userInfo');
        if (cached) {
          this.user = {
            ...this.user,
            ...cached
          };
        }
        // 如需直接从后端按 token 查询，可在此补充：
        // const token = uni.getStorageSync('token');
        // if (token) { 调用后端 /user/info 之类接口刷新最新资料 }
      } catch (e) {
        console.error('loadUserInfo error', e);
      }
    },
    goToVerify() {
      uni.navigateTo({
        url: '/pages/auth/auth' // 预留认证页面路由
      });
    },
    goToEditProfile() {
      uni.navigateTo({
        url: '/pages/my/edit' // 路由到编辑个人资料页
      });
    },
    goToShelf(type) {
      uni.navigateTo({
        url: `/pages/shelf/shelf?type=${type}`
      });
    },
    goToOrder(status) {
      uni.navigateTo({
        url: `/pages/order/list?status=${status}`
      });
    },
    goToCollections() {
      uni.navigateTo({
        url: '/pages/collection/collection'
      });
    },
    goToAnnotations() {
      uni.navigateTo({
        url: '/pages/annotation/annotation'
      });
    },
    goToPaths() {
      uni.navigateTo({
        url: '/pages/paths/my'
      });
    },
    goToResources() {
      uni.navigateTo({
        url: '/pages/resources/my'
      });
    },
    goToAddress() {
      uni.navigateTo({
        url: '/pages/address/list'
      });
    },
    goToSettings() {
      uni.navigateTo({
        url: '/pages/settings/settings'
      });
    },
    contactService() {
      // 这里可以改为跳转客服页或调用小程序客服能力
      uni.showModal({
        title: '联系客服',
        content: '请通过公众号或客服微信联系管理员。',
        showCancel: false
      });
    },
    logout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('userInfo');
            uni.removeStorageSync('token');
            this.user = {
              ...this.user,
              userId: null,
              nickname: '',
              avatarUrl: '',
              creditLevelText: '新用户',
              isVerified: false
            };
            uni.showToast({ title: '已退出登录', icon: 'none' });
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f6f7fb;
  padding: 24rpx;
  box-sizing: border-box;
}

.profile-header {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  border-radius: 24rpx;
  padding: 32rpx 28rpx 24rpx;
  color: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  margin-bottom: 24rpx;
}

.profile-main {
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background-color: #ecf0f1;
  margin-right: 24rpx;
}

.profile-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.row {
  display: flex;
  align-items: center;
}

.nickname {
  font-size: 32rpx;
  font-weight: 600;
  margin-right: 16rpx;
}

.badge {
  padding: 4rpx 16rpx;
  border-radius: 100rpx;
  background: rgba(255, 255, 255, 0.18);
}

.badge-text {
  font-size: 22rpx;
}

.uid {
  margin-top: 8rpx;
  font-size: 24rpx;
  opacity: 0.9;
}

.profile-meta {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-around;
}

.meta-item {
  align-items: center;
  display: flex;
  flex-direction: column;
}

.meta-number {
  font-size: 32rpx;
  font-weight: 600;
}

.meta-label {
  margin-top: 4rpx;
  font-size: 24rpx;
  opacity: 0.9;
}

.verify-bar {
  margin-bottom: 24rpx;
  padding: 20rpx 24rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.verify-left {
  display: flex;
  flex-direction: column;
}

.verify-status {
  font-size: 28rpx;
  color: #e67e22;
}

.verify-status.passed {
  color: #2ecc71;
}

.verify-sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #7f8c8d;
}

.arrow {
  font-size: 30rpx;
  color: #bdc3c7;
}

.card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx 24rpx 12rpx;
  margin-bottom: 20rpx;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2c3e50;
}

.card-sub {
  font-size: 24rpx;
  color: #95a5a6;
}

.grid {
  display: flex;
  flex-wrap: wrap;
}

.grid-3 .grid-item {
  width: 33.33%;
}

.grid-4 .grid-item {
  width: 25%;
}

.grid-item {
  padding: 16rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.grid-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.grid-text {
  font-size: 24rpx;
  color: #34495e;
}

.list-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  margin-top: 4rpx;
}

.list-item {
  padding: 20rpx 24rpx;
  border-bottom: 2rpx solid #f0f3f7;
  display: flex;
  align-items: center;
}

.list-item:last-child {
  border-bottom-width: 0;
}

.list-text {
  font-size: 28rpx;
  color: #2c3e50;
}

.list-sub {
  margin-left: 16rpx;
  font-size: 24rpx;
  color: #95a5a6;
  flex: 1;
}

.logout-wrapper {
  margin: 40rpx 0 20rpx;
}

.logout-btn {
  width: 100%;
  border-radius: 999rpx;
  background-color: #e74c3c;
  color: #ffffff;
}
</style>
