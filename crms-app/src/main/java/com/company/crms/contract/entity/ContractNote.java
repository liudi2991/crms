package com.company.crms.contract.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("contract_note")
public class ContractNote implements Serializable {
    @TableId
    private Long id;
    private Long contractId;
    private Long authorId;
    private String content;
    private LocalDateTime createdAt;
}
