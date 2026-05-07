package com.company.crms.customer.service;

import com.company.crms.customer.dto.CreateContactDTO;
import com.company.crms.customer.dto.UpdateContactDTO;
import com.company.crms.customer.vo.CustomerContactVO;

import java.util.List;

public interface CustomerContactService {
    List<CustomerContactVO> listByCustomer(Long customerId);

    Long create(CreateContactDTO dto);

    void update(Long id, UpdateContactDTO dto);

    void remove(Long id);

    void setPrimary(Long id);
}
