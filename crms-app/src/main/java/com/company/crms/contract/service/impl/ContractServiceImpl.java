package com.company.crms.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.CodeGenerator;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.dto.ContractQuery;
import com.company.crms.contract.dto.CreateContractDTO;
import com.company.crms.contract.dto.UpdateContractDTO;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.enums.ContractStatus;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.contract.service.ContractService;
import com.company.crms.contract.vo.ContractVO;
import com.company.crms.customer.entity.Customer;
import com.company.crms.customer.mapper.CustomerMapper;
import com.company.crms.iam.entity.Department;
import com.company.crms.iam.entity.User;
import com.company.crms.iam.mapper.DepartmentMapper;
import com.company.crms.iam.mapper.UserMapper;
import com.company.crms.system.service.ChangeLogService;
import com.company.crms.system.service.HardDeleteLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final CodeGenerator codeGenerator;
    private final ChangeLogService changeLogService;
    private final HardDeleteLogService hardDeleteLogService;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final CustomerMapper customerMapper;

    @Override
    public PageResult<ContractVO> page(ContractQuery query) {
        Page<Contract> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<Contract> w = new QueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like("name", kw).or().like("code", kw));
        }
        if (query.getType() != null) w.eq("type", query.getType());
        if (query.getStatus() != null) w.eq("status", query.getStatus());
        if (query.getCustomerId() != null) w.eq("customer_id", query.getCustomerId());
        if (query.getOwnerId() != null) w.eq("owner_id", query.getOwnerId());
        if (query.getSignedFrom() != null) w.ge("signed_at", query.getSignedFrom());
        if (query.getSignedTo() != null) w.le("signed_at", query.getSignedTo());
        w.orderByDesc("signed_at");

        Page<Contract> result = (Page<Contract>) contractMapper.selectPageWithDataScope(page, w);
        List<ContractVO> vos = result.getRecords().stream().map(this::toVO).toList();
        enrichNames(vos);
        return PageResult.of(result, vos);
    }

    @Override
    public ContractVO detail(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        ContractVO vo = toVO(c);
        enrichNames(List.of(vo));
        return vo;
    }

    @Override
    @OperationLog(module = "合同", action = "新建合同", type = "CREATE")
    @Transactional
    public Long create(CreateContractDTO dto) {
        if (dto.getPerformEndAt().isBefore(dto.getPerformStartAt())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "履约结束日期必须不早于开始日期");
        }
        Contract c = new Contract();
        BeanUtils.copyProperties(dto, c);
        c.setId(SnowflakeIdGenerator.next());
        c.setCode(codeGenerator.contractCode());
        c.setStatus(ContractStatus.DRAFT.name());
        c.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : UserContextHolder.currentUserId());
        c.setDeptId(UserContextHolder.require().getDeptId());
        contractMapper.insert(c);
        return c.getId();
    }

    @Override
    @OperationLog(module = "合同", action = "更新合同", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdateContractDTO dto) {
        Contract existing = contractMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        if (dto.getPerformEndAt().isBefore(dto.getPerformStartAt())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "履约结束日期必须不早于开始日期");
        }
        Contract upd = new Contract();
        upd.setId(id);
        upd.setVersion(dto.getVersion());
        BeanUtils.copyProperties(dto, upd);
        // 仅超管可改变所有权
        if (dto.getOwnerId() != null && !UserContextHolder.require().isSuperAdmin()
                && !Objects.equals(dto.getOwnerId(), existing.getOwnerId())) {
            upd.setOwnerId(existing.getOwnerId());
        }
        int rows = contractMapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.CT_NOT_FOUND, "已被其他用户修改，请刷新后重试");
        }
        // 关键字段变更写 change_log
        recordIfChanged(id, "name", existing.getName(), dto.getName(), null);
        recordIfChanged(id, "amount",
                existing.getAmount() == null ? null : existing.getAmount().toPlainString(),
                dto.getAmount() == null ? null : dto.getAmount().toPlainString(), null);
        recordIfChanged(id, "perform_end_at",
                String.valueOf(existing.getPerformEndAt()),
                String.valueOf(dto.getPerformEndAt()), null);
    }

    @Override
    @OperationLog(module = "合同", action = "软删除合同", type = "DELETE")
    @Transactional
    public void softDelete(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        if (!ContractStatus.DRAFT.name().equals(c.getStatus())
                && !ContractStatus.TERMINATED.name().equals(c.getStatus())
                && !ContractStatus.EXPIRED.name().equals(c.getStatus())) {
            throw new BizException(ErrorCode.CT_STATUS_INVALID, "仅 DRAFT/TERMINATED/EXPIRED 状态合同可删除");
        }
        contractMapper.softDeleteAttachments(id);
        contractMapper.softDeletePaymentPlans(id);
        contractMapper.deleteById(id);
        log.info("contract {} soft-deleted with cascade", id);
    }

    @Override
    @OperationLog(module = "合同", action = "硬删除合同", type = "HARD_DELETE", recordParams = false)
    @Transactional
    public void hardDelete(Long id, String reason) {
        if (!UserContextHolder.require().isSuperAdmin()) {
            throw new BizException(ErrorCode.SY_HARD_DELETE_DENIED);
        }
        Contract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        hardDeleteLogService.record("CONTRACT", id, c, reason);
        // 级联物理清理（按 FK 依赖序：settlement → record → plan → attachment/note → contract）
        int s = contractMapper.physicalDeleteSettlementsByContract(id);
        int r = contractMapper.physicalDeletePaymentRecords(id);
        int p = contractMapper.physicalDeletePaymentPlans(id);
        int a = contractMapper.physicalDeleteAttachments(id);
        int n = contractMapper.physicalDeleteNotes(id);
        contractMapper.physicalDelete(id);
        log.warn("HARD DELETE contract id={} by={} reason={} cascade(settle={},rec={},plan={},att={},note={})",
                id, UserContextHolder.currentUserId(), reason, s, r, p, a, n);
    }

    @Override
    @OperationLog(module = "合同", action = "状态流转", type = "UPDATE")
    @Transactional
    public void transition(Long contractId, ContractStatus to, String reason) {
        Contract c = contractMapper.selectById(contractId);
        if (c == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        ContractStatus from = ContractStatus.of(c.getStatus());
        ContractStatus.assertTransition(from, to);

        Contract upd = new Contract();
        upd.setId(contractId);
        upd.setVersion(c.getVersion());
        upd.setStatus(to.name());
        int rows = contractMapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.CT_STATUS_INVALID, "并发冲突，请刷新后重试");
        }
        changeLogService.record("CONTRACT", contractId, "status",
                from.name(), to.name(), reason);
        log.info("contract {} transition {} -> {} reason={}", contractId, from, to, reason);
    }

    @Override
    public int markExpiredAuto() {
        int rows = contractMapper.markExpired(LocalDate.now());
        if (rows > 0) {
            log.info("[scheduler] markExpired affected {} contracts", rows);
        }
        return rows;
    }

    @Override
    public List<Contract> dueSoon(int advanceDays) {
        LocalDate today = LocalDate.now();
        return contractMapper.selectDueSoon(today, today.plusDays(advanceDays));
    }

    private void recordIfChanged(Long id, String field, String oldVal, String newVal, String reason) {
        if (!Objects.equals(oldVal, newVal)) {
            changeLogService.record("CONTRACT", id, field, oldVal, newVal, reason);
        }
    }

    private ContractVO toVO(Contract c) {
        ContractVO vo = new ContractVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    /**
     * 批量补 customerName / ownerName / deptName，避免 N+1。
     * 一次 page 列表 (size 默认 20) 最多 3 次额外查询。
     */
    private void enrichNames(Collection<ContractVO> vos) {
        if (vos == null || vos.isEmpty()) return;

        Set<Long> customerIds = vos.stream()
                .map(ContractVO::getCustomerId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> userIds = vos.stream()
                .map(ContractVO::getOwnerId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> deptIds = vos.stream()
                .map(ContractVO::getDeptId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, String> customerNameById = customerIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                    .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a, HashMap::new));
        Map<Long, String> userNameById = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a, HashMap::new));
        Map<Long, String> deptNameById = deptIds.isEmpty() ? Map.of()
                : departmentMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a, HashMap::new));

        for (ContractVO vo : vos) {
            if (vo.getCustomerId() != null) {
                vo.setCustomerName(customerNameById.getOrDefault(vo.getCustomerId(), null));
            }
            if (vo.getOwnerId() != null) {
                vo.setOwnerName(userNameById.getOrDefault(vo.getOwnerId(), null));
            }
            if (vo.getDeptId() != null) {
                vo.setDeptName(deptNameById.getOrDefault(vo.getDeptId(), null));
            }
        }
    }
}
