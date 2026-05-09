"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_bookDetail = require("../../utils/book-detail.js");
const common_assets = require("../../common/assets.js");
function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}
function normalizeConditionLabel(book) {
  if (book.conditionLabel)
    return book.conditionLabel;
  const condition = toNumber(book.condition, 3);
  if (condition === 1)
    return "全新";
  if (condition === 2)
    return "9成新";
  if (condition === 3)
    return "8成新";
  if (condition === 4)
    return "7成新";
  return "6成新";
}
function buildSellerTag(score) {
  if (score >= 4.8)
    return "高信誉";
  if (score >= 4.4)
    return "信誉良好";
  return "普通信誉";
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: "",
      allBooks: [],
      visibleBooks: [],
      loading: false,
      isLoadingMore: false,
      hasMore: true,
      pageSize: 10,
      currentPage: 1,
      showMoreMenu: false,
      selectedCategory: "全部",
      selectedPriceRange: "all",
      selectedCondition: "all",
      selectedAnnotation: "all",
      selectedSort: "default",
      categoryOptions: ["全部"],
      textMap: {
        searchPlaceholder: "搜索书名、作者、分类",
        search: "搜索",
        category: "分类",
        priceRange: "价格区间",
        condition: "新旧程度",
        annotationFilter: "是否含批注",
        annotationLabel: "批注",
        loading: "加载中...",
        empty: "暂无匹配书籍",
        loadingMore: "加载更多中...",
        pullMore: "上拉加载更多",
        noMore: "没有更多了",
        unknownCategory: "未分类",
        message: "消息",
        history: "浏览历史",
        feedback: "反馈",
        cancel: "取消"
      },
      priceOptions: [
        { label: "全部", value: "all" },
        { label: "0-30", value: "0-30" },
        { label: "30-60", value: "30-60" },
        { label: "60+", value: "60+" }
      ],
      conditionOptions: [
        { label: "全部", value: "all" },
        { label: "全新", value: "new" },
        { label: "9成新", value: "90" },
        { label: "8成新及以下", value: "80-" }
      ],
      annotationOptions: [
        { label: "全部", value: "all" },
        { label: "含批注", value: "has" },
        { label: "无批注", value: "none" }
      ],
      sortOptions: [
        { label: "综合", value: "default" },
        { label: "价格从低到高", value: "priceAsc" },
        { label: "价格从高到低", value: "priceDesc" },
        { label: "最新发布", value: "latest" }
      ]
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 10;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 56;
    }
    this.keyword = decodeURIComponent(options.keyword || "");
    this.fetchCategories();
    this.fetchBooks(true);
  },
  onReachBottom() {
    this.loadMore();
  },
  methods: {
    formatPrice(price) {
      return `¥${utils_bookDetail.toPriceText(price)}`;
    },
    async fetchCategories() {
      try {
        const grouped = await utils_api_book.getBooksByCategory();
        const categories = ["全部", ...Object.keys(grouped || {})];
        this.categoryOptions = Array.from(new Set(categories));
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/search/search.vue:284", "fetchCategories failed", error);
      }
    },
    buildBookItem(book, index) {
      const annotationCount = toNumber(book.annotationCount ?? book.annotations, 0);
      const sellerScore = Math.min(5, Math.max(1, Number(book.sellerScore || 4.5)));
      const itemId = book.id !== void 0 && book.id !== null && book.id !== "" ? String(book.id) : `remote-${index + 1}`;
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
        sellerName: book.sellerName || "校园书友"
      };
    },
    async fetchBooks(reset = false) {
      if (reset) {
        this.currentPage = 1;
        this.hasMore = true;
        this.allBooks = [];
      }
      if (!this.hasMore && !reset)
        return;
      const loadingField = reset ? "loading" : "isLoadingMore";
      this[loadingField] = true;
      try {
        const data = await utils_api_book.searchBooks({
          keyword: this.keyword.trim(),
          category: this.selectedCategory === "全部" ? "" : this.selectedCategory,
          pageNo: this.currentPage,
          pageSize: this.pageSize
        });
        const list = ((data == null ? void 0 : data.list) || []).map((book, index) => this.buildBookItem(book, (this.currentPage - 1) * this.pageSize + index));
        this.allBooks = reset ? list : this.allBooks.concat(list);
        this.hasMore = Boolean(data == null ? void 0 : data.hasMore);
        this.applyClientFilters();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/search/search.vue:326", "searchBooks failed", error);
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
        if (this.selectedPriceRange !== "all") {
          const price = toNumber(book.price, 0);
          if (this.selectedPriceRange === "0-30" && !(price >= 0 && price < 30))
            return false;
          if (this.selectedPriceRange === "30-60" && !(price >= 30 && price < 60))
            return false;
          if (this.selectedPriceRange === "60+" && !(price >= 60))
            return false;
        }
        if (this.selectedCondition !== "all") {
          const label = book.conditionLabel || "";
          if (this.selectedCondition === "new" && label !== "全新")
            return false;
          if (this.selectedCondition === "90" && label !== "9成新")
            return false;
          if (this.selectedCondition === "80-" && (label === "全新" || label === "9成新"))
            return false;
        }
        if (this.selectedAnnotation === "has" && !book.hasAnnotations)
          return false;
        if (this.selectedAnnotation === "none" && book.hasAnnotations)
          return false;
        return true;
      });
    },
    sortBooks(list) {
      const wrapped = list.map((book, idx) => ({ book, idx }));
      wrapped.sort((a, b) => {
        if (this.selectedSort === "priceAsc") {
          const diff = toNumber(a.book.price, 0) - toNumber(b.book.price, 0);
          return diff !== 0 ? diff : a.idx - b.idx;
        }
        if (this.selectedSort === "priceDesc") {
          const diff = toNumber(b.book.price, 0) - toNumber(a.book.price, 0);
          return diff !== 0 ? diff : a.idx - b.idx;
        }
        if (this.selectedSort === "latest") {
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
      if (this.loading || this.isLoadingMore || !this.hasMore)
        return;
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
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
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
      common_vendor.index.setStorageSync("communityInitialTab", "chat");
      common_vendor.index.switchTab({ url: "/pages/community/community" });
    },
    goToHistory() {
      this.closeMoreMenu();
      common_vendor.index.navigateTo({ url: "/pages/placeholder/history" });
    },
    goToFeedback() {
      this.closeMoreMenu();
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?pagePath=${encodeURIComponent("/pages/search/search")}` });
    },
    goToBookDetail(book) {
      const query = utils_bookDetail.buildBookQueryFromListItem(book);
      common_vendor.index.navigateTo({ url: `/pages/books/detail?${query}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: $data.textMap.searchPlaceholder,
    d: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    e: $data.keyword,
    f: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    g: common_assets._imports_1,
    h: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    i: common_vendor.o((...args) => $options.openMoreMenu && $options.openMoreMenu(...args)),
    j: $data.statusBarHeight + "px",
    k: $data.headerRightSafe + "px",
    l: $data.headerHeight + "px",
    m: $data.headerHeight + "px",
    n: common_vendor.t($data.textMap.category),
    o: common_vendor.f($data.categoryOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item),
        b: item,
        c: $data.selectedCategory === item ? 1 : "",
        d: common_vendor.o(($event) => $options.selectCategory(item), item)
      };
    }),
    p: common_vendor.t($data.textMap.priceRange),
    q: common_vendor.f($data.priceOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.selectedPriceRange === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.selectPriceRange(item.value), item.value)
      };
    }),
    r: common_vendor.t($data.textMap.condition),
    s: common_vendor.f($data.conditionOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.selectedCondition === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.selectCondition(item.value), item.value)
      };
    }),
    t: common_vendor.t($data.textMap.annotationFilter),
    v: common_vendor.f($data.annotationOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.selectedAnnotation === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.selectAnnotation(item.value), item.value)
      };
    }),
    w: common_vendor.f($data.sortOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.selectedSort === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.selectSort(item.value), item.value)
      };
    }),
    x: $data.loading
  }, $data.loading ? {
    y: common_vendor.t($data.textMap.loading)
  } : $data.visibleBooks.length === 0 ? {
    A: common_vendor.t($data.textMap.empty)
  } : {
    B: common_vendor.f($data.visibleBooks, (book, k0, i0) => {
      return {
        a: book.cover || "/static/cover_placeholder.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author),
        d: common_vendor.t($options.formatPrice(book.price)),
        e: common_vendor.t(book.annotationCount),
        f: common_vendor.t(book.categoryName || $data.textMap.unknownCategory),
        g: common_vendor.t(book.sellerTag),
        h: book._uniqueKey,
        i: common_vendor.o(($event) => $options.goToBookDetail(book), book._uniqueKey)
      };
    }),
    C: common_vendor.t($data.textMap.annotationLabel)
  }, {
    z: $data.visibleBooks.length === 0,
    D: !$data.loading
  }, !$data.loading ? common_vendor.e({
    E: $data.isLoadingMore
  }, $data.isLoadingMore ? {
    F: common_vendor.t($data.textMap.loadingMore)
  } : $data.hasMore ? {
    H: common_vendor.t($data.textMap.pullMore)
  } : {
    I: common_vendor.t($data.textMap.noMore)
  }, {
    G: $data.hasMore
  }) : {}, {
    J: $data.showMoreMenu
  }, $data.showMoreMenu ? {
    K: common_vendor.o((...args) => $options.closeMoreMenu && $options.closeMoreMenu(...args))
  } : {}, {
    L: common_assets._imports_2,
    M: common_vendor.t($data.textMap.message),
    N: common_vendor.o((...args) => $options.goToMessage && $options.goToMessage(...args)),
    O: common_assets._imports_2,
    P: common_vendor.t($data.textMap.history),
    Q: common_vendor.o((...args) => $options.goToHistory && $options.goToHistory(...args)),
    R: common_assets._imports_2,
    S: common_vendor.t($data.textMap.feedback),
    T: common_vendor.o((...args) => $options.goToFeedback && $options.goToFeedback(...args)),
    U: common_vendor.t($data.textMap.cancel),
    V: common_vendor.o((...args) => $options.closeMoreMenu && $options.closeMoreMenu(...args)),
    W: $data.showMoreMenu ? 1 : ""
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-c10c040c"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/search/search.js.map
