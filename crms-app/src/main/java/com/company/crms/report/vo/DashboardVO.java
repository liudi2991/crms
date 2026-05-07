package com.company.crms.report.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardVO {
    /** KPI - 合同总额 */
    private BigDecimal contractAmount;
    /** KPI - 已回款金额 */
    private BigDecimal paidAmount;
    /** KPI - 待回款金额（unsettled total） */
    private BigDecimal unpaidAmount;
    /** KPI - 逾期金额 */
    private BigDecimal overdueAmount;
    /** KPI - 当月回款 */
    private BigDecimal paidThisMonth;
    /** 合同总数 */
    private long contractCount;
    /** 客户数 */
    private long customerCount;
    /** 30 天内到期合同数 */
    private long contractDueIn30Days;
}
