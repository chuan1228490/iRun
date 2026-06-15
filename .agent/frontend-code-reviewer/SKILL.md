---
name: "frontend-code-reviewer"
description: "Vue 3/uni-app 前端代码审查专家，覆盖正确性、组件规范、性能、可访问性和代码风格五个维度。触发于 admin/ 或 mobile/ 下的 Vue/TS/JS/CSS 代码变更。"
model: sonnet
memory: user
---

# Frontend Code Reviewer

## When to use this skill

**触发场景：**
- `admin/` 或 `mobile/` 下 Vue/TypeScript/JS/CSS 文件新增或修改
- Vue 3 Composition API、Element Plus、uni-app 相关代码变更
- 前端 PR 提交前的最终审查
- 组件重构或设计变更后需要验证代码质量

**DO NOT use when:**
- 审查的是 Java/Spring Boot 后端代码 → 使用 `backend-code-reviewer`
- 审查的是 Python 代码 → 使用 `deep-code-reviewer`
- 只是预览 UI 效果（不改代码） → 使用 `webapp-testing`
- 修改量极小（文案修改、颜色调整） → 人工确认即可

## How to use this skill

```
/agent:frontend-code-reviewer --scope <file|directory|branch>
```

1. 明确审查范围：单个组件、整个页面目录或当前分支变更
2. Agent 读取源文件并逐文件分析
3. 输出结构化审查报告：按管理端/移动端分层分类
4. 样式和布局问题标记建议，逻辑和状态问题直接修复

## General Review Rules

### 审查维度

按优先级排序：
1. **正确性** — 响应式数据、异步处理、状态管理、边界条件
2. **组件规范** — Composition API 使用、组件职责单一、Props/Emits 定义
3. **性能** — 虚拟列表、图片懒加载、防抖节流、不必要的重渲染
4. **可访问性** — ARIA 标签、键盘导航、颜色对比度
5. **代码风格** — TypeScript 类型、命名规范、代码组织

### 管理端 (Vue 3 + TS + Element Plus) 检查重点
- `<script setup>` Composition API
- TypeScript 类型定义完整
- Element Plus 组件使用正确（`el-table`, `el-form`, `el-dialog`）
- Pinia store 使用规范
- Axios 请求通过 `utils/request.ts` 拦截器
- 双向 token 刷新队列机制

### 移动端 (uni-app + Vue 3) 检查重点
- 页面在 `pages.json` 正确注册
- `navigationStyle: "custom"` 使用自定义导航栏
- API 调用通过 `api/index.js` 模块 + `utils/request.js`
- 上传使用 `uni.uploadFile`（非 `uni.request`）
- WebSocket/STOMP 连接管理
- 支付密码弹窗 promise 式调用
- 草稿保存 composable 使用 `useSubmitLock`

## CheckList

### 通用
- [ ] 使用 `<script setup>` 语法（Vue 3.5 Composition API）
- [ ] TypeScript 类型注解完整，避免 `any`
- [ ] 组件 Props/Emits 有类型定义
- [ ] 无 `console.log` 残留（调试日志）
- [ ] 无硬编码的 API 地址或密钥

### 状态管理
- [ ] Pinia store 使用组合式 API 风格（管理端）
- [ ] Store 变更逻辑在 actions 中，不在组件中直接修改 state
- [ ] token 存储键名正确（`admin_token` / `d2d_user_token`）

### HTTP 请求
- [ ] 管理端：通过 `utils/request.ts` 的 Axios 实例，请求头 `token`
- [ ] 移动端：通过 `utils/request.js` 的 `get/post/put/del`，`auth` 参数指定认证类型
- [ ] 移动端上传使用 `uni.uploadFile` 手动注入 `authentication` 头
- [ ] 401 刷新逻辑不重复实现

### 样式 (管理端)
- [ ] 使用 Element Plus 原生组件，不自定义样式覆盖（除非必要）
- [ ] CSS 自定义属性使用 `theme.css` 中的设计令牌
- [ ] 响应式设计使用 Element Plus 栅格系统

### 样式 (移动端)
- [ ] 使用 `App.vue` 中定义的 CSS 自定义属性（`--primary`, `--surface` 等）
- [ ] 组件使用 `.card`, `.badge`, `.safe-area-bottom` 等全局工具类
- [ ] 入场动画使用预定义 keyframes（`fadeInUp`, `scaleIn`, `slideInRight`）

### 编译 & 类型
- [ ] `npx vue-tsc --noEmit` 类型检查通过（管理端）
- [ ] HBuilderX 编译无报错（移动端）
