"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      keyword: "",
      categories: [],
      selectedCategoryId: null,
      categoryBooksMap: {},
      // 分类名 -> 书籍列表（后端返回，每类最多6条）
      books: [],
      loading: false,
      // 搜索历史（从本地存储读取，示例用静态）
      searchHistory: ["Java编程思想", "算法导论", "设计模式", "数据结构"],
      // 热门搜索（带角标）
      hotSearches: [
        { name: "Java编程思想", badge: "热" },
        { name: "机器学习实战", badge: "新" },
        { name: "高等数学", badge: "热" },
        { name: "操作系统", badge: "" }
      ]
    };
  },
  onLoad() {
    common_vendor.index.getSystemInfo({
      success: (res) => {
        this.statusBarHeight = res.statusBarHeight;
      }
    });
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const screenWidth = common_vendor.index.getSystemInfoSync().windowWidth;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
    } else {
      this.capsulePaddingRight = 100;
    }
    this.fetchCategoryData();
  },
  onReady() {
    common_vendor.index.createSelectorQuery().in(this).select(".fixed-header").boundingClientRect((rect) => {
      if (rect) {
        this.fixedHeaderHeight = rect.height;
      }
    }).exec();
  },
  methods: {
    // 从后端获取分类及书籍数据（每个分类前6条）
    fetchCategoryData() {
      this.loading = true;
      common_vendor.index.request({
        url: "http://localhost:8080/book/category",
        method: "GET",
        success: (res) => {
          if (res.data.code === "200" && res.data.data) {
            const data = res.data.data;
            const categoryNames = Object.keys(data);
            this.categories = categoryNames.map((name, index) => ({
              id: name,
              name
            }));
            this.categoryBooksMap = data;
            if (this.categories.length > 0) {
              this.selectedCategoryId = this.categories[0].id;
              this.books = data[this.selectedCategoryId] || [];
            }
          } else {
            common_vendor.index.showToast({ title: "分类数据加载失败", icon: "none" });
          }
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/category/category.vue:211", "category api error", err);
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
        },
        complete: () => {
          this.loading = false;
        }
      });
    },
    selectCategory(categoryId) {
      if (this.selectedCategoryId === categoryId)
        return;
      this.selectedCategoryId = categoryId;
      this.books = this.categoryBooksMap[categoryId] || [];
    },
    // 处理搜索
    handleSearch() {
      if (!this.keyword.trim()) {
        common_vendor.index.showToast({ title: "请输入关键词", icon: "none" });
        return;
      }
      if (!this.searchHistory.includes(this.keyword)) {
        this.searchHistory.unshift(this.keyword);
        if (this.searchHistory.length > 10)
          this.searchHistory.pop();
      }
      common_vendor.index.navigateTo({
        url: `/pages/search/search?keyword=${encodeURIComponent(this.keyword)}&categoryId=${this.selectedCategoryId}`
      });
    },
    // 使用历史搜索词
    useHistory(item) {
      this.keyword = item;
      this.handleSearch();
    },
    // 使用热门搜索
    useHotSearch(item) {
      this.keyword = item.name;
      this.handleSearch();
    },
    // 清空历史
    clearHistory() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定清空搜索历史吗？",
        success: (res) => {
          if (res.confirm) {
            this.searchHistory = [];
          }
        }
      });
    },
    goToBookDetail(book) {
      common_vendor.index.navigateTo({
        url: `/pages/books/detail?id=${book.id}`
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: $data.statusBarHeight + "px",
    c: $data.capsulePaddingRight + "px",
    d: $data.fixedHeaderHeight + "px",
    e: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    f: $data.keyword,
    g: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    h: common_assets._imports_1,
    i: common_vendor.o((...args) => $options.handleSearch && $options.handleSearch(...args)),
    j: $data.searchHistory.length > 0
  }, $data.searchHistory.length > 0 ? {
    k: common_vendor.o((...args) => $options.clearHistory && $options.clearHistory(...args)),
    l: common_vendor.f($data.searchHistory, (item, index, i0) => {
      return {
        a: common_vendor.t(item),
        b: index,
        c: common_vendor.o(($event) => $options.useHistory(item), index)
      };
    })
  } : {}, {
    m: common_vendor.f($data.hotSearches, (item, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.name),
        b: item.badge
      }, item.badge ? {
        c: common_vendor.t(item.badge)
      } : {}, {
        d: index,
        e: common_vendor.o(($event) => $options.useHotSearch(item), index)
      });
    }),
    n: common_vendor.f($data.categories, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.name),
        b: item.id,
        c: $data.selectedCategoryId === item.id ? 1 : "",
        d: common_vendor.o(($event) => $options.selectCategory(item.id), item.id)
      };
    }),
    o: $data.loading
  }, $data.loading ? {} : common_vendor.e({
    p: common_vendor.f($data.books, (book, k0, i0) => {
      return {
        a: book.cover || "/static/cover_placeholder.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author),
        d: common_vendor.t(book.price),
        e: common_vendor.t(book.annotations),
        f: common_vendor.t(book.categoryName || book.category),
        g: book.id,
        h: common_vendor.o(($event) => $options.goToBookDetail(book), book.id)
      };
    }),
    q: $data.books.length === 0
  }, $data.books.length === 0 ? {} : {}));
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-8145b772"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/category/category.js.map
