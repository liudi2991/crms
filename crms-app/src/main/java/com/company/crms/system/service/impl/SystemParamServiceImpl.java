package com.company.crms.system.service.impl;

import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.system.entity.SystemParam;
import com.company.crms.system.mapper.SystemParamMapper;
import com.company.crms.system.service.SystemParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemParamServiceImpl implements SystemParamService {

    private final SystemParamMapper mapper;

    @Override
    public List<SystemParam> listAll() {
        return mapper.selectList(null);
    }

    @Override
    @Cacheable(cacheNames = "system-params", key = "#key", unless = "#result == null")
    public String get(String key) {
        SystemParam p = mapper.selectByKey(key);
        return p == null ? null : p.getParamValue();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @CacheEvict(cacheNames = "system-params", key = "#key")
    @OperationLog(module = "系统", action = "更新系统参数")
    public void update(String key, String value) {
        SystemParam p = mapper.selectByKey(key);
        if (p == null) {
            throw new BizException(ErrorCode.SY_PARAM_NOT_FOUND);
        }
        p.setParamValue(value);
        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(UserContextHolder.currentUserId());
        mapper.updateById(p);
    }
}
