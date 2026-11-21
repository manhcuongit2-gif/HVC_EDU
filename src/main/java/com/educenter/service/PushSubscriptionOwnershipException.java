package com.educenter.service;

/** Raised when a push endpoint or account-link registration conflicts with stored ownership data. */
public class PushSubscriptionOwnershipException extends RuntimeException {
    public PushSubscriptionOwnershipException() {
        super("Đăng ký thiết bị không khớp dữ liệu đã lưu.");
    }
}
