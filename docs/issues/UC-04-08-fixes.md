# UC-04-08 逾期预警 — 口径不一致修复记录

> 来源：2026-05-07 用户反馈"首页综合看板逾期金额有数，但回款计划列表里没有任何条目被标红"。
> 关联：`crms-app/src/main/java/com/company/crms/report/service/impl/ReportServiceImpl.java`、`crms-app/src/main/java/com/company/crms/payment/service/impl/PaymentPlanServiceImpl.java`、`crms-app/src/main/java/com/company/crms/payment/scheduler/PaymentScheduler.java`、`crms-app/src/main/java/com/company/crms/payment/mapper/PaymentPlanMapper.java`。
> 状态：**已修复（VO 层即时计算）**，遗留改进项见文末。

---

## 现象

| 位置 | 显示 |
| --- | --- |
| 首页综合看板「逾期金额」 | 有数（与实际过期未核销金额一致） |
| 合同详情 → 回款计划 Tab → 「逾期」列 | **全部显示 `-`，无任何红 tag** |
| `GET /payment-plans?overdueOnly=true` | **返回空** |

业务/演示场景下"看板说逾期 X 万、明细页一条逾期都看不见"，观感与可信度直接崩。

---

## 根因：两套口径

| 路径 | 判定方式 | 实时性 |
| --- | --- | --- |
| `ReportServiceImpl.dashboard()` | `plan_date < today` 流式过滤 | ✅ 实时 |
| `ReportMapper / PaymentPlanMapper.drillBucket` 账龄 | SQL `DATEDIFF(today, plan_date)` | ✅ 实时 |
| **`PaymentPlanServiceImpl.toVOs()`「逾期」字段** | 读 DB 字段 `is_overdue` | ❌ 等定时任务 |
| **`overdueOnly` 列表过滤** | `WHERE is_overdue = 1` | ❌ 等定时任务 |
| **`overdueReminder` 通知任务（09:45）** | `WHERE is_overdue = 1` | ❌ 等定时任务 |

DB 字段 `payment_plan.is_overdue` 只在 **每天 06:00** 被 `PaymentScheduler.markOverdue()` 刷一次。新建/生成 plan 时一律硬写 `is_overdue=0, overdue_days=0`。所以：

1. 验收/演示期间手工塞数据，scheduler 还没跑过 06:00 → DB 字段全 0。
2. 业务运行中今天新生成的逾期计划要等明天早上 6 点才会被标红。
3. `firstPlanDate` 选过去日期（导入历史数据）也会立刻命中这个坑。

---

## 修复（已实施）

**方案 B：VO 层即时计算**，与看板 / 账龄同口径。

### 代码变更

`crms-app/src/main/java/com/company/crms/payment/service/impl/PaymentPlanServiceImpl.java#toVOs`：

- `BeanUtils.copyProperties(p, vo, "isOverdue", "overdueDays")` 把两个字段都排除。
- 在 VO 装配阶段实时算：

```java
boolean past = p.getPlanDate() != null && p.getPlanDate().isBefore(today);
boolean unsettled = p.getUnsettledAmount() != null && p.getUnsettledAmount().signum() > 0;
boolean realOverdue = past && unsettled && !"SETTLED".equals(p.getStatus());
vo.setOverdue(realOverdue);
vo.setOverdueDays(realOverdue
    ? (int) ChronoUnit.DAYS.between(p.getPlanDate(), today)
    : 0);
```

### 影响面

| 路径 | 改动后行为 |
| --- | --- |
| 合同详情页 → 回款计划 Tab 「逾期」红 tag | ✅ 实时显示 |
| `GET /payment-plans` 全字段查询 | ✅ `overdue/overdueDays` 实时 |
| `GET /payment-plans?overdueOnly=true` 筛选 | ⚠️ **仍走 DB 字段**，与 VO 显示可能短暂不一致（仅在 06:00 之前的"今日新增逾期"窗口） |
| 09:45 逾期提醒通知 | ⚠️ **仍走 DB 字段**，今天新逾期的 plan 当天不会通知，需等次日 |

### 设计取舍

只动 VO 装配、不动 SQL 过滤与通知任务，理由：

- **筛选 SQL 实时化**会让分页 + 索引失效（`plan_date < CURDATE()` 还能走索引，可接受），但口径切换风险大，留作后续。
- **通知任务实时化**需要额外的"今日已发"幂等表，否则每次任务都会全量重发，需求面更大。
- 目前观感问题（看板 vs 列表脱节）已经被 VO 层修掉，承担用户感知层面 90% 的解释成本。

---

## 验证

| 项 | 方法 | 通过标准 |
| --- | --- | --- |
| 列表实时显示 | 把某条 plan 的 `plan_date` 改成昨天，刷新合同详情 | 立刻显示「逾期 1 天」红 tag，无需等 06:00 |
| 看板与列表对齐 | 首页看板逾期金额 ≈ ∑（列表显示「逾期」的 unsettledAmount） | 两侧金额一致 |
| 单测 | 新增 `PaymentPlanServiceImplTest`：plan_date=昨天 + unsettled>0 + status=PENDING | `vo.overdue == true && overdueDays >= 1` |
| 单测：已结清不视为逾期 | plan_date=去年 + status=SETTLED | `vo.overdue == false` |
| 单测：已核销 0 元不视为逾期 | unsettled=0 + plan_date=去年 | `vo.overdue == false` |
| acceptance.sh | 现有 `D` 段（算法）继续通过 | PASS |

---

## 遗留改进（不在本次修复范围）

| 优先级 | 内容 |
| --- | --- |
| P3 | `overdueOnly=true` 改成 SQL 实时判断（`AND plan_date < CURDATE() AND unsettled_amount > 0 AND status != 'SETTLED'`），同时考虑加复合索引 `(is_deleted, plan_date, status)`。 |
| P3 | 09:45 逾期提醒任务也用实时 SQL，但需要新增 `payment_overdue_notice_log(plan_id, notice_date)` 唯一约束实现"当日只发一次"的幂等。 |
| P4 | 长期目标：`is_overdue` 字段降级为"提醒任务的去重标记"，不再作为业务真值；或干脆移除该列，统一走实时计算。 |

---

## 决策记录

- **2026-05-07**：用户反馈口径不一致 → 排查到看板实时算 + 列表读 DB 字段的根因 → 选 B 方案（VO 层即时计算）一次性修齐展示路径，DB 字段保留给 scheduler/筛选/提醒用。遗留两个 P3 改进项归到本文档备查。
