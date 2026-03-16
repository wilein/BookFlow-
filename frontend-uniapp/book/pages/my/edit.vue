<template>
  <view class="page">
    <view class="card">
      <view class="field">
        <text class="label">头像</text>
        <view class="avatar-row" @click="changeAvatar">
          <image class="avatar" :src="form.avatarUrl || defaultAvatar" mode="aspectFill"></image>
          <text class="hint">点击更换头像</text>
        </view>
      </view>
      <view class="field">
        <text class="label">昵称</text>
        <input
          class="input"
          v-model="form.nickname"
          placeholder="请输入昵称"
          maxlength="20"
        />
      </view>
      <view class="field">
        <text class="label">手机号</text>
        <input
          class="input"
          v-model="form.mobile"
          placeholder="绑定手机号用于联系"
          type="number"
          maxlength="11"
        />
      </view>
      <view class="field">
        <text class="label">城市</text>
        <input
          class="input"
          v-model="form.city"
          placeholder="所在城市"
        />
      </view>
    </view>

    <button class="save-btn" type="primary" @click="submit">保存修改</button>
  </view>
</template>

<script>
export default {
  data() {
    return {
      defaultAvatar: '/static/avatar_placeholder.png',
      form: {
        nickname: '',
        avatarUrl: '',
        mobile: '',
        city: ''
      }
    };
  },
  onLoad() {
    // 从本地缓存或上个页面带过来的 userInfo 预填
    const cached = uni.getStorageSync('userInfo');
    if (cached) {
      this.form.nickname = cached.nickname || '';
      this.form.avatarUrl = cached.avatarUrl || '';
      this.form.mobile = cached.mobile || '';
      this.form.city = cached.city || '';
    }
  },
  methods: {
    changeAvatar() {
      uni.showToast({ title: '头像上传功能待接入', icon: 'none' });
    },
    submit() {
      if (!this.form.nickname.trim()) {
        uni.showToast({ title: '请输入昵称', icon: 'none' });
        return;
      }
      // 这里预留调用后端更新接口，例如 /user/updateProfile
      // 目前先本地更新缓存，保证个人中心页能看到修改效果
      const cached = uni.getStorageSync('userInfo') || {};
      const newUserInfo = {
        ...cached,
        nickname: this.form.nickname,
        avatarUrl: this.form.avatarUrl,
        mobile: this.form.mobile,
        city: this.form.city
      };
      uni.setStorageSync('userInfo', newUserInfo);
      uni.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => {
        uni.navigateBack();
      }, 500);
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

.card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
}

.field {
  margin-bottom: 20rpx;
}

.label {
  font-size: 26rpx;
  color: #7f8c8d;
  margin-bottom: 8rpx;
  display: block;
}

.input {
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background-color: #f0f3f7;
  font-size: 28rpx;
}

.avatar-row {
  flex-direction: row;
  align-items: center;
  display: flex;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50rpx;
  background-color: #ecf0f1;
  margin-right: 20rpx;
}

.hint {
  font-size: 24rpx;
  color: #95a5a6;
}

.save-btn {
  margin-top: 32rpx;
  border-radius: 999rpx;
}
</style>

