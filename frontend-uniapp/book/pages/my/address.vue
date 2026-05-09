<template>
  <view class="page">
    <view class="header" :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px' }">
      <view class="back-btn" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">{{ selectMode ? texts.selectTitle : texts.title }}</text>
      <view class="header-placeholder"></view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="items.length" class="list">
        <view
          v-for="item in items"
          :key="item.id"
          class="card"
          :class="{ selected: selectMode && String(item.id) === String(selectedId) }"
          @click="selectItem(item)"
        >
          <view class="card-head">
            <view class="user-row">
              <text class="name">{{ item.receiverName }}</text>
              <text class="phone">{{ item.receiverPhone }}</text>
            </view>
            <text v-if="item.isDefault" class="default-tag">{{ texts.defaultTag }}</text>
          </view>
          <text class="address">{{ item.fullAddress }}</text>
          <view class="action-row">
            <view v-if="selectMode" class="action-btn primary" @click.stop="selectItem(item)">{{ texts.select }}</view>
            <view class="action-btn" @click.stop="editItem(item)">{{ texts.edit }}</view>
            <view v-if="!item.isDefault" class="action-btn primary" @click.stop="setDefault(item)">{{ texts.setDefault }}</view>
            <view class="action-btn danger" @click.stop="removeItem(item)">{{ texts.delete }}</view>
          </view>
        </view>
      </view>
      <view v-else class="empty">{{ texts.empty }}</view>
    </view>

    <view class="bottom-bar">
      <view class="add-btn" @click="createAddress">{{ texts.add }}</view>
    </view>
  </view>
</template>

<script>
import { deleteAddress, getAddressList, setDefaultAddress } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const SELECTED_ADDRESS_KEY = 'BOOKFLOW_CHECKOUT_SELECTED_ADDRESS_ID';

function buildQuery(item) {
  return [
    `id=${encodeURIComponent(item.id || '')}`,
    `receiverName=${encodeURIComponent(item.receiverName || '')}`,
    `receiverPhone=${encodeURIComponent(item.receiverPhone || '')}`,
    `province=${encodeURIComponent(item.province || '')}`,
    `city=${encodeURIComponent(item.city || '')}`,
    `district=${encodeURIComponent(item.district || '')}`,
    `detailAddress=${encodeURIComponent(item.detailAddress || '')}`,
    `isDefault=${encodeURIComponent(item.isDefault ? 1 : 0)}`
  ].join('&');
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      selectMode: false,
      selectedId: '',
      items: [],
      texts: {
        title: '\u6536\u8d27\u5730\u5740',
        selectTitle: '\u9009\u62e9\u6536\u8d27\u5730\u5740',
        select: '\u9009\u62e9',
        defaultTag: '\u9ed8\u8ba4',
        edit: '\u7f16\u8f91',
        setDefault: '\u8bbe\u4e3a\u9ed8\u8ba4',
        delete: '\u5220\u9664',
        empty: '\u6682\u65e0\u6536\u8d27\u5730\u5740',
        add: '\u65b0\u589e\u5730\u5740',
        deleteTitle: '\u5220\u9664\u5730\u5740',
        deleteText: '\u786e\u8ba4\u5220\u9664\u8be5\u6536\u8d27\u5730\u5740\u5417\uff1f',
        deleteSuccess: '\u5220\u9664\u6210\u529f',
        defaultSuccess: '\u5df2\u8bbe\u4e3a\u9ed8\u8ba4\u5730\u5740'
      }
    };
  },
  onLoad(options = {}) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    this.selectMode = options.select === 'checkout';
    this.selectedId = decodeURIComponent(options.selectedId || '');
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        this.items = await getAddressList() || [];
      } catch (error) {
        console.error('getAddressList failed', error);
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
    },
    createAddress() {
      uni.navigateTo({ url: '/pages/my/address-edit' });
    },
    editItem(item) {
      uni.navigateTo({ url: `/pages/my/address-edit?${buildQuery(item)}` });
    },
    selectItem(item) {
      if (!this.selectMode || !item || !item.id) return;
      uni.setStorageSync(SELECTED_ADDRESS_KEY, item.id);
      uni.navigateBack();
    },
    async setDefault(item) {
      try {
        await setDefaultAddress(item.id);
        uni.showToast({ title: this.texts.defaultSuccess, icon: 'success' });
        this.fetchData();
      } catch (error) {
        console.error('setDefaultAddress failed', error);
      }
    },
    removeItem(item) {
      uni.showModal({
        title: this.texts.deleteTitle,
        content: this.texts.deleteText,
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await deleteAddress(item.id);
            uni.showToast({ title: this.texts.deleteSuccess, icon: 'success' });
            this.fetchData();
          } catch (error) {
            console.error('deleteAddress failed', error);
          }
        }
      });
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
.list { display: flex; flex-direction: column; gap: 16rpx; }
.card { background: #ffffff; border-radius: 20rpx; padding: 20rpx; }
.card.selected { box-shadow: 0 0 0 3rpx #1f5eff inset; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.user-row { display: flex; align-items: center; gap: 16rpx; }
.name { font-size: 28rpx; color: #243548; font-weight: 700; }
.phone { font-size: 24rpx; color: #6c8198; }
.default-tag { padding: 6rpx 14rpx; border-radius: 999rpx; background: #e9f8ed; color: #2f7a4f; font-size: 22rpx; }
.address { display: block; margin-top: 14rpx; font-size: 24rpx; color: #4d6278; line-height: 1.7; }
.action-row { margin-top: 18rpx; display: flex; gap: 12rpx; }
.action-btn { flex: 1; height: 70rpx; border-radius: 16rpx; background: #eef3f9; color: #5a7088; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
.action-btn.primary { background: #edf3ff; color: #1f5eff; }
.action-btn.danger { background: #fff1ee; color: #c85a3b; }
.empty { margin-top: 120rpx; text-align: center; color: #7d8fa2; font-size: 28rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.96); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); }
.add-btn { height: 84rpx; border-radius: 16rpx; background: #173b75; color: #ffffff; font-size: 30rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }
</style>
