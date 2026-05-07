package com.company.crms.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.customer.entity.Customer;
import com.company.crms.customer.mapper.CustomerMapper;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.service.AgingService;
import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.report.mapper.ReportMapper;
import com.company.crms.report.service.ReportService;
import com.company.crms.report.vo.DashboardVO;
import com.company.crms.report.vo.TodoItemVO;
import com.company.crms.report.vo.TopCustomerVO;
import com.company.crms.report.vo.TrendPointVO;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ContractMapper contractMapper;
    private final CustomerMapper customerMapper;
    private final PaymentPlanMapper planMapper;
    private final PaymentRecordMapper recordMapper;
    private final AgingService agingService;
    private final ReportMapper reportMapper;

    @Override
    @Cacheable(cacheNames = "report-dashboard", unless = "#result == null")
    public DashboardVO dashboard() {
        DashboardVO d = new DashboardVO();
        d.setContractCount(contractMapper.selectCount(new QueryWrapper<Contract>().eq("is_deleted", 0)));
        d.setCustomerCount(customerMapper.selectCount(new QueryWrapper<Customer>().eq("is_deleted", 0)));

        BigDecimal contractAmount = contractMapper.selectList(new QueryWrapper<Contract>().eq("is_deleted", 0)).stream()
                .map(Contract::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        d.setContractAmount(contractAmount);

        List<PaymentPlan> plans = planMapper.selectList(new QueryWrapper<PaymentPlan>().eq("is_deleted", 0));
        BigDecimal paid = plans.stream().map(PaymentPlan::getSettledAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaid = plans.stream().map(PaymentPlan::getUnsettledAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = plans.stream()
                .filter(p -> p.getPlanDate() != null && p.getPlanDate().isBefore(LocalDate.now()))
                .map(PaymentPlan::getUnsettledAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        d.setPaidAmount(paid);
        d.setUnpaidAmount(unpaid);
        d.setOverdueAmount(overdue);

        YearMonth ym = YearMonth.now();
        BigDecimal monthPaid = recordMapper.selectList(new QueryWrapper<PaymentRecord>()
                .eq("is_deleted", 0)
                .notIn("status", "REVERSED", "RED")
                .ge("arrival_date", ym.atDay(1))
                .le("arrival_date", ym.atEndOfMonth())).stream()
                .map(PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        d.setPaidThisMonth(monthPaid);

        d.setContractDueIn30Days(contractMapper.selectDueSoon(LocalDate.now(), LocalDate.now().plusDays(30)).size());
        return d;
    }

    @Override
    @Cacheable(cacheNames = "report-trend", key = "#months")
    public List<TrendPointVO> monthlyTrend(int months) {
        List<LocalDate> firstDays = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            firstDays.add(start.plusMonths(i).atDay(1));
        }
        return reportMapper.monthlyTrend(firstDays);
    }

    @Override
    public List<AgingBucketVO> aging(LocalDate today) {
        return agingService.aging(today);
    }

    @Override
    @Cacheable(cacheNames = "report-top-customers", key = "#metric + ':' + #n")
    public List<TopCustomerVO> topCustomers(int n, String metric) {
        return reportMapper.topCustomers(Math.max(1, Math.min(n, 100)), metric == null ? "PAID" : metric);
    }

    @Override
    public List<TodoItemVO> myTodos(int contractAdvance, int paymentAdvance) {
        Long uid = UserContextHolder.currentUserId();
        if (uid == null) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        LocalDate today = LocalDate.now();
        List<TodoItemVO> result = new ArrayList<>();

        // 合同到期
        List<Contract> contracts = contractMapper.selectList(new QueryWrapper<Contract>()
                .eq("is_deleted", 0)
                .eq("owner_id", uid)
                .eq("status", "EFFECTIVE")
                .between("perform_end_at", today, today.plusDays(contractAdvance)));
        for (Contract c : contracts) {
            result.add(new TodoItemVO("CONTRACT_DUE",
                    String.format("合同 %s 即将到期", c.getCode()),
                    "/contracts/" + c.getId(),
                    c.getPerformEndAt(),
                    c.getAmount(), 0, c.getId()));
        }

        // 回款临期 + 逾期：通过合同 owner_id 过滤
        List<Contract> myContracts = contractMapper.selectList(new QueryWrapper<Contract>()
                .eq("is_deleted", 0)
                .eq("owner_id", uid));
        if (myContracts.isEmpty()) {
            return result;
        }
        List<Long> ids = myContracts.stream().map(Contract::getId).toList();

        List<PaymentPlan> due = planMapper.selectList(new QueryWrapper<PaymentPlan>()
                .eq("is_deleted", 0)
                .gt("unsettled_amount", 0)
                .ne("status", "SETTLED")
                .between("plan_date", today, today.plusDays(paymentAdvance))
                .in("contract_id", ids));
        for (PaymentPlan p : due) {
            result.add(new TodoItemVO("PAYMENT_DUE",
                    String.format("第 %d 期回款临近 (%s)", p.getPeriodNo(), p.getPlanDate()),
                    "/contracts/" + p.getContractId(),
                    p.getPlanDate(), p.getUnsettledAmount(), 0, p.getId()));
        }

        List<PaymentPlan> overdue = planMapper.selectList(new QueryWrapper<PaymentPlan>()
                .eq("is_deleted", 0)
                .gt("unsettled_amount", 0)
                .ne("status", "SETTLED")
                .lt("plan_date", today)
                .in("contract_id", ids));
        for (PaymentPlan p : overdue) {
            int days = (int) java.time.temporal.ChronoUnit.DAYS.between(p.getPlanDate(), today);
            result.add(new TodoItemVO("PAYMENT_OVERDUE",
                    String.format("第 %d 期回款已逾期 %d 天", p.getPeriodNo(), days),
                    "/contracts/" + p.getContractId(),
                    p.getPlanDate(), p.getUnsettledAmount(), days, p.getId()));
        }
        return result;
    }

    @Override
    public void exportExcel(String reportName, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String filename = reportName + "_" + LocalDate.now() + ".xlsx";
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + enc);

        try (OutputStream os = response.getOutputStream();
             ExcelWriter writer = EasyExcel.write(os).inMemory(false).build()) {
            switch (reportName) {
                case "trend" -> writeTrend(writer);
                case "aging" -> writeAging(writer);
                case "top-customers" -> writeTopCustomers(writer);
                case "todos" -> writeTodos(writer);
                default -> throw new BizException(ErrorCode.NOT_FOUND, "不支持的报表 " + reportName);
            }
            writer.finish();
        } catch (IOException e) {
            log.error("export excel error", e);
            throw new BizException(ErrorCode.SYS_ERROR, "导出失败");
        }
    }

    private void writeTrend(ExcelWriter writer) {
        WriteSheet sheet = EasyExcel.writerSheet("月度趋势")
                .head(Arrays.asList(
                        java.util.Collections.singletonList("月份"),
                        java.util.Collections.singletonList("合同签订额"),
                        java.util.Collections.singletonList("回款收款额")
                )).build();
        List<List<Object>> rows = new ArrayList<>();
        for (TrendPointVO p : monthlyTrend(12)) {
            rows.add(Arrays.asList(p.getMonth(), p.getContractAmount(), p.getPaidAmount()));
        }
        writer.write(rows, sheet);
    }

    private void writeAging(ExcelWriter writer) {
        WriteSheet sheet = EasyExcel.writerSheet("账龄分析")
                .head(Arrays.asList(
                        java.util.Collections.singletonList("桶"),
                        java.util.Collections.singletonList("金额"),
                        java.util.Collections.singletonList("条数")
                )).build();
        List<List<Object>> rows = new ArrayList<>();
        for (AgingBucketVO b : agingService.aging(LocalDate.now())) {
            rows.add(Arrays.asList(b.getBucket(), b.getAmount(), b.getCount()));
        }
        writer.write(rows, sheet);
    }

    private void writeTopCustomers(ExcelWriter writer) {
        WriteSheet sheet = EasyExcel.writerSheet("TOP 客户")
                .head(Arrays.asList(
                        java.util.Collections.singletonList("客户ID"),
                        java.util.Collections.singletonList("客户名称"),
                        java.util.Collections.singletonList("金额")
                )).build();
        List<List<Object>> rows = new ArrayList<>();
        for (TopCustomerVO t : topCustomers(50, "PAID")) {
            rows.add(Arrays.asList(t.getCustomerId(), t.getCustomerName(), t.getAmount()));
        }
        writer.write(rows, sheet);
    }

    private void writeTodos(ExcelWriter writer) {
        WriteSheet sheet = EasyExcel.writerSheet("我的待办")
                .head(Arrays.asList(
                        java.util.Collections.singletonList("类型"),
                        java.util.Collections.singletonList("标题"),
                        java.util.Collections.singletonList("日期"),
                        java.util.Collections.singletonList("金额"),
                        java.util.Collections.singletonList("逾期天数")
                )).build();
        List<List<Object>> rows = new ArrayList<>();
        for (TodoItemVO t : myTodos(30, 7)) {
            rows.add(Arrays.asList(t.getType(), t.getTitle(), t.getDate(), t.getAmount(),
                    t.getOverdueDays() == null ? 0 : t.getOverdueDays()));
        }
        writer.write(rows, sheet);
    }

    @Override
    public void warmupCache() {
        log.info("[cache] warmup report caches");
        dashboard();
        monthlyTrend(12);
        topCustomers(10, "PAID");
        topCustomers(10, "UNPAID");
    }

    @Override
    @CacheEvict(cacheNames = {"report-dashboard", "report-trend", "report-top-customers"}, allEntries = true)
    public void evictCache() {
        log.info("[cache] evict report caches");
    }
}
