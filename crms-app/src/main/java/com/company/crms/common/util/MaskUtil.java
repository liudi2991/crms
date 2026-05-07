package com.company.crms.common.util;

import com.company.crms.common.annotation.SensitiveField;

/**
 * 敏感字段脱敏展示工具。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    public static String mask(String value, SensitiveField.Mask type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case PHONE -> maskPhone(value);
            case EMAIL -> maskEmail(value);
            case NAME -> maskName(value);
            case ID_CARD -> maskIdCard(value);
            case NONE -> value;
        };
    }

    private static String maskPhone(String phone) {
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "****" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "****" + email.substring(at);
    }

    private static String maskName(String name) {
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    private static String maskIdCard(String id) {
        if (id.length() < 9) {
            return "****";
        }
        return id.substring(0, 4) + "*".repeat(id.length() - 8) + id.substring(id.length() - 4);
    }
}
