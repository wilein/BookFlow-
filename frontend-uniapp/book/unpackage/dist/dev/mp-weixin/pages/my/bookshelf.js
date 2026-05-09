"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const utils_bookDetail = require("../../utils/book-detail.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      selectMode: "",
      currentTab: "selling",
      texts: {
        title: "我的书架",
        selectForAnnotation: "选择批注书籍",
        currency: "¥",
        annotationCount: "批注",
        empty: "暂无书籍",
        unknownAuthor: "未知作者"
      },
      tabs: [
        { key: "selling", label: "在售" },
        { key: "sold", label: "已售" }
      ],
      books: []
    };
  },
  onLoad(options = {}) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.selectMode = options.select || "";
    if (options.status === "sold")
      this.currentTab = "sold";
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.books = await utils_api_user.getMyBookshelf(this.currentTab) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/bookshelf.vue:85", "getMyBookshelf failed", error);
      }
    },
    switchTab(tab) {
      if (this.currentTab === tab)
        return;
      this.currentTab = tab;
      this.fetchData();
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    openDetail(book) {
      if (this.selectMode === "annotation") {
        const params = [
          `bookId=${encodeURIComponent(book.id || "")}`,
          `bookTitle=${encodeURIComponent(book.title || "")}`
        ].join("&");
        common_vendor.index.navigateTo({ url: `/pages/annotations/list?${params}` });
        return;
      }
      common_vendor.index.navigateTo({ url: `/pages/books/detail?${utils_bookDetail.buildBookQueryFromListItem(book)}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.selectMode === "annotation" ? $data.texts.selectForAnnotation : $data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.f($data.tabs, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.key,
        c: $data.currentTab === item.key ? 1 : "",
        d: common_vendor.o(($event) => $options.switchTab(item.key), item.key)
      };
    }),
    h: $data.books.length
  }, $data.books.length ? {
    i: common_vendor.f($data.books, (book, k0, i0) => {
      return {
        a: book.cover || "/static/logo.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author || $data.texts.unknownAuthor),
        d: common_vendor.t(book.conditionLabel),
        e: common_vendor.t(book.statusLabel),
        f: common_vendor.t(book.price),
        g: common_vendor.t(book.annotationCount || 0),
        h: book.id,
        i: common_vendor.o(($event) => $options.openDetail(book), book.id)
      };
    }),
    j: common_vendor.t($data.texts.currency),
    k: common_vendor.t($data.texts.annotationCount)
  } : {
    l: common_vendor.t($data.texts.empty)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e23eefad"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/bookshelf.js.map
