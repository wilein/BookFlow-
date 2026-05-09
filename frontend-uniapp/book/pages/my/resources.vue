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
      <view class="add-btn" @click="goCreate">{{ texts.add }}</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="summary-card">
        <view>
          <text class="summary-title">{{ texts.summaryTitle }}</text>
          <text class="summary-sub">{{ items.length }}{{ texts.resourceUnit }} · {{ currentTabLabel }}</text>
        </view>
        <view class="summary-badge">{{ texts.grouped }}</view>
      </view>

      <view class="search-card">
        <text class="search-icon">⌕</text>
        <input class="search-input" v-model="keyword" :placeholder="texts.searchPlaceholder" confirm-type="search" />
        <text v-if="keyword" class="clear-btn" @click="keyword = ''">×</text>
      </view>

      <scroll-view class="tabs-scroll" scroll-x show-scrollbar="false">
        <view class="tabs-row">
          <view
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-chip"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <text>{{ tab.label }}</text>
            <text class="tab-count">{{ tab.count }}</text>
          </view>
        </view>
      </scroll-view>

      <view v-if="filteredItems.length" class="list">
        <view v-for="item in filteredItems" :key="item.id" class="card" @click="openResource(item)">
          <view class="icon" :class="item.bindType">{{ previewIcon(item) }}</view>
          <view class="card-main">
            <view class="card-head">
              <text class="title">{{ item.title || item.name }}</text>
              <text class="type-pill">{{ item.typeLabel || texts.resource }}</text>
            </view>
            <text class="binding">{{ item.bindingSummary || buildBindingSummary(item) }}</text>
            <text class="desc">{{ item.description || texts.emptyDesc }}</text>
            <view class="meta-row">
              <text>{{ item.fileFormat || item.previewType || texts.file }}</text>
              <text>{{ item.fileSizeLabel || texts.unknownSize }}</text>
              <text>{{ item.visibilityLabel || texts.public }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-title">{{ emptyTitle }}</text>
        <text class="empty-sub">{{ emptySub }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyResources } from '../../utils/api/resource';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const TEXTS = {
  title: '我的资源',
  summaryTitle: '按绑定类型整理',
  grouped: '资源视图',
  resourceUnit: '个资源',
  searchPlaceholder: '搜索资源名称、绑定对象或说明',
  resource: '资源',
  emptyDesc: '暂无资源描述',
  file: '文件',
  unknownSize: '未知大小',
  public: '公开',
  empty: '暂无上传资源',
  emptySub: '你上传的课件、PDF、视频或图片会按绑定对象自动整理到这里',
  noResult: '没有找到相关资源',
  noResultSub: '换一个关键词或分类试试',
  add: '添加'
};

function normalizeBindType(value) {
  const text = String(value || 'none');
  return ['book', 'pathNode', 'none'].includes(text) ? text : 'none';
}

function bindTypeLabel(value) {
  const type = normalizeBindType(value);
  if (type === 'book') return '关联书籍';
  if (type === 'pathNode') return '关联路径节点';
  return '未绑定';
}

function normalizeItem(item) {
  const bindType = normalizeBindType(item.bindType);
  return {
    ...item,
    bindType,
    bindTypeLabel: item.bindTypeLabel || bindTypeLabel(bindType),
    title: item.title || item.name || '未命名资源',
    bindingSummary: item.bindingSummary || buildBindingSummary(item),
    previewType: item.previewType || inferPreviewType(item)
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

function buildBindingSummary(item) {
  const label = item.bindTypeLabel || bindTypeLabel(item.bindType);
  const id = item.bindId || (item.bindType === 'book' ? item.bookId : '');
  const target = item.bindTargetTitle || item.bookTitle || '';
  const idText = id ? ` #${id}` : '';
  return target ? `${label}${idText} · ${target}` : `${label}${idText}`;
}

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: '',
      activeTab: 'all',
      items: []
    };
  },
  computed: {
    tabs() {
      return [
        { key: 'all', label: '全部', count: this.items.length },
        { key: 'book', label: '关联书籍', count: this.countByBindType('book') },
        { key: 'pathNode', label: '路径节点', count: this.countByBindType('pathNode') },
        { key: 'none', label: '未绑定', count: this.countByBindType('none') }
      ];
    },
    currentTabLabel() {
      return this.tabs.find((item) => item.key === this.activeTab)?.label || '全部';
    },
    filteredItems() {
      const keyword = this.keyword.trim().toLowerCase();
      return this.items.filter((item) => {
        if (this.activeTab !== 'all' && item.bindType !== this.activeTab) return false;
        if (!keyword) return true;
        const target = [
          item.title,
          item.description,
          item.typeLabel,
          item.bindTypeLabel,
          item.bindingSummary,
          item.bindTargetTitle,
          item.fileFormat
        ].join(' ').toLowerCase();
        return target.includes(keyword);
      });
    },
    emptyTitle() {
      return this.items.length ? TEXTS.noResult : TEXTS.empty;
    },
    emptySub() {
      return this.items.length ? TEXTS.noResultSub : TEXTS.emptySub;
    }
  },
  onLoad() {
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
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const data = await getMyResources();
        this.items = Array.isArray(data) ? data.map(normalizeItem) : [];
      } catch (error) {
        console.error('getMyResources failed', error);
      }
    },
    countByBindType(type) {
      return this.items.filter((item) => item.bindType === type).length;
    },
    previewIcon(item) {
      if (item.previewType === 'image') return '图';
      if (item.previewType === 'video') return '视';
      if (item.previewType === 'document') return '文';
      return item.typeLabel ? item.typeLabel.slice(0, 1) : '资';
    },
    buildBindingSummary,
    openResource(item) {
      uni.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(item.id)}` });
    },
    goCreate() {
      uni.navigateTo({ url: '/pages/resources/create' });
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f5f8; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding-left: 20rpx; background: rgba(243, 245, 248, 0.96); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; }
.back-btn, .add-btn { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.add-btn { border-radius: 16rpx; background: #2f5f8f; color: #ffffff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.content { padding: 16rpx 20rpx calc(36rpx + env(safe-area-inset-bottom)); }
.summary-card { min-height: 138rpx; box-sizing: border-box; border-radius: 24rpx; padding: 24rpx; background: #2f5f8f; color: #ffffff; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; }
.summary-title { display: block; font-size: 34rpx; font-weight: 800; line-height: 1.35; }
.summary-sub { display: block; margin-top: 8rpx; font-size: 24rpx; opacity: 0.9; }
.summary-badge { flex-shrink: 0; padding: 10rpx 16rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.16); font-size: 22rpx; }
.search-card { margin-top: 16rpx; height: 84rpx; border-radius: 18rpx; background: #ffffff; padding: 0 18rpx; display: flex; align-items: center; gap: 12rpx; box-sizing: border-box; }
.search-icon { width: 34rpx; color: #8a9aae; font-size: 30rpx; text-align: center; }
.search-input { flex: 1; min-width: 0; height: 72rpx; font-size: 26rpx; color: #26384c; }
.clear-btn { width: 44rpx; height: 44rpx; border-radius: 50%; background: #eef2f7; color: #7b8ea1; font-size: 32rpx; line-height: 40rpx; text-align: center; }
.tabs-scroll { margin-top: 16rpx; white-space: nowrap; }
.tabs-row { display: inline-flex; gap: 12rpx; padding-bottom: 2rpx; }
.tab-chip { min-width: 134rpx; height: 64rpx; border-radius: 999rpx; background: #ffffff; color: #63758a; font-size: 24rpx; display: inline-flex; align-items: center; justify-content: center; gap: 8rpx; padding: 0 18rpx; box-sizing: border-box; }
.tab-chip.active { background: #e8efff; color: #2d55c7; font-weight: 800; }
.tab-count { font-size: 21rpx; color: inherit; opacity: 0.78; }
.list { margin-top: 16rpx; display: flex; flex-direction: column; gap: 16rpx; }
.card { display: flex; gap: 16rpx; background: #ffffff; border-radius: 22rpx; padding: 18rpx; box-shadow: 0 10rpx 24rpx rgba(20, 38, 58, 0.05); }
.icon { width: 88rpx; height: 88rpx; border-radius: 20rpx; background: #e8f6ef; color: #239263; font-size: 30rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon.book { background: #e7efff; color: #2d55c7; }
.icon.pathNode { background: #fff2df; color: #b87910; }
.card-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12rpx; }
.title { flex: 1; min-width: 0; font-size: 30rpx; color: #243548; font-weight: 800; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.type-pill { flex-shrink: 0; max-width: 130rpx; padding: 6rpx 12rpx; border-radius: 999rpx; background: #f2f6fa; color: #607389; font-size: 21rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.binding { font-size: 23rpx; color: #2f5f8f; line-height: 1.5; }
.desc { font-size: 23rpx; color: #708399; line-height: 1.6; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.meta-row { display: flex; flex-wrap: wrap; gap: 8rpx; margin-top: 4rpx; }
.meta-row text { padding: 6rpx 10rpx; border-radius: 999rpx; background: #f4f7fb; color: #7d8fa2; font-size: 21rpx; }
.empty { margin-top: 120rpx; padding: 0 40rpx; text-align: center; }
.empty-title { display: block; color: #31465c; font-size: 30rpx; font-weight: 800; }
.empty-sub { display: block; margin-top: 12rpx; color: #7d8fa2; font-size: 24rpx; line-height: 1.6; }
</style>
