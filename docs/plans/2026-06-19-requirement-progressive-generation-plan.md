# Requirement Progressive Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the frontend show the generated outline first, then fill generated chapter content under each chapter while the requirement generation task continues through final review.

**Architecture:** Keep one `REQUIREMENT_GENERATE` task. The AI service writes structured progress snapshots to `ai_task.result`; the core service still syncs only the final `content` after the task reaches `COMPLETED`. The frontend polls the existing task status endpoint and renders the process result read-only until review completes.

**Tech Stack:** JDK 21, Spring Boot, MyBatis-Plus, Jackson, Vue 3, TypeScript, Vite.

---

### Task 1: Backend Progress Snapshots

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/test/java/com/jy/eleaitender/ai/processor/generator/RequirementGeneratorTest.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/mapper/AiTaskMapper.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/RequirementGenerator.java`

- [ ] Add a failing unit test that captures interim `ai_task.result` updates.
- [ ] Add an AI mapper method to update only `result`.
- [ ] Update `RequirementGenerator` after outline, each chapter, draft assembly, review start, and review completion.
- [ ] Keep `content` absent before the first chapter content exists.

### Task 2: Frontend Process Rendering

**Files:**
- Modify: `ele-ai-tender-frontend/src/types/ai-task.ts`
- Modify: `ele-ai-tender-frontend/src/views/requirement/RequirementGenerate.vue`

- [ ] Add process-result types for outline, chapters, content stage, and review status.
- [ ] Parse `latestTask.result` during polling.
- [ ] Show outline skeleton before content exists.
- [ ] Fill generated chapters under the matching outline chapter in read-only mode.
- [ ] Keep editing disabled until `COMPLETED + resultSynced=1`.

### Task 3: Verification

**Commands:**
- Backend targeted test:
  `mvn.cmd -q -pl ele-ai-tender-ai -am "-Dtest=RequirementGeneratorTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-DargLine=-XX:+EnableDynamicAgentLoading" "-Dmaven.repo.local=D:\JavaWorkSpace\tender-document-tool\.m2-test" test -s D:\Maven\nexus\apache-maven-3.6.3\conf\settings.xml`
- Frontend build or type check:
  `npm run build`

- [ ] Confirm backend test passes.
- [ ] Confirm frontend build/type check passes or report exact blocker.
