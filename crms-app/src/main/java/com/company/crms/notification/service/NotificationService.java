package com.company.crms.notification.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.notification.entity.Notification;
import com.company.crms.notification.entity.NotificationSetting;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    PageResult<Notification> page(int page, int size, String scene, Boolean unreadOnly);

    long unreadCount();

    /** 用户在登录后的 5 条最新未读，用于顶栏铃铛弹层。 */
    List<Notification> topUnread(int limit);

    void markRead(Long id);

    int markAllRead();

    void archive(Long id);

    List<NotificationSetting> mySettings();

    /** 批量更新当前用户的偏好。Map<scene, NotificationSetting>。 */
    void saveMySettings(Map<String, NotificationSetting> settings);
}
