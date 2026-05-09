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
        <text class="header-title">{{ pathInfo.title }}</text>
        <view class="header-placeholder"></view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="path-info-card">
        <image v-if="pathInfo.coverImage" class="path-cover" :src="pathInfo.coverImage" mode="aspectFill"></image>
        <view class="title-row">
          <text class="path-title">{{ pathInfo.title }}</text>
          <text class="difficulty">{{ pathInfo.difficulty }}</text>
        </view>
        <text v-if="isCreator && pathInfo.coverImageStatusLabel" class="cover-audit">{{ texts.coverAudit }}{{ pathInfo.coverImageStatusLabel }}</text>
        <text class="meta-text">{{ texts.creator }}{{ pathInfo.creator }}</text>
        <text class="meta-text">{{ texts.duration }}{{ pathInfo.totalDuration }}</text>
        <text class="desc-text">{{ pathInfo.description }}</text>
        <view class="path-actions-row">
          <view class="path-action-chip" @click="handleShare">{{ texts.share }}</view>
          <view class="path-action-chip" :class="{ active: isFavorite }" @click="toggleFavoriteAction">
            {{ isFavorite ? texts.favoritedShort : texts.favorite }}
          </view>
        </view>
      </view>

      <view class="progress-card">
        <view class="progress-head">
          <text class="section-title">{{ texts.progress }}</text>
          <text class="progress-text">{{ completedCount }}/{{ totalNodeCount }} {{ texts.nodeDone }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
        </view>
      </view>

      <view class="nodes-card">
        <text class="section-title">{{ texts.nodes }}</text>
        <view class="nodes-list">
          <view class="node-item" :class="{ completed: node.completed }" v-for="(node, index) in nodes" :key="node.id || index">
            <view class="timeline-col">
              <view class="status-dot" :class="{ done: node.completed }">
                <text class="dot-text">{{ node.completed ? texts.doneMark : index + 1 }}</text>
              </view>
              <view v-if="index !== nodes.length - 1" class="line"></view>
            </view>

            <view class="node-main" @click="openNodeDetail(node)">
              <view class="node-top">
                <view class="node-title-wrap">
                  <text class="node-title">{{ node.title }}</text>
                  <text class="node-duration">{{ node.duration }}</text>
                </view>
                <view
                  v-if="!isCreator && started"
                  class="check-btn"
                  :class="{ done: node.completed }"
                  @click.stop="toggleNodeCompleted(node)"
                >
                  {{ node.completed ? texts.doneMark : texts.checkMark }}
                </view>
              </view>
              <text class="node-desc" v-if="node.description">{{ node.description }}</text>
              <view class="node-action-row">
                <text class="node-status" :class="{ done: node.completed }">{{ node.completed ? texts.done : texts.todo }}</text>
                <view v-if="node.resourceCount > 0" class="resource-btn" @click.stop="viewResources(node)">
                  {{ texts.viewResourcePrefix }}{{ node.resourceCount }}{{ texts.viewResourceSuffix }}
                </view>
                <text class="detail-hint">{{ texts.nodeDetailHint }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <view class="bottom-bar">
      <view v-if="isCreator" class="main-action-btn" @click="handleMainAction">
        {{ texts.editPath }}
      </view>
      <view v-else-if="started" class="bottom-actions">
        <view class="main-action-btn continue-btn" @click="handleMainAction">{{ texts.continueLearning }}</view>
        <view class="cancel-action-btn" @click="handleCancelLearning">{{ texts.cancelLearning }}</view>
      </view>
      <view v-else class="main-action-btn" @click="handleMainAction">
        {{ texts.startLearning }}
      </view>
    </view>
  </view>
</template>

<script>
import { getFavoriteStatus, toggleFavorite } from '../../utils/api/favorite';
import { cancelPathLearning, completePathNode, getPathDetail, startPathLearning } from '../../utils/api/path';
import { recordBrowseHistory } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl, hasValidSession } from '../../utils/auth';

function toBooleanFlag(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  const normalized = String(value).toLowerCase();
  return normalized === '1' || normalized === 'true' || normalized === 'yes';
}

function normalizeNumericId(value) {
  const text = String(value || '').trim();
  if (!text) return '';
  return /^\d+$/.test(text) ? text : '';
}

function normalizeNode(node, index) {
  const resources = Array.isArray(node.resources) ? node.resources : [];
  return {
    id: node.id || `node-${index + 1}`,
    title: node.title || `Node ${index + 1}`,
    description: node.description || '',
    duration: node.duration || node.estimatedDuration || '1h',
    completed: Boolean(node.completed),
    resourceCount: Number(node.resourceCount || resources.length || 0),
    resourceIds: Array.isArray(node.resourceIds) ? node.resourceIds : [],
    resources,
    learningGoal: node.learningGoal || '',
    learningMethod: node.learningMethod || '',
    deliverable: node.deliverable || '',
    learningSteps: Array.isArray(node.learningSteps) ? node.learningSteps : []
  };
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      validPathId: '',
      isFavorite: false,
      isCreator: false,
      started: false,
      progressPercent: 0,
      completedCount: 0,
      pathInfo: {
        id: '',
        title: '',
        creator: '',
        difficulty: '',
        totalDuration: '',
        description: ''
      },
      nodes: [],
      texts: {
        share: '\u5206\u4eab',
        favorite: '\u6536\u85cf',
        favoritedShort: '\u5df2\u85cf',
        creator: '\u521b\u5efa\u8005\uff1a',
        duration: '\u9884\u4f30\u603b\u65f6\u957f\uff1a',
        progress: '\u5b66\u4e60\u8fdb\u5ea6',
        nodeDone: '\u8282\u70b9\u5b8c\u6210',
        nodes: '\u8def\u5f84\u8282\u70b9',
        doneMark: '\u2713',
        checkMark: '',
        done: '\u5df2\u5b8c\u6210',
        todo: '\u672a\u5b8c\u6210',
        viewResourcePrefix: '\u67e5\u770b\u8d44\u6e90\uff08',
        viewResourceSuffix: '\uff09',
        nodeDetailHint: '\u8fdb\u5165\u8be6\u60c5',
        markDone: '\u6807\u8bb0\u5b8c\u6210',
        undoDone: '\u53d6\u6d88\u5b8c\u6210',
        editPath: '\u7f16\u8f91\u8def\u5f84',
        startLearning: '\u5f00\u59cb\u5b66\u4e60',
        continueLearning: '\u7ee7\u7eed\u5b66\u4e60',
        cancelLearning: '\u53d6\u6d88\u5b66\u4e60',
        startLearningToast: '\u5df2\u5f00\u59cb\u5b66\u4e60',
        cancelLearningToast: '\u5df2\u53d6\u6d88\u5b66\u4e60',
        cancelConfirmTitle: '\u53d6\u6d88\u5b66\u4e60',
        cancelConfirmContent: '\u53d6\u6d88\u540e\uff0c\u8be5\u8def\u5f84\u5c06\u4ece\u6211\u7684\u8def\u5f84\u79fb\u9664\uff0c\u8282\u70b9\u8fdb\u5ea6\u4e5f\u4f1a\u6e05\u7a7a\u3002',
        shareDev: '\u5206\u4eab\u529f\u80fd\u5f00\u53d1\u4e2d',
        unavailable: '\u5f53\u524d\u8def\u5f84\u6682\u672a\u63a5\u5165\u8be6\u60c5\u6570\u636e',
        coverAudit: '\u5c01\u9762\u5ba1\u6838\uff1a'
      }
    };
  },
  computed: {
    totalNodeCount() {
      return this.nodes.length;
    }
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
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }

    this.pathInfo.id = decodeURIComponent(options.pathId || '');
    this.validPathId = normalizeNumericId(this.pathInfo.id);
    this.pathInfo.title = decodeURIComponent(options.title || '');
    this.pathInfo.creator = decodeURIComponent(options.creator || '');
    this.pathInfo.difficulty = decodeURIComponent(options.difficulty || '');
    this.pathInfo.totalDuration = decodeURIComponent(options.totalDuration || '');
    this.pathInfo.description = decodeURIComponent(options.description || '');
    this.isCreator = toBooleanFlag(options.isCreator, false);

    this.recordHistory(options);
    if (this.validPathId) {
      this.fetchPathDetail();
      this.fetchFavoriteState();
    }
  },
  methods: {
    buildRouteUrl(options = {}) {
      const id = encodeURIComponent(this.validPathId || options.pathId || '');
      return id ? `/pages/path/detail?pathId=${id}` : '/pages/path/detail';
    },
    async recordHistory(options) {
      if (!this.validPathId || !hasValidSession()) return;
      try {
        await recordBrowseHistory({
          targetType: 'path',
          targetId: this.validPathId,
          title: this.pathInfo.title,
          subTitle: this.pathInfo.creator,
          coverUrl: this.pathInfo.coverImage || '/static/logo.png',
          routeUrl: this.buildRouteUrl(options)
        });
      } catch (error) {
        console.error('record path history failed', error);
      }
    },
    async fetchFavoriteState() {
      if (!hasValidSession() || !this.validPathId) return;
      try {
        const data = await getFavoriteStatus('path', this.validPathId);
        this.isFavorite = Boolean(data?.favorited);
      } catch (error) {
        console.error('getFavoriteStatus failed', error);
      }
    },
    async fetchPathDetail() {
      try {
        const data = await getPathDetail(this.validPathId);
        if (!data || !data.id) return;
        this.pathInfo = {
          id: data.id,
          title: data.title || this.pathInfo.title,
          creator: data.creator || this.pathInfo.creator,
          difficulty: data.difficulty || this.pathInfo.difficulty,
          totalDuration: data.totalDuration || this.pathInfo.totalDuration,
          description: data.description || this.pathInfo.description,
          coverImage: data.coverImage || '',
          coverImageStatus: Number(data.coverImageStatus || 0),
          coverImageStatusLabel: data.coverImageStatusLabel || ''
        };
        this.nodes = Array.isArray(data.nodes) && data.nodes.length
          ? data.nodes.map((node, index) => normalizeNode(node, index))
          : [];
        this.isCreator = Boolean(data.isCreator);
        this.started = Boolean(data.started);
        this.completedCount = Number(data.completedCount || 0);
        this.progressPercent = Number(data.progressPercent || 0);
      } catch (error) {
        console.error('fetchPathDetail failed', error);
      }
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    handleShare() {
      uni.showToast({ title: this.texts.shareDev, icon: 'none' });
    },
    async toggleFavoriteAction() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      if (!this.validPathId) {
        uni.showToast({ title: this.texts.unavailable, icon: 'none' });
        return;
      }
      try {
        const data = await toggleFavorite('path', this.validPathId);
        this.isFavorite = Boolean(data?.favorited);
      } catch (error) {
        console.error('toggleFavorite failed', error);
      }
    },
    viewResources(node) {
      uni.navigateTo({
        url: `/pages/resources/list?pathNodeId=${encodeURIComponent(node.id || '')}&title=${encodeURIComponent((node.title || '') + this.texts.nodes)}`
      });
    },
    openNodeDetail(node) {
      if (!node || !node.id || !this.validPathId) {
        uni.showToast({ title: this.texts.unavailable, icon: 'none' });
        return;
      }
      uni.navigateTo({
        url: `/pages/path/node-detail?pathId=${encodeURIComponent(this.validPathId)}&nodeId=${encodeURIComponent(node.id)}`
      });
    },
    async handleMainAction() {
      if (this.isCreator) {
        const params = [`pathId=${encodeURIComponent(this.validPathId || '')}`].join('&');
        uni.navigateTo({ url: `/pages/path/create?${params}` });
        return;
      }
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      if (!this.validPathId) {
        uni.showToast({ title: this.texts.unavailable, icon: 'none' });
        return;
      }
      try {
        const data = await startPathLearning(this.validPathId);
        this.started = true;
        this.completedCount = Number(data && data.completedCount !== undefined ? data.completedCount : this.completedCount || 0);
        this.progressPercent = Number(data && data.progressPercent !== undefined ? data.progressPercent : this.progressPercent || 0);
        uni.showToast({ title: this.texts.startLearningToast, icon: 'success' });
      } catch (error) {
        console.error('startPathLearning failed', error);
      }
    },
    handleCancelLearning() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      if (!this.validPathId) {
        uni.showToast({ title: this.texts.unavailable, icon: 'none' });
        return;
      }
      uni.showModal({
        title: this.texts.cancelConfirmTitle,
        content: this.texts.cancelConfirmContent,
        success: async (res) => {
          if (!res.confirm) return;
          try {
            const data = await cancelPathLearning(this.validPathId);
            this.started = Boolean(data && data.started);
            this.completedCount = 0;
            this.progressPercent = 0;
            this.nodes = this.nodes.map((node) => ({ ...node, completed: false }));
            uni.showToast({ title: this.texts.cancelLearningToast, icon: 'none' });
          } catch (error) {
            console.error('cancelPathLearning failed', error);
          }
        }
      });
    },
    async toggleNodeCompleted(node) {
      if (!ensureLoggedIn(getCurrentPageUrl())) return;
      if (!this.validPathId) {
        uni.showToast({ title: this.texts.unavailable, icon: 'none' });
        return;
      }
      try {
        const nextCompleted = !node.completed;
        const data = await completePathNode(this.validPathId, node.id, nextCompleted);
        node.completed = nextCompleted;
        this.completedCount = Number(data?.completedCount || 0);
        this.progressPercent = Number(data?.progressPercent || 0);
      } catch (error) {
        console.error('completePathNode failed', error);
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 300rpx, #eef3fb 100%); }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 30; box-sizing: border-box; padding-left: 20rpx; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(10px); border-bottom: 1rpx solid #dfe8f4; box-shadow: 0 10rpx 28rpx rgba(23, 32, 51, 0.06); }
.header-inner { height: 100%; display: flex; align-items: center; gap: 14rpx; padding-bottom: 12rpx; box-sizing: border-box; }
.back-btn { width: 72rpx; height: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; font-size: 30rpx; color: #2d3d52; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.content { padding: 18rpx 20rpx 0; }
.path-info-card, .progress-card, .nodes-card { background: #ffffff; border-radius: 30rpx; padding: 26rpx; border: 1rpx solid #e2eaf5; box-shadow: 0 16rpx 36rpx rgba(23, 32, 51, 0.07); margin-bottom: 18rpx; }
.path-cover { width: 100%; height: 300rpx; border-radius: 18rpx; background: #eef2f8; margin-bottom: 20rpx; }
.title-row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.path-title { flex: 1; min-width: 0; font-size: 36rpx; color: #263442; font-weight: 700; }
.difficulty { padding: 8rpx 18rpx; border-radius: 999rpx; background: #e8efff; color: #1f5eff; font-size: 22rpx; }
.cover-audit { display: block; margin-top: 10rpx; font-size: 22rpx; color: #1f5eff; }
.meta-text { display: block; margin-top: 12rpx; font-size: 24rpx; color: #6f7c8e; }
.desc-text { display: block; margin-top: 18rpx; font-size: 24rpx; line-height: 1.8; color: #4d6278; }
.path-actions-row { margin-top: 20rpx; display: flex; gap: 12rpx; }
.path-action-chip { height: 60rpx; padding: 0 22rpx; border-radius: 16rpx; background: #eef3f9; color: #536b84; font-size: 24rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.path-action-chip.active { background: #e8efff; color: #1f5eff; }
.progress-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.section-title { font-size: 30rpx; color: #263442; font-weight: 700; }
.progress-text { font-size: 24rpx; color: #68809a; }
.progress-track { margin-top: 20rpx; width: 100%; height: 16rpx; border-radius: 999rpx; background: #eef2f8; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 999rpx; background: linear-gradient(90deg, #0f766e 0%, #14b8a6 100%); }
.nodes-list { margin-top: 12rpx; display: flex; flex-direction: column; gap: 18rpx; }
.node-item { display: flex; gap: 16rpx; }
.node-item.completed .node-main { background: #f1f3f5; }
.node-item.completed .node-title,
.node-item.completed .node-desc,
.node-item.completed .node-duration { color: #99a4b1; }
.timeline-col { width: 56rpx; display: flex; flex-direction: column; align-items: center; flex-shrink: 0; }
.status-dot { width: 44rpx; height: 44rpx; border-radius: 50%; background: #e5ebf4; display: flex; align-items: center; justify-content: center; color: #5e7388; font-size: 22rpx; font-weight: 700; }
.status-dot.done { background: #c8d0da; color: #ffffff; }
.dot-text { font-size: 20rpx; font-weight: 700; }
.line { flex: 1; width: 4rpx; background: #e4eaf2; margin-top: 8rpx; }
.node-main { flex: 1; background: #f7faff; border-radius: 22rpx; padding: 20rpx; border: 1rpx solid #e2eaf5; }
.node-top { display: flex; align-items: center; justify-content: space-between; gap: 14rpx; }
.node-title-wrap { flex: 1; min-width: 0; }
.node-title { display: block; font-size: 28rpx; color: #25384d; font-weight: 700; line-height: 1.4; }
.node-duration { display: block; margin-top: 6rpx; font-size: 22rpx; color: #7590a9; }
.check-btn { width: 58rpx; height: 58rpx; border-radius: 50%; border: 2rpx solid #c7d2df; background: #ffffff; color: #8a9bad; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 700; flex-shrink: 0; }
.check-btn.done { border-color: #bfc8d3; background: #dfe4ea; color: #7a8795; }
.node-desc { display: block; margin-top: 10rpx; font-size: 23rpx; color: #6a7d92; line-height: 1.6; }
.node-action-row { margin-top: 14rpx; display: flex; flex-wrap: wrap; gap: 12rpx; align-items: center; }
.node-status { font-size: 22rpx; color: #8799ad; }
.node-status.done { color: #8a97a6; font-weight: 700; }
.resource-btn { height: 56rpx; padding: 0 18rpx; border-radius: 14rpx; font-size: 22rpx; display: flex; align-items: center; justify-content: center; }
.resource-btn { background: #e8efff; color: #1f5eff; }
.detail-hint { margin-left: auto; font-size: 22rpx; color: #7b8da0; }
.bottom-space { height: 140rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.main-action-btn { height: 84rpx; border-radius: 22rpx; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; box-shadow: 0 12rpx 26rpx rgba(31, 94, 255, 0.2); }
.bottom-actions { display: flex; gap: 12rpx; }
.continue-btn { flex: 1; }
.cancel-action-btn { width: 190rpx; height: 84rpx; border-radius: 18rpx; background: #eef2f8; color: #53657a; font-size: 28rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
