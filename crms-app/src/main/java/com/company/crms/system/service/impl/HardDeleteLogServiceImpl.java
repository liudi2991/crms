package com.company.crms.system.service.impl;

import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.system.entity.HardDeleteLog;
import com.company.crms.system.mapper.HardDeleteLogMapper;
import com.company.crms.system.service.HardDeleteLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HardDeleteLogServiceImpl implements HardDeleteLogService {

    private final HardDeleteLogMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(String bizType, Long bizId, Object snapshot, String reason) {
        HardDeleteLog e = new HardDeleteLog();
        e.setId(SnowflakeIdGenerator.next());
        e.setOperatorId(UserContextHolder.currentUserId());
        e.setBizType(bizType);
        e.setBizId(bizId);
        try {
            e.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (Exception ex) {
            log.warn("serialize snapshot failed for hard delete log: {}", ex.getMessage());
            e.setSnapshotJson("{}");
        }
        e.setReason(reason);
        e.setCreatedAt(LocalDateTime.now());
        mapper.insert(e);
    }
}
