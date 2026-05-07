package com.company.crms.contract.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.contract.dto.ContractQuery;
import com.company.crms.contract.dto.CreateContractDTO;
import com.company.crms.contract.dto.UpdateContractDTO;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.enums.ContractStatus;
import com.company.crms.contract.vo.ContractVO;

import java.util.List;

public interface ContractService {

    PageResult<ContractVO> page(ContractQuery query);

    ContractVO detail(Long id);

    Long create(CreateContractDTO dto);

    void update(Long id, UpdateContractDTO dto);

    void softDelete(Long id);

    void hardDelete(Long id, String reason);

    /**
     * 根据合同状态机迁移状态，写 change_log。
     */
    void transition(Long contractId, ContractStatus to, String reason);

    /**
     * 定时任务调用：自动把过到期日的合同迁到 EXPIRED。
     */
    int markExpiredAuto();

    /**
     * 找出未来 advanceDays 内到期的合同。
     */
    List<Contract> dueSoon(int advanceDays);
}
