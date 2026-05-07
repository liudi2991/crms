package com.company.crms.payment.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.payment.dto.CreateRecordDTO;
import com.company.crms.payment.service.PaymentImportService;
import com.company.crms.payment.service.PaymentRecordService;
import com.company.crms.payment.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentImportServiceImpl implements PaymentImportService {

    private final ContractMapper contractMapper;
    private final PaymentRecordService recordService;

    @Override
    @OperationLog(module = "回款", action = "批量导入", recordParams = false)
    public ImportResultVO importRecords(MultipartFile file) {
        ImportResultVO result = new ImportResultVO();
        try {
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
                    int rowIndex = ctx.readRowHolder().getRowIndex() + 1;
                    result.setTotal(result.getTotal() + 1);
                    try {
                        importRow(row, rowIndex);
                        result.setSuccess(result.getSuccess() + 1);
                    } catch (Exception ex) {
                        result.setFailed(result.getFailed() + 1);
                        result.getErrors().add(new ImportResultVO.RowError(rowIndex, ex.getMessage()));
                        log.warn("[import] row {} failed: {}", rowIndex, ex.getMessage());
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    /* nothing */
                }
            }).headRowNumber(1).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException("导入失败：" + e.getMessage(), e);
        }
        return result;
    }

    private void importRow(Map<Integer, String> row, int rowIndex) {
        String contractCode = trim(row.get(0));
        String arrivalStr = trim(row.get(1));
        String amountStr = trim(row.get(2));
        String payer = trim(row.get(3));
        String voucherNo = trim(row.get(4));
        String remark = trim(row.get(5));

        if (contractCode == null) {
            throw new IllegalArgumentException("合同编号不能为空");
        }
        Contract c = contractMapper.selectOne(new QueryWrapper<Contract>()
                .eq("code", contractCode)
                .eq("is_deleted", 0));
        if (c == null) {
            throw new IllegalArgumentException("合同 [" + contractCode + "] 不存在");
        }
        LocalDate arrival = parseDate(arrivalStr);
        BigDecimal amount = parseAmount(amountStr);

        CreateRecordDTO dto = new CreateRecordDTO();
        dto.setContractId(c.getId());
        dto.setArrivalDate(arrival);
        dto.setAmount(amount);
        dto.setPayer(payer);
        dto.setVoucherNo(voucherNo);
        dto.setRemark(remark);
        recordService.create(dto);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseDate(String s) {
        if (s == null) throw new IllegalArgumentException("到账日期不能为空");
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy/M/d"));
            } catch (Exception e2) {
                throw new IllegalArgumentException("到账日期格式错误，请用 yyyy-MM-dd");
            }
        }
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null) throw new IllegalArgumentException("金额不能为空");
        try {
            BigDecimal v = new BigDecimal(s.replace(",", ""));
            if (v.signum() <= 0) throw new IllegalArgumentException("金额必须大于 0");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金额格式错误");
        }
    }
}
