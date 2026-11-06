package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tuition_id", nullable = false)
    private Tuition tuition;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod = "VNPAY"; // VNPAY, CASH, TRANSFER

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tuition getTuition() { return tuition; }
    public void setTuition(Tuition tuition) { this.tuition = tuition; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVnpayResponseCode() { return vnpayResponseCode; }
    public void setVnpayResponseCode(String vnpayResponseCode) { this.vnpayResponseCode = vnpayResponseCode; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
