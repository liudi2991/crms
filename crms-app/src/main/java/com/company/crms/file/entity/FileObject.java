package com.company.crms.file.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("file_object")
public class FileObject implements Serializable {
    @TableId
    private Long id;
    private String objectKey;
    private String bucket;
    private String fileName;
    private String contentType;
    private Long size;
    private String bizType;
    private Long bizId;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
