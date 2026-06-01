<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in TASK_STATUS" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="publicDesc" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisherNickname" label="发布者" width="120" />
        <el-table-column prop="reward" label="报酬" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ TASK_STATUS[row.status as keyof typeof TASK_STATUS] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-popconfirm title="确定修改任务状态？" @confirm="updateStatus(row)">
              <template #reference>
                <el-button type="warning" link>修改状态</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        @change="fetchData" class="pagination"
      />
    </el-card>

    <el-dialog v-model="statusDialog.visible" title="修改任务状态" width="400px">
      <el-select v-model="statusDialog.newStatus" placeholder="选择状态" style="width:100%">
        <el-option v-for="(label, key) in TASK_STATUS" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <template #footer>
        <el-button @click="statusDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatus">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTasks, updateTaskStatus } from '@/api/tasks'
import { TASK_STATUS } from '@/utils/constants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: undefined as number | undefined, page: 1, size: 10 })
const statusDialog = reactive({ visible: false, taskId: 0, newStatus: 1 })

function statusTag(s: number) {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: '', 4: 'warning', 5: 'success', 6: 'danger' }
  return map[s] ?? 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listTasks({ status: query.status, page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function reset() { query.status = undefined; search() }

function updateStatus(row: any) {
  statusDialog.taskId = row.taskId
  statusDialog.newStatus = 1
  statusDialog.visible = true
}

async function confirmStatus() {
  await updateTaskStatus(statusDialog.taskId, statusDialog.newStatus)
  ElMessage.success('状态已更新')
  statusDialog.visible = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
