---
name: "init"
description: "【最高优先级】项目初始化探索代理。新窗口/会话中首先调用，负责探索项目全貌、检查 Git 状态、分析代码问题。触发词例如：'启用现有agent探索检查项目'、'探索项目'、'初始化项目'、'检查项目状态'。"
model: sonnet
priority: highest
---

# Init Agent — 项目探索检查

## 最高优先级声明

**本代理是新会话中必须首先调用的入口代理。** 当用户输入类似以下任何触发词时，必须优先调用本代理再进行其他操作：

- `启用现有agent探索检查项目`
- `探索项目`
- `初始化项目`
- `检查项目状态`
- `探索项目架构`
- `project init`

## When to use this skill

**必须触发场景：**
- 新成员加入团队，首次打开项目
- 新会话开始，需要了解项目最新状态
- 长时间未接触项目，重新进入时需要更新认知
- 任何"先看看项目情况"类请求

**DO NOT use when:**
- 已有明确的具体修改任务（直接执行任务）
- 只是问一个局部问题（如"这个函数做什么"）
- 纯闲聊

## How to use this skill

```
用户: "启用现有agent探索检查项目"
  → Init Agent 启动
    → Phase 1: Git 状态检查 (必须)
    → Phase 2: 三端并行探索 (调用 project-explorer)
    → Phase 3: 代码问题分析 (调用 backend/frontend-code-reviewer)
    → Phase 4: 安全审计 (调用 security-auditor，如有敏感文件)
    → Phase 5: 汇总报告
```

## General Rules

### Phase 1: Git 状态（必须首先执行）

```bash
# 1. 获取最新远程状态
git fetch origin

# 2. 分支对比
git log --oneline -1 master
git log --oneline -1 origin/master
git log --oneline -1 origin/main
echo "本地 main vs 远程 main:"
git rev-list --count main..origin/main
echo "本地 master vs 远程 master:"
git rev-list --count master..origin/master

# 3. 未提交变更
git status --short
git stash list

# 4. 最近提交
git log --oneline -10
```

输出：分支同步状态表 + 未提交文件列表 + 需要注意的变更

### Phase 2: 三端并行探索

启动 3 个并行子代理，分别探索后端、管理端、移动端：

```
子代理 1 (backend): 按 backend-exploration-guide.md 探索 Spring Boot 架构
子代理 2 (admin):   按 admin-exploration-guide.md 探索 Vue 3 管理端
子代理 3 (mobile):  按 mobile-exploration-guide.md 探索 uni-app 移动端
```

汇总输出：每端的文件数、Controller/页面数、配置状态、结构偏差

### Phase 3: 代码问题分析

对最近变更的代码或关键模块调用审查代理：

- 如近期有 Java 变更 → 调用 `backend-code-reviewer`
- 如近期有前端变更 → 调用 `frontend-code-reviewer`
- 如近期有配置变更 → 调用 `security-auditor`

### Phase 4: 安全检查

扫描以下风险点：

- [ ] `.gitignore` 是否正确排除敏感文件（`application-dev.yml`, `.env`）
- [ ] `application.yml` 是否含硬编码密钥（如含 → 立即报告）
- [ ] 最近 commit 是否含敏感数据（passwords, tokens, keys）
- [ ] 依赖是否有已知 CVE（可选，如时间长可跳过）

### Phase 5: 汇总报告

生成以下格式的汇总表：

```markdown
# 项目健康检查报告 — {日期}

## Git 状态
| 分支 | 本地 | 远程 | 差异 |
|------|------|------|------|

## 项目结构
| 端 | 文件数 | 关键指标 | 问题数 |
|-----|--------|---------|--------|

## 代码问题（如有）
| 文件 | 严重度 | 问题描述 |

## 建议
1. 如果有未提交变更 → 建议提交或暂存
2. 如果分支不同步 → 建议 pull/rebase
3. 如果发现代码问题 → 建议执行对应修复
```

## CheckList

- [ ] Git fetch 完成，本地/远程分支状态已获取
- [ ] 三端探索完成，无关键文件缺失
- [ ] 未提交变更已识别
- [ ] 敏感信息未泄露（配置、最近 commit）
- [ ] 代码问题已标记
- [ ] 完整报告已输出

## 可用子代理清单

本代理可调用的所有子代理：

| 代理 | 用途 | 何时调用 |
|------|------|---------|
| `project-explorer` | 三端并行探索 | 了解项目结构 |
| `backend-code-reviewer` | Java 代码审查 | 后端代码问题 |
| `frontend-code-reviewer` | Vue/TS 代码审查 | 前端代码问题 |
| `security-auditor` | 安全审计 | 敏感信息检查 |
| `api-consistency-checker` | API 规范检查 | 接口一致性 |
| `autonomous-test-generator` | 测试生成/执行 | 验证修改影响 |
| `git-workflow-agent` | Git 工作流 | 分支/PR/提交 |
| `java-comment-enforcer` | Javadoc 补充 | 注释完善 |
