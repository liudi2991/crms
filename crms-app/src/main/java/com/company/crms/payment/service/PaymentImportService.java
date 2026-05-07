package com.company.crms.payment.service;

import com.company.crms.payment.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface PaymentImportService {
    /**
     * 通过 Excel 批量导入实际回款（DSS §3.4.3）。
     * 列：合同编号 / 到账日期 / 金额 / 付款方 / 凭证号 / 备注
     * 全部成功才提交，任一行失败仅记录错误并跳过。
     */
    ImportResultVO importRecords(MultipartFile file);
}
