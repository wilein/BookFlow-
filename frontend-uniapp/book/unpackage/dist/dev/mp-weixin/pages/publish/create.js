"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  title: "发布书籍",
  uploadImages: "上传图片（最多9张）",
  mainImage: "主图",
  addImage: "添加图片",
  remove: "x",
  isbnPlaceholder: "请输入 ISBN",
  scan: "扫码",
  bookTitle: "书名",
  bookTitlePlaceholder: "请输入书名",
  author: "作者",
  authorPlaceholder: "请输入作者",
  publisher: "出版社",
  publisherPlaceholder: "请输入出版社",
  category: "分类",
  chooseCategory: "请选择分类",
  condition: "新旧程度",
  price: "价格",
  pricePlaceholder: "请输入价格",
  description: "书籍描述",
  descriptionPlaceholder: "请填写书籍描述、版本信息、备注等",
  permission: "批注权限",
  relatedPath: "关联学习路径（可选）",
  existingPath: "已有路径",
  choosePath: "请选择已有路径",
  newPath: "新建路径",
  newPathPlaceholder: "输入新路径名称（可选）",
  submit: "提交发布",
  chooseFromAlbum: "从相册选择",
  chooseCamera: "拍照",
  scanFail: "扫码失败",
  autoFillSuccess: "已自动填充部分信息",
  needVerifyTitle: "需要认证",
  needVerifyContent: "发布前请先完成认证",
  goVerify: "去认证",
  publishing: "发布中...",
  publishSuccess: "发布成功",
  imageRequired: "请至少上传 1 张图片",
  isbnRequired: "请填写 ISBN",
  titleRequired: "请填写书名",
  authorRequired: "请填写作者",
  publisherRequired: "请填写出版社",
  categoryRequired: "请选择分类",
  priceRequired: "请填写价格"
};
const isbnPresetMap = {
  "9787111213826": {
    title: "Java Core Volume I",
    author: "Cay S. Horstmann",
    publisher: "机械工业出版社",
    category: "计算机"
  },
  "9787302515693": {
    title: "数据结构（C语言版）",
    author: "严蔚敏",
    publisher: "清华大学出版社",
    category: "计算机"
  }
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      userVerified: false,
      categoryOptions: ["计算机", "文学", "外语", "经管", "考研", "其他"],
      conditionOptions: [
        { label: "全新", value: 1 },
        { label: "9成新", value: 2 },
        { label: "8成新", value: 3 },
        { label: "6成新", value: 5 }
      ],
      pathOptions: ["Java后端路线", "前端进阶路线", "算法刷题路线"],
      permissionOptions: [
        { label: "公开", value: "public" },
        { label: "仅买家可见", value: "buyer_only" },
        { label: "私密", value: "private" }
      ],
      form: {
        images: [],
        isbn: "",
        title: "",
        author: "",
        publisher: "",
        category: "",
        condition: 3,
        price: "",
        description: "",
        permission: "public",
        pathName: "",
        newPathName: ""
      }
    };
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  async onShow() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    await this.fetchProfile();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await utils_api_user.getUserProfile();
        this.userVerified = Boolean((data == null ? void 0 : data.verified) || Number((data == null ? void 0 : data.authStatus) || 0) === 2);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/publish/create.vue:230", "fetchProfile failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    chooseImageSource() {
      common_vendor.index.showActionSheet({
        itemList: [TEXTS.chooseFromAlbum, TEXTS.chooseCamera],
        success: ({ tapIndex }) => {
          const sourceType = tapIndex === 0 ? ["album"] : ["camera"];
          this.chooseImage(sourceType);
        }
      });
    },
    chooseImage(sourceType) {
      const remain = 9 - this.form.images.length;
      common_vendor.index.chooseImage({
        count: remain,
        sourceType,
        success: (res) => {
          this.form.images = [...this.form.images, ...res.tempFilePaths || []];
        }
      });
    },
    removeImage(index) {
      this.form.images.splice(index, 1);
    },
    scanISBN() {
      common_vendor.index.scanCode({
        onlyFromCamera: false,
        success: (res) => {
          this.form.isbn = String(res.result || "").replace(/\s/g, "");
          this.autoFillByISBN();
        },
        fail: () => {
          common_vendor.index.showToast({ title: TEXTS.scanFail, icon: "none" });
        }
      });
    },
    autoFillByISBN() {
      const isbn = String(this.form.isbn || "").replace(/[^0-9Xx]/g, "");
      if (!isbn)
        return;
      const preset = isbnPresetMap[isbn];
      if (!preset)
        return;
      this.form.title = this.form.title || preset.title;
      this.form.author = this.form.author || preset.author;
      this.form.publisher = this.form.publisher || preset.publisher;
      this.form.category = this.form.category || preset.category;
      common_vendor.index.showToast({ title: TEXTS.autoFillSuccess, icon: "none" });
    },
    onCategoryChange(e) {
      const index = Number(e.detail.value);
      this.form.category = this.categoryOptions[index] || "";
    },
    selectCondition(value) {
      this.form.condition = Number(value);
    },
    onPermissionChange(e) {
      this.form.permission = e.detail.value;
    },
    onPathChange(e) {
      const index = Number(e.detail.value);
      this.form.pathName = this.pathOptions[index] || "";
    },
    validateForm() {
      if (this.form.images.length === 0)
        return TEXTS.imageRequired;
      if (!this.form.isbn)
        return TEXTS.isbnRequired;
      if (!this.form.title)
        return TEXTS.titleRequired;
      if (!this.form.author)
        return TEXTS.authorRequired;
      if (!this.form.publisher)
        return TEXTS.publisherRequired;
      if (!this.form.category)
        return TEXTS.categoryRequired;
      if (!this.form.price)
        return TEXTS.priceRequired;
      return "";
    },
    ensureAuth() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return false;
      if (!(this.userVerified || utils_auth.isVerified())) {
        common_vendor.index.showModal({
          title: TEXTS.needVerifyTitle,
          content: TEXTS.needVerifyContent,
          confirmText: TEXTS.goVerify,
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.switchTab({ url: "/pages/my/my" });
            }
          }
        });
        return false;
      }
      return true;
    },
    async uploadImages() {
      const uploaded = [];
      for (const item of this.form.images) {
        if (!item)
          continue;
        if (/^(https?:)?\//.test(item)) {
          uploaded.push(item);
          continue;
        }
        const data = await utils_api_book.uploadBookImage(item);
        if (data == null ? void 0 : data.url) {
          uploaded.push(data.url);
        }
      }
      return uploaded;
    },
    resetForm() {
      this.form = {
        images: [],
        isbn: "",
        title: "",
        author: "",
        publisher: "",
        category: "",
        condition: 3,
        price: "",
        description: "",
        permission: "public",
        pathName: "",
        newPathName: ""
      };
    },
    async submitPublish() {
      const error = this.validateForm();
      if (error) {
        common_vendor.index.showToast({ title: error, icon: "none" });
        return;
      }
      if (!this.ensureAuth())
        return;
      common_vendor.index.showLoading({ title: TEXTS.publishing });
      try {
        const imageUrls = await this.uploadImages();
        await utils_api_book.publishBook({
          images: imageUrls,
          isbn: this.form.isbn,
          title: this.form.title,
          author: this.form.author,
          publisher: this.form.publisher,
          category: this.form.category,
          condition: this.form.condition,
          price: this.form.price,
          description: this.form.description,
          permission: this.form.permission,
          pathName: this.form.pathName,
          newPathName: this.form.newPathName
        });
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: TEXTS.publishSuccess, icon: "success" });
        this.resetForm();
      } catch (error2) {
        common_vendor.index.__f__("error", "at pages/publish/create.vue:385", "publishBook failed", error2);
        common_vendor.index.hideLoading();
      }
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
    g: common_vendor.t($data.texts.uploadImages),
    h: common_vendor.f($data.form.images, (img, index, i0) => {
      return common_vendor.e({
        a: img,
        b: index === 0
      }, index === 0 ? {
        c: common_vendor.t($data.texts.mainImage)
      } : {}, {
        d: common_vendor.o(($event) => $options.removeImage(index), img + index),
        e: img + index
      });
    }),
    i: common_vendor.t($data.texts.remove),
    j: $data.form.images.length < 9
  }, $data.form.images.length < 9 ? {
    k: common_vendor.t($data.texts.addImage),
    l: common_vendor.o((...args) => $options.chooseImageSource && $options.chooseImageSource(...args))
  } : {}, {
    m: $data.texts.isbnPlaceholder,
    n: common_vendor.o((...args) => $options.autoFillByISBN && $options.autoFillByISBN(...args)),
    o: $data.form.isbn,
    p: common_vendor.o(($event) => $data.form.isbn = $event.detail.value),
    q: common_vendor.t($data.texts.scan),
    r: common_vendor.o((...args) => $options.scanISBN && $options.scanISBN(...args)),
    s: common_vendor.t($data.texts.bookTitle),
    t: $data.texts.bookTitlePlaceholder,
    v: $data.form.title,
    w: common_vendor.o(($event) => $data.form.title = $event.detail.value),
    x: common_vendor.t($data.texts.author),
    y: $data.texts.authorPlaceholder,
    z: $data.form.author,
    A: common_vendor.o(($event) => $data.form.author = $event.detail.value),
    B: common_vendor.t($data.texts.publisher),
    C: $data.texts.publisherPlaceholder,
    D: $data.form.publisher,
    E: common_vendor.o(($event) => $data.form.publisher = $event.detail.value),
    F: common_vendor.t($data.texts.category),
    G: common_vendor.t($data.form.category || $data.texts.chooseCategory),
    H: $data.categoryOptions,
    I: common_vendor.o((...args) => $options.onCategoryChange && $options.onCategoryChange(...args)),
    J: common_vendor.t($data.texts.condition),
    K: common_vendor.f($data.conditionOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.form.condition === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.selectCondition(item.value), item.value)
      };
    }),
    L: common_vendor.t($data.texts.price),
    M: $data.texts.pricePlaceholder,
    N: $data.form.price,
    O: common_vendor.o(($event) => $data.form.price = $event.detail.value),
    P: common_vendor.t($data.texts.description),
    Q: $data.texts.descriptionPlaceholder,
    R: $data.form.description,
    S: common_vendor.o(($event) => $data.form.description = $event.detail.value),
    T: common_vendor.t($data.texts.permission),
    U: common_vendor.f($data.permissionOptions, (item, k0, i0) => {
      return {
        a: item.value,
        b: $data.form.permission === item.value,
        c: common_vendor.t(item.label),
        d: item.value
      };
    }),
    V: common_vendor.o((...args) => $options.onPermissionChange && $options.onPermissionChange(...args)),
    W: common_vendor.t($data.texts.relatedPath),
    X: common_vendor.t($data.texts.existingPath),
    Y: common_vendor.t($data.form.pathName || $data.texts.choosePath),
    Z: $data.pathOptions,
    aa: common_vendor.o((...args) => $options.onPathChange && $options.onPathChange(...args)),
    ab: common_vendor.t($data.texts.newPath),
    ac: $data.texts.newPathPlaceholder,
    ad: $data.form.newPathName,
    ae: common_vendor.o(($event) => $data.form.newPathName = $event.detail.value),
    af: common_vendor.t($data.texts.submit),
    ag: common_vendor.o((...args) => $options.submitPublish && $options.submitPublish(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-9f692334"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/publish/create.js.map
