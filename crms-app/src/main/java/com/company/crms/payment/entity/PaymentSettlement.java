package com.company.crms.payment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_settlement")
public class PaymentSettlement implements Serializable {
    @TableId
    private Long id;
    private Long paymentPlanId;
    private Long paymentRecordId;
    private BigDecimal settleAmount;
    private LocalDateTime settleAt;
    private String settleType;
}
