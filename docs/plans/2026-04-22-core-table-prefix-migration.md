# Core 模块表前缀迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Core 模块 6 张表从 `ai_` 前缀迁移为 `tb_` 前缀，实体类名从 `AiXxx` 改为 `TbXxx`，全链路引用同步更新。

**Architecture:** 一次性批量重命名所有实体类、Mapper 接口、Service/Controller/DTO 引用，更新 SQL 硬编码表名，最后执行数据库 RENAME TABLE。所有代码变更在一次提交中完成。

**Tech Stack:** Java 21 · MyBatis-Plus 3.5.5 · Spring Boot 3.2.2 · MySQL 8.4.0

---

## 变更映射表

| 原类名 | 新类名 | 原表名 | 新表名 |
|--------|--------|--------|--------|
| `AiProject` | `TbProject` | `ai_project` | `tb_project` |
| `AiRequirement` | `TbRequirement` | `ai_requirement` | `tb_requirement` |
| `AiReviewItem` | `TbProjectReviewItem` | `ai_review_item` | `tb_project_review_item` |
| `AiDetectionRecord` | `TbDetectionRecord` | `ai_detection_record` | `tb_detection_record` |
| `AiProjectVersion` | `TbProjectVersion` | `ai_project_version` | `tb_project_version` |
| `AiPolicyFile` | `TbPolicyFile` | `ai_policy_file` | `tb_policy_file` |

---

### Task 1: 重命名实体类文件

**Files:**
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiProject.java` → `TbProject.java`
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiRequirement.java` → `TbRequirement.java`
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiReviewItem.java` → `TbProjectReviewItem.java`
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiDetectionRecord.java` → `TbDetectionRecord.java`
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiProjectVersion.java` → `TbProjectVersion.java`
- Rename: `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/core/AiPolicyFile.java` → `TbPolicyFile.java`

- [ ] **Step 1: 重命名 AiProject.java → TbProject.java**

文件重命名后，内容替换：

```java
// 替换类声明和注解
/**
 * AI编制项目实体
 * 对应表: tb_project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_project")
@Schema(description = "AI编制项目")
public class TbProject extends BaseEntity {
```

- [ ] **Step 2: 重命名 AiRequirement.java → TbRequirement.java**

```java
/**
 * 业务需求实体
 * 对应表: tb_requirement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_requirement")
@Schema(description = "业务需求")
public class TbRequirement extends BaseEntity {
```

- [ ] **Step 3: 重命名 AiReviewItem.java → TbProjectReviewItem.java**

```java
/**
 * 评审项实体
 * 对应表: tb_project_review_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_project_review_item")
@Schema(description = "评审项")
public class TbProjectReviewItem extends BaseEntity {
```

- [ ] **Step 4: 重命名 AiDetectionRecord.java → TbDetectionRecord.java**

```java
/**
 * 检测记录实体（Core模块视图）
 * 对应表: tb_detection_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_detection_record")
@Schema(description = "检测记录")
public class TbDetectionRecord extends BaseEntity {
```

- [ ] **Step 5: 重命名 AiProjectVersion.java → TbProjectVersion.java**

```java
/**
 * 项目版本实体
 * 对应表: tb_project_version
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_project_version")
@Schema(description = "项目版本")
public class TbProjectVersion extends BaseEntity {
```

- [ ] **Step 6: 重命名 AiPolicyFile.java → TbPolicyFile.java**

```java
/**
 * 用户政策文件实体
 * 对应表: tb_policy_file
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_policy_file")
@Schema(description = "用户政策文件")
public class TbPolicyFile extends BaseEntity {
```

- [ ] **Step 7: 安装 common 模块**

```bash
cd ele-ai-tender-system && mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

---

### Task 2: 重命名 Mapper 接口文件

**Files:**
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiProjectMapper.java` → `TbProjectMapper.java`
- Rename: `ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/mapper/AiProjectMapper.java` → `TbProjectMapper.java`
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiRequirementMapper.java` → `TbRequirementMapper.java`
- Rename: `ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/mapper/AiRequirementMapper.java` → `TbRequirementMapper.java`
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiReviewItemMapper.java` → `TbProjectReviewItemMapper.java`
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiDetectionRecordMapper.java` → `TbDetectionRecordMapper.java`
- Rename: `ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/mapper/AiDetectionRecordMapper.java` → `TbDetectionRecordMapper.java`
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiProjectVersionMapper.java` → `TbProjectVersionMapper.java`
- Rename: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/mapper/AiPolicyFileMapper.java` → `TbPolicyFileMapper.java`

- [ ] **Step 1: core/TbProjectMapper.java**

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI编制项目Mapper
 */
@Mapper
public interface TbProjectMapper extends BaseMapper<TbProject> {
}
```

- [ ] **Step 2: support/TbProjectMapper.java**

```java
package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI编制项目Mapper（支撑中心统计用）
 */
@Mapper
public interface TbProjectMapper extends BaseMapper<TbProject> {
}
```

- [ ] **Step 3: core/TbRequirementMapper.java**

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务需求Mapper
 */
@Mapper
public interface TbRequirementMapper extends BaseMapper<TbRequirement> {

}
```

- [ ] **Step 4: support/TbRequirementMapper.java**

```java
package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务需求Mapper（支撑中心统计用）
 */
@Mapper
public interface TbRequirementMapper extends BaseMapper<TbRequirement> {
}
```

- [ ] **Step 5: core/TbProjectReviewItemMapper.java**

注意：SQL 硬编码的表名也要从 `ai_review_item` 改为 `tb_project_review_item`。

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评审项Mapper
 */
@Mapper
public interface TbProjectReviewItemMapper extends BaseMapper<TbProjectReviewItem> {

    /**
     * 根据项目ID查询评审项列表
     */
    @Select("SELECT * FROM tb_project_review_item WHERE project_id = #{projectId} AND is_delete = 0 ORDER BY sort_order ASC, id ASC")
    List<TbProjectReviewItem> selectByProjectId(@Param("projectId") Long projectId);
}
```

- [ ] **Step 6: core/TbDetectionRecordMapper.java**

注意：SQL 硬编码的表名也要从 `ai_detection_record` 改为 `tb_detection_record`。

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检测记录Mapper（Core模块）
 */
@Mapper
public interface TbDetectionRecordMapper extends BaseMapper<TbDetectionRecord> {

    /**
     * 根据项目ID查询检测记录
     */
    @Select("SELECT * FROM tb_detection_record WHERE project_id = #{projectId} AND is_delete = 0 ORDER BY id ASC")
    List<TbDetectionRecord> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM tb_detection_record WHERE requirement_id = #{requirementId} AND is_delete = 0 ORDER BY id ASC")
    List<TbDetectionRecord> selectByRequirementId(@Param("requirementId") Long requirementId);
}
```

- [ ] **Step 7: ai/TbDetectionRecordMapper.java**

```java
package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbDetectionRecordMapper extends BaseMapper<TbDetectionRecord> {
}
```

- [ ] **Step 8: core/TbProjectVersionMapper.java**

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProjectVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目版本Mapper
 */
@Mapper
public interface TbProjectVersionMapper extends BaseMapper<TbProjectVersion> {
}
```

- [ ] **Step 9: core/TbPolicyFileMapper.java**

```java
package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbPolicyFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户政策文件Mapper（Core模块）
 */
@Mapper
public interface TbPolicyFileMapper extends BaseMapper<TbPolicyFile> {
}
```

---

### Task 3: 更新 Service 接口

**Files:**
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectService.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IRequirementService.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IReviewItemService.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IPolicyFileService.java`

- [ ] **Step 1: IProjectService.java — 替换所有 AiProject → TbProject**

import 替换:
```java
import com.jy.eleaitender.common.entity.core.TbProject;
```

方法签名替换:
```java
Page<TbProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory);
TbProject getById(Long id);
TbProject create(TbProject project);
void update(Long id, TbProject project);
```

- [ ] **Step 2: IRequirementService.java — 替换所有 AiRequirement → TbRequirement, AiDetectionRecord → TbDetectionRecord**

import 替换:
```java
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import com.jy.eleaitender.common.entity.core.TbRequirement;
```

方法签名替换:
```java
Page<TbRequirement> getPage(Integer pageNum, Integer pageSize, String requirementName, String status);
TbRequirement getById(Long id);
TbRequirement create(TbRequirement requirement);
void update(Long id, TbRequirement requirement);
TbRequirement matchTemplate(Long id, Long matchedFileId, String matchMode);
List<TbDetectionRecord> getDetectionRecords(Long requirementId);
```

- [ ] **Step 3: IReviewItemService.java — 替换所有 AiReviewItem → TbProjectReviewItem**

import 替换:
```java
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
```

方法签名替换:
```java
List<TbProjectReviewItem> getTreeByProjectId(Long projectId);
TbProjectReviewItem create(TbProjectReviewItem reviewItem);
void update(Long id, TbProjectReviewItem reviewItem);
AiTask submitGenerate(Long projectId, Map<String, Object> params);
void batchCreate(List<TbProjectReviewItem> items);
void batchUpdate(List<TbProjectReviewItem> items);
```

- [ ] **Step 4: IProjectVersionService.java — 替换所有 AiProjectVersion → TbProjectVersion**

import 替换:
```java
import com.jy.eleaitender.common.entity.core.TbProjectVersion;
```

方法签名替换:
```java
List<TbProjectVersion> getByProjectId(Long projectId);
TbProjectVersion createVersion(Long projectId, String contentSnapshot, String changeDescription);
```

- [ ] **Step 5: IPolicyFileService.java — 替换所有 AiPolicyFile → TbPolicyFile**

import 替换:
```java
import com.jy.eleaitender.common.entity.core.TbPolicyFile;
```

方法签名替换:
```java
Page<TbPolicyFile> getPage(Integer pageNum, Integer pageSize, String fileCategory, String applicableCategory);
TbPolicyFile getById(Long id);
TbPolicyFile create(TbPolicyFile policyFile);
```

---

### Task 4: 更新 Service 实现类

**Files:**
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/RequirementServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ReviewItemServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/DetectionServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/PolicyFileServiceImpl.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/DocumentIntegrationServiceImpl.java`
- Modify: `ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/service/impl/StatisticsServiceImpl.java`

- [ ] **Step 1: ProjectServiceImpl.java — 全局替换**

替换规则（按顺序执行，避免部分匹配）:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
3. 所有 `AiProject` → `TbProject`
4. 所有 `AiProjectMapper` → `TbProjectMapper`
5. 字段声明 `private AiProjectMapper` → `private TbProjectMapper`

- [ ] **Step 2: RequirementServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
2. `import com.jy.eleaitender.common.entity.core.AiDetectionRecord;` → `import com.jy.eleaitender.common.entity.core.TbDetectionRecord;`
3. `import com.jy.eleaitender.core.mapper.AiRequirementMapper;` → `import com.jy.eleaitender.core.mapper.TbRequirementMapper;`
4. `import com.jy.eleaitender.core.mapper.AiDetectionRecordMapper;` → `import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;`
5. 所有 `AiRequirement` → `TbRequirement`
6. 所有 `AiDetectionRecord` → `TbDetectionRecord`
7. 所有 `AiRequirementMapper` → `TbRequirementMapper`
8. 所有 `AiDetectionRecordMapper` → `TbDetectionRecordMapper`

- [ ] **Step 3: ReviewItemServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
2. `import com.jy.eleaitender.core.mapper.AiReviewItemMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;`
3. 所有 `AiReviewItem` → `TbProjectReviewItem`
4. 所有 `AiReviewItemMapper` → `TbProjectReviewItemMapper`

- [ ] **Step 4: DetectionServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiDetectionRecord;` → `import com.jy.eleaitender.common.entity.core.TbDetectionRecord;`
3. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
4. `import com.jy.eleaitender.core.mapper.AiDetectionRecordMapper;` → `import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;`
5. 所有 `AiProject` → `TbProject`
6. 所有 `AiDetectionRecord` → `TbDetectionRecord`
7. 所有 `AiProjectMapper` → `TbProjectMapper`
8. 所有 `AiDetectionRecordMapper` → `TbDetectionRecordMapper`

- [ ] **Step 5: ProjectVersionServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiProjectVersion;` → `import com.jy.eleaitender.common.entity.core.TbProjectVersion;`
2. `import com.jy.eleaitender.core.mapper.AiProjectVersionMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectVersionMapper;`
3. 所有 `AiProjectVersion` → `TbProjectVersion`
4. 所有 `AiProjectVersionMapper` → `TbProjectVersionMapper`

- [ ] **Step 6: PolicyFileServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiPolicyFile;` → `import com.jy.eleaitender.common.entity.core.TbPolicyFile;`
2. `import com.jy.eleaitender.core.mapper.AiPolicyFileMapper;` → `import com.jy.eleaitender.core.mapper.TbPolicyFileMapper;`
3. 所有 `AiPolicyFile` → `TbPolicyFile`
4. 所有 `AiPolicyFileMapper` → `TbPolicyFileMapper`

- [ ] **Step 7: DocumentIntegrationServiceImpl.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
3. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
4. `import com.jy.eleaitender.core.mapper.AiReviewItemMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;`
5. 所有 `AiProject` → `TbProject`
6. 所有 `AiReviewItem` → `TbProjectReviewItem`
7. 所有 `AiProjectMapper` → `TbProjectMapper`
8. 所有 `AiReviewItemMapper` → `TbProjectReviewItemMapper`

- [ ] **Step 8: StatisticsServiceImpl.java (support模块) — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.support.mapper.AiProjectMapper;` → `import com.jy.eleaitender.support.mapper.TbProjectMapper;`
3. 所有 `AiProject` → `TbProject`
4. 所有 `AiProjectMapper` → `TbProjectMapper`

---

### Task 5: 更新 Controller

**Files:**
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ProjectController.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/RequirementController.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ReviewItemController.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/PolicyFileController.java`

- [ ] **Step 1: ProjectController.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiProjectVersion;` → `import com.jy.eleaitender.common.entity.core.TbProjectVersion;`
3. 所有 `AiProject` → `TbProject`
4. 所有 `AiProjectVersion` → `TbProjectVersion`

- [ ] **Step 2: RequirementController.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
2. `import com.jy.eleaitender.common.entity.core.AiDetectionRecord;` → `import com.jy.eleaitender.common.entity.core.TbDetectionRecord;`
3. 所有 `AiRequirement` → `TbRequirement`
4. 所有 `AiDetectionRecord` → `TbDetectionRecord`

- [ ] **Step 3: ReviewItemController.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
2. 所有 `AiReviewItem` → `TbProjectReviewItem`

- [ ] **Step 4: PolicyFileController.java — 全局替换**

替换规则:
1. `import com.jy.eleaitender.common.entity.core.AiPolicyFile;` → `import com.jy.eleaitender.common.entity.core.TbPolicyFile;`
2. 所有 `AiPolicyFile` → `TbPolicyFile`

---

### Task 6: 更新 DTO/VO

**Files:**
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/ProjectDetailVO.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/RequirementDetailVO.java`

- [ ] **Step 1: ProjectDetailVO.java**

```java
package com.jy.eleaitender.core.dto.response;

import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "项目详情")
public class ProjectDetailVO {
    @Schema(description = "项目信息")
    private TbProject project;
    @Schema(description = "关联的业务需求")
    private TbRequirement requirement;
}
```

- [ ] **Step 2: RequirementDetailVO.java**

```java
package com.jy.eleaitender.core.dto.response;

import com.jy.eleaitender.common.entity.core.TbRequirement;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "需求详情")
public class RequirementDetailVO {
    @Schema(description = "需求信息")
    private TbRequirement requirement;
    @Schema(description = "匹配的模板")
    private SupTemplate matchedTemplate;
}
```

---

### Task 7: 更新状态机/触发器/调度器/引擎

**Files:**
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/trigger/BasicInfoTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/trigger/DocumentTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/trigger/DetectionPhaseTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/trigger/RequirementTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/trigger/ReviewItemTrigger.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java`
- Modify: `ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/util/DetectionResultParser.java`

- [ ] **Step 1: ProjectStateMachine.java — 替换 AiProject → TbProject**

import 和方法签名中:
1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 2: PhaseFlowController.java — 替换 AiProject → TbProject**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 3: PhaseTrigger.java — 替换 AiProject → TbProject**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 4: BasicInfoTrigger.java — 替换 AiProject → TbProject**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 5: DocumentTrigger.java — 替换 AiProject → TbProject**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 6: DetectionPhaseTrigger.java — 替换 AiProject → TbProject**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. 所有 `AiProject` → `TbProject`

- [ ] **Step 7: RequirementTrigger.java — 多实体替换**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
3. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
4. `import com.jy.eleaitender.core.mapper.AiRequirementMapper;` → `import com.jy.eleaitender.core.mapper.TbRequirementMapper;`
5. 所有 `AiProject` → `TbProject`
6. 所有 `AiRequirement` → `TbRequirement`
7. 所有 `AiProjectMapper` → `TbProjectMapper`
8. 所有 `AiRequirementMapper` → `TbRequirementMapper`
9. 字段 `private AiProjectMapper aiProjectMapper;` → `private TbProjectMapper tbProjectMapper;`
10. 字段 `private AiRequirementMapper requirementMapper;` → `private TbRequirementMapper requirementMapper;`
11. 方法体中引用 `aiProjectMapper.` → `tbProjectMapper.`
12. 方法体中引用 `requirementMapper.` → `requirementMapper.` (名称不变，类型变了)

- [ ] **Step 8: ReviewItemTrigger.java — 多实体替换**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
3. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
4. `import com.jy.eleaitender.core.mapper.AiRequirementMapper;` → `import com.jy.eleaitender.core.mapper.TbRequirementMapper;`
5. `import com.jy.eleaitender.core.mapper.AiReviewItemMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;`
6. 所有 `AiProject` → `TbProject`
7. 所有 `AiRequirement` → `TbRequirement`
8. 所有 `AiReviewItem` → `TbProjectReviewItem`
9. 所有 `AiRequirementMapper` → `TbRequirementMapper`
10. 所有 `AiReviewItemMapper` → `TbProjectReviewItemMapper`
11. 字段 `private AiReviewItemMapper reviewItemMapper;` → `private TbProjectReviewItemMapper reviewItemMapper;`
12. 字段 `private AiRequirementMapper requirementMapper;` → `private TbRequirementMapper requirementMapper;`

- [ ] **Step 9: AiTaskResultSyncHandler.java — 多实体替换**

1. `import com.jy.eleaitender.common.entity.core.AiDetectionRecord;` → `import com.jy.eleaitender.common.entity.core.TbDetectionRecord;`
2. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
3. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
4. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
5. `import com.jy.eleaitender.core.mapper.AiDetectionRecordMapper;` → `import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;`
6. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
7. `import com.jy.eleaitender.core.mapper.AiRequirementMapper;` → `import com.jy.eleaitender.core.mapper.TbRequirementMapper;`
8. `import com.jy.eleaitender.core.mapper.AiReviewItemMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;`
9. 所有 `AiDetectionRecord` → `TbDetectionRecord`
10. 所有 `AiProject` → `TbProject`
11. 所有 `AiRequirement` → `TbRequirement`
12. 所有 `AiReviewItem` → `TbProjectReviewItem`
13. 所有 `AiDetectionRecordMapper` → `TbDetectionRecordMapper`
14. 所有 `AiProjectMapper` → `TbProjectMapper`
15. 所有 `AiRequirementMapper` → `TbRequirementMapper`
16. 所有 `AiReviewItemMapper` → `TbProjectReviewItemMapper`
17. 字段 `private AiRequirementMapper requirementMapper;` → `private TbRequirementMapper requirementMapper;`
18. 字段 `private AiReviewItemMapper reviewItemMapper;` → `private TbProjectReviewItemMapper reviewItemMapper;`
19. 字段 `private AiDetectionRecordMapper detectionRecordMapper;` → `private TbDetectionRecordMapper detectionRecordMapper;`
20. 字段 `private AiProjectMapper projectMapper;` → `private TbProjectMapper projectMapper;`
21. `IdentityHashMap<AiReviewItem, AiReviewItem>` → `IdentityHashMap<TbProjectReviewItem, TbProjectReviewItem>`
22. 方法体中 `new AiReviewItem()` → `new TbProjectReviewItem()`
23. 方法体中 `List<AiReviewItem>` → `List<TbProjectReviewItem>`
24. 方法体中 `AiReviewItem parent = parentMap.get(item);` → `TbProjectReviewItem parent = parentMap.get(item);`
25. 方法体中 `AiProject project = projectMapper.selectById(projectId);` → `TbProject project = projectMapper.selectById(projectId);`
26. 方法体中 `AiRequirement requirement = requirementMapper.selectById(requirementId);` → `TbRequirement requirement = requirementMapper.selectById(requirementId);`
27. 方法体中 `AiDetectionRecord record = detectionRecordMapper.selectById(recordId);` → `TbDetectionRecord record = detectionRecordMapper.selectById(recordId);`
28. 方法体中 `List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);` → `List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);`

- [ ] **Step 10: DocumentDataAssembler.java — 多实体替换**

1. `import com.jy.eleaitender.common.entity.core.AiProject;` → `import com.jy.eleaitender.common.entity.core.TbProject;`
2. `import com.jy.eleaitender.common.entity.core.AiRequirement;` → `import com.jy.eleaitender.common.entity.core.TbRequirement;`
3. `import com.jy.eleaitender.common.entity.core.AiReviewItem;` → `import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;`
4. `import com.jy.eleaitender.core.mapper.AiProjectMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectMapper;`
5. `import com.jy.eleaitender.core.mapper.AiRequirementMapper;` → `import com.jy.eleaitender.core.mapper.TbRequirementMapper;`
6. `import com.jy.eleaitender.core.mapper.AiReviewItemMapper;` → `import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;`
7. 所有 `AiProject` → `TbProject`
8. 所有 `AiRequirement` → `TbRequirement`
9. 所有 `AiReviewItem` → `TbProjectReviewItem`
10. 所有 `AiProjectMapper` → `TbProjectMapper`
11. 所有 `AiRequirementMapper` → `TbRequirementMapper`
12. 所有 `AiReviewItemMapper` → `TbProjectReviewItemMapper`
13. 字段替换同上规则
14. `Map<String, List<AiReviewItem>>` → `Map<String, List<TbProjectReviewItem>>`
15. `List<AiReviewItem> reviewItems` → `List<TbProjectReviewItem> reviewItems`

- [ ] **Step 11: DetectionResultParser.java — 替换 AiDetectionRecord → TbDetectionRecord**

1. `import com.jy.eleaitender.common.entity.core.AiDetectionRecord;` → `import com.jy.eleaitender.common.entity.core.TbDetectionRecord;`
2. 方法签名 `parseIssues(AiDetectionRecord record)` → `parseIssues(TbDetectionRecord record)`

---

### Task 8: 更新 SQL 初始化脚本

**Files:**
- Modify: `ele-ai-tender-core/sql/init.sql`

- [ ] **Step 1: 更新 init.sql 中的表名和实体类引用**

替换规则:
1. 所有 `` `ai_project` `` → `` `tb_project` ``
2. 所有 `` `ai_project_version` `` → `` `tb_project_version` ``
3. 所有 `` `ai_requirement` `` → `` `tb_requirement` ``
4. 所有 `` `ai_review_item` `` → `` `tb_project_review_item` ``
5. 所有 `` `ai_detection_record` `` → `` `tb_detection_record` ``
6. 所有 `` `ai_policy_file` `` → `` `tb_policy_file` ``
7. 注释中的实体类引用: `AiProject` → `TbProject`, `AiRequirement` → `TbRequirement`, `AiReviewItem` → `TbProjectReviewItem`, `AiDetectionRecord` → `TbDetectionRecord`, `AiProjectVersion` → `TbProjectVersion`, `AiPolicyFile` → `TbPolicyFile`
8. 表前缀说明: `表前缀: ai_` → `表前缀: tb_(Core模块) / ai_(AI模块) / sup_(支撑中心) / file_(文件服务)`
9. COMMENT 也需更新: `COMMENT='AI编制项目表'` → `COMMENT='AI编制项目表'`（中文描述不变，因为业务含义不变）

---

### Task 9: 检查 MapperScan 配置

**Files:**
- Check: `ele-ai-tender-core/src/main/java/` — 查找 `@MapperScan` 注解
- Check: `ele-ai-tender-support/src/main/java/` — 查找 `@MapperScan` 注解
- Check: `ele-ai-tender-ai/src/main/java/` — 查找 `@MapperScan` 注解

- [ ] **Step 1: 检查各模块 @MapperScan 配置**

搜索所有 `@MapperScan` 注解，确认扫描路径是基于包路径（如 `com.jy.eleaitender.core.mapper`）而非具体类名。包路径不变，仅类名变了，所以 MapperScan 不需要修改。

如发现任何基于具体类名的配置，需同步更新。

---

### Task 10: 编译验证

- [ ] **Step 1: clean compile 全模块**

```bash
cd ele-ai-tender-system && mvn clean compile -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

如有编译错误，根据错误信息逐个修复遗漏的引用。

- [ ] **Step 2: install common 模块**

```bash
cd ele-ai-tender-system && mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 全模块编译验证**

```bash
cd ele-ai-tender-system && mvn clean compile -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS，无任何编译错误

---

### Task 11: 提交代码

- [ ] **Step 1: 暂存所有变更**

```bash
cd ele-ai-tender-system && git add -A
```

- [ ] **Step 2: 检查暂存内容**

```bash
rtk git status
rtk git diff --cached --stat
```

确认没有遗漏或误改无关文件。

- [ ] **Step 3: 提交**

```bash
git commit -m "refactor(core): Core模块表前缀从ai_迁移为tb_

- 实体类重命名: AiProject→TbProject, AiRequirement→TbRequirement, AiReviewItem→TbProjectReviewItem, AiDetectionRecord→TbDetectionRecord, AiProjectVersion→TbProjectVersion, AiPolicyFile→TbPolicyFile
- 表名重命名: ai_project→tb_project, ai_requirement→tb_requirement, ai_review_item→tb_project_review_item, ai_detection_record→tb_detection_record, ai_project_version→tb_project_version, ai_policy_file→tb_policy_file
- Mapper/Service/Controller/DTO/StateMachine全链路引用同步更新
- SQL硬编码表名同步更新
- 数据库RENAME TABLE SQL待执行

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 12: 数据库迁移（手动执行）

> 此步骤需要 DBA 或开发人员手动在数据库执行，不在代码提交范围内。

- [ ] **Step 1: 执行 RENAME TABLE**

```sql
-- 在 ele_ai_tender 数据库上执行（db=6）
USE ele_ai_tender;

RENAME TABLE ai_project TO tb_project;
RENAME TABLE ai_requirement TO tb_requirement;
RENAME TABLE ai_review_item TO tb_project_review_item;
RENAME TABLE ai_detection_record TO tb_detection_record;
RENAME TABLE ai_project_version TO tb_project_version;
RENAME TABLE ai_policy_file TO tb_policy_file;
```

- [ ] **Step 2: 验证表名**

```sql
SHOW TABLES LIKE 'tb_%';
```

Expected: 6 行结果

- [ ] **Step 3: 回滚 SQL（备用）**

```sql
-- 仅在需要回滚时执行
RENAME TABLE tb_project TO ai_project;
RENAME TABLE tb_requirement TO ai_requirement;
RENAME TABLE tb_project_review_item TO ai_review_item;
RENAME TABLE tb_detection_record TO ai_detection_record;
RENAME TABLE tb_project_version TO ai_project_version;
RENAME TABLE tb_policy_file TO ai_policy_file;
```
