<template>
  <div class="page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="发送通知" name="send">
        <el-card>
          <template #header>
            <span>发送通知</span>
          </template>
          <el-form :model="sendForm" label-width="80px">
            <el-form-item label="发送方式">
              <el-radio-group v-model="sendForm.isBroadcast">
                <el-radio :value="false">指定用户</el-radio>
                <el-radio :value="true">全部广播</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="!sendForm.isBroadcast" label="用户ID">
              <el-input v-model="sendForm.userIdsStr" placeholder="多个用逗号分隔" />
            </el-form-item>
            <el-form-item label="通知类型">
              <el-select v-model="sendForm.type" style="width:240px">
                <el-option v-for="(label, key) in NOTIFICATION_TYPES" :key="key" :label="label" :value="Number(key)" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题">
              <el-input v-model="sendForm.title" style="width:400px" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="sendForm.content" type="textarea" :rows="4" style="width:400px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitSend" :loading="sending">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="发送记录" name="history">
        <el-card>
          <el-table :data="historyData" v-loading="historyLoading" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-detail" v-if="row.requestParams">
                  <h4>通知详情</h4>
                  <pre class="params-json">{{ formatJson(row.requestParams) }}</pre>
                </div>
                <div v-else class="expand-empty">暂无详情</div>
              </template>
            </el-table-column>
            <el-table-column prop="adminName" label="操作人" width="100" />
            <el-table-column label="发送方式" width="90">
              <template #default="{ row }">
                <el-tag :type="row.action === '广播' ? 'warning' : ''" size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>

          <el-pagination
            v-model:current-page="historyQuery.page" v-model:page-size="historyQuery.size"
            :total="historyTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
            @change="fetchHistory" class="pagination"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sendNotification, broadcastNotification } from '@/api/notifications'
import { listLogs } from '@/api/logs'
import { NOTIFICATION_TYPES } from '@/utils/constants'

const activeTab = ref('send')
const sending = ref(false)
const sendForm = reactive({ isBroadcast: false, userIdsStr: '', type: 1, title: '', content: '' })

async function submitSend() {
  const title = sendForm.isBroadcast ? '确认向全部用户发送该通知？' : '确认向指定用户发送该通知？'
  await ElMessageBox.confirm(title, '提示', { type: 'warning' })
  sending.value = true
  try {
    if (sendForm.isBroadcast) {
      await broadcastNotification({ type: sendForm.type, title: sendForm.title, content: sendForm.content })
      ElMessage.success('广播成功')
    } else {
      const ids = sendForm.userIdsStr.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
      if (ids.length === 0) { ElMessage.warning('请输入有效的用户ID'); return }
      await sendNotification({ userIds: ids, type: sendForm.type, title: sendForm.title, content: sendForm.content })
      ElMessage.success('发送成功')
    }
    sendForm.userIdsStr = ''; sendForm.title = ''; sendForm.content = ''
    // 切到发送记录并刷新
    activeTab.value = 'history'
    fetchHistory()
  } finally { sending.value = false }
}

// --- 发送记录 ---
const historyLoading = ref(false)
const historyData = ref<any[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({ page: 1, size: 10 })

function formatJson(str: string) {
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await listLogs({ module: '消息管理', page: historyQuery.page, size: historyQuery.size }) as any
    historyData.value = res.records
    historyTotal.value = res.total
  } finally { historyLoading.value = false }
}

watch(activeTab, (tab) => { if (tab === 'history') fetchHistory() })
</script>

<style scoped>
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.expand-detail { padding: 8px 16px; }
.expand-detail h4 { margin: 0 0 8px; font-size: 14px; color: #303133; }
.params-json { background: #f5f7fa; padding: 12px; border-radius: 6px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; margin: 0; max-height: 300px; overflow-y: auto; }
.expand-empty { padding: 16px; color: #909399; font-size: 13px; text-align: center; }
</style>
