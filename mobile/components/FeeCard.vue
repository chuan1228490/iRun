<template>
  <view class="form-card form-card--pay">
    <view class="card-title">费用明细</view>
    <view class="fee-row">
      <text class="fee-label">{{ feeLabel }}</text>
      <text class="fee-value">¥ {{ baseFee.toFixed(2) }}</text>
    </view>
    <template v-if="taskType === 4">
      <view class="form-label" style="margin-top:18rpx">预估商品费</view>
      <view class="custom-tip-row">
        <text class="custom-tip-unit">¥</text>
        <input class="custom-tip-input" name="digit" v-model.number="productFeeModel" placeholder="输入预估商品费用" />
      </view>
      <view class="info-hint" style="margin-top:12rpx">
        <iconpark-icon name="info" size="18" color="#FF6B4A" />
        <text>配送员垫付商品费，送达后当面结算</text>
      </view>
      <view class="fee-divider"></view>
    </template>
    <view class="fee-divider" v-if="taskType !== 4"></view>
    <view class="form-label">小费（提高接单率）</view>
    <view class="chip-row tip-chips">
      <view class="chip" :class="{ 'chip--active': reward === 2 && !showCustomTip }" @click="$emit('update:reward', 2)">¥2</view>
      <view class="chip" :class="{ 'chip--active': reward === 5 && !showCustomTip }" @click="$emit('update:reward', 5)">¥5</view>
      <view class="chip" :class="{ 'chip--active': reward === 10 && !showCustomTip }" @click="$emit('update:reward', 10)">¥10</view>
      <view class="chip" :class="{ 'chip--active': showCustomTip }" @click="$emit('toggleCustomTip')">自定义</view>
    </view>
    <view class="custom-tip-row" v-if="showCustomTip">
      <text class="custom-tip-unit">¥</text>
      <input class="custom-tip-input" name="digit" v-model.number="customTipModel" placeholder="输入小费金额" @input="$emit('update:customTip', $event.target.value)" />
    </view>
    <view class="fee-divider"></view>
    <view class="fee-row fee-row--total">
      <text class="fee-label">合计支付</text>
      <text class="fee-total">¥ {{ totalReward.toFixed(2) }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  taskType: { type: Number, required: true },
  subType: { type: [Number, String], default: null },
  baseFee: { type: Number, required: true },
  totalReward: { type: Number, required: true },
  reward: { type: Number, default: 0 },
  customTip: { type: Number, default: 0 },
  showCustomTip: { type: Boolean, default: false },
  estimatedProductFee: { type: Number, default: 0 },
})

defineEmits(['update:reward', 'update:customTip', 'update:estimatedProductFee', 'toggleCustomTip'])

const productFeeModel = computed({
  get: () => props.estimatedProductFee,
  set: (val) => emit('update:estimatedProductFee', val)
})
const customTipModel = computed({
  get: () => props.customTip,
  set: (val) => emit('update:customTip', val)
})

const emit = defineEmits(['update:reward', 'update:customTip', 'update:estimatedProductFee', 'toggleCustomTip'])

const feeLabel = computed(() =>
  props.taskType === 3 && props.subType === 35 ? '基础服务费' : '基础配送费')
</script>
