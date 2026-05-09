"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_order = require("../../utils/api/order.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  title: "我的订单",
  buyerRole: "我是买家",
  sellerRole: "我是卖家",
  all: "全部",
  pendingPay: "待付款",
  pendingShip: "待发货",
  pendingReceive: "待收货",
  finished: "已完成",
  canceled: "已取消",
  refunding: "售后中",
  orderNo: "订单号：",
  seller: "卖家：",
  buyer: "买家：",
  createTime: "创建时间：",
  receiverInfo: "收货信息",
  receiverAddress: "收货地址",
  notFilled: "未填写",
  noBuyerMessage: "无买家留言",
  empty: "暂无订单",
  cancelOrder: "取消订单",
  pay: "去付款",
  confirmReceipt: "确认收货",
  shipOrder: "去发货",
  canceledSuccess: "已取消订单",
  paySuccess: "支付成功",
  receiveSuccess: "已确认收货",
  shipSuccess: "已发货",
  loading: "加载中...",
  retry: "重试",
  loadFailed: "订单加载失败",
  currency: "￥"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      currentTab: "all",
      currentRole: "buyer",
      roleTabs: [
        { key: "buyer", label: TEXTS.buyerRole },
        { key: "seller", label: TEXTS.sellerRole }
      ],
      tabs: [
        { key: "all", label: TEXTS.all },
        { key: "1", label: TEXTS.pendingPay },
        { key: "2", label: TEXTS.pendingShip },
        { key: "3", label: TEXTS.pendingReceive },
        { key: "4", label: TEXTS.finished },
        { key: "5", label: TEXTS.canceled },
        { key: "6", label: TEXTS.refunding }
      ],
      orders: [],
      loading: false,
      errorMessage: ""
    };
  },
  onLoad(options) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    if (options.status)
      this.currentTab = String(options.status);
    if (options.role)
      this.currentRole = String(options.role) === "seller" ? "seller" : "buyer";
  },
  onShow() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    this.fetchData();
  },
  methods: {
    async fetchData() {
      this.loading = true;
      this.errorMessage = "";
      try {
        this.orders = await utils_api_user.getMyOrders(this.currentTab, this.currentRole) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/orders.vue:175", "getMyOrders failed", error);
        this.errorMessage = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.loading = false;
      }
    },
    buildActions(item) {
      const actions = [];
      if (item.canCancel) {
        actions.push({ key: "cancel", label: TEXTS.cancelOrder, type: "secondary" });
      }
      if (item.canPay) {
        actions.push({ key: "pay", label: TEXTS.pay, type: "primary" });
      }
      if (item.canShip) {
        actions.push({ key: "ship", label: TEXTS.shipOrder, type: "primary" });
      }
      if (item.canConfirm) {
        actions.push({ key: "receive", label: TEXTS.confirmReceipt, type: "primary" });
      }
      return actions;
    },
    async handleAction(action, item) {
      try {
        if (action === "cancel") {
          await utils_api_order.cancelOrder(item.id);
          common_vendor.index.showToast({ title: TEXTS.canceledSuccess, icon: "success" });
        } else if (action === "pay") {
          await utils_api_order.payOrder(item.id);
          common_vendor.index.showToast({ title: TEXTS.paySuccess, icon: "success" });
        } else if (action === "receive") {
          await utils_api_order.confirmReceipt(item.id);
          common_vendor.index.showToast({ title: TEXTS.receiveSuccess, icon: "success" });
        } else if (action === "ship") {
          await utils_api_order.shipOrder(item.id);
          common_vendor.index.showToast({ title: TEXTS.shipSuccess, icon: "success" });
        }
        this.fetchData();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/orders.vue:214", "handle order action failed", error);
      }
    },
    openDetail(item) {
      common_vendor.index.navigateTo({ url: `/pages/my/order-detail?orderId=${encodeURIComponent(item.id || "")}&role=${encodeURIComponent(this.currentRole)}` });
    },
    switchTab(tab) {
      if (this.currentTab === tab)
        return;
      this.currentTab = tab;
      this.fetchData();
    },
    switchRole(role) {
      if (this.currentRole === role)
        return;
      this.currentRole = role;
      this.fetchData();
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
    c: common_vendor.t($data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.f($data.roleTabs, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.key,
        c: $data.currentRole === item.key ? 1 : "",
        d: common_vendor.o(($event) => $options.switchRole(item.key), item.key)
      };
    }),
    h: common_vendor.f($data.tabs, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.key,
        c: $data.currentTab === item.key ? 1 : "",
        d: common_vendor.o(($event) => $options.switchTab(item.key), item.key)
      };
    }),
    i: $data.loading
  }, $data.loading ? {
    j: common_vendor.t($data.texts.loading)
  } : $data.errorMessage ? {
    l: common_vendor.t($data.errorMessage),
    m: common_vendor.t($data.texts.retry),
    n: common_vendor.o((...args) => $options.fetchData && $options.fetchData(...args))
  } : $data.orders.length ? {
    p: common_vendor.f($data.orders, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.orderNo),
        b: common_vendor.t(item.statusLabel),
        c: item.bookCover || "/static/logo.png",
        d: common_vendor.t(item.bookTitle),
        e: common_vendor.t($data.currentRole === "seller" ? item.buyerName : item.sellerName),
        f: common_vendor.t(item.createTime),
        g: common_vendor.t(item.receiverName || $data.texts.notFilled),
        h: common_vendor.t(item.receiverPhone || ""),
        i: common_vendor.t(item.receiverAddress || $data.texts.notFilled),
        j: common_vendor.t(item.buyerMessage || $data.texts.noBuyerMessage),
        k: common_vendor.t(item.totalAmount),
        l: $options.buildActions(item).length
      }, $options.buildActions(item).length ? {
        m: common_vendor.f($options.buildActions(item), (action, k1, i1) => {
          return {
            a: common_vendor.t(action.label),
            b: action.key,
            c: common_vendor.n(action.type),
            d: common_vendor.o(($event) => $options.handleAction(action.key, item), action.key)
          };
        })
      } : {}, {
        n: item.id,
        o: common_vendor.o(($event) => $options.openDetail(item), item.id)
      });
    }),
    q: common_vendor.t($data.texts.orderNo),
    r: common_vendor.t($data.currentRole === "seller" ? $data.texts.buyer : $data.texts.seller),
    s: common_vendor.t($data.texts.createTime),
    t: common_vendor.t($data.texts.receiverInfo),
    v: common_vendor.t($data.texts.receiverAddress),
    w: common_vendor.t($data.texts.currency)
  } : {
    x: common_vendor.t($data.texts.empty)
  }, {
    k: $data.errorMessage,
    o: $data.orders.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-09724fb8"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/orders.js.map
