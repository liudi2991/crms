package com.company.crms.contract.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.company.crms.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("""
        <script>
        SELECT * FROM contract WHERE is_deleted = 0
          <choose>
            <when test="ew != null and ew.nonEmptyOfWhere">AND ${ew.sqlSegment}</when>
            <when test="ew != null and ew.sqlSegment != null and ew.sqlSegment != ''">${ew.sqlSegment}</when>
          </choose>
        </script>
        """)
    IPage<Contract> selectPageWithDataScope(IPage<Contract> page,
                                            @Param(Constants.WRAPPER) Wrapper<Contract> wrapper);

    /** 找出 [today, today + advanceDays] 即将到期且仍 EFFECTIVE 的合同。 */
    @Select("""
        SELECT * FROM contract
         WHERE is_deleted = 0
           AND status = 'EFFECTIVE'
           AND perform_end_at BETWEEN #{from} AND #{to}
        """)
    List<Contract> selectDueSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 把已经超过到期日的 EFFECTIVE 合同迁到 EXPIRED。 */
    @Update("""
        UPDATE contract
           SET status = 'EXPIRED', updated_at = NOW()
         WHERE is_deleted = 0 AND status = 'EFFECTIVE' AND perform_end_at < #{today}
        """)
    int markExpired(@Param("today") LocalDate today);

    /** 物理删除（绕过 @TableLogic）。 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM contract WHERE id = #{id}")
    int physicalDelete(@Param("id") Long id);

    /** 是否存在未结清的回款计划（含红冲时已结清的不算）。 */
    @Select("""
        SELECT COUNT(1) FROM payment_plan
         WHERE contract_id = #{contractId} AND is_deleted = 0
        """)
    long countPaymentPlans(@Param("contractId") Long contractId);

    /** 软删合同时级联软删附件、备注、回款。 */
    @Update("UPDATE contract_attachment SET is_deleted = 1 WHERE contract_id = #{contractId} AND is_deleted = 0")
    int softDeleteAttachments(@Param("contractId") Long contractId);

    @Update("UPDATE payment_plan SET is_deleted = 1 WHERE contract_id = #{contractId} AND is_deleted = 0")
    int softDeletePaymentPlans(@Param("contractId") Long contractId);

    /** 硬删合同时级联物理清理（按 FK 依赖序逆向）。 */
    @org.apache.ibatis.annotations.Delete("""
        DELETE s FROM payment_settlement s
         JOIN payment_record r ON s.payment_record_id = r.id
         WHERE r.contract_id = #{contractId}
        """)
    int physicalDeleteSettlementsByContract(@Param("contractId") Long contractId);

    @org.apache.ibatis.annotations.Delete(
        "DELETE FROM payment_record WHERE contract_id = #{contractId}")
    int physicalDeletePaymentRecords(@Param("contractId") Long contractId);

    @org.apache.ibatis.annotations.Delete(
        "DELETE FROM payment_plan WHERE contract_id = #{contractId}")
    int physicalDeletePaymentPlans(@Param("contractId") Long contractId);

    @org.apache.ibatis.annotations.Delete(
        "DELETE FROM contract_attachment WHERE contract_id = #{contractId}")
    int physicalDeleteAttachments(@Param("contractId") Long contractId);

    @org.apache.ibatis.annotations.Delete(
        "DELETE FROM contract_note WHERE contract_id = #{contractId}")
    int physicalDeleteNotes(@Param("contractId") Long contractId);
}
