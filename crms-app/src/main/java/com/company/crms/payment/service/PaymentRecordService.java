package com.company.crms.payment.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.payment.dto.CreateRecordDTO;
import com.company.crms.payment.dto.ManualSettleDTO;
import com.company.crms.payment.dto.RecordQuery;
import com.company.crms.payment.vo.PaymentRecordVO;

import java.util.List;

public interface PaymentRecordService {
    PageResult<PaymentRecordVO> page(RecordQuery query);

    PaymentRecordVO detail(Long id);

    Long create(CreateRecordDTO dto);

    /** 手工核销：将指定 record 与 plans 进行核销。 */
    void manualSettle(ManualSettleDTO dto);

    void remove(Long id);

    void hardDelete(Long id, String reason);

    List<PaymentRecordVO> listByContract(Long contractId);
}
