package com.educenter.repository;

import com.educenter.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByPaymentDateDesc();
    List<Payment> findByTuitionId(Long tuitionId);
    List<Payment> findByStatus(String status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'SUCCESS' and p.tuition.student.id = :studentId " +
            "and p.tuition.classEntity.id = :classId and p.tuition.billingPeriod = :period")
    BigDecimal sumSuccessfulForBillingPeriod(@Param("studentId") Long studentId, @Param("classId") Long classId,
                                             @Param("period") java.time.LocalDate period);
}
