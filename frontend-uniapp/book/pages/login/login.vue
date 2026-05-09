<template>
  <view class="page">
    <view class="hero">
      <image class="logo" src="/static/logo.png" mode="aspectFit"></image>
      <text class="title">{{ texts.appName }}</text>
      <text class="subtitle">{{ texts.subtitle }}</text>
    </view>

    <view class="card">
      <text class="card-title">{{ texts.cardTitle }}</text>
      <text class="card-desc">{{ texts.cardDesc }}</text>
      <view class="login-btn primary" :class="{ disabled: loading }" @click="handleLogin">
        {{ loading ? texts.loggingIn : loginButtonText }}
      </view>
      <text class="tip-text">{{ texts.tip }}</text>
    </view>
  </view>
</template>

<script>
import { loginWithDev, loginWithWechat } from '../../utils/api/user';
import { hasValidSession, navigateAfterLogin } from '../../utils/auth';

const TEXTS = {
  appName: '\u85aa\u4f20',
  subtitle: '\u6559\u6750\u6d41\u8f6c\u3001\u6279\u6ce8\u4f20\u627f\u3001\u5b66\u4e60\u8def\u5f84\u5171\u4eab',
  cardTitle: '\u767b\u5f55\u540e\u7ee7\u7eed\u4f7f\u7528',
  cardDesc: '\u9996\u6b21\u8fdb\u5165\u9700\u8981\u5b8c\u6210\u767b\u5f55\uff0c\u767b\u5f55\u72b6\u6001\u6709\u6548\u671f 15 \u5929\uff0c\u6d3b\u8dc3\u8bbf\u95ee\u4f1a\u81ea\u52a8\u7eed\u671f\u3002',
  h5Login: '\u5f00\u53d1\u73af\u5883\u4e00\u952e\u767b\u5f55',
  wechatLogin: '\u5fae\u4fe1\u4e00\u952e\u767b\u5f55',
  loggingIn: '\u767b\u5f55\u4e2d...',
  tip: '\u5f53\u524d\u4e0d\u652f\u6301\u533f\u540d\u8fdb\u5165\u4e1a\u52a1\u9875',
  loginSuccess: '\u767b\u5f55\u6210\u529f',
  loginFail: '\u767b\u5f55\u5931\u8d25',
  codeFail: '\u83b7\u53d6\u767b\u5f55\u51ed\u8bc1\u5931\u8d25'
};

function getRuntimePlatform() {
  let platform = 'unknown';
  // #ifdef MP-WEIXIN
  platform = 'mp-weixin';
  // #endif
  // #ifdef H5
  platform = 'h5';
  // #endif
  return platform;
}

export default {
  data() {
    return {
      texts: TEXTS,
      loading: false,
      platform: getRuntimePlatform()
    };
  },
  computed: {
    loginButtonText() {
      return this.platform === 'h5' ? TEXTS.h5Login : TEXTS.wechatLogin;
    }
  },
  onLoad() {
    if (hasValidSession()) {
      navigateAfterLogin();
    }
  },
  methods: {
    async handleLogin() {
      if (this.loading) return;
      this.loading = true;
      try {
        if (this.platform === 'h5') {
          await loginWithDev();
        } else {
          const code = await this.fetchWechatCode();
          await loginWithWechat(code);
        }
        uni.showToast({ title: TEXTS.loginSuccess, icon: 'success' });
        setTimeout(() => {
          navigateAfterLogin();
        }, 300);
      } catch (error) {
        console.error('handleLogin failed', error);
        uni.showToast({ title: error?.message || TEXTS.loginFail, icon: 'none' });
      } finally {
        this.loading = false;
      }
    },
    fetchWechatCode() {
      return new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: (res) => {
            if (res.code) {
              resolve(res.code);
              return;
            }
            reject(new Error('missing wechat code'));
          },
          fail: (error) => {
            uni.showToast({ title: TEXTS.codeFail, icon: 'none' });
            reject(error);
          }
        });
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding: 120rpx 40rpx 80rpx; box-sizing: border-box; background: radial-gradient(circle at top left, rgba(255, 255, 255, 0.18), transparent 34%), linear-gradient(160deg, #2148b7 0%, #2d55c7 55%, #6e9bff 100%); display: flex; flex-direction: column; justify-content: space-between; }
.hero { padding-top: 80rpx; color: #ffffff; }
.logo { width: 112rpx; height: 112rpx; border-radius: 28rpx; background: rgba(255, 255, 255, 0.16); padding: 18rpx; box-sizing: border-box; }
.title { display: block; margin-top: 26rpx; font-size: 64rpx; font-weight: 700; letter-spacing: 6rpx; }
.subtitle { display: block; margin-top: 18rpx; font-size: 28rpx; line-height: 1.6; opacity: 0.92; }
.card { padding: 36rpx 30rpx 30rpx; border-radius: 28rpx; background: rgba(255, 255, 255, 0.95); box-shadow: 0 18rpx 48rpx rgba(8, 35, 99, 0.2); }
.card-title { display: block; font-size: 36rpx; color: #22344a; font-weight: 700; }
.card-desc { display: block; margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: #66798f; }
.login-btn { height: 88rpx; border-radius: 20rpx; margin-top: 24rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 700; }
.login-btn.primary { background: #2d55c7; color: #ffffff; }
.login-btn.disabled { opacity: 0.7; }
.tip-text { display: block; margin-top: 20rpx; text-align: center; font-size: 22rpx; color: #7a8da2; }
</style>
