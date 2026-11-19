package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.TeacherClassPayRate;
import com.educenter.entity.User;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.TeacherClassPayRateRepository;
import com.educenter.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class TeacherPayServiceImplTest {

    private final TeacherClassPayRateRepository rateRepository = mock(TeacherClassPayRateRepository.class);
    private final ClassRepository classRepository = mock(ClassRepository.class);
    private final ClassSessionRepository sessionRepository = mock(ClassSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TeacherPayServiceImpl service = new TeacherPayServiceImpl(
            rateRepository, classRepository, sessionRepository, userRepository);

    @Test
    void earningsUseRateEffectiveOnEachSessionAndFilterBySelectedMonth() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(7L);
        ClassSession january = session(classEntity, LocalDate.of(2026, 1, 15));
        ClassSession february = session(classEntity, LocalDate.of(2026, 2, 15));
        when(sessionRepository.findEarningSessionsForTeacher(eq(12L), eq(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))),
                org.mockito.ArgumentMatchers.any(LocalTime.class))).thenReturn(List.of(february, january));
        when(rateRepository.findByClassEntityIdInOrderByEffectiveFromDesc(anyList())).thenReturn(List.of(
                rate(classEntity, LocalDate.of(2026, 2, 1), "150000"),
                rate(classEntity, LocalDate.of(2026, 1, 1), "100000")));

        var page = service.getEarnings(12L, YearMonth.of(2026, 2));

        assertEquals(1, page.sessions().size());
        assertEquals(0, page.sessions().getFirst().amount().compareTo(new BigDecimal("150000")));
        assertEquals(0, page.monthEarned().compareTo(new BigDecimal("150000")));
        assertEquals(0, page.totalEarned().compareTo(new BigDecimal("250000")));
    }

    @Test
    void duplicateEffectiveDateIsRejectedToPreserveHistoricalEarnings() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(7L);
        when(classRepository.findById(7L)).thenReturn(Optional.of(classEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(rateRepository.findByClassEntityIdAndEffectiveFrom(7L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(rate(classEntity, LocalDate.of(2026, 1, 1), "100000")));

        assertThrows(IllegalArgumentException.class,
                () -> service.recordRate(1L, 7L, new BigDecimal("125000"), LocalDate.of(2026, 1, 1)));
        verify(rateRepository, never()).save(org.mockito.ArgumentMatchers.any(TeacherClassPayRate.class));
    }

    private ClassSession session(ClassEntity classEntity, LocalDate date) {
        ClassSession session = new ClassSession();
        session.setClassEntity(classEntity);
        session.setSessionDate(date);
        session.setStartTime(LocalTime.of(17, 0));
        session.setEndTime(LocalTime.of(18, 0));
        session.setStatus("SCHEDULED");
        return session;
    }

    private TeacherClassPayRate rate(ClassEntity classEntity, LocalDate effectiveFrom, String amount) {
        TeacherClassPayRate rate = new TeacherClassPayRate();
        rate.setClassEntity(classEntity);
        rate.setEffectiveFrom(effectiveFrom);
        rate.setRate(new BigDecimal(amount));
        return rate;
    }
}
