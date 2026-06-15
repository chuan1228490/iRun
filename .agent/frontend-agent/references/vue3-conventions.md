# Vue 3 Conventions

## Scope

适用于 `admin/` 和 `mobile/` 下所有 Vue 3 代码。

---

## `<script setup>` 语法

**Rule**: 所有组件使用 `<script setup lang="ts">`（管理端）或 `<script setup>`（移动端）。

### Good
```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
const count = ref(0)
const doubled = computed(() => count.value * 2)
onMounted(() => { console.log('mounted') })
</script>
```

### Bad
```vue
<script>
export default {
  data() { return { count: 0 } },    // ❌ Options API
  computed: { doubled() { ... } }
}
</script>
```

---

## Props & Emits

**Rule**: 使用 TypeScript interface 定义 Props，使用 `defineEmits` 定义事件。

### Good
```typescript
interface Props {
  userId: number
  userName?: string
  disabled?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  disabled: false
})
const emit = defineEmits<{
  (e: 'update', id: number): void
  (e: 'delete', id: number): void
}>()
```

### Bad
```typescript
const props = defineProps({ userId: Number })   // ❌ 没有类型推导
const emit = defineEmits(['update', 'delete'])   // ❌ 没有参数类型
```

---

## 响应式

**Rule**: 基本类型用 `ref`，对象用 `reactive` 或 `ref`，明确场景。

### Good
```typescript
const loading = ref(false)                        // 基本类型 ref
const form = reactive({ name: '', age: 0 })       // 表单对象 reactive
const list = ref<Item[]>([])                      // 数组 ref（方便整体替换）
```

### Bad
```typescript
const form = ref({ name: '', age: 0 })            // ❌ 对象用 ref 每次 .value 繁琐
```

---

## Composables

**Rule**: 以 `use` 开头，返回 ref/reactive/computed/function。

### Good
```typescript
// composables/useCountUp.ts
export function useCountUp(target: number, duration = 2000) {
  const displayValue = ref(0)
  function animate() { /* GSAP 动画逻辑 */ }
  return { displayValue, animate }
}
```

### Bad
```typescript
export function countUp(target: number) { ... }   // ❌ 不以 use 开头
```

---

## 生命周期

**Rule**: 使用 Composition API 生命周期钩子。

### Good
```typescript
onMounted(() => { fetchData() })
onUnmounted(() => { clearInterval(timer) })
watch(() => props.userId, (newId) => { fetchData(newId) })
```

### Bad
```typescript
mounted() { this.fetchData() }    // ❌ Options API
```
