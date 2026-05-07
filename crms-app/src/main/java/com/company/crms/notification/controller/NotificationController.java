package com.company.crms.notification.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.notification.entity.Notification;
import com.company.crms.notification.entity.NotificationSetting;
import com.company.crms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "通知中心")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @Operation(summary = "通知列表")
    @SaCheckLogin
    @GetMapping
    public Result<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) Boolean unreadOnly) {
        return Result.ok(service.page(page, size, scene, unreadOnly));
    }

    @Operation(summary = "未读数")
    @SaCheckLogin
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", service.unreadCount()));
    }

    @Operation(summary = "顶栏弹层用：最近 N 条未读")
    @SaCheckLogin
    @GetMapping("/top")
    public Result<List<Notification>> top(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(service.topUnread(limit));
    }

    @Operation(summary = "标记已读")
    @SaCheckLogin
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        service.markRead(id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @SaCheckLogin
    @PutMapping("/read-all")
    public Result<Map<String, Integer>> markAllRead() {
        return Result.ok(Map.of("affected", service.markAllRead()));
    }

    @Operation(summary = "归档")
    @SaCheckLogin
    @PutMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id) {
        service.archive(id);
        return Result.ok();
    }

    @Operation(summary = "我的偏好")
    @SaCheckLogin
    @GetMapping("/settings")
    public Result<List<NotificationSetting>> settings() {
        return Result.ok(service.mySettings());
    }

    @Operation(summary = "保存偏好（按 scene → setting）")
    @SaCheckLogin
    @PostMapping("/settings")
    public Result<Void> saveSettings(@RequestBody Map<String, NotificationSetting> settings) {
        service.saveMySettings(settings);
        return Result.ok();
    }
}
