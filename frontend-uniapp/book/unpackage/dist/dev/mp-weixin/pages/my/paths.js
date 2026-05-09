"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      activeTab: "learning",
      currentPath: null,
      learningItems: [],
      createdItems: [],
      tabs: [
        { label: "正在学习", value: "learning" },
        { label: "我创建的", value: "created" }
      ],
      texts: {
        title: "我的路径",
        current: "当前学习",
        justNow: "刚刚学习",
        emptyDesc: "暂无路径描述",
        emptyLearning: "还没有开始学习的路径",
        emptyCreated: "还没有创建学习路径",
        goFind: "去路径广场看看",
        nodeCount: "节点",
        nodeDone: "节点完成"
      }
    };
  },
  computed: {
    displayItems() {
      return this.activeTab === "learning" ? this.learningItems : this.createdItems;
    },
    emptyText() {
      return this.activeTab === "learning" ? this.texts.emptyLearning : this.texts.emptyCreated;
    }
  },
  onLoad() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
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
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const [learning, current, created] = await Promise.all([
          utils_api_path.getMyLearningPaths(),
          utils_api_path.getCurrentLearningPath(),
          utils_api_user.getMyPaths()
        ]);
        this.learningItems = Array.isArray(learning) ? learning : [];
        this.currentPath = current || null;
        this.createdItems = Array.isArray(created) ? created : [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/paths.vue:133", "fetch my paths failed", error);
      }
    },
    getStatusText(item) {
      if (this.activeTab === "learning") {
        return `${item.progressPercent || 0}%`;
      }
      return item.statusLabel || "";
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    openPath(item) {
      const id = item.pathId || item.id;
      if (!id)
        return;
      const isCreator = this.activeTab === "created" || item.isCreator ? "1" : "0";
      common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}&isCreator=${isCreator}` });
    },
    goPathList() {
      common_vendor.index.navigateTo({ url: "/pages/path/list" });
    },
    goCreate() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({ url: "/pages/path/create" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: common_vendor.o((...args) => $options.goCreate && $options.goCreate(...args)),
    e: $data.headerHeight + "px",
    f: $data.statusBarHeight + "px",
    g: $data.headerRightSafe + "px",
    h: $data.headerHeight + "px",
    i: $data.currentPath
  }, $data.currentPath ? {
    j: common_vendor.t($data.texts.current),
    k: common_vendor.t($data.currentPath.progressPercent || 0),
    l: common_vendor.t($data.currentPath.title),
    m: common_vendor.t($data.currentPath.completedCount || 0),
    n: common_vendor.t($data.currentPath.nodeCount || 0),
    o: common_vendor.t($data.texts.nodeDone),
    p: common_vendor.t($data.currentPath.lastLearnTime || $data.texts.justNow),
    q: ($data.currentPath.progressPercent || 0) + "%",
    r: common_vendor.o(($event) => $options.openPath($data.currentPath))
  } : {}, {
    s: common_vendor.f($data.tabs, (tab, k0, i0) => {
      return {
        a: common_vendor.t(tab.label),
        b: tab.value,
        c: $data.activeTab === tab.value ? 1 : "",
        d: common_vendor.o(($event) => $data.activeTab = tab.value, tab.value)
      };
    }),
    t: $options.displayItems.length
  }, $options.displayItems.length ? {
    v: common_vendor.f($options.displayItems, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.title),
        b: common_vendor.t($options.getStatusText(item)),
        c: common_vendor.t(item.description || $data.texts.emptyDesc)
      }, $data.activeTab === "learning" ? {
        d: (item.progressPercent || 0) + "%",
        e: common_vendor.t(item.progressPercent || 0)
      } : {}, {
        f: common_vendor.t(item.difficulty),
        g: common_vendor.t(item.totalDuration),
        h: common_vendor.t(item.nodeCount || 0),
        i: item.id,
        j: common_vendor.o(($event) => $options.openPath(item), item.id)
      });
    }),
    w: $data.activeTab === "learning",
    x: common_vendor.t($data.texts.nodeCount)
  } : {
    y: common_vendor.t($options.emptyText),
    z: common_vendor.t($data.texts.goFind),
    A: common_vendor.o((...args) => $options.goPathList && $options.goPathList(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e15c8cfb"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/paths.js.map
