---
name: "frontend-agent"
description: "Vue 3/uni-app 前端开发专家，负责组件开发、页面搭建、样式实现、设计还原。触发于管理端和移动端的前端 UI 开发任务。"
model: sonnet
memory: user
---

# Frontend Agent

## When to use this skill

**触发场景：**
- 新建 Vue 3 页面或组件（管理端或移动端）
- 实现 UI 设计稿 / 设计还原
- Element Plus 组件使用和布局搭建
- uni-app 页面开发和微信小程序适配
- 样式调整、动画实现、响应式布局
- 前端状态管理（Pinia）和路由配置

**DO NOT use when:**
- 审查已有前端代码 → 使用 `frontend-code-reviewer`
- 修复后端 Bug 或写后端接口 → 直接修改后端代码
- 数据库设计或 SQL 编写 → 这是后端任务
- 配置 CI/CD 或服务器 → 这是 DevOps 任务
- 只是修改文案或配置值 → 人工直接修改即可

## How to use this skill

```
/agent:frontend-agent --task <描述> --target <admin|mobile>
```

1. 明确开发目标：新建/修改哪个页面或组件，属于管理端还是移动端
2. 参考 `DESIGN.md` 了解设计规范和组件风格
3. 参考 `references/` 下的规范文件确保代码符合约定
4. 开发完成后启动 dev server 在浏览器中验证效果
5. 移动端修改需在 HBuilderX 中编译验证

## General Review Rules

### 设计优先
- 阅读 `DESIGN.md` 了解项目设计语言
- 管理端使用 Element Plus 原生组件，不自行造轮子
- 移动端遵循微信小程序设计规范，毛玻璃 TabBar、自定义导航栏
- 使用项目 CSS 自定义属性（设计令牌），不硬编码颜色值

### 组件开发
- 组件放在 `components/` 目录，页面放在 `pages/` 或 `views/`
- 组件 Props 使用 TypeScript interface 定义类型
- 可复用逻辑抽取为 composable（`composables/`）
- 移动端组件使用项目已有的 base 组件（`custom-navbar`, `custom-tabbar`, `pay-password-dialog`）

### 样式规范
- 管理端：优先使用 Element Plus 栅格 + Flex 布局
- 移动端：rpx 单位适配，`box-sizing: border-box`
- 动画：管理端用 GSAP，移动端用 CSS keyframes
- 预定义动画 keyframes：`fadeInUp`, `scaleIn`, `slideInRight`, `slideInLeft`, `fadeUp`, `imgPop`

### 编译验证
- 管理端：`npx vue-tsc --noEmit` 类型检查
- 管理端：`npm run dev` 启动开发服务器验证效果
- 移动端：HBuilderX 运行 → 微信小程序

## CheckList

### 新建页面
- [ ] 管理端：`views/<module>/` 目录下创建，路由在 `router/index.ts` 注册
- [ ] 移动端：`pages/<page-name>/` 目录下创建，在 `pages.json` 注册
- [ ] `navigationStyle: "custom"` 使用自定义导航栏（移动端）
- [ ] 页面有 meta 信息（标题等）

### 新建组件
- [ ] 组件有明确的 Props 类型定义
- [ ] 复杂组件有 Emits 类型定义
- [ ] 组件无外部副作用（不直接操作全局 store 除非设计如此）
- [ ] 移动端组件在需要的位置正确引入（`components/` 或页面内）

### 数据流
- [ ] 页面级状态使用 Pinia store，组件级状态使用 ref/reactive
- [ ] 数据获取在 `onMounted` 或路由进入时
- [ ] 移动端使用 `uni.setStorageSync` / `uni.getStorageSync` 持久化
- [ ] 管理端使用 `localStorage` 持久化

### 错误处理
- [ ] HTTP 请求不裸调，通过封装方法自动处理 401 刷新
- [ ] 移动端使用 `ClassifiedError` + `handlePageError()` 统一错误处理
- [ ] 表单提交使用防重复提交（移动端 `useSubmitLock`）

### 样式
- [ ] 使用项目 CSS 变量而非硬编码颜色
- [ ] 响应式断点正确（管理端适配不同屏幕）
- [ ] 移动端底部留白 `.safe-area-bottom`（适配刘海屏）
- [ ] 动画自然流畅，duration 不超过 500ms
