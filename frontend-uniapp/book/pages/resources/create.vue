<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }"
    >
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ texts.title }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="section">
        <text class="section-title">{{ texts.basic }}</text>
        <view class="field-row">
          <text class="label">{{ texts.name }}</text>
          <input class="input" v-model="form.title" maxlength="60" :placeholder="texts.namePlaceholder" />
        </view>
        <view class="field-row">
          <text class="label">{{ texts.description }}</text>
          <textarea class="textarea" v-model="form.description" maxlength="400" :placeholder="texts.descPlaceholder"></textarea>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.resourceType }}</text>
          <picker :range="typeOptions" range-key="label" :value="typeIndex" @change="onTypeChange">
            <view class="picker-value">{{ selectedTypeLabel }}</view>
          </picker>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.visibility }}</text>
          <picker :range="visibilityOptions" range-key="label" :value="visibilityIndex" @change="onVisibilityChange">
            <view class="picker-value">{{ selectedVisibilityLabel }}</view>
          </picker>
        </view>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.file }}</text>
        <view class="file-box" @click="chooseFile">
          <text class="file-title">{{ form.fileUrl ? texts.fileReady : texts.chooseFile }}</text>
          <text class="file-sub">{{ form.fileUrl || texts.fileHint }}</text>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.fileUrl }}</text>
          <input class="input" v-model="form.fileUrl" :placeholder="texts.fileUrlPlaceholder" />
        </view>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.binding }}</text>
        <view class="field-row">
          <text class="label">{{ texts.bindType }}</text>
          <picker :range="bindOptions" range-key="label" :value="bindIndex" @change="onBindTypeChange">
            <view class="picker-value">{{ selectedBindLabel }}</view>
          </picker>
        </view>
        <view v-if="form.bindType !== 'none'" class="field-row">
          <text class="label">{{ texts.bindTarget }}</text>
          <picker
            :range="targetOptions"
            range-key="label"
            :value="targetIndex"
            :disabled="!targetOptions.length"
            @change="onTargetChange"
          >
            <view class="picker-value" :class="{ placeholder: !form.bindId }">{{ selectedTargetLabel }}</view>
          </picker>
          <text class="field-tip">{{ targetTip }}</text>
        </view>
      </view>

      <view class="submit-btn" :class="{ disabled: submitting }" @click="submitResource">
        {{ submitting ? texts.submitting : texts.submit }}
      </view>
      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { createResource, uploadResourceFile } from '../../utils/api/resource';
import { API_BASE_URL } from '../../utils/api/request';
import { getPathDetail } from '../../utils/api/path';
import { getMyBookshelf, getMyPaths } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TYPE_OPTIONS = [
  { label: '\u8bfe\u4ef6', value: 1 },
  { label: '\u4e60\u9898', value: 2 },
  { label: '\u7b14\u8bb0', value: 3 },
  { label: '\u62d3\u5c55\u9605\u8bfb', value: 4 },
  { label: '\u5176\u4ed6', value: 5 }
];

const VISIBILITY_OPTIONS = [
  { label: '\u516c\u5f00', value: 1 },
  { label: '\u4ec5\u4e70\u5bb6\u53ef\u89c1', value: 2 },
  { label: '\u79c1\u5bc6', value: 3 }
];

const BIND_OPTIONS = [
  { label: '\u4e0d\u7ed1\u5b9a', value: 'none' },
  { label: '\u7ed1\u5b9a\u4e66\u7c4d', value: 'book' },
  { label: '\u7ed1\u5b9a\u8def\u5f84\u8282\u70b9', value: 'pathNode' }
];

const TEXTS = {
  title: '\u6dfb\u52a0\u8d44\u6e90',
  basic: '\u57fa\u672c\u4fe1\u606f',
  name: '\u8d44\u6e90\u540d\u79f0',
  namePlaceholder: '\u4f8b\u5982\uff1a\u7b2c\u4e09\u7ae0\u590d\u4e60\u8bfe\u4ef6',
  description: '\u8d44\u6e90\u8bf4\u660e',
  descPlaceholder: '\u8bf4\u660e\u8d44\u6e90\u9002\u5408\u54ea\u672c\u4e66\u6216\u54ea\u4e2a\u8def\u5f84\u8282\u70b9',
  resourceType: '\u8d44\u6e90\u7c7b\u578b',
  visibility: '\u53ef\u89c1\u6027',
  file: '\u8d44\u6e90\u6587\u4ef6',
  chooseFile: '\u9009\u62e9\u6587\u4ef6',
  fileReady: '\u5df2\u9009\u62e9\u6587\u4ef6',
  fileHint: '\u652f\u6301\u6587\u6863\u3001\u56fe\u7247\u3001\u89c6\u9891\uff0c\u4e5f\u53ef\u5728\u4e0b\u65b9\u76f4\u63a5\u586b\u5199\u94fe\u63a5',
  fileUrl: '\u6587\u4ef6\u94fe\u63a5',
  fileUrlPlaceholder: '\u4e0a\u4f20\u540e\u81ea\u52a8\u586b\u5199\uff0c\u4e5f\u53ef\u7c98\u8d34\u5916\u90e8\u94fe\u63a5',
  binding: '\u7ed1\u5b9a\u5bf9\u8c61',
  bindType: '\u7ed1\u5b9a\u7c7b\u578b',
  bindTarget: '\u7ed1\u5b9a\u5230',
  selectBook: '\u9009\u62e9\u4e00\u672c\u6211\u7684\u4e66\u7c4d',
  selectNode: '\u9009\u62e9\u4e00\u4e2a\u8def\u5f84\u8282\u70b9',
  noBook: '\u4f60\u7684\u4e66\u67b6\u6682\u65e0\u53ef\u7ed1\u5b9a\u4e66\u7c4d',
  noNode: '\u4f60\u7684\u8def\u5f84\u6682\u65e0\u53ef\u7ed1\u5b9a\u8282\u70b9',
  fillName: '\u8bf7\u586b\u5199\u8d44\u6e90\u540d\u79f0',
  fillFile: '\u8bf7\u9009\u62e9\u6587\u4ef6\u6216\u586b\u5199\u6587\u4ef6\u94fe\u63a5',
  fillTarget: '\u8bf7\u9009\u62e9\u7ed1\u5b9a\u5bf9\u8c61',
  submit: '\u4fdd\u5b58\u8d44\u6e90',
  submitting: '\u4fdd\u5b58\u4e2d...',
  saved: '\u8d44\u6e90\u5df2\u4fdd\u5b58',
  uploading: '\u4e0a\u4f20\u4e2d...',
  uploadUnsupported: '\u5f53\u524d\u73af\u5883\u4e0d\u652f\u6301\u9009\u62e9\u6587\u4ef6'
};

function normalizeFileUrl(value) {
  const text = String(value || '').trim();
  if (!text) return '';
  const lower = text.toLowerCase();
  if (
    lower.startsWith('http://') ||
    lower.startsWith('https://') ||
    lower.startsWith('wxfile://') ||
    lower.startsWith('cloud://') ||
    lower.startsWith('data:')
  ) {
    return text;
  }
  const baseUrl = String(API_BASE_URL || '').replace(/\/+$/, '');
  if (!baseUrl) return text;
  return text.startsWith('/') ? `${baseUrl}${text}` : `${baseUrl}/${text}`;
}

function extractExtension(name = '') {
  const clean = String(name).split('?')[0].split('#')[0];
  const index = clean.lastIndexOf('.');
  return index >= 0 ? clean.slice(index + 1).toUpperCase() : '';
}

function normalizeId(value) {
  const text = String(value == null ? '' : value).trim();
  return /^\d+$/.test(text) ? text : '';
}

function dedupeBooks(list) {
  const seen = new Set();
  return (Array.isArray(list) ? list : []).filter((book) => {
    const id = normalizeId(book.id || book.bookId);
    if (!id || seen.has(id)) return false;
    seen.add(id);
    return true;
  });
}

export default {
  data() {
    return {
      texts: TEXTS,
      typeOptions: TYPE_OPTIONS,
      visibilityOptions: VISIBILITY_OPTIONS,
      bindOptions: BIND_OPTIONS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      submitting: false,
      books: [],
      pathNodes: [],
      form: {
        title: '',
        description: '',
        type: 1,
        visibility: 1,
        bindType: 'none',
        bindId: '',
        bookId: '',
        fileUrl: '',
        fileFormat: '',
        fileSize: 0
      }
    };
  },
  computed: {
    typeIndex() {
      return Math.max(0, TYPE_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.type)));
    },
    selectedTypeLabel() {
      return TYPE_OPTIONS[this.typeIndex]?.label || TYPE_OPTIONS[0].label;
    },
    visibilityIndex() {
      return Math.max(0, VISIBILITY_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.visibility)));
    },
    selectedVisibilityLabel() {
      return VISIBILITY_OPTIONS[this.visibilityIndex]?.label || VISIBILITY_OPTIONS[0].label;
    },
    bindIndex() {
      return Math.max(0, BIND_OPTIONS.findIndex((item) => item.value === this.form.bindType));
    },
    selectedBindLabel() {
      return BIND_OPTIONS[this.bindIndex]?.label || BIND_OPTIONS[0].label;
    },
    targetOptions() {
      if (this.form.bindType === 'book') {
        return this.books.map((book) => ({
          label: book.title || book.name || `Book #${book.id}`,
          value: normalizeId(book.id || book.bookId),
          bookId: normalizeId(book.id || book.bookId)
        }));
      }
      if (this.form.bindType === 'pathNode') {
        return this.pathNodes;
      }
      return [];
    },
    targetIndex() {
      const index = this.targetOptions.findIndex((item) => String(item.value) === String(this.form.bindId));
      return index >= 0 ? index : 0;
    },
    selectedTargetLabel() {
      const selected = this.targetOptions[this.targetIndex];
      if (this.form.bindId && selected) return selected.label;
      if (this.form.bindType === 'book') return TEXTS.selectBook;
      if (this.form.bindType === 'pathNode') return TEXTS.selectNode;
      return '';
    },
    targetTip() {
      if (this.form.bindType === 'book' && !this.targetOptions.length) return TEXTS.noBook;
      if (this.form.bindType === 'pathNode' && !this.targetOptions.length) return TEXTS.noNode;
      return '';
    }
  },
  onLoad(options = {}) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
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
      this.headerHeight = this.statusBarHeight + 48;
    }
    if (options.bookId) {
      this.form.bindType = 'book';
      this.form.bindId = normalizeId(options.bookId);
      this.form.bookId = normalizeId(options.bookId);
    }
    this.loadBindTargets();
  },
  methods: {
    async loadBindTargets() {
      try {
        const [selling, sold, paths] = await Promise.all([
          getMyBookshelf('selling').catch(() => []),
          getMyBookshelf('sold').catch(() => []),
          getMyPaths().catch(() => [])
        ]);
        this.books = dedupeBooks([...(selling || []), ...(sold || [])]);

        const pathList = Array.isArray(paths) ? paths.slice(0, 50) : [];
        const details = await Promise.all(
          pathList.map((path) => getPathDetail(path.id).catch(() => ({ ...path, nodes: [] })))
        );
        this.pathNodes = details.flatMap((path, pathIndex) => {
          const pathTitle = path.title || path.name || pathList[pathIndex]?.title || TEXTS.bindTarget;
          return (Array.isArray(path.nodes) ? path.nodes : [])
            .filter((node) => normalizeId(node.id))
            .map((node) => ({
              label: `${pathTitle} / ${node.title || `Node #${node.id}`}`,
              value: normalizeId(node.id),
              pathId: normalizeId(path.id || pathList[pathIndex]?.id),
              bookId: normalizeId(path.bookId || pathList[pathIndex]?.bookId)
            }));
        });
      } catch (error) {
        console.error('load bind targets failed', error);
      }
    },
    onTypeChange(event) {
      this.form.type = TYPE_OPTIONS[Number(event.detail.value)]?.value || 5;
    },
    onVisibilityChange(event) {
      this.form.visibility = VISIBILITY_OPTIONS[Number(event.detail.value)]?.value || 1;
    },
    onBindTypeChange(event) {
      this.form.bindType = BIND_OPTIONS[Number(event.detail.value)]?.value || 'none';
      this.form.bindId = '';
      this.form.bookId = '';
    },
    onTargetChange(event) {
      const option = this.targetOptions[Number(event.detail.value)];
      if (!option) return;
      this.form.bindId = option.value;
      this.form.bookId = this.form.bindType === 'book' ? option.bookId : (option.bookId || '');
    },
    chooseFile() {
      const handleFile = async (file) => {
        const path = file.path || file.tempFilePath;
        if (!path) return;
        uni.showLoading({ title: TEXTS.uploading });
        try {
          const data = await uploadResourceFile(path);
          this.form.fileUrl = normalizeFileUrl(data.url || this.form.fileUrl);
          this.form.fileFormat = extractExtension(file.name || data.fileName || this.form.fileUrl);
          this.form.fileSize = Number(file.size || this.form.fileSize || 0);
        } finally {
          uni.hideLoading();
        }
      };

      if (typeof uni.chooseMessageFile === 'function') {
        uni.chooseMessageFile({
          count: 1,
          type: 'all',
          success: (res) => handleFile((res.tempFiles || [])[0] || {})
        });
        return;
      }
      if (typeof uni.chooseFile === 'function') {
        uni.chooseFile({
          count: 1,
          success: (res) => handleFile((res.tempFiles || [])[0] || {})
        });
        return;
      }
      if (typeof uni.chooseImage === 'function') {
        uni.chooseImage({
          count: 1,
          success: (res) => handleFile({ tempFilePath: (res.tempFilePaths || [])[0], size: 0, name: 'image' })
        });
        return;
      }
      uni.showToast({ title: TEXTS.uploadUnsupported, icon: 'none' });
    },
    async submitResource() {
      if (this.submitting) return;
      if (!this.form.title.trim()) {
        uni.showToast({ title: TEXTS.fillName, icon: 'none' });
        return;
      }
      if (!this.form.fileUrl.trim()) {
        uni.showToast({ title: TEXTS.fillFile, icon: 'none' });
        return;
      }
      if (this.form.bindType !== 'none' && !this.form.bindId) {
        uni.showToast({ title: TEXTS.fillTarget, icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        await createResource({
          ...this.form,
          title: this.form.title.trim(),
          description: this.form.description.trim(),
          fileUrl: normalizeFileUrl(this.form.fileUrl),
          fileFormat: this.form.fileFormat || extractExtension(this.form.fileUrl),
          bindId: this.form.bindType === 'none' ? null : this.form.bindId,
          bookId: this.form.bookId || null
        });
        uni.showToast({ title: TEXTS.saved, icon: 'success' });
        setTimeout(() => uni.navigateBack({ fail: () => uni.navigateTo({ url: '/pages/my/resources' }) }), 500);
      } catch (error) {
        console.error('createResource failed', error);
      } finally {
        this.submitting = false;
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.navigateTo({ url: '/pages/my/resources' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding-left: 20rpx; background: rgba(243, 245, 248, 0.96); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.content { padding: 18rpx 20rpx calc(42rpx + env(safe-area-inset-bottom)); }
.section { margin-bottom: 18rpx; background: #ffffff; border-radius: 24rpx; padding: 22rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04); }
.section-title { display: block; font-size: 30rpx; color: #263442; font-weight: 900; }
.field-row { margin-top: 18rpx; }
.label { display: block; margin-bottom: 10rpx; font-size: 24rpx; color: #4b6075; font-weight: 800; }
.input, .picker-value { min-height: 78rpx; border-radius: 16rpx; background: #f5f8fb; padding: 0 18rpx; box-sizing: border-box; font-size: 25rpx; color: #27394d; display: flex; align-items: center; line-height: 1.45; }
.picker-value.placeholder { color: #8a9aae; }
.textarea { width: 100%; min-height: 150rpx; border-radius: 16rpx; background: #f5f8fb; padding: 18rpx; box-sizing: border-box; font-size: 25rpx; color: #27394d; line-height: 1.6; }
.file-box { margin-top: 18rpx; min-height: 150rpx; border-radius: 18rpx; background: #eef4fb; padding: 22rpx; box-sizing: border-box; display: flex; flex-direction: column; justify-content: center; }
.file-title { font-size: 28rpx; color: #0f766e; font-weight: 900; }
.file-sub { margin-top: 10rpx; font-size: 23rpx; color: #6b7f94; line-height: 1.6; word-break: break-all; }
.field-tip { display: block; margin-top: 8rpx; color: #bf6a27; font-size: 22rpx; line-height: 1.5; }
.submit-btn { height: 84rpx; border-radius: 18rpx; background: #0f766e; color: #ffffff; font-size: 30rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; }
.submit-btn.disabled { background: #b7c4d4; }
.bottom-space { height: 20rpx; }
</style>
