package com.company.crms.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.payment.dto.CreatePlanDTO;
import com.company.crms.payment.dto.GeneratePlansDTO;
import com.company.crms.payment.dto.PlanQuery;
import com.company.crms.payment.dto.UpdatePlanDTO;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.service.PaymentPlanService;
import com.company.crms.payment.vo.PaymentPlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPlanServiceImpl implements PaymentPlanService {

    private final PaymentPlanMapper planMapper;
    private final ContractMapper contractMapper;

    @Override
    public PageResult<PaymentPlanVO> page(PlanQuery query) {
        Page<PaymentPlan> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<PaymentPlan> w = new QueryWrapper<>();
        if (query.getContractId() != null) w.eq("contract_id", query.getContractId());
        if (query.getStatus() != null) w.eq("status", query.getStatus());
        if (Boolean.TRUE.equals(query.getOverdueOnly())) w.eq("is_overdue", 1);
        if (query.getFromDate() != null) w.ge("plan_date", query.getFromDate());
        if (query.getToDate() != null) w.le("plan_date", query.getToDate());
        w.orderByAsc("plan_date").orderByAsc("period_no");

        Page<PaymentPlan> result = (Page<PaymentPlan>) planMapper.selectPagePlans(page, w);
        List<PaymentPlanVO> vos = toVOs(result.getRecords());
        return PageResult.of(result, vos);
    }

    @Override
    public List<PaymentPlanVO> listByContract(Long contractId) {
        QueryWrapper<PaymentPlan> w = new QueryWrapper<PaymentPlan>()
                .eq("contract_id", contractId)
                .orderByAsc("period_no");
        return toVOs(planMapper.selectList(w));
    }

    @Override
    public PaymentPlanVO detail(Long id) {
        PaymentPlan p = planMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND);
        }
        return toVOs(List.of(p)).get(0);
    }

    @Override
    @OperationLog(module = "回款", action = "新增计划", type = "CREATE")
    @Transactional
    public Long create(CreatePlanDTO dto) {
        Contract c = contractMapper.selectById(dto.getContractId());
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        PaymentPlan p = new PaymentPlan();
        p.setId(SnowflakeIdGenerator.next());
        p.setContractId(dto.getContractId());
        p.setPeriodNo(dto.getPeriodNo());
        p.setPlanDate(dto.getPlanDate());
        p.setPlanAmount(dto.getPlanAmount());
        p.setSettledAmount(BigDecimal.ZERO);
        p.setUnsettledAmount(dto.getPlanAmount());
        p.setStatus("PENDING");
        p.setIsOverdue(0);
        p.setOverdueDays(0);
        p.setRemindDays(dto.getRemindDays());
        planMapper.insert(p);
        return p.getId();
    }

    @Override
    @OperationLog(module = "回款", action = "生成计划", type = "CREATE")
    @Transactional
    public List<Long> generate(GeneratePlansDTO dto) {
        Contract c = contractMapper.selectById(dto.getContractId());
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(dto.getOverwrite())) {
            planMapper.softDeleteByContract(dto.getContractId());
        } else if (planMapper.countByContract(dto.getContractId()) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "该合同已存在回款计划，请使用覆盖模式或删除后重建");
        }

        BigDecimal total = c.getAmount();
        int periods = dto.getPeriods();
        BigDecimal each = total.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        BigDecimal sum = each.multiply(BigDecimal.valueOf(periods));
        BigDecimal lastDelta = total.subtract(sum); // 最后一期补差，避免分摊误差

        List<Long> ids = new ArrayList<>();
        LocalDate date = dto.getFirstPlanDate();
        for (int i = 0; i < periods; i++) {
            PaymentPlan p = new PaymentPlan();
            p.setId(SnowflakeIdGenerator.next());
            p.setContractId(dto.getContractId());
            p.setPeriodNo(i + 1);
            p.setPlanDate(date);
            BigDecimal amt = (i == periods - 1) ? each.add(lastDelta) : each;
            p.setPlanAmount(amt);
            p.setSettledAmount(BigDecimal.ZERO);
            p.setUnsettledAmount(amt);
            p.setStatus("PENDING");
            p.setIsOverdue(0);
            p.setOverdueDays(0);
            planMapper.insert(p);
            ids.add(p.getId());
            date = nextDate(date, dto.getFrequency());
        }
        log.info("[plan] generated {} periods for contract {}", periods, dto.getContractId());
        return ids;
    }

    @Override
    @OperationLog(module = "回款", action = "更新计划", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdatePlanDTO dto) {
        PaymentPlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND);
        }
        if ("SETTLED".equals(existing.getStatus())) {
            throw new BizException(ErrorCode.PM_PLAN_SETTLED);
        }
        BigDecimal diff = dto.getPlanAmount().subtract(existing.getPlanAmount());
        PaymentPlan upd = new PaymentPlan();
        upd.setId(id);
        upd.setVersion(dto.getVersion());
        upd.setPlanDate(dto.getPlanDate());
        upd.setPlanAmount(dto.getPlanAmount());
        upd.setRemindDays(dto.getRemindDays());
        // 同步 unsettled_amount，避免出现金额错位
        upd.setUnsettledAmount(existing.getUnsettledAmount().add(diff).max(BigDecimal.ZERO));
        int rows = planMapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "已被其他用户修改，请刷新后重试");
        }
    }

    @Override
    @OperationLog(module = "回款", action = "删除计划", type = "DELETE")
    @Transactional
    public void remove(Long id) {
        PaymentPlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND);
        }
        if (existing.getSettledAmount() != null && existing.getSettledAmount().signum() > 0) {
            throw new BizException(ErrorCode.PM_PLAN_SETTLED, "已有核销记录，无法删除");
        }
        planMapper.deleteById(id);
    }

    @Override
    public int markOverdueAuto() {
        int rows = planMapper.markOverdue(LocalDate.now());
        if (rows > 0) {
            log.info("[scheduler] markOverdue affected {} plans", rows);
        }
        return rows;
    }

    private LocalDate nextDate(LocalDate base, String freq) {
        return switch (freq) {
            case "MONTHLY" -> base.plusMonths(1);
            case "QUARTERLY" -> base.plusMonths(3);
            case "ONCE" -> base; // 单期不会进入下一次
            default -> base.plusMonths(1);
        };
    }

    private List<PaymentPlanVO> toVOs(List<PaymentPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        List<Long> contractIds = plans.stream()
                .map(PaymentPlan::getContractId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Contract> contracts = contractIds.isEmpty()
                ? Collections.emptyMap()
                : contractMapper.selectBatchIds(contractIds).stream()
                        .collect(Collectors.toMap(Contract::getId, c -> c));
        // VO 层即时计算 overdue/overdueDays，与 Dashboard / 账龄分析的口径对齐，
        // 避免 06:00 markOverdue 定时任务尚未跑完时列表里看不到「逾期」红 tag。
        // DB 字段 is_overdue / overdue_days 仍由 scheduler 维护，给筛选与提醒任务用。
        LocalDate today = LocalDate.now();
        return plans.stream().map(p -> {
            PaymentPlanVO vo = new PaymentPlanVO();
            BeanUtils.copyProperties(p, vo, "isOverdue", "overdueDays");
            boolean past = p.getPlanDate() != null && p.getPlanDate().isBefore(today);
            boolean unsettled = p.getUnsettledAmount() != null && p.getUnsettledAmount().signum() > 0;
            boolean realOverdue = past && unsettled && !"SETTLED".equals(p.getStatus());
            vo.setOverdue(realOverdue);
            vo.setOverdueDays(realOverdue
                    ? (int) java.time.temporal.ChronoUnit.DAYS.between(p.getPlanDate(), today)
                    : 0);
            Contract c = contracts.get(p.getContractId());
            if (c != null) {
                vo.setContractCode(c.getCode());
                vo.setContractName(c.getName());
            }
            return vo;
        }).toList();
    }
}
