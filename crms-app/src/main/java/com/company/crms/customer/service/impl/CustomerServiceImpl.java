package com.company.crms.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.CodeGenerator;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.customer.dto.CreateCustomerDTO;
import com.company.crms.customer.dto.CustomerQuery;
import com.company.crms.customer.dto.MergeCustomerDTO;
import com.company.crms.customer.dto.UpdateCustomerDTO;
import com.company.crms.customer.entity.Customer;
import com.company.crms.customer.mapper.CustomerMapper;
import com.company.crms.customer.service.CustomerContactService;
import com.company.crms.customer.service.CustomerService;
import com.company.crms.customer.vo.CustomerAggregateVO;
import com.company.crms.customer.vo.CustomerDuplicateVO;
import com.company.crms.customer.vo.CustomerVO;
import com.company.crms.system.service.ChangeLogService;
import com.company.crms.system.service.HardDeleteLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CodeGenerator codeGenerator;
    private final HardDeleteLogService hardDeleteLogService;
    private final ChangeLogService changeLogService;
    private final CustomerContactService contactService;

    @Override
    public PageResult<CustomerVO> page(CustomerQuery query) {
        Page<Customer> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like("name", kw)
                    .or().like("short_name", kw)
                    .or().like("code", kw)
                    .or().eq("uscc", kw));
        }
        if (query.getType() != null) wrapper.eq("type", query.getType());
        if (query.getLevel() != null) wrapper.eq("level", query.getLevel());
        if (query.getStatus() != null) wrapper.eq("status", query.getStatus());
        if (query.getOwnerId() != null) wrapper.eq("owner_id", query.getOwnerId());
        wrapper.orderByDesc("created_at");

        Page<Customer> result = (Page<Customer>) customerMapper.selectPageWithDataScope(page, wrapper);
        List<CustomerVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result, vos);
    }

    @Override
    public CustomerVO detail(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        return toVO(c);
    }

    @Override
    @OperationLog(module = "客户", action = "新建客户", type = "CREATE")
    @Transactional
    public Long create(CreateCustomerDTO dto) {
        Customer c = new Customer();
        c.setId(SnowflakeIdGenerator.next());
        c.setCode(codeGenerator.customerCode());
        BeanUtils.copyProperties(dto, c);
        c.setStatus("ACTIVE");
        c.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : UserContextHolder.currentUserId());
        c.setDeptId(UserContextHolder.require().getDeptId());
        if (c.getUscc() != null && c.getUscc().isBlank()) {
            c.setUscc(null);
        }
        try {
            customerMapper.insert(c);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CU_USCC_DUPLICATE);
        }
        return c.getId();
    }

    @Override
    @OperationLog(module = "客户", action = "更新客户", type = "UPDATE")
    @Transactional
    public void update(UpdateCustomerDTO dto) {
        Customer existing = customerMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        Customer upd = new Customer();
        upd.setId(dto.getId());
        upd.setVersion(dto.getVersion());
        BeanUtils.copyProperties(dto, upd);
        // 仅超管可改变所有权
        if (dto.getOwnerId() != null && !UserContextHolder.require().isSuperAdmin()
                && !Objects.equals(dto.getOwnerId(), existing.getOwnerId())) {
            upd.setOwnerId(existing.getOwnerId());
        }
        try {
            int rows = customerMapper.updateById(upd);
            if (rows == 0) {
                throw new BizException(ErrorCode.CU_NOT_FOUND, "已被其他用户修改，请刷新后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CU_USCC_DUPLICATE);
        }
    }

    @Override
    @OperationLog(module = "客户", action = "软删除客户", type = "DELETE")
    @Transactional
    public void softDelete(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        if (customerMapper.countActiveContracts(id) > 0) {
            throw new BizException(ErrorCode.CU_HAS_CONTRACTS);
        }
        customerMapper.deleteById(id);
    }

    @Override
    @OperationLog(module = "客户", action = "硬删除客户", type = "HARD_DELETE", recordParams = false)
    @Transactional
    public void hardDelete(Long id, String reason) {
        if (!UserContextHolder.require().isSuperAdmin()) {
            throw new BizException(ErrorCode.SY_HARD_DELETE_DENIED);
        }
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        hardDeleteLogService.record("CUSTOMER", id, c, reason);
        customerMapper.physicalDelete(id);
        log.warn("HARD DELETE customer id={} by={} reason={}", id, UserContextHolder.currentUserId(), reason);
    }

    @Override
    @OperationLog(module = "客户", action = "停用客户")
    public void disable(Long id) {
        Customer c = ensure(id);
        Customer upd = new Customer();
        upd.setId(c.getId());
        upd.setVersion(c.getVersion());
        upd.setStatus("DISABLED");
        customerMapper.updateById(upd);
    }

    @Override
    @OperationLog(module = "客户", action = "启用客户")
    public void enable(Long id) {
        Customer c = ensure(id);
        Customer upd = new Customer();
        upd.setId(c.getId());
        upd.setVersion(c.getVersion());
        upd.setStatus("ACTIVE");
        customerMapper.updateById(upd);
    }

    @Override
    @OperationLog(module = "客户", action = "客户合并", type = "UPDATE")
    @Transactional
    public void merge(MergeCustomerDTO dto) {
        if (dto.getMergedIds().contains(dto.getMainId())) {
            throw new BizException(ErrorCode.CU_MERGE_INVALID, "主体客户不能在被合并集合中");
        }
        Customer main = customerMapper.selectById(dto.getMainId());
        if (main == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND, "主体客户不存在");
        }
        for (Long mid : dto.getMergedIds()) {
            Customer m = customerMapper.selectById(mid);
            if (m == null) {
                throw new BizException(ErrorCode.CU_NOT_FOUND, "被合并客户 " + mid + " 不存在");
            }
            if ("MERGED".equals(m.getStatus())) {
                throw new BizException(ErrorCode.CU_MERGE_INVALID, "客户 " + m.getName() + " 已被合并");
            }
        }

        Long operatorId = UserContextHolder.currentUserId();
        int contracts = customerMapper.reassignContracts(dto.getMergedIds(), dto.getMainId());
        int contacts = customerMapper.reassignContacts(dto.getMergedIds(), dto.getMainId());
        int marked = customerMapper.markMerged(dto.getMergedIds(), dto.getMainId(), operatorId);

        for (Long mid : dto.getMergedIds()) {
            changeLogService.record("CUSTOMER", mid, "merged_to", null,
                    String.valueOf(dto.getMainId()), dto.getReason());
            changeLogService.record("CUSTOMER", dto.getMainId(), "merge_in", null,
                    String.valueOf(mid), dto.getReason());
        }
        log.info("customer merge done main={} merged={} contracts={} contacts={} marked={}",
                dto.getMainId(), dto.getMergedIds(), contracts, contacts, marked);
    }

    @Override
    public List<CustomerDuplicateVO> checkDuplicate(String name, String uscc, Long selfId) {
        if ((name == null || name.isBlank()) && (uscc == null || uscc.isBlank())) {
            return new ArrayList<>();
        }
        return customerMapper.checkDuplicate(
                name == null ? null : name.trim(),
                uscc == null ? null : uscc.trim(),
                selfId);
    }

    @Override
    public CustomerAggregateVO aggregate(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        CustomerAggregateVO vo = new CustomerAggregateVO();
        vo.setCustomer(toVO(c));
        vo.setContacts(contactService.listByCustomer(id));
        vo.setRecentContracts(customerMapper.recentContracts(id));
        vo.setRecentChanges(changeLogService.listByBiz("CUSTOMER", id, 50));
        vo.setTotalContracts(customerMapper.countContracts(id));
        BigDecimal sum = customerMapper.sumContractAmount(id);
        vo.setTotalContractAmount(sum == null ? BigDecimal.ZERO : sum);
        return vo;
    }

    private Customer ensure(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        return c;
    }

    private CustomerVO toVO(Customer c) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
