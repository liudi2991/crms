package com.company.crms.notification.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("notification_setting")
public class NotificationSetting implements Serializable {
    @TableId
    private Long id;
    private Long userId;
    private String scene;
    private Integer enabled;
    private Integer advanceDays;
}
