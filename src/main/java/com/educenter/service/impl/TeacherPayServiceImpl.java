package com.educenter.service.impl;

import com.educenter.dto.TeacherEarningRow;
import com.educenter.dto.TeacherEarningsPage;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.TeacherClassPayRate;
import com.educenter.entity.User;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.TeacherClassPayRateRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.TeacherPayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Service
public class TeacherPayServiceImpl implements TeacherPayService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TeacherClassPayRateRepository rateRepository;
    private final ClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public TeacherPayServiceImpl(TeacherClassPayRateRepository rateRepository,
                                 ClassRepository classRepository,
                                 ClassSessionRepository sessionRepository,
                                 UserRepository userRepository) {
        this.rateRepository = rateRepository;
        this.classRepository = classRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassPayRate> getRateHistory(Long classId) {
        return rateRepository.findByClassEntityIdOrderByEffectiveFromDesc(classId);
    }

    @Override
    @Transactional
    public void recordRate(Long adminUserId, Long classId, BigDecimal rate, LocalDate effectiveFrom) {
        if (rate == null || rate.signum() < 0) {
            throw new IllegalArgumentException("Đơn giá buổi dạy phải là số không âm.");
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày hiệu lực.");
        }
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản admin."));

        if (rateRepository.findByClassEntityIdAndEffectiveFrom(classId, effectiveFrom).isPresent()) {
            throw new IllegalArgumentException("Đã có đơn giá được lưu cho ngày hiệu lực này. Hãy chọn ngày khác để giữ nguyên lịch sử.");
        }
        TeacherClassPayRate payRate = new TeacherClassPayRate();
        payRate.setClassEntity(classEntity);
        payRate.setRate(rate.setScale(2, java.math.RoundingMode.HALF_UP));
        payRate.setEffectiveFrom(effectiveFrom);
        payRate.setCreatedBy(admin);
        rateRepository.save(payRate);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherEarningsPage getEarnings(Long teacherUserId, YearMonth month) {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalTime now = LocalTime.now(APP_ZONE);
        List<ClassSession> eligibleSessions = sessionRepository.findEarningSessionsForTeacher(teacherUserId, today, now);
        List<Long> classIds = eligibleSessions.stream().map(s -> s.getClassEntity().getId()).distinct().toList();
        Map<Long, List<TeacherClassPayRate>> ratesByClass = new HashMap<>();
        if (!classIds.isEmpty()) {
            for (TeacherClassPayRate rate : rateRepository.findByClassEntityIdInOrderByEffectiveFromDesc(classIds)) {
                ratesByClass.computeIfAbsent(rate.getClassEntity().getId(), ignored -> new ArrayList<>()).add(rate);
            }
        }

        List<TeacherEarningRow> monthRows = new ArrayList<>();
        BigDecimal allTimeTotal = BigDecimal.ZERO;
        BigDecimal monthTotal = BigDecimal.ZERO;
        for (ClassSession session : eligibleSessions) {
            TeacherClassPayRate applicableRate = ratesByClass.getOrDefault(session.getClassEntity().getId(), List.of())
                    .stream()
                    .filter(rate -> !rate.getEffectiveFrom().isAfter(session.getSessionDate()))
                    .findFirst().orElse(null);
            BigDecimal amount = applicableRate == null ? null : applicableRate.getRate();
            if (amount != null) allTimeTotal = allTimeTotal.add(amount);
            if (YearMonth.from(session.getSessionDate()).equals(month)) {
                monthRows.add(new TeacherEarningRow(session,
                        applicableRate == null ? null : applicableRate.getRate(), amount));
                if (amount != null) monthTotal = monthTotal.add(amount);
            }
        }
        return new TeacherEarningsPage(List.copyOf(monthRows), allTimeTotal, monthTotal, month);
    }
}
