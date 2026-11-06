package com.educenter.service;

import com.educenter.entity.Payment;
import com.educenter.entity.Tuition;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TuitionService {
    List<Tuition> getAllTuition();
    List<Tuition> getTuitionByParentUserId(Long userId);
    Tuition getTuitionById(Long id);
    Tuition createTuition(Long studentId, Long classId, BigDecimal amount, LocalDate dueDate, String description);
    void markAsPaid(Long tuitionId, String paymentMethod, String transactionCode, String note);
    String createVNPayPaymentUrl(Long tuitionId, HttpServletRequest request);
    Payment processVNPayCallback(Map<String, String> vnpParams);
    BigDecimal getTotalPaidRevenue();
    long getUnpaidCount();
    long getPaidCount();
}
