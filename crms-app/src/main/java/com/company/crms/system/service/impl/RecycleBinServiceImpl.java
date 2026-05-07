package com.company.crms.system.service.impl;

import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.system.dto.RecycleBinQuery;
import com.company.crms.system.mapper.RecycleBinMapper;
import com.company.crms.system.service.HardDeleteLogService;
import com.company.crms.system.service.RecycleBinService;
import com.company.crms.system.vo.RecycleBinItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    private final RecycleBinMapper mapper;
    private final HardDeleteLogService hardDeleteLogService;

    @Override
    public PageResult<RecycleBinItemVO> page(RecycleBinQuery query) {
        String kw = query.getKeyword() == null || query.getKeyword().isBlank() ? null : query.getKeyword().trim();
        int offset = (query.getPage() - 1) * query.getSize();
        int size = query.getSize();

        List<RecycleBinItemVO> items;
        long total;
        switch (query.getBizType()) {
            case "CUSTOMER" -> {
                items = mapper.pageCustomer(kw, offset, size);
                total = mapper.countCustomer(kw);
            }
            case "CONTRACT" -> {
                items = mapper.pageContract(kw, offset, size);
                total = mapper.countContract(kw);
            }
            case "PAYMENT_RECORD" -> {
                items = mapper.pagePaymentRecord(kw, offset, size);
                total = mapper.countPaymentRecord(kw);
            }
            default -> throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "不支持的回收站类型: " + query.getBizType());
        }

        PageResult<RecycleBinItemVO> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(query.getPage());
        result.setSize(query.getSize());
        return result;
    }

    @Override
    @OperationLog(module = "系统", action = "回收站还原")
    @Transactional
    public void restore(String bizType, Long id) {
        int rows = switch (bizType) {
            case "CUSTOMER" -> mapper.restoreCustomer(id);
            case "CONTRACT" -> mapper.restoreContract(id);
            case "PAYMENT_RECORD" -> mapper.restorePaymentRecord(id);
            default -> throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "不支持的回收站类型: " + bizType);
        };
        if (rows == 0) {
            throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "记录不存在或已还原");
        }
    }

    @Override
    @OperationLog(module = "系统", action = "硬删除", recordParams = false)
    @Transactional
    public void hardDelete(String bizType, Long id, String reason) {
        if (!UserContextHolder.require().isSuperAdmin()) {
            throw new BizException(ErrorCode.SY_HARD_DELETE_DENIED);
        }
        Map<String, Object> snapshot;
        int rows;
        switch (bizType) {
            case "CUSTOMER" -> {
                snapshot = mapper.selectCustomerSnapshot(id);
                if (snapshot == null) throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "记录不存在");
                hardDeleteLogService.record(bizType, id, snapshot, reason);
                rows = mapper.hardDeleteCustomer(id);
            }
            case "CONTRACT" -> {
                snapshot = mapper.selectContractSnapshot(id);
                if (snapshot == null) throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "记录不存在");
                hardDeleteLogService.record(bizType, id, snapshot, reason);
                rows = mapper.hardDeleteContract(id);
            }
            case "PAYMENT_RECORD" -> {
                snapshot = mapper.selectPaymentRecordSnapshot(id);
                if (snapshot == null) throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "记录不存在");
                hardDeleteLogService.record(bizType, id, snapshot, reason);
                rows = mapper.hardDeletePaymentRecord(id);
            }
            default -> throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "不支持的回收站类型: " + bizType);
        }
        if (rows == 0) {
            throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND, "记录不存在或不在回收站中");
        }
    }
}
