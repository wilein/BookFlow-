<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }"
    >
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ isEditMode ? '编辑学习路径' : '创建学习路径' }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <scroll-view class="content" scroll-y>
      <view class="hero-card">
        <text class="hero-title">{{ isEditMode ? '路径编辑器' : '新建路径' }}</text>
        <text class="hero-sub">先设计学习体验，再提交审核；通过后其他同学才能看到</text>
      </view>

      <view class="audit-card">
        <text class="audit-title">发布审核</text>
        <text class="audit-desc">封面图、标题、描述、节点内容会一起进入审核。审核通过后，封面和完整学习步骤会展示在公开路径里。</text>
      </view>

      <view class="card">
        <text class="section-title">基本信息</text>

        <view class="field-row">
          <text class="label">标题</text>
          <input class="input" v-model="form.title" maxlength="40" placeholder="请输入路径标题" />
        </view>

        <view class="field-row">
          <text class="label">描述</text>
          <textarea class="textarea" v-model="form.description" maxlength="300" placeholder="简要介绍学习目标与适用人群"></textarea>
        </view>

        <view class="field-row">
          <text class="label">难度</text>
          <view class="chips-row">
            <view
              v-for="item in difficultyOptions"
              :key="item"
              class="chip"
              :class="{ active: form.difficulty === item }"
              @click="form.difficulty = item"
            >
              {{ item }}
            </view>
          </view>
        </view>

        <view class="field-row">
          <text class="label">预估总时长</text>
          <input class="input" v-model="form.totalDuration" maxlength="20" placeholder="如：12小时 / 7天" />
        </view>

        <view class="field-row">
          <text class="label">封面图</text>
          <text class="field-tip">建议使用 16:9 图片，保存/发布时会自动上传。发布后封面随路径一起审核。</text>
          <view v-if="!form.cover" class="cover-upload" @click="chooseCover">
            <text class="upload-plus">+</text>
            <text class="upload-text">上传封面图</text>
          </view>
          <view v-else class="cover-wrap">
            <image class="cover-image" :src="form.cover" mode="aspectFill"></image>
            <view class="cover-actions">
              <view class="mini-btn" @click="chooseCover">更换</view>
              <view class="mini-btn danger" @click="form.cover = ''">移除</view>
            </view>
          </view>
          <text v-if="form.coverImageStatusLabel" class="cover-status">封面状态：{{ form.coverImageStatusLabel }}</text>
        </view>
      </view>

      <view class="card">
        <view class="section-top">
          <text class="section-title">节点编辑器</text>
          <view class="add-node-btn" @click="addNode">+ 添加节点</view>
        </view>
        <text class="section-tip">层级用于表示父子关系；排序先用上移/下移，拖动排序接口后续接入。</text>

        <view v-if="form.nodes.length === 0" class="empty-block">
          <text class="empty-text">暂无节点，点击右上角添加</text>
        </view>

        <view class="node-card" v-for="(node, index) in form.nodes" :key="node.id">
          <view class="node-head">
            <view class="node-head-left">
              <text class="drag-hint">⋮⋮</text>
              <text class="node-index">节点 {{ index + 1 }}</text>
            </view>
            <view class="node-head-right">
              <view class="sort-btn" :class="{ disabled: index === 0 }" @click="moveNodeUp(index)">上移</view>
              <view class="sort-btn" :class="{ disabled: index === form.nodes.length - 1 }" @click="moveNodeDown(index)">下移</view>
              <view class="sort-btn danger" @click="removeNode(index)">删除</view>
            </view>
          </view>

          <view class="field-row">
            <text class="label">节点标题</text>
            <input class="input" v-model="node.title" maxlength="40" placeholder="输入节点标题" />
          </view>

          <view class="field-row">
            <text class="label">节点描述</text>
            <textarea class="textarea node-textarea" v-model="node.description" maxlength="300" placeholder="节点学习说明"></textarea>
          </view>

          <view class="field-row">
            <text class="label">要学什么</text>
            <textarea class="textarea node-textarea" v-model="node.learningGoal" maxlength="300" placeholder="明确本节点要掌握的知识点、能力或产出"></textarea>
          </view>

          <view class="field-row">
            <text class="label">怎么学</text>
            <textarea class="textarea node-textarea" v-model="node.learningMethod" maxlength="400" placeholder="建议学习方式，例如先看视频、再读 PDF、最后做练习"></textarea>
          </view>

          <view class="field-row">
            <text class="label">完成标准</text>
            <textarea class="textarea node-textarea" v-model="node.deliverable" maxlength="300" placeholder="写清楚什么情况下可以把这个节点打勾"></textarea>
          </view>

          <view class="inline-fields">
            <view class="inline-field">
              <text class="label">预计时长</text>
              <input class="input" v-model="node.duration" maxlength="20" placeholder="如：2小时" />
            </view>
            <view class="inline-field">
              <text class="label">层级</text>
              <view class="chips-row small">
                <view
                  v-for="level in levelOptions"
                  :key="level.value"
                  class="chip"
                  :class="{ active: node.level === level.value }"
                  @click="node.level = level.value"
                >
                  {{ level.label }}
                </view>
              </view>
            </view>
          </view>

          <view class="field-row">
            <text class="label">关联资源</text>
            <view class="chips-row">
              <view
                v-for="resource in resourceOptions"
                :key="resource.id"
                class="chip resource-chip"
                :class="{ active: node.resourceIds.includes(resource.id) }"
                @click="toggleResource(node, resource.id)"
              >
                {{ resource.name }}
              </view>
            </view>
          </view>

          <view class="field-row">
            <view class="section-top compact">
              <text class="label no-margin">学习步骤</text>
              <view class="add-step-btn" @click="addStep(node)">+ 步骤</view>
            </view>
            <view class="step-editor" v-for="(step, stepIndex) in node.learningSteps" :key="step.id || stepIndex">
              <view class="step-head">
                <text class="step-index">步骤 {{ stepIndex + 1 }}</text>
                <view v-if="node.learningSteps.length > 1" class="step-remove" @click="removeStep(node, stepIndex)">删除</view>
              </view>
              <input class="input" v-model="step.title" maxlength="30" placeholder="步骤标题，如：先读目标" />
              <textarea class="textarea step-textarea" v-model="step.content" maxlength="300" placeholder="具体说明这一步要做什么"></textarea>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="draft-btn" @click="saveDraft">保存草稿</view>
      <view class="publish-btn" @click="publishPath">发布审核</view>
    </view>
  </view>
</template>

<script>
import { getPathDetail, publishPath as publishPathApi, savePathDraft, uploadPathCover } from '../../utils/api/path';
import { getMyResources } from '../../utils/api/resource';

function buildDefaultSteps() {
  return [
    { id: `step-${Date.now()}-1`, title: '明确目标', content: '先阅读节点说明，确认本节点要解决的问题和关键词。' },
    { id: `step-${Date.now()}-2`, title: '学习资料', content: '按顺序学习关联资源，边学边记录不理解的地方。' },
    { id: `step-${Date.now()}-3`, title: '练习复盘', content: '完成练习或输出笔记，确认达到完成标准后再打勾。' }
  ];
}

function buildDefaultNode(index = 0) {
  return {
    id: `node-${Date.now()}-${index}`,
    title: '',
    description: '',
    duration: '',
    level: 1,
    learningGoal: '',
    learningMethod: '',
    deliverable: '',
    resourceIds: [],
    learningSteps: buildDefaultSteps()
  };
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      isEditMode: false,
      difficultyOptions: ['入门', '中级', '进阶'],
      levelOptions: [
        { label: '一级', value: 1 },
        { label: '二级', value: 2 },
        { label: '三级', value: 3 }
      ],
      resourceOptions: [
        { id: 1, name: '章节导图' },
        { id: 2, name: '练习题集' },
        { id: 3, name: '知识卡片' },
        { id: 4, name: '课程视频' }
      ],
      form: {
        id: '',
        title: '',
        description: '',
        difficulty: '入门',
        totalDuration: '',
        cover: '',
        coverImageStatus: 0,
        coverImageStatusLabel: '',
        nodes: [buildDefaultNode(1)]
      }
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
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }

    this.form.id = decodeURIComponent(options.pathId || '');
    this.isEditMode = Boolean(this.form.id);

    if (this.isEditMode) {
      this.loadPathDetail(this.form.id);
    }
    this.fetchMyResources();
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    chooseCover() {
      uni.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: (res) => {
          const file = (res.tempFilePaths || [])[0];
          if (file) {
            this.form.cover = file;
            this.form.coverImageStatus = 1;
            this.form.coverImageStatusLabel = '待上传';
          }
        }
      });
    },
    isLocalFile(url) {
      if (!url) return false;
      return !/^https?:\/\//i.test(url);
    },
    async ensureCoverUploaded() {
      if (!this.form.cover || !this.isLocalFile(this.form.cover)) {
        return;
      }
      uni.showLoading({ title: '上传封面' });
      try {
        const data = await uploadPathCover(this.form.cover);
        this.form.cover = data.url || this.form.cover;
        this.form.coverImageStatus = data.auditStatus || 1;
        this.form.coverImageStatusLabel = data.auditStatusLabel || '待审核';
      } finally {
        uni.hideLoading();
      }
    },
    addNode() {
      this.form.nodes.push(buildDefaultNode(this.form.nodes.length + 1));
    },
    removeNode(index) {
      this.form.nodes.splice(index, 1);
      if (this.form.nodes.length === 0) {
        this.form.nodes.push(buildDefaultNode(1));
      }
    },
    moveNodeUp(index) {
      if (index <= 0) return;
      const list = [...this.form.nodes];
      [list[index - 1], list[index]] = [list[index], list[index - 1]];
      this.form.nodes = list;
    },
    moveNodeDown(index) {
      if (index >= this.form.nodes.length - 1) return;
      const list = [...this.form.nodes];
      [list[index], list[index + 1]] = [list[index + 1], list[index]];
      this.form.nodes = list;
    },
    toggleResource(node, resourceId) {
      const idx = node.resourceIds.indexOf(resourceId);
      if (idx >= 0) {
        node.resourceIds.splice(idx, 1);
      } else {
        node.resourceIds.push(resourceId);
      }
    },
    addStep(node) {
      node.learningSteps.push({
        id: `step-${Date.now()}-${node.learningSteps.length + 1}`,
        title: '',
        content: ''
      });
    },
    removeStep(node, index) {
      node.learningSteps.splice(index, 1);
      if (node.learningSteps.length === 0) {
        node.learningSteps.push({ id: `step-${Date.now()}-1`, title: '', content: '' });
      }
    },
    validateForm() {
      if (!this.form.title.trim()) {
        uni.showToast({ title: '请填写路径标题', icon: 'none' });
        return false;
      }
      if (!this.form.totalDuration.trim()) {
        uni.showToast({ title: '请填写预估总时长', icon: 'none' });
        return false;
      }
      if (this.form.nodes.some((node) => !node.title.trim())) {
        uni.showToast({ title: '请完善节点标题', icon: 'none' });
        return false;
      }
      if (this.form.nodes.some((node) => !node.learningGoal.trim() || !node.learningMethod.trim())) {
        uni.showToast({ title: '请完善节点学习目标和学习方法', icon: 'none' });
        return false;
      }
      return true;
    },
    buildPayload() {
      return {
        id: this.form.id || undefined,
        title: this.form.title,
        description: this.form.description,
        difficulty: this.form.difficulty,
        totalDuration: this.form.totalDuration,
        cover: this.form.cover,
        nodes: this.form.nodes.map((node) => ({
          id: /^\d+$/.test(String(node.id || '')) ? node.id : undefined,
          title: node.title,
          description: node.description,
          duration: node.duration,
          level: node.level,
          learningGoal: node.learningGoal,
          learningMethod: node.learningMethod,
          deliverable: node.deliverable,
          resourceIds: node.resourceIds,
          learningSteps: node.learningSteps
            .map((step) => ({ title: step.title.trim(), content: step.content.trim() }))
            .filter((step) => step.title || step.content)
        }))
      };
    },
    async saveDraft() {
      if (!this.form.title.trim()) {
        uni.showToast({ title: '请先填写标题再保存', icon: 'none' });
        return;
      }
      try {
        await this.ensureCoverUploaded();
        const data = await savePathDraft(this.buildPayload());
        if (data && data.pathId) {
          this.form.id = data.pathId;
          this.isEditMode = true;
        }
        uni.showToast({ title: '草稿已保存', icon: 'none' });
      } catch (error) {
        console.error('saveDraft failed', error);
      }
    },
    publishPath() {
      if (!this.validateForm()) return;
      uni.showModal({
        title: '发布确认',
        content: '发布后将进入审核流程，确认继续？',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await this.ensureCoverUploaded();
            const data = await publishPathApi(this.buildPayload());
            if (data && data.pathId) {
              this.form.id = data.pathId;
              this.isEditMode = true;
            }
            uni.showToast({ title: '已提交审核', icon: 'none' });
            setTimeout(() => {
              uni.redirectTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(this.form.id)}` });
            }, 450);
          } catch (error) {
            console.error('publishPath failed', error);
          }
        }
      });
    },
    async fetchMyResources() {
      try {
        const data = await getMyResources();
        if (Array.isArray(data) && data.length) {
          this.resourceOptions = data.map((item) => ({
            id: item.id,
            name: item.name || item.title || '未命名资源'
          }));
        }
      } catch (error) {
        console.error('fetchMyResources failed', error);
      }
    },
    async loadPathDetail(pathId) {
      if (!pathId) return;
      try {
        const data = await getPathDetail(pathId);
        if (!data || !data.id) return;
        this.form = {
          ...this.form,
          id: data.id,
          title: data.title || '',
          description: data.description || '',
          difficulty: data.difficulty || '入门',
          totalDuration: data.totalDuration || '',
          cover: data.coverImage || '',
          coverImageStatus: Number(data.coverImageStatus || 0),
          coverImageStatusLabel: data.coverImageStatusLabel || '',
          nodes: Array.isArray(data.nodes) && data.nodes.length
            ? data.nodes.map((node, index) => ({
              id: node.id || `node-${index}`,
              title: node.title || '',
              description: node.description || '',
              duration: node.duration || '',
              level: node.level || 1,
              learningGoal: node.learningGoal || '',
              learningMethod: node.learningMethod || '',
              deliverable: node.deliverable || '',
              resourceIds: Array.isArray(node.resourceIds) ? node.resourceIds : [],
              learningSteps: Array.isArray(node.learningSteps) && node.learningSteps.length
                ? node.learningSteps.map((step, stepIndex) => ({
                  id: `step-${node.id || index}-${stepIndex}`,
                  title: step.title || '',
                  content: step.content || ''
                }))
                : buildDefaultSteps()
            }))
            : [buildDefaultNode(1)]
        };
      } catch (error) {
        console.error('loadPathDetail failed', error);
      }
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f5f8;
}

.header {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  z-index: 30;
  box-sizing: border-box;
  padding-left: 20rpx;
  padding-right: 20rpx;
  background: #f3f5f8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: #edf2f8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-placeholder {
  width: 72rpx;
  height: 72rpx;
  flex-shrink: 0;
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 30rpx;
  color: #2d3d52;
  font-weight: 700;
}

.save-btn {
  width: 110rpx;
  height: 62rpx;
  line-height: 62rpx;
  text-align: center;
  border-radius: 14rpx;
  background: #2d55c7;
  color: #ffffff;
  font-size: 24rpx;
  flex-shrink: 0;
}

.content {
  height: 100vh;
  box-sizing: border-box;
  padding: 0 20rpx;
}

.hero-card {
  background: linear-gradient(135deg, #2d55c7 0%, #2349b7 100%);
  border-radius: 24rpx;
  padding: 24rpx;
  color: #ffffff;
}

.hero-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
}

.hero-sub {
  margin-top: 8rpx;
  display: block;
  font-size: 24rpx;
  opacity: 0.92;
}

.audit-card {
  margin-top: 16rpx;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  border-left: 8rpx solid #2d55c7;
}

.audit-title {
  display: block;
  font-size: 28rpx;
  color: #263442;
  font-weight: 700;
}

.audit-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #617389;
  line-height: 1.6;
}

.card {
  margin-top: 16rpx;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
}

.section-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.section-top.compact {
  margin-bottom: 12rpx;
}

.section-title {
  font-size: 30rpx;
  color: #2d3d52;
  font-weight: 700;
}

.add-node-btn {
  height: 54rpx;
  line-height: 54rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #e8efff;
  color: #2d55c7;
  font-size: 22rpx;
}

.section-tip {
  margin-top: 10rpx;
  display: block;
  font-size: 22rpx;
  color: #7b8da3;
}

.field-row {
  margin-top: 16rpx;
}

.label {
  display: block;
  font-size: 24rpx;
  color: #5d7186;
  margin-bottom: 8rpx;
}

.label.no-margin {
  margin-bottom: 0;
}

.field-tip {
  display: block;
  margin-bottom: 10rpx;
  font-size: 22rpx;
  color: #7e8fa2;
  line-height: 1.5;
}

.input {
  height: 72rpx;
  border-radius: 14rpx;
  background: #f3f7fb;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #2b3f53;
}

.textarea {
  width: 100%;
  min-height: 140rpx;
  border-radius: 14rpx;
  background: #f3f7fb;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  font-size: 24rpx;
  color: #2b3f53;
}

.node-textarea {
  min-height: 120rpx;
}

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.chips-row.small .chip {
  min-width: 90rpx;
}

.chip {
  min-width: 110rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: #eef3fa;
  color: #4b5f76;
  font-size: 22rpx;
}

.chip.active {
  background: #2d55c7;
  color: #ffffff;
}

.cover-upload {
  height: 200rpx;
  border-radius: 16rpx;
  border: 2rpx dashed #c9d7e6;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-plus {
  font-size: 46rpx;
  color: #7f98b3;
  line-height: 1;
}

.upload-text {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #7f98b3;
}

.cover-wrap {
  border-radius: 16rpx;
  overflow: hidden;
  background: #f3f7fb;
}

.cover-image {
  width: 100%;
  height: 280rpx;
  background: #eef2f6;
}

.cover-actions {
  display: flex;
  gap: 10rpx;
  padding: 12rpx;
}

.cover-status {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #2d55c7;
}

.mini-btn {
  min-width: 88rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  border-radius: 999rpx;
  padding: 0 16rpx;
  background: #e8efff;
  color: #2d55c7;
  font-size: 22rpx;
}

.mini-btn.danger {
  background: #ffeef0;
  color: #ce4f5f;
}

.empty-block {
  margin-top: 16rpx;
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #91a0b1;
}

.node-card {
  margin-top: 14rpx;
  border-radius: 16rpx;
  background: #f8fafd;
  padding: 16rpx;
}

.node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.node-head-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.drag-hint {
  color: #8da0b5;
  font-size: 24rpx;
}

.node-index {
  color: #2f4560;
  font-size: 24rpx;
  font-weight: 600;
}

.node-head-right {
  display: flex;
  gap: 8rpx;
}

.sort-btn {
  min-width: 72rpx;
  height: 46rpx;
  line-height: 46rpx;
  text-align: center;
  border-radius: 999rpx;
  padding: 0 12rpx;
  background: #e8effa;
  color: #3f5c7b;
  font-size: 20rpx;
}

.sort-btn.disabled {
  opacity: 0.45;
}

.sort-btn.danger {
  background: #ffeef0;
  color: #ce4f5f;
}

.inline-fields {
  margin-top: 16rpx;
  display: flex;
  gap: 14rpx;
}

.inline-field {
  flex: 1;
}

.resource-chip {
  min-width: 0;
}

.add-step-btn {
  height: 50rpx;
  line-height: 50rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #e8efff;
  color: #2d55c7;
  font-size: 22rpx;
  font-weight: 700;
}

.step-editor {
  margin-top: 12rpx;
  padding: 14rpx;
  border-radius: 16rpx;
  background: #ffffff;
  border: 1rpx solid #e7edf4;
}

.step-head {
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.step-index {
  font-size: 23rpx;
  color: #2f4560;
  font-weight: 700;
}

.step-remove {
  font-size: 22rpx;
  color: #ce4f5f;
}

.step-textarea {
  margin-top: 10rpx;
  min-height: 100rpx;
}

.bottom-space {
  height: 170rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid #e7edf4;
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 12rpx;
}

.draft-btn,
.publish-btn {
  flex: 1;
  height: 84rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.draft-btn {
  background: #edf2f8;
  color: #3b526d;
}

.publish-btn {
  background: #2f4f75;
  color: #ffffff;
}
</style>
