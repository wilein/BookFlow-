"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      notifications: []
    };
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  onShow() {
    this.fetchNotifications();
  },
  methods: {
    async fetchNotifications() {
      try {
        this.notifications = await utils_api_user.getNotifications() || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/notifications.vue:61", "getNotifications failed", error);
      }
    },
    async openNotification(item) {
      try {
        if (!item.isRead) {
          await utils_api_user.readNotification(item.id);
          item.isRead = true;
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/notifications.vue:71", "readNotification failed", error);
      }
      if (item.routeUrl) {
        const url = item.routeUrl.startsWith("/") ? item.routeUrl : `/${item.routeUrl}`;
        if (url.includes("/pages/") && !url.includes("/pages/index/index") && !url.includes("/pages/community/community") && !url.includes("/pages/my/my")) {
          common_vendor.index.navigateTo({ url });
        } else if (url.includes("/pages/community/community") || url.includes("/pages/index/index") || url.includes("/pages/my/my")) {
          common_vendor.index.switchTab({ url });
        }
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: $data.headerHeight + "px",
    d: $data.statusBarHeight + "px",
    e: $data.headerHeight + "px",
    f: $data.notifications.length
  }, $data.notifications.length ? {
    g: common_vendor.f($data.notifications, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.title),
        b: common_vendor.t(item.createTime),
        c: common_vendor.t(item.type),
        d: common_vendor.t(item.content),
        e: item.id,
        f: !item.isRead ? 1 : "",
        g: common_vendor.o(($event) => $options.openNotification(item), item.id)
      };
    })
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-5e61bb80"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/notifications.js.map
