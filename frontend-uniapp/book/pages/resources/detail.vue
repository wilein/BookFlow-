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
      <view class="edit-toggle" v-if="resource.canEdit" @click="toggleEdit">{{ editMode ? texts.cancel : texts.edit }}</view>
      <view v-else class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="hero-card">
        <view class="hero-top">
          <view class="resource-icon" :class="resource.bindType">{{ previewIcon }}</view>
          <view class="hero-main">
            <text class="resource-title">{{ resource.title || texts.unnamed }}</text>
            <text class="resource-meta">{{ resource.typeLabel || texts.resource }} · {{ resource.fileFormat || previewLabel }}</text>
          </view>
        </view>
        <view class="binding-pill">{{ resource.bindingSummary || texts.unbound }}</view>
      </view>

      <view class="preview-card">
        <view class="section-head">
          <text class="section-title">{{ texts.preview }}</text>
          <text class="section-sub">{{ resource.fileSizeLabel || texts.unknownSize }}</text>
        </view>

        <image
          v-if="resource.previewType === 'image' && resource.fileUrl"
          class="image-preview"
          :src="resource.fileUrl"
          mode="aspectFill"
          @click="previewImage"
        ></image>

        <video
          v-else-if="resource.previewType === 'video' && resource.fileUrl"
          id="resourceVideo"
          class="video-preview"
          :src="resource.fileUrl"
          controls
        ></video>

        <view v-else class="file-preview">
          <view class="file-icon">{{ previewIcon }}</view>
          <text class="file-title">{{ resource.fileFormat || previewLabel }}</text>
          <text class="file-desc">{{ previewDescription }}</text>
        </view>

        <view class="action-row">
          <view class="primary-btn" @click="openResource">{{ primaryActionText }}</view>
          <view class="secondary-btn" @click="copyUrl">{{ texts.copy }}</view>
        </view>
      </view>

      <view class="info-card">
        <text class="section-title">{{ texts.info }}</text>
        <view class="info-row">
          <text class="info-label">{{ texts.description }}</text>
          <text class="info-text">{{ resource.description || texts.emptyDesc }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">{{ texts.visibility }}</text>
          <text class="info-text">{{ resource.visibilityLabel || texts.public }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">{{ texts.bindType }}</text>
          <text class="info-text">{{ resource.bindTypeLabel || texts.unbound }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">{{ texts.bindId }}</text>
          <text class="info-text">{{ resource.bindId || resource.bookId || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">{{ texts.bindTarget }}</text>
          <text class="info-text">{{ resource.bindTargetTitle || '-' }}</text>
        </view>
      </view>

      <view v-if="editMode" class="edit-card">
        <text class="section-title">{{ texts.editResource }}</text>

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

        <view v-if="form.bindType !== 'none' && !targetOptions.length" class="field-grid">
          <view class="field-row half">
            <text class="label">{{ texts.bindId }}</text>
            <input class="input" v-model="form.bindId" type="number" :placeholder="texts.bindIdPlaceholder" />
          </view>
          <view class="field-row half">
            <text class="label">{{ texts.bookId }}</text>
            <input class="input" v-model="form.bookId" type="number" :placeholder="texts.bookIdPlaceholder" />
          </view>
        </view>

        <view class="field-row">
          <text class="label">{{ texts.fileUrl }}</text>
          <input class="input" v-model="form.fileUrl" :placeholder="texts.fileUrlPlaceholder" />
        </view>

        <view class="edit-actions">
          <view class="secondary-btn grow" @click="chooseFile">{{ texts.replaceFile }}</view>
          <view class="primary-btn grow" @click="saveResource">{{ texts.save }}</view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { getResourceDetail, updateResource, uploadResourceFile } from '../../utils/api/resource';
import { API_BASE_URL } from '../../utils/api/request';
import { getPathDetail } from '../../utils/api/path';
import { getMyBookshelf, getMyPaths } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TYPE_OPTIONS = [
  { label: '课件', value: 1 },
  { label: '习题', value: 2 },
  { label: '笔记', value: 3 },
  { label: '拓展阅读', value: 4 },
  { label: '其他', value: 5 }
];

const VISIBILITY_OPTIONS = [
  { label: '公开', value: 1 },
  { label: '仅买家可见', value: 2 },
  { label: '私密', value: 3 }
];

const BIND_OPTIONS = [
  { label: '未绑定', value: 'none' },
  { label: '关联书籍', value: 'book' },
  { label: '关联路径节点', value: 'pathNode' }
];

const TEXTS = {
  title: '资源详情',
  edit: '编辑',
  cancel: '取消',
  unnamed: '未命名资源',
  resource: '资源',
  unbound: '未绑定',
  preview: '内容预览',
  unknownSize: '未知大小',
  open: '打开',
  openDocument: '打开文档',
  openVideo: '播放视频',
  openImage: '查看图片',
  copy: '复制链接',
  info: '资源信息',
  description: '说明',
  visibility: '可见性',
  bindType: '绑定类型',
  bindId: '绑定ID',
  bindTarget: '绑定对象',
  selectBook: '选择一本我的书籍',
  selectNode: '选择一个路径节点',
  noBook: '你的书架暂无可绑定书籍',
  noNode: '你的路径暂无可绑定节点',
  public: '公开',
  emptyDesc: '暂无资源说明',
  editResource: '编辑资源',
  name: '名称',
  namePlaceholder: '请输入资源名称',
  descPlaceholder: '说明这个资源适合怎么使用',
  resourceType: '资源类型',
  bookId: '书籍ID',
  bindIdPlaceholder: '书籍ID或节点ID',
  bookIdPlaceholder: '资源所属书籍ID',
  fileUrl: '文件链接',
  fileUrlPlaceholder: '上传后自动填写，也可手动粘贴',
  replaceFile: '替换文件',
  save: '保存修改',
  filePending: '资源文件待补充',
  copied: '资源链接已复制',
  saved: '已保存',
  uploadUnsupported: '当前环境不支持选择文件',
  uploading: '上传中',
  opening: '打开中',
  openFailed: '打开失败，链接已复制',
  imageDesc: '图片资源可以直接预览，点击图片可放大查看。',
  videoDesc: '视频资源可以直接播放，也可以复制链接到浏览器打开。',
  documentDesc: 'PDF、Word、PPT、Excel 等文档会下载后打开。',
  linkDesc: '该资源是外部链接或普通文件，可复制链接后查看。'
};

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

function normalizeResource(item = {}) {
  const fileUrl = normalizeFileUrl(item.fileUrl || item.rawFileUrl || '');
  return {
    id: item.id || '',
    title: item.title || item.name || '',
    description: item.description || '',
    type: Number(item.type || 5),
    typeLabel: item.typeLabel || '',
    fileUrl,
    fileFormat: item.fileFormat || extractExtension(fileUrl),
    fileSize: Number(item.fileSize || 0),
    fileSizeLabel: item.fileSizeLabel || '',
    previewType: item.previewType || inferPreviewType(item),
    visibility: Number(item.visibility || 1),
    visibilityLabel: item.visibilityLabel || '',
    bindType: item.bindType || 'none',
    bindTypeLabel: item.bindTypeLabel || '',
    bindId: item.bindId || '',
    bookId: item.bookId || '',
    bindTargetTitle: item.bindTargetTitle || '',
    bindingSummary: item.bindingSummary || '',
    canEdit: Boolean(item.canEdit)
  };
}

function inferPreviewType(item) {
  const text = `${item.fileFormat || ''} ${item.fileUrl || ''}`.toLowerCase();
  if (/(jpg|jpeg|png|gif|webp|bmp)/.test(text)) return 'image';
  if (/(mp4|mov|m4v|webm|avi)/.test(text)) return 'video';
  if (/(pdf|doc|docx|ppt|pptx|xls|xlsx)/.test(text)) return 'document';
  if (/^https?:\/\//.test(item.fileUrl || '')) return 'link';
  return 'file';
}

function extractExtension(name = '') {
  const clean = String(name).split('?')[0].split('#')[0];
  const index = clean.lastIndexOf('.');
  return index >= 0 ? clean.slice(index + 1).toUpperCase() : '';
}

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      resourceId: '',
      resource: normalizeResource(),
      editMode: false,
      form: {},
      books: [],
      pathNodes: [],
      typeOptions: TYPE_OPTIONS,
      visibilityOptions: VISIBILITY_OPTIONS,
      bindOptions: BIND_OPTIONS
    };
  },
  computed: {
    previewIcon() {
      if (this.resource.previewType === 'image') return '图';
      if (this.resource.previewType === 'video') return '视';
      if (this.resource.previewType === 'document') return '文';
      return '链';
    },
    previewLabel() {
      if (this.resource.previewType === 'image') return '图片';
      if (this.resource.previewType === 'video') return '视频';
      if (this.resource.previewType === 'document') return '文档';
      if (this.resource.previewType === 'link') return '链接';
      return '文件';
    },
    previewDescription() {
      if (this.resource.previewType === 'image') return TEXTS.imageDesc;
      if (this.resource.previewType === 'video') return TEXTS.videoDesc;
      if (this.resource.previewType === 'document') return TEXTS.documentDesc;
      return TEXTS.linkDesc;
    },
    primaryActionText() {
      if (this.resource.previewType === 'image') return TEXTS.openImage;
      if (this.resource.previewType === 'video') return TEXTS.openVideo;
      if (this.resource.previewType === 'document') return TEXTS.openDocument;
      return TEXTS.open;
    },
    typeIndex() {
      return Math.max(0, TYPE_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.type)));
    },
    selectedTypeLabel() {
      return TYPE_OPTIONS[this.typeIndex]?.label || '其他';
    },
    visibilityIndex() {
      return Math.max(0, VISIBILITY_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.visibility)));
    },
    selectedVisibilityLabel() {
      return VISIBILITY_OPTIONS[this.visibilityIndex]?.label || '公开';
    },
    bindIndex() {
      return Math.max(0, BIND_OPTIONS.findIndex((item) => item.value === this.form.bindType));
    },
    selectedBindLabel() {
      return BIND_OPTIONS[this.bindIndex]?.label || '未绑定';
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
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.resourceId = normalizeId(options.id);
    this.loadBindTargets();
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      if (!this.resourceId) return;
      try {
        const data = await getResourceDetail(this.resourceId);
        this.resource = normalizeResource(data);
        this.resetForm();
      } catch (error) {
        console.error('getResourceDetail failed', error);
      }
    },
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
    resetForm() {
      this.form = {
        id: this.resource.id,
        title: this.resource.title,
        description: this.resource.description,
        type: this.resource.type,
        visibility: this.resource.visibility,
        bindType: this.resource.bindType || 'none',
        bindId: this.resource.bindId || '',
        bookId: this.resource.bookId || '',
        fileUrl: this.resource.fileUrl,
        fileFormat: this.resource.fileFormat,
        fileSize: this.resource.fileSize
      };
    },
    toggleEdit() {
      this.editMode = !this.editMode;
      if (this.editMode) this.resetForm();
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
      uni.showToast({ title: TEXTS.uploadUnsupported, icon: 'none' });
    },
    async saveResource() {
      if (!this.form.title || !this.form.title.trim()) {
        uni.showToast({ title: '请填写资源名称', icon: 'none' });
        return;
      }
      if (this.form.bindType !== 'none' && !this.form.bindId) {
        uni.showToast({ title: '请选择绑定对象', icon: 'none' });
        return;
      }
      try {
        const bindType = this.form.bindType || 'none';
        await updateResource({
          ...this.form,
          bindType,
          fileUrl: normalizeFileUrl(this.form.fileUrl),
          fileFormat: this.form.fileFormat || extractExtension(this.form.fileUrl),
          bindId: this.form.bindId || (bindType === 'book' ? this.form.bookId : null) || null,
          bookId: this.form.bookId || null
        });
        uni.showToast({ title: TEXTS.saved, icon: 'none' });
        this.editMode = false;
        await this.fetchDetail();
      } catch (error) {
        console.error('updateResource failed', error);
      }
    },
    previewImage() {
      if (!this.resource.fileUrl) return;
      uni.previewImage({ urls: [this.resource.fileUrl], current: this.resource.fileUrl });
    },
    openResource() {
      const url = this.resource.fileUrl;
      if (!url) {
        uni.showToast({ title: TEXTS.filePending, icon: 'none' });
        return;
      }
      if (this.resource.previewType === 'image') {
        this.previewImage();
        return;
      }
      if (this.resource.previewType === 'video') {
        const videoContext = uni.createVideoContext('resourceVideo', this);
        if (videoContext && typeof videoContext.play === 'function') {
          videoContext.play();
        }
        return;
      }
      // #ifdef H5
      window.open(url, '_blank');
      // #endif
      // #ifndef H5
      if (this.resource.previewType !== 'document') {
        this.copyUrl();
        return;
      }
      uni.showLoading({ title: TEXTS.opening });
      uni.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode !== 200 || !res.tempFilePath) {
            this.copyUrl(TEXTS.openFailed);
            return;
          }
          uni.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            fail: () => this.copyUrl(TEXTS.openFailed)
          });
        },
        fail: () => this.copyUrl(TEXTS.openFailed),
        complete: () => uni.hideLoading()
      });
      // #endif
    },
    copyUrl(title = TEXTS.copied) {
      if (!this.resource.fileUrl) {
        uni.showToast({ title: TEXTS.filePending, icon: 'none' });
        return;
      }
      uni.setClipboardData({
        data: this.resource.fileUrl,
        success: () => uni.showToast({ title, icon: 'none' })
      });
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
.back-btn, .header-placeholder, .edit-toggle { width: 88rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { width: 72rpx; border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.edit-toggle { border-radius: 16rpx; background: #0f766e; color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.content { padding: 18rpx 20rpx calc(42rpx + env(safe-area-inset-bottom)); }
.hero-card { border-radius: 26rpx; padding: 26rpx; background: #0f766e; color: #ffffff; box-shadow: 0 12rpx 28rpx rgba(47, 95, 143, 0.16); }
.hero-top { display: flex; align-items: center; gap: 18rpx; }
.resource-icon { width: 92rpx; height: 92rpx; border-radius: 22rpx; background: rgba(255, 255, 255, 0.16); color: #ffffff; font-size: 34rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.hero-main { flex: 1; min-width: 0; }
.resource-title { display: block; font-size: 36rpx; font-weight: 900; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-meta { display: block; margin-top: 8rpx; font-size: 24rpx; opacity: 0.88; }
.binding-pill { margin-top: 18rpx; display: inline-flex; max-width: 100%; box-sizing: border-box; padding: 9rpx 16rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.15); font-size: 23rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-card, .info-card, .edit-card { margin-top: 18rpx; background: #ffffff; border-radius: 24rpx; padding: 22rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04); }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-bottom: 18rpx; }
.section-title { font-size: 30rpx; color: #263442; font-weight: 900; }
.section-sub { font-size: 22rpx; color: #7d8fa2; }
.image-preview { width: 100%; height: 420rpx; border-radius: 18rpx; background: #eef2f8; }
.video-preview { width: 100%; height: 420rpx; border-radius: 18rpx; overflow: hidden; background: #111827; }
.file-preview { min-height: 260rpx; border-radius: 18rpx; background: #f5f8fb; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30rpx; box-sizing: border-box; text-align: center; }
.file-icon { width: 104rpx; height: 104rpx; border-radius: 26rpx; background: #e7efff; color: #1f5eff; font-size: 42rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; }
.file-title { margin-top: 18rpx; font-size: 30rpx; color: #263442; font-weight: 900; }
.file-desc { margin-top: 10rpx; font-size: 24rpx; color: #6e7f92; line-height: 1.6; }
.action-row, .edit-actions { margin-top: 18rpx; display: flex; gap: 14rpx; }
.primary-btn, .secondary-btn { height: 76rpx; border-radius: 18rpx; display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: 800; }
.primary-btn { flex: 1; background: #0f766e; color: #ffffff; }
.secondary-btn { flex: 1; background: #eef2f7; color: #3f5268; }
.grow { flex: 1; }
.info-row { padding: 18rpx 0; border-bottom: 1rpx solid #edf1f6; display: flex; align-items: flex-start; gap: 18rpx; }
.info-row:last-child { border-bottom: 0; padding-bottom: 0; }
.info-label { width: 140rpx; flex-shrink: 0; color: #7d8fa2; font-size: 24rpx; }
.info-text { flex: 1; min-width: 0; color: #2f4053; font-size: 25rpx; line-height: 1.6; word-break: break-all; }
.field-row { margin-top: 18rpx; }
.field-row:first-of-type { margin-top: 16rpx; }
.field-grid { display: flex; gap: 14rpx; }
.field-row.half { flex: 1; min-width: 0; }
.label { display: block; margin-bottom: 10rpx; font-size: 24rpx; color: #4b6075; font-weight: 800; }
.input, .picker-value { height: 78rpx; border-radius: 16rpx; background: #f5f8fb; padding: 0 18rpx; box-sizing: border-box; font-size: 25rpx; color: #27394d; display: flex; align-items: center; }
.picker-value.placeholder { color: #8a9aae; }
.textarea { width: 100%; min-height: 150rpx; border-radius: 16rpx; background: #f5f8fb; padding: 18rpx; box-sizing: border-box; font-size: 25rpx; color: #27394d; line-height: 1.6; }
.field-tip { display: block; margin-top: 8rpx; color: #bf6a27; font-size: 22rpx; line-height: 1.5; }
.bottom-space { height: 20rpx; }
</style>
