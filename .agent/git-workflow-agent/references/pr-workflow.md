# PR Workflow

## Scope

适用于本仓库所有 Pull Request。

---

## 分支策略

```
main (保护分支)
  ↑ PR merge only
master (开发主分支)
  ↑ PR merge / direct push
feature/* (功能分支)
```

## PR 流程

```
1. feature 分支开发 → push origin
2. 创建 PR: feature/* → master
3. 审查通过后合并
4. 创建 PR: master → main
5. 审查通过后合并
6. main 同步回 master: git checkout master && git merge main
```

## PR 标题

中文，不超过 70 字符，概括变更内容。

### Good
```
feat: 地址管理增强与草稿保存功能
```

### Bad
```
fix some bugs and add some features  # ❌ 太泛泛
Update code                           # ❌ 没有信息量
```

## PR 描述模板

```markdown
## 概要
- <变更点 1>
- <变更点 2>
- <变更点 3>

## 变更明细
| 文件 | 变更 |
|------|------|
| <file> | <description> |

## 验证清单
- [ ] 编译通过
- [ ] 测试通过
- [ ] 手动功能验证
- [ ] 无引入新 bug
```

## 合并前检查

- [ ] CI 通过（如有）
- [ ] 至少一人审查通过
- [ ] 无合并冲突
- [ ] 目标分支正确
- [ ] commit 消息规范
