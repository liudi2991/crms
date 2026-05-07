package com.company.crms.system.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.system.dto.OperationLogQuery;
import com.company.crms.system.entity.OperationLogEntity;

public interface OperationLogQueryService {
    PageResult<OperationLogEntity> page(OperationLogQuery query);
}
