package com.company.crms.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 业务编号生成器：客户编号、合同编号等。
 *
 * <ul>
 *   <li>客户：{@code CU-yyyyMM-NNNN}</li>
 *   <li>合同：{@code HT-yyyyMM-NNNN}</li>
 * </ul>
 *
 * <p>使用 Redis INCR 保证并发唯一；Key 按月切换，3 个月后过期清理。
 */
@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate redis;

    public String customerCode() {
        return generate("CU", "code:customer:");
    }

    public String contractCode() {
        return generate("HT", "code:contract:");
    }

    private String generate(String prefix, String redisKeyPrefix) {
        String month = LocalDate.now().format(MONTH);
        String key = redisKeyPrefix + month;
        Long n = redis.opsForValue().increment(key);
        if (n != null && n == 1L) {
            redis.expire(key, 100, TimeUnit.DAYS);
        }
        return String.format("%s-%s-%04d", prefix, month, n == null ? 1 : n);
    }
}
