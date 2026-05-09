<template>
  <view class="page">
    <view
      class="header"
      :style="{ height: headerHeight + 'px', paddingTop: statusBarHeight + 'px', paddingRight: headerRightSafe + 'px' }"
    >
      <view class="header-inner">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
        </view>
        <text class="title">{{ textMap.title }}</text>
        <view class="clear-btn" @click="handleClear" :class="{ disabled: !historyList.length }">{{ textMap.clear }}</view>
      </view>
    </view>
    <view :style="{ height: headerHeight + 'px' }"></view>

    <view class="content">
      <view v-if="loading" class="status-block">{{ textMap.loading }}</view>
      <view v-else-if="historyList.length === 0" class="status-block">{{ textMap.empty }}</view>
      <view v-else class="history-list">
        <view class="history-card" v-for="item in historyList" :key="item.id">
          <view class="history-main" @click="openHistory(item)">
            <image class="cover" :src="item.coverUrl || '/static/cover_placeholder.png'" mode="aspectFill"></image>
            <view class="info">
              <view class="type-row">
                <text class="type-tag" :class="item.targetType">{{ item.targetType === 'book' ? textMap.book : textMap.path }}</text>
                <text class="time">{{ item.lastViewTime }}</text>
              </view>
              <text class="item-title">{{ item.title }}</text>
              <text class="sub-title">{{ item.subTitle || textMap.noSubTitle }}</text>
            </view>
          </view>
          <view class="delete-btn" @click="deleteItem(item.id)">{{ textMap.delete }}</view>
        </view>
      </view>
      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script>
import { clearBrowseHistory, deleteBrowseHistory, getBrowseHistory } from '../../utils/api/user';

export default {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      loading: false,
      historyList: [],
      textMap: {
        title: '\u6d4f\u89c8\u5386\u53f2',
        clear: '\u6e05\u7a7a',
        loading: '\u52a0\u8f7d\u4e2d...',
        empty: '\u6682\u65e0\u6d4f\u89c8\u8bb0\u5f55',
        book: '\u4e66\u7c4d',
        path: '\u8def\u5f84',
        noSubTitle: '\u6682\u65e0\u526f\u6807\u9898',
        delete: '\u5220\u9664'
      }
    };
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof uni.getMenuButtonBoundingClientRect === 'function'
      ? uni.getMenuButtonBoundingClientRect()
      : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
  },
  onShow() {
    this.fetchHistory();
  },
  methods: {
    async fetchHistory() {
      this.loading = true;
      try {
        const data = await getBrowseHistory();
        this.historyList = Array.isArray(data) ? data : [];
      } catch (error) {
        console.error('fetchHistory failed', error);
      } finally {
        this.loading = false;
      }
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' });
        }
      });
    },
    openHistory(item) {
      if (item.routeUrl) {
        uni.navigateTo({ url: item.routeUrl });
      }
    },
    async deleteItem(id) {
      try {
        await deleteBrowseHistory(id);
        this.historyList = this.historyList.filter((item) => item.id !== id);
      } catch (error) {
        console.error('delete history failed', error);
      }
    },
    handleClear() {
      if (!this.historyList.length) return;
      uni.showModal({
        title: '\u6e05\u7a7a\u786e\u8ba4',
        content: '\u786e\u8ba4\u6e05\u7a7a\u6240\u6709\u6d4f\u89c8\u8bb0\u5f55\u5417\uff1f',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await clearBrowseHistory();
            this.historyList = [];
            uni.showToast({ title: '\u5df2\u6e05\u7a7a', icon: 'none' });
          } catch (error) {
            console.error('clear history failed', error);
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f8fc;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  box-sizing: border-box;
  padding-left: 20rpx;
  background: rgba(246, 248, 252, 0.96);
  backdrop-filter: blur(10px);
}

.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding-bottom: 12rpx;
  box-sizing: border-box;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: #edf2f8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
}

.title {
  flex: 1;
  font-size: 30rpx;
  color: #2d3d52;
  font-weight: 700;
}

.clear-btn {
  min-width: 104rpx;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #2f4f75;
  color: #ffffff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn.disabled {
  background: #d7dee8;
  color: #7f8b99;
}

.content {
  padding: 16rpx 20rpx 0;
}

.status-block {
  padding: 120rpx 0;
  text-align: center;
  color: #90a0b2;
  font-size: 26rpx;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.history-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.03);
}

.history-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.cover {
  width: 112rpx;
  height: 144rpx;
  border-radius: 16rpx;
  background: #e9edf2;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.type-tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.type-tag.book {
  background: #eef3ff;
  color: #2d55c7;
}

.type-tag.path {
  background: #eaf8ef;
  color: #2d7f54;
}

.time {
  font-size: 22rpx;
  color: #8b9aab;
}

.item-title {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #24364b;
  line-height: 1.45;
}

.sub-title {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #72859a;
  line-height: 1.5;
}

.delete-btn {
  width: 84rpx;
  height: 60rpx;
  border-radius: 14rpx;
  background: #f2f5f9;
  color: #6f8194;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bottom-space {
  height: calc(88rpx + env(safe-area-inset-bottom));
}
</style>
