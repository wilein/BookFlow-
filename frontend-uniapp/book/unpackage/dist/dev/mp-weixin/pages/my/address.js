"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const SELECTED_ADDRESS_KEY = "BOOKFLOW_CHECKOUT_SELECTED_ADDRESS_ID";
function buildQuery(item) {
  return [
    `id=${encodeURIComponent(item.id || "")}`,
    `receiverName=${encodeURIComponent(item.receiverName || "")}`,
    `receiverPhone=${encodeURIComponent(item.receiverPhone || "")}`,
    `province=${encodeURIComponent(item.province || "")}`,
    `city=${encodeURIComponent(item.city || "")}`,
    `district=${encodeURIComponent(item.district || "")}`,
    `detailAddress=${encodeURIComponent(item.detailAddress || "")}`,
    `isDefault=${encodeURIComponent(item.isDefault ? 1 : 0)}`
  ].join("&");
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      selectMode: false,
      selectedId: "",
      items: [],
      texts: {
        title: "收货地址",
        selectTitle: "选择收货地址",
        select: "选择",
        defaultTag: "默认",
        edit: "编辑",
        setDefault: "设为默认",
        delete: "删除",
        empty: "暂无收货地址",
        add: "新增地址",
        deleteTitle: "删除地址",
        deleteText: "确认删除该收货地址吗？",
        deleteSuccess: "删除成功",
        defaultSuccess: "已设为默认地址"
      }
    };
  },
  onLoad(options = {}) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    this.selectMode = options.select === "checkout";
    this.selectedId = decodeURIComponent(options.selectedId || "");
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.items = await utils_api_user.getAddressList() || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/address.vue:107", "getAddressList failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    createAddress() {
      common_vendor.index.navigateTo({ url: "/pages/my/address-edit" });
    },
    editItem(item) {
      common_vendor.index.navigateTo({ url: `/pages/my/address-edit?${buildQuery(item)}` });
    },
    selectItem(item) {
      if (!this.selectMode || !item || !item.id)
        return;
      common_vendor.index.setStorageSync(SELECTED_ADDRESS_KEY, item.id);
      common_vendor.index.navigateBack();
    },
    async setDefault(item) {
      try {
        await utils_api_user.setDefaultAddress(item.id);
        common_vendor.index.showToast({ title: this.texts.defaultSuccess, icon: "success" });
        this.fetchData();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/address.vue:130", "setDefaultAddress failed", error);
      }
    },
    removeItem(item) {
      common_vendor.index.showModal({
        title: this.texts.deleteTitle,
        content: this.texts.deleteText,
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            await utils_api_user.deleteAddress(item.id);
            common_vendor.index.showToast({ title: this.texts.deleteSuccess, icon: "success" });
            this.fetchData();
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/my/address.vue:144", "deleteAddress failed", error);
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
    c: common_vendor.t($data.selectMode ? $data.texts.selectTitle : $data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: $data.items.length
  }, $data.items.length ? {
    h: common_vendor.f($data.items, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.receiverName),
        b: common_vendor.t(item.receiverPhone),
        c: item.isDefault
      }, item.isDefault ? {
        d: common_vendor.t($data.texts.defaultTag)
      } : {}, {
        e: common_vendor.t(item.fullAddress)
      }, $data.selectMode ? {
        f: common_vendor.t($data.texts.select),
        g: common_vendor.o(($event) => $options.selectItem(item), item.id)
      } : {}, {
        h: common_vendor.o(($event) => $options.editItem(item), item.id),
        i: !item.isDefault
      }, !item.isDefault ? {
        j: common_vendor.t($data.texts.setDefault),
        k: common_vendor.o(($event) => $options.setDefault(item), item.id)
      } : {}, {
        l: common_vendor.o(($event) => $options.removeItem(item), item.id),
        m: item.id,
        n: $data.selectMode && String(item.id) === String($data.selectedId) ? 1 : "",
        o: common_vendor.o(($event) => $options.selectItem(item), item.id)
      });
    }),
    i: $data.selectMode,
    j: common_vendor.t($data.texts.edit),
    k: common_vendor.t($data.texts.delete)
  } : {
    l: common_vendor.t($data.texts.empty)
  }, {
    m: common_vendor.t($data.texts.add),
    n: common_vendor.o((...args) => $options.createAddress && $options.createAddress(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-ea533e64"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/address.js.map
