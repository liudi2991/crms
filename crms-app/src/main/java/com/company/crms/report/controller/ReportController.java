package com.company.crms.report.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.report.service.ReportService;
import com.company.crms.report.vo.DashboardVO;
import com.company.crms.report.vo.TodoItemVO;
import com.company.crms.report.vo.TopCustomerVO;
import com.company.crms.report.vo.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "看板与报表")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "综合看板")
    @SaCheckPermission("report:dashboard")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(reportService.dashboard());
    }

    @Operation(summary = "月度趋势")
    @SaCheckPermission("report:dashboard")
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "12") int months) {
        return Result.ok(reportService.monthlyTrend(months));
    }

    @Operation(summary = "账龄分析")
    @SaCheckPermission("report:payment")
    @GetMapping("/aging")
    public Result<List<AgingBucketVO>> aging() {
        return Result.ok(reportService.aging(LocalDate.now()));
    }

    @Operation(summary = "TOP 客户")
    @SaCheckPermission("report:payment")
    @GetMapping("/top-customers")
    public Result<List<TopCustomerVO>> topCustomers(
            @RequestParam(defaultValue = "10") int n,
            @RequestParam(defaultValue = "PAID") String metric) {
        return Result.ok(reportService.topCustomers(n, metric));
    }

    @Operation(summary = "我的待办（合同到期/回款临期/逾期）")
    @SaCheckLogin
    @GetMapping("/my-todos")
    public Result<List<TodoItemVO>> myTodos(
            @RequestParam(defaultValue = "30") int contractAdvance,
            @RequestParam(defaultValue = "7") int paymentAdvance) {
        return Result.ok(reportService.myTodos(contractAdvance, paymentAdvance));
    }

    @Operation(summary = "导出报表 (xlsx) reportName: trend / aging / top-customers / todos")
    @SaCheckPermission("report:export")
    @GetMapping("/export/{reportName}")
    public void export(@PathVariable String reportName, HttpServletResponse response) {
        reportService.exportExcel(reportName, response);
    }

    @Operation(summary = "失效报表缓存（管理员）")
    @SaCheckPermission("system:manage")
    @PostMapping("/cache/evict")
    public Result<Void> evict() {
        reportService.evictCache();
        return Result.ok();
    }
}
