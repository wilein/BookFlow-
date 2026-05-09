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
        <view class="hero-main">
          <view>
            <text class="hero-title">{{ texts.heroTitle }}</text>
            <text class="hero-sub">{{ texts.heroSub }}</text>
          </view>
          <view class="status-chip" :class="statusClass">{{ statusText }}</view>
        </view>
      </view>

      <view class="mode-card">
        <view
          v-for="item in verifyModes"
          :key="item.key"
          class="mode-item"
          :class="{ active: verifyMode === item.key }"
          @click="switchMode(item.key)"
        >
          <text class="mode-title">{{ item.title }}</text>
          <text class="mode-desc">{{ item.desc }}</text>
        </view>
      </view>

      <template v-if="verifyMode === 'student_card'">
        <view class="section-card">
          <view class="field-row">
            <text class="label">{{ texts.realName }}</text>
            <input class="input" v-model="form.realName" maxlength="20" :placeholder="texts.realNamePlaceholder" />
          </view>
          <view class="field-row">
            <text class="label">{{ texts.studentId }}</text>
            <input class="input" v-model="form.studentId" maxlength="30" :placeholder="texts.studentIdPlaceholder" />
          </view>
          <view class="field-row">
            <text class="label">{{ texts.school }}</text>
            <input class="input" v-model="form.school" maxlength="50" :placeholder="texts.schoolPlaceholder" />
          </view>
          <view class="field-row">
            <text class="label">{{ texts.department }}</text>
            <input class="input" v-model="form.department" maxlength="50" :placeholder="texts.departmentPlaceholder" />
          </view>
        </view>

        <view class="section-card">
          <view class="section-head">
            <text class="section-title">{{ texts.studentCard }}</text>
            <text class="section-tip">{{ texts.required }}</text>
          </view>
          <view v-if="!form.studentCardImageUrl" class="upload-box" @click="chooseStudentCard">
            <text class="upload-icon">+</text>
            <text class="upload-text">{{ uploading ? texts.uploading : texts.uploadText }}</text>
          </view>
          <view v-else class="image-wrap">
            <image
              v-if="studentCardPreviewSrc"
              class="preview"
              :src="studentCardPreviewSrc"
              mode="aspectFit"
              @click="previewStudentCard"
            ></image>
            <view v-else class="preview preview-loading">
              <text>{{ texts.previewLoading }}</text>
            </view>
            <view class="image-actions">
              <view class="image-btn" @click="chooseStudentCard">{{ texts.reupload }}</view>
              <view class="image-btn ghost" @click="removeStudentCard">{{ texts.remove }}</view>
            </view>
          </view>
        </view>
      </template>

      <view v-else class="placeholder-card">
        <text class="placeholder-title">{{ texts.emailComingTitle }}</text>
        <text class="placeholder-desc">{{ texts.emailComingDesc }}</text>
      </view>

      <view class="tips-card">
        <text class="tips-title">{{ texts.tipsTitle }}</text>
        <text class="tips-text">{{ texts.tip1 }}</text>
        <text class="tips-text">{{ texts.tip2 }}</text>
        <text class="tips-text">{{ texts.tip3 }}</text>
      </view>
    </view>

    <view class="submit-bar" v-if="verifyMode === 'student_card'">
      <view class="submit-btn" @click="submitVerify">{{ submitting ? texts.submitting : texts.submit }}</view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, verifyStudent, uploadProfileImage, downloadStudentCardImage } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      uploading: false,
      submitting: false,
      studentCardPreviewSrc: '',
      verifyMode: 'student_card',
      profile: {
        authStatus: 0
      },
      form: {
        realName: '',
        studentId: '',
        school: '',
        department: '',
        studentCardImageUrl: ''
      },
      texts: {
        title: '\u5b66\u751f\u8ba4\u8bc1',
        heroTitle: '\u8eab\u4efd\u8ba4\u8bc1',
        heroSub: '\u5b8c\u6210\u8ba4\u8bc1\u540e\u53ef\u53d1\u5e03\u4e66\u7c4d\u3001\u63d0\u5347\u4fe1\u8a89\u5c55\u793a\uff0c\u5ba1\u6838\u7ed3\u679c\u6682\u4ee5\u72b6\u6001\u5b57\u6bb5\u8868\u793a\u3002',
        realName: '\u771f\u5b9e\u59d3\u540d',
        realNamePlaceholder: '\u8bf7\u8f93\u5165\u771f\u5b9e\u59d3\u540d',
        studentId: '\u5b66\u53f7',
        studentIdPlaceholder: '\u8bf7\u8f93\u5165\u5b66\u53f7',
        school: '\u5b66\u6821',
        schoolPlaceholder: '\u8bf7\u8f93\u5165\u5b66\u6821\u540d\u79f0',
        department: '\u9662\u7cfb',
        departmentPlaceholder: '\u8bf7\u8f93\u5165\u9662\u7cfb\u540d\u79f0',
        studentCard: '\u5b66\u751f\u8bc1\u7167\u7247',
        required: '\u5fc5\u586b',
        uploading: '\u4e0a\u4f20\u4e2d...',
        uploadText: '\u4e0a\u4f20\u5b66\u751f\u8bc1\u6b63\u9762\u7167\u7247',
        reupload: '\u91cd\u65b0\u4e0a\u4f20',
        remove: '\u79fb\u9664',
        emailComingTitle: '\u6559\u80b2\u90ae\u7bb1\u9a8c\u8bc1\u5373\u5c06\u5f00\u653e',
        emailComingDesc: '\u672c\u6b21\u5148\u5b8c\u6210\u5b66\u751f\u8bc1\u8ba4\u8bc1\u4e3b\u6d41\u7a0b\uff0c\u6559\u80b2\u90ae\u7bb1\u9a8c\u8bc1\u7801\u94fe\u8def\u540e\u7eed\u8865\u5145\u3002',
        tipsTitle: '\u8bf4\u660e',
        tip1: '1. \u5b66\u751f\u8bc1\u56fe\u7247\u4ec5\u7528\u4e8e\u5ba1\u6838\uff0c\u4e0d\u4f1a\u5728\u524d\u53f0\u516c\u5f00\u5c55\u793a\u3002',
        tip2: '2. \u63d0\u4ea4\u540e\u72b6\u6001\u4f1a\u53d8\u4e3a\u201c\u5f85\u5ba1\u6838\u201d\uff0c\u5ba1\u6838\u540e\u53f0\u672c\u6b21\u5148\u4e0d\u5b9e\u73b0\u3002',
        tip3: '3. \u5982\u9700\u4fee\u6539\u8ba4\u8bc1\u8d44\u6599\uff0c\u53ef\u5728\u672a\u5ba1\u6838\u901a\u8fc7\u524d\u91cd\u65b0\u63d0\u4ea4\u3002',
        submit: '\u63d0\u4ea4\u5ba1\u6838',
        submitting: '\u63d0\u4ea4\u4e2d...',
        uploadLoading: '\u4e0a\u4f20\u4e2d',
        uploaded: '\u56fe\u7247\u5df2\u4e0a\u4f20',
        modeStudent: '\u5b66\u751f\u8bc1\u8ba4\u8bc1',
        modeStudentDesc: '\u4e0a\u4f20\u5b66\u751f\u8bc1\u7167\u7247\u5e76\u63d0\u4ea4\u5ba1\u6838',
        modeEmail: '\u6559\u80b2\u90ae\u7bb1\u9a8c\u8bc1',
        modeEmailDesc: '\u5165\u53e3\u9884\u7559\uff0c\u540e\u7eed\u8865\u5145\u9a8c\u8bc1\u7801\u6d41\u7a0b',
        waitAudit: '\u5f85\u5ba1\u6838',
        verified: '\u5df2\u8ba4\u8bc1',
        rejected: '\u5df2\u9a73\u56de',
        unverified: '\u672a\u8ba4\u8bc1',
        emailSoon: '\u6559\u80b2\u90ae\u7bb1\u9a8c\u8bc1\u5373\u5c06\u5f00\u653e',
        currentVerified: '\u5f53\u524d\u5df2\u901a\u8fc7\u8ba4\u8bc1',
        alreadyPending: '\u5df2\u63d0\u4ea4\u5ba1\u6838\uff0c\u8bf7\u8010\u5fc3\u7b49\u5f85',
        needRealName: '\u8bf7\u586b\u5199\u771f\u5b9e\u59d3\u540d',
        needStudentId: '\u8bf7\u586b\u5199\u5b66\u53f7',
        needCard: '\u8bf7\u4e0a\u4f20\u5b66\u751f\u8bc1\u7167\u7247',
        previewLoading: '\u56fe\u7247\u52a0\u8f7d\u4e2d...',
        submitSuccess: '\u5df2\u63d0\u4ea4\u5ba1\u6838'
      }
    };
  },
  computed: {
    verifyModes() {
      return [
        { key: 'student_card', title: this.texts.modeStudent, desc: this.texts.modeStudentDesc },
        { key: 'edu_email', title: this.texts.modeEmail, desc: this.texts.modeEmailDesc }
      ];
    },
    statusText() {
      const status = Number(this.profile.authStatus || 0);
      if (status === 1) return this.texts.waitAudit;
      if (status === 2) return this.texts.verified;
      if (status === 3) return this.texts.rejected;
      return this.texts.unverified;
    },
    statusClass() {
      const status = Number(this.profile.authStatus || 0);
      if (status === 1) return 'pending';
      if (status === 2) return 'approved';
      if (status === 3) return 'rejected';
      return 'unverified';
    }
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
        this.profile = data;
        this.form.realName = data.realName || '';
        this.form.studentId = data.studentId || '';
        this.form.school = data.school || '';
        this.form.department = data.department || '';
        this.form.studentCardImageUrl = data.studentCardImageUrl || '';
        this.loadStudentCardPreview();
        if (data.verifyType === 'edu_email') {
          this.verifyMode = 'edu_email';
        }
      } catch (error) {
        console.error('fetchProfile failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    switchMode(mode) {
      this.verifyMode = mode;
      if (mode === 'edu_email') {
        uni.showToast({ title: this.texts.emailSoon, icon: 'none' });
      }
    },
    async loadStudentCardPreview(keepCurrent = false) {
      const url = this.form.studentCardImageUrl;
      if (!keepCurrent) {
        this.studentCardPreviewSrc = '';
      }
      if (!url) return;
      if (/^(wxfile:\/\/|blob:|data:)/i.test(url)) {
        this.studentCardPreviewSrc = url;
        return;
      }
      try {
        this.studentCardPreviewSrc = await downloadStudentCardImage(url);
      } catch (error) {
        console.error('download student card failed', error);
      }
    },
    chooseStudentCard() {
      uni.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const filePath = (res.tempFilePaths || [])[0];
          if (!filePath) return;
          this.uploading = true;
          uni.showLoading({ title: this.texts.uploadLoading, mask: true });
          try {
            const result = await uploadProfileImage(filePath, 'studentCard');
            this.form.studentCardImageUrl = result.url || '';
            this.studentCardPreviewSrc = filePath;
            this.loadStudentCardPreview(true);
            uni.showToast({ title: this.texts.uploaded, icon: 'success' });
          } catch (error) {
            console.error('upload student card failed', error);
          } finally {
            this.uploading = false;
            uni.hideLoading();
          }
        }
      });
    },
    removeStudentCard() {
      this.form.studentCardImageUrl = '';
      this.studentCardPreviewSrc = '';
    },
    previewStudentCard() {
      if (!this.studentCardPreviewSrc) return;
      uni.previewImage({
        urls: [this.studentCardPreviewSrc],
        current: this.studentCardPreviewSrc
      });
    },
    async submitVerify() {
      if (this.submitting || this.verifyMode !== 'student_card') return;
      if (Number(this.profile.authStatus) === 2) {
        uni.showToast({ title: this.texts.currentVerified, icon: 'none' });
        return;
      }
      if (Number(this.profile.authStatus) === 1) {
        uni.showToast({ title: this.texts.alreadyPending, icon: 'none' });
        return;
      }
      if (!this.form.realName.trim()) {
        uni.showToast({ title: this.texts.needRealName, icon: 'none' });
        return;
      }
      if (!this.form.studentId.trim()) {
        uni.showToast({ title: this.texts.needStudentId, icon: 'none' });
        return;
      }
      if (!this.form.studentCardImageUrl) {
        uni.showToast({ title: this.texts.needCard, icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        const data = await verifyStudent({
          realName: this.form.realName,
          studentId: this.form.studentId,
          school: this.form.school,
          department: this.form.department,
          studentCardImageUrl: this.form.studentCardImageUrl,
          verifyType: 'student_card'
        });
        this.profile = { ...this.profile, ...(data || {}) };
        uni.showToast({ title: this.texts.submitSuccess, icon: 'success' });
        setTimeout(() => {
          uni.switchTab({ url: '/pages/my/my' });
        }, 500);
      } catch (error) {
        console.error('verifyStudent failed', error);
      } finally {
        this.submitting = false;
      }
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
.content { padding: 0 20rpx 150rpx; }
.hero-card { background: linear-gradient(135deg, #1f5eff 0%, #143a7b 100%); border-radius: 24rpx; padding: 24rpx; color: #ffffff; }
.hero-main { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.hero-title { display: block; font-size: 36rpx; font-weight: 700; }
.hero-sub { display: block; margin-top: 8rpx; font-size: 24rpx; line-height: 1.5; opacity: 0.92; }
.status-chip { padding: 10rpx 16rpx; border-radius: 999rpx; font-size: 22rpx; white-space: nowrap; background: rgba(255, 255, 255, 0.18); }
.status-chip.pending { background: rgba(255, 243, 214, 0.26); }
.status-chip.approved { background: rgba(221, 252, 232, 0.25); }
.status-chip.rejected { background: rgba(255, 227, 227, 0.24); }
.mode-card, .section-card, .placeholder-card, .tips-card { margin-top: 16rpx; background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.mode-card { display: flex; gap: 16rpx; }
.mode-item { flex: 1; border-radius: 18rpx; padding: 18rpx; background: #f4f7fb; border: 2rpx solid transparent; }
.mode-item.active { background: #edf3ff; border-color: #1f5eff; }
.mode-title { display: block; font-size: 28rpx; color: #2b3f53; font-weight: 700; }
.mode-desc { display: block; margin-top: 8rpx; font-size: 22rpx; color: #70859c; line-height: 1.45; }
.field-row { margin-bottom: 16rpx; }
.field-row:last-child { margin-bottom: 0; }
.label { display: block; font-size: 24rpx; color: #5d7186; margin-bottom: 8rpx; }
.input { width: 100%; height: 72rpx; box-sizing: border-box; border-radius: 14rpx; background: #f3f7fb; padding: 0 20rpx; font-size: 26rpx; color: #2b3f53; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14rpx; }
.section-title { font-size: 28rpx; color: #2b3f53; font-weight: 700; }
.section-tip { font-size: 22rpx; color: #d05a36; }
.upload-box { height: 220rpx; border-radius: 16rpx; border: 2rpx dashed #c9d7e6; background: #f8fbff; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.upload-icon { font-size: 52rpx; color: #7f98b3; }
.upload-text { margin-top: 10rpx; font-size: 24rpx; color: #7f98b3; }
.image-wrap { border-radius: 16rpx; overflow: hidden; background: #f3f7fb; }
.preview { width: 100%; height: 320rpx; background: #eef2f6; }
.preview-loading { display: flex; align-items: center; justify-content: center; color: #7f98b3; font-size: 24rpx; }
.image-actions { display: flex; gap: 12rpx; padding: 16rpx; }
.image-btn { flex: 1; height: 68rpx; border-radius: 14rpx; background: #1f5eff; color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.image-btn.ghost { background: #eef3f8; color: #5f758a; }
.placeholder-title, .tips-title { display: block; font-size: 28rpx; color: #2b3f53; font-weight: 700; }
.placeholder-desc, .tips-text { display: block; margin-top: 10rpx; font-size: 22rpx; color: #73879b; line-height: 1.55; }
.submit-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.submit-btn { height: 84rpx; border-radius: 16rpx; background: #173b75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
