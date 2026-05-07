package com.company.crms.report.service;

import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.report.vo.DashboardVO;
import com.company.crms.report.vo.TodoItemVO;
import com.company.crms.report.vo.TopCustomerVO;
import com.company.crms.report.vo.TrendPointVO;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** 综合 KPI 看板。 */
    DashboardVO dashboard();

    /** 月度合同/回款趋势（默认最近 12 个月）。 */
    List<TrendPointVO> monthlyTrend(int months);

    /** 应收账龄。 */
    List<AgingBucketVO> aging(LocalDate today);

    /** TOP N 客户（PAID/UNPAID/CONTRACT）。 */
    List<TopCustomerVO> topCustomers(int n, String metric);

    /** 我的待办：合同到期 / 回款临期 / 回款逾期。 */
    List<TodoItemVO> myTodos(int contractAdvance, int paymentAdvance);

    /** 导出 Excel：报表名 trend / aging / top-customers / todos。 */
    void exportExcel(String reportName, HttpServletResponse response);

    /** 缓存预热 / 失效，由调度任务调用。 */
    void warmupCache();

    void evictCache();
}
