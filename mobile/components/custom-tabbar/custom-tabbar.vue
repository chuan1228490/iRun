<template>
  <view class="tab-bar safe-area-bottom" @touchmove.stop.prevent>
    <view
      v-for="(item, index) in list"
      :key="index"
      class="tab-item"
      :class="{ 'tab-item--active': selected === index }"
      @click="switchTab(index, item.pagePath)"
    >
      <view class="tab-icon-wrap">
        <uni-icons v-if="item.icon === 'home'" type="home" size="22" :color="selected === index ? '#FF6B4A' : '#7A7A7A'"></uni-icons>
        <uni-icons v-else-if="item.icon === 'hall'" type="search" size="22" :color="selected === index ? '#FF6B4A' : '#7A7A7A'"></uni-icons>
        <uni-icons v-else-if="item.icon === 'orders'" type="list" size="22" :color="selected === index ? '#FF6B4A' : '#7A7A7A'"></uni-icons>
        <uni-icons v-else-if="item.icon === 'message'" type="chat" size="22" :color="selected === index ? '#FF6B4A' : '#7A7A7A'"></uni-icons>
        <uni-icons v-else-if="item.icon === 'profile'" type="person" size="22" :color="selected === index ? '#FF6B4A' : '#7A7A7A'"></uni-icons>
      </view>
      <text class="tab-text" :class="{ 'tab-text--active': selected === index }">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  selected: { type: Number, default: 0 }
})

const emit = defineEmits(['switchTab'])

const list = [
  { pagePath: '/pages/index/index', text: '首页', icon: 'home' },
  { pagePath: '/pages/task-hall/task-hall', text: '大厅', icon: 'hall' },
  { pagePath: '/pages/orders/orders', text: '订单', icon: 'orders' },
  { pagePath: '/pages/message/message', text: '消息', icon: 'message' },
  { pagePath: '/pages/userprofile/userprofile', text: '我的', icon: 'profile' }
]

function switchTab(index, pagePath) {
  emit('switchTab', index)
  uni.switchTab({ url: pagePath })
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 156rpx;
  z-index: 99999;
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  padding: 10rpx 24rpx 0;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: border-box;
  background: rgba(250, 250, 252, 0.92);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-top: 0.5px solid rgba(0, 0, 0, 0.08);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rpx 18rpx;
  border-radius: 20rpx;
  transition: transform 0.12s cubic-bezier(0.16, 1, 0.3, 1);
}

.tab-item:active {
  transform: scale(0.94);
}

.tab-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-text {
  font-size: 20rpx;
  font-weight: 400;
  color: var(--text-tertiary, #7A7A7A);
  margin-top: 2rpx;
  letter-spacing: -0.01em;
  transition: color 0.12s cubic-bezier(0.16, 1, 0.3, 1);
}

.tab-text--active {
  color: var(--primary, #FF6B4A);
  font-weight: 600;
}
</style>
