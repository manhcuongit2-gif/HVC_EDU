package com.educenter.config;

import com.educenter.entity.*;
import com.educenter.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ScheduleRepository scheduleRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final TuitionRepository tuitionRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           SubjectRepository subjectRepository,
                           RoomRepository roomRepository,
                           TeacherRepository teacherRepository,
                           ParentRepository parentRepository,
                           StudentRepository studentRepository,
                           ClassRepository classRepository,
                           ClassStudentRepository classStudentRepository,
                           ScheduleRepository scheduleRepository,
                           RegistrationRepository registrationRepository,
                           AttendanceRepository attendanceRepository,
                           GradeRepository gradeRepository,
                           TuitionRepository tuitionRepository,
                           PaymentRepository paymentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.subjectRepository = subjectRepository;
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.scheduleRepository = scheduleRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.gradeRepository = gradeRepository;
        this.tuitionRepository = tuitionRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Roles
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN", "Quản trị viên")));
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_TEACHER", "Người dạy")));
        Role parentRole = roleRepository.findByName("ROLE_PARENT").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_PARENT", "Phụ huynh")));
        roleRepository.findByName("ROLE_STUDENT").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_STUDENT", "Học sinh")));

        // 2. Users: admin, gv_toan, gv_ly, gv_hoa, ph_dung, ph_lan
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@educenter.edu.vn");
            u.setFullName("Quản Trị Viên Luyện Thi Hùng Cường");
            u.setPhone("0988000001");
            u.setRole(adminRole);
            u.setActive(true);
            return u;
        });
        if ("Quản Trị Viên EduCenter".equals(admin.getFullName())) {
            admin.setFullName("Quản Trị Viên Luyện Thi Hùng Cường");
        }
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setActive(true);
        userRepository.save(admin);

        User gvToanUser = userRepository.findByUsername("gv_toan").orElseGet(() -> {
            User u = new User();
            u.setUsername("gv_toan");
            u.setEmail("tranvantoan@educenter.edu.vn");
            u.setFullName("ThS. Trần Văn Toán");
            u.setPhone("0988000002");
            u.setRole(teacherRole);
            u.setActive(true);
            return u;
        });
        gvToanUser.setPassword(passwordEncoder.encode("123456"));
        gvToanUser.setActive(true);
        userRepository.save(gvToanUser);

        User gvLyUser = userRepository.findByUsername("gv_ly").orElseGet(() -> {
            User u = new User();
            u.setUsername("gv_ly");
            u.setEmail("lethily@educenter.edu.vn");
            u.setFullName("Cô Lê Thị Lý");
            u.setPhone("0988000003");
            u.setRole(teacherRole);
            u.setActive(true);
            return u;
        });
        gvLyUser.setPassword(passwordEncoder.encode("123456"));
        gvLyUser.setActive(true);
        userRepository.save(gvLyUser);

        User gvHoaUser = userRepository.findByUsername("gv_hoa").orElseGet(() -> {
            User u = new User();
            u.setUsername("gv_hoa");
            u.setEmail("phamvanhoa@educenter.edu.vn");
            u.setFullName("ThS. Phạm Văn Hóa");
            u.setPhone("0988000004");
            u.setRole(teacherRole);
            u.setActive(true);
            return u;
        });
        gvHoaUser.setPassword(passwordEncoder.encode("123456"));
        gvHoaUser.setActive(true);
        userRepository.save(gvHoaUser);

        User phDungUser = userRepository.findByUsername("ph_dung").orElseGet(() -> {
            User u = new User();
            u.setUsername("ph_dung");
            u.setEmail("hoangtiendung@gmail.com");
            u.setFullName("Hoàng Tiến Dũng");
            u.setPhone("0912345678");
            u.setRole(parentRole);
            u.setActive(true);
            return u;
        });
        phDungUser.setPassword(passwordEncoder.encode("123456"));
        phDungUser.setActive(true);
        userRepository.save(phDungUser);

        User phLanUser = userRepository.findByUsername("ph_lan").orElseGet(() -> {
            User u = new User();
            u.setUsername("ph_lan");
            u.setEmail("nguyenthilan@gmail.com");
            u.setFullName("Nguyễn Thị Lan");
            u.setPhone("0912345679");
            u.setRole(parentRole);
            u.setActive(true);
            return u;
        });
        phLanUser.setPassword(passwordEncoder.encode("123456"));
        phLanUser.setActive(true);
        userRepository.save(phLanUser);

        // 3. Teachers profiles
        Teacher teacherToan = teacherRepository.findByUserId(gvToanUser.getId()).orElseGet(() -> {
            Teacher t = new Teacher();
            t.setUser(gvToanUser);
            t.setSpecialization("Toán THPT & Luyện thi ĐH");
            t.setDegree("Thạc sĩ Toán học - ĐH Sư Phạm Hà Nội");
            t.setExperience("10 năm kinh nghiệm luyện thi vào 10 và THPT Quốc gia");
            return teacherRepository.save(t);
        });

        Teacher teacherLy = teacherRepository.findByUserId(gvLyUser.getId()).orElseGet(() -> {
            Teacher t = new Teacher();
            t.setUser(gvLyUser);
            t.setSpecialization("Vật Lý THPT");
            t.setDegree("Cử nhân Sư phạm Vật Lý - ĐH Sư Phạm Hà Nội");
            t.setExperience("6 năm kinh nghiệm giảng dạy Vật Lý");
            return teacherRepository.save(t);
        });

        Teacher teacherHoa = teacherRepository.findByUserId(gvHoaUser.getId()).orElseGet(() -> {
            Teacher t = new Teacher();
            t.setUser(gvHoaUser);
            t.setSpecialization("Hóa Học THPT & Chuyên sâu");
            t.setDegree("Thạc sĩ Hóa học - ĐH Khoa học Tự nhiên");
            t.setExperience("8 năm kinh nghiệm bồi dưỡng học sinh giỏi Hóa");
            return teacherRepository.save(t);
        });

        // 4. Parents profiles
        Parent parentDung = parentRepository.findByUserId(phDungUser.getId()).orElseGet(() -> {
            Parent p = new Parent();
            p.setUser(phDungUser);
            p.setOccupation("Kỹ sư CNTT");
            p.setNote("Phụ huynh em Hoàng Nam và Hoàng Mai");
            return parentRepository.save(p);
        });

        Parent parentLan = parentRepository.findByUserId(phLanUser.getId()).orElseGet(() -> {
            Parent p = new Parent();
            p.setUser(phLanUser);
            p.setOccupation("Kế toán trưởng");
            p.setNote("Phụ huynh em Lê Quốc Bảo");
            return parentRepository.save(p);
        });

        // 5. Subjects
        Subject subToan = subjectRepository.findByName("Toán học").orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Toán học");
            s.setDescription("Môn Toán nền tảng và nâng cao từ lớp 6 đến lớp 12");
            s.setActive(true);
            return subjectRepository.save(s);
        });

        Subject subLy = subjectRepository.findByName("Vật lý").orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Vật lý");
            s.setDescription("Môn Vật lý lý thuyết và thực hành, luyện thi chuyển cấp");
            s.setActive(true);
            return subjectRepository.save(s);
        });

        Subject subHoa = subjectRepository.findByName("Hóa học").orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Hóa học");
            s.setDescription("Môn Hóa học cơ bản và bồi dưỡng chuyên đề");
            s.setActive(true);
            return subjectRepository.save(s);
        });

        // 6. Rooms
        Room room101 = roomRepository.findByName("Phòng P101").orElseGet(() -> {
            Room r = new Room();
            r.setName("Phòng P101");
            r.setCapacity(30);
            r.setLocation("Tầng 1 - Dãy A");
            r.setDescription("Phòng học máy chiếu, điều hòa 2 chiều");
            r.setActive(true);
            return roomRepository.save(r);
        });

        Room room102 = roomRepository.findByName("Phòng P102").orElseGet(() -> {
            Room r = new Room();
            r.setName("Phòng P102");
            r.setCapacity(25);
            r.setLocation("Tầng 1 - Dãy A");
            r.setDescription("Phòng tiêu chuẩn, điều hòa, bảng từ");
            r.setActive(true);
            return roomRepository.save(r);
        });

        Room room201 = roomRepository.findByName("Phòng P201").orElseGet(() -> {
            Room r = new Room();
            r.setName("Phòng P201");
            r.setCapacity(35);
            r.setLocation("Tầng 2 - Dãy B");
            r.setDescription("Phòng lớn phục vụ lớp nâng cao và thi thử");
            r.setActive(true);
            return roomRepository.save(r);
        });

        // 7. Students
        Student stNam = studentRepository.findByFullNameAndParentId("Hoàng Nam", parentDung.getId()).orElseGet(() -> {
            Student s = new Student();
            s.setFullName("Hoàng Nam");
            s.setDateOfBirth(LocalDate.of(2009, 3, 15));
            s.setGender("Nam");
            s.setPhone("0961000001");
            s.setSchool("THPT Chuyên Sư Phạm");
            s.setGradeLevel("Lớp 10");
            s.setParent(parentDung);
            s.setActive(true);
            return studentRepository.save(s);
        });

        Student stMai = studentRepository.findByFullNameAndParentId("Hoàng Mai", parentDung.getId()).orElseGet(() -> {
            Student s = new Student();
            s.setFullName("Hoàng Mai");
            s.setDateOfBirth(LocalDate.of(2011, 8, 20));
            s.setGender("Nữ");
            s.setPhone("0961000002");
            s.setSchool("THCS Cầu Giấy");
            s.setGradeLevel("Lớp 8");
            s.setParent(parentDung);
            s.setActive(true);
            return studentRepository.save(s);
        });

        Student stBao = studentRepository.findByFullNameAndParentId("Lê Quốc Bảo", parentLan.getId()).orElseGet(() -> {
            Student s = new Student();
            s.setFullName("Lê Quốc Bảo");
            s.setDateOfBirth(LocalDate.of(2009, 5, 10));
            s.setGender("Nam");
            s.setPhone("0961000003");
            s.setSchool("THPT Yên Hòa");
            s.setGradeLevel("Lớp 10");
            s.setParent(parentLan);
            s.setActive(true);
            return studentRepository.save(s);
        });

        // 8. Classes
        ClassEntity classToan = null;
        if (!classRepository.existsByName("Toán 10 - Nâng Cao (T10-NC1)")) {
            ClassEntity c = new ClassEntity();
            c.setName("Toán 10 - Nâng Cao (T10-NC1)");
            c.setSubject(subToan);
            c.setTeacher(teacherToan);
            c.setMaxStudents(25);
            c.setTuitionFee(new BigDecimal("1200000.00"));
            c.setStatus("ACTIVE");
            c.setStartDate(LocalDate.of(2026, 9, 1));
            c.setEndDate(LocalDate.of(2027, 5, 30));
            c.setDescription("Lớp bồi dưỡng Toán nâng cao khối 10 ôn thi ĐH");
            classToan = classRepository.save(c);
        } else {
            classToan = classRepository.findAll().stream().filter(c -> c.getName().contains("Toán 10")).findFirst().orElse(null);
        }

        ClassEntity classLy = null;
        if (!classRepository.existsByName("Vật Lý 10 - Cơ Bản & Ôn Luyện (L10-CB1)")) {
            ClassEntity c = new ClassEntity();
            c.setName("Vật Lý 10 - Cơ Bản & Ôn Luyện (L10-CB1)");
            c.setSubject(subLy);
            c.setTeacher(teacherLy);
            c.setMaxStudents(25);
            c.setTuitionFee(new BigDecimal("1000000.00"));
            c.setStatus("ACTIVE");
            c.setStartDate(LocalDate.of(2026, 9, 1));
            c.setEndDate(LocalDate.of(2027, 5, 30));
            c.setDescription("Lớp củng cố kiến thức Vật lý 10 bám sát SGK mới");
            classLy = classRepository.save(c);
        } else {
            classLy = classRepository.findAll().stream().filter(c -> c.getName().contains("Vật Lý 10")).findFirst().orElse(null);
        }

        ClassEntity classHoa = null;
        if (!classRepository.existsByName("Hóa Học 10 - Nâng Cao (H10-NC1)")) {
            ClassEntity c = new ClassEntity();
            c.setName("Hóa Học 10 - Nâng Cao (H10-NC1)");
            c.setSubject(subHoa);
            c.setTeacher(teacherHoa);
            c.setMaxStudents(20);
            c.setTuitionFee(new BigDecimal("1100000.00"));
            c.setStatus("ACTIVE");
            c.setStartDate(LocalDate.of(2026, 9, 1));
            c.setEndDate(LocalDate.of(2027, 5, 30));
            c.setDescription("Lớp Hóa học 10 chuyên đề bài tập và thực hành");
            classHoa = classRepository.save(c);
        } else {
            classHoa = classRepository.findAll().stream().filter(c -> c.getName().contains("Hóa Học 10")).findFirst().orElse(null);
        }

        // 9. Class enrollments (ClassStudent)
        if (classToan != null) {
            if (!classStudentRepository.existsByClassEntityIdAndStudentId(classToan.getId(), stNam.getId())) {
                classStudentRepository.save(new ClassStudent(classToan, stNam, LocalDate.of(2026, 9, 1), "ACTIVE"));
            }
            if (!classStudentRepository.existsByClassEntityIdAndStudentId(classToan.getId(), stBao.getId())) {
                classStudentRepository.save(new ClassStudent(classToan, stBao, LocalDate.of(2026, 9, 1), "ACTIVE"));
            }
        }
        if (classLy != null) {
            if (!classStudentRepository.existsByClassEntityIdAndStudentId(classLy.getId(), stNam.getId())) {
                classStudentRepository.save(new ClassStudent(classLy, stNam, LocalDate.of(2026, 9, 1), "ACTIVE"));
            }
        }

        // 10. Schedules (TKB)
        if (classToan != null && scheduleRepository.findByClassEntityId(classToan.getId()).isEmpty()) {
            Schedule s1 = new Schedule();
            s1.setClassEntity(classToan);
            s1.setRoom(room101);
            s1.setDayOfWeek(2); // Thứ 2
            s1.setStartTime(LocalTime.of(17, 30));
            s1.setEndTime(LocalTime.of(19, 30));
            s1.setActive(true);
            scheduleRepository.save(s1);

            Schedule s2 = new Schedule();
            s2.setClassEntity(classToan);
            s2.setRoom(room101);
            s2.setDayOfWeek(5); // Thứ 5
            s2.setStartTime(LocalTime.of(17, 30));
            s2.setEndTime(LocalTime.of(19, 30));
            s2.setActive(true);
            scheduleRepository.save(s2);
        }

        if (classLy != null && scheduleRepository.findByClassEntityId(classLy.getId()).isEmpty()) {
            Schedule s3 = new Schedule();
            s3.setClassEntity(classLy);
            s3.setRoom(room102);
            s3.setDayOfWeek(3); // Thứ 3
            s3.setStartTime(LocalTime.of(18, 0));
            s3.setEndTime(LocalTime.of(20, 0));
            s3.setActive(true);
            scheduleRepository.save(s3);

            Schedule s4 = new Schedule();
            s4.setClassEntity(classLy);
            s4.setRoom(room102);
            s4.setDayOfWeek(6); // Thứ 6
            s4.setStartTime(LocalTime.of(18, 0));
            s4.setEndTime(LocalTime.of(20, 0));
            s4.setActive(true);
            scheduleRepository.save(s4);
        }

        if (classHoa != null && scheduleRepository.findByClassEntityId(classHoa.getId()).isEmpty()) {
            Schedule s5 = new Schedule();
            s5.setClassEntity(classHoa);
            s5.setRoom(room201);
            s5.setDayOfWeek(4); // Thứ 4
            s5.setStartTime(LocalTime.of(18, 0));
            s5.setEndTime(LocalTime.of(20, 0));
            s5.setActive(true);
            scheduleRepository.save(s5);
        }

        // 11. Registrations (Đơn đăng ký học)
        if (registrationRepository.count() == 0 && classToan != null) {
            Registration r1 = new Registration();
            r1.setParent(parentDung);
            r1.setStudent(stNam);
            r1.setClassEntity(classToan);
            r1.setRegistrationDate(LocalDateTime.now().minusDays(10));
            r1.setStatus("APPROVED");
            r1.setNote("Đăng ký cho cháu Nam học Toán nâng cao");
            r1.setAdminNote("Đã duyệt và xếp lớp thành công");
            r1.setProcessedBy(admin);
            r1.setProcessedDate(LocalDateTime.now().minusDays(9));
            registrationRepository.save(r1);

            Registration r2 = new Registration();
            r2.setParent(parentDung);
            r2.setStudent(stMai);
            r2.setClassEntity(classToan);
            r2.setRegistrationDate(LocalDateTime.now().minusHours(3));
            r2.setStatus("PENDING");
            r2.setNote("Xin đăng ký cho cháu Mai học thử 1 buổi Toán");
            registrationRepository.save(r2);
        }

        // 12. Attendance (Điểm danh mẫu)
        if (attendanceRepository.count() == 0 && classToan != null) {
            Attendance a1 = new Attendance();
            a1.setClassEntity(classToan);
            a1.setStudent(stNam);
            a1.setAttendanceDate(LocalDate.now().minusDays(3));
            a1.setStatus("PRESENT");
            a1.setNote("Đúng giờ, hăng hái phát biểu");
            a1.setCreatedBy(gvToanUser);
            attendanceRepository.save(a1);

            Attendance a2 = new Attendance();
            a2.setClassEntity(classToan);
            a2.setStudent(stBao);
            a2.setAttendanceDate(LocalDate.now().minusDays(3));
            a2.setStatus("EXCUSED");
            a2.setNote("Có phép");
            a2.setCreatedBy(gvToanUser);
            attendanceRepository.save(a2);
        }

        // 13. Grades (Bảng điểm mẫu)
        if (gradeRepository.count() == 0 && classToan != null) {
            Grade g1 = new Grade();
            g1.setClassEntity(classToan);
            g1.setStudent(stNam);
            g1.setTestScore(new BigDecimal("9.00"));
            g1.setMidtermScore(new BigDecimal("8.50"));
            g1.setFinalScore(new BigDecimal("9.00"));
            g1.recalculateAverageScore();
            g1.setNote("Học lực Xuất sắc, tư duy logic rất tốt");
            gradeRepository.save(g1);

            Grade g2 = new Grade();
            g2.setClassEntity(classToan);
            g2.setStudent(stBao);
            g2.setTestScore(new BigDecimal("7.50"));
            g2.setMidtermScore(new BigDecimal("7.00"));
            g2.setFinalScore(new BigDecimal("8.00"));
            g2.recalculateAverageScore();
            g2.setNote("Khá, cần rèn luyện thêm bài tập hình học");
            gradeRepository.save(g2);
        }

        // 14. Tuition (Khoản học phí) & Payments (VNPay)
        if (tuitionRepository.count() == 0 && classToan != null && classLy != null) {
            Tuition t1 = new Tuition();
            t1.setStudent(stNam);
            t1.setClassEntity(classToan);
            t1.setAmount(new BigDecimal("1200000.00"));
            t1.setDueDate(LocalDate.now().plusDays(10));
            t1.setStatus("PAID");
            t1.setDescription("Học phí Tháng 10 - Lớp Toán 10 NC");
            tuitionRepository.save(t1);

            Payment p1 = new Payment();
            p1.setTuition(t1);
            p1.setAmount(t1.getAmount());
            p1.setPaymentDate(LocalDateTime.now().minusDays(5));
            p1.setPaymentMethod("VNPAY");
            p1.setTransactionCode("VNP14892011");
            p1.setStatus("SUCCESS");
            p1.setVnpayResponseCode("00");
            p1.setNote("Thanh toán học phí Toán 10 qua Cổng VNPay thành công");
            paymentRepository.save(p1);

            Tuition t2 = new Tuition();
            t2.setStudent(stNam);
            t2.setClassEntity(classLy);
            t2.setAmount(new BigDecimal("1000000.00"));
            t2.setDueDate(LocalDate.now().plusDays(12));
            t2.setStatus("UNPAID");
            t2.setDescription("Học phí Tháng 10 - Lớp Lý 10 CB");
            tuitionRepository.save(t2);

            Tuition t3 = new Tuition();
            t3.setStudent(stBao);
            t3.setClassEntity(classToan);
            t3.setAmount(new BigDecimal("1200000.00"));
            t3.setDueDate(LocalDate.now().plusDays(10));
            t3.setStatus("UNPAID");
            t3.setDescription("Học phí Tháng 10 - Lớp Toán 10 NC");
            tuitionRepository.save(t3);
        }

        System.out.println(">>> [DataInitializer] Hoàn tất đồng bộ toàn diện Database: Users, Teachers, Parents, Students, Classes, ClassStudents, Schedules, Registrations, Attendance, Grades, Tuition & Payments!");
    }
}
