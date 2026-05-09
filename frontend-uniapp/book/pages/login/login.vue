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
.page { min-height: 100vh; padding: 120rpx 40rpx 80rpx; box-sizing: border-box; background: radial-gradient(circle at 18% 14%, rgba(20, 184, 166, 0.5), transparent 30%), radial-gradient(circle at 90% 8%, rgba(245, 158, 11, 0.34), transparent 26%), linear-gradient(160deg, #0f2557 0%, #143a7b 44%, #1f5eff 100%); display: flex; flex-direction: column; justify-content: space-between; }
.hero { padding-top: 80rpx; color: #ffffff; }
.logo { width: 116rpx; height: 116rpx; border-radius: 32rpx; background: rgba(255, 255, 255, 0.16); padding: 18rpx; box-sizing: border-box; border: 1rpx solid rgba(255, 255, 255, 0.28); box-shadow: 0 18rpx 42rpx rgba(15, 32, 68, 0.26); }
.title { display: block; margin-top: 30rpx; font-size: 66rpx; font-weight: 800; letter-spacing: 0; }
.subtitle { display: block; margin-top: 18rpx; font-size: 28rpx; line-height: 1.7; opacity: 0.9; max-width: 620rpx; }
.card { padding: 38rpx 32rpx 32rpx; border-radius: 32rpx; background: rgba(255, 255, 255, 0.96); box-shadow: 0 24rpx 64rpx rgba(8, 25, 61, 0.28); border: 1rpx solid rgba(255, 255, 255, 0.64); }
.card-title { display: block; font-size: 36rpx; color: #22344a; font-weight: 700; }
.card-desc { display: block; margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: #66798f; }
.login-btn { height: 88rpx; border-radius: 20rpx; margin-top: 24rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 700; }
.login-btn.primary { background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; box-shadow: 0 14rpx 28rpx rgba(31, 94, 255, 0.24); }
.login-btn.disabled { opacity: 0.7; }
.tip-text { display: block; margin-top: 20rpx; text-align: center; font-size: 22rpx; color: #7a8da2; }
</style>
