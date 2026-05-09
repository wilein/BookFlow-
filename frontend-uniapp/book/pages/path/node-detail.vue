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
      <view class="hero-card" :class="{ completed: node.completed }">
        <view class="hero-meta">
          <text class="status-pill">{{ node.completed ? texts.completed : texts.learning }}</text>
          <text class="duration-pill">{{ node.duration }}</text>
        </view>
        <text class="node-title">{{ node.title }}</text>
        <text class="node-desc">{{ node.description || texts.defaultDescription }}</text>
      </view>

      <view class="section-card">
        <view class="brief-row">
          <view class="brief-item">
            <text class="brief-label">{{ texts.goal }}</text>
            <text class="brief-text">{{ node.learningGoal }}</text>
          </view>
          <view class="brief-item">
            <text class="brief-label">{{ texts.method }}</text>
            <text class="brief-text">{{ node.learningMethod }}</text>
          </view>
          <view class="brief-item">
            <text class="brief-label">{{ texts.deliverable }}</text>
            <text class="brief-text">{{ node.deliverable }}</text>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">{{ texts.steps }}</text>
          <text class="section-sub">{{ texts.stepsSub }}</text>
        </view>
        <view class="step-list">
          <view v-for="(step, index) in learningSteps" :key="step.order || index" class="step-row">
            <view class="step-index">{{ step.order || index + 1 }}</view>
            <view class="step-main">
              <text class="step-title">{{ step.title }}</text>
              <text class="step-content">{{ step.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">{{ texts.resources }}</text>
          <text class="section-sub">{{ resources.length }}{{ texts.resourcesCount }}</text>
        </view>
        <view v-if="resources.length" class="resource-list">
          <view v-for="resource in resources" :key="resource.id" class="resource-row" @click="openResource(resource)">
            <view class="resource-icon">{{ resource.typeLabel ? resource.typeLabel.slice(0, 1) : texts.resourceShort }}</view>
            <view class="resource-main">
              <text class="resource-title">{{ resource.title || resource.name }}</text>
              <text class="resource-desc">{{ resource.description || texts.resourceDesc }}</text>
              <view class="resource-meta">
                <text>{{ resource.typeLabel || texts.resource }}</text>
                <text>{{ resource.fileFormat || inferFormat(resource) }}</text>
              </view>
            </view>
            <text class="open-text">{{ texts.open }}</text>
          </view>
        </view>
        <view v-else class="empty-resource">{{ texts.emptyResources }}</view>
      </view>

      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { getPathDetail } from '../../utils/api/path';

function normalizeId(value) {
  const text = String(value == null ? '' : value).trim();
  return /^\d+$/.test(text) ? text : '';
}

function normalizeNode(node = {}, index = 0) {
  const resources = Array.isArray(node.resources) ? node.resources : [];
  return {
    id: node.id || `node-${index + 1}`,
    title: node.title || `节点 ${index + 1}`,
    description: node.description || '',
    duration: node.duration || '时长待补充',
    completed: Boolean(node.completed),
    learningGoal: node.learningGoal || `掌握「${node.title || '当前节点'}」的核心内容，并能独立复述重点。`,
    learningMethod: node.learningMethod || '先阅读节点说明，再结合资源学习，最后通过练习和笔记完成复盘。',
    deliverable: node.deliverable || '完成一页学习笔记，整理关键知识点、练习结果和待解决问题。',
    learningSteps: Array.isArray(node.learningSteps) ? node.learningSteps : [],
    resources
  };
}

function fallbackSteps(node) {
  return [
    { order: 1, title: '读目标', content: '先看节点说明，确认本节点要解决的问题和关键词。' },
    { order: 2, title: '看资源', content: '按顺序学习关联资源，视频负责理解流程，PDF/课件负责补充细节。' },
    { order: 3, title: '做练习', content: `围绕「${node.title || '当前节点'}」完成例题、代码、思维导图或问答练习。` },
    { order: 4, title: '写复盘', content: '把关键结论和易错点写进笔记，确认掌握后回到路径页打勾。' }
  ];
}

const TEXTS = {
  title: '节点详情',
  completed: '已完成',
  learning: '待学习',
  defaultDescription: '本节点还没有补充说明，请结合学习路径目标和关联资源完成学习。',
  goal: '要学什么',
  method: '怎么学',
  deliverable: '完成标准',
  steps: '学习步骤',
  stepsSub: '按顺序完成',
  resources: '关联资源',
  resourcesCount: ' 个资源',
  resourceShort: '资',
  resource: '资源',
  resourceDesc: '点击查看资源内容',
  open: '打开',
  emptyResources: '该节点暂未关联资源，可先按节点说明完成学习。',
  filePending: '资源链接待补充',
  copied: '资源链接已复制',
  openFailed: '打开失败，链接已复制'
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      pathId: '',
      nodeId: '',
      pathInfo: {},
      node: normalizeNode()
    };
  },
  computed: {
    resources() {
      return Array.isArray(this.node.resources) ? this.node.resources : [];
    },
    learningSteps() {
      return this.node.learningSteps.length ? this.node.learningSteps : fallbackSteps(this.node);
    }
  },
  onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 54;
    this.pathId = normalizeId(options.pathId);
    this.nodeId = normalizeId(options.nodeId);
    this.fetchNode();
  },
  methods: {
    async fetchNode() {
      if (!this.pathId || !this.nodeId) return;
      try {
        const data = await getPathDetail(this.pathId);
        this.pathInfo = data || {};
        const nodes = Array.isArray(data?.nodes) ? data.nodes : [];
        const index = nodes.findIndex((item) => String(item.id) === String(this.nodeId));
        if (index >= 0) {
          this.node = normalizeNode(nodes[index], index);
        }
      } catch (error) {
        console.error('fetch node detail failed', error);
      }
    },
    inferFormat(resource) {
      const url = resource?.fileUrl || '';
      const match = url.match(/\.([a-zA-Z0-9]+)(\?|#|$)/);
      return match ? match[1].toUpperCase() : TEXTS.resource;
    },
    isDocumentResource(resource) {
      const text = `${resource?.fileFormat || ''} ${resource?.fileUrl || ''}`.toLowerCase();
      return ['.pdf', '.doc', '.docx', '.ppt', '.pptx', '.xls', '.xlsx'].some((suffix) => text.includes(suffix))
        || ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].some((suffix) => text.split(/\s+/).includes(suffix));
    },
    copyResourceUrl(url, title = TEXTS.copied) {
      uni.setClipboardData({
        data: url,
        success: () => uni.showToast({ title, icon: 'none' })
      });
    },
    openResource(resource) {
      if (!resource?.id) return;
      uni.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(resource.id)}` });
    },
    goBack() {
      uni.navigateBack({
        fail: () => uni.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(this.pathId)}` })
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: rgba(243, 245, 248, 0.96); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 18rpx 20rpx 0; }
.hero-card { background: linear-gradient(135deg, #173b75 0%, #376b74 100%); border-radius: 24rpx; padding: 28rpx; box-shadow: 0 12rpx 28rpx rgba(47, 79, 117, 0.16); }
.hero-card.completed { background: linear-gradient(135deg, #65717d 0%, #7b858e 100%); }
.hero-meta { display: flex; align-items: center; gap: 12rpx; margin-bottom: 18rpx; }
.status-pill, .duration-pill { height: 48rpx; padding: 0 18rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.16); color: #ffffff; font-size: 22rpx; display: flex; align-items: center; }
.node-title { display: block; font-size: 40rpx; color: #ffffff; font-weight: 800; line-height: 1.35; }
.node-desc { display: block; margin-top: 16rpx; font-size: 25rpx; color: rgba(255, 255, 255, 0.86); line-height: 1.75; }
.section-card { margin-top: 18rpx; background: #ffffff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04); }
.brief-row { display: flex; flex-direction: column; gap: 18rpx; }
.brief-item { padding-bottom: 18rpx; border-bottom: 1rpx solid #edf1f6; }
.brief-item:last-child { padding-bottom: 0; border-bottom: 0; }
.brief-label { display: block; margin-bottom: 8rpx; font-size: 24rpx; color: #1f5eff; font-weight: 700; }
.brief-text { display: block; font-size: 25rpx; color: #35485b; line-height: 1.7; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16rpx; margin-bottom: 18rpx; }
.section-title { font-size: 30rpx; color: #263442; font-weight: 800; }
.section-sub { font-size: 22rpx; color: #7d8fa2; }
.step-list { display: flex; flex-direction: column; gap: 18rpx; }
.step-row { display: flex; gap: 16rpx; }
.step-index { width: 46rpx; height: 46rpx; border-radius: 50%; background: #e7efff; color: #1f5eff; font-size: 24rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.step-main { flex: 1; min-width: 0; padding-bottom: 18rpx; border-bottom: 1rpx solid #edf1f6; }
.step-row:last-child .step-main { padding-bottom: 0; border-bottom: 0; }
.step-title { display: block; font-size: 27rpx; color: #25384d; font-weight: 800; }
.step-content { display: block; margin-top: 8rpx; font-size: 24rpx; color: #627487; line-height: 1.7; }
.resource-list { display: flex; flex-direction: column; gap: 18rpx; }
.resource-row { display: flex; align-items: center; gap: 16rpx; padding-bottom: 18rpx; border-bottom: 1rpx solid #edf1f6; }
.resource-row:last-child { padding-bottom: 0; border-bottom: 0; }
.resource-icon { width: 76rpx; height: 76rpx; border-radius: 18rpx; background: #e8f6ef; color: #239263; font-size: 30rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.resource-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.resource-title { font-size: 27rpx; color: #25384d; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.resource-desc { font-size: 23rpx; color: #6f8194; line-height: 1.5; }
.resource-meta { display: flex; gap: 14rpx; color: #8a99a8; font-size: 21rpx; }
.open-text { color: #1f5eff; font-size: 24rpx; font-weight: 800; flex-shrink: 0; }
.empty-resource { padding: 36rpx 0; text-align: center; color: #8292a4; font-size: 25rpx; }
.bottom-space { height: 46rpx; }
</style>
