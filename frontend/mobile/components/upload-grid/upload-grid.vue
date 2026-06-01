<template>
  <view class="ug-wrap">
    <view class="ug-row">
      <view v-for="(url, i) in modelValue" :key="i" class="ug-thumb" @click="onPreview(url)">
        <image class="ug-thumb-img" :src="url" mode="aspectFill" />
        <view class="ug-thumb-del" @click.stop="onRemove(i)">
          <iconpark-icon name="closeempty" size="14" color="#fff" />
        </view>
      </view>
      <view v-if="modelValue.length < maxCount" class="ug-add" @click="onAdd">
        <iconpark-icon name="plusempty" size="28" color="#D4D2CC" />
      </view>
    </view>
    <text class="ug-count">{{ modelValue.length }}/{{ maxCount }}</text>
  </view>
</template>

<script setup>
import { commonApi } from '@/api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  maxCount: { type: Number, default: 3 },
  acceptFile: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

function onRemove(index) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)
}

function onPreview(url) {
  uni.previewImage({ urls: [url], current: url })
}

async function onAdd() {
  const remaining = props.maxCount - props.modelValue.length
  if (remaining <= 0) return

  // #ifdef MP-WEIXIN
  if (props.acceptFile) {
    try {
      const actionRes = await new Promise((resolve) => {
        uni.showActionSheet({
          itemList: ['从相册选择图片', '从聊天文件选择'],
          success: resolve,
          fail: () => resolve(null)
        })
      })
      if (!actionRes) return
      if (actionRes.tapIndex === 0) {
        await chooseAndUpload(remaining)
      } else {
        const fileRes = await new Promise((resolve, reject) => {
          wx.chooseMessageFile({ count: Math.min(remaining, 1), type: 'file', success: resolve, fail: reject })
        })
        const file = fileRes.tempFiles[0]
        const url = await commonApi.uploadFile(file.path)
        if (url) {
          emit('update:modelValue', [...props.modelValue, url])
        }
      }
    } catch (e) { /* cancelled */ }
    return
  }
  // #endif

  await chooseAndUpload(remaining)
}

async function chooseAndUpload(count) {
  try {
    const res = await new Promise((resolve, reject) => {
      uni.chooseImage({
        count,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: resolve,
        fail: reject
      })
    })
    uni.showLoading({ title: '上传中…' })
    const urls = await commonApi.uploadFiles(res.tempFilePaths)
    uni.hideLoading()
    if (urls && urls.length) {
      emit('update:modelValue', [...props.modelValue, ...urls])
    }
  } catch (e) {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.ug-wrap { width: 100% }
.ug-row { display: flex; flex-wrap: wrap; gap: 16rpx; align-items: flex-start }
.ug-thumb { width: 160rpx; height: 160rpx; border-radius: 16rpx; overflow: hidden; position: relative; flex-shrink: 0 }
.ug-thumb-img { width: 100%; height: 100%; object-fit: cover }
.ug-thumb-del { position: absolute; top: 4rpx; right: 4rpx; width: 40rpx; height: 40rpx; border-radius: 50%; background: rgba(0,0,0,.55); display: flex; align-items: center; justify-content: center }
.ug-add { width: 160rpx; height: 160rpx; border-radius: 16rpx; border: 2rpx dashed var(--outline); display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: var(--surface) }
.ug-add:active { background: var(--surface-hover) }
.ug-count { display: block; font-size: 22rpx; color: var(--text-tertiary); margin-top: 12rpx }
</style>
