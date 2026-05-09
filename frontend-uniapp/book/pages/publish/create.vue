<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ texts.title }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <scroll-view class="content" scroll-y>
      <view class="section">
        <text class="section-title">{{ texts.uploadImages }}</text>
        <view class="image-grid">
          <view class="image-item" v-for="(img, index) in form.images" :key="img + index">
            <image class="preview" :src="img" mode="aspectFill"></image>
            <view class="main-tag" v-if="index === 0">{{ texts.mainImage }}</view>
            <view class="remove-btn" @click.stop="removeImage(index)">{{ texts.remove }}</view>
          </view>
          <view v-if="form.images.length < 9" class="image-item add-item" @click="chooseImageSource">
            <text class="add-icon">+</text>
            <text class="add-text">{{ texts.addImage }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="field-row">
          <text class="label">ISBN</text>
          <view class="isbn-wrap">
            <input class="input" v-model="form.isbn" :placeholder="texts.isbnPlaceholder" @blur="autoFillByISBN" />
            <view class="scan-btn" @click="scanISBN">{{ texts.scan }}</view>
          </view>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.bookTitle }}</text>
          <input class="input" v-model="form.title" :placeholder="texts.bookTitlePlaceholder" />
        </view>
        <view class="field-row">
          <text class="label">{{ texts.author }}</text>
          <input class="input" v-model="form.author" :placeholder="texts.authorPlaceholder" />
        </view>
        <view class="field-row">
          <text class="label">{{ texts.publisher }}</text>
          <input class="input" v-model="form.publisher" :placeholder="texts.publisherPlaceholder" />
        </view>
        <view class="field-row">
          <text class="label">{{ texts.category }}</text>
          <picker :range="categoryOptions" @change="onCategoryChange">
            <view class="picker-value">{{ form.category || texts.chooseCategory }}</view>
          </picker>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.condition }}</text>
          <view class="condition-options">
            <view
              v-for="item in conditionOptions"
              :key="item.value"
              class="condition-btn"
              :class="{ active: form.condition === item.value }"
              @click="selectCondition(item.value)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.price }}</text>
          <input class="input" type="digit" v-model="form.price" :placeholder="texts.pricePlaceholder" />
        </view>
        <view class="field-row textarea-row">
          <text class="label">{{ texts.description }}</text>
          <textarea class="textarea" v-model="form.description" :placeholder="texts.descriptionPlaceholder"></textarea>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.permission }}</text>
          <radio-group class="radio-group" @change="onPermissionChange">
            <label class="radio-item" v-for="item in permissionOptions" :key="item.value">
              <radio :value="item.value" :checked="form.permission === item.value" color="#1F5EFF" />
              <text>{{ item.label }}</text>
            </label>
          </radio-group>
        </view>
      </view>

      <view class="section">
        <text class="section-title">{{ texts.relatedPath }}</text>
        <view class="field-row">
          <text class="label">{{ texts.existingPath }}</text>
          <picker :range="pathOptions" @change="onPathChange">
            <view class="picker-value">{{ form.pathName || texts.choosePath }}</view>
          </picker>
        </view>
        <view class="field-row">
          <text class="label">{{ texts.newPath }}</text>
          <input class="input" v-model="form.newPathName" :placeholder="texts.newPathPlaceholder" />
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="submit-bar">
      <view class="submit-btn" @click="submitPublish">{{ texts.submit }}</view>
    </view>
  </view>
</template>

<script>
import { publishBook, uploadBookImage } from '../../utils/api/book';
import { getUserProfile } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl, isVerified } from '../../utils/auth';

const TEXTS = {
  title: '\u53d1\u5e03\u4e66\u7c4d',
  uploadImages: '\u4e0a\u4f20\u56fe\u7247\uff08\u6700\u591a9\u5f20\uff09',
  mainImage: '\u4e3b\u56fe',
  addImage: '\u6dfb\u52a0\u56fe\u7247',
  remove: 'x',
  isbnPlaceholder: '\u8bf7\u8f93\u5165 ISBN',
  scan: '\u626b\u7801',
  bookTitle: '\u4e66\u540d',
  bookTitlePlaceholder: '\u8bf7\u8f93\u5165\u4e66\u540d',
  author: '\u4f5c\u8005',
  authorPlaceholder: '\u8bf7\u8f93\u5165\u4f5c\u8005',
  publisher: '\u51fa\u7248\u793e',
  publisherPlaceholder: '\u8bf7\u8f93\u5165\u51fa\u7248\u793e',
  category: '\u5206\u7c7b',
  chooseCategory: '\u8bf7\u9009\u62e9\u5206\u7c7b',
  condition: '\u65b0\u65e7\u7a0b\u5ea6',
  price: '\u4ef7\u683c',
  pricePlaceholder: '\u8bf7\u8f93\u5165\u4ef7\u683c',
  description: '\u4e66\u7c4d\u63cf\u8ff0',
  descriptionPlaceholder: '\u8bf7\u586b\u5199\u4e66\u7c4d\u63cf\u8ff0\u3001\u7248\u672c\u4fe1\u606f\u3001\u5907\u6ce8\u7b49',
  permission: '\u6279\u6ce8\u6743\u9650',
  relatedPath: '\u5173\u8054\u5b66\u4e60\u8def\u5f84\uff08\u53ef\u9009\uff09',
  existingPath: '\u5df2\u6709\u8def\u5f84',
  choosePath: '\u8bf7\u9009\u62e9\u5df2\u6709\u8def\u5f84',
  newPath: '\u65b0\u5efa\u8def\u5f84',
  newPathPlaceholder: '\u8f93\u5165\u65b0\u8def\u5f84\u540d\u79f0\uff08\u53ef\u9009\uff09',
  submit: '\u63d0\u4ea4\u53d1\u5e03',
  chooseFromAlbum: '\u4ece\u76f8\u518c\u9009\u62e9',
  chooseCamera: '\u62cd\u7167',
  scanFail: '\u626b\u7801\u5931\u8d25',
  autoFillSuccess: '\u5df2\u81ea\u52a8\u586b\u5145\u90e8\u5206\u4fe1\u606f',
  needVerifyTitle: '\u9700\u8981\u8ba4\u8bc1',
  needVerifyContent: '\u53d1\u5e03\u524d\u8bf7\u5148\u5b8c\u6210\u8ba4\u8bc1',
  goVerify: '\u53bb\u8ba4\u8bc1',
  publishing: '\u53d1\u5e03\u4e2d...',
  publishSuccess: '\u53d1\u5e03\u6210\u529f',
  imageRequired: '\u8bf7\u81f3\u5c11\u4e0a\u4f20 1 \u5f20\u56fe\u7247',
  isbnRequired: '\u8bf7\u586b\u5199 ISBN',
  titleRequired: '\u8bf7\u586b\u5199\u4e66\u540d',
  authorRequired: '\u8bf7\u586b\u5199\u4f5c\u8005',
  publisherRequired: '\u8bf7\u586b\u5199\u51fa\u7248\u793e',
  categoryRequired: '\u8bf7\u9009\u62e9\u5206\u7c7b',
  priceRequired: '\u8bf7\u586b\u5199\u4ef7\u683c'
};

const isbnPresetMap = {
  '9787111213826': {
    title: 'Java Core Volume I',
    author: 'Cay S. Horstmann',
    publisher: '\u673a\u68b0\u5de5\u4e1a\u51fa\u7248\u793e',
    category: '\u8ba1\u7b97\u673a'
  },
  '9787302515693': {
    title: '\u6570\u636e\u7ed3\u6784\uff08C\u8bed\u8a00\u7248\uff09',
    author: '\u4e25\u851a\u654f',
    publisher: '\u6e05\u534e\u5927\u5b66\u51fa\u7248\u793e',
    category: '\u8ba1\u7b97\u673a'
  }
};

export default {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      userVerified: false,
      categoryOptions: ['\u8ba1\u7b97\u673a', '\u6587\u5b66', '\u5916\u8bed', '\u7ecf\u7ba1', '\u8003\u7814', '\u5176\u4ed6'],
      conditionOptions: [
        { label: '\u5168\u65b0', value: 1 },
        { label: '9\u6210\u65b0', value: 2 },
        { label: '8\u6210\u65b0', value: 3 },
        { label: '6\u6210\u65b0', value: 5 }
      ],
      pathOptions: ['Java\u540e\u7aef\u8def\u7ebf', '\u524d\u7aef\u8fdb\u9636\u8def\u7ebf', '\u7b97\u6cd5\u5237\u9898\u8def\u7ebf'],
      permissionOptions: [
        { label: '\u516c\u5f00', value: 'public' },
        { label: '\u4ec5\u4e70\u5bb6\u53ef\u89c1', value: 'buyer_only' },
        { label: '\u79c1\u5bc6', value: 'private' }
      ],
      form: {
        images: [],
        isbn: '',
        title: '',
        author: '',
        publisher: '',
        category: '',
        condition: 3,
        price: '',
        description: '',
        permission: 'public',
        pathName: '',
        newPathName: ''
      }
    };
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  async onShow() {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    await this.fetchProfile();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await getUserProfile();
        this.userVerified = Boolean(data?.verified || Number(data?.authStatus || 0) === 2);
      } catch (error) {
        console.error('fetchProfile failed', error);
      }
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    chooseImageSource() {
      uni.showActionSheet({
        itemList: [TEXTS.chooseFromAlbum, TEXTS.chooseCamera],
        success: ({ tapIndex }) => {
          const sourceType = tapIndex === 0 ? ['album'] : ['camera'];
          this.chooseImage(sourceType);
        }
      });
    },
    chooseImage(sourceType) {
      const remain = 9 - this.form.images.length;
      uni.chooseImage({
        count: remain,
        sourceType,
        success: (res) => {
          this.form.images = [...this.form.images, ...(res.tempFilePaths || [])];
        }
      });
    },
    removeImage(index) {
      this.form.images.splice(index, 1);
    },
    scanISBN() {
      uni.scanCode({
        onlyFromCamera: false,
        success: (res) => {
          this.form.isbn = String(res.result || '').replace(/\s/g, '');
          this.autoFillByISBN();
        },
        fail: () => {
          uni.showToast({ title: TEXTS.scanFail, icon: 'none' });
        }
      });
    },
    autoFillByISBN() {
      const isbn = String(this.form.isbn || '').replace(/[^0-9Xx]/g, '');
      if (!isbn) return;
      const preset = isbnPresetMap[isbn];
      if (!preset) return;
      this.form.title = this.form.title || preset.title;
      this.form.author = this.form.author || preset.author;
      this.form.publisher = this.form.publisher || preset.publisher;
      this.form.category = this.form.category || preset.category;
      uni.showToast({ title: TEXTS.autoFillSuccess, icon: 'none' });
    },
    onCategoryChange(e) {
      const index = Number(e.detail.value);
      this.form.category = this.categoryOptions[index] || '';
    },
    selectCondition(value) {
      this.form.condition = Number(value);
    },
    onPermissionChange(e) {
      this.form.permission = e.detail.value;
    },
    onPathChange(e) {
      const index = Number(e.detail.value);
      this.form.pathName = this.pathOptions[index] || '';
    },
    validateForm() {
      if (this.form.images.length === 0) return TEXTS.imageRequired;
      if (!this.form.isbn) return TEXTS.isbnRequired;
      if (!this.form.title) return TEXTS.titleRequired;
      if (!this.form.author) return TEXTS.authorRequired;
      if (!this.form.publisher) return TEXTS.publisherRequired;
      if (!this.form.category) return TEXTS.categoryRequired;
      if (!this.form.price) return TEXTS.priceRequired;
      return '';
    },
    ensureAuth() {
      if (!ensureLoggedIn(getCurrentPageUrl())) return false;
      if (!(this.userVerified || isVerified())) {
        uni.showModal({
          title: TEXTS.needVerifyTitle,
          content: TEXTS.needVerifyContent,
          confirmText: TEXTS.goVerify,
          success: (res) => {
            if (res.confirm) {
              uni.switchTab({ url: '/pages/my/my' });
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
        if (!item) continue;
        if (/^(https?:)?\//.test(item)) {
          uploaded.push(item);
          continue;
        }
        const data = await uploadBookImage(item);
        if (data?.url) {
          uploaded.push(data.url);
        }
      }
      return uploaded;
    },
    resetForm() {
      this.form = {
        images: [],
        isbn: '',
        title: '',
        author: '',
        publisher: '',
        category: '',
        condition: 3,
        price: '',
        description: '',
        permission: 'public',
        pathName: '',
        newPathName: ''
      };
    },
    async submitPublish() {
      const error = this.validateForm();
      if (error) {
        uni.showToast({ title: error, icon: 'none' });
        return;
      }
      if (!this.ensureAuth()) return;
      uni.showLoading({ title: TEXTS.publishing });
      try {
        const imageUrls = await this.uploadImages();
        await publishBook({
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
        uni.hideLoading();
        uni.showToast({ title: TEXTS.publishSuccess, icon: 'success' });
        this.resetForm();
      } catch (error) {
        console.error('publishBook failed', error);
        uni.hideLoading();
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #dbeafe 0%, #eef3fb 240rpx, #eef3fb 100%); }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 30; background: rgba(255, 255, 255, 0.96); padding-left: 20rpx; padding-right: 20rpx; border-bottom: 1rpx solid #dfe8f4; display: flex; align-items: center; justify-content: space-between; box-sizing: border-box; box-shadow: 0 10rpx 28rpx rgba(23, 32, 51, 0.06); }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 18rpx; background: #e8efff; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { flex: 1; text-align: center; font-size: 34rpx; color: #2b3f53; font-weight: 700; line-height: 76rpx; }
.content { height: 100vh; padding-bottom: 160rpx; box-sizing: border-box; }
.section { margin: 18rpx 24rpx 0; padding: 24rpx; border-radius: 28rpx; background: #ffffff; border: 1rpx solid #e2eaf5; box-shadow: 0 16rpx 36rpx rgba(23, 32, 51, 0.07); }
.section-title { display: block; font-size: 28rpx; color: #2b3f53; font-weight: 700; margin-bottom: 16rpx; }
.image-grid { display: flex; flex-wrap: wrap; gap: 14rpx; }
.image-item { width: 200rpx; height: 200rpx; border-radius: 22rpx; position: relative; overflow: hidden; background: #f1f5f9; border: 1rpx solid #dfe8f4; }
.preview { width: 100%; height: 100%; }
.main-tag { position: absolute; left: 10rpx; top: 10rpx; padding: 4rpx 10rpx; border-radius: 999rpx; background: rgba(15, 118, 110, 0.9); color: #ffffff; font-size: 20rpx; }
.remove-btn { position: absolute; right: 10rpx; top: 10rpx; width: 40rpx; height: 40rpx; border-radius: 50%; background: rgba(0, 0, 0, 0.45); color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.add-item { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #7890a7; }
.add-icon { font-size: 56rpx; line-height: 1; }
.add-text { margin-top: 10rpx; font-size: 24rpx; }
.field-row { margin-top: 18rpx; }
.field-row:first-child { margin-top: 0; }
.label { display: block; margin-bottom: 12rpx; font-size: 26rpx; color: #51657d; font-weight: 600; }
.isbn-wrap { display: flex; gap: 12rpx; }
.input, .picker-value { height: 80rpx; line-height: 80rpx; border-radius: 18rpx; background: #f7faff; padding: 0 20rpx; font-size: 26rpx; color: #2d3d52; box-sizing: border-box; border: 1rpx solid #e2eaf5; }
.input { width: 100%; }
.picker-value { color: #5d7086; }
.scan-btn { width: 120rpx; height: 80rpx; border-radius: 18rpx; background: #e8efff; color: #1f5eff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.condition-options { display: flex; flex-wrap: wrap; gap: 12rpx; }
.condition-btn { padding: 14rpx 22rpx; border-radius: 999rpx; background: #f4f7fb; color: #617489; font-size: 24rpx; border: 1rpx solid #e2eaf5; }
.condition-btn.active { background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; font-weight: 700; border-color: transparent; }
.textarea { width: 100%; min-height: 220rpx; border-radius: 18rpx; background: #f7faff; padding: 20rpx; font-size: 26rpx; color: #2d3d52; box-sizing: border-box; border: 1rpx solid #e2eaf5; }
.radio-group { display: flex; flex-wrap: wrap; gap: 20rpx; }
.radio-item { display: flex; align-items: center; gap: 10rpx; font-size: 24rpx; color: #55697e; }
.bottom-space { height: 40rpx; }
.submit-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.submit-btn { height: 84rpx; border-radius: 22rpx; background: linear-gradient(135deg, #1f5eff 0%, #0f766e 100%); color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; box-shadow: 0 12rpx 26rpx rgba(31, 94, 255, 0.2); }
</style>
