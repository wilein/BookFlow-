"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_community = require("../../utils/api/community.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  title: "发布动态",
  publish: "发布",
  type: "类型",
  recommend: "推荐",
  review: "书评",
  qa: "问答",
  path: "路径",
  postTitle: "标题",
  titlePlaceholder: "请输入标题",
  content: "内容",
  contentPlaceholder: "分享你的学习心得、书评、问题或路径整理",
  sharedPath: "关联学习路径（可选）",
  emptyPath: "暂无可分享的自建路径",
  nodeUnit: "个节点",
  needTitle: "请填写标题",
  needContent: "请填写内容",
  success: "发布成功"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      typeOptions: [
        { label: TEXTS.recommend, value: "recommend" },
        { label: TEXTS.review, value: "review" },
        { label: TEXTS.qa, value: "qa" },
        { label: TEXTS.path, value: "path" }
      ],
      form: {
        type: "recommend",
        title: "",
        content: "",
        sharedPathId: ""
      },
      pathOptions: []
    };
  },
  onLoad() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.fetchMyPaths();
  },
  methods: {
    async fetchMyPaths() {
      try {
        this.pathOptions = await utils_api_user.getMyPaths() || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/create.vue:119", "getMyPaths failed", error);
      }
    },
    async submitPost() {
      if (!this.form.title.trim()) {
        common_vendor.index.showToast({ title: TEXTS.needTitle, icon: "none" });
        return;
      }
      if (!this.form.content.trim()) {
        common_vendor.index.showToast({ title: TEXTS.needContent, icon: "none" });
        return;
      }
      try {
        await utils_api_community.createCommunityPost({
          ...this.form,
          sharedPathId: this.form.type === "path" ? this.form.sharedPathId : ""
        });
        common_vendor.index.showToast({ title: TEXTS.success, icon: "success" });
        setTimeout(() => {
          common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/community/community" }) });
        }, 400);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/create.vue:141", "createCommunityPost failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/community/community" }) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: common_vendor.t($data.texts.publish),
    e: common_vendor.o((...args) => $options.submitPost && $options.submitPost(...args)),
    f: $data.headerHeight + "px",
    g: $data.statusBarHeight + "px",
    h: $data.headerHeight + "px",
    i: common_vendor.t($data.texts.type),
    j: common_vendor.f($data.typeOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.form.type === item.value ? 1 : "",
        d: common_vendor.o(($event) => $data.form.type = item.value, item.value)
      };
    }),
    k: $data.form.type === "path"
  }, $data.form.type === "path" ? common_vendor.e({
    l: common_vendor.t($data.texts.sharedPath),
    m: $data.pathOptions.length
  }, $data.pathOptions.length ? {
    n: common_vendor.f($data.pathOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.title),
        b: common_vendor.t(item.difficulty),
        c: common_vendor.t(item.totalDuration),
        d: common_vendor.t(item.nodeCount || 0),
        e: item.id,
        f: $data.form.sharedPathId === item.id ? 1 : "",
        g: common_vendor.o(($event) => $data.form.sharedPathId = $data.form.sharedPathId === item.id ? "" : item.id, item.id)
      };
    }),
    o: common_vendor.t($data.texts.nodeUnit)
  } : {
    p: common_vendor.t($data.texts.emptyPath)
  }) : {}, {
    q: common_vendor.t($data.texts.postTitle),
    r: $data.texts.titlePlaceholder,
    s: $data.form.title,
    t: common_vendor.o(($event) => $data.form.title = $event.detail.value),
    v: common_vendor.t($data.texts.content),
    w: $data.texts.contentPlaceholder,
    x: $data.form.content,
    y: common_vendor.o(($event) => $data.form.content = $event.detail.value)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-a2e5626f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/community/create.js.map
