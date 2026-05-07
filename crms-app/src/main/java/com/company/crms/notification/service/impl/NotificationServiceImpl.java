package com.company.crms.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.notification.entity.Notification;
import com.company.crms.notification.entity.NotificationSetting;
import com.company.crms.notification.mapper.NotificationMapper;
import com.company.crms.notification.mapper.NotificationSettingMapper;
import com.company.crms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationSettingMapper settingMapper;

    @Override
    public PageResult<Notification> page(int p, int size, String scene, Boolean unreadOnly) {
        Long uid = UserContextHolder.currentUserId();
        Page<Notification> page = Page.of(p, size);
        QueryWrapper<Notification> w = new QueryWrapper<Notification>()
                .eq("archived", 0);
        if (scene != null && !scene.isBlank()) w.eq("scene", scene);
        if (Boolean.TRUE.equals(unreadOnly)) w.eq("is_read", 0);
        w.orderByDesc("created_at");
        Page<Notification> result = (Page<Notification>) notificationMapper.selectPageForUser(page, uid, w);
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public long unreadCount() {
        return notificationMapper.countUnread(UserContextHolder.currentUserId());
    }

    @Override
    public List<Notification> topUnread(int limit) {
        Long uid = UserContextHolder.currentUserId();
        QueryWrapper<Notification> w = new QueryWrapper<Notification>()
                .eq("receiver_id", uid)
                .eq("is_read", 0)
                .eq("archived", 0)
                .orderByDesc("created_at")
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return notificationMapper.selectList(w);
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        int rows = notificationMapper.markRead(id, UserContextHolder.currentUserId());
        if (rows == 0) {
            throw new BizException(ErrorCode.NT_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public int markAllRead() {
        return notificationMapper.markAllRead(UserContextHolder.currentUserId());
    }

    @Override
    @Transactional
    public void archive(Long id) {
        int rows = notificationMapper.archive(id, UserContextHolder.currentUserId());
        if (rows == 0) {
            throw new BizException(ErrorCode.NT_NOT_FOUND);
        }
    }

    @Override
    public List<NotificationSetting> mySettings() {
        return settingMapper.listByUser(UserContextHolder.currentUserId());
    }

    @Override
    @Transactional
    public void saveMySettings(Map<String, NotificationSetting> settings) {
        Long uid = UserContextHolder.currentUserId();
        if (settings == null) return;
        for (Map.Entry<String, NotificationSetting> e : settings.entrySet()) {
            String scene = e.getKey();
            NotificationSetting incoming = e.getValue();
            NotificationSetting existing = settingMapper.findByUserScene(uid, scene);
            if (existing == null) {
                NotificationSetting s = new NotificationSetting();
                s.setId(SnowflakeIdGenerator.next());
                s.setUserId(uid);
                s.setScene(scene);
                s.setEnabled(incoming.getEnabled() != null ? incoming.getEnabled() : 1);
                s.setAdvanceDays(incoming.getAdvanceDays());
                settingMapper.insert(s);
            } else {
                existing.setEnabled(incoming.getEnabled() != null ? incoming.getEnabled() : 1);
                existing.setAdvanceDays(incoming.getAdvanceDays());
                settingMapper.updateById(existing);
            }
        }
    }
}
