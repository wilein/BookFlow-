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
        <text class="title">{{ pageTitle }}</text>
        <view class="submit-mini" @click="submit">{{ submitText }}</view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="form-card">
        <text v-if="displayTitle" class="field-title">{{ textMap.target }}</text>
        <input v-if="displayTitle" class="input read-only" type="text" :value="displayTitle" disabled />

        <block v-if="mode === 'generic'">
          <text class="field-title">{{ textMap.type }}</text>
          <view class="type-row">
            <view
              v-for="item in typeOptions"
              :key="item.value"
              class="type-chip"
              :class="{ active: form.feedbackType === item.value }"
              @click="form.feedbackType = item.value"
            >
              {{ item.label }}
            </view>
          </view>

          <text class="field-title">{{ textMap.contact }}</text>
          <input
            class="input"
            type="text"
            v-model="form.contact"
            :placeholder="textMap.contactPlaceholder"
          />
        </block>

        <text v-if="mode === 'order-report' || mode === 'community-report'" class="field-title">{{ textMap.reasonType }}</text>
        <view v-if="mode === 'order-report' || mode === 'community-report'" class="type-row">
          <view
            v-for="item in reasonOptions"
            :key="item.value"
            class="type-chip"
            :class="{ active: form.reasonType === item.value }"
            @click="form.reasonType = item.value"
          >
            {{ item.label }}
          </view>
        </view>

        <text v-if="mode === 'order-issue-create'" class="field-title">{{ textMap.issueType }}</text>
        <view v-if="mode === 'order-issue-create'" class="type-row">
          <view
            v-for="item in issueTypeOptions"
            :key="item.value"
            class="type-chip"
            :class="{ active: form.issueType === item.value }"
            @click="form.issueType = item.value"
          >
            {{ item.label }}
          </view>
        </view>

        <text class="field-title">{{ contentLabel }}</text>
        <textarea
          class="textarea"
          v-model="form.content"
          :placeholder="contentPlaceholder"
          maxlength="1000"
        />

        <text class="field-title">{{ textMap.page }}</text>
        <input class="input read-only" type="text" :value="form.pagePath" disabled />
      </view>

      <view class="bottom-btn" @click="submit">{{ submitText }}</view>
      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { reportCommunityPost } from '../../utils/api/community';
import { createOrderIssue, replyOrderIssue, reportOrder } from '../../utils/api/order';
import { submitFeedback } from '../../utils/api/user';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      submitting: false,
      mode: 'generic',
      orderId: '',
      issueId: '',
      postId: '',
      form: {
        feedbackType: 'bug',
        issueType: 'question',
        reasonType: 'other',
        content: '',
        contact: '',
        pagePath: ''
      },
      displayTitle: '',
      textMap: {
        title: '反馈',
        submit: '提交',
        target: '反馈对象',
        type: '反馈类型',
        issueType: '问题类型',
        reasonType: '举报原因',
        content: '内容说明',
        issueReply: '回复内容',
        problemPlaceholder: '请描述具体情况，便于我们处理。',
        replyPlaceholder: '请输入你的回复内容。',
        contact: '联系方式（可选）',
        contactPlaceholder: '微信号、QQ 或手机号',
        page: '当前页面'
      },
      typeOptions: [
        { value: 'bug', label: '功能异常' },
        { value: 'ux', label: '体验问题' },
        { value: 'suggestion', label: '产品建议' }
      ],
      issueTypeOptions: [
        { value: 'question', label: '订单疑问' },
        { value: 'after_sale', label: '售后处理' }
      ],
      reasonOptions: [
        { value: 'fraud', label: '疑似欺诈' },
        { value: 'abuse', label: '不当内容' },
        { value: 'other', label: '其他原因' }
      ]
    };
  },
  computed: {
    pageTitle() {
      const titleMap = {
        generic: '反馈',
        'order-issue-create': '订单问题',
        'order-issue-reply': '问题回复',
        'order-report': '举报订单',
        'community-report': '举报帖子'
      };
      return titleMap[this.mode] || this.textMap.title;
    },
    submitText() {
      return this.submitting ? '提交中' : this.textMap.submit;
    },
    contentLabel() {
      return this.mode === 'order-issue-reply' ? this.textMap.issueReply : this.textMap.content;
    },
    contentPlaceholder() {
      return this.mode === 'order-issue-reply' ? this.textMap.replyPlaceholder : this.textMap.problemPlaceholder;
    }
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.mode = options.mode || 'generic';
    this.orderId = options.orderId || '';
    this.issueId = options.issueId || '';
    this.postId = options.postId || '';
    this.displayTitle = decodeURIComponent(options.title || '');
    this.form.pagePath = decodeURIComponent(options.pagePath || this.getCurrentPagePath());
    if (options.issueType) {
      this.form.issueType = options.issueType;
    }
  },
  methods: {
    getCurrentPagePath() {
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      return current?.route ? `/${current.route}` : '/pages/placeholder/feedback';
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    async submit() {
      if (this.submitting) return;
      if (!this.form.content.trim()) {
        uni.showToast({ title: '请填写内容', icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        if (this.mode === 'order-issue-create') {
          await createOrderIssue({
            orderId: this.orderId,
            type: this.form.issueType,
            content: this.form.content.trim()
          });
        } else if (this.mode === 'order-issue-reply') {
          await replyOrderIssue({
            issueId: this.issueId,
            replyContent: this.form.content.trim()
          });
        } else if (this.mode === 'order-report') {
          await reportOrder({
            orderId: this.orderId,
            reasonType: this.form.reasonType,
            content: this.form.content.trim()
          });
        } else if (this.mode === 'community-report') {
          await reportCommunityPost({
            postId: this.postId,
            reasonType: this.form.reasonType,
            content: this.form.content.trim()
          });
        } else {
          if (!this.form.feedbackType) {
            uni.showToast({ title: '请选择反馈类型', icon: 'none' });
            return;
          }
          await submitFeedback({
            feedbackType: this.form.feedbackType,
            content: this.form.content.trim(),
            contact: this.form.contact.trim(),
            pagePath: this.form.pagePath
          });
        }
        uni.showToast({ title: '提交成功', icon: 'none' });
        setTimeout(() => this.goBack(), 500);
      } catch (error) {
        console.error('submit feedback failed', error);
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #f6f8fc; }
.header { position: fixed; top: 0; left: 0; right: 0; z-index: 40; box-sizing: border-box; padding-left: 20rpx; background: rgba(246, 248, 252, 0.96); backdrop-filter: blur(10px); }
.header-inner { height: 100%; display: flex; align-items: center; gap: 14rpx; padding-bottom: 12rpx; box-sizing: border-box; }
.back-btn { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.title { flex: 1; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.submit-mini { min-width: 104rpx; height: 64rpx; padding: 0 18rpx; border-radius: 999rpx; background: #173b75; color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.content { padding: 16rpx 20rpx 0; }
.form-card { background: #ffffff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.03); }
.field-title { display: block; margin-bottom: 12rpx; font-size: 24rpx; color: #6e8094; }
.field-title:not(:first-child) { margin-top: 24rpx; }
.type-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.type-chip { padding: 10rpx 18rpx; border-radius: 999rpx; background: #f2f6fb; color: #5f748a; font-size: 24rpx; }
.type-chip.active { background: #173b75; color: #ffffff; }
.input, .textarea { width: 100%; box-sizing: border-box; border-radius: 16rpx; background: #f5f8fc; padding: 18rpx 20rpx; font-size: 26rpx; color: #24364b; }
.input { height: 84rpx; }
.textarea { min-height: 260rpx; }
.read-only { color: #7f90a4; }
.bottom-btn { margin-top: 24rpx; height: 88rpx; border-radius: 18rpx; background: #173b75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.bottom-space { height: calc(88rpx + env(safe-area-inset-bottom)); }
</style>
