"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_cart = require("../../utils/api/cart.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      loading: false,
      loadError: false,
      items: [],
      selectedMap: {},
      texts: {
        title: "购物车",
        loading: "加载中...",
        loadError: "购物车加载失败",
        retry: "重试",
        empty: "购物车暂无书籍",
        seller: "卖家：",
        invalid: "书籍不可购买",
        remove: "移除",
        selectAll: "全选",
        selectedUnit: " 件",
        checkout: "去结算",
        currency: "￥",
        removeSuccess: "已移除"
      }
    };
  },
  computed: {
    availableItems() {
      return this.items.filter((item) => item.available);
    },
    selectedItems() {
      return this.availableItems.filter((item) => this.selectedMap[item.id]);
    },
    selectedCount() {
      return this.selectedItems.length;
    },
    totalAmount() {
      return this.selectedItems.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2);
    },
    allSelected() {
      return this.availableItems.length > 0 && this.availableItems.every((item) => this.selectedMap[item.id]);
    }
  },
  onLoad() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.fetchCart();
  },
  onShow() {
    if (this.headerHeight)
      this.fetchCart();
  },
  methods: {
    async fetchCart() {
      this.loading = true;
      this.loadError = false;
      try {
        this.items = await utils_api_cart.getCartItems() || [];
        const nextSelected = {};
        this.items.forEach((item) => {
          if (item.available && this.selectedMap[item.id]) {
            nextSelected[item.id] = true;
          }
        });
        this.selectedMap = nextSelected;
      } catch (error) {
        this.loadError = true;
        common_vendor.index.__f__("error", "at pages/cart/cart.vue:125", "getCartItems failed", error);
      } finally {
        this.loading = false;
      }
    },
    toggleItem(item) {
      if (!item.available)
        return;
      this.selectedMap = { ...this.selectedMap, [item.id]: !this.selectedMap[item.id] };
    },
    toggleAll() {
      if (this.allSelected) {
        this.selectedMap = {};
        return;
      }
      const selected = {};
      this.availableItems.forEach((item) => {
        selected[item.id] = true;
      });
      this.selectedMap = selected;
    },
    async removeOne(item) {
      try {
        await utils_api_cart.removeCartItems([item.id]);
        common_vendor.index.showToast({ title: this.texts.removeSuccess, icon: "success" });
        await this.fetchCart();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/cart/cart.vue:151", "removeCartItems failed", error);
      }
    },
    goCheckout() {
      if (!this.selectedCount)
        return;
      const ids = this.selectedItems.map((item) => item.id).join(",");
      common_vendor.index.navigateTo({ url: `/pages/cart/checkout?cartItemIds=${encodeURIComponent(ids)}` });
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
    g: $data.loading
  }, $data.loading ? {
    h: common_vendor.t($data.texts.loading)
  } : $data.loadError ? {
    j: common_vendor.t($data.texts.loadError),
    k: common_vendor.t($data.texts.retry),
    l: common_vendor.o((...args) => $options.fetchCart && $options.fetchCart(...args))
  } : $data.items.length ? {
    n: common_vendor.f($data.items, (item, k0, i0) => {
      return common_vendor.e({
        a: $data.selectedMap[item.id]
      }, $data.selectedMap[item.id] ? {} : {}, {
        b: $data.selectedMap[item.id] ? 1 : "",
        c: !item.available ? 1 : "",
        d: common_vendor.o(($event) => $options.toggleItem(item), item.id),
        e: item.bookCover || "/static/logo.png",
        f: common_vendor.t(item.bookTitle),
        g: common_vendor.t(item.sellerName),
        h: !item.available
      }, !item.available ? {
        i: common_vendor.t(item.invalidReason || $data.texts.invalid)
      } : {}, {
        j: common_vendor.t(item.price),
        k: common_vendor.o(($event) => $options.removeOne(item), item.id),
        l: item.id,
        m: !item.available ? 1 : ""
      });
    }),
    o: common_vendor.t($data.texts.seller),
    p: common_vendor.t($data.texts.currency),
    q: common_vendor.t($data.texts.remove)
  } : {
    r: common_vendor.t($data.texts.empty)
  }, {
    i: $data.loadError,
    m: $data.items.length,
    s: $options.allSelected
  }, $options.allSelected ? {} : {}, {
    t: $options.allSelected ? 1 : "",
    v: common_vendor.t($data.texts.selectAll),
    w: common_vendor.o((...args) => $options.toggleAll && $options.toggleAll(...args)),
    x: common_vendor.t($options.selectedCount),
    y: common_vendor.t($data.texts.selectedUnit),
    z: common_vendor.t($data.texts.currency),
    A: common_vendor.t($options.totalAmount),
    B: common_vendor.t($data.texts.checkout),
    C: !$options.selectedCount ? 1 : "",
    D: common_vendor.o((...args) => $options.goCheckout && $options.goCheckout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-c91e7611"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/cart/cart.js.map
