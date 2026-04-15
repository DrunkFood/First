# 阶段四：集成联调与优化 - 设计文档

> 创建日期: 2026-04-15
> 状态: 已确认

## 1. 目标

对业务需求编制和项目管理进行全链路联调，按用户操作路径顺序验证前后端打通，修复所有阻断性缺陷，确保核心流程跑通。

## 2. 联调范围

### 覆盖的两条核心链路

**链路1：业务需求编制**
```
登录 → 需求列表 → 新建需求(三种匹配模式) → AI生成 → 编辑(自动保存) → 智能检测(敏感词+错别字) → 确认结果
```

**链路2：项目管理五阶段**
```
项目列表 → 新建项目(引用/系统生成) → 阶段1(基本信息+模板选择) → 阶段2(详细需求生成) → 阶段3(评审项设置) → 阶段4(文档集成) → 阶段5(智能检测4类) → 发布/归档
```

### 不包含
- 知识库RAG补全（Tika解析、Milvus向量化、向量检索）
- 对话历史管理
- 版本管理功能补全
- 模板CRUD端点补全
- 统计分析/消息中心/系统设置模块联调

## 3. 联调步骤

| 步骤 | 操作页面 | 验证要点 | 涉及后端接口 | 已知风险 |
|------|----------|----------|-------------|----------|
| S1 | 登录页 | 手机号+验证码登录成功跳转 | POST /auth/phone-login | 需SMS服务 |
| S2 | 需求列表 | 列表加载、搜索、筛选 | GET /requirements | 无 |
| S3 | 新建需求 | 表单提交、三种匹配模式 | POST /requirements, POST /ai/match/* | 匹配评分硬编码 |
| S4 | AI生成需求 | SSE流式生成、自动保存 | POST /requirements/{id}/generate, SSE /ai/chat | 对话历史未使用 |
| S5 | 编辑需求 | 编辑器加载、自动保存、保存 | PUT /requirements/{id}, auto-save | 无 |
| S6 | 智能检测(需求) | 提交检测→进度→报告→接受/拒绝 | POST /requirements/{id}/detect, /ai-tasks/*, /detection/* | parseIssues为空，accept/reject为存根 |
| S7 | 项目列表 | 列表加载、搜索 | GET /projects | 无 |
| S8 | 新建项目 | 表单提交、引用需求/系统生成 | POST /projects | 无 |
| S9 | 阶段1-基本信息 | 模板选择、历史匹配 | PUT /projects/{id}/phase, GET /templates/default | 无 |
| S10 | 阶段2-详细需求 | AI生成+优化+聊天 | 同S4+S5 | 同S4+S5 |
| S11 | 阶段3-评审项 | AI生成评审项、树编辑 | POST /review-items/{id}/generate, GET /review-items | 树结构无children字段 |
| S12 | 阶段4-文档集成 | 集成预览、编辑、导出Word | POST /documents/integrate, GET preview, GET export | Word模板文件缺失 |
| S13 | 阶段5-智能检测 | 4类检测、政策文件选择、接受/拒绝 | POST /detection/submit, progress, report | 同S6的阻断缺陷 |
| S14 | 发布/归档 | 状态流转 | PUT /projects/{id}/status | 无 |

## 4. 必须修复的阻断性缺陷

| 优先级 | 缺陷 | 修复方案 | 影响步骤 |
|--------|------|----------|----------|
| P0 | `DetectionServiceImpl.parseIssues()` 返回空列表 | 解析AI检测结果JSON，提取issues数组 | S6, S13 |
| P0 | `acceptIssue()/rejectIssue()` 只打日志无实际修改 | 更新result JSON中对应issue的handleStatus | S6, S13 |
| P0 | `acceptAll()` 只打日志无实际修改 | 遍历result JSON中所有issue，批量更新handleStatus | S6, S13 |
| P1 | `parseIssueCount()` 返回0 | 从解析出的issues列表计算数量 | S6, S13 |
| P1 | AI模块DetectionController创建记录但不触发AI | 移除冗余端点或统一到Core路径 | S6, S13 |
| P2 | Word导出模板文件缺失 | 提供默认tender-document-template.docx | S12 |

### P0缺陷修复详细方案

#### 4.1 parseIssues() - 检测结果解析

**当前代码**（DetectionServiceImpl ~L330）:
```java
private List<DetectionIssueVO> parseIssues(String resultJson) {
    // 简化实现，返回空列表
    return Collections.emptyList();
}
```

**修复方案**:
AI模块检测结果存储在 `ai_detection_record.result` 字段，格式为:
```json
{
  "issues": [
    {
      "location": "第3段第2行",
      "original": "原始文本",
      "suggestion": "修改建议",
      "reason": "原因说明",
      "severity": "HIGH/MEDIUM/LOW",
      "handleStatus": "PENDING"
    }
  ]
}
```

修复思路: 使用Jackson ObjectMapper解析result JSON，提取issues数组，映射为DetectionIssueVO列表。

#### 4.2 acceptIssue()/rejectIssue() - 接受/拒绝建议

**当前代码**（DetectionServiceImpl ~L207-L218）:
```java
public void acceptIssue(Long recordId, Long issueIndex) {
    log.info("接受检测建议: recordId={}, issueIndex={}", recordId, issueIndex);
    // TODO: 实际修改result JSON中对应issue的状态
}
```

**修复方案**:
1. 读取record的result JSON
2. 定位对应index的issue
3. 更新handleStatus为ACCEPTED/REJECTED
4. 将修改后的JSON写回record.result字段
5. 如果是accept，还需要将suggestion应用到项目内容中

#### 4.3 acceptAll() - 批量接受

**修复方案**:
1. 读取record的result JSON
2. 遍历所有issues，将handleStatus为PENDING的设为ACCEPTED
3. 写回result字段

## 5. 联调方式

每一步的操作模式:
1. 启动服务 → 确认无启动报错
2. 页面操作 → 在浏览器中执行用户操作
3. 检查请求/响应 → 对照API定义，验证字段是否匹配
4. 修复缺陷 → 发现问题立即修复
5. 回归验证 → 修复后重新执行该步骤

## 6. 验收标准

- [ ] S1-S6 业务需求编制全流程跑通
- [ ] S7-S14 项目管理五阶段全流程跑通
- [ ] 所有P0缺陷修复并验证
- [ ] 所有P1缺陷修复并验证
- [ ] AI生成/优化/检测的SSE流式响应正常
- [ ] 状态机流转正确（9种状态）
- [ ] 前端字段与后端接口完全对齐
