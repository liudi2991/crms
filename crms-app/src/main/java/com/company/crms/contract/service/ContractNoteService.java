package com.company.crms.contract.service;

import com.company.crms.contract.entity.ContractNote;

import java.util.List;

public interface ContractNoteService {
    List<ContractNote> listByContract(Long contractId);

    Long create(Long contractId, String content);

    void remove(Long id);
}
