# 2026/06/16 — 支付密码重构 + 常量类规范化 + 缺陷修复

## 完成工作

### 1. 常量类规范化

6 个常量类统一为 `public final class` + 私有构造器：

| 类 | 修改 |
|---|------|
| `StatusConstant` | ➕ `final`, ➕ 私有构造器 |
| `RedisConstant` | ➕ `final`, ➕ 私有构造器 |
| `TaskTypeConstant` | ➕ `final`（已有私有构造器） |
| `MessageConstant` | ➕ `final`, ➕ 私有构造器 |
| `JwtClaimsConstant` | ➕ `final`, ➕ 私有构造器 |
| `TaskSpecKeys` | 已是标杆 |

提交：`b201b4b`

### 2. 支付密码系统重构

**变更范围**：后端 6 文件 + 前端 4 文件

#### 首次设置支付密码 → 无需校验

- `SetPayPasswordDTO` 删除 `loginPassword`/`code` 字段，仅保留 `payPassword`（加 `@Pattern(regexp = "^\\d{6}$")` 服务端校验）
- `UserServiceImpl.setPayPassword` 精简为：查用户 → 判重复 → 直接加密存储（-25 行）
- 前端 `set` 模式移除微信用户/普通用户分支和全部验证字段

#### 忘记密码 → 短信验证码重置

| 端点 | 用途 | 校验链 |
|------|------|--------|
| `PUT /user/password/reset` | 重置登录密码 | userId 空值 → 用户存在 → 手机号归属 → SMS 验证码 |
| `PUT /user/pay-password/reset` | 重置支付密码 | 同上 |

- `ResetPasswordDTO` 复用为两种重置（`phone` + `code` + `newPassword`）
- 手机号归属校验：`user.getPhone().equals(dto.getPhone())` 防跨账户攻击
- `userId` 空值防御统一：`setPayPassword`/`changePayPassword`/`hasPayPassword` 补齐守卫

#### 前端交互

- `pay-password-edit.vue`：`login`/`change` 模式左下角蓝色"忘记密码？"链接 → 切换 `forgot` 模式（区分 login/pay 目标）
- `privacy-security.vue`：移除独立"忘记登录密码"入口（改为内联链接）
- `api/user.js`：`setPayPassword` 精简为单参数 + 新增 `resetPassword`/`resetPayPassword`

提交：`4c4b8e2`

### 3. 缺陷修复

#### 草稿自动保存三连修

| # | 问题 | 修复 |
|---|------|------|
| 1 | 每个发布页弹窗"已恢复未发布的草稿" | 5 个页面 `restoreDraft()` 改为静默调用 |
| 2 | 30 分钟 TTL（原 24h 过长，原注释不匹配） | `DRAFT_TTL_MS = 30 * 60 * 1000`，注释同步 |
| 3 | 页面 pop 后 watcher/timer/草稿残留 → 穿透 | `onUnload` → `dispose()` 同步清除 watcher + timer + localStorage |

根因：`dispose()` 只清理了 watcher 和 timer，未调 `clearDraft()`。页面 `onUnload` 后 localStorage 中草稿数据残留，下次进入同一页面仍在 TTL 内被恢复。修复后 `dispose()` 三步清理：timer → watchers → localStorage。

#### 组件 CSS 丢失

| # | 组件 | 修复 |
|---|------|------|
| 4 | `FeeCard.vue` | 新增 `<style scoped>` 含 `.fee-row`/`.chip`/`.custom-tip-*` 等全部依赖样式 |
| 5 | `GenderRestriction.vue` | 新增 `<style scoped>` 含 `.form-card`/`.chip-row`/`.chip--active` |

根因：`1e3848e` 提取组件时 CSS 留在父组件 `<style scoped>` 中，Vue scoped 不穿透子组件内部元素。

#### FeeCard NaN 守卫

`v-model.number` 清空输入时 emit `NaN` → `isNaN(val) ? 0 : val` 守卫。

### 4. 文档同步

- `docs/2026-06-09-fix-summary.md` — P0/P1/P2 全部标记完成，P3 更新状态
- `docs/2026-06-10-refactor-summary.md` — P0/P1b/P3 标记完成，其余标记进行中
- `docs/2026-06-15-work-summary.md` — P0 标记完成，新增 P0.5 安全加固计划（S1/S2），Git 记录补充

### 5. 代码审查（2 轮）

**第 1 轮** — `deep-code-reviewer`：8 项发现（1 CRITICAL 为设计决策，2 项已修复，2 项已记录为安全加固计划）

**第 2 轮** — 前端审查 `agent-skills:code-reviewer`：8 项发现（2 项已修复 — 注释更正 + NaN 守卫，2 项后续优化 — CSS 去重 + TTL 可配置）

## 涉及文件

```
backend/
  runningerrands-common/.../constant/
    MessageConstant.java              (+3 常量)
    StatusConstant.java               (final + 私有构造器)
    RedisConstant.java                (final + 私有构造器)
    TaskTypeConstant.java             (final)
    JwtClaimsConstant.java            (final + 私有构造器)
  runningerrands-model/.../dto/
    SetPayPasswordDTO.java            (精简, @Pattern)
    ResetPasswordDTO.java             (新建 — 复用)
  runningerrands-server/.../
    controller/user/UserController.java  (+2 端点)
    service/UserService.java             (+2 方法签名)
    service/impl/UserServiceImpl.java    (精简 + 2 新方法 + 3 处 null 守卫)

mobile/
  api/user.js                        (setPayPassword 精简, +resetPassword, +resetPayPassword)
  pages/pay-password-edit/           (set/change/login/forgot 四模式重构)
  components/
    FeeCard.vue                      (scoped CSS + NaN 守卫)
    GenderRestriction.vue            (scoped CSS)
  utils/draft-save.js                (TTL + onUnload + clearDraft)
  pages/*/ (5 个发布页)               (移除 draft toast)
  utils/campus-data.js               (删除重复函数)

docs/
  2026-06-09-fix-summary.md          (标记完成)
  2026-06-10-refactor-summary.md     (标记完成)
  2026-06-15-work-summary.md         (标记完成 + 安全加固计划)
  2026-06-16-work-summary.md         (本文件)
```

## Git 记录

```
4c4b8e2 feat: refactor password system — first-time pay password skip verification, add forgot password (login + pay) via SMS
b201b4b refactor: make all constant classes final with private constructors
```

## 后续计划

### P0 — 安全加固（06/16 审查发现）

| # | 内容 |
|---|------|
| S1 | 短信验证码 Redis key 操作作用域化（注册/登录/重置共用 `user:code:{phone}` 可跨操作复用） |
| S2 | 密码重置端点暴力破解防护（缺少失败计数锁定） |

### P1 — 往期遗留

| # | 内容 | 来源 |
|---|------|------|
| — | 跑腿员信用分机制 | 06-10 P2 |
| — | 管理端跑腿员详情页 / 系统设置页 | 06-15 P1 |
| — | 订单详情页任务类型视觉区分 | 06-15 P1 #7 |
| — | 未使用 import 清理 | 06-15 P1 #8 |

### P2 — 前端优化

| # | 内容 | 来源 |
|---|------|------|
| — | `FeeCard`/`GenderRestriction` 与 `service-publish.vue` CSS 去重 | 06/16 审查 |
| — | `service-publish.vue` onSubmit 拆分 | 多期遗留 |
| — | `displayDescription` 3 种变体统一 | 多期遗留 |

### P3 — 长期规划

同 06-15 P3（地图 API、智能推荐、消息推送、数据看板、纠纷处理、评价体系、批量发布、CI/CD）
