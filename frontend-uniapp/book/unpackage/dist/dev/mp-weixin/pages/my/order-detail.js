"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_order = require("../../utils/api/order.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      orderId: "",
      role: "buyer",
      detail: null,
      issues: []
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.orderId = options.orderId || "";
    this.role = options.role === "seller" ? "seller" : "buyer";
  },
  onShow() {
    this.fetchDetail();
    this.fetchIssues();
  },
  methods: {
    async fetchDetail() {
      if (!this.orderId)
        return;
      try {
        this.detail = await utils_api_order.getOrderDetail(this.orderId);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/order-detail.vue:100", "getOrderDetail failed", error);
      }
    },
    async fetchIssues() {
      if (!this.orderId)
        return;
      try {
        this.issues = await utils_api_order.getOrderIssues(this.orderId) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/order-detail.vue:108", "getOrderIssues failed", error);
      }
    },
    goIssueCreate(type) {
      const query = [
        "mode=order-issue-create",
        `orderId=${encodeURIComponent(this.orderId)}`,
        `issueType=${encodeURIComponent(type)}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goIssueReply(issue) {
      const query = [
        "mode=order-issue-reply",
        `orderId=${encodeURIComponent(this.orderId)}`,
        `issueId=${encodeURIComponent(issue.id || "")}`,
        `title=${encodeURIComponent(issue.typeLabel || "订单问题")}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goOrderReport() {
      var _a;
      const query = [
        "mode=order-report",
        `orderId=${encodeURIComponent(this.orderId)}`,
        `title=${encodeURIComponent(((_a = this.detail) == null ? void 0 : _a.orderNo) || "")}`,
        `pagePath=${encodeURIComponent(`/pages/my/order-detail?orderId=${this.orderId}&role=${this.role}`)}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.navigateTo({ url: `/pages/my/orders?role=${this.role}` }) });
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
    f: $data.detail
  }, $data.detail ? common_vendor.e({
    g: common_vendor.t($data.detail.orderNo),
    h: common_vendor.t($data.detail.statusLabel),
    i: $data.detail.bookCover || "/static/logo.png",
    j: common_vendor.t($data.detail.bookTitle),
    k: common_vendor.t($data.detail.sellerName),
    l: common_vendor.t($data.detail.buyerName),
    m: common_vendor.t($data.detail.createTime || "-"),
    n: common_vendor.t($data.detail.totalAmount),
    o: common_vendor.t($data.detail.receiverName || "未填写"),
    p: common_vendor.t($data.detail.receiverPhone || ""),
    q: common_vendor.t($data.detail.receiverAddress || "未填写"),
    r: common_vendor.t($data.detail.buyerMessage || "暂无留言"),
    s: $data.issues.length
  }, $data.issues.length ? {
    t: common_vendor.f($data.issues, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.typeLabel),
        b: common_vendor.t(item.statusLabel),
        c: common_vendor.t(item.content),
        d: common_vendor.t(item.creatorName),
        e: common_vendor.t(item.createTime),
        f: item.replyContent
      }, item.replyContent ? {
        g: common_vendor.t(item.replyContent),
        h: common_vendor.t(item.replyUserName || "对方"),
        i: common_vendor.t(item.replyTime)
      } : {}, {
        j: item.canReply
      }, item.canReply ? {
        k: common_vendor.o(($event) => $options.goIssueReply(item), item.id)
      } : {}, {
        l: item.id
      });
    })
  } : {}, {
    v: common_vendor.o(($event) => $options.goIssueCreate("question")),
    w: common_vendor.o(($event) => $options.goIssueCreate("after_sale")),
    x: common_vendor.o((...args) => $options.goOrderReport && $options.goOrderReport(...args))
  }) : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-433b65bc"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/order-detail.js.map
