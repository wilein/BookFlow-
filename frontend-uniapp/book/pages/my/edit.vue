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
      <view class="hero-card">
        <text class="hero-title">{{ texts.heroTitle }}</text>
        <text class="hero-sub">{{ texts.heroSub }}</text>
      </view>

      <view class="card">
        <view class="field">
          <text class="label">{{ texts.avatar }}</text>
          <view class="avatar-row" @click="changeAvatar">
            <image class="avatar" :src="form.avatarUrl || '/static/logo.png'" mode="aspectFill"></image>
            <view class="avatar-meta">
              <text class="avatar-title">{{ texts.changeAvatar }}</text>
              <text class="avatar-desc">{{ avatarUploading ? texts.uploading : texts.avatarDesc }}</text>
            </view>
          </view>
        </view>

        <view class="field">
          <text class="label">{{ texts.nickname }}</text>
          <input class="input" v-model="form.nickname" maxlength="20" :placeholder="texts.nicknamePlaceholder" />
        </view>

        <view class="field">
          <text class="label">{{ texts.mobile }}</text>
          <input class="input" v-model="form.mobile" type="number" maxlength="11" :placeholder="texts.mobilePlaceholder" />
        </view>

        <view class="field">
          <text class="label">{{ texts.city }}</text>
          <input class="input" v-model="form.city" maxlength="30" :placeholder="texts.cityPlaceholder" />
        </view>

        <view class="field">
          <text class="label">{{ texts.school }}</text>
          <input class="input" v-model="form.school" maxlength="50" :placeholder="texts.schoolPlaceholder" />
        </view>

        <view class="field">
          <text class="label">{{ texts.department }}</text>
          <input class="input" v-model="form.department" maxlength="50" :placeholder="texts.departmentPlaceholder" />
        </view>

        <view class="field">
          <text class="label">{{ texts.intro }}</text>
          <textarea class="textarea" v-model="form.intro" maxlength="120" :placeholder="texts.introPlaceholder"></textarea>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="save-btn" @click="submit">{{ submitting ? texts.saving : texts.save }}</view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, updateUserProfile, uploadProfileImage } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      avatarUploading: false,
      submitting: false,
      texts: {
        title: '\u7f16\u8f91\u8d44\u6599',
        heroTitle: '\u5b8c\u5584\u4e2a\u4eba\u4fe1\u606f',
        heroSub: '\u6635\u79f0\u3001\u5b66\u6821\u548c\u7b80\u4ecb\u4f1a\u540c\u6b65\u5c55\u793a\u5728\u4e2a\u4eba\u4e3b\u9875\uff0c\u5934\u50cf\u4e0a\u4f20\u540e\u53ef\u76f4\u63a5\u9884\u89c8\u3002',
        avatar: '\u5934\u50cf',
        changeAvatar: '\u70b9\u51fb\u66f4\u6362\u5934\u50cf',
        avatarDesc: '\u652f\u6301\u4ece\u76f8\u518c\u6216\u62cd\u7167\u9009\u62e9',
        uploading: '\u4e0a\u4f20\u4e2d...',
        nickname: '\u6635\u79f0',
        nicknamePlaceholder: '\u8bf7\u8f93\u5165\u6635\u79f0',
        mobile: '\u624b\u673a\u53f7',
        mobilePlaceholder: '\u8bf7\u8f93\u5165\u624b\u673a\u53f7',
        city: '\u57ce\u5e02',
        cityPlaceholder: '\u8bf7\u8f93\u5165\u6240\u5728\u57ce\u5e02',
        school: '\u5b66\u6821',
        schoolPlaceholder: '\u8bf7\u8f93\u5165\u5b66\u6821\u540d\u79f0',
        department: '\u9662\u7cfb',
        departmentPlaceholder: '\u8bf7\u8f93\u5165\u9662\u7cfb\u540d\u79f0',
        intro: '\u4e2a\u4eba\u7b80\u4ecb',
        introPlaceholder: '\u4ecb\u7ecd\u4e00\u4e0b\u4f60\u7684\u7814\u7a76\u65b9\u5411\u3001\u611f\u5174\u8da3\u9886\u57df\u6216\u4e00\u53e5\u7b7e\u540d',
        save: '\u4fdd\u5b58\u4fee\u6539',
        saving: '\u4fdd\u5b58\u4e2d...',
        uploaded: '\u5934\u50cf\u5df2\u66f4\u65b0',
        saved: '\u5df2\u4fdd\u5b58',
        uploadLoading: '\u4e0a\u4f20\u4e2d',
        needNickname: '\u8bf7\u8f93\u5165\u6635\u79f0'
      },
      form: {
        nickname: '',
        avatarUrl: '',
        mobile: '',
        city: '',
        school: '',
        department: '',
        intro: ''
      }
    };
  },
  async onLoad() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    await this.fetchProfile();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await getUserProfile();
        if (!data) return;
        this.form = {
          nickname: data.nickname || '',
          avatarUrl: data.avatarUrl || data.avatar || '',
          mobile: data.mobile || '',
          city: data.city || '',
          school: data.school || '',
          department: data.department || '',
          intro: data.intro || ''
        };
      } catch (error) {
        console.error('fetchProfile failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const filePath = (res.tempFilePaths || [])[0];
          if (!filePath) return;
          this.avatarUploading = true;
          uni.showLoading({ title: this.texts.uploadLoading, mask: true });
          try {
            const result = await uploadProfileImage(filePath, 'avatar');
            this.form.avatarUrl = result.url || '';
            uni.showToast({ title: this.texts.uploaded, icon: 'success' });
          } catch (error) {
            console.error('upload avatar failed', error);
          } finally {
            this.avatarUploading = false;
            uni.hideLoading();
          }
        }
      });
    },
    async submit() {
      if (this.submitting) return;
      if (!this.form.nickname.trim()) {
        uni.showToast({ title: this.texts.needNickname, icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        await updateUserProfile({
          nickname: this.form.nickname,
          avatarUrl: this.form.avatarUrl,
          mobile: this.form.mobile,
          city: this.form.city,
          school: this.form.school,
          department: this.form.department,
          intro: this.form.intro
        });
        uni.showToast({ title: this.texts.saved, icon: 'success' });
        setTimeout(() => this.goBack(), 500);
      } catch (error) {
        console.error('updateUserProfile failed', error);
      } finally {
        this.submitting = false;
      }
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
.content { padding: 0 20rpx 140rpx; }
.hero-card { background: linear-gradient(135deg, #2d55c7 0%, #2349b7 100%); border-radius: 24rpx; padding: 24rpx; color: #ffffff; }
.hero-title { display: block; font-size: 34rpx; font-weight: 700; }
.hero-sub { display: block; margin-top: 8rpx; font-size: 24rpx; line-height: 1.5; opacity: 0.92; }
.card { margin-top: 16rpx; background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.field { margin-bottom: 18rpx; }
.field:last-child { margin-bottom: 0; }
.label { display: block; font-size: 24rpx; color: #5d7186; margin-bottom: 10rpx; }
.input, .textarea { width: 100%; box-sizing: border-box; border-radius: 14rpx; background: #f3f7fb; font-size: 26rpx; color: #2b3f53; }
.input { height: 72rpx; line-height: 72rpx; padding: 0 20rpx; }
.textarea { min-height: 180rpx; padding: 20rpx; }
.avatar-row { display: flex; align-items: center; gap: 18rpx; padding: 18rpx; border-radius: 16rpx; background: #f3f7fb; }
.avatar { width: 112rpx; height: 112rpx; border-radius: 56rpx; background: #edf2f8; }
.avatar-meta { flex: 1; }
.avatar-title { display: block; font-size: 28rpx; color: #2b3f53; font-weight: 600; }
.avatar-desc { display: block; margin-top: 8rpx; font-size: 22rpx; color: #7b8ea1; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.save-btn { height: 84rpx; border-radius: 16rpx; background: #2f4f75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
