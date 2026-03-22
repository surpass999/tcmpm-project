# EMR 模块依赖清单

> 本文档记录若依 RuoYi-Vue-Pro + Vben Admin 电子病历系统（EMR）的所有依赖

## 📦 Canvas Editor 相关依赖

### 运行时依赖

| 包名 | 版本 | 用途 | 位置 | 标签 |
|------|------|------|------|------|
| `prismjs` | ^1.29.0 | 代码高亮（用于病历中的代码片段） | `apps/web-antd` | `[EMR]` |
| `@types/prismjs` | ^1.26.0 | PrismJS TypeScript 类型定义 | `apps/web-antd` | `[EMR]` |

### 依赖来源

所有依赖版本在根目录 `pnpm-workspace.yaml` 的 `catalog` 字段中统一管理。

### 查找 EMR 依赖

```bash
# 方法 1: 搜索 [EMR] 标签
cd apps/web-antd
grep "\[EMR\]" package.json

# 方法 2: 搜索 catalog 中的 EMR 注释
cd /www/wwwroot/ruoyi-vue/base-ui/base-ui-admin-vben
grep -i "emr\|canvas-editor" pnpm-workspace.yaml
```

## 🔧 EMR 源码包

### 本地源码包

| 包名 | 路径 | 说明 |
|------|------|------|
| `@vben-core/emr-editor` | `packages/@core/emr-editor/` | Canvas Editor 本地源码，专为 EMR 定制 |

### 使用方式

```typescript
// 直接引入源码（推荐）
import Editor from '@vben-core/emr-editor/src/editor/core/Editor'
import '@vben-core/emr-editor/src/style.css'

// 初始化编辑器
const editor = new Editor(containerElement, options)
```

## 📝 如何添加新的 EMR 依赖

### 步骤 1: 在 catalog 中添加版本

编辑 `pnpm-workspace.yaml`:

```yaml
catalog:
  # ... 现有依赖
  
  # [EMR] 新增依赖说明
  new-package: ^1.0.0
```

### 步骤 2: 在 web-antd 中声明依赖

编辑 `apps/web-antd/package.json`:

```json
{
  "dependencies": {
    "new-package": "catalog:",  // [EMR] 依赖说明
  }
}
```

### 步骤 3: 更新本文档

在上面的表格中添加新依赖的记录。

### 步骤 4: 安装依赖

```bash
cd /www/wwwroot/ruoyi-vue/base-ui/base-ui-admin-vben
pnpm install
```

## 🗑️ 如何移除 EMR 模块

### 完整卸载步骤

```bash
# 1. 删除 EMR 编辑器源码
rm -rf packages/@core/emr-editor

# 2. 删除 web-antd 中的 EMR 依赖
# 编辑 apps/web-antd/package.json，删除所有带 [EMR] 标签的依赖

# 3. 删除 catalog 中的 EMR 依赖（可选，如果其他模块不用）
# 编辑 pnpm-workspace.yaml，删除 prismjs 等

# 4. 删除 EMR 业务代码
rm -rf apps/web-antd/src/views/emr/
rm -rf apps/web-antd/src/api/emr/

# 5. 重新安装依赖
pnpm install

# 6. 删除本文档目录
rm -rf docs/emr/
```

## 📊 依赖分析

### 为什么使用本地源码？

| 优点 | 说明 |
|------|------|
| ✅ 完全可控 | 可随时修改和扩展源码 |
| ✅ 深度定制 | 便于添加医疗行业特定功能（ICD-10、电子签名等） |
| ✅ 调试方便 | 可直接在源码中断点调试 |
| ✅ 无需等待 | 不用等待官方发布新版本 |

| 缺点 | 说明 |
|------|------|
| ⚠️ 自行维护 | 需要手动合并上游更新 |
| ⚠️ 构建依赖 | 需要本地构建环境 |

### 依赖提升策略

采用 **方案 B-5（catalog + 标签 + 文档）**:

- ✅ 符合 Vben Admin monorepo 架构
- ✅ 版本统一管理，避免冲突
- ✅ `[EMR]` 标签清晰标识
- ✅ 文档记录完整

## 🔗 相关文档

- [Canvas Editor 官方文档](https://hufe.club/canvas-editor/)
- [Canvas Editor GitHub](https://github.com/Hufe921/canvas-editor)
- [Vben Admin 文档](https://doc.vben.pro/)
- EMR 编辑器使用说明：`packages/@core/emr-editor/README_VBEN.md`
- 若依后端 EMR 模块文档：`/www/wwwroot/ruoyi-vue/base-module-emr/README.md`（待创建）

## �� 维护记录

| 日期 | 操作 | 操作人 | 说明 |
|------|------|--------|------|
| 2026-01-20 | 初始化 | - | 创建 EMR 依赖管理文档，添加 prismjs 依赖 |

---

**维护提示**: 每次添加/删除 EMR 相关依赖时，请及时更新本文档。
