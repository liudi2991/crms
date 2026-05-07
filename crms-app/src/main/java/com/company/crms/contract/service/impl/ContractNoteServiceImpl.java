package com.company.crms.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.entity.ContractNote;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.contract.mapper.ContractNoteMapper;
import com.company.crms.contract.service.ContractNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractNoteServiceImpl implements ContractNoteService {

    private final ContractNoteMapper noteMapper;
    private final ContractMapper contractMapper;

    @Override
    public List<ContractNote> listByContract(Long contractId) {
        QueryWrapper<ContractNote> w = new QueryWrapper<ContractNote>()
                .eq("contract_id", contractId)
                .orderByDesc("created_at");
        return noteMapper.selectList(w);
    }

    @Override
    @OperationLog(module = "合同", action = "新增备注", type = "CREATE")
    @Transactional
    public Long create(Long contractId, String content) {
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "备注内容不能为空");
        }
        if (contractMapper.selectById(contractId) == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        ContractNote n = new ContractNote();
        n.setId(SnowflakeIdGenerator.next());
        n.setContractId(contractId);
        n.setAuthorId(UserContextHolder.currentUserId());
        n.setContent(content.length() > 1000 ? content.substring(0, 1000) : content);
        n.setCreatedAt(LocalDateTime.now());
        noteMapper.insert(n);
        return n.getId();
    }

    @Override
    @OperationLog(module = "合同", action = "删除备注", type = "DELETE")
    @Transactional
    public void remove(Long id) {
        ContractNote n = noteMapper.selectById(id);
        if (n == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "备注不存在");
        }
        if (!UserContextHolder.require().isSuperAdmin()
                && !n.getAuthorId().equals(UserContextHolder.currentUserId())) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "只能删除自己的备注");
        }
        noteMapper.deleteById(id);
    }
}
