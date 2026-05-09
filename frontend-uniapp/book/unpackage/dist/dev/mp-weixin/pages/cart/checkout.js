"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_api_cart = require("../../utils/api/cart.js");
const utils_api_order = require("../../utils/api/order.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const SELECTED_ADDRESS_KEY = "BOOKFLOW_CHECKOUT_SELECTED_ADDRESS_ID";
function splitIds(value) {
  return String(value || "").split(",").map((item) => item.trim()).filter(Boolean);
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      cartItemIds: [],
      directBookId: "",
      items: [],
      addresses: [],
      selectedAddressId: "",
      buyerMessage: "",
      loading: false,
      submitting: false,
      loadError: false,
      texts: {
        title: "确认订单",
        loading: "加载中...",
        loadError: "结算信息加载失败",
        retry: "重试",
        address: "收货地址",
        noAddress: "请选择收货地址",
        books: "结算书籍",
        seller: "卖家：",
        message: "买家留言",
        messagePlaceholder: "给卖家留言，可选",
        selectedUnit: " 件",
        submit: "提交订单",
        submitting: "提交中...",
        needAddress: "请选择收货地址",
        empty: "请选择要结算的书籍",
        successTitle: "下单成功",
        successText: "订单已创建，是否前往订单列表付款？",
        viewOrders: "查看订单",
        currency: "￥"
      }
    };
  },
  computed: {
    selectedAddress() {
      return this.addresses.find((item) => String(item.id) === String(this.selectedAddressId)) || this.addresses.find((item) => item.isDefault);
    },
    totalAmount() {
      return this.items.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2);
    }
  },
  async onLoad(options = {}) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.cartItemIds = splitIds(options.cartItemIds);
    this.directBookId = String(options.bookId || "").trim();
    await this.loadData();
  },
  onShow() {
    const addressId = common_vendor.index.getStorageSync(SELECTED_ADDRESS_KEY);
    if (addressId) {
      this.selectedAddressId = addressId;
      common_vendor.index.removeStorageSync(SELECTED_ADDRESS_KEY);
      this.refreshAddresses(addressId);
    }
  },
  methods: {
    async refreshAddresses(preferredId = "") {
      try {
        this.addresses = await utils_api_user.getAddressList() || [];
        if (preferredId) {
          this.selectedAddressId = preferredId;
        } else if (!this.selectedAddressId) {
          const defaultAddress = this.addresses.find((item) => item.isDefault) || this.addresses[0];
          this.selectedAddressId = defaultAddress ? defaultAddress.id : "";
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/cart/checkout.vue:152", "refresh addresses failed", error);
      }
    },
    async loadData() {
      var _a;
      this.loading = true;
      this.loadError = false;
      try {
        const [addresses, cartItems] = await Promise.all([
          utils_api_user.getAddressList(),
          this.cartItemIds.length ? utils_api_cart.getCartItems() : Promise.resolve([])
        ]);
        this.addresses = addresses || [];
        const defaultAddress = this.addresses.find((item) => item.isDefault) || this.addresses[0];
        this.selectedAddressId = defaultAddress ? defaultAddress.id : "";
        if (this.cartItemIds.length) {
          const idSet = new Set(this.cartItemIds.map(String));
          this.items = (cartItems || []).filter((item) => idSet.has(String(item.id)) && item.available);
        } else if (this.directBookId) {
          const book = await utils_api_book.getBookDetail(this.directBookId);
          this.items = [{
            bookId: book.id,
            bookTitle: book.title,
            bookCover: Array.isArray(book.imageList) ? book.imageList[0] : book.cover,
            sellerName: book.sellerName || ((_a = book.seller) == null ? void 0 : _a.name) || "书友",
            price: book.price
          }];
        } else {
          this.items = [];
        }
      } catch (error) {
        this.loadError = true;
        common_vendor.index.__f__("error", "at pages/cart/checkout.vue:183", "load checkout failed", error);
      } finally {
        this.loading = false;
      }
    },
    openAddressPicker() {
      common_vendor.index.navigateTo({
        url: `/pages/my/address?select=checkout&selectedId=${encodeURIComponent(this.selectedAddressId || "")}`
      });
    },
    async submitCheckout() {
      if (this.submitting)
        return;
      if (!this.items.length) {
        common_vendor.index.showToast({ title: this.texts.empty, icon: "none" });
        return;
      }
      const address = this.selectedAddress;
      if (!address) {
        common_vendor.index.showToast({ title: this.texts.needAddress, icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        const result = await utils_api_order.checkoutOrder({
          addressId: address.id,
          buyerMessage: this.buyerMessage,
          items: this.items.map((item) => ({
            bookId: item.bookId,
            cartItemId: item.cartItemId || item.id
          }))
        });
        common_vendor.index.showModal({
          title: this.texts.successTitle,
          content: this.texts.successText,
          confirmText: this.texts.viewOrders,
          success: ({ confirm }) => {
            if (confirm) {
              common_vendor.index.navigateTo({ url: "/pages/my/orders?status=1&role=buyer" });
            } else {
              common_vendor.index.navigateBack();
            }
          }
        });
        return result;
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/cart/checkout.vue:228", "checkoutOrder failed", error);
      } finally {
        this.submitting = false;
      }
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
    c: common_vendor.t($data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: $data.loading
  }, $data.loading ? {
    h: common_vendor.t($data.texts.loading)
  } : $data.loadError ? {
    j: common_vendor.t($data.texts.loadError),
    k: common_vendor.t($data.texts.retry),
    l: common_vendor.o((...args) => $options.loadData && $options.loadData(...args))
  } : common_vendor.e({
    m: common_vendor.t($data.texts.address),
    n: $options.selectedAddress
  }, $options.selectedAddress ? {
    o: common_vendor.t($options.selectedAddress.receiverName),
    p: common_vendor.t($options.selectedAddress.receiverPhone),
    q: common_vendor.t($options.selectedAddress.fullAddress)
  } : {
    r: common_vendor.t($data.texts.noAddress)
  }, {
    s: common_vendor.o((...args) => $options.openAddressPicker && $options.openAddressPicker(...args)),
    t: common_vendor.t($data.texts.books),
    v: common_vendor.f($data.items, (item, k0, i0) => {
      return {
        a: item.bookCover || "/static/logo.png",
        b: common_vendor.t(item.bookTitle),
        c: common_vendor.t(item.sellerName),
        d: common_vendor.t(item.price),
        e: item.bookId
      };
    }),
    w: common_vendor.t($data.texts.seller),
    x: common_vendor.t($data.texts.currency),
    y: common_vendor.t($data.texts.message),
    z: $data.texts.messagePlaceholder,
    A: $data.buyerMessage,
    B: common_vendor.o(($event) => $data.buyerMessage = $event.detail.value)
  }), {
    i: $data.loadError,
    C: common_vendor.t($data.items.length),
    D: common_vendor.t($data.texts.selectedUnit),
    E: common_vendor.t($data.texts.currency),
    F: common_vendor.t($options.totalAmount),
    G: common_vendor.t($data.submitting ? $data.texts.submitting : $data.texts.submit),
    H: $data.submitting || !$data.items.length ? 1 : "",
    I: common_vendor.o((...args) => $options.submitCheckout && $options.submitCheckout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0d7381c5"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/cart/checkout.js.map
