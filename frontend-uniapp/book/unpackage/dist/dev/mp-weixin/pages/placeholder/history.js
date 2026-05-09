"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      loading: false,
      historyList: [],
      textMap: {
        title: "浏览历史",
        clear: "清空",
        loading: "加载中...",
        empty: "暂无浏览记录",
        book: "书籍",
        path: "路径",
        noSubTitle: "暂无副标题",
        delete: "删除"
      }
    };
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
  },
  onShow() {
    this.fetchHistory();
  },
  methods: {
    async fetchHistory() {
      this.loading = true;
      try {
        const data = await utils_api_user.getBrowseHistory();
        this.historyList = Array.isArray(data) ? data : [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/history.vue:89", "fetchHistory failed", error);
      } finally {
        this.loading = false;
      }
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    openHistory(item) {
      if (item.routeUrl) {
        common_vendor.index.navigateTo({ url: item.routeUrl });
      }
    },
    async deleteItem(id) {
      try {
        await utils_api_user.deleteBrowseHistory(id);
        this.historyList = this.historyList.filter((item) => item.id !== id);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/history.vue:111", "delete history failed", error);
      }
    },
    handleClear() {
      if (!this.historyList.length)
        return;
      common_vendor.index.showModal({
        title: "清空确认",
        content: "确认清空所有浏览记录吗？",
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            await utils_api_user.clearBrowseHistory();
            this.historyList = [];
            common_vendor.index.showToast({ title: "已清空", icon: "none" });
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/placeholder/history.vue:126", "clear history failed", error);
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.textMap.title),
    d: common_vendor.t($data.textMap.clear),
    e: common_vendor.o((...args) => $options.handleClear && $options.handleClear(...args)),
    f: !$data.historyList.length ? 1 : "",
    g: $data.headerHeight + "px",
    h: $data.statusBarHeight + "px",
    i: $data.headerRightSafe + "px",
    j: $data.headerHeight + "px",
    k: $data.loading
  }, $data.loading ? {
    l: common_vendor.t($data.textMap.loading)
  } : $data.historyList.length === 0 ? {
    n: common_vendor.t($data.textMap.empty)
  } : {
    o: common_vendor.f($data.historyList, (item, k0, i0) => {
      return {
        a: item.coverUrl || "/static/cover_placeholder.png",
        b: common_vendor.t(item.targetType === "book" ? $data.textMap.book : $data.textMap.path),
        c: common_vendor.n(item.targetType),
        d: common_vendor.t(item.lastViewTime),
        e: common_vendor.t(item.title),
        f: common_vendor.t(item.subTitle || $data.textMap.noSubTitle),
        g: common_vendor.o(($event) => $options.openHistory(item), item.id),
        h: common_vendor.o(($event) => $options.deleteItem(item.id), item.id),
        i: item.id
      };
    }),
    p: common_vendor.t($data.textMap.delete)
  }, {
    m: $data.historyList.length === 0
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-de590ec9"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/placeholder/history.js.map
