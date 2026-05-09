"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: "",
      activeCategory: "全部",
      categories: ["全部", "编程开发", "计算机基础", "考研课程", "设计产品", "语言文学", "其他"],
      paths: [],
      currentPath: null,
      loading: false
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 12;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.keyword = decodeURIComponent(options.keyword || "");
    const category = decodeURIComponent(options.category || "");
    if (this.categories.includes(category)) {
      this.activeCategory = category;
    }
    this.fetchPaths();
  },
  onShow() {
    this.fetchCurrentPath();
  },
  methods: {
    async fetchPaths() {
      this.loading = true;
      try {
        const params = {
          category: this.activeCategory === "全部" ? "" : this.activeCategory,
          keyword: this.keyword.trim()
        };
        this.paths = await utils_api_path.getPathList(params) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/list.vue:143", "fetchPaths failed", error);
      } finally {
        this.loading = false;
      }
    },
    async fetchCurrentPath() {
      if (!utils_auth.hasValidSession()) {
        this.currentPath = null;
        return;
      }
      try {
        this.currentPath = await utils_api_path.getCurrentLearningPath() || null;
      } catch (error) {
        this.currentPath = null;
      }
    },
    switchCategory(category) {
      if (this.activeCategory === category)
        return;
      this.activeCategory = category;
      this.fetchPaths();
    },
    clearSearch() {
      this.keyword = "";
      this.fetchPaths();
    },
    openPath(item) {
      const id = item.pathId || item.id;
      if (!id)
        return;
      common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
    },
    goCreate() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({ url: "/pages/path/create" });
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/index/index" }) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.o((...args) => $options.goCreate && $options.goCreate(...args)),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerRightSafe + "px",
    g: $data.headerHeight + "px",
    h: $data.currentPath
  }, $data.currentPath ? {
    i: common_vendor.t($data.currentPath.progressPercent || 0),
    j: common_vendor.t($data.currentPath.title),
    k: common_vendor.t($data.currentPath.creator),
    l: common_vendor.t($data.currentPath.difficulty),
    m: common_vendor.t($data.currentPath.totalDuration),
    n: ($data.currentPath.progressPercent || 0) + "%",
    o: common_vendor.o(($event) => $options.openPath($data.currentPath))
  } : {}, {
    p: common_assets._imports_1,
    q: common_vendor.o((...args) => $options.fetchPaths && $options.fetchPaths(...args)),
    r: $data.keyword,
    s: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    t: $data.keyword
  }, $data.keyword ? {
    v: common_vendor.o((...args) => $options.clearSearch && $options.clearSearch(...args))
  } : {}, {
    w: common_vendor.f($data.categories, (item, k0, i0) => {
      return {
        a: common_vendor.t(item),
        b: item,
        c: $data.activeCategory === item ? 1 : "",
        d: common_vendor.o(($event) => $options.switchCategory(item), item)
      };
    }),
    x: common_vendor.t($data.activeCategory),
    y: common_vendor.t($data.paths.length),
    z: common_vendor.o((...args) => $options.fetchPaths && $options.fetchPaths(...args)),
    A: $data.paths.length
  }, $data.paths.length ? {
    B: common_vendor.f($data.paths, (item, k0, i0) => {
      return common_vendor.e({
        a: item.coverImage || item.cover
      }, item.coverImage || item.cover ? {
        b: item.coverImage || item.cover
      } : {}, {
        c: common_vendor.t(item.title),
        d: common_vendor.t(item.description || "暂无路径说明"),
        e: common_vendor.t(item.category || "其他"),
        f: common_vendor.t(item.creator || "校园同学"),
        g: common_vendor.t(item.difficulty || "入门"),
        h: common_vendor.t(item.totalDuration || "时长待补充"),
        i: common_vendor.t(item.nodeCount || 0),
        j: common_vendor.t(item.learnerCount || item.learners || 0),
        k: item.started
      }, item.started ? {
        l: common_vendor.t(item.progressPercent || 0)
      } : {}, {
        m: item.id,
        n: common_vendor.o(($event) => $options.openPath(item), item.id)
      });
    })
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-8694a2ab"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/path/list.js.map
