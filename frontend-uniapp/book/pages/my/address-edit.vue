<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ form.id ? texts.editTitle : texts.createTitle }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view class="card">
        <view class="field">
          <text class="label">{{ texts.receiver }}</text>
          <input class="input" v-model="form.receiverName" :placeholder="texts.receiverPlaceholder" />
        </view>
        <view class="field">
          <text class="label">{{ texts.phone }}</text>
          <input class="input" v-model="form.receiverPhone" type="number" maxlength="20" :placeholder="texts.phonePlaceholder" />
        </view>
        <view class="field">
          <text class="label">{{ texts.province }}</text>
          <input class="input" v-model="form.province" :placeholder="texts.provincePlaceholder" />
        </view>
        <view class="field">
          <text class="label">{{ texts.city }}</text>
          <input class="input" v-model="form.city" :placeholder="texts.cityPlaceholder" />
        </view>
        <view class="field">
          <text class="label">{{ texts.district }}</text>
          <input class="input" v-model="form.district" :placeholder="texts.districtPlaceholder" />
        </view>
        <view class="field">
          <text class="label">{{ texts.detail }}</text>
          <textarea class="textarea" v-model="form.detailAddress" :placeholder="texts.detailPlaceholder"></textarea>
        </view>
        <view class="default-row" @click="toggleDefault">
          <view class="checkbox" :class="{ active: form.isDefault }"></view>
          <text class="default-text">{{ texts.defaultText }}</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="save-btn" @click="submit">{{ submitting ? texts.saving : texts.save }}</view>
    </view>
  </view>
</template>

<script>
import { saveAddress } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

function decodeValue(value) {
  if (!value) return '';
  try {
    return decodeURIComponent(value);
  } catch (error) {
    return value;
  }
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      submitting: false,
      texts: {
        editTitle: '\u7f16\u8f91\u5730\u5740',
        createTitle: '\u65b0\u589e\u5730\u5740',
        receiver: '\u6536\u8d27\u4eba',
        receiverPlaceholder: '\u8bf7\u8f93\u5165\u6536\u8d27\u4eba\u59d3\u540d',
        phone: '\u8054\u7cfb\u7535\u8bdd',
        phonePlaceholder: '\u8bf7\u8f93\u5165\u8054\u7cfb\u7535\u8bdd',
        province: '\u7701\u4efd',
        provincePlaceholder: '\u8bf7\u8f93\u5165\u7701\u4efd',
        city: '\u57ce\u5e02',
        cityPlaceholder: '\u8bf7\u8f93\u5165\u57ce\u5e02',
        district: '\u533a\u53bf',
        districtPlaceholder: '\u8bf7\u8f93\u5165\u533a\u53bf',
        detail: '\u8be6\u7ec6\u5730\u5740',
        detailPlaceholder: '\u8bf7\u8f93\u5165\u8be6\u7ec6\u5730\u5740',
        defaultText: '\u8bbe\u4e3a\u9ed8\u8ba4\u5730\u5740',
        saving: '\u4fdd\u5b58\u4e2d...',
        save: '\u4fdd\u5b58\u5730\u5740',
        saveSuccess: '\u4fdd\u5b58\u6210\u529f',
        errorReceiver: '\u8bf7\u8f93\u5165\u6536\u8d27\u4eba\u59d3\u540d',
        errorPhone: '\u8bf7\u8f93\u5165\u8054\u7cfb\u7535\u8bdd',
        errorRegion: '\u8bf7\u5b8c\u6574\u586b\u5199\u7701\u5e02\u533a',
        errorDetail: '\u8bf7\u8f93\u5165\u8be6\u7ec6\u5730\u5740'
      },
      form: {
        id: '',
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        isDefault: false
      }
    };
  },
  onLoad(options) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.form = {
      id: decodeValue(options.id || ''),
      receiverName: decodeValue(options.receiverName || ''),
      receiverPhone: decodeValue(options.receiverPhone || ''),
      province: decodeValue(options.province || ''),
      city: decodeValue(options.city || ''),
      district: decodeValue(options.district || ''),
      detailAddress: decodeValue(options.detailAddress || ''),
      isDefault: String(options.isDefault || '') === '1'
    };
  },
  methods: {
    goBack() {
      uni.navigateBack({ fail: () => uni.navigateTo({ url: '/pages/my/address' }) });
    },
    toggleDefault() {
      this.form.isDefault = !this.form.isDefault;
    },
    async submit() {
      if (this.submitting) return;
      if (!this.form.receiverName.trim()) {
        uni.showToast({ title: this.texts.errorReceiver, icon: 'none' });
        return;
      }
      if (!this.form.receiverPhone.trim()) {
        uni.showToast({ title: this.texts.errorPhone, icon: 'none' });
        return;
      }
      if (!this.form.province.trim() || !this.form.city.trim() || !this.form.district.trim()) {
        uni.showToast({ title: this.texts.errorRegion, icon: 'none' });
        return;
      }
      if (!this.form.detailAddress.trim()) {
        uni.showToast({ title: this.texts.errorDetail, icon: 'none' });
        return;
      }

      this.submitting = true;
      try {
        await saveAddress({
          id: this.form.id || undefined,
          receiverName: this.form.receiverName,
          receiverPhone: this.form.receiverPhone,
          province: this.form.province,
          city: this.form.city,
          district: this.form.district,
          detailAddress: this.form.detailAddress,
          isDefault: this.form.isDefault
        });
        uni.showToast({ title: this.texts.saveSuccess, icon: 'success' });
        setTimeout(() => this.goBack(), 400);
      } catch (error) {
        console.error('saveAddress failed', error);
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #eef3fb; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #eef3fb; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 16rpx 20rpx 140rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.field { margin-bottom: 18rpx; }
.field:last-child { margin-bottom: 0; }
.label { display: block; margin-bottom: 10rpx; font-size: 24rpx; color: #5d7186; }
.input, .textarea { width: 100%; box-sizing: border-box; border-radius: 14rpx; background: #f3f7fb; font-size: 26rpx; color: #2b3f53; }
.input { height: 72rpx; line-height: 72rpx; padding: 0 20rpx; }
.textarea { min-height: 160rpx; padding: 20rpx; }
.default-row { margin-top: 10rpx; display: flex; align-items: center; gap: 14rpx; }
.checkbox { width: 34rpx; height: 34rpx; border-radius: 10rpx; background: #e7edf5; border: 2rpx solid #cbd7e4; }
.checkbox.active { background: #1f5eff; border-color: #1f5eff; }
.default-text { font-size: 24rpx; color: #44596f; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.save-btn { height: 84rpx; border-radius: 16rpx; background: #173b75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
