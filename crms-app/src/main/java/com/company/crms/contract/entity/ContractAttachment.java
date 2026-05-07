package com.company.crms.contract.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("contract_attachment")
public class ContractAttachment implements Serializable {
    @TableId
    private Long id;
    private Long contractId;
    private Long fileObjectId;
    private String fileName;
    private Long fileSize;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
