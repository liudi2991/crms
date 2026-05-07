package com.company.crms.customer.vo;

import com.company.crms.system.entity.ChangeLog;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerAggregateVO implements Serializable {
    private CustomerVO customer;
    private List<CustomerContactVO> contacts;
    private List<RecentContract> recentContracts;
    private List<ChangeLog> recentChanges;
    private long totalContracts;
    private BigDecimal totalContractAmount;

    @Data
    public static class RecentContract {
        private Long id;
        private String code;
        private String name;
        private BigDecimal amount;
        private String status;
        private LocalDate signedAt;
    }
}
