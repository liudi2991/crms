package com.company.crms.report.mapper;

import com.company.crms.report.vo.TopCustomerVO;
import com.company.crms.report.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReportMapper {

    /** TOP-N 客户：metric=PAID 已回款总额；metric=UNPAID 待回款总额；metric=CONTRACT 合同总额。 */
    @Select("""
        <script>
        <choose>
          <when test="metric == 'UNPAID'">
            SELECT c.id AS customerId, c.name AS customerName,
                   IFNULL(SUM(p.unsettled_amount), 0) AS amount
            FROM customer c
            LEFT JOIN contract t ON t.customer_id = c.id AND t.is_deleted = 0
            LEFT JOIN payment_plan p ON p.contract_id = t.id AND p.is_deleted = 0
            WHERE c.is_deleted = 0
            GROUP BY c.id, c.name
            HAVING amount > 0
            ORDER BY amount DESC LIMIT #{n}
          </when>
          <when test="metric == 'CONTRACT'">
            SELECT c.id AS customerId, c.name AS customerName,
                   IFNULL(SUM(t.amount), 0) AS amount
            FROM customer c
            LEFT JOIN contract t ON t.customer_id = c.id AND t.is_deleted = 0
            WHERE c.is_deleted = 0
            GROUP BY c.id, c.name
            HAVING amount > 0
            ORDER BY amount DESC LIMIT #{n}
          </when>
          <otherwise>
            SELECT c.id AS customerId, c.name AS customerName,
                   IFNULL(SUM(p.settled_amount), 0) AS amount
            FROM customer c
            LEFT JOIN contract t ON t.customer_id = c.id AND t.is_deleted = 0
            LEFT JOIN payment_plan p ON p.contract_id = t.id AND p.is_deleted = 0
            WHERE c.is_deleted = 0
            GROUP BY c.id, c.name
            HAVING amount > 0
            ORDER BY amount DESC LIMIT #{n}
          </otherwise>
        </choose>
        </script>
        """)
    List<TopCustomerVO> topCustomers(@Param("n") int n, @Param("metric") String metric);

    /** 月度趋势：合同签订额 + 回款收款额（剔除红冲）。 */
    @Select("""
        <script>
        SELECT m.month AS month,
          (SELECT IFNULL(SUM(amount), 0) FROM contract
            WHERE is_deleted = 0
              AND signed_at &gt;= m.first_day AND signed_at &lt;= m.last_day) AS contractAmount,
          (SELECT IFNULL(SUM(amount), 0) FROM payment_record
            WHERE is_deleted = 0 AND status NOT IN ('REVERSED', 'RED')
              AND arrival_date &gt;= m.first_day AND arrival_date &lt;= m.last_day) AS paidAmount
        FROM (
          SELECT DATE_FORMAT(d, '%Y-%m') AS month,
                 DATE_FORMAT(d, '%Y-%m-01') AS first_day,
                 LAST_DAY(d) AS last_day
          FROM (
            <foreach collection="months" item="d" separator=" UNION ALL ">
              SELECT #{d} AS d
            </foreach>
          ) tmp
        ) m
        ORDER BY m.month ASC
        </script>
        """)
    List<TrendPointVO> monthlyTrend(@Param("months") List<LocalDate> months);
}
