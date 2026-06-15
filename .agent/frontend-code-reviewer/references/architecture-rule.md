# Frontend Architecture Rules

## Scope

适用于 `admin/` (Vue 3 + TS + Element Plus) 和 `mobile/` (uni-app Vue 3) 所有前端代码。

---

## 1. 组件职责单一

**Rule**: 一个组件只做一件事，复杂逻辑抽取为 composable。

### Good
```vue
<!-- UserList.vue — 只负责列表展示 -->
<script setup lang="ts">
import { useUserList } from '@/composables/useUserList'
const { users, loading, loadUsers } = useUserList()
</script>
```

### Bad
```vue
<!-- 组件内混杂 API 调用、状态管理、UI 渲染 → 职责不清 -->
<script setup lang="ts">
const users = ref([])
const loading = ref(false)
const loadUsers = async () => {
  loading.value = true
  const res = await fetch('/api/users')
  users.value = await res.json()
  loading.value = false
}
</script>
```

---

## 2. 响应式数据不可变性

**Rule**: 数组/对象用新引用触发响应，不直接修改原引用。

### Good
```typescript
users.value = [...users.value, newUser]           // 新数组引用
user.value = { ...user.value, name: 'new name' }   // 新对象引用
```

### Bad
```typescript
users.value.push(newUser)              // ❌ 直接修改原数组
user.value.name = 'new name'           // ❌ 直接修改属性（ref 没问题但 reactive 有坑）
```

---

## 3. Pinia Store 规范

**Rule**: Store 只暴露 state/getters/actions，组件不直接修改 state。

### Good
```typescript
// stores/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const adminInfo = ref<AdminInfo | null>(null)
  function login(credentials: LoginDTO) { /* 异步登录逻辑 */ }
  function logout() { /* 清理 token */ }
  return { token, adminInfo, login, logout }
})
```

### Bad
```typescript
// 组件内
const authStore = useAuthStore()
authStore.token = 'new-token'         // ❌ 直接修改，绕过 action
```

---

## 4. 异步请求规范

**Rule**: 统一通过项目封装的方法发请求，不直接用 `fetch`/`axios`。

### Good
```typescript
// 管理端
import request from '@/utils/request'
const data = await request.get('/admin/users', { params: { page, size } })
```
```javascript
// 移动端
import { get, post } from '@/utils/request'
const data = await get('/user/tasks', { page, size }, 'user')
const data = await post('/user/tasks/publish', payload, 'user')
```

### Bad
```typescript
const res = await fetch('/api/admin/users')       // ❌ 绕过封装
const res = await axios.get('/admin/users')        // ❌ 绕过拦截器
```

---

## 5. Token 管理

**Rule**: 使用项目规定的 token 键名，不自行定义认证方式。

### Good
```typescript
// 管理端
localStorage.setItem('admin_token', value)
localStorage.setItem('admin_refresh_token', value)
// 请求头
headers: { token: localStorage.getItem('admin_token') }
```
```javascript
// 移动端
uni.setStorageSync('d2d_user_token', value)
uni.setStorageSync('d2d_user_refresh_token', value)
// 请求头
headers: { authentication: uni.getStorageSync('d2d_user_token') }
```

### Bad
```typescript
localStorage.setItem('token', value)           // ❌ 键名不统一
headers: { Authorization: `Bearer ${token}` }  // ❌ 使用了 Bearer 格式
```

---

## 6. 移动端路由 & 导航

**Rule**: Tab 页面用 `uni.switchTab`，普通页面用 `uni.navigateTo`。

### Good
```javascript
uni.switchTab({ url: '/pages/task-hall/task-hall' })   // Tab 页
uni.navigateTo({ url: '/pages/task-detail/task-detail?id=1' }) // 普通页面
```

### Bad
```javascript
uni.navigateTo({ url: '/pages/task-hall/task-hall' })   // ❌ Tab 页不能用 navigateTo
```

---

## 7. 防抖 / 防重复提交

**Rule**: 提交类操作必须防重复。

### Good
```javascript
// 移动端使用 submit-guard.js
import { useSubmitLock } from '@/utils/submit-guard'
const { locked, withLock } = useSubmitLock()
async function handleSubmit() {
  await withLock(async () => { await api.submit(data) })
}
```

### Bad
```html
<button @click="handleSubmit">提交</button>  <!-- ❌ 可连续点击，重复提交 -->
```
