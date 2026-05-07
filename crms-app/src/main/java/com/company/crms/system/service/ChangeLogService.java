package com.company.crms.system.service;

import com.company.crms.system.entity.ChangeLog;

import java.util.List;

public interface ChangeLogService {

    /** 记录单字段变更。 */
    void record(String bizType, Long bizId, String field, String oldValue, String newValue, String reason);

    /** 列出某业务实体的变更（按时间倒序，限制条数）。 */
    List<ChangeLog> listByBiz(String bizType, Long bizId, int limit);
}
