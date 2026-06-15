<template>
  <view class="form-card">
    <view class="card-title">接单限制</view>
    <view class="chip-row">
      <view class="chip" :class="{ 'chip--active': modelValue === undefined }" @click="$emit('update:modelValue', undefined)">不限</view>
      <view class="chip" :class="{ 'chip--active': modelValue === '男' }" @click="$emit('update:modelValue', '男')">仅男生</view>
      <view class="chip" :class="{ 'chip--active': modelValue === '女' }" @click="$emit('update:modelValue', '女')">仅女生</view>
    </view>
  </view>
</template>

<script setup>
defineProps({ modelValue: { type: [String, undefined], default: undefined } })
defineEmits(['update:modelValue'])
</script>
