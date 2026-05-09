"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_bookDetail = require("../../utils/book-detail.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      keyword: "",
      loading: false,
      categories: [],
      selectedCategoryId: "",
      categoryBooksMap: {},
      books: [],
      searchHistory: ["Java 编程思想", "算法导论", "设计模式", "数据结构"]
    };
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const screenWidth = common_vendor.index.getSystemInfoSync().windowWidth;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
      this.fixedHeaderHeight = capsule.top + capsule.height + 58;
    } else {
      this.capsulePaddingRight = 100;
      this.fixedHeaderHeight = this.statusBarHeight + 92;
    }
    this.fetchCategoryData();
  },
  methods: {
    async fetchCategoryData() {
      this.loading = true;
      try {
        const data = await utils_api_book.getBooksByCategory();
        const categoryNames = Object.keys(data || {});
        this.categories = categoryNames.map((name) => ({ id: name, name }));
        this.categoryBooksMap = data || {};
        if (this.categories.length > 0) {
          this.selectedCategoryId = this.categories[0].id;
          this.books = this.categoryBooksMap[this.selectedCategoryId] || [];
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/category/category.vue:113", "fetchCategoryData failed", error);
      } finally {
        this.loading = false;
      }
    },
    selectCategory(categoryId) {
      this.selectedCategoryId = categoryId;
      this.books = this.categoryBooksMap[categoryId] || [];
    },
    handleSearch() {
      if (!this.keyword.trim()) {
        common_vendor.index.showToast({ title: "请输入关键词", icon: "none" });
        return;
      }
      if (!this.searchHistory.includes(this.keyword.trim())) {
        this.searchHistory.unshift(this.keyword.trim());
        this.searchHistory = this.searchHistory.slice(0, 10);
      }
      common_vendor.index.navigateTo({
        url: `/pages/search/search?keyword=${encodeURIComponent(this.keyword.trim())}`
      });
    },
    useKeyword(keyword) {
      this.keyword = keyword;
      this.handleSearch();
    },
    clearHistory() {
      this.searchHistory = [];
    },
    goToBookDetail(book) {
      const query = utils_bookDetail.buildBookQueryFromListItem(book);
      common_vendor.index.navigateTo({ url: `/pages/books/detail?${query}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_2,
    b: $data.statusBarHeight + "px",
    c: $data.capsulePaddingRight + "px",
    d: $data.fixedHeaderHeight + "px",
    e: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    f: $data.keyword,
    g: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    h: common_assets._imports_1,
    i: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    j: common_vendor.o((...args) => $options.clearHistory && $options.clearHistory(...args)),
    k: common_vendor.f($data.searchHistory, (item, k0, i0) => {
      return {
        a: common_vendor.t(item),
        b: item,
        c: common_vendor.o(($event) => $options.useKeyword(item), item)
      };
    }),
    l: common_vendor.f($data.categories, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.name),
        b: item.id,
        c: $data.selectedCategoryId === item.id ? 1 : "",
        d: common_vendor.o(($event) => $options.selectCategory(item.id), item.id)
      };
    }),
    m: $data.loading
  }, $data.loading ? {} : $data.books.length === 0 ? {} : {}, {
    n: $data.books.length === 0,
    o: common_vendor.f($data.books, (book, k0, i0) => {
      return {
        a: book.cover || "/static/cover_placeholder.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author),
        d: common_vendor.t(book.price),
        e: common_vendor.t(book.annotationCount || book.annotations || 0),
        f: common_vendor.t(book.categoryName || book.category),
        g: book.id,
        h: common_vendor.o(($event) => $options.goToBookDetail(book), book.id)
      };
    })
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-8145b772"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/category/category.js.map
