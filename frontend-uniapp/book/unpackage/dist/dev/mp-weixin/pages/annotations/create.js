"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_annotation = require("../../utils/api/annotation.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      bookId: "",
      bookTitle: "",
      form: {
        type: "highlight",
        page: "1",
        positionText: "",
        content: "",
        imageUrl: ""
      },
      typeOptions: [
        { value: "highlight", label: "重点", icon: "H" },
        { value: "question", label: "疑问", icon: "Q" },
        { value: "insight", label: "心得", icon: "I" }
      ]
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.bookId = decodeURIComponent(options.bookId || "");
    this.bookTitle = decodeURIComponent(options.bookTitle || "");
    this.form.page = decodeURIComponent(options.page || "1") || "1";
    utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl());
  },
  methods: {
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/index/index" }) });
    },
    chooseImage() {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        success: async (res) => {
          const filePath = res.tempFilePaths && res.tempFilePaths[0];
          if (!filePath)
            return;
          common_vendor.index.showLoading({ title: "上传中..." });
          try {
            const data = await utils_api_annotation.uploadAnnotationImage(filePath);
            this.form.imageUrl = data.url || "";
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/annotations/create.vue:124", "uploadAnnotationImage failed", error);
          } finally {
            common_vendor.index.hideLoading();
          }
        }
      });
    },
    async submit() {
      if (!this.bookId) {
        common_vendor.index.showToast({ title: "书籍参数缺失", icon: "none" });
        return;
      }
      if (!String(this.form.content || "").trim()) {
        common_vendor.index.showToast({ title: "请输入批注内容", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "发布中..." });
      try {
        await utils_api_annotation.createAnnotation({
          bookId: this.bookId,
          page: this.form.page,
          type: this.form.type,
          content: this.form.content.trim(),
          positionText: this.form.positionText.trim(),
          imageUrl: this.form.imageUrl
        });
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "批注已发布", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 500);
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at pages/annotations/create.vue:155", "createAnnotation failed", error);
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: $data.headerHeight + "px",
    d: $data.statusBarHeight + "px",
    e: $data.headerRightSafe + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.t($data.bookTitle || "书籍批注"),
    h: common_vendor.f($data.typeOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.label),
        c: item.value,
        d: $data.form.type === item.value ? 1 : "",
        e: common_vendor.o(($event) => $data.form.type = item.value, item.value)
      };
    }),
    i: $data.form.page,
    j: common_vendor.o(($event) => $data.form.page = $event.detail.value),
    k: $data.form.positionText,
    l: common_vendor.o(($event) => $data.form.positionText = $event.detail.value),
    m: $data.form.content,
    n: common_vendor.o(($event) => $data.form.content = $event.detail.value),
    o: $data.form.imageUrl
  }, $data.form.imageUrl ? {
    p: $data.form.imageUrl
  } : {}, {
    q: common_vendor.o((...args) => $options.chooseImage && $options.chooseImage(...args)),
    r: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-95ab360d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/annotations/create.js.map
