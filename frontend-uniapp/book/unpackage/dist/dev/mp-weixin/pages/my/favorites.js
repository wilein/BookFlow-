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
      currentTab: "book",
      texts: {
        title: "我的收藏",
        creator: "创建者：",
        bookDescFallback: "暂无描述",
        pathDescFallback: "暂无路径描述",
        empty: "暂无收藏",
        currency: "¥"
      },
      tabs: [
        { key: "book", label: "书籍" },
        { key: "path", label: "路径" }
      ],
      items: []
    };
  },
  onLoad(options) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.type === "path")
      this.currentTab = "path";
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.items = await utils_api_user.getMyFavorites(this.currentTab) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/favorites.vue:81", "getMyFavorites failed", error);
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
    openItem(item) {
      if (this.currentTab === "book") {
        common_vendor.index.navigateTo({ url: `/pages/books/detail?${utils_bookDetail.buildBookQueryFromListItem(item)}` });
        return;
      }
      const params = [
        `pathId=${encodeURIComponent(item.id || "")}`,
        `title=${encodeURIComponent(item.title || "")}`,
        `creator=${encodeURIComponent(item.creator || "")}`,
        `difficulty=${encodeURIComponent(item.difficulty || "")}`,
        `totalDuration=${encodeURIComponent(item.totalDuration || "")}`,
        `description=${encodeURIComponent(item.description || "")}`,
        "isCreator=0"
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/path/detail?${params}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
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
    h: $data.items.length
  }, $data.items.length ? {
    i: common_vendor.f($data.items, (item, k0, i0) => {
      return common_vendor.e($data.currentTab === "book" ? {
        a: item.cover || "/static/logo.png"
      } : {}, {
        b: common_vendor.t(item.title)
      }, $data.currentTab === "book" ? {
        c: common_vendor.t(item.author),
        d: common_vendor.t(item.conditionLabel)
      } : {
        e: common_vendor.t($data.texts.creator),
        f: common_vendor.t(item.creator)
      }, {
        g: common_vendor.t(item.description || ($data.currentTab === "book" ? $data.texts.bookDescFallback : $data.texts.pathDescFallback))
      }, $data.currentTab === "book" ? {
        h: common_vendor.t($data.texts.currency),
        i: common_vendor.t(item.price)
      } : {
        j: common_vendor.t(item.difficulty),
        k: common_vendor.t(item.totalDuration)
      }, {
        l: item.id,
        m: common_vendor.o(($event) => $options.openItem(item), item.id)
      });
    }),
    j: $data.currentTab === "book",
    k: $data.currentTab === "book",
    l: $data.currentTab === "book"
  } : {
    m: common_vendor.t($data.texts.empty)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-1a2a9709"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/favorites.js.map
