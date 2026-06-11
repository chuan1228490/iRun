# 2026/06/10 — 管理端详情页重构 + 费用字段拆分

## 完成工作

### 1. 移动端 P2 代码审查修复（7 项）

| # | 文件 | 修复 |
|---|------|------|
| 1 | `order-completed.vue` | `displayDescription` 新增 type=1 快递包裹解析 |
| 2 | `order-delivering.vue` | 移除 type=4 分支死逻辑（if/else 双臂相同） |
| 3-5 | 三个详情页 | 移除未使用的 `parseShoppingItemsFromSpecs` 导入 |
| 6 | `order-completed.vue` | 送达信息卡片补充收货人、联系电话 |
| 7 | `order-waiting.vue` | 非发布者视图新增 `extraFee` 渲染行 |

额外修复：`productFeeText` 三页统一 `Number()` 包装，防止 JSON 字符串值引发 TypeError。

### 2. 管理端详情页分块重构

三个详情页从单卡片 `el-descriptions` 平铺改为分块卡片布局：

| 页面 | 块数 | 结构 |
|------|:--:|------|
| UserDetailView | 3 | 基本信息 → 账户与认证 → 认证材料 |
| TaskDetailView | 4 | 任务信息 → 取送信息 → 描述信息 → 取消原因+图片 |
| OrderDetailView | 5 | 订单信息 → 取送信息 → 人员信息 → 时间记录 → 图片凭证 |

关键改进：
- 用户认证照片改为图片网格（最多 3 张），参照凭证图片布局
- 费用明细改为「合计支付」悬停弹窗（el-popover），不破坏卡片布局
- 统一 `el-page-header` 返回导航 + 卡片间距 16px + `--anim-order` 交错入场动画
- Label 列宽度统一 9em，值列自动填充

### 3. Vite 代理配置修复

**根因**: `/api` 全量代理到后端 8080，Vite dev server 沦为纯代理，HMR 完全失效。

**修复**: `vite.config.ts` 改为仅代理 `/api/(admin|user/|common|ws)`，前端资源由 Vite 直接提供。`/api/user/` 精确匹配移动端 API，不拦截 `/api/users/`（SPA 路由）。

### 4. 数据库费用字段拆分

task 表新增三列：

```sql
ALTER TABLE task ADD COLUMN tip          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '小费';
ALTER TABLE task ADD COLUMN delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '配送费';
ALTER TABLE task ADD COLUMN product_cost DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '预估商品费';
```

配套变更：
- **Entity** `Task.java` — 3 个 `@TableField` 字段
- **DTO** `TaskPublishDTO.java` — `reward` 字段重命名 `tip`，校验注解中文更新
- **VO** `TaskDetailVO.java`、`TaskListVO.java`、`OrderDetailVO.java` — 新增三个字段
- **Service** `AdminServiceImpl.java` — builder 补充 `.tip/.deliveryFee/.productCost`（根因所在）
- **Service** `TaskServiceImpl.java` — `publishTask` 写入新列，`mergeTaskSpecs` 简化
- **全局**「赏金」→「小费」文案替换（8 个文件）
- **移动端** 5 个发布页的 API 字段 `reward:` → `tip:`

### 5. 文档

- 新增 `docs/nginx-config.md` — 生产环境 nginx 反向代理 + WebSocket 配置
- 更新 `CLAUDE.md` — Vite 代理说明、构建部署工作流

### 6. 代码审查修复

| 问题 | 文件 | 修复 |
|------|------|------|
| `@Schema(name=...)` 与文件约定不一致 | `OrderDetailVO.java` | 改为 `@Schema(description=...)` |
| 调试 SELECT 残留在迁移 SQL | `runningerrands.sql` | 移除 |

## 涉及文件

```
backend/runningerrands.sql
backend/runningerrands-model/src/main/java/com/ikeu/model/
  ├── entity/Task.java
  ├── dto/TaskPublishDTO.java
  └── vo/{TaskDetailVO,TaskListVO,OrderDetailVO}.java
backend/runningerrands-server/src/main/java/com/ikeu/server/
  └── service/impl/{AdminServiceImpl,TaskServiceImpl}.java
admin/
  ├── vite.config.ts
  └── src/views/
      ├── tasks/TaskDetailView.vue
      ├── orders/OrderDetailView.vue
      └── users/UserDetailView.vue
mobile/pages/
  ├── order-completed/order-completed.vue
  ├── order-delivering/order-delivering.vue
  ├── order-waiting/order-waiting.vue
  ├── coffee-order/coffee-order.vue
  ├── general-publish/general-publish.vue
  ├── paper-express/paper-express.vue
  ├── print-order/print-order.vue
  └── service-publish/service-publish.vue
docs/
  ├── nginx-config.md (新增)
  └── 2026-06-10-work-summary.md (本文件)
.claude/CLAUDE.md
```

---

## 后续计划

### P0 — 用户端地址管理增强

当前地址簿只能选择预设地址（宿舍楼等），无法添加自定义地址。

**计划：**
- 地址管理页新增「自定义地址」入口
- 支持输入详细地址、联系人、联系电话、经纬度
- 自定义地址与预设地址统一管理，支持编辑和删除
- 发布任务时可选自定义地址

### P1 — 任务发布页体验优化

**1a. 联系人信息智能填充**

图书馆还书、办事代排等不需要选择收货地址的任务，联系人和电话需手动输入。改为自动填充地址簿默认地址的联系人和电话。

**1b. 发布页草稿自动保存**

所有发布页增加本地存储（localStorage）自动保存功能：
- 用户输入过程中实时保存表单状态
- 退出后重新进入发布页，自动恢复上次输入
- 成功发布后清除对应草稿

### P2 — 跑腿员信用分机制

**核心逻辑：**
- 初始信用分 100，满分 100
- 接单后超时未取货：-10 分/次
- 配送超时：-5 分/次
- 被投诉/差评：-10 分/次
- 成功完成订单：+1 分/次（上限 100）
- 低于 60 分禁止接单，显示冻结提示
- 信用分变动记录到 `credit_log` 表

### P3 — 用户端订单详情页改进（上期遗留）

- 三个详情页抽共享 `useTaskSpecs` composable（约 200 行重复代码）
- 统一 `info-card` 标签/值间距规范
- 为不同任务类型专属区块添加视觉区分（边框色/背景色）
- `service-publish.vue` 拆分（~900 行 → 按任务类型分子组件）

### P4 — 建议扩展功能

以下是我基于当前业务提出的扩展方向：

**4a. 任务大厅智能推荐**
- 基于用户历史发布偏好（类型、地址、预算）推荐匹配任务
- 常用地址一键发布，减少重复输入

**4b. 消息推送完善**
- 微信小程序订阅消息接入（任务被接、即将超时、已送达等关键节点）
- 站内通知支持分类筛选和批量已读

**4c. 数据看板增强**
- 管理端增加「跑腿员排行榜」（接单量、评分、收入）
- 用户端增加「我的数据」（发布统计、消费趋势）

**4d. 订单纠纷处理**
- 用户/跑腿员双方申诉通道
- 管理员裁决 + 退款处理
- 纠纷记录关联信用分

**4e. 批量发布/接单**
- 代取快递支持一键发布多个包裹（同一快递点）
- 跑腿员顺路接单推荐（路径匹配）

**4f. 评价体系完善**
- 双方互评（发布者评跑腿员 + 跑腿员评发布者）
- 评价标签化（态度好、速度快、包装仔细等）
- 匿名评价选项
