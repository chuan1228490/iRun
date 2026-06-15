# Frontend Design System — 小i跑腿

## 设计语言

### 品牌色彩

| Token | 值 | 用途 |
|-------|-----|------|
| `--primary` | `#FF6B4A` | 暖珊瑚橙 — 主色调、主要按钮、强调元素 |
| `--primary-light` | `#FF8A6E` | 浅珊瑚 — hover 态、辅助高亮 |
| `--primary-dark` | `#E55A3A` | 深珊瑚 — active 态、深色强调 |
| `--accent` | `#2EC4B6` | 青绿 — 成功状态、次要强调、完成标记 |
| `--accent-light` | `#5DD5C8` | 浅青绿 — hover 态 |

### 中性色

| Token | 值 | 用途 |
|-------|-----|------|
| `--background` | `#FFF5F2` | 暖白底色 — 页面背景 |
| `--surface` | `#FFFFFF` | 纯白 — 卡片、列表项 |
| `--raised` | `#FFFAF8` | 暖白浮层 — 弹窗、浮层 |
| `--text-primary` | `#2C2C2C` | 主文字 |
| `--text-secondary` | `#8E8E93` | 次要文字、占位符 |
| `--text-disabled` | `#C6C6C8` | 禁用文字 |
| `--divider` | `#F0F0F0` | 分割线 |

### 功能色

| Token | 值 | 用途 |
|-------|-----|------|
| `--success` | `#34C759` | 成功 |
| `--warning` | `#FF9500` | 警告 |
| `--danger` | `#FF3B30` | 错误/危险 |
| `--info` | `#007AFF` | 信息提示 |

### 字体

| 层级 | 管理端 | 移动端 |
|------|--------|--------|
| 标题 (H1) | 28px/700 | 36rpx/700 |
| 副标题 (H2) | 22px/600 | 32rpx/600 |
| 卡片标题 (H3) | 18px/500 | 28rpx/500 |
| 正文 | 14px/400 | 26rpx/400 |
| 辅助文字 | 12px/400 | 22rpx/400 |

字体族：`-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif`

### 圆角 & 阴影

| Token | 值 | 用途 |
|-------|-----|------|
| `--radius-sm` | `6rpx` / `4px` | 小标签、徽章 |
| `--radius-md` | `12rpx` / `8px` | 卡片、按钮、输入框 |
| `--radius-lg` | `20rpx` / `12px` | 弹窗、大卡片 |
| `--shadow-card` | `0 2rpx 12rpx rgba(0,0,0,0.06)` | 卡片浮起 |
| `--shadow-dialog` | `0 8rpx 32rpx rgba(0,0,0,0.12)` | 弹窗/浮层 |

---

## 管理端（Vue 3 + Element Plus）

### 布局模式

```
AdminLayout
├── Sidebar (el-menu, 可折叠)
│   ├── Logo + 标题
│   └── 菜单项 (router-link)
└── Main
    ├── 顶部面包屑 (el-breadcrumb)
    └── Content
        ├── 搜索/筛选区 (el-form inline)
        ├── 操作按钮区
        ├── 数据表格 (el-table + el-pagination)
        └── 或详情卡片 (el-card 块式布局)
```

### 列表页组件模式

```vue
<script setup lang="ts">
// 1. 状态
const loading = ref(false)
const list = ref<Item[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

// 2. 数据获取
const fetchData = async () => { ... }
onMounted(() => fetchData())

// 3. 操作
const handleDelete = async (id: number) => { ... }
</script>

<template>
  <div class="list-container">
    <!-- 搜索区 -->
    <el-form :inline="true" :model="query">
      <el-form-item><el-input v-model="query.keyword" placeholder="搜索..." /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
    </el-form>
    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <!-- 更多列... -->
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="total, prev, pager, next" @change="fetchData" />
  </div>
</template>
```

### 详情页 v2 块式布局

```vue
<template>
  <div class="detail-container">
    <el-card v-for="(section, i) in sections" :key="i"
      :class="{ entered: true }" :style="{ '--anim-order': i }">
      <template #header>{{ section.title }}</template>
      <!-- 内容 -->
    </el-card>
  </div>
</template>

<style>
.el-card { opacity: 0; animation: fadeUp 0.4s ease forwards; animation-delay: calc(var(--anim-order) * 0.08s); }
@keyframes fadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
</style>
```

### ECharts 图表

使用 `vue-echarts` 封装：

```vue
<script setup lang="ts">
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])
</script>

<template>
  <v-chart :option="chartOption" autoresize style="height: 320px" />
</template>
```

---

## 移动端（uni-app + Vue 3）

### 页面结构

```
┌─────────────────────────┐
│  custom-navbar          │  ← 自定义导航栏（毛玻璃效果）
│   标题 + 返回按钮        │
├─────────────────────────┤
│                         │
│  页面内容（scroll-view） │
│  ├─ card 1              │
│  ├─ card 2              │
│  └─ card 3              │
│                         │
├─────────────────────────┤
│  safe-area-bottom       │  ← 底部安全区
├─────────────────────────┤
│  custom-tabbar          │  ← 毛玻璃 TabBar（5 标签）
└─────────────────────────┘
```

### 卡片组件

```vue
<template>
  <view class="card">
    <view class="card-header">
      <text class="card-title">{{ title }}</text>
    </view>
    <view class="card-body">
      <slot />
    </view>
  </view>
</template>
```

### 任务卡片（task-normalizer 统一格式）

所有任务卡片通过 `utils/task-normalizer.js` 将 API 数据转换为统一格式：
- `taskId`, `taskNo`, `type`, `subType`, `publicDesc`
- `reward`, `tip`, `deliveryFee`, `productCost`
- `pickupAddress`, `deliveryAddress`
- `publisherNickname`, `publisherAvatar`
- `status`, `expireTime`, `publishTime`

### 图片上传

使用 `upload-grid` 组件：
```vue
<upload-grid v-model="imageUrls" :max="3" />
```

### 支付密码弹窗

全局 promise 式调用：
```javascript
import { promptPayPassword } from '@/store/index'
const password = await promptPayPassword('请输入支付密码确认支付')
if (password) { /* 继续支付流程 */ }
```

### WebSocket/STOMP 连接

由 `store/chat.js` 和 `store/index.js` 自动管理，页面无需手动处理：
- 登录后自动连接，退出后断开
- 心跳 10s，3 次重试后尝试 refresh token
- 消息去重 + 乐观替换
