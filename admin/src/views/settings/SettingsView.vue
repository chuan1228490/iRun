<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">系统设置</span>
      </template>
    </el-page-header>

    <div class="stats-bar anim-block" style="--anim-order: 0">
      <div class="stat-item">
        <span class="stat-value">{{ configs.length }}</span>
        <span class="stat-label">配置项总数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ lastUpdated }}</span>
        <span class="stat-label">最近更新</span>
      </div>
    </div>

    <div class="detail-blocks" v-loading="loading">
      <el-card
        v-for="group in groups"
        :key="group.name"
        class="detail-block anim-block color-card"
        :style="{ '--anim-order': group.order, '--card-color': group.color, '--card-bg': group.bgColor }"
      >
        <template #header>
          <span class="card-title-text">
            <el-icon class="card-title-icon"><component :is="group.icon" /></el-icon>
            {{ group.name }}
          </span>
        </template>
        <div class="config-rows">
          <div v-for="item in group.items" :key="item.configKey" class="config-row">
            <div class="config-info">
              <span class="config-label">{{ item.description || item.configKey }}</span>
              <span class="config-key">{{ item.configKey }}</span>
            </div>
            <div class="config-value-area">
              <template v-if="editingKey === item.configKey">
                <el-input
                  v-model="editValue"
                  size="small"
                  class="edit-input"
                  :type="item.valueType === 'int' || item.valueType === 'decimal' ? 'number' : 'text'"
                  @keyup.enter="saveItem(item)"
                  @keyup.escape="cancelEdit"
                />
                <el-button type="primary" size="small" @click="saveItem(item)">保存</el-button>
                <el-button size="small" @click="cancelEdit">取消</el-button>
              </template>
              <template v-else>
                <span class="config-value">{{ formatValue(item) }}</span>
                <el-button type="primary" link size="small" @click="startEdit(item)">编辑</el-button>
              </template>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Document, Van, Money } from '@element-plus/icons-vue'
import { getSettings, updateSettings } from '@/api/settings'

const loading = ref(false)
const entered = ref(false)
const configs = ref<any[]>([])
const editingKey = ref<string | null>(null)
const editValue = ref('')

const groupMeta: Record<string, any> = {
  '基础设置': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Setting, order: 1 },
  '订单规则': { color: '#2EB89E', bgColor: '#EDFAF7', icon: Document, order: 2 },
  '跑腿限制': { color: '#C8925D', bgColor: '#FDF3EB', icon: Van, order: 3 },
  '费率设置': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: Money, order: 4 },
}

const lastUpdated = computed(() => {
  if (configs.value.length === 0) return '-'
  const latest = configs.value
    .filter((c: any) => c.updatedAt)
    .reduce((best: any, c: any) => {
      if (!best) return c
      return new Date(c.updatedAt).getTime() > new Date(best.updatedAt).getTime() ? c : best
    }, null)
  return latest?.updatedAt || '-'
})

const groups = computed(() => {
  const map = new Map<string, any[]>()
  for (const c of configs.value) {
    const g = c.configGroup || '其他'
    if (!map.has(g)) map.set(g, [])
    map.get(g)!.push(c)
  }
  return Array.from(map.entries())
    .map(([name, items]) => ({
      name,
      items,
      ...(groupMeta[name] || { color: '#909399', bgColor: '#F5F5F5', icon: Setting, order: 99 }),
    }))
    .sort((a, b) => a.order - b.order)
})

function formatValue(item: any) {
  const v = item.configValue
  if (v === null || v === undefined || v === '') return '-'
  const key = item.configKey as string
  if (key?.includes('fee_rate')) return v + '%'
  if (key?.includes('min_withdrawal')) return '¥' + Number(v).toFixed(2)
  if (key?.includes('_hours')) return v + ' h'
  return v
}

function startEdit(item: any) {
  editingKey.value = item.configKey
  editValue.value = item.configValue
}

function cancelEdit() {
  editingKey.value = null
  editValue.value = ''
}

async function saveItem(item: any) {
  if (editValue.value === item.configValue) {
    cancelEdit()
    return
  }
  try {
    await updateSettings({ items: [{ configKey: item.configKey, configValue: editValue.value }] })
    item.configValue = editValue.value
    item.updatedAt = new Date().toISOString().replace('T', ' ')
    ElMessage.success('配置已更新')
    cancelEdit()
  } catch {
    /* error handled by interceptor */
  }
}

onMounted(async () => {
  loading.value = true
  try {
    configs.value = (await getSettings()) as any[]
  } finally {
    loading.value = false
  }
  await nextTick()
  entered.value = true
})
</script>

<style scoped>
.detail-blocks {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 统计条 ===== */
.stats-bar {
  margin-top: 20px;
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ===== 独立配色卡片 ===== */
.color-card {
  border-left: 3px solid var(--card-color);
}

.color-card :deep(.el-card__header) {
  background: var(--card-bg);
  border-bottom-color: var(--card-color);
  border-bottom-width: 1px;
  transition: background 0.3s ease;
}

.card-title-text {
  font-weight: 600;
  color: var(--text-primary);
}

.card-title-icon {
  margin-right: 6px;
  font-size: 15px;
  vertical-align: -2px;
  color: var(--text-secondary);
}

/* ===== 配置行 ===== */
.config-rows {
  display: flex;
  flex-direction: column;
}

.config-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--neutral-outline-light);
}

.config-row:last-child {
  border-bottom: none;
}

.config-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.config-label {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.config-key {
  font-size: 12px;
  color: var(--text-placeholder);
  font-family: monospace;
}

.config-value-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.config-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--brand-primary);
}

.edit-input {
  width: 160px;
}

/* ===== 入场动画 ===== */
.anim-header,
.anim-block {
  opacity: 0;
}

.entered .anim-header {
  animation: slideInLeft 0.4s var(--ease-out) both;
}

.entered .anim-block {
  animation: fadeUp 0.45s calc(0.05s + var(--anim-order, 0) * 0.06s) var(--ease-out) both;
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
