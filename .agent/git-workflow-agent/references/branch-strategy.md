# Branch Strategy

## Scope

适用于本仓库所有分支管理操作。

---

## 1. 分支模型

```
main (保护分支 — 生产就绪)
  ↑ PR merge only
master (开发主分支 — 功能集成)
  ↑ PR merge / direct push
feature/* (功能分支)
fix/* (修复分支)
```

## 2. 分支命名

| 类型 | 格式 | 示例 |
|------|------|------|
| 功能 | `feature/<module>-<desc>` | `feature/order-wallet-pay` |
| 修复 | `fix/<module>-<desc>` | `fix/auth-token-refresh` |
| 紧急修复 | `hotfix/<version>-<desc>` | `hotfix/v1.2.1-null-pointer` |

Desc 部分使用英文小写 + 短横线，简明描述变更内容。

### Good
```
feature/task-hall-cache
fix/user-login-lock
hotfix/v1.3.0-payment-race
```

### Bad
```
feature/添加支付功能          # ❌ 中文
mybranch                      # ❌ 无前缀
fix/bug                       # ❌ 含义不清
```

## 3. 分支操作规则

| 操作 | 允许 | 条件 |
|------|------|------|
| push master | 是 | 编译通过 |
| push main | **禁止** | 只能通过 PR 合入 |
| force push | 谨慎 | 绝不 force push main |
| merge --no-ff | 推荐 | 保留分支历史 |
| rebase master | 允许 | 保持历史线性，确认后执行 |
| 删除分支 | 是 | 确认已合并后删除 |

## 4. 分支同步

```
# 功能分支开发中同步最新 master
git checkout master && git pull origin master
git checkout feature/xxx && git rebase master

# PR 合并后同步 main → master
git checkout main && git pull origin main
git checkout master && git merge main && git push origin master
```

## 5. 分支清理

```bash
# 列出已合并到 main 的分支
git branch --merged main | grep -v "main\|master"

# 删除本地分支
git branch -d feature/xxx

# 删除远程分支
git push origin --delete feature/xxx

# 清理本地过期的远程跟踪
git remote prune origin
```
