package com.company.crms.system.service;

/**
 * 统一的硬删除日志服务：业务模块在执行物理删除前调用 record 留痕。
 */
public interface HardDeleteLogService {

    /**
     * 记录硬删除快照。
     * @param bizType 业务类型，如 CUSTOMER / CONTRACT / PAYMENT
     * @param bizId   业务主键
     * @param snapshot 序列化后的整条记录（JSON）
     * @param reason  操作原因
     */
    void record(String bizType, Long bizId, Object snapshot, String reason);
}
