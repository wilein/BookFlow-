"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_path = require("../../utils/api/path.js");
const common_assets = require("../../common/assets.js");
function normalizeId(value) {
  const text = String(value == null ? "" : value).trim();
  return /^\d+$/.test(text) ? text : "";
}
function normalizeNode(node = {}, index = 0) {
  const resources = Array.isArray(node.resources) ? node.resources : [];
  return {
    id: node.id || `node-${index + 1}`,
    title: node.title || `节点 ${index + 1}`,
    description: node.description || "",
    duration: node.duration || "时长待补充",
    completed: Boolean(node.completed),
    learningGoal: node.learningGoal || `掌握「${node.title || "当前节点"}」的核心内容，并能独立复述重点。`,
    learningMethod: node.learningMethod || "先阅读节点说明，再结合资源学习，最后通过练习和笔记完成复盘。",
    deliverable: node.deliverable || "完成一页学习笔记，整理关键知识点、练习结果和待解决问题。",
    learningSteps: Array.isArray(node.learningSteps) ? node.learningSteps : [],
    resources
  };
}
function fallbackSteps(node) {
  return [
    { order: 1, title: "读目标", content: "先看节点说明，确认本节点要解决的问题和关键词。" },
    { order: 2, title: "看资源", content: "按顺序学习关联资源，视频负责理解流程，PDF/课件负责补充细节。" },
    { order: 3, title: "做练习", content: `围绕「${node.title || "当前节点"}」完成例题、代码、思维导图或问答练习。` },
    { order: 4, title: "写复盘", content: "把关键结论和易错点写进笔记，确认掌握后回到路径页打勾。" }
  ];
}
const TEXTS = {
  title: "节点详情",
  completed: "已完成",
  learning: "待学习",
  defaultDescription: "本节点还没有补充说明，请结合学习路径目标和关联资源完成学习。",
  goal: "要学什么",
  method: "怎么学",
  deliverable: "完成标准",
  steps: "学习步骤",
  stepsSub: "按顺序完成",
  resources: "关联资源",
  resourcesCount: " 个资源",
  resourceShort: "资",
  resource: "资源",
  resourceDesc: "点击查看资源内容",
  open: "打开",
  emptyResources: "该节点暂未关联资源，可先按节点说明完成学习。",
  filePending: "资源链接待补充",
  copied: "资源链接已复制",
  openFailed: "打开失败，链接已复制"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      pathId: "",
      nodeId: "",
      pathInfo: {},
      node: normalizeNode()
    };
  },
  computed: {
    resources() {
      return Array.isArray(this.node.resources) ? this.node.resources : [];
    },
    learningSteps() {
      return this.node.learningSteps.length ? this.node.learningSteps : fallbackSteps(this.node);
    }
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 54;
    this.pathId = normalizeId(options.pathId);
    this.nodeId = normalizeId(options.nodeId);
    this.fetchNode();
  },
  methods: {
    async fetchNode() {
      if (!this.pathId || !this.nodeId)
        return;
      try {
        const data = await utils_api_path.getPathDetail(this.pathId);
        this.pathInfo = data || {};
        const nodes = Array.isArray(data == null ? void 0 : data.nodes) ? data.nodes : [];
        const index = nodes.findIndex((item) => String(item.id) === String(this.nodeId));
        if (index >= 0) {
          this.node = normalizeNode(nodes[index], index);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/node-detail.vue:180", "fetch node detail failed", error);
      }
    },
    inferFormat(resource) {
      const url = (resource == null ? void 0 : resource.fileUrl) || "";
      const match = url.match(/\.([a-zA-Z0-9]+)(\?|#|$)/);
      return match ? match[1].toUpperCase() : TEXTS.resource;
    },
    isDocumentResource(resource) {
      const text = `${(resource == null ? void 0 : resource.fileFormat) || ""} ${(resource == null ? void 0 : resource.fileUrl) || ""}`.toLowerCase();
      return [".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx"].some((suffix) => text.includes(suffix)) || ["pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"].some((suffix) => text.split(/\s+/).includes(suffix));
    },
    copyResourceUrl(url, title = TEXTS.copied) {
      common_vendor.index.setClipboardData({
        data: url,
        success: () => common_vendor.index.showToast({ title, icon: "none" })
      });
    },
    openResource(resource) {
      if (!(resource == null ? void 0 : resource.id))
        return;
      common_vendor.index.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(resource.id)}` });
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(this.pathId)}` })
      });
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
    g: common_vendor.t($data.node.completed ? $data.texts.completed : $data.texts.learning),
    h: common_vendor.t($data.node.duration),
    i: common_vendor.t($data.node.title),
    j: common_vendor.t($data.node.description || $data.texts.defaultDescription),
    k: $data.node.completed ? 1 : "",
    l: common_vendor.t($data.texts.goal),
    m: common_vendor.t($data.node.learningGoal),
    n: common_vendor.t($data.texts.method),
    o: common_vendor.t($data.node.learningMethod),
    p: common_vendor.t($data.texts.deliverable),
    q: common_vendor.t($data.node.deliverable),
    r: common_vendor.t($data.texts.steps),
    s: common_vendor.t($data.texts.stepsSub),
    t: common_vendor.f($options.learningSteps, (step, index, i0) => {
      return {
        a: common_vendor.t(step.order || index + 1),
        b: common_vendor.t(step.title),
        c: common_vendor.t(step.content),
        d: step.order || index
      };
    }),
    v: common_vendor.t($data.texts.resources),
    w: common_vendor.t($options.resources.length),
    x: common_vendor.t($data.texts.resourcesCount),
    y: $options.resources.length
  }, $options.resources.length ? {
    z: common_vendor.f($options.resources, (resource, k0, i0) => {
      return {
        a: common_vendor.t(resource.typeLabel ? resource.typeLabel.slice(0, 1) : $data.texts.resourceShort),
        b: common_vendor.t(resource.title || resource.name),
        c: common_vendor.t(resource.description || $data.texts.resourceDesc),
        d: common_vendor.t(resource.typeLabel || $data.texts.resource),
        e: common_vendor.t(resource.fileFormat || $options.inferFormat(resource)),
        f: resource.id,
        g: common_vendor.o(($event) => $options.openResource(resource), resource.id)
      };
    }),
    A: common_vendor.t($data.texts.open)
  } : {
    B: common_vendor.t($data.texts.emptyResources)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-7d30b115"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/path/node-detail.js.map
