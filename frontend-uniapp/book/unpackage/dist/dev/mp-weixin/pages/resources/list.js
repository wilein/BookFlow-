"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_resource = require("../../utils/api/resource.js");
const common_assets = require("../../common/assets.js");
function normalizeId(value) {
  const text = String(value == null ? "" : value).trim();
  if (!text || text === "undefined" || text === "null") {
    return void 0;
  }
  return /^\d+$/.test(text) ? text : void 0;
}
const TEXTS = {
  defaultTitle: "配套资源",
  resourceShort: "资",
  resource: "资源",
  emptyDesc: "暂无资源描述",
  file: "文件",
  download: "下载",
  empty: "暂无资源",
  filePending: "资源文件待补充",
  copied: "资源链接已复制"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      pageTitle: TEXTS.defaultTitle,
      items: []
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.title) {
      this.pageTitle = decodeURIComponent(options.title);
    }
    this.fetchData(options);
  },
  methods: {
    async fetchData(options) {
      try {
        const bookId = normalizeId(options.bookId);
        const pathNodeId = normalizeId(options.pathNodeId);
        this.items = await utils_api_resource.getResourceList({
          bookId,
          pathNodeId
        }) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/resources/list.vue:88", "getResourceList failed", error);
      }
    },
    openResource(item) {
      common_vendor.index.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(item.id)}` });
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
    c: common_vendor.t($data.pageTitle),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: $data.items.length
  }, $data.items.length ? {
    h: common_vendor.f($data.items, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.typeLabel ? item.typeLabel.slice(0, 1) : $data.texts.resourceShort),
        b: common_vendor.t(item.title || item.name),
        c: common_vendor.t(item.typeLabel || $data.texts.resource),
        d: common_vendor.t(item.description || $data.texts.emptyDesc),
        e: common_vendor.t(item.fileFormat || $data.texts.file),
        f: common_vendor.t(item.downloadCount || 0),
        g: item.id,
        h: common_vendor.o(($event) => $options.openResource(item), item.id)
      };
    }),
    i: common_vendor.t($data.texts.download)
  } : {
    j: common_vendor.t($data.texts.empty)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-6490c4e0"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/resources/list.js.map
