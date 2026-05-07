package com.company.crms.payment.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.vo.AgingDrillVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PaymentPlanMapper extends BaseMapper<PaymentPlan> {

    /**
     * 取合同下未结清计划，按 plan_date 升序，使用 {@code FOR UPDATE} 防止并发核销重复。
     */
    @Select("""
        SELECT * FROM payment_plan
         WHERE contract_id = #{contractId}
           AND is_deleted = 0
           AND status <> 'SETTLED'
         ORDER BY plan_date ASC, period_no ASC
         FOR UPDATE
        """)
    List<PaymentPlan> selectUnsettledForUpdate(@Param("contractId") Long contractId);

    @Select("""
        <script>
        SELECT * FROM payment_plan
         WHERE id IN
         <foreach collection="ids" open="(" close=")" separator="," item="i">
           #{i}
         </foreach>
         AND is_deleted = 0
         FOR UPDATE
        </script>
        """)
    List<PaymentPlan> selectByIdsForUpdate(@Param("ids") List<Long> ids);

    /** 分页（用于服务层 + DataScope 拦截器；通过命名约定可后续接入）。 */
    @Select("""
        <script>
        SELECT * FROM payment_plan WHERE is_deleted = 0
          <choose>
            <when test="ew != null and ew.nonEmptyOfWhere">AND ${ew.sqlSegment}</when>
            <when test="ew != null and ew.sqlSegment != null and ew.sqlSegment != ''">${ew.sqlSegment}</when>
          </choose>
        </script>
        """)
    IPage<PaymentPlan> selectPagePlans(IPage<PaymentPlan> page,
                                       @Param(Constants.WRAPPER) Wrapper<PaymentPlan> wrapper);

    @Select("SELECT COUNT(1) FROM payment_plan WHERE contract_id = #{contractId} AND is_deleted = 0")
    long countByContract(@Param("contractId") Long contractId);

    @Update("UPDATE payment_plan SET is_deleted = 1 WHERE contract_id = #{contractId} AND is_deleted = 0")
    int softDeleteByContract(@Param("contractId") Long contractId);

    @Select("""
        SELECT contract_id AS contractId, IFNULL(SUM(plan_amount), 0) AS amt
        FROM payment_plan WHERE contract_id = #{contractId} AND is_deleted = 0
        """)
    BigDecimal sumPlanAmount(@Param("contractId") Long contractId);

    /**
     * 把所有 plan_date < today 且 status != SETTLED 的计划标记为逾期。
     */
    @Update("""
        UPDATE payment_plan
           SET is_overdue = 1,
               overdue_days = DATEDIFF(#{today}, plan_date),
               updated_at = NOW()
         WHERE is_deleted = 0
           AND status <> 'SETTLED'
           AND plan_date < #{today}
        """)
    int markOverdue(@Param("today") LocalDate today);

    /**
     * 账龄钻取：在指定 [minDays, maxDays] 区间内列出所有 unsettled_amount > 0 的计划。
     * minDays/maxDays 以 today - plan_date 计算。
     */
    @Select("""
        <script>
        SELECT
          p.id AS planId,
          p.contract_id AS contractId,
          c.code AS contractCode,
          c.name AS contractName,
          p.period_no AS periodNo,
          p.plan_date AS planDate,
          p.unsettled_amount AS unsettledAmount,
          DATEDIFF(#{today}, p.plan_date) AS overdueDays
        FROM payment_plan p
        LEFT JOIN contract c ON c.id = p.contract_id
        WHERE p.is_deleted = 0
          AND p.unsettled_amount &gt; 0
          AND p.status != 'SETTLED'
          <choose>
            <when test="bucket == 'UNDUE'">AND DATEDIFF(#{today}, p.plan_date) &lt;= 0</when>
            <when test="bucket == '0-30'">AND DATEDIFF(#{today}, p.plan_date) BETWEEN 1 AND 30</when>
            <when test="bucket == '31-60'">AND DATEDIFF(#{today}, p.plan_date) BETWEEN 31 AND 60</when>
            <when test="bucket == '61-90'">AND DATEDIFF(#{today}, p.plan_date) BETWEEN 61 AND 90</when>
            <when test="bucket == '90+'">AND DATEDIFF(#{today}, p.plan_date) &gt; 90</when>
          </choose>
        ORDER BY p.plan_date ASC
        LIMIT #{offset}, #{limit}
        </script>
        """)
    List<AgingDrillVO> drillBucket(@Param("today") LocalDate today,
                                   @Param("bucket") String bucket,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);
}
