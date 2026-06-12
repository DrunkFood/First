# Core 模块表前缀迁移设计文档

## 1. 背景

项目数据库表需按服务模块进行命名区分：

| 模块 | 表前缀 | 当前状态 |
|------|--------|----------|
| 支撑中心 (Support) | `sup_` | 已统一 |
| AI 服务 | `ai_` | 已统一 |
| 文件服务 (File) | `file_` | 已统一 |
| **核心服务 (Core)** | `tb_` | **当前使用 `ai_`，需迁移** |

Core 模块 6 张表当前使用 `ai_` 前缀，与 AI 模块表名无法区分，需统一改为 `tb_` 前缀。

## 2. 变更范围

### 2.1 实体类变更

| 原类名 | 新类名 | 原表名 | 新表名 | 文件夹 |
|--------|--------|--------|--------|--------|
| `AiProject` | `TbProject` | `ai_project` | `tb_project` | `entity/core/` |
| `AiRequirement` | `TbRequirement` | `ai_requirement` | `tb_requirement` | `entity/core/` |
| `AiReviewItem` | `TbProjectReviewItem` | `ai_review_item` | `tb_project_review_item` | `entity/core/` |
| `AiDetectionRecord` | `TbDetectionRecord` | `ai_detection_record` | `tb_detection_record` | `entity/core/` |
| `AiProjectVersion` | `TbProjectVersion` | `ai_project_version` | `tb_project_version` | `entity/core/` |
| `AiPolicyFile` | `TbPolicyFile` | `ai_policy_file` | `tb_policy_file` | `entity/core/` |

### 2.2 Mapper 接口变更

| 原名 | 新名 | 所在模块 |
|------|------|----------|
| `AiProjectMapper` | `TbProjectMapper` | core, support |
| `AiRequirementMapper` | `TbRequirementMapper` | core, support |
| `AiReviewItemMapper` | `TbProjectReviewItemMapper` | core |
| `AiDetectionRecordMapper` | `TbDetectionRecordMapper` | core, ai |
| `AiProjectVersionMapper` | `TbProjectVersionMapper` | core |
| `AiPolicyFileMapper` | `TbPolicyFileMapper` | core |

### 2.3 受影响文件清单

#### 实体类（6个，重命名 + 内容修改）
- `common/entity/core/AiProject.java` → `TbProject.java`
- `common/entity/core/AiRequirement.java` → `TbRequirement.java`
- `common/entity/core/AiReviewItem.java` → `TbProjectReviewItem.java`
- `common/entity/core/AiDetectionRecord.java` → `TbDetectionRecord.java`
- `common/entity/core/AiProjectVersion.java` → `TbProjectVersion.java`
- `common/entity/core/AiPolicyFile.java` → `TbPolicyFile.java`

#### Mapper 接口（9个）
- `core/mapper/AiProjectMapper.java` → `TbProjectMapper.java`
- `support/mapper/AiProjectMapper.java` → `TbProjectMapper.java`
- `core/mapper/AiRequirementMapper.java` → `TbRequirementMapper.java`
- `support/mapper/AiRequirementMapper.java` → `TbRequirementMapper.java`
- `core/mapper/AiReviewItemMapper.java` → `TbProjectReviewItemMapper.java`
- `core/mapper/AiDetectionRecordMapper.java` → `TbDetectionRecordMapper.java`
- `ai/mapper/AiDetectionRecordMapper.java` → `TbDetectionRecordMapper.java`
- `core/mapper/AiProjectVersionMapper.java` → `TbProjectVersionMapper.java`
- `core/mapper/AiPolicyFileMapper.java` → `TbPolicyFileMapper.java`

#### Service 接口（6个）
- `core/service/IProjectService.java`
- `core/service/IRequirementService.java`
- `core/service/IReviewItemService.java`
- `core/service/IDetectionService.java`
- `core/service/IProjectVersionService.java`
- `core/service/IPolicyFileService.java`

#### Service 实现（约13个）
- `core/service/impl/ProjectServiceImpl.java`
- `core/service/impl/RequirementServiceImpl.java`
- `core/service/impl/ReviewItemServiceImpl.java`
- `core/service/impl/DetectionServiceImpl.java`
- `core/service/impl/ProjectVersionServiceImpl.java`
- `core/service/impl/PolicyFileServiceImpl.java`
- `core/service/impl/DocumentIntegrationServiceImpl.java`
- `support/service/impl/StatisticsServiceImpl.java`

#### Controller（5个）
- `core/controller/ProjectController.java`
- `core/controller/RequirementController.java`
- `core/controller/ReviewItemController.java`
- `core/controller/PolicyFileController.java`
- （Detection 相关接口在 RequirementController 内）

#### DTO/VO（3个）
- `core/dto/response/ProjectDetailVO.java`
- `core/dto/response/RequirementDetailVO.java`

#### 状态机/触发器/调度器/引擎（约10个）
- `core/statemachine/ProjectStateMachine.java`
- `core/statemachine/PhaseFlowController.java`
- `core/statemachine/PhaseTrigger.java`
- `core/statemachine/trigger/BasicInfoTrigger.java`
- `core/statemachine/trigger/DocumentTrigger.java`
- `core/statemachine/trigger/DetectionPhaseTrigger.java`
- `core/statemachine/trigger/RequirementTrigger.java`
- `core/statemachine/trigger/ReviewItemTrigger.java`
- `core/scheduler/AiTaskResultSyncHandler.java`
- `core/engine/DocumentDataAssembler.java`

#### 工具类
- `core/util/DetectionResultParser.java`

#### SQL 脚本
- `core/sql/init.sql`

## 3. 数据库迁移

```sql
-- Core 模块表前缀迁移：ai_ → tb_
RENAME TABLE ai_project TO tb_project;
RENAME TABLE ai_requirement TO tb_requirement;
RENAME TABLE ai_review_item TO tb_project_review_item;
RENAME TABLE ai_detection_record TO tb_detection_record;
RENAME TABLE ai_project_version TO tb_project_version;
RENAME TABLE ai_policy_file TO tb_policy_file;
```

> 注意：RENAME TABLE 是原子操作，不会丢失数据。但执行前需确保没有活跃的长事务持有这些表的锁。

## 4. 执行策略

采用一次性批量迁移（方案 B），所有 6 张表同时变更。

### 执行步骤

```
1. 修改实体类（类名 + @TableName + 文件重命名）
2. 修改 Mapper 接口（类名 + 泛型引用 + 文件重命名）
3. 修改 Service 接口和实现（import + 类型引用 + 字段名引用）
4. 修改 Controller（import + 类型引用）
5. 修改 DTO/VO（import + 字段类型）
6. 修改状态机/触发器/调度器/引擎等
7. 修改工具类
8. 更新 SQL 初始化脚本
9. 编译验证（mvn clean compile 全模块）
10. 执行数据库 RENAME TABLE
11. 启动验证（逐模块启动确认）
```

## 5. 风险与回滚

### 风险点
1. **跨模块 Mapper 重名**：support 和 core 都有 `AiProjectMapper`，改名后都变 `TbProjectMapper`，需确认包路径不同不会冲突
2. **数据库 RENAME 时机**：代码部署和数据库变更需同步，中间不能有请求打到旧表名
3. **MyBatis-Plus 缓存**：Mapper 缓存可能需要重启服务清除

### 回滚方案
1. 代码回滚：`git revert` 本次提交
2. 数据库回滚：反向 RENAME TABLE（`tb_xxx` → `ai_xxx`，`tb_project_review_item` → `ai_review_item`）
3. 两者需同步执行

## 6. 验证标准

1. 全模块 `mvn clean compile` 编译通过
2. 数据库 6 张表成功 RENAME
3. 各服务启动无报错
4. 核心业务接口测试通过（项目 CRUD、需求管理、评审项、检测）
