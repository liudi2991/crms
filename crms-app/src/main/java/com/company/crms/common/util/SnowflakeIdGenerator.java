package com.company.crms.common.util;

/**
 * 雪花 ID 生成器（简化版）。
 *
 * <p>结构：1 bit 符号 + 41 bit 时间戳 + 10 bit 机器位 + 12 bit 序列。
 * 多实例部署时通过环境变量 {@code CRMS_NODE_ID}（0–1023）区分。
 */
public final class SnowflakeIdGenerator {

    private static final long EPOCH = 1735689600000L; // 2025-01-01 00:00:00 UTC

    private static final long NODE_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_NODE = (1L << NODE_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long NODE_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_BITS;

    private static final long NODE_ID = resolveNodeId();

    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    private SnowflakeIdGenerator() {
    }

    public static synchronized long next() {
        long ts = System.currentTimeMillis();
        if (ts < lastTimestamp) {
            // 时钟回拨，等待
            ts = lastTimestamp;
        }
        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                ts = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = ts;
        return ((ts - EPOCH) << TIMESTAMP_SHIFT) | (NODE_ID << NODE_SHIFT) | sequence;
    }

    private static long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    private static long resolveNodeId() {
        String env = System.getenv("CRMS_NODE_ID");
        try {
            long node = env == null ? 0L : Long.parseLong(env);
            if (node < 0 || node > MAX_NODE) {
                throw new IllegalStateException("CRMS_NODE_ID 必须在 0-" + MAX_NODE);
            }
            return node;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
