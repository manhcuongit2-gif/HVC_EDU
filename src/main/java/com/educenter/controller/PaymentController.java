package com.educenter.controller;

import com.educenter.entity.Payment;
import com.educenter.service.TuitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final TuitionService tuitionService;

    public PaymentController(TuitionService tuitionService) {
        this.tuitionService = tuitionService;
    }

    @GetMapping("/pay-tuition/{id}")
    public String createPayment(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            String paymentUrl = tuitionService.createVNPayPaymentUrl(id, request);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/parent/tuition";
        }
    }

    @GetMapping("/vnpay-return")
    public String handleVNPayReturn(HttpServletRequest request, Model model) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> vnpParams = new HashMap<>();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue().length > 0) {
                vnpParams.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        try {
            Payment payment = tuitionService.processVNPayCallback(vnpParams);
            model.addAttribute("payment", payment);
            model.addAttribute("tuition", payment.getTuition());

            if ("SUCCESS".equals(payment.getStatus())) {
                return "payment/success";
            } else {
                return "payment/failed";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi xử lý phản hồi từ VNPay: " + e.getMessage());
            return "payment/failed";
        }
    }
}
