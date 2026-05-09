<template>
  <view class="page">
    <view
      class="header"
      :style="{ paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px', height: headerHeight + 'px' }"
    >
      <view class="header-inner">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
        </view>
        <view class="search-wrap">
          <input
            class="search-input"
            type="text"
            v-model="keyword"
            :placeholder="textMap.searchPlaceholder"
            placeholder-class="placeholder"
            @confirm="handleSearch"
          />
          <view class="search-action" @click="handleSearch">
            <image class="search-action-icon" src="/static/search.png" mode="aspectFit"></image>
          </view>
        </view>
        <view class="more-btn" @click="openMoreMenu">...</view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="filter-card">
        <view class="filter-row">
          <text class="filter-title">{{ textMap.category }}</text>
          <scroll-view class="chips-scroll" scroll-x show-scrollbar="false" enable-flex>
            <view class="chips-line">
              <view
                v-for="item in categoryOptions"
                :key="item"
                class="chip"
                :class="{ active: selectedCategory === item }"
                @click="selectCategory(item)"
              >
                {{ item }}
              </view>
            </view>
          </scroll-view>
        </view>

        <view class="filter-row">
          <text class="filter-title">{{ textMap.priceRange }}</text>
          <view class="chips-wrap">
            <view
              v-for="item in priceOptions"
              :key="item.value"
              class="chip"
              :class="{ active: selectedPriceRange === item.value }"
              @click="selectPriceRange(item.value)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>

        <view class="filter-row">
          <text class="filter-title">{{ textMap.condition }}</text>
          <view class="chips-wrap">
            <view
              v-for="item in conditionOptions"
              :key="item.value"
              class="chip"
              :class="{ active: selectedCondition === item.value }"
              @click="selectCondition(item.value)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>

        <view class="filter-row">
          <text class="filter-title">{{ textMap.annotationFilter }}</text>
          <view class="chips-wrap">
            <view
              v-for="item in annotationOptions"
              :key="item.value"
              class="chip"
              :class="{ active: selectedAnnotation === item.value }"
              @click="selectAnnotation(item.value)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
      </view>

      <view class="sort-bar">
        <view
          v-for="item in sortOptions"
          :key="item.value"
          class="sort-item"
          :class="{ active: selectedSort === item.value }"
          @click="selectSort(item.value)"
        >
          {{ item.label }}
        </view>
      </view>

      <view class="list-wrap">
        <view v-if="loading" class="status-block">{{ textMap.loading }}</view>
        <view v-else-if="visibleBooks.length === 0" class="status-block">{{ textMap.empty }}</view>
        <view
          v-else
          class="book-card"
          v-for="book in visibleBooks"
          :key="book._uniqueKey"
          @click="goToBookDetail(book)"
        >
          <image class="cover" :src="book.cover || '/static/cover_placeholder.png'" mode="aspectFill"></image>
          <view class="book-main">
            <text class="title">{{ book.title }}</text>
            <text class="author">{{ book.author }}</text>
            <view class="meta-row">
              <text class="price">{{ formatPrice(book.price) }}</text>
              <text class="annotation">{{ textMap.annotationLabel }} {{ book.annotationCount }}</text>
            </view>
            <view class="meta-row">
              <text class="category">{{ book.categoryName || textMap.unknownCategory }}</text>
              <text class="credit-tag">{{ book.sellerTag }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="pagination-tip" v-if="!loading">
        <text v-if="isLoadingMore">{{ textMap.loadingMore }}</text>
        <text v-else-if="hasMore">{{ textMap.pullMore }}</text>
        <text v-else>{{ textMap.noMore }}</text>
      </view>

      <view class="bottom-space"></view>
    </view>

    <view class="menu-mask" v-if="showMoreMenu" @click="closeMoreMenu"></view>
    <view class="menu-sheet" :class="{ show: showMoreMenu }">
      <view class="menu-icons-row">
        <view class="menu-icon-item" @click="goToMessage">
          <image class="menu-icon" src="/static/logo.png" mode="aspectFit"></image>
          <text class="menu-icon-text">{{ textMap.message }}</text>
        </view>
        <view class="menu-icon-item" @click="goToHistory">
          <image class="menu-icon" src="/static/logo.png" mode="aspectFit"></image>
          <text class="menu-icon-text">{{ textMap.history }}</text>
        </view>
        <view class="menu-icon-item" @click="goToFeedback">
          <image class="menu-icon" src="/static/logo.png" mode="aspectFit"></image>
          <text class="menu-icon-text">{{ textMap.feedback }}</text>
        </view>
      </view>
      <view class="menu-cancel" @click="closeMoreMenu">{{ textMap.cancel }}</view>
    </view>
  </view>
</template>

<script>
import { getBooksByCategory, searchBooks } from '../../utils/api/book';
import { buildBookQueryFromListItem, toPriceText } from '../../utils/book-detail';

function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function normalizeConditionLabel(book) {
  if (book.conditionLabel) return book.conditionLabel;
  const condition = toNumber(book.condition, 3);
  if (condition === 1) return '全新';
  if (condition === 2) return '9成新';
  if (condition === 3) return '8成新';
  if (condition === 4) return '7成新';
  return '6成新';
}

function buildSellerTag(score) {
  if (score >= 4.8) return '高信誉';
  if (score >= 4.4) return '信誉良好';
  return '普通信誉';
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: '',
      allBooks: [],
      visibleBooks: [],
      loading: false,
      isLoadingMore: false,
      hasMore: true,
      pageSize: 10,
      currentPage: 1,
      showMoreMenu: false,
      selectedCategory: '全部',
      selectedPriceRange: 'all',
      selectedCondition: 'all',
      selectedAnnotation: 'all',
      selectedSort: 'default',
      categoryOptions: ['全部'],
      textMap: {
        searchPlaceholder: '搜索书名、作者、分类',
        search: '搜索',
        category: '分类',
        priceRange: '价格区间',
        condition: '新旧程度',
        annotationFilter: '是否含批注',
        annotationLabel: '批注',
        loading: '加载中...',
        empty: '暂无匹配书籍',
        loadingMore: '加载更多中...',
        pullMore: '上拉加载更多',
        noMore: '没有更多了',
        unknownCategory: '未分类',
        message: '消息',
        history: '浏览历史',
        feedback: '反馈',
        cancel: '取消'
      },
      priceOptions: [
        { label: '全部', value: 'all' },
        { label: '0-30', value: '0-30' },
        { label: '30-60', value: '30-60' },
        { label: '60+', value: '60+' }
      ],
      conditionOptions: [
        { label: '全部', value: 'all' },
        { label: '全新', value: 'new' },
        { label: '9成新', value: '90' },
        { label: '8成新及以下', value: '80-' }
      ],
      annotationOptions: [
        { label: '全部', value: 'all' },
        { label: '含批注', value: 'has' },
        { label: '无批注', value: 'none' }
      ],
      sortOptions: [
        { label: '综合', value: 'default' },
        { label: '价格从低到高', value: 'priceAsc' },
        { label: '价格从高到低', value: 'priceDesc' },
        { label: '最新发布', value: 'latest' }
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
      this.headerHeight = capsule.top + capsule.height + 10;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 56;
    }
    this.keyword = decodeURIComponent(options.keyword || '');
    this.fetchCategories();
    this.fetchBooks(true);
  },
  onReachBottom() {
    this.loadMore();
  },
  methods: {
    formatPrice(price) {
      return `¥${toPriceText(price)}`;
    },
    async fetchCategories() {
      try {
        const grouped = await getBooksByCategory();
        const categories = ['全部', ...Object.keys(grouped || {})];
        this.categoryOptions = Array.from(new Set(categories));
      } catch (error) {
        console.error('fetchCategories failed', error);
      }
    },
    buildBookItem(book, index) {
      const annotationCount = toNumber(book.annotationCount ?? book.annotations, 0);
      const sellerScore = Math.min(5, Math.max(1, Number(book.sellerScore || 4.5)));
      const itemId = book.id !== undefined && book.id !== null && book.id !== '' ? String(book.id) : `remote-${index + 1}`;
      return {
        ...book,
        id: itemId,
        _originIndex: index,
        _uniqueKey: `${itemId}-${index}`,
        price: toNumber(book.price, 0),
        annotationCount,
        hasAnnotations: annotationCount > 0,
        conditionLabel: normalizeConditionLabel(book),
        categoryName: book.categoryName || book.category || this.textMap.unknownCategory,
        sellerTag: buildSellerTag(sellerScore),
        sellerName: book.sellerName || '校园书友'
      };
    },
    async fetchBooks(reset = false) {
      if (reset) {
        this.currentPage = 1;
        this.hasMore = true;
        this.allBooks = [];
      }
      if (!this.hasMore && !reset) return;
      const loadingField = reset ? 'loading' : 'isLoadingMore';
      this[loadingField] = true;
      try {
        const data = await searchBooks({
          keyword: this.keyword.trim(),
          category: this.selectedCategory === '全部' ? '' : this.selectedCategory,
          pageNo: this.currentPage,
          pageSize: this.pageSize
        });
        const list = (data?.list || []).map((book, index) => this.buildBookItem(book, (this.currentPage - 1) * this.pageSize + index));
        this.allBooks = reset ? list : this.allBooks.concat(list);
        this.hasMore = Boolean(data?.hasMore);
        this.applyClientFilters();
      } catch (error) {
        console.error('searchBooks failed', error);
        if (reset) {
          this.allBooks = [];
          this.visibleBooks = [];
        }
      } finally {
        this[loadingField] = false;
      }
    },
    filterBooks() {
      return this.allBooks.filter((book) => {
        if (this.selectedPriceRange !== 'all') {
          const price = toNumber(book.price, 0);
          if (this.selectedPriceRange === '0-30' && !(price >= 0 && price < 30)) return false;
          if (this.selectedPriceRange === '30-60' && !(price >= 30 && price < 60)) return false;
          if (this.selectedPriceRange === '60+' && !(price >= 60)) return false;
        }
        if (this.selectedCondition !== 'all') {
          const label = book.conditionLabel || '';
          if (this.selectedCondition === 'new' && label !== '全新') return false;
          if (this.selectedCondition === '90' && label !== '9成新') return false;
          if (this.selectedCondition === '80-' && (label === '全新' || label === '9成新')) return false;
        }
        if (this.selectedAnnotation === 'has' && !book.hasAnnotations) return false;
        if (this.selectedAnnotation === 'none' && book.hasAnnotations) return false;
        return true;
      });
    },
    sortBooks(list) {
      const wrapped = list.map((book, idx) => ({ book, idx }));
      wrapped.sort((a, b) => {
        if (this.selectedSort === 'priceAsc') {
          const diff = toNumber(a.book.price, 0) - toNumber(b.book.price, 0);
          return diff !== 0 ? diff : a.idx - b.idx;
        }
        if (this.selectedSort === 'priceDesc') {
          const diff = toNumber(b.book.price, 0) - toNumber(a.book.price, 0);
          return diff !== 0 ? diff : a.idx - b.idx;
        }
        if (this.selectedSort === 'latest') {
          return toNumber(b.book.id, 0) - toNumber(a.book.id, 0);
        }
        return a.idx - b.idx;
      });
      return wrapped.map((item) => item.book);
    },
    applyClientFilters() {
      this.visibleBooks = this.sortBooks(this.filterBooks());
    },
    loadMore() {
      if (this.loading || this.isLoadingMore || !this.hasMore) return;
      this.currentPage += 1;
      this.fetchBooks();
    },
    handleSearch() {
      this.fetchBooks(true);
    },
    selectCategory(category) {
      this.selectedCategory = category;
      this.fetchBooks(true);
    },
    selectPriceRange(value) {
      this.selectedPriceRange = value;
      this.applyClientFilters();
    },
    selectCondition(value) {
      this.selectedCondition = value;
      this.applyClientFilters();
    },
    selectAnnotation(value) {
      this.selectedAnnotation = value;
      this.applyClientFilters();
    },
    selectSort(value) {
      this.selectedSort = value;
      this.applyClientFilters();
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    openMoreMenu() {
      this.showMoreMenu = true;
    },
    closeMoreMenu() {
      this.showMoreMenu = false;
    },
    goToMessage() {
      this.closeMoreMenu();
      uni.setStorageSync('communityInitialTab', 'chat');
      uni.switchTab({ url: '/pages/community/community' });
    },
    goToHistory() {
      this.closeMoreMenu();
      uni.navigateTo({ url: '/pages/placeholder/history' });
    },
    goToFeedback() {
      this.closeMoreMenu();
      uni.navigateTo({ url: `/pages/placeholder/feedback?pagePath=${encodeURIComponent('/pages/search/search')}` });
    },
    goToBookDetail(book) {
      const query = buildBookQueryFromListItem(book);
      uni.navigateTo({ url: `/pages/books/detail?${query}` });
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8f9fc;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  border-bottom: 1rpx solid #ecf0f3;
  padding-left: 20rpx;
}

.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding-bottom: 12rpx;
  box-sizing: border-box;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-icon {
  width: 30rpx;
  height: 30rpx;
}

.search-wrap {
  flex: 1;
  height: 72rpx;
  border-radius: 40rpx;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  padding-left: 20rpx;
  padding-right: 84rpx;
  overflow: hidden;
  position: relative;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #2c3e50;
  height: 72rpx;
}

.placeholder {
  color: #95a5a6;
}

.search-action {
  position: absolute;
  right: 8rpx;
  top: 8rpx;
  width: 56rpx;
  height: 56rpx;
  border-radius: 28rpx;
  background: #2f4f75;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-action-icon {
  width: 28rpx;
  height: 28rpx;
}

.more-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 30rpx;
  background: #eef3f8;
  color: #2f4f75;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1;
}

.content {
  padding: 16rpx 20rpx 0;
}

.filter-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx 16rpx 8rpx;
  box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.02);
}

.filter-row {
  margin-bottom: 16rpx;
}

.filter-title {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #71859a;
}

.chips-scroll {
  white-space: nowrap;
}

.chips-line {
  display: inline-flex;
  gap: 12rpx;
}

.chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.chip {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #f3f7fb;
  color: #5d7288;
  font-size: 24rpx;
}

.chip.active {
  background: #2f4f75;
  color: #ffffff;
}

.sort-bar {
  margin-top: 16rpx;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 12rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.02);
}

.sort-item {
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: #f4f7fa;
  color: #63788f;
  font-size: 24rpx;
}

.sort-item.active {
  background: #e8efff;
  color: #2d55c7;
  font-weight: 700;
}

.list-wrap {
  margin-top: 16rpx;
}

.book-card {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
  margin-bottom: 16rpx;
  border-radius: 22rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.02);
}

.cover {
  width: 164rpx;
  height: 212rpx;
  border-radius: 16rpx;
  background: #e9edf2;
  flex-shrink: 0;
}

.book-main {
  flex: 1;
  min-width: 0;
}

.title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #233547;
  line-height: 1.45;
}

.author {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #7c8ea1;
}

.meta-row {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.price {
  font-size: 32rpx;
  font-weight: 700;
  color: #e67e22;
}

.annotation,
.category,
.credit-tag {
  font-size: 22rpx;
}

.annotation {
  color: #4a78b4;
}

.category {
  color: #7a8ea3;
}

.credit-tag {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eff4ff;
  color: #2d55c7;
}

.status-block,
.pagination-tip {
  text-align: center;
  color: #8e9cad;
  font-size: 24rpx;
  padding: 24rpx 0;
}

.bottom-space {
  height: calc(96rpx + env(safe-area-inset-bottom));
}

.menu-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.22);
  z-index: 60;
}

.menu-sheet {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-radius: 28rpx;
  padding: 26rpx 24rpx 20rpx;
  transform: translateY(120%);
  transition: transform 0.22s ease;
  z-index: 61;
  box-shadow: 0 18rpx 48rpx rgba(0, 0, 0, 0.14);
}

.menu-sheet.show {
  transform: translateY(0);
}

.menu-icons-row {
  display: flex;
  justify-content: space-around;
  gap: 20rpx;
}

.menu-icon-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.menu-icon {
  width: 68rpx;
  height: 68rpx;
  border-radius: 34rpx;
  background: #f3f7fb;
}

.menu-icon-text {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #41556d;
}

.menu-cancel {
  margin-top: 22rpx;
  height: 84rpx;
  border-radius: 18rpx;
  background: #f3f7fb;
  color: #42566d;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
