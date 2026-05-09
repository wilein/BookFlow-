<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }"
    >
      <view class="header-inner">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
        </view>
        <text class="header-title">添加批注</text>
        <view class="header-placeholder"></view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="hero-card">
        <text class="book-title">{{ bookTitle || '书籍批注' }}</text>
        <text class="book-sub">记录页码、位置和你的阅读想法</text>
      </view>

      <view class="card">
        <text class="label">批注类型</text>
        <view class="type-row">
          <view
            v-for="item in typeOptions"
            :key="item.value"
            class="type-chip"
            :class="{ active: form.type === item.value }"
            @click="form.type = item.value"
          >
            {{ item.icon }} {{ item.label }}
          </view>
        </view>

        <text class="label">页码</text>
        <input class="field" type="number" v-model="form.page" placeholder="例如 45" />

        <text class="label">位置描述</text>
        <input class="field" v-model="form.positionText" placeholder="例如 左上角公式旁、第三段末尾" />

        <text class="label">批注内容</text>
        <textarea class="textarea" v-model="form.content" maxlength="500" placeholder="输入你的批注内容"></textarea>

        <text class="label">批注图片（可选）</text>
        <view class="upload-card" @click="chooseImage">
          <image v-if="form.imageUrl" class="upload-image" :src="form.imageUrl" mode="aspectFill"></image>
          <view v-else class="upload-placeholder">
            <text class="upload-title">上传图片</text>
            <text class="upload-sub">可上传书页截图或标记位置</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="submit-btn" @click="submit">发布批注</view>
    </view>
  </view>
</template>

<script>
import { createAnnotation, uploadAnnotationImage } from '../../utils/api/annotation';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      bookId: '',
      bookTitle: '',
      form: {
        type: 'highlight',
        page: '1',
        positionText: '',
        content: '',
        imageUrl: ''
      },
      typeOptions: [
        { value: 'highlight', label: '重点', icon: 'H' },
        { value: 'question', label: '疑问', icon: 'Q' },
        { value: 'insight', label: '心得', icon: 'I' }
      ]
    };
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule =
      typeof uni.getMenuButtonBoundingClientRect === 'function'
        ? uni.getMenuButtonBoundingClientRect()
        : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.bookId = decodeURIComponent(options.bookId || '');
    this.bookTitle = decodeURIComponent(options.bookTitle || '');
    this.form.page = decodeURIComponent(options.page || '1') || '1';
    ensureLoggedIn(getCurrentPageUrl());
  },
  methods: {
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) });
    },
    chooseImage() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const filePath = res.tempFilePaths && res.tempFilePaths[0];
          if (!filePath) return;
          uni.showLoading({ title: '上传中...' });
          try {
            const data = await uploadAnnotationImage(filePath);
            this.form.imageUrl = data.url || '';
          } catch (error) {
            console.error('uploadAnnotationImage failed', error);
          } finally {
            uni.hideLoading();
          }
        }
      });
    },
    async submit() {
      if (!this.bookId) {
        uni.showToast({ title: '书籍参数缺失', icon: 'none' });
        return;
      }
      if (!String(this.form.content || '').trim()) {
        uni.showToast({ title: '请输入批注内容', icon: 'none' });
        return;
      }
      uni.showLoading({ title: '发布中...' });
      try {
        await createAnnotation({
          bookId: this.bookId,
          page: this.form.page,
          type: this.form.type,
          content: this.form.content.trim(),
          positionText: this.form.positionText.trim(),
          imageUrl: this.form.imageUrl
        });
        uni.hideLoading();
        uni.showToast({ title: '批注已发布', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 500);
      } catch (error) {
        uni.hideLoading();
        console.error('createAnnotation failed', error);
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 30; box-sizing: border-box; padding-left: 20rpx; background: rgba(243, 245, 248, 0.96); }
.header-inner { height: 100%; display: flex; align-items: center; gap: 14rpx; padding-bottom: 12rpx; box-sizing: border-box; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 14rpx 20rpx 160rpx; }
.hero-card { background: linear-gradient(135deg, #1f5eff 0%, #143a7b 100%); border-radius: 24rpx; padding: 24rpx; color: #ffffff; }
.book-title { display: block; font-size: 32rpx; font-weight: 700; }
.book-sub { display: block; margin-top: 10rpx; font-size: 24rpx; opacity: 0.92; }
.card { margin-top: 16rpx; background: #ffffff; border-radius: 22rpx; padding: 22rpx; }
.label { display: block; margin-top: 18rpx; margin-bottom: 12rpx; font-size: 24rpx; color: #53677f; font-weight: 700; }
.label:first-child { margin-top: 0; }
.type-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.type-chip { padding: 12rpx 18rpx; border-radius: 999rpx; background: #edf2f8; font-size: 24rpx; color: #53677f; }
.type-chip.active { background: #173b75; color: #ffffff; }
.field {
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  box-sizing: border-box;
  background: #f3f7fb;
  border-radius: 16rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #2b3f53;
}
.textarea {
  width: 100%;
  min-height: 220rpx;
  box-sizing: border-box;
  background: #f3f7fb;
  border-radius: 16rpx;
  padding: 18rpx 20rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #2b3f53;
}
.upload-card { margin-top: 6rpx; border-radius: 18rpx; overflow: hidden; background: #f3f7fb; min-height: 220rpx; }
.upload-image { width: 100%; height: 220rpx; }
.upload-placeholder { min-height: 220rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #7c8ea2; }
.upload-title { font-size: 28rpx; color: #42576d; font-weight: 700; }
.upload-sub { margin-top: 10rpx; font-size: 22rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 30; padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom)); background: rgba(255,255,255,0.98); border-top: 1rpx solid #e8eef5; }
.submit-btn { height: 84rpx; border-radius: 20rpx; background: #173b75; color: #ffffff; font-size: 28rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
