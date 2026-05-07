package com.company.crms.common.aop;

import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.security.UserContext;
import com.company.crms.common.security.UserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link OperationLog} 切面：捕获操作并异步写入 operation_log 表。
 *
 * <p>记录字段：操作人、IP、模块、动作、参数、耗时、结果。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ObjectMapper objectMapper;
    private final OperationLogWriter writer;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        Throwable error = null;
        Object retVal;
        try {
            retVal = pjp.proceed();
            return retVal;
        } catch (Throwable t) {
            result = "FAIL";
            error = t;
            throw t;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            try {
                Map<String, Object> entry = buildEntry(pjp, operationLog, result, error, elapsed);
                writer.write(entry);
            } catch (Exception e) {
                log.warn("write operation log failed", e);
            }
        }
    }

    private Map<String, Object> buildEntry(ProceedingJoinPoint pjp, OperationLog ann,
                                           String result, Throwable error, long elapsed) {
        Map<String, Object> m = new HashMap<>();
        UserContext ctx = UserContextHolder.get();
        m.put("operatorId", ctx == null ? null : ctx.getUserId());
        m.put("operatorName", ctx == null ? null : ctx.getUsername());
        HttpServletRequest req = currentRequest();
        if (req != null) {
            m.put("ip", clientIp(req));
            m.put("uri", req.getRequestURI());
            m.put("method", req.getMethod());
        }
        m.put("module", ann.module());
        m.put("action", ann.action());
        m.put("type", ann.type());
        if (ann.recordParams()) {
            m.put("params", safeStringify(pjp.getArgs()));
        }
        m.put("result", result);
        if (error != null) {
            m.put("errorMessage", error.getMessage());
        }
        m.put("durationMs", (int) elapsed);
        m.put("createdAt", LocalDateTime.now());
        return m;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return req.getRemoteAddr();
    }

    private String safeStringify(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    /**
     * 异步写入器，避免阻塞业务线程。具体实现注入 OperationLog Mapper。
     */
    public interface OperationLogWriter {
        @Async
        void write(Map<String, Object> entry);
    }
}
