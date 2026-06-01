<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.replace('/dashboard')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found { text-align: center; padding: 100px 0; }
.not-found h1 { font-size: 72px; color: #909399; }
</style>
