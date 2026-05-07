# 示例：合同自动核销（复杂业务）

## 用例背景
DSS §3.4.1 自动核销算法。

## 输入

### 系统消息
[crms/prompts/system-backend.md] 完整加载。

### 用户消息

```markdown
## 上下文

- **任务**：I3-PR-002 自动核销算法（复杂）
- **关联 SRS**：UC-PM-04
- **关联 DSS §3.4.1**：
  - 输入：一条 PaymentRecord（合同 X、金额 A、到账日期 D）
  - 输出：根据合同 X 下未结清的 PaymentPlan 按 plan_date 升序逐条核销，剩余 unallocated；
  - 同一计划被多笔实际回款核销时，settled_amount = sum(settle_amount)；
  - 支持手工指定核销目标（用户上传时提供 planId 列表）。
- **数据库表**（节选）：
  ```sql
  payment_plan(id, contract_id, period_no, plan_amount, settled_amount, unsettled_amount, status)
  payment_record(id, contract_id, amount, unallocated_amount, status)
  payment_settlement(payment_plan_id, payment_record_id, settle_amount, settle_at, settle_type)
  ```
- **依赖**：
  - PaymentPlanService.findUnsettledByContractWithDataScope(contractId)
  - PaymentRecordMapper.updateUnallocated(id, amount)
  - 编号已生成；
  - 已存在 `RedReverseService`（红冲）。

## 要求

1. 在 Service 中实现 `autoSettle(PaymentRecord rec, List<Long> targetPlanIds /* 可空 */)`；
2. 必须支持并发：同一 PaymentPlan 在并发场景下不能被多条 record 重复核销；
3. 用 `for update` + 数据库唯一索引联合保证；
4. 红冲（status=RED）的实际回款不能再核销，需抛 `PM_RED_REVERSED`；
5. 在 Repository 内开启事务；
6. 单测使用 Testcontainers MySQL 模拟 5 条线程并发核销同一 plan，期望最终 settled_amount 不超过 plan_amount。

## 输出顺序

1. 先输出**伪代码**（10–20 行 markdown 列表），等我评审；
2. 评审通过后再输出代码。
```

## 期望伪代码

```text
function autoSettle(record, [targetPlanIds]):
  if record.status == RED:
    throw PM_RED_REVERSED
  Acquire row lock on record (select for update)
  remaining = record.amount - record.unallocated_amount  // 已核销过的不重核
  // 选择候选 plan
  if targetPlanIds present:
    plans = findByIds(targetPlanIds) ordered by plan_date asc
  else:
    plans = findUnsettledByContract(record.contract_id) ordered by plan_date asc
  for plan in plans:
    Acquire row lock on plan (select for update)
    if plan.status == SETTLED: continue
    portion = min(plan.unsettled_amount, remaining)
    if portion <= 0: break
    insert payment_settlement(plan.id, record.id, portion, NOW(), AUTO)
    plan.settled_amount += portion
    plan.unsettled_amount -= portion
    plan.status = (unsettled_amount == 0 ? SETTLED : PARTIAL)
    update plan
    remaining -= portion
  record.unallocated_amount = remaining
  update record
  publish PaymentSettledEvent
```

（评审通过后再生成 Java 代码与单测。）
