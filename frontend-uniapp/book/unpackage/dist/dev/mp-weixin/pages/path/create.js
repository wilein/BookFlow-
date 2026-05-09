"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_api_resource = require("../../utils/api/resource.js");
const common_assets = require("../../common/assets.js");
function buildDefaultSteps() {
  return [
    { id: `step-${Date.now()}-1`, title: "明确目标", content: "先阅读节点说明，确认本节点要解决的问题和关键词。" },
    { id: `step-${Date.now()}-2`, title: "学习资料", content: "按顺序学习关联资源，边学边记录不理解的地方。" },
    { id: `step-${Date.now()}-3`, title: "练习复盘", content: "完成练习或输出笔记，确认达到完成标准后再打勾。" }
  ];
}
function buildDefaultNode(index = 0) {
  return {
    id: `node-${Date.now()}-${index}`,
    title: "",
    description: "",
    duration: "",
    level: 1,
    learningGoal: "",
    learningMethod: "",
    deliverable: "",
    resourceIds: [],
    learningSteps: buildDefaultSteps()
  };
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      isEditMode: false,
      difficultyOptions: ["入门", "中级", "进阶"],
      levelOptions: [
        { label: "一级", value: 1 },
        { label: "二级", value: 2 },
        { label: "三级", value: 3 }
      ],
      resourceOptions: [
        { id: 1, name: "章节导图" },
        { id: 2, name: "练习题集" },
        { id: 3, name: "知识卡片" },
        { id: 4, name: "课程视频" }
      ],
      form: {
        id: "",
        title: "",
        description: "",
        difficulty: "入门",
        totalDuration: "",
        cover: "",
        coverImageStatus: 0,
        coverImageStatusLabel: "",
        nodes: [buildDefaultNode(1)]
      }
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
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.form.id = decodeURIComponent(options.pathId || "");
    this.isEditMode = Boolean(this.form.id);
    if (this.isEditMode) {
      this.loadPathDetail(this.form.id);
    }
    this.fetchMyResources();
  },
  methods: {
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    chooseCover() {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        success: (res) => {
          const file = (res.tempFilePaths || [])[0];
          if (file) {
            this.form.cover = file;
            this.form.coverImageStatus = 1;
            this.form.coverImageStatusLabel = "待上传";
          }
        }
      });
    },
    isLocalFile(url) {
      if (!url)
        return false;
      return !/^https?:\/\//i.test(url);
    },
    async ensureCoverUploaded() {
      if (!this.form.cover || !this.isLocalFile(this.form.cover)) {
        return;
      }
      common_vendor.index.showLoading({ title: "上传封面" });
      try {
        const data = await utils_api_path.uploadPathCover(this.form.cover);
        this.form.cover = data.url || this.form.cover;
        this.form.coverImageStatus = data.auditStatus || 1;
        this.form.coverImageStatusLabel = data.auditStatusLabel || "待审核";
      } finally {
        common_vendor.index.hideLoading();
      }
    },
    addNode() {
      this.form.nodes.push(buildDefaultNode(this.form.nodes.length + 1));
    },
    removeNode(index) {
      this.form.nodes.splice(index, 1);
      if (this.form.nodes.length === 0) {
        this.form.nodes.push(buildDefaultNode(1));
      }
    },
    moveNodeUp(index) {
      if (index <= 0)
        return;
      const list = [...this.form.nodes];
      [list[index - 1], list[index]] = [list[index], list[index - 1]];
      this.form.nodes = list;
    },
    moveNodeDown(index) {
      if (index >= this.form.nodes.length - 1)
        return;
      const list = [...this.form.nodes];
      [list[index], list[index + 1]] = [list[index + 1], list[index]];
      this.form.nodes = list;
    },
    toggleResource(node, resourceId) {
      const idx = node.resourceIds.indexOf(resourceId);
      if (idx >= 0) {
        node.resourceIds.splice(idx, 1);
      } else {
        node.resourceIds.push(resourceId);
      }
    },
    addStep(node) {
      node.learningSteps.push({
        id: `step-${Date.now()}-${node.learningSteps.length + 1}`,
        title: "",
        content: ""
      });
    },
    removeStep(node, index) {
      node.learningSteps.splice(index, 1);
      if (node.learningSteps.length === 0) {
        node.learningSteps.push({ id: `step-${Date.now()}-1`, title: "", content: "" });
      }
    },
    validateForm() {
      if (!this.form.title.trim()) {
        common_vendor.index.showToast({ title: "请填写路径标题", icon: "none" });
        return false;
      }
      if (!this.form.totalDuration.trim()) {
        common_vendor.index.showToast({ title: "请填写预估总时长", icon: "none" });
        return false;
      }
      if (this.form.nodes.some((node) => !node.title.trim())) {
        common_vendor.index.showToast({ title: "请完善节点标题", icon: "none" });
        return false;
      }
      if (this.form.nodes.some((node) => !node.learningGoal.trim() || !node.learningMethod.trim())) {
        common_vendor.index.showToast({ title: "请完善节点学习目标和学习方法", icon: "none" });
        return false;
      }
      return true;
    },
    buildPayload() {
      return {
        id: this.form.id || void 0,
        title: this.form.title,
        description: this.form.description,
        difficulty: this.form.difficulty,
        totalDuration: this.form.totalDuration,
        cover: this.form.cover,
        nodes: this.form.nodes.map((node) => ({
          id: /^\d+$/.test(String(node.id || "")) ? node.id : void 0,
          title: node.title,
          description: node.description,
          duration: node.duration,
          level: node.level,
          learningGoal: node.learningGoal,
          learningMethod: node.learningMethod,
          deliverable: node.deliverable,
          resourceIds: node.resourceIds,
          learningSteps: node.learningSteps.map((step) => ({ title: step.title.trim(), content: step.content.trim() })).filter((step) => step.title || step.content)
        }))
      };
    },
    async saveDraft() {
      if (!this.form.title.trim()) {
        common_vendor.index.showToast({ title: "请先填写标题再保存", icon: "none" });
        return;
      }
      try {
        await this.ensureCoverUploaded();
        const data = await utils_api_path.savePathDraft(this.buildPayload());
        if (data && data.pathId) {
          this.form.id = data.pathId;
          this.isEditMode = true;
        }
        common_vendor.index.showToast({ title: "草稿已保存", icon: "none" });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/create.vue:411", "saveDraft failed", error);
      }
    },
    publishPath() {
      if (!this.validateForm())
        return;
      common_vendor.index.showModal({
        title: "发布确认",
        content: "发布后将进入审核流程，确认继续？",
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            await this.ensureCoverUploaded();
            const data = await utils_api_path.publishPath(this.buildPayload());
            if (data && data.pathId) {
              this.form.id = data.pathId;
              this.isEditMode = true;
            }
            common_vendor.index.showToast({ title: "已提交审核", icon: "none" });
            setTimeout(() => {
              common_vendor.index.redirectTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(this.form.id)}` });
            }, 450);
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/path/create.vue:433", "publishPath failed", error);
          }
        }
      });
    },
    async fetchMyResources() {
      try {
        const data = await utils_api_resource.getMyResources();
        if (Array.isArray(data) && data.length) {
          this.resourceOptions = data.map((item) => ({
            id: item.id,
            name: item.name || item.title || "未命名资源"
          }));
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/create.vue:448", "fetchMyResources failed", error);
      }
    },
    async loadPathDetail(pathId) {
      if (!pathId)
        return;
      try {
        const data = await utils_api_path.getPathDetail(pathId);
        if (!data || !data.id)
          return;
        this.form = {
          ...this.form,
          id: data.id,
          title: data.title || "",
          description: data.description || "",
          difficulty: data.difficulty || "入门",
          totalDuration: data.totalDuration || "",
          cover: data.coverImage || "",
          coverImageStatus: Number(data.coverImageStatus || 0),
          coverImageStatusLabel: data.coverImageStatusLabel || "",
          nodes: Array.isArray(data.nodes) && data.nodes.length ? data.nodes.map((node, index) => ({
            id: node.id || `node-${index}`,
            title: node.title || "",
            description: node.description || "",
            duration: node.duration || "",
            level: node.level || 1,
            learningGoal: node.learningGoal || "",
            learningMethod: node.learningMethod || "",
            deliverable: node.deliverable || "",
            resourceIds: Array.isArray(node.resourceIds) ? node.resourceIds : [],
            learningSteps: Array.isArray(node.learningSteps) && node.learningSteps.length ? node.learningSteps.map((step, stepIndex) => ({
              id: `step-${node.id || index}-${stepIndex}`,
              title: step.title || "",
              content: step.content || ""
            })) : buildDefaultSteps()
          })) : [buildDefaultNode(1)]
        };
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/path/create.vue:488", "loadPathDetail failed", error);
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.isEditMode ? "编辑学习路径" : "创建学习路径"),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerRightSafe + "px",
    g: $data.headerHeight + "px",
    h: common_vendor.t($data.isEditMode ? "路径编辑器" : "新建路径"),
    i: $data.form.title,
    j: common_vendor.o(($event) => $data.form.title = $event.detail.value),
    k: $data.form.description,
    l: common_vendor.o(($event) => $data.form.description = $event.detail.value),
    m: common_vendor.f($data.difficultyOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item),
        b: item,
        c: $data.form.difficulty === item ? 1 : "",
        d: common_vendor.o(($event) => $data.form.difficulty = item, item)
      };
    }),
    n: $data.form.totalDuration,
    o: common_vendor.o(($event) => $data.form.totalDuration = $event.detail.value),
    p: !$data.form.cover
  }, !$data.form.cover ? {
    q: common_vendor.o((...args) => $options.chooseCover && $options.chooseCover(...args))
  } : {
    r: $data.form.cover,
    s: common_vendor.o((...args) => $options.chooseCover && $options.chooseCover(...args)),
    t: common_vendor.o(($event) => $data.form.cover = "")
  }, {
    v: $data.form.coverImageStatusLabel
  }, $data.form.coverImageStatusLabel ? {
    w: common_vendor.t($data.form.coverImageStatusLabel)
  } : {}, {
    x: common_vendor.o((...args) => $options.addNode && $options.addNode(...args)),
    y: $data.form.nodes.length === 0
  }, $data.form.nodes.length === 0 ? {} : {}, {
    z: common_vendor.f($data.form.nodes, (node, index, i0) => {
      return {
        a: common_vendor.t(index + 1),
        b: index === 0 ? 1 : "",
        c: common_vendor.o(($event) => $options.moveNodeUp(index), node.id),
        d: index === $data.form.nodes.length - 1 ? 1 : "",
        e: common_vendor.o(($event) => $options.moveNodeDown(index), node.id),
        f: common_vendor.o(($event) => $options.removeNode(index), node.id),
        g: node.title,
        h: common_vendor.o(($event) => node.title = $event.detail.value, node.id),
        i: node.description,
        j: common_vendor.o(($event) => node.description = $event.detail.value, node.id),
        k: node.learningGoal,
        l: common_vendor.o(($event) => node.learningGoal = $event.detail.value, node.id),
        m: node.learningMethod,
        n: common_vendor.o(($event) => node.learningMethod = $event.detail.value, node.id),
        o: node.deliverable,
        p: common_vendor.o(($event) => node.deliverable = $event.detail.value, node.id),
        q: node.duration,
        r: common_vendor.o(($event) => node.duration = $event.detail.value, node.id),
        s: common_vendor.f($data.levelOptions, (level, k1, i1) => {
          return {
            a: common_vendor.t(level.label),
            b: level.value,
            c: node.level === level.value ? 1 : "",
            d: common_vendor.o(($event) => node.level = level.value, level.value)
          };
        }),
        t: common_vendor.f($data.resourceOptions, (resource, k1, i1) => {
          return {
            a: common_vendor.t(resource.name),
            b: resource.id,
            c: node.resourceIds.includes(resource.id) ? 1 : "",
            d: common_vendor.o(($event) => $options.toggleResource(node, resource.id), resource.id)
          };
        }),
        v: common_vendor.o(($event) => $options.addStep(node), node.id),
        w: common_vendor.f(node.learningSteps, (step, stepIndex, i1) => {
          return common_vendor.e({
            a: common_vendor.t(stepIndex + 1)
          }, node.learningSteps.length > 1 ? {
            b: common_vendor.o(($event) => $options.removeStep(node, stepIndex), step.id || stepIndex)
          } : {}, {
            c: step.title,
            d: common_vendor.o(($event) => step.title = $event.detail.value, step.id || stepIndex),
            e: step.content,
            f: common_vendor.o(($event) => step.content = $event.detail.value, step.id || stepIndex),
            g: step.id || stepIndex
          });
        }),
        x: node.learningSteps.length > 1,
        y: node.id
      };
    }),
    A: common_vendor.o((...args) => $options.saveDraft && $options.saveDraft(...args)),
    B: common_vendor.o((...args) => $options.publishPath && $options.publishPath(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-06e12881"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/path/create.js.map
