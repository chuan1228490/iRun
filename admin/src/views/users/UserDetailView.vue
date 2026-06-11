<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">用户详情 — #{{ userId || '--' }}</span>
      </template>
    </el-page-header>

    <div class="detail-blocks" v-loading="loading">
      <!-- 基本信息 -->
      <el-card class="detail-block anim-block" style="--anim-order: 0">
        <template #header>
          <span class="card-title-text">基本信息</span>
        </template>
        <el-descriptions v-if="user" :column="2" border>
          <el-descriptions-item label="ID">{{ user.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="头像">
            <el-avatar v-if="user.avatarUrl" :src="user.avatarUrl" :size="60" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ user.phone }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ user.sex || '-' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ user.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ user.studentId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ user.campus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="个性签名" :span="2">{{ user.signature || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 账户与认证 -->
      <el-card class="detail-block anim-block" style="--anim-order: 1">
        <template #header>
          <span class="card-title-text">账户与认证</span>
        </template>
        <el-descriptions v-if="user" :column="2" border>
          <el-descriptions-item label="余额">{{ user.balance }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">
            <el-tag :type="certTag(user.isCertify)" size="small">{{ certLabel(user.isCertify) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="跑腿员认证">
            <el-tag :type="certTag(user.verifyStatus)" size="small">{{ certLabel(user.verifyStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">{{ user.status === 1 ? '正常' : '禁用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ user.lastLoginTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ user.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 认证材料 -->
      <el-card v-if="user && (certifyImages.length || user.certifyRemark)" class="detail-block anim-block" style="--anim-order: 2">
        <template #header>
          <span class="card-title-text">认证材料</span>
        </template>
        <div v-if="certifyImages.length" class="certify-section">
          <div class="image-grid">
            <el-image
              v-for="(url, i) in certifyImages"
              :key="i"
              :src="url"
              :preview-src-list="certifyImages"
              :initial-index="i"
              fit="cover"
              class="task-image anim-img"
              :style="{ animationDelay: (0.2 + i * 0.07) + 's' }"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
        </div>
        <el-alert v-if="user.certifyRemark" :title="'备注：' + user.certifyRemark" type="info" show-icon :closable="false" class="certify-remark" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getUserDetail } from '@/api/users'

const route = useRoute()
const userId = ref(Number(route.params.id))
const user = ref<any>(null)
const loading = ref(false)
const entered = ref(false)

const certifyImages = computed(() => {
  if (!user.value?.certifyImg) return []
  return user.value.certifyImg.split(',').map((u: string) => u.trim()).filter(Boolean)
})

function certLabel(s: number) {
  const map: Record<number, string> = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '认证驳回' }
  return s != null ? map[s] ?? '-' : '-'
}
function certTag(s: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? map[s] ?? 'info' : 'info'
}

onMounted(async () => {
  loading.value = true
  try {
    user.value = await getUserDetail(userId.value)
  } finally { loading.value = false }
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

.card-title-text {
  font-weight: 600;
  color: var(--text-primary);
}

.certify-section {
  margin-bottom: 16px;
}

.certify-remark {
  margin-top: 16px;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.task-image {
  width: 160px;
  height: 160px;
  border-radius: var(--radius-sm);
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-placeholder);
  font-size: 12px;
  background: var(--neutral-surface);
  gap: 4px;
}

/* ===== 入场动画 ===== */
.anim-header,
.anim-block,
.anim-img {
  opacity: 0;
}

.entered .anim-header {
  animation: slideInLeft 0.4s var(--ease-out) both;
}

.entered .anim-block {
  animation: fadeUp 0.45s calc(0.05s + var(--anim-order, 0) * 0.06s) var(--ease-out) both;
}

.entered .anim-img {
  animation: imgPop 0.45s var(--ease-spring) both;
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes imgPop {
  from { opacity: 0; transform: scale(0.88); }
  to   { opacity: 1; transform: scale(1); }
}
</style>
