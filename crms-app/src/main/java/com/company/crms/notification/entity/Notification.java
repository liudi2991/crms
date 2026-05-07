package com.company.crms.notification.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification implements Serializable {
    @TableId
    private Long id;
    private Long receiverId;
    private String scene;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    private String linkUrl;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Integer archived;
}
