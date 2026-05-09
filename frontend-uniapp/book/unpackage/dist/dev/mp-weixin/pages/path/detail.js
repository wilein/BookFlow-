"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_favorite = require("../../utils/api/favorite.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
function toBooleanFlag(value, fallback = false) {
  if (value === void 0 || value === null || value === "")
    return fallback;
  const normalized = String(value).toLowerCase();
  return normalized === "1" || normalized === "true" || normalized === "yes";
}
function normalizeNumericId(value) {
  const text = String(value || "").trim();
  if (!text)
    return "";
  return /^\d+$/.test(text) ? text : "";
}
function normalizeNode(node, index) {
  const resources = Array.isArray(node.resources) ? node.resources : [];
  return {
    id: node.id || `node-${index + 1}`,
    title: node.title || `Node ${index + 1}`,
    description: node.description || "",
    duration: node.duration || node.estimatedDuration || "1h",
    completed: Boolean(node.completed),
    resourceCount: Number(node.resourceCount || resources.length || 0),
    resourceIds: Array.isArray(node.resourceIds) ? node.resourceIds : [],
    resources,
    learningGoal: node.learningGoal || "",
    learningMethod: node.learningMethod || "",
    deliverable: node.deliverable || "",
    learningSteps: Array.isArray(node.learningSteps) ? node.learningSteps : []
  };
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      validPathId: "",
      isFavorite: false,
      isCreator: false,
      started: false,
      progressPercent: 0,
      completedCount: 0,
      pathInfo: {
        id: "",
        title: "",
        creator: "",
        difficulty: "",
        totalDuration: "",
        description: ""
      },
      nodes: [],
      texts: {
        share: "分享",
        favorite: "收藏",
        favoritedShort: "已藏",
        creator: "创建者：",
        duration: "预估总时长：",
        progress: "学习进度",
        nodeDone: "节点完成",
        nodes: "路径节点",
        doneMark: "✓",
        checkMark: "",
        done: "已完成",
        todo: "未完成",
        viewResourcePrefix: "查看资源（",
        viewResourceSuffix: "）",
        nodeDetailHint: "进入详情",
        markDone: "标记完成",
        undoDone: "取消完成",
        editPath: "编辑路径",
        startLearning: "开始学习",
        continueLearning: "继续学习",
        cancelLearning: "取消学习",
        startLearningToast: "已开始学习",
        cancelLearningToast: "已取消学习",
        cancelConfirmTitle: "取消学习",
        cancelConfirmContent: "取消后，该路径将从我的路径移除，节点进度也会清空。",
        shareDev: "分享功能开发中",
        unavailable: "当前路径暂未接入详情数据",
        coverAudit: "封面审核："
      }
    };
  },
  computed: {
    totalNodeCount() {
      return this.nodes.length;
    }
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
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.pathInfo.id = decodeURIComponent(options.pathId || "");
    this.validPathId = normalizeNumericId(this.pathInfo.id);
    this.pathInfo.title = decodeURIComponent(options.title || "");
    this.pathInfo.creator = decodeURIComponent(options.creator || "");
    this.pathInfo.difficulty = decodeURIComponent(options.difficulty || "");
    this.pathInfo.totalDuration = decodeURIComponent(options.totalDuration || "");
    this.pathInfo.description = decodeURIComponent(options.description || "");
    this.isCreator = toBooleanFlag(options.isCreator, false);
    this.recordHistory(options);
    if (this.validPathId) {
      this.fetchPathDetail();
      this.fetchFavoriteState();
    }
  },
  methods: {
    buildRouteUrl(options = {}) {
      const id = encodeURIComponent(this.validPathId || options.pathId || "");
      return id ? `/pages/path/detail?pathId=${id}` : "/pages/path/detail";
    },
    async recordHistory(options) {
      if (!this.validPathId || !utils_auth.hasValidSession())
        return;
      try {
        await utils_api_user.recordBrowseHistory({
          targetType: "path",
          targetId: this.validPathId,
          title: this.pathInfo.title,
          subTitle: this.pathInfo.creator,
          coverUrl: this.pathInfo.coverImage || "/static/logo.png",
          routeUrl: this.buildRouteUrl(options)
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:245", "record path history failed", error);
      }
    },
    async fetchFavoriteState() {
      if (!utils_auth.hasValidSession() || !this.validPathId)
        return;
      try {
        const data = await utils_api_favorite.getFavoriteStatus("path", this.validPathId);
        this.isFavorite = Boolean(data == null ? void 0 : data.favorited);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:254", "getFavoriteStatus failed", error);
      }
    },
    async fetchPathDetail() {
      try {
        const data = await utils_api_path.getPathDetail(this.validPathId);
        if (!data || !data.id)
          return;
        this.pathInfo = {
          id: data.id,
          title: data.title || this.pathInfo.title,
          creator: data.creator || this.pathInfo.creator,
          difficulty: data.difficulty || this.pathInfo.difficulty,
          totalDuration: data.totalDuration || this.pathInfo.totalDuration,
          description: data.description || this.pathInfo.description,
          coverImage: data.coverImage || "",
          coverImageStatus: Number(data.coverImageStatus || 0),
          coverImageStatusLabel: data.coverImageStatusLabel || ""
        };
        this.nodes = Array.isArray(data.nodes) && data.nodes.length ? data.nodes.map((node, index) => normalizeNode(node, index)) : [];
        this.isCreator = Boolean(data.isCreator);
        this.started = Boolean(data.started);
        this.completedCount = Number(data.completedCount || 0);
        this.progressPercent = Number(data.progressPercent || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:280", "fetchPathDetail failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    handleShare() {
      common_vendor.index.showToast({ title: this.texts.shareDev, icon: "none" });
    },
    async toggleFavoriteAction() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      if (!this.validPathId) {
        common_vendor.index.showToast({ title: this.texts.unavailable, icon: "none" });
        return;
      }
      try {
        const data = await utils_api_favorite.toggleFavorite("path", this.validPathId);
        this.isFavorite = Boolean(data == null ? void 0 : data.favorited);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:303", "toggleFavorite failed", error);
      }
    },
    viewResources(node) {
      common_vendor.index.navigateTo({
        url: `/pages/resources/list?pathNodeId=${encodeURIComponent(node.id || "")}&title=${encodeURIComponent((node.title || "") + this.texts.nodes)}`
      });
    },
    openNodeDetail(node) {
      if (!node || !node.id || !this.validPathId) {
        common_vendor.index.showToast({ title: this.texts.unavailable, icon: "none" });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/path/node-detail?pathId=${encodeURIComponent(this.validPathId)}&nodeId=${encodeURIComponent(node.id)}`
      });
    },
    async handleMainAction() {
      if (this.isCreator) {
        const params = [`pathId=${encodeURIComponent(this.validPathId || "")}`].join("&");
        common_vendor.index.navigateTo({ url: `/pages/path/create?${params}` });
        return;
      }
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      if (!this.validPathId) {
        common_vendor.index.showToast({ title: this.texts.unavailable, icon: "none" });
        return;
      }
      try {
        const data = await utils_api_path.startPathLearning(this.validPathId);
        this.started = true;
        this.completedCount = Number(data && data.completedCount !== void 0 ? data.completedCount : this.completedCount || 0);
        this.progressPercent = Number(data && data.progressPercent !== void 0 ? data.progressPercent : this.progressPercent || 0);
        common_vendor.index.showToast({ title: this.texts.startLearningToast, icon: "success" });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:338", "startPathLearning failed", error);
      }
    },
    handleCancelLearning() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      if (!this.validPathId) {
        common_vendor.index.showToast({ title: this.texts.unavailable, icon: "none" });
        return;
      }
      common_vendor.index.showModal({
        title: this.texts.cancelConfirmTitle,
        content: this.texts.cancelConfirmContent,
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            const data = await utils_api_path.cancelPathLearning(this.validPathId);
            this.started = Boolean(data && data.started);
            this.completedCount = 0;
            this.progressPercent = 0;
            this.nodes = this.nodes.map((node) => ({ ...node, completed: false }));
            common_vendor.index.showToast({ title: this.texts.cancelLearningToast, icon: "none" });
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/path/detail.vue:360", "cancelPathLearning failed", error);
          }
        }
      });
    },
    async toggleNodeCompleted(node) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      if (!this.validPathId) {
        common_vendor.index.showToast({ title: this.texts.unavailable, icon: "none" });
        return;
      }
      try {
        const nextCompleted = !node.completed;
        const data = await utils_api_path.completePathNode(this.validPathId, node.id, nextCompleted);
        node.completed = nextCompleted;
        this.completedCount = Number((data == null ? void 0 : data.completedCount) || 0);
        this.progressPercent = Number((data == null ? void 0 : data.progressPercent) || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/detail.vue:378", "completePathNode failed", error);
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.pathInfo.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerRightSafe + "px",
    g: $data.headerHeight + "px",
    h: $data.pathInfo.coverImage
  }, $data.pathInfo.coverImage ? {
    i: $data.pathInfo.coverImage
  } : {}, {
    j: common_vendor.t($data.pathInfo.title),
    k: common_vendor.t($data.pathInfo.difficulty),
    l: $data.isCreator && $data.pathInfo.coverImageStatusLabel
  }, $data.isCreator && $data.pathInfo.coverImageStatusLabel ? {
    m: common_vendor.t($data.texts.coverAudit),
    n: common_vendor.t($data.pathInfo.coverImageStatusLabel)
  } : {}, {
    o: common_vendor.t($data.texts.creator),
    p: common_vendor.t($data.pathInfo.creator),
    q: common_vendor.t($data.texts.duration),
    r: common_vendor.t($data.pathInfo.totalDuration),
    s: common_vendor.t($data.pathInfo.description),
    t: common_vendor.t($data.texts.share),
    v: common_vendor.o((...args) => $options.handleShare && $options.handleShare(...args)),
    w: common_vendor.t($data.isFavorite ? $data.texts.favoritedShort : $data.texts.favorite),
    x: $data.isFavorite ? 1 : "",
    y: common_vendor.o((...args) => $options.toggleFavoriteAction && $options.toggleFavoriteAction(...args)),
    z: common_vendor.t($data.texts.progress),
    A: common_vendor.t($data.completedCount),
    B: common_vendor.t($options.totalNodeCount),
    C: common_vendor.t($data.texts.nodeDone),
    D: $data.progressPercent + "%",
    E: common_vendor.t($data.texts.nodes),
    F: common_vendor.f($data.nodes, (node, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(node.completed ? $data.texts.doneMark : index + 1),
        b: node.completed ? 1 : "",
        c: index !== $data.nodes.length - 1
      }, index !== $data.nodes.length - 1 ? {} : {}, {
        d: common_vendor.t(node.title),
        e: common_vendor.t(node.duration)
      }, !$data.isCreator && $data.started ? {
        f: common_vendor.t(node.completed ? $data.texts.doneMark : $data.texts.checkMark),
        g: node.completed ? 1 : "",
        h: common_vendor.o(($event) => $options.toggleNodeCompleted(node), node.id || index)
      } : {}, {
        i: node.description
      }, node.description ? {
        j: common_vendor.t(node.description)
      } : {}, {
        k: common_vendor.t(node.completed ? $data.texts.done : $data.texts.todo),
        l: node.completed ? 1 : "",
        m: node.resourceCount > 0
      }, node.resourceCount > 0 ? {
        n: common_vendor.t($data.texts.viewResourcePrefix),
        o: common_vendor.t(node.resourceCount),
        p: common_vendor.t($data.texts.viewResourceSuffix),
        q: common_vendor.o(($event) => $options.viewResources(node), node.id || index)
      } : {}, {
        r: common_vendor.o(($event) => $options.openNodeDetail(node), node.id || index),
        s: node.completed ? 1 : "",
        t: node.id || index
      });
    }),
    G: !$data.isCreator && $data.started,
    H: common_vendor.t($data.texts.nodeDetailHint),
    I: $data.isCreator
  }, $data.isCreator ? {
    J: common_vendor.t($data.texts.editPath),
    K: common_vendor.o((...args) => $options.handleMainAction && $options.handleMainAction(...args))
  } : $data.started ? {
    M: common_vendor.t($data.texts.continueLearning),
    N: common_vendor.o((...args) => $options.handleMainAction && $options.handleMainAction(...args)),
    O: common_vendor.t($data.texts.cancelLearning),
    P: common_vendor.o((...args) => $options.handleCancelLearning && $options.handleCancelLearning(...args))
  } : {
    Q: common_vendor.t($data.texts.startLearning),
    R: common_vendor.o((...args) => $options.handleMainAction && $options.handleMainAction(...args))
  }, {
    L: $data.started
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-4d011187"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/path/detail.js.map
