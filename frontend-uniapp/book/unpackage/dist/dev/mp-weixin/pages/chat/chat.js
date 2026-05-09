"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_chat = require("../../utils/api/chat.js");
const utils_api_order = require("../../utils/api/order.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  tradeTarget: "交易对象",
  bookFallback: "书籍",
  sellerFallback: "卖家",
  buyerFallback: "买家",
  sellerLabel: "卖家：",
  buyerLabel: "买家：",
  orderNoLabel: "订单号：",
  currency: "￥",
  inputPlaceholder: "输入消息...",
  send: "发送",
  orderStatusTitle: "订单状态",
  orderStatusDesc: "付款、发货、收货都会在顶部卡片同步更新，聊天只保留和交易直接相关的信息。",
  bookWrapStart: "《",
  bookTrading: "》交易中",
  sellerRole: "我是卖家",
  buyerRole: "我是买家",
  ship: "去发货",
  confirm: "确认收货",
  pay: "去付款",
  createOrder: "去下单",
  viewOrder: "查看订单",
  cancel: "取消订单",
  paySuccess: "支付成功",
  cancelSuccess: "订单已取消",
  shipSuccess: "发货成功",
  confirmSuccess: "已确认收货",
  loading: "加载中...",
  retry: "重试",
  emptyMessages: "暂无消息",
  loadFailed: "聊天加载失败"
};
const POLL_INTERVAL_MS = 3e3;
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      sellerName: "",
      bookTitle: "",
      sellerId: "",
      bookId: "",
      sessionId: "",
      draftMessage: "",
      scrollAnchor: "",
      messages: [],
      sessionMeta: {},
      loading: false,
      errorMessage: "",
      pollTimer: null,
      polling: false
    };
  },
  computed: {
    peerName() {
      return this.sessionMeta.name || this.sellerName || this.texts.tradeTarget;
    },
    currentBookTitle() {
      return this.sessionMeta.bookTitle || this.bookTitle || this.texts.bookFallback;
    },
    currentStatusLabel() {
      return this.sessionMeta.orderStatusLabel || this.sessionMeta.bookStatusLabel || "沟通中";
    },
    roleLabel() {
      return this.sessionMeta.role === "seller" ? this.texts.sellerRole : this.texts.buyerRole;
    },
    primaryAction() {
      if (this.sessionMeta.canShip)
        return { key: "ship", label: this.texts.ship };
      if (this.sessionMeta.canConfirm)
        return { key: "confirm", label: this.texts.confirm };
      if (this.sessionMeta.canPay)
        return { key: "pay", label: this.texts.pay };
      if (this.sessionMeta.canCreateOrder)
        return { key: "createOrder", label: this.texts.createOrder };
      if (this.sessionMeta.orderId)
        return { key: "viewOrder", label: this.texts.viewOrder };
      return null;
    },
    secondaryAction() {
      if (this.sessionMeta.canCancel)
        return { key: "cancel", label: this.texts.cancel };
      return null;
    }
  },
  async onLoad(options) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.sellerName = decodeURIComponent(options.sellerName || "");
    this.bookTitle = decodeURIComponent(options.bookTitle || "");
    this.sellerId = decodeURIComponent(options.sellerId || "");
    this.bookId = decodeURIComponent(options.bookId || "");
    this.sessionId = decodeURIComponent(options.sessionId || "");
    await this.initSession();
  },
  async onShow() {
    if (!this.sessionId)
      return;
    await this.refreshSessionMeta();
    await this.fetchMessages({ silent: this.messages.length > 0 });
    this.startPolling();
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
  },
  methods: {
    applySession(session = {}) {
      this.sessionMeta = { ...this.sessionMeta, ...session };
      this.sessionId = session.sessionId || session.id || this.sessionId;
      this.sellerName = session.name || this.sellerName;
      this.bookTitle = session.bookTitle || this.bookTitle;
      this.bookId = session.bookId || this.bookId;
      this.sellerId = session.sellerId || this.sellerId;
    },
    async initSession() {
      try {
        const session = await utils_api_chat.openChatSession({
          sessionId: this.sessionId || void 0,
          sellerId: this.sellerId || void 0,
          bookId: this.bookId || void 0
        });
        this.applySession(session);
        await this.fetchMessages();
        this.startPolling();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:233", "initSession failed", error);
        this.errorMessage = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      }
    },
    async refreshSessionMeta() {
      if (!this.sessionId)
        return;
      try {
        const session = await utils_api_chat.openChatSession({ sessionId: this.sessionId });
        this.applySession(session);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:243", "refreshSessionMeta failed", error);
      }
    },
    async fetchMessages(options = {}) {
      if (!this.sessionId)
        return;
      const silent = Boolean(options.silent);
      if (!silent) {
        this.loading = true;
        this.errorMessage = "";
      }
      try {
        const data = await utils_api_chat.getChatMessages(this.sessionId);
        if (Array.isArray(data)) {
          this.messages = data;
          this.errorMessage = "";
          await utils_api_chat.markChatRead(this.sessionId);
          this.scrollToBottom();
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:262", "fetchMessages failed", error);
        this.errorMessage = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.loading = false;
      }
    },
    retryLoad() {
      this.initSession();
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/community/community" });
        }
      });
    },
    scrollToBottom() {
      this.scrollAnchor = "";
      this.$nextTick(() => {
        this.scrollAnchor = "chat-bottom-anchor";
      });
    },
    formatPrice(value) {
      const amount = Number(value || 0);
      if (!Number.isFinite(amount))
        return "0.00";
      return amount.toFixed(2);
    },
    openOrderDetail() {
      if (!this.sessionMeta.orderId)
        return;
      common_vendor.index.navigateTo({
        url: `/pages/my/order-detail?orderId=${encodeURIComponent(this.sessionMeta.orderId)}`
      });
    },
    async handleTradeAction(action) {
      try {
        if (action === "viewOrder") {
          this.openOrderDetail();
          return;
        }
        if (action === "createOrder") {
          const bookId = this.sessionMeta.bookId || this.bookId;
          if (bookId) {
            common_vendor.index.navigateTo({ url: `/pages/cart/checkout?bookId=${encodeURIComponent(bookId)}` });
          }
          return;
        }
        if (action === "pay") {
          await utils_api_order.payOrder(this.sessionMeta.orderId);
          common_vendor.index.showToast({ title: this.texts.paySuccess, icon: "success" });
        }
        if (action === "cancel") {
          await utils_api_order.cancelOrder(this.sessionMeta.orderId);
          common_vendor.index.showToast({ title: this.texts.cancelSuccess, icon: "success" });
        }
        if (action === "ship") {
          await utils_api_order.shipOrder(this.sessionMeta.orderId);
          common_vendor.index.showToast({ title: this.texts.shipSuccess, icon: "success" });
        }
        if (action === "confirm") {
          await utils_api_order.confirmReceipt(this.sessionMeta.orderId);
          common_vendor.index.showToast({ title: this.texts.confirmSuccess, icon: "success" });
        }
        await this.refreshSessionMeta();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:326", "handleTradeAction failed", error);
      }
    },
    async submitMessage() {
      if (!this.sessionId || !this.draftMessage.trim())
        return;
      try {
        const message = await utils_api_chat.sendChatMessage({
          sessionId: this.sessionId,
          content: this.draftMessage.trim()
        });
        this.messages.push(message);
        this.draftMessage = "";
        this.scrollToBottom();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:340", "submitMessage failed", error);
      }
    },
    latestMessageId() {
      return this.messages.reduce((maxId, item) => Math.max(maxId, Number(item.id || 0)), 0);
    },
    startPolling() {
      if (!this.sessionId)
        return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        this.pollMessages();
      }, POLL_INTERVAL_MS);
    },
    stopPolling() {
      if (!this.pollTimer)
        return;
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    },
    async pollMessages() {
      if (!this.sessionId || this.polling)
        return;
      this.polling = true;
      try {
        const data = await utils_api_chat.pollChatMessages(this.sessionId, this.latestMessageId());
        const items = Array.isArray(data == null ? void 0 : data.items) ? data.items : [];
        if (items.length) {
          const seen = new Set(this.messages.map((item) => String(item.id)));
          this.messages = [
            ...this.messages,
            ...items.filter((item) => !seen.has(String(item.id)))
          ];
          this.scrollToBottom();
        }
        if (items.length || Number((data == null ? void 0 : data.unreadCount) || 0) > 0) {
          await utils_api_chat.markChatRead(this.sessionId);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:376", "pollMessages failed", error);
      } finally {
        this.polling = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($options.peerName),
    d: common_vendor.t($data.texts.bookWrapStart),
    e: common_vendor.t($options.currentBookTitle),
    f: common_vendor.t($data.texts.bookTrading),
    g: $data.headerHeight + "px",
    h: $data.statusBarHeight + "px",
    i: $data.headerHeight + "px",
    j: $data.sessionMeta.bookCover || "/static/logo.png",
    k: common_vendor.t($options.roleLabel),
    l: common_vendor.t($options.currentStatusLabel),
    m: common_vendor.t($options.currentBookTitle),
    n: common_vendor.t($data.texts.sellerLabel),
    o: common_vendor.t($data.sessionMeta.sellerName || $data.sellerName || $data.texts.sellerFallback),
    p: common_vendor.t($data.texts.buyerLabel),
    q: common_vendor.t($data.sessionMeta.buyerName || $data.texts.buyerFallback),
    r: $data.sessionMeta.orderNo
  }, $data.sessionMeta.orderNo ? {
    s: common_vendor.t($data.texts.orderNoLabel),
    t: common_vendor.t($data.sessionMeta.orderNo)
  } : {}, {
    v: common_vendor.t($data.texts.currency),
    w: common_vendor.t($options.formatPrice($data.sessionMeta.bookPrice)),
    x: $options.primaryAction || $options.secondaryAction
  }, $options.primaryAction || $options.secondaryAction ? common_vendor.e({
    y: $options.secondaryAction
  }, $options.secondaryAction ? {
    z: common_vendor.t($options.secondaryAction.label),
    A: common_vendor.o(($event) => $options.handleTradeAction($options.secondaryAction.key))
  } : {}, {
    B: $options.primaryAction
  }, $options.primaryAction ? {
    C: common_vendor.t($options.primaryAction.label),
    D: common_vendor.o(($event) => $options.handleTradeAction($options.primaryAction.key))
  } : {}) : {}, {
    E: common_vendor.o((...args) => $options.openOrderDetail && $options.openOrderDetail(...args)),
    F: common_vendor.t($data.texts.orderStatusTitle),
    G: common_vendor.t($data.texts.orderStatusDesc),
    H: $data.loading
  }, $data.loading ? {
    I: common_vendor.t($data.texts.loading)
  } : $data.errorMessage ? {
    K: common_vendor.t($data.errorMessage),
    L: common_vendor.t($data.texts.retry),
    M: common_vendor.o((...args) => $options.retryLoad && $options.retryLoad(...args))
  } : !$data.messages.length ? {
    O: common_vendor.t($data.texts.emptyMessages)
  } : {
    P: common_vendor.f($data.messages, (item, k0, i0) => {
      return common_vendor.e({
        a: item.mine
      }, item.mine ? {
        b: common_vendor.t(item.content),
        c: common_vendor.t(item.time),
        d: item.senderAvatar || $data.sessionMeta.selfAvatar || "/static/logo.png"
      } : {
        e: item.senderAvatar || $data.sessionMeta.avatar || "/static/logo.png",
        f: common_vendor.t(item.content),
        g: common_vendor.t(item.time)
      }, {
        h: item.id
      });
    })
  }, {
    J: $data.errorMessage,
    N: !$data.messages.length,
    Q: $data.scrollAnchor,
    R: $data.texts.inputPlaceholder,
    S: common_vendor.o((...args) => $options.submitMessage && $options.submitMessage(...args)),
    T: $data.draftMessage,
    U: common_vendor.o(($event) => $data.draftMessage = $event.detail.value),
    V: common_vendor.t($data.texts.send),
    W: common_vendor.o((...args) => $options.submitMessage && $options.submitMessage(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0a633310"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/chat/chat.js.map
