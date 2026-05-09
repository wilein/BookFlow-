<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ texts.title }}</text>
      <view class="save-btn" @click="submitPost">{{ texts.publish }}</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="card">
        <text class="label">{{ texts.type }}</text>
        <view class="type-row">
          <view
            v-for="item in typeOptions"
            :key="item.value"
            class="type-btn"
            :class="{ active: form.type === item.value }"
            @click="form.type = item.value"
          >
            {{ item.label }}
          </view>
        </view>
      </view>

      <view v-if="form.type === 'path'" class="card">
        <text class="label">{{ texts.sharedPath }}</text>
        <view v-if="pathOptions.length" class="path-options">
          <view
            v-for="item in pathOptions"
            :key="item.id"
            class="path-option"
            :class="{ active: form.sharedPathId === item.id }"
            @click="form.sharedPathId = form.sharedPathId === item.id ? '' : item.id"
          >
            <text class="path-name">{{ item.title }}</text>
            <text class="path-meta">{{ item.difficulty }} · {{ item.totalDuration }} · {{ item.nodeCount || 0 }}{{ texts.nodeUnit }}</text>
          </view>
        </view>
        <text v-else class="empty-tip">{{ texts.emptyPath }}</text>
      </view>

      <view class="card">
        <text class="label">{{ texts.postTitle }}</text>
        <input class="input" v-model="form.title" :placeholder="texts.titlePlaceholder" />
      </view>

      <view class="card">
        <text class="label">{{ texts.content }}</text>
        <textarea class="textarea" v-model="form.content" :placeholder="texts.contentPlaceholder"></textarea>
      </view>
    </view>
  </view>
</template>

<script>
import { createCommunityPost } from '../../utils/api/community';
import { getMyPaths } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TEXTS = {
  title: '\u53d1\u5e03\u52a8\u6001',
  publish: '\u53d1\u5e03',
  type: '\u7c7b\u578b',
  recommend: '\u63a8\u8350',
  review: '\u4e66\u8bc4',
  qa: '\u95ee\u7b54',
  path: '\u8def\u5f84',
  postTitle: '\u6807\u9898',
  titlePlaceholder: '\u8bf7\u8f93\u5165\u6807\u9898',
  content: '\u5185\u5bb9',
  contentPlaceholder: '\u5206\u4eab\u4f60\u7684\u5b66\u4e60\u5fc3\u5f97\u3001\u4e66\u8bc4\u3001\u95ee\u9898\u6216\u8def\u5f84\u6574\u7406',
  sharedPath: '\u5173\u8054\u5b66\u4e60\u8def\u5f84\uff08\u53ef\u9009\uff09',
  emptyPath: '\u6682\u65e0\u53ef\u5206\u4eab\u7684\u81ea\u5efa\u8def\u5f84',
  nodeUnit: '\u4e2a\u8282\u70b9',
  needTitle: '\u8bf7\u586b\u5199\u6807\u9898',
  needContent: '\u8bf7\u586b\u5199\u5185\u5bb9',
  success: '\u53d1\u5e03\u6210\u529f'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      typeOptions: [
        { label: TEXTS.recommend, value: 'recommend' },
        { label: TEXTS.review, value: 'review' },
        { label: TEXTS.qa, value: 'qa' },
        { label: TEXTS.path, value: 'path' }
      ],
      form: {
        type: 'recommend',
        title: '',
        content: '',
        sharedPathId: ''
      },
      pathOptions: []
    };
  },
  onLoad() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.fetchMyPaths();
  },
  methods: {
    async fetchMyPaths() {
      try {
        this.pathOptions = (await getMyPaths()) || [];
      } catch (error) {
        console.error('getMyPaths failed', error);
      }
    },
    async submitPost() {
      if (!this.form.title.trim()) {
        uni.showToast({ title: TEXTS.needTitle, icon: 'none' });
        return;
      }
      if (!this.form.content.trim()) {
        uni.showToast({ title: TEXTS.needContent, icon: 'none' });
        return;
      }
      try {
        await createCommunityPost({
          ...this.form,
          sharedPathId: this.form.type === 'path' ? this.form.sharedPathId : ''
        });
        uni.showToast({ title: TEXTS.success, icon: 'success' });
        setTimeout(() => {
          uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/community/community' }) });
        }, 400);
      } catch (error) {
        console.error('createCommunityPost failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/community/community' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.save-btn { min-width: 96rpx; height: 64rpx; border-radius: 16rpx; background: #1f5eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; }
.content { padding: 18rpx 20rpx 40rpx; }
.card { background: #fff; border-radius: 22rpx; padding: 20rpx; margin-bottom: 16rpx; }
.label { display: block; font-size: 28rpx; color: #2b3f53; font-weight: 700; margin-bottom: 16rpx; }
.type-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.type-btn { padding: 12rpx 22rpx; border-radius: 999rpx; background: #eef2f8; color: #5d7085; font-size: 24rpx; }
.type-btn.active { background: #e6efff; color: #1f5eff; font-weight: 700; }
.path-options { display: flex; flex-direction: column; gap: 12rpx; }
.path-option { border-radius: 16rpx; background: #f3f6fb; padding: 16rpx; border: 2rpx solid transparent; }
.path-option.active { background: #edf3ff; border-color: #1f5eff; }
.path-name { display: block; font-size: 27rpx; color: #2b3f53; font-weight: 700; line-height: 1.4; }
.path-meta { display: block; margin-top: 8rpx; font-size: 22rpx; color: #718398; }
.empty-tip { display: block; font-size: 24rpx; color: #7d8fa2; }
.input { height: 80rpx; line-height: 80rpx; border-radius: 16rpx; background: #f3f6fb; padding: 0 20rpx; font-size: 26rpx; color: #2d3d52; }
.textarea { min-height: 260rpx; border-radius: 16rpx; background: #f3f6fb; padding: 20rpx; font-size: 26rpx; color: #2d3d52; box-sizing: border-box; width: 100%; }
</style>
