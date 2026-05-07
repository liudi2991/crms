package com.company.crms.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.payment.dto.CreateRecordDTO;
import com.company.crms.payment.dto.ManualSettleDTO;
import com.company.crms.payment.dto.RecordQuery;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.service.PaymentRecordService;
import com.company.crms.payment.service.SettlementService;
import com.company.crms.payment.vo.PaymentRecordVO;
import com.company.crms.system.service.HardDeleteLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentRecordMapper recordMapper;
    private final ContractMapper contractMapper;
    private final SettlementService settlementService;
    private final HardDeleteLogService hardDeleteLogService;

    @Override
    public PageResult<PaymentRecordVO> page(RecordQuery query) {
        Page<PaymentRecord> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<PaymentRecord> w = new QueryWrapper<>();
        if (query.getContractId() != null) w.eq("contract_id", query.getContractId());
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like("payer", kw).or().like("voucher_no", kw));
        }
        if (query.getStatus() != null) w.eq("status", query.getStatus());
        if (query.getFromDate() != null) w.ge("arrival_date", query.getFromDate());
        if (query.getToDate() != null) w.le("arrival_date", query.getToDate());
        w.orderByDesc("arrival_date").orderByDesc("id");

        Page<PaymentRecord> result = (Page<PaymentRecord>) recordMapper.selectPageRecords(page, w);
        List<PaymentRecordVO> vos = toVOs(result.getRecords());
        return PageResult.of(result, vos);
    }

    @Override
    public PaymentRecordVO detail(Long id) {
        PaymentRecord r = recordMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "回款记录不存在");
        }
        return toVOs(List.of(r)).get(0);
    }

    @Override
    public List<PaymentRecordVO> listByContract(Long contractId) {
        QueryWrapper<PaymentRecord> w = new QueryWrapper<PaymentRecord>()
                .eq("contract_id", contractId)
                .orderByDesc("arrival_date");
        return toVOs(recordMapper.selectList(w));
    }

    @Override
    @OperationLog(module = "回款", action = "登记实际回款", type = "CREATE")
    @Transactional
    public Long create(CreateRecordDTO dto) {
        if (contractMapper.selectById(dto.getContractId()) == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        PaymentRecord r = new PaymentRecord();
        r.setId(SnowflakeIdGenerator.next());
        r.setContractId(dto.getContractId());
        r.setArrivalDate(dto.getArrivalDate());
        r.setAmount(dto.getAmount());
        r.setPayer(dto.getPayer());
        r.setVoucherNo(dto.getVoucherNo());
        r.setStatus("NORMAL");
        r.setUnallocatedAmount(dto.getAmount());
        r.setRemark(dto.getRemark());
        r.setVoucherFileId(dto.getVoucherFileId());
        recordMapper.insert(r);

        BigDecimal settled = settlementService.settle(r, dto.getTargetPlanIds());
        log.info("created record {} amount={} settled={}", r.getId(), r.getAmount(), settled);
        return r.getId();
    }

    @Override
    @OperationLog(module = "回款", action = "手工核销", type = "UPDATE")
    @Transactional
    public void manualSettle(ManualSettleDTO dto) {
        PaymentRecord r = recordMapper.selectById(dto.getRecordId());
        if (r == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "回款记录不存在");
        }
        settlementService.settle(r, dto.getPlanIds());
    }

    @Override
    @OperationLog(module = "回款", action = "软删除回款", type = "DELETE")
    @Transactional
    public void remove(Long id) {
        PaymentRecord r = recordMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "回款记录不存在");
        }
        if (r.getUnallocatedAmount() != null
                && r.getUnallocatedAmount().compareTo(r.getAmount()) != 0) {
            throw new BizException(ErrorCode.PM_RED_REVERSED, "已部分核销，请先红冲再删除");
        }
        recordMapper.deleteById(id);
    }

    @Override
    @OperationLog(module = "回款", action = "硬删除回款", type = "HARD_DELETE", recordParams = false)
    @Transactional
    public void hardDelete(Long id, String reason) {
        if (!UserContextHolder.require().isSuperAdmin()) {
            throw new BizException(ErrorCode.SY_HARD_DELETE_DENIED);
        }
        PaymentRecord r = recordMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "回款记录不存在");
        }
        hardDeleteLogService.record("PAYMENT_RECORD", id, r, reason);
        recordMapper.physicalDelete(id);
    }

    private List<PaymentRecordVO> toVOs(List<PaymentRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Long> contractIds = records.stream()
                .map(PaymentRecord::getContractId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Contract> contracts = contractIds.isEmpty()
                ? Collections.emptyMap()
                : contractMapper.selectBatchIds(contractIds).stream()
                        .collect(Collectors.toMap(Contract::getId, c -> c));
        return records.stream().map(r -> {
            PaymentRecordVO vo = new PaymentRecordVO();
            BeanUtils.copyProperties(r, vo);
            Contract c = contracts.get(r.getContractId());
            if (c != null) {
                vo.setContractCode(c.getCode());
                vo.setContractName(c.getName());
            }
            return vo;
        }).toList();
    }
}
