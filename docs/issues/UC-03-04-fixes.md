# UC-03-04 编辑合同 — 偏差记录

> 来源：2026-05-07 与用户对话中复核 SRS §4.3 UC-03-04 的实现，发现 1 项越权偏差。
> 关联：`crms-app/src/main/java/com/company/crms/contract/service/impl/ContractServiceImpl.java`、`crms-app/src/main/java/com/company/crms/contract/enums/ContractStatus.java`。
> 状态：**已记录、暂不修复**（待用户决定是否收紧）。

---

## Issue #1 · 已结案合同也能被编辑（与 SRS 不一致）

| 字段 | 内容 |
| --- | --- |
| 严重度 | P2（合规/审计风险，业务无阻断） |
| SRS UC-03-04 期望 | 「**仅"草稿"或"履行中"状态可编辑**，关键字段变更需写入变更记录」 |
| 当前实现 | `ContractServiceImpl.update(Long, UpdateContractDTO)` **未做 status 白名单校验**，任意状态的合同都可改 |
| 可重现 | 把合同状态流转到 `COMPLETED / TERMINATED / EXPIRED` 后，仍能 `PUT /api/v1/contracts/{id}` 改 name / amount / performEndAt |

### 状态 vs SRS

| 状态 | SRS 期望 | 当前代码 | 一致？ |
| --- | --- | --- | --- |
| `DRAFT` 草稿 | ✅ 可编辑 | ✅ 可编辑 | ✅ |
| `EFFECTIVE` 履行中 | ✅ 可编辑（关键字段写 change_log） | ✅ 可编辑（已写 change_log） | ✅ |
| `COMPLETED` 已完成 | ❌ 禁止 | ⚠️ 能改 | ❌ |
| `TERMINATED` 已终止 | ❌ 禁止 | ⚠️ 能改 | ❌ |
| `EXPIRED` 已过期 | ❌ 禁止 | ⚠️ 能改 | ❌ |

### 风险

- **审计混乱**：结案后还能调金额/履约日，时间线 Tab 会出现"合同已完成 → 之后又改了金额"这种异常事件，财务侧难以解释。
- **绕过红冲流程**：本来回款核销错应走「红冲 + 重登」，结案合同金额可改会留下"私改账"的可能。
- **仅靠前端拦不住**：当前前端 `<el-button v-perm="contract:update">` 只校验权限点，没按状态隐藏「编辑」按钮；超管 R05 永远能看到编辑按钮。

---

## 修复策略（待批准后实施）

**后端**（推荐）：在 `ContractServiceImpl.update` 顶部加白名单校验，与 `softDelete` 同模式：

```java
String status = existing.getStatus();
if (!ContractStatus.DRAFT.name().equals(status)
        && !ContractStatus.EFFECTIVE.name().equals(status)) {
    throw new BizException(ErrorCode.CT_STATUS_INVALID,
            "仅 DRAFT/EFFECTIVE 状态合同可编辑");
}
```

**前端**（配套）：`ContractDetail.vue` 的「编辑」按钮加 `v-if="canEdit"`，与 `canDelete` 同模式：

```ts
const canEdit = computed(() => {
  const s = contract.value?.status
  return s === 'DRAFT' || s === 'EFFECTIVE'
})
```

**例外口子**（如业务确实需要）：可保留一个「超管 + 二次密码」的强制改通道，但走单独 endpoint，明确写 `hard_delete_log` 同款审计表（如 `contract_force_edit_log`），不走常规 `update`。

---

## 验证计划

| 项 | 方法 | 通过标准 |
| --- | --- | --- |
| 单测 | 新增 `ContractServiceImplTest#update_rejectWhenCompleted/Terminated/Expired` | 抛 `BizException(CT_STATUS_INVALID)` |
| 集成测 | `acceptance.sh` 增 case：转 COMPLETED 后改 name | HTTP 400 + `code=CT-002` |
| 前端冒烟 | 完成态合同详情页 | 「编辑」按钮不显示 |

## 时间盒

- 一次提交完成，约 25 分钟（后端校验 5 行 + 前端 1 行 computed + 单测 3 个 + acceptance 1 段）。

---

## 决策记录

- **2026-05-07**：用户复核 UC-03-04 实现，确认偏差存在。**暂不修复**，先记录到本文件，等业务确认是否需要收紧再排期。
