package com.company.crms.system.service.impl;

import com.company.crms.common.aop.OperationLogAspect;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.system.entity.OperationLogEntity;
import com.company.crms.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志写入器（异步）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogWriterImpl implements OperationLogAspect.OperationLogWriter {

    private final OperationLogMapper mapper;

    @Override
    @Async("logExecutor")
    public void write(Map<String, Object> entry) {
        try {
            OperationLogEntity e = new OperationLogEntity();
            e.setId(SnowflakeIdGenerator.next());
            e.setOperatorId((Long) entry.get("operatorId"));
            e.setOperatorName((String) entry.get("operatorName"));
            e.setOperatorIp((String) entry.get("ip"));
            e.setModule((String) entry.get("module"));
            e.setAction((String) entry.get("action"));
            e.setOpType((String) entry.get("type"));
            e.setBizType((String) entry.get("bizType"));
            Object bizId = entry.get("bizId");
            e.setBizId(bizId == null ? null : Long.valueOf(bizId.toString()));
            e.setUri((String) entry.get("uri"));
            e.setMethod((String) entry.get("method"));
            String params = (String) entry.get("params");
            if (params != null && params.length() > 4000) {
                params = params.substring(0, 4000);
            }
            e.setParamsJson(params);
            e.setResult((String) entry.get("result"));
            e.setErrorMessage((String) entry.get("errorMessage"));
            e.setDurationMs((Integer) entry.get("durationMs"));
            e.setCreatedAt((LocalDateTime) entry.getOrDefault("createdAt", LocalDateTime.now()));
            mapper.insert(e);
        } catch (Exception ex) {
            log.warn("persist operation log failed: {}", ex.getMessage());
        }
    }
}
