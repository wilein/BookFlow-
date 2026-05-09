"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
function decodeValue(value) {
  if (!value)
    return "";
  try {
    return decodeURIComponent(value);
  } catch (error) {
    return value;
  }
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      submitting: false,
      texts: {
        editTitle: "编辑地址",
        createTitle: "新增地址",
        receiver: "收货人",
        receiverPlaceholder: "请输入收货人姓名",
        phone: "联系电话",
        phonePlaceholder: "请输入联系电话",
        province: "省份",
        provincePlaceholder: "请输入省份",
        city: "城市",
        cityPlaceholder: "请输入城市",
        district: "区县",
        districtPlaceholder: "请输入区县",
        detail: "详细地址",
        detailPlaceholder: "请输入详细地址",
        defaultText: "设为默认地址",
        saving: "保存中...",
        save: "保存地址",
        saveSuccess: "保存成功",
        errorReceiver: "请输入收货人姓名",
        errorPhone: "请输入联系电话",
        errorRegion: "请完整填写省市区",
        errorDetail: "请输入详细地址"
      },
      form: {
        id: "",
        receiverName: "",
        receiverPhone: "",
        province: "",
        city: "",
        district: "",
        detailAddress: "",
        isDefault: false
      }
    };
  },
  onLoad(options) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.form = {
      id: decodeValue(options.id || ""),
      receiverName: decodeValue(options.receiverName || ""),
      receiverPhone: decodeValue(options.receiverPhone || ""),
      province: decodeValue(options.province || ""),
      city: decodeValue(options.city || ""),
      district: decodeValue(options.district || ""),
      detailAddress: decodeValue(options.detailAddress || ""),
      isDefault: String(options.isDefault || "") === "1"
    };
  },
  methods: {
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.navigateTo({ url: "/pages/my/address" }) });
    },
    toggleDefault() {
      this.form.isDefault = !this.form.isDefault;
    },
    async submit() {
      if (this.submitting)
        return;
      if (!this.form.receiverName.trim()) {
        common_vendor.index.showToast({ title: this.texts.errorReceiver, icon: "none" });
        return;
      }
      if (!this.form.receiverPhone.trim()) {
        common_vendor.index.showToast({ title: this.texts.errorPhone, icon: "none" });
        return;
      }
      if (!this.form.province.trim() || !this.form.city.trim() || !this.form.district.trim()) {
        common_vendor.index.showToast({ title: this.texts.errorRegion, icon: "none" });
        return;
      }
      if (!this.form.detailAddress.trim()) {
        common_vendor.index.showToast({ title: this.texts.errorDetail, icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_api_user.saveAddress({
          id: this.form.id || void 0,
          receiverName: this.form.receiverName,
          receiverPhone: this.form.receiverPhone,
          province: this.form.province,
          city: this.form.city,
          district: this.form.district,
          detailAddress: this.form.detailAddress,
          isDefault: this.form.isDefault
        });
        common_vendor.index.showToast({ title: this.texts.saveSuccess, icon: "success" });
        setTimeout(() => this.goBack(), 400);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/address-edit.vue:164", "saveAddress failed", error);
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.form.id ? $data.texts.editTitle : $data.texts.createTitle),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.t($data.texts.receiver),
    h: $data.texts.receiverPlaceholder,
    i: $data.form.receiverName,
    j: common_vendor.o(($event) => $data.form.receiverName = $event.detail.value),
    k: common_vendor.t($data.texts.phone),
    l: $data.texts.phonePlaceholder,
    m: $data.form.receiverPhone,
    n: common_vendor.o(($event) => $data.form.receiverPhone = $event.detail.value),
    o: common_vendor.t($data.texts.province),
    p: $data.texts.provincePlaceholder,
    q: $data.form.province,
    r: common_vendor.o(($event) => $data.form.province = $event.detail.value),
    s: common_vendor.t($data.texts.city),
    t: $data.texts.cityPlaceholder,
    v: $data.form.city,
    w: common_vendor.o(($event) => $data.form.city = $event.detail.value),
    x: common_vendor.t($data.texts.district),
    y: $data.texts.districtPlaceholder,
    z: $data.form.district,
    A: common_vendor.o(($event) => $data.form.district = $event.detail.value),
    B: common_vendor.t($data.texts.detail),
    C: $data.texts.detailPlaceholder,
    D: $data.form.detailAddress,
    E: common_vendor.o(($event) => $data.form.detailAddress = $event.detail.value),
    F: $data.form.isDefault ? 1 : "",
    G: common_vendor.t($data.texts.defaultText),
    H: common_vendor.o((...args) => $options.toggleDefault && $options.toggleDefault(...args)),
    I: common_vendor.t($data.submitting ? $data.texts.saving : $data.texts.save),
    J: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-7272ca84"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/address-edit.js.map
