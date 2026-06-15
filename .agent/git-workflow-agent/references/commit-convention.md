# Commit Convention

## Scope

适用于本仓库所有 commit，严格遵守 Conventional Commits 1.0。

---

## 格式

```
<type>(<scope>): <description>

[body]

[footer]
```

### Type

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(order): add wallet recharge API` |
| `fix` | Bug 修复 | `fix(auth): resolve token refresh race condition` |
| `docs` | 文档变更 | `docs: add API usage guide` |
| `refactor` | 重构（不改变功能） | `refactor(user): extract UserValidator` |
| `test` | 测试 | `test(order): add order state machine tests` |
| `chore` | 构建/工具/依赖 | `chore: upgrade Spring Boot to 3.2.1` |
| `style` | 格式（空格/换行/注释） | `style(dto): unify blank line formatting` |
| `perf` | 性能优化 | `perf(task): add Redis cache for task hall` |
| `ci` | CI/CD | `ci: add GitHub Actions test workflow` |

### Scope

本项目常用 scope：
`auth`, `order`, `task`, `user`, `admin`, `mobile`, `model`, `config`, `dashboard`, `notification`, `common`

### Description
- 祈使语气，小写开头
- 不超过 72 字符
- 不以句号结尾
- 中文英文均可，保持一致性

### Body（可选）
- 解释 **为什么** 而不是 **做了什么**
- 每行不超过 72 字符

## Good

```
feat(order): add wallet recharge and deduction API

Use Redisson distributed lock to prevent concurrent balance
updates. Deduction validates pay password before proceeding.
```

```
fix(auth): prevent parallel token refresh from invalidating active tokens

Use isRefreshing flag + refreshQueue to serialize concurrent
401 responses, ensuring only one refresh call executes while
others wait for the result.
```

```
docs: add application-template.yml developer onboarding guide
```

## Bad

```
Added new feature           # ❌ 缺少 type
feat: add stuff             # ❌ 缺少 scope
feat(order): add wallet recharge API.  # ❌ 以句号结尾
FEAT(ORDER): ADD WALLET     # ❌ 大写
fix: 修复了一个问题          # ❌ scope 缺失
```
