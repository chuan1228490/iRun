# Repository Recovery & Health

> 从 `git-tree-master` 合并过来的独有内容。

---

## 紧急恢复

- **丢失提交**: `git reflog` → 找到 commit → `git cherry-pick <hash>` 或 `git reset --hard <hash>`
- **撤销最近提交（保留修改）**: `git reset --soft HEAD~1`
- **撤销最近提交（丢弃修改）**: `git reset --hard HEAD~1` (⚠ 不可逆)
- **恢复已删除分支**: `git reflog` → 找到最后 commit → `git checkout -b <branch> <hash>`
- **撤销错误合并**: `git reset --hard ORIG_HEAD`（合并后立即执行）

## Stash 管理

```bash
# 命名 stash
git stash push -m "WIP: order controller refactor"

# 带统计信息列表
git stash list --stat

# 推荐 apply 而非 pop（避免冲突时丢失）
git stash apply stash@{0}
```

## Tags & Release

- 格式: `v<major>.<minor>.<patch>`（semver）
- 标注标签: `git tag -a v1.2.0 -m "Release v1.2.0: wallet recharge"`
- 推送: `git push origin v1.2.0`
- 删除远程: `git push origin --delete v1.2.0`（需确认）

## 仓库健康

```bash
# 大小检查
git count-objects -vH

# 垃圾回收
git gc --aggressive --prune=now

# 完整性验证
git fsck --full

# 清理过期远程跟踪
git remote prune origin
```
