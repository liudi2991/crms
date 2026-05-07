package com.company.crms.payment.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.payment.dto.CreatePlanDTO;
import com.company.crms.payment.dto.GeneratePlansDTO;
import com.company.crms.payment.dto.PlanQuery;
import com.company.crms.payment.dto.UpdatePlanDTO;
import com.company.crms.payment.vo.PaymentPlanVO;

import java.util.List;

public interface PaymentPlanService {

    PageResult<PaymentPlanVO> page(PlanQuery query);

    List<PaymentPlanVO> listByContract(Long contractId);

    PaymentPlanVO detail(Long id);

    Long create(CreatePlanDTO dto);

    /** 根据合同金额按频率（MONTHLY / QUARTERLY / ONCE）批量生成计划。 */
    List<Long> generate(GeneratePlansDTO dto);

    void update(Long id, UpdatePlanDTO dto);

    void remove(Long id);

    /** 标记逾期：plan_date < today 且未结清。返回受影响行数。 */
    int markOverdueAuto();
}
