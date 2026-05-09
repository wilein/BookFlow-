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
      <view class="create-btn" @click="goFindBook">添加</view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="summary-card">
        <view>
          <text class="summary-title">{{ texts.summaryTitle }}</text>
          <text class="summary-sub">{{ totalAnnotations }}{{ texts.annotationUnit }} · {{ bookGroups.length }}{{ texts.bookUnit }}</text>
        </view>
        <view class="summary-badge">{{ texts.byBook }}</view>
      </view>

      <view class="search-card">
        <text class="search-icon">⌕</text>
        <input
          class="search-input"
          v-model="searchKeyword"
          confirm-type="search"
          :placeholder="texts.searchPlaceholder"
        />
        <text v-if="searchKeyword" class="clear-btn" @click="searchKeyword = ''">×</text>
      </view>

      <view v-if="filteredBookGroups.length" class="book-list">
        <view
          v-for="book in filteredBookGroups"
          :key="book.id"
          class="book-card"
          @click="openBookAnnotations(book)"
        >
          <image class="book-cover" :src="book.cover" mode="aspectFill" @error="handleCoverError(book)"></image>
          <view class="book-main">
            <view class="book-head">
              <text class="book-title">{{ book.bookTitle }}</text>
              <text class="category">{{ book.category }}</text>
            </view>

            <view class="stats-row">
              <text>{{ book.annotationCount }}{{ texts.annotationUnit }}</text>
              <text>{{ book.pageCount }}{{ texts.pageUnit }}</text>
              <text v-if="book.latestTime">{{ book.latestTime }}</text>
            </view>

            <view class="latest-box">
              <text class="type-tag">{{ book.latestTypeLabel }}</text>
              <text class="latest-text">{{ book.latestContent || texts.noContent }}</text>
            </view>

            <view class="bottom-row">
              <view class="page-tags">
                <text
                  v-for="page in book.visiblePages"
                  :key="page"
                  class="page-tag"
                >
                  {{ texts.pagePrefix }}{{ page }}{{ texts.pageSuffix }}
                </text>
                <text v-if="book.morePageCount > 0" class="page-more">+{{ book.morePageCount }}</text>
              </view>
              <text class="open-text">{{ texts.open }}</text>
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
import { getMyAnnotations } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const DEFAULT_COVER = '/static/logo.png';

function normalizeText(value) {
  return String(value || '').trim();
}

function normalizePageNums(value, fallback) {
  const source = Array.isArray(value) ? value : (fallback ? [fallback] : []);
  return Array.from(new Set(source.map((item) => Number(item)).filter((item) => item > 0))).sort((a, b) => a - b);
}

function toBookGroup(item, index) {
  const pageNums = normalizePageNums(item.pageNums, item.latestPageNum || item.pageNum);
  const visiblePages = pageNums.slice(0, 3);
  return {
    id: item.id || `book-${item.bookId || index}`,
    bookId: item.bookId || '',
    bookTitle: normalizeText(item.bookTitle) || '书籍已下架',
    cover: item.bookCover || item.cover || DEFAULT_COVER,
    category: normalizeText(item.category) || '未分类',
    annotationCount: Number(item.annotationCount || item.count || 0),
    pageCount: Number(item.pageCount || pageNums.length || 0),
    pageNums,
    visiblePages,
    morePageCount: Math.max(0, pageNums.length - visiblePages.length),
    latestContent: normalizeText(item.latestContent || item.content),
    latestTypeLabel: normalizeText(item.latestTypeLabel || item.typeLabel) || '批注',
    latestTime: normalizeText(item.latestTime || item.createTime)
  };
}

function groupFlatAnnotations(items) {
  const grouped = new Map();
  items.forEach((item, index) => {
    const key = item.bookId || `unknown-${index}`;
    if (!grouped.has(key)) {
      grouped.set(key, {
        id: `book-${key}`,
        bookId: item.bookId || '',
        bookTitle: item.bookTitle || '书籍已下架',
        cover: item.bookCover || item.cover || DEFAULT_COVER,
        category: item.category || '未分类',
        annotationCount: 0,
        pageCount: 0,
        pageNums: [],
        latestContent: item.content || '',
        latestTypeLabel: item.typeLabel || '批注',
        latestTime: item.createTime || ''
      });
    }
    const group = grouped.get(key);
    group.annotationCount += 1;
    const page = Number(item.pageNum || item.latestPageNum || 0);
    if (page > 0 && !group.pageNums.includes(page)) {
      group.pageNums.push(page);
      group.pageCount = group.pageNums.length;
    }
  });
  return Array.from(grouped.values()).map(toBookGroup);
}

function normalizeGroups(items) {
  if (!Array.isArray(items) || !items.length) return [];
  const groupedShape = items.some((item) => item.annotationCount !== undefined || item.latestContent !== undefined || item.bookCover);
  return groupedShape ? items.map(toBookGroup) : groupFlatAnnotations(items);
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      searchKeyword: '',
      bookGroups: [],
      texts: {
        title: '我的批注',
        summaryTitle: '按书整理的批注',
        byBook: '书籍视图',
        annotationUnit: '条批注',
        bookUnit: '本书',
        pageUnit: '个页码',
        searchPlaceholder: '搜索书名、分类或批注内容',
        pagePrefix: '第',
        pageSuffix: '页',
        noContent: '暂无批注内容',
        open: '查看',
        empty: '暂无批注',
        emptySub: '在书籍详情里添加批注后，会按书籍自动归类到这里',
        noResult: '没有找到相关书籍',
        noResultSub: '换一个关键词试试'
      }
    };
  },
  computed: {
    totalAnnotations() {
      return this.bookGroups.reduce((sum, item) => sum + Number(item.annotationCount || 0), 0);
    },
    filteredBookGroups() {
      const keyword = this.searchKeyword.trim().toLowerCase();
      if (!keyword) return this.bookGroups;
      return this.bookGroups.filter((book) => {
        const target = [
          book.bookTitle,
          book.category,
          book.latestContent,
          book.pageNums.join(',')
        ].join(' ').toLowerCase();
        return target.includes(keyword);
      });
    },
    emptyTitle() {
      return this.bookGroups.length ? this.texts.noResult : this.texts.empty;
    },
    emptySub() {
      return this.bookGroups.length ? this.texts.noResultSub : this.texts.emptySub;
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
        const data = await getMyAnnotations();
        this.bookGroups = normalizeGroups(data);
      } catch (error) {
        console.error('getMyAnnotations failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    handleCoverError(book) {
      book.cover = DEFAULT_COVER;
    },
    goFindBook() {
      uni.navigateTo({ url: '/pages/my/bookshelf?select=annotation' });
    },
    openBookAnnotations(book) {
      if (!book.bookId) {
        uni.showToast({ title: '书籍不存在，无法查看批注', icon: 'none' });
        return;
      }
      const params = [
        `bookId=${encodeURIComponent(book.bookId)}`,
        `bookTitle=${encodeURIComponent(book.bookTitle)}`,
        'mineOnly=1'
      ].join('&');
      uni.navigateTo({ url: `/pages/annotations/list?${params}` });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding-left: 20rpx; background: rgba(243, 245, 248, 0.96); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; }
.back-btn { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; min-width: 0; text-align: center; font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.create-btn { width: 88rpx; height: 64rpx; border-radius: 16rpx; background: #173b75; color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; flex-shrink: 0; }
.content { padding: 16rpx 20rpx calc(32rpx + env(safe-area-inset-bottom)); }
.summary-card { min-height: 138rpx; box-sizing: border-box; border-radius: 24rpx; padding: 24rpx; background: #0f766e; color: #ffffff; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; }
.summary-title { display: block; font-size: 34rpx; font-weight: 700; line-height: 1.35; }
.summary-sub { display: block; margin-top: 8rpx; font-size: 24rpx; opacity: 0.9; }
.summary-badge { flex-shrink: 0; padding: 10rpx 16rpx; border-radius: 999rpx; background: rgba(255, 255, 255, 0.16); font-size: 22rpx; }
.search-card { margin-top: 16rpx; height: 84rpx; border-radius: 18rpx; background: #ffffff; padding: 0 18rpx; display: flex; align-items: center; gap: 12rpx; box-sizing: border-box; }
.search-icon { width: 34rpx; color: #8a9aae; font-size: 30rpx; text-align: center; }
.search-input { flex: 1; min-width: 0; height: 72rpx; font-size: 26rpx; color: #26384c; }
.clear-btn { width: 44rpx; height: 44rpx; border-radius: 50%; background: #eef2f7; color: #7b8ea1; font-size: 32rpx; line-height: 40rpx; text-align: center; }
.book-list { margin-top: 16rpx; display: flex; flex-direction: column; gap: 16rpx; }
.book-card { background: #ffffff; border-radius: 22rpx; padding: 18rpx; display: flex; gap: 18rpx; box-shadow: 0 10rpx 24rpx rgba(20, 38, 58, 0.05); }
.book-cover { width: 136rpx; height: 184rpx; border-radius: 14rpx; background: #e9eef5; flex-shrink: 0; }
.book-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.book-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12rpx; }
.book-title { flex: 1; min-width: 0; font-size: 30rpx; color: #243548; font-weight: 700; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.category { flex-shrink: 0; max-width: 140rpx; padding: 6rpx 12rpx; border-radius: 999rpx; background: #edf3ff; color: #1f5eff; font-size: 21rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stats-row { margin-top: 10rpx; display: flex; flex-wrap: wrap; gap: 8rpx; }
.stats-row text { padding: 6rpx 10rpx; border-radius: 999rpx; background: #f2f6fa; color: #607389; font-size: 21rpx; }
.latest-box { margin-top: 12rpx; min-height: 58rpx; border-radius: 14rpx; background: #f7f9fc; padding: 12rpx; display: flex; gap: 10rpx; align-items: flex-start; }
.type-tag { flex-shrink: 0; padding: 4rpx 10rpx; border-radius: 999rpx; background: #fff2d8; color: #b87900; font-size: 20rpx; }
.latest-text { flex: 1; min-width: 0; color: #52667a; font-size: 23rpx; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.bottom-row { margin-top: auto; padding-top: 12rpx; display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.page-tags { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8rpx; overflow: hidden; }
.page-tag, .page-more { flex-shrink: 0; padding: 5rpx 10rpx; border-radius: 999rpx; background: #eef6f1; color: #2f7a50; font-size: 20rpx; }
.page-more { background: #eef2f7; color: #697c90; }
.open-text { flex-shrink: 0; color: #0f766e; font-size: 24rpx; font-weight: 700; }
.empty { margin-top: 120rpx; padding: 0 40rpx; text-align: center; }
.empty-title { display: block; color: #31465c; font-size: 30rpx; font-weight: 700; }
.empty-sub { display: block; margin-top: 12rpx; color: #7d8fa2; font-size: 24rpx; line-height: 1.6; }
</style>
