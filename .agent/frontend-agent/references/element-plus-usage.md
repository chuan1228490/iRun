# Element Plus Usage Rules

## Scope

适用于 `admin/` 下所有使用 Element Plus 组件的页面和组件。

---

## 全局注册

**Rule**: Element Plus 组件和图标已全局注册（`main.ts`），无需手动导入。

### Good
```vue
<template>
  <el-button type="primary" @click="handleClick">提交</el-button>
  <el-icon><Search /></el-icon>
</template>
```

### Bad
```vue
<script setup lang="ts">
import { ElButton, ElIcon } from 'element-plus'  // ❌ 不需要手动导入
</script>
```

---

## 表格 (el-table)

### Good
```vue
<el-table :data="list" v-loading="loading" border stripe
  @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="55" />
  <el-table-column prop="id" label="ID" width="80" />
  <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
  <el-table-column label="状态" width="100">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'">
        {{ row.status === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </el-table-column>
  <el-table-column label="操作" width="180" fixed="right">
    <template #default="{ row }">
      <el-button size="small" @click="handleEdit(row)">编辑</el-button>
      <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
```

### Bad
```vue
<el-table :data="list">
  <el-table-column prop="id" />  <!-- ❌ 没有 label -->
</el-table>
```
---

## 表单 (el-form)

### Good
```vue
<el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
  <el-form-item label="用户名" prop="username">
    <el-input v-model="form.username" placeholder="请输入用户名" />
  </el-form-item>
  <el-form-item label="角色" prop="role">
    <el-select v-model="form.role" placeholder="请选择角色">
      <el-option label="超级管理员" :value="1" />
      <el-option label="普通管理员" :value="2" />
    </el-select>
  </el-form-item>
  <el-form-item>
    <el-button type="primary" @click="handleSubmit">保存</el-button>
    <el-button @click="handleReset">重置</el-button>
  </el-form-item>
</el-form>
```

### Bad
```vue
<el-form :model="form">
  <el-input v-model="form.username" />    <!-- ❌ 没有 el-form-item 包裹 -->
</el-form>
```

---

## 分页 (el-pagination)

### Good
```vue
<el-pagination
  v-model:current-page="query.page"
  v-model:page-size="query.size"
  :total="total"
  :page-sizes="[10, 20, 50, 100]"
  layout="total, sizes, prev, pager, next, jumper"
  background
  @current-change="fetchData"
  @size-change="fetchData"
/>
```

---

## 弹窗 (el-dialog)

### Good
```vue
<el-dialog v-model="visible" title="编辑用户" width="600px" :close-on-click-modal="false"
  @close="handleReset">
  <el-form :model="form" :rules="rules">
    <!-- 表单内容 -->
  </el-form>
  <template #footer>
    <el-button @click="visible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit">确认</el-button>
  </template>
</el-dialog>
```
---
