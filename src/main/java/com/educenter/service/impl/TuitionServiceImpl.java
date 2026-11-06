package com.educenter.service.impl;

import com.educenter.config.VNPayConfig;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.Payment;
import com.educenter.entity.Student;
import com.educenter.entity.Tuition;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.PaymentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.TuitionRepository;
import com.educenter.service.TuitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TuitionServiceImpl implements TuitionService {

    private final TuitionRepository tuitionRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    public TuitionServiceImpl(TuitionRepository tuitionRepository,
                              PaymentRepository paymentRepository,
                              StudentRepository studentRepository,
                              ClassRepository classRepository) {
        this.tuitionRepository = tuitionRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
    }

    @Override
    public List<Tuition> getAllTuition() {
        return tuitionRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<Tuition> getTuitionByParentUserId(Long userId) {
        return tuitionRepository.findByStudentParentUserIdOrderByIdDesc(userId);
    }

    @Override
    public Tuition getTuitionById(Long id) {
        return tuitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản học phí với ID: " + id));
    }

    @Override
    @Transactional
    public Tuition createTuition(Long studentId, Long classId, BigDecimal amount, LocalDate dueDate, String description) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh"));
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));

        Tuition t = new Tuition();
        t.setStudent(student);
        t.setClassEntity(classEntity);
        t.setAmount(amount != null ? amount : classEntity.getTuitionFee());
        t.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(15));
        t.setStatus("UNPAID");
        t.setDescription(description != null ? description : "Học phí lớp " + classEntity.getName());

        return tuitionRepository.save(t);
    }

    @Override
    @Transactional
    public void markAsPaid(Long tuitionId, String paymentMethod, String transactionCode, String note) {
        Tuition t = getTuitionById(tuitionId);
        t.setStatus("PAID");
        tuitionRepository.save(t);

        Payment p = new Payment();
        p.setTuition(t);
        p.setAmount(t.getAmount());
        p.setPaymentDate(LocalDateTime.now());
        p.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        p.setTransactionCode(transactionCode != null ? transactionCode : "OFFLINE_" + System.currentTimeMillis());
        p.setStatus("SUCCESS");
        p.setVnpayResponseCode("00");
        p.setNote(note != null ? note : "Thu học phí trực tiếp");
        paymentRepository.save(p);
    }

    @Override
    public String createVNPayPaymentUrl(Long tuitionId, HttpServletRequest request) {
        Tuition t = getTuitionById(tuitionId);
        if ("PAID".equals(t.getStatus())) {
            throw new IllegalArgumentException("Khoản học phí này đã được thanh toán!");
        }

        long amount = t.getAmount().multiply(new BigDecimal("100")).longValue();

        String vnp_TxnRef = t.getId() + "_" + System.currentTimeMillis();
        String vnp_IpAddr = VNPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoc phi lop " + t.getClassEntity().getName() + " - HS " + t.getStudent().getFullName());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    @Override
    @Transactional
    public Payment processVNPayCallback(Map<String, String> vnpParams) {
        Map<String, String> fields = new HashMap<>(vnpParams);
        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String signValue = VNPayConfig.hashAllFields(fields);
        boolean isValidSignature = signValue.equalsIgnoreCase(vnp_SecureHash);

        String vnp_TxnRef = fields.get("vnp_TxnRef");
        String vnp_ResponseCode = fields.get("vnp_ResponseCode");
        String vnp_TransactionNo = fields.get("vnp_TransactionNo");
        String vnp_Amount = fields.get("vnp_Amount");

        Long tuitionId = null;
        if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
            tuitionId = Long.parseLong(vnp_TxnRef.split("_")[0]);
        }

        if (tuitionId == null) {
            throw new IllegalArgumentException("Mã tham chiếu thanh toán không hợp lệ!");
        }

        Tuition tuition = getTuitionById(tuitionId);
        Payment payment = new Payment();
        payment.setTuition(tuition);
        payment.setAmount(tuition.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod("VNPAY");
        payment.setTransactionCode(vnp_TransactionNo != null ? vnp_TransactionNo : vnp_TxnRef);
        payment.setVnpayResponseCode(vnp_ResponseCode);

        if (isValidSignature && "00".equals(vnp_ResponseCode)) {
            tuition.setStatus("PAID");
            tuitionRepository.save(tuition);

            payment.setStatus("SUCCESS");
            payment.setNote("Thanh toán thành công qua VNPay");
        } else {
            payment.setStatus("FAILED");
            payment.setNote("Giao dịch không thành công hoặc lỗi chữ ký (Mã phản hồi: " + vnp_ResponseCode + ")");
        }

        return paymentRepository.save(payment);
    }

    @Override
    public BigDecimal getTotalPaidRevenue() {
        BigDecimal sum = tuitionRepository.calculateTotalPaidRevenue();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public long getUnpaidCount() {
        return tuitionRepository.countByStatus("UNPAID");
    }

    @Override
    public long getPaidCount() {
        return tuitionRepository.countByStatus("PAID");
    }
}
