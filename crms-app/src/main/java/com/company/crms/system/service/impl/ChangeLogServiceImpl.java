package com.company.crms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.system.entity.ChangeLog;
import com.company.crms.system.mapper.ChangeLogMapper;
import com.company.crms.system.service.ChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeLogServiceImpl implements ChangeLogService {

    private final ChangeLogMapper mapper;

    @Override
    public void record(String bizType, Long bizId, String field, String oldValue, String newValue, String reason) {
        ChangeLog e = new ChangeLog();
        e.setId(SnowflakeIdGenerator.next());
        e.setBizType(bizType);
        e.setBizId(bizId);
        e.setField(field);
        e.setOldValue(truncate(oldValue));
        e.setNewValue(truncate(newValue));
        e.setReason(reason);
        e.setOperatorId(UserContextHolder.currentUserId());
        e.setOperatedAt(LocalDateTime.now());
        mapper.insert(e);
    }

    @Override
    public List<ChangeLog> listByBiz(String bizType, Long bizId, int limit) {
        QueryWrapper<ChangeLog> w = new QueryWrapper<ChangeLog>()
                .eq("biz_type", bizType)
                .eq("biz_id", bizId)
                .orderByDesc("operated_at")
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return mapper.selectList(w);
    }

    private String truncate(String v) {
        if (v == null) return null;
        return v.length() > 1000 ? v.substring(0, 1000) : v;
    }
}
