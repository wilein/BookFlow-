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

    <view class="content">
      <view v-if="loading" class="state">{{ texts.loading }}</view>
      <view v-else-if="loadError" class="state">
        <text>{{ texts.loadError }}</text>
        <view class="retry-btn" @click="loadData">{{ texts.retry }}</view>
      </view>
      <template v-else>
        <view class="section address-card" @click="openAddressPicker">
          <text class="section-title">{{ texts.address }}</text>
          <view v-if="selectedAddress" class="address-main">
            <view class="address-user">
              <text class="receiver">{{ selectedAddress.receiverName }}</text>
              <text class="phone">{{ selectedAddress.receiverPhone }}</text>
            </view>
            <text class="full-address">{{ selectedAddress.fullAddress }}</text>
          </view>
          <view v-else class="address-empty">{{ texts.noAddress }}</view>
        </view>

        <view class="section">
          <text class="section-title">{{ texts.books }}</text>
          <view v-for="item in items" :key="item.bookId" class="book-row">
            <image class="cover" :src="item.bookCover || '/static/logo.png'" mode="aspectFill"></image>
            <view class="book-main">
              <text class="book-title">{{ item.bookTitle }}</text>
              <text class="seller">{{ texts.seller }}{{ item.sellerName }}</text>
              <text class="price">{{ texts.currency }}{{ item.price }}</text>
            </view>
          </view>
        </view>

        <view class="section">
          <text class="section-title">{{ texts.message }}</text>
          <textarea class="message-input" v-model="buyerMessage" maxlength="120" :placeholder="texts.messagePlaceholder"></textarea>
        </view>
      </template>
    </view>

    <view class="bottom-bar">
      <view class="summary">
        <text class="count">{{ items.length }}{{ texts.selectedUnit }}</text>
        <text class="amount">{{ texts.currency }}{{ totalAmount }}</text>
      </view>
      <view class="submit-btn" :class="{ disabled: submitting || !items.length }" @click="submitCheckout">
        {{ submitting ? texts.submitting : texts.submit }}
      </view>
    </view>
  </view>
</template>

<script>
import { getBookDetail } from '../../utils/api/book';
import { getCartItems } from '../../utils/api/cart';
import { checkoutOrder } from '../../utils/api/order';
import { getAddressList } from '../../utils/api/user';
import { ensureLoggedIn, getCurrentPageUrl } from '../../utils/auth';

const SELECTED_ADDRESS_KEY = 'BOOKFLOW_CHECKOUT_SELECTED_ADDRESS_ID';

function splitIds(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      cartItemIds: [],
      directBookId: '',
      items: [],
      addresses: [],
      selectedAddressId: '',
      buyerMessage: '',
      loading: false,
      submitting: false,
      loadError: false,
      texts: {
        title: '\u786e\u8ba4\u8ba2\u5355',
        loading: '\u52a0\u8f7d\u4e2d...',
        loadError: '\u7ed3\u7b97\u4fe1\u606f\u52a0\u8f7d\u5931\u8d25',
        retry: '\u91cd\u8bd5',
        address: '\u6536\u8d27\u5730\u5740',
        noAddress: '\u8bf7\u9009\u62e9\u6536\u8d27\u5730\u5740',
        books: '\u7ed3\u7b97\u4e66\u7c4d',
        seller: '\u5356\u5bb6\uff1a',
        message: '\u4e70\u5bb6\u7559\u8a00',
        messagePlaceholder: '\u7ed9\u5356\u5bb6\u7559\u8a00\uff0c\u53ef\u9009',
        selectedUnit: ' \u4ef6',
        submit: '\u63d0\u4ea4\u8ba2\u5355',
        submitting: '\u63d0\u4ea4\u4e2d...',
        needAddress: '\u8bf7\u9009\u62e9\u6536\u8d27\u5730\u5740',
        empty: '\u8bf7\u9009\u62e9\u8981\u7ed3\u7b97\u7684\u4e66\u7c4d',
        successTitle: '\u4e0b\u5355\u6210\u529f',
        successText: '\u8ba2\u5355\u5df2\u521b\u5efa\uff0c\u662f\u5426\u524d\u5f80\u8ba2\u5355\u5217\u8868\u4ed8\u6b3e\uff1f',
        viewOrders: '\u67e5\u770b\u8ba2\u5355',
        currency: '\uffe5'
      }
    };
  },
  computed: {
    selectedAddress() {
      return this.addresses.find((item) => String(item.id) === String(this.selectedAddressId)) || this.addresses.find((item) => item.isDefault);
    },
    totalAmount() {
      return this.items.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2);
    }
  },
  async onLoad(options = {}) {
    if (!ensureLoggedIn(getCurrentPageUrl())) return;
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function' ? uni.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.cartItemIds = splitIds(options.cartItemIds);
    this.directBookId = String(options.bookId || '').trim();
    await this.loadData();
  },
  onShow() {
    const addressId = uni.getStorageSync(SELECTED_ADDRESS_KEY);
    if (addressId) {
      this.selectedAddressId = addressId;
      uni.removeStorageSync(SELECTED_ADDRESS_KEY);
      this.refreshAddresses(addressId);
    }
  },
  methods: {
    async refreshAddresses(preferredId = '') {
      try {
        this.addresses = await getAddressList() || [];
        if (preferredId) {
          this.selectedAddressId = preferredId;
        } else if (!this.selectedAddressId) {
          const defaultAddress = this.addresses.find((item) => item.isDefault) || this.addresses[0];
          this.selectedAddressId = defaultAddress ? defaultAddress.id : '';
        }
      } catch (error) {
        console.error('refresh addresses failed', error);
      }
    },
    async loadData() {
      this.loading = true;
      this.loadError = false;
      try {
        const [addresses, cartItems] = await Promise.all([
          getAddressList(),
          this.cartItemIds.length ? getCartItems() : Promise.resolve([])
        ]);
        this.addresses = addresses || [];
        const defaultAddress = this.addresses.find((item) => item.isDefault) || this.addresses[0];
        this.selectedAddressId = defaultAddress ? defaultAddress.id : '';
        if (this.cartItemIds.length) {
          const idSet = new Set(this.cartItemIds.map(String));
          this.items = (cartItems || []).filter((item) => idSet.has(String(item.id)) && item.available);
        } else if (this.directBookId) {
          const book = await getBookDetail(this.directBookId);
          this.items = [{
            bookId: book.id,
            bookTitle: book.title,
            bookCover: Array.isArray(book.imageList) ? book.imageList[0] : book.cover,
            sellerName: book.sellerName || book.seller?.name || '\u4e66\u53cb',
            price: book.price
          }];
        } else {
          this.items = [];
        }
      } catch (error) {
        this.loadError = true;
        console.error('load checkout failed', error);
      } finally {
        this.loading = false;
      }
    },
    openAddressPicker() {
      uni.navigateTo({
        url: `/pages/my/address?select=checkout&selectedId=${encodeURIComponent(this.selectedAddressId || '')}`
      });
    },
    async submitCheckout() {
      if (this.submitting) return;
      if (!this.items.length) {
        uni.showToast({ title: this.texts.empty, icon: 'none' });
        return;
      }
      const address = this.selectedAddress;
      if (!address) {
        uni.showToast({ title: this.texts.needAddress, icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        const result = await checkoutOrder({
          addressId: address.id,
          buyerMessage: this.buyerMessage,
          items: this.items.map((item) => ({
            bookId: item.bookId,
            cartItemId: item.cartItemId || item.id
          }))
        });
        uni.showModal({
          title: this.texts.successTitle,
          content: this.texts.successText,
          confirmText: this.texts.viewOrders,
          success: ({ confirm }) => {
            if (confirm) {
              uni.navigateTo({ url: '/pages/my/orders?status=1&role=buyer' });
            } else {
              uni.navigateBack();
            }
          }
        });
        return result;
      } catch (error) {
        console.error('checkoutOrder failed', error);
      } finally {
        this.submitting = false;
      }
    },
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f5f8; }
.header { position: fixed; left: 0; right: 0; top: 0; z-index: 20; box-sizing: border-box; padding: 0 20rpx; background: #f3f5f8; display: flex; align-items: center; justify-content: space-between; }
.back-btn, .header-placeholder { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.back-btn { border-radius: 16rpx; background: #edf2f8; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 32rpx; height: 32rpx; }
.header-title { font-size: 30rpx; color: #2d3d52; font-weight: 700; }
.content { padding: 16rpx 20rpx 150rpx; }
.section { background: #ffffff; border-radius: 20rpx; padding: 20rpx; margin-bottom: 16rpx; }
.section-title { display: block; margin-bottom: 14rpx; font-size: 28rpx; color: #243548; font-weight: 700; }
.address-card { position: relative; }
.address-user { display: flex; gap: 16rpx; align-items: center; }
.receiver { font-size: 28rpx; color: #243548; font-weight: 700; }
.phone, .full-address, .seller { font-size: 24rpx; color: #6f8196; }
.full-address { display: block; margin-top: 10rpx; line-height: 1.55; }
.address-empty { color: #2d55c7; font-size: 26rpx; }
.book-row { display: flex; gap: 16rpx; padding: 16rpx 0; border-bottom: 1rpx solid #edf1f5; }
.book-row:last-child { border-bottom: none; }
.cover { width: 126rpx; height: 148rpx; border-radius: 14rpx; background: #e8edf3; flex-shrink: 0; }
.book-main { flex: 1; min-width: 0; }
.book-title { display: block; font-size: 28rpx; color: #243548; font-weight: 700; line-height: 1.45; }
.seller { display: block; margin-top: 8rpx; }
.price { display: block; margin-top: 14rpx; color: #d05a25; font-size: 30rpx; font-weight: 700; }
.message-input { width: 100%; min-height: 140rpx; box-sizing: border-box; border-radius: 16rpx; background: #f4f7fb; padding: 18rpx; color: #2b3f53; font-size: 26rpx; }
.state { margin-top: 160rpx; display: flex; flex-direction: column; gap: 20rpx; align-items: center; color: #7d8fa2; font-size: 28rpx; }
.retry-btn { height: 64rpx; padding: 0 32rpx; border-radius: 14rpx; background: #2d55c7; color: #ffffff; display: flex; align-items: center; justify-content: center; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; background: rgba(255, 255, 255, 0.97); border-top: 1rpx solid #e7edf4; padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 20rpx; }
.summary { flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
.count { color: #7a8da2; font-size: 22rpx; }
.amount { color: #d05a25; font-size: 34rpx; font-weight: 700; }
.submit-btn { width: 220rpx; height: 78rpx; border-radius: 16rpx; background: #2d55c7; color: #ffffff; display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: 700; }
.submit-btn.disabled { background: #bfc8d6; }
</style>
