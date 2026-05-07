package com.company.crms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.response.PageResult;
import com.company.crms.system.dto.OperationLogQuery;
import com.company.crms.system.entity.OperationLogEntity;
import com.company.crms.system.mapper.OperationLogMapper;
import com.company.crms.system.service.OperationLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogQueryServiceImpl implements OperationLogQueryService {

    private final OperationLogMapper mapper;

    @Override
    public PageResult<OperationLogEntity> page(OperationLogQuery query) {
        Page<OperationLogEntity> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<OperationLogEntity> w = new QueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like("operator_name", kw)
                    .or().like("action", kw)
                    .or().like("uri", kw));
        }
        if (query.getOperatorId() != null) w.eq("operator_id", query.getOperatorId());
        if (query.getModule() != null && !query.getModule().isBlank()) w.eq("module", query.getModule());
        if (query.getOpType() != null && !query.getOpType().isBlank()) w.eq("op_type", query.getOpType());
        if (query.getBizType() != null && !query.getBizType().isBlank()) w.eq("biz_type", query.getBizType());
        if (query.getResult() != null && !query.getResult().isBlank()) w.eq("result", query.getResult());
        if (query.getFromTime() != null) w.ge("created_at", query.getFromTime());
        if (query.getToTime() != null) w.le("created_at", query.getToTime());
        w.orderByDesc("created_at");

        Page<OperationLogEntity> result = mapper.selectPage(page, w);
        return PageResult.of(result, result.getRecords());
    }
}
