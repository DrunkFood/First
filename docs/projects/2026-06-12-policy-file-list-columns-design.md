# 政策文件管理列表字段调整

## 需求

编制中心政策文件管理页面（`PolicyFileList.vue`）字段调整：
1. 新增"文件大小"列（友好格式：1.5 MB）
2. "来源"列 → "上传人"列，显示 createName
3. "创建时间" → "上传时间"
4. 移除搜索区"来源"筛选项

## 改动范围

### 后端（2文件）

1. **PolicyFileController.java** — `list()` 返回类型 `Page<TbPolicyFile>` → `Page<PolicyFileVO>`
2. **PolicyFileServiceImpl.java** — `getPage()` 返回 `Page<PolicyFileVO>`，调用 toVO 转换

### 前端（1文件）

1. **PolicyFileList.vue** — 表格列调整 + 搜索条件移除 source

## 表格列变更

| 原列 | 新列 | 字段 |
|------|------|------|
| 文件名 | 文件名 | fileName |
| 文件分类 | 文件分类 | fileCategory |
| 适用类别 | 适用类别 | applicableCategory |
| 来源 | **上传人** | createName |
| (新增) | **文件大小** | fileSize |
| 创建时间 | **上传时间** | createTime |
| 状态 | 状态 | status |
| 操作 | 操作 | - |

## 不变项

- PolicyFileVO 已有 fileSize、createName、createTime 字段，无需新增
- 详情弹窗"来源"同步改为"上传人"
- 操作列删除按钮逻辑：用户上传的文件才显示删除（source !== 'SYSTEM' → 保留判断）
