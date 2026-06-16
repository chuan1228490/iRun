/**
 * 草稿自动保存组合式函数
 *
 * 监听表单 ref 变化，防抖写入 localStorage；页面加载时恢复，成功提交后清除。
 *
 * 用法:
 *   const { clearDraft, restoreDraft } = useDraftSave('draft_key', {
 *     field1: ref1,
 *     field2: ref2
 *   })
 *   // onLoad 中: restoreDraft()
 *   // 提交成功后: clearDraft()
 */
import { watch } from 'vue'
import { onHide, onUnload } from '@dcloudio/uni-app'

const DRAFT_VERSION = 1
const DRAFT_TTL_MS = 30 * 60 * 1000 // 30分钟过期

export function useDraftSave(storageKey, formRefs, options = {}) {
  const { debounceMs = 2000 } = options
  let timer = null

  function persist() {
    const snapshot = { __v: DRAFT_VERSION, __t: Date.now() }
    for (const [key, ref] of Object.entries(formRefs)) {
      if (ref && ref.__v_isRef) {
        snapshot[key] = ref.value
      }
    }
    try {
      uni.setStorageSync(storageKey, snapshot)
    } catch (_) {
      // 存储空间满时静默失败
    }
  }

  function scheduleSave() {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      persist()
      timer = null
    }, debounceMs)
  }

  const stopFns = []
  for (const ref of Object.values(formRefs)) {
    if (ref && ref.__v_isRef) {
      stopFns.push(watch(ref, scheduleSave, { deep: true }))
    }
  }

  onHide(() => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    persist()
  })

  onUnload(() => {
    dispose()
  })

  function dispose() {
    if (timer) clearTimeout(timer)
    stopFns.forEach(fn => fn())
    clearDraft()
  }

  function restoreDraft() {
    try {
      const saved = uni.getStorageSync(storageKey)
      if (!saved || saved.__v !== DRAFT_VERSION) {
        if (saved) clearDraft()
        return false
      }
      // 超过30分钟自动清除
      if (saved.__t && Date.now() - saved.__t > DRAFT_TTL_MS) {
        clearDraft()
        return false
      }
      for (const key of Object.keys(formRefs)) {
        if (key in saved && formRefs[key] && formRefs[key].__v_isRef) {
          formRefs[key].value = saved[key]
        }
      }
      return true
    } catch (_) {
      return false
    }
  }

  function clearDraft() {
    try {
      uni.removeStorageSync(storageKey)
    } catch (_) { /* ignore */ }
  }

  function hasDraft() {
    try {
      const saved = uni.getStorageSync(storageKey)
      return saved && saved.__v === DRAFT_VERSION
    } catch (_) {
      return false
    }
  }

  return { clearDraft, restoreDraft, hasDraft, dispose }
}
