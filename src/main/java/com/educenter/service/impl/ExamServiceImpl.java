package com.educenter.service.impl;

import com.educenter.dto.*;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.ExamService;
import com.educenter.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> QUESTION_TYPES = Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_IN", "ESSAY");
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String WAITING_REVIEW = "WAITING_REVIEW";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String GRADED = "GRADED";

    private final ExamQuestionRepository questions;
    private final ExamBlueprintRepository blueprints;
    private final ExamMatrixCellRepository cells;
    private final ExamBlueprintItemRepository items;
    private final ExamAssignmentRepository assignments;
    private final ExamAttemptRepository attempts;
    private final ExamAttemptAnswerRepository answers;
    private final TeacherRepository teachers;
    private final SubjectRepository subjects;
    private final ClassRepository classes;
    private final ClassStudentRepository enrollments;
    private final StudentRepository students;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ObjectMapper mapper;

    public ExamServiceImpl(ExamQuestionRepository questions, ExamBlueprintRepository blueprints,
                           ExamMatrixCellRepository cells, ExamBlueprintItemRepository items,
                           ExamAssignmentRepository assignments, ExamAttemptRepository attempts,
                           ExamAttemptAnswerRepository answers, TeacherRepository teachers,
                           SubjectRepository subjects, ClassRepository classes, ClassStudentRepository enrollments,
                           StudentRepository students, UserRepository users, NotificationService notifications,
                           ObjectMapper mapper) {
        this.questions = questions; this.blueprints = blueprints; this.cells = cells; this.items = items;
        this.assignments = assignments; this.attempts = attempts; this.answers = answers; this.teachers = teachers;
        this.subjects = subjects; this.classes = classes; this.enrollments = enrollments; this.students = students;
        this.users = users; this.notifications = notifications; this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<ExamQuestion> questionsForTeacher(Long teacherUserId) {
        return questions.findByOwnerTeacherIdOrderByUpdatedAtDesc(requireTeacher(teacherUserId).getId());
    }

    @Override @Transactional(readOnly = true)
    public List<ExamQuestion> searchQuestions(Long teacherUserId, Long subjectId, String gradeLevel, String topic,
                                               String questionType, String difficulty) {
        return questionsForTeacher(teacherUserId).stream().filter(ExamQuestion::isActive)
                .filter(q -> subjectId == null || Objects.equals(q.getSubject().getId(), subjectId))
                .filter(q -> gradeLevel == null || gradeLevel.isBlank() || q.getGradeLevel().equalsIgnoreCase(gradeLevel.trim()))
                .filter(q -> topic == null || topic.isBlank() || Optional.ofNullable(q.getTopic()).orElse("").toLowerCase(Locale.ROOT).contains(topic.trim().toLowerCase(Locale.ROOT)))
                .filter(q -> questionType == null || questionType.isBlank() || q.getQuestionType().equalsIgnoreCase(questionType.trim()))
                .filter(q -> difficulty == null || difficulty.isBlank() || q.getDifficulty().equalsIgnoreCase(difficulty.trim())).toList();
    }

    @Override @Transactional(readOnly = true)
    public ExamQuestionForm questionFormForTeacher(Long teacherUserId, Long questionId) {
        ExamQuestion q = questions.findByIdAndOwnerTeacherId(questionId, requireTeacher(teacherUserId).getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi của bạn."));
        ExamQuestionForm form = new ExamQuestionForm(); form.setId(q.getId()); form.setSubjectId(q.getSubject().getId());
        form.setGradeLevel(q.getGradeLevel()); form.setTopic(q.getTopic()); form.setQuestionType(q.getQuestionType());
        form.setDifficulty(q.getDifficulty()); form.setPrompt(q.getPrompt()); form.setExplanation(q.getExplanation());
        form.setDefaultPoints(q.getDefaultPoints());
        List<Choice> choices = readJson(q.getOptionsJson() == null ? "[]" : q.getOptionsJson(), new TypeReference<>() { });
        Map<String, String> byKey = choices.stream().collect(Collectors.toMap(Choice::key, Choice::text));
        form.setOptionA(byKey.get("A")); form.setOptionB(byKey.get("B")); form.setOptionC(byKey.get("C")); form.setOptionD(byKey.get("D"));
        Key key = readJson(q.getAnswerKeyJson(), new TypeReference<>() { }); form.setCorrectKey(key.correctOptionKey());
        form.setAcceptedAnswers(String.join(System.lineSeparator(), Optional.ofNullable(key.acceptedAnswers()).orElse(List.of())));
        return form;
    }

    @Override @Transactional
    public ExamQuestion saveQuestion(Long teacherUserId, ExamQuestionForm form) {
        Teacher teacher = requireTeacher(teacherUserId);
        if (form == null) throw new IllegalArgumentException("Thiếu dữ liệu câu hỏi.");
        String type = upper(form.getQuestionType());
        String difficulty = upper(form.getDifficulty());
        if (!QUESTION_TYPES.contains(type)) throw new IllegalArgumentException("Dạng câu hỏi không hợp lệ.");
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException("Mức độ câu hỏi không hợp lệ.");
        if (form.getSubjectId() == null) throw new IllegalArgumentException("Vui lòng chọn môn học.");
        Subject subject = subjects.findById(form.getSubjectId()).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học đang hoạt động."));
        String grade = trimRequired(form.getGradeLevel(), 20, "Khối lớp");
        String prompt = trimRequired(form.getPrompt(), 10_000, "Nội dung câu hỏi");
        BigDecimal points = form.getDefaultPoints() == null ? BigDecimal.ONE : form.getDefaultPoints();
        if (points.compareTo(BigDecimal.ZERO) <= 0 || points.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Điểm mặc định phải lớn hơn 0 và không quá 100.");

        List<Choice> choices = new ArrayList<>();
        Key key;
        if ("MULTIPLE_CHOICE".equals(type)) {
            addChoice(choices, "A", form.getOptionA()); addChoice(choices, "B", form.getOptionB());
            addChoice(choices, "C", form.getOptionC()); addChoice(choices, "D", form.getOptionD());
            if (choices.size() < 2 || choices.stream().noneMatch(c -> c.key().equalsIgnoreCase(form.getCorrectKey())))
                throw new IllegalArgumentException("Trắc nghiệm cần ít nhất hai phương án và một đáp án đúng.");
            key = new Key(form.getCorrectKey().trim().toUpperCase(Locale.ROOT), List.of());
        } else if ("TRUE_FALSE".equals(type)) {
            choices.add(new Choice("A", "Đúng")); choices.add(new Choice("B", "Sai"));
            String correct = upper(form.getCorrectKey());
            if (!Set.of("A", "B").contains(correct)) throw new IllegalArgumentException("Vui lòng chọn đáp án đúng/sai.");
            key = new Key(correct, List.of());
        } else if ("FILL_IN".equals(type)) {
            List<String> accepted = Arrays.stream(Optional.ofNullable(form.getAcceptedAnswers()).orElse("").split("\\R"))
                    .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
            if (accepted.isEmpty() || accepted.stream().anyMatch(s -> s.length() > 300))
                throw new IllegalArgumentException("Nhập ít nhất một đáp án chấp nhận, mỗi đáp án tối đa 300 ký tự.");
            key = new Key(null, accepted);
        } else key = new Key(null, List.of());

        ExamQuestion question = form.getId() == null ? new ExamQuestion()
                : questions.findByIdAndOwnerTeacherId(form.getId(), teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi của bạn."));
        question.setOwnerTeacher(teacher); question.setSubject(subject); question.setGradeLevel(grade);
        question.setTopic(trimNullable(form.getTopic(), 150)); question.setQuestionType(type); question.setDifficulty(difficulty);
        question.setPrompt(prompt); question.setOptionsJson(writeJson(choices)); question.setAnswerKeyJson(writeJson(key));
        question.setExplanation(trimNullable(form.getExplanation(), 10_000)); question.setDefaultPoints(points.setScale(2, RoundingMode.HALF_UP));
        question.setActive(true);
        return questions.save(question);
    }

    @Override @Transactional
    public void archiveQuestion(Long teacherUserId, Long questionId) {
        ExamQuestion q = questions.findByIdAndOwnerTeacherId(questionId, requireTeacher(teacherUserId).getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi của bạn."));
        q.setActive(false);
    }

    @Override @Transactional(readOnly = true)
    public List<Subject> subjects() { return subjects.findByActiveTrue(); }

    @Override @Transactional(readOnly = true)
    public List<ExamBlueprint> blueprintsForTeacher(Long teacherUserId) {
        return blueprints.findByOwnerTeacherIdOrderByCreatedAtDesc(requireTeacher(teacherUserId).getId());
    }

    @Override @Transactional
    public ExamBlueprint createBlueprint(Long teacherUserId, ExamBlueprintForm form) {
        Teacher teacher = requireTeacher(teacherUserId);
        if (form == null || form.getSubjectId() == null) throw new IllegalArgumentException("Thiếu thông tin ma trận đề.");
        String title = trimRequired(form.getTitle(), 150, "Tên đề");
        Subject subject = subjects.findById(form.getSubjectId()).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học đang hoạt động."));
        String grade = trimRequired(form.getGradeLevel(), 20, "Khối lớp");
        int duration = form.getDurationMinutes() == null ? 0 : form.getDurationMinutes();
        if (duration < 1 || duration > 240) throw new IllegalArgumentException("Thời lượng đề phải từ 1 đến 240 phút.");
        BigDecimal total = form.getTotalPoints() == null ? BigDecimal.ZERO : form.getTotalPoints().setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0 || total.compareTo(BigDecimal.valueOf(1000)) > 0)
            throw new IllegalArgumentException("Tổng điểm phải lớn hơn 0 và không quá 1.000.");

        List<ExamMatrixCellForm> validCells = Optional.ofNullable(form.getCells()).orElse(List.of()).stream()
                .filter(c -> c != null && c.getQuestionCount() != null && c.getQuestionCount() > 0).toList();
        if (validCells.isEmpty()) throw new IllegalArgumentException("Ma trận cần ít nhất một ô có số câu lớn hơn 0.");
        BigDecimal cellSum = validCells.stream().map(c -> c.getCellPoints() == null ? BigDecimal.ZERO : c.getCellPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (cellSum.compareTo(total) != 0) throw new IllegalArgumentException("Tổng điểm các ô ma trận phải bằng tổng điểm đề.");

        List<ResolvedCell> resolved = new ArrayList<>();
        Set<Long> selected = new HashSet<>();
        for (ExamMatrixCellForm cellForm : validCells) {
            String type = upper(cellForm.getQuestionType()); String difficulty = upper(cellForm.getDifficulty());
            if (!QUESTION_TYPES.contains(type) || !DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException("Dạng câu hỏi hoặc mức độ trong ma trận không hợp lệ.");
            BigDecimal cellPoints = cellForm.getCellPoints();
            if (cellPoints == null || cellPoints.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Điểm mỗi ô ma trận phải lớn hơn 0.");
            String topic = trimNullable(cellForm.getTopic(), 150);
            List<ExamQuestion> candidates = questions.findByOwnerTeacherIdAndSubjectIdAndGradeLevelAndActiveTrue(teacher.getId(), subject.getId(), grade).stream()
                    .filter(q -> q.getQuestionType().equals(type) && q.getDifficulty().equals(difficulty))
                    .filter(q -> topic == null || topic.equalsIgnoreCase(Optional.ofNullable(q.getTopic()).orElse("")))
                    .filter(q -> !selected.contains(q.getId())).collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(candidates);
            int count = cellForm.getQuestionCount();
            if (candidates.size() < count) throw new IllegalArgumentException("Ô " + label(topic, type, difficulty) + " cần " + count + " câu nhưng chỉ còn " + candidates.size() + ".");
            List<ExamQuestion> picked = candidates.subList(0, count);
            picked.forEach(q -> selected.add(q.getId()));
            resolved.add(new ResolvedCell(topic, type, difficulty, count, cellPoints.setScale(2, RoundingMode.HALF_UP), picked));
        }

        ExamBlueprint blueprint = new ExamBlueprint(); blueprint.setOwnerTeacher(teacher); blueprint.setSubject(subject);
        blueprint.setGradeLevel(grade); blueprint.setTitle(title); blueprint.setScope(trimNullable(form.getScope(), 2000));
        blueprint.setDurationMinutes(duration); blueprint.setTotalPoints(total);
        blueprint = blueprints.save(blueprint);
        int position = 0;
        for (ResolvedCell rc : resolved) {
            ExamMatrixCell cell = new ExamMatrixCell(); cell.setBlueprint(blueprint); cell.setTopic(rc.topic());
            cell.setQuestionType(rc.type()); cell.setDifficulty(rc.difficulty()); cell.setQuestionCount(rc.count());
            cell.setCellPoints(rc.points()); cell = cells.save(cell);
        List<BigDecimal> pointShares = ExamScoring.dividePoints(rc.points(), rc.count());
            for (int i = 0; i < rc.questions().size(); i++) {
                ExamBlueprintItem item = new ExamBlueprintItem(); item.setBlueprint(blueprint); item.setMatrixCell(cell);
                item.setQuestion(rc.questions().get(i)); item.setPoints(pointShares.get(i)); item.setPosition(++position); items.save(item);
            }
        }
        return blueprint;
    }

    @Override @Transactional
    public ExamBlueprint saveImportedBlueprint(Long teacherUserId, ExamImportSaveForm form) {
        Teacher teacher = requireTeacher(teacherUserId);
        if (form == null) throw new IllegalArgumentException("Thiếu dữ liệu đề nhập.");
        List<ExamImportQuestionDraft> drafts = Optional.ofNullable(form.getQuestions()).orElse(List.of());
        if (drafts.isEmpty()) throw new IllegalArgumentException("Đề nhập cần có ít nhất một câu hỏi.");

        BigDecimal importedPoints = BigDecimal.ZERO;
        for (int i = 0; i < drafts.size(); i++) {
            ExamImportQuestionDraft draft = drafts.get(i);
            if (draft == null) throw new IllegalArgumentException("Câu " + (i + 1) + " không hợp lệ.");
            validateImportedQuestion(draft, i + 1);
            importedPoints = importedPoints.add(draft.getPoints());
        }
        importedPoints = importedPoints.setScale(2, RoundingMode.HALF_UP);
        BigDecimal confirmedImportTotal = form.getTotalPoints() == null ? null
                : form.getTotalPoints().setScale(2, RoundingMode.HALF_UP);
        if (confirmedImportTotal == null || confirmedImportTotal.compareTo(importedPoints) != 0)
            throw new IllegalArgumentException("Tổng điểm phải bằng tổng điểm từng câu (" + importedPoints + "). Hãy kiểm tra lại trước khi lưu.");

        ExamBlueprint blueprint;
        List<ExamBlueprintItem> oldItems = List.of();
        boolean append = false;
        if (form.getBlueprintId() != null) {
            blueprint = blueprintForTeacher(teacherUserId, form.getBlueprintId());
            oldItems = items.findByBlueprintIdOrderByPositionAsc(blueprint.getId());
            String mode = upper(form.getMode());
            if (!oldItems.isEmpty() && !Set.of("REPLACE", "APPEND").contains(mode))
                throw new IllegalArgumentException("Đề đã có câu hỏi. Hãy chọn thay toàn bộ hoặc bổ sung vào cuối đề.");
            if (!oldItems.isEmpty() && "APPEND".equals(mode)) append = true;
            if ("APPEND".equals(mode) && oldItems.isEmpty()) append = true;

            if (append) {
                BigDecimal total = blueprint.getTotalPoints().add(importedPoints).setScale(2, RoundingMode.HALF_UP);
                if (total.compareTo(BigDecimal.valueOf(1000)) > 0)
                    throw new IllegalArgumentException("Tổng điểm sau khi bổ sung không được quá 1.000.");
                if (!oldItems.isEmpty() && sumPoints(oldItems).compareTo(blueprint.getTotalPoints()) != 0)
                    throw new IllegalArgumentException("Tổng điểm đề hiện tại không khớp với các câu. Hãy sửa đề trước khi bổ sung.");
                blueprint.setTotalPoints(total);
                blueprint.setPreserveQuestionOrder(true);
            } else {
                applyImportedMetadata(blueprint, teacher, form, importedPoints);
                replaceBlueprintItems(blueprint);
            }
        } else {
            blueprint = new ExamBlueprint();
            applyImportedMetadata(blueprint, teacher, form, importedPoints);
            blueprint = blueprints.save(blueprint);
        }

        int lastPosition = oldItems.stream().mapToInt(ExamBlueprintItem::getPosition).max().orElse(0);
        appendImportedQuestions(teacherUserId, blueprint, drafts, append ? lastPosition : 0);
        return blueprints.save(blueprint);
    }

    private void applyImportedMetadata(ExamBlueprint blueprint, Teacher teacher, ExamImportSaveForm form, BigDecimal total) {
        String title = trimRequired(form.getTitle(), 150, "Tên đề");
        if (form.getSubjectId() == null) throw new IllegalArgumentException("Vui lòng chọn môn học.");
        Subject subject = subjects.findById(form.getSubjectId()).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học đang hoạt động."));
        String grade = trimRequired(form.getGradeLevel(), 20, "Khối lớp");
        Integer duration = form.getDurationMinutes();
        if (duration == null || duration < 1 || duration > 240)
            throw new IllegalArgumentException("Thời lượng đề phải từ 1 đến 240 phút.");
        if (total.compareTo(BigDecimal.ZERO) <= 0 || total.compareTo(BigDecimal.valueOf(1000)) > 0)
            throw new IllegalArgumentException("Tổng điểm phải lớn hơn 0 và không quá 1.000.");
        blueprint.setOwnerTeacher(teacher); blueprint.setSubject(subject); blueprint.setGradeLevel(grade);
        blueprint.setTitle(title); blueprint.setScope(trimNullable(form.getScope(), 2000));
        blueprint.setPreserveQuestionOrder(true);
        blueprint.setDurationMinutes(duration); blueprint.setTotalPoints(total);
    }

    private void validateImportedQuestion(ExamImportQuestionDraft draft, int number) {
        String label = "Câu " + number + ": ";
        trimRequired(draft.getPrompt(), 10_000, label + "nội dung");
        String type = upper(draft.getQuestionType());
        String difficulty = upper(draft.getDifficulty());
        if (!QUESTION_TYPES.contains(type)) throw new IllegalArgumentException(label + "hãy chọn dạng câu hỏi.");
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException(label + "hãy chọn mức độ.");
        BigDecimal point = draft.getPoints();
        if (point == null || point.compareTo(BigDecimal.ZERO) <= 0 || point.compareTo(BigDecimal.valueOf(100)) > 0
                || point.scale() > 2) throw new IllegalArgumentException(label + "hãy nhập điểm lớn hơn 0, tối đa 100 và không quá hai chữ số thập phân.");
        if ("MULTIPLE_CHOICE".equals(type)) {
            int count = (int) java.util.stream.Stream.of(draft.getOptionA(), draft.getOptionB(), draft.getOptionC(), draft.getOptionD())
                    .filter(v -> v != null && !v.isBlank()).count();
            if (count < 2 || !upper(draft.getCorrectKey()).matches("[A-D]"))
                throw new IllegalArgumentException(label + "trắc nghiệm cần ít nhất hai phương án và đáp án đúng A–D.");
            String answer = upper(draft.getCorrectKey());
            String selected = switch (answer) { case "A" -> draft.getOptionA(); case "B" -> draft.getOptionB(); case "C" -> draft.getOptionC(); case "D" -> draft.getOptionD(); default -> null; };
            if (selected == null || selected.isBlank()) throw new IllegalArgumentException(label + "đáp án đúng phải thuộc một phương án đã nhập.");
        } else if ("TRUE_FALSE".equals(type) && !Set.of("A", "B").contains(upper(draft.getCorrectKey()))) {
            throw new IllegalArgumentException(label + "hãy chọn đáp án Đúng hoặc Sai.");
        } else if ("FILL_IN".equals(type) && (draft.getAcceptedAnswers() == null || draft.getAcceptedAnswers().isBlank())) {
            throw new IllegalArgumentException(label + "hãy nhập ít nhất một đáp án được chấp nhận.");
        }
    }

    private void replaceBlueprintItems(ExamBlueprint blueprint) {
        List<ExamBlueprintItem> oldItems = items.findByBlueprintIdOrderByPositionAsc(blueprint.getId());
        List<ExamMatrixCell> oldCells = cells.findByBlueprintIdOrderByIdAsc(blueprint.getId());
        if (!oldItems.isEmpty()) {
            items.deleteAll(oldItems);
            items.flush();
        }
        if (!oldCells.isEmpty()) {
            cells.deleteAll(oldCells);
            cells.flush();
        }
    }

    private void appendImportedQuestions(Long teacherUserId, ExamBlueprint blueprint,
                                         List<ExamImportQuestionDraft> drafts, int startingPosition) {
        Map<ImportedCellKey, List<ExamImportQuestionDraft>> grouped = new LinkedHashMap<>();
        for (ExamImportQuestionDraft draft : drafts) {
            String type = upper(draft.getQuestionType());
            String difficulty = upper(draft.getDifficulty());
            String topic = trimNullable(draft.getTopic(), 150);
            grouped.computeIfAbsent(new ImportedCellKey(topic, type, difficulty), ignored -> new ArrayList<>()).add(draft);
        }
        Map<ImportedCellKey, ExamMatrixCell> cellsByGroup = new LinkedHashMap<>();
        for (Map.Entry<ImportedCellKey, List<ExamImportQuestionDraft>> entry : grouped.entrySet()) {
            ImportedCellKey key = entry.getKey();
            List<ExamImportQuestionDraft> group = entry.getValue();
            BigDecimal cellPoints = group.stream().map(ExamImportQuestionDraft::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            ExamMatrixCell cell = new ExamMatrixCell(); cell.setBlueprint(blueprint); cell.setTopic(key.topic());
            cell.setQuestionType(key.type()); cell.setDifficulty(key.difficulty());
            cell.setQuestionCount(group.size()); cell.setCellPoints(cellPoints); cell = cells.save(cell);
            cellsByGroup.put(key, cell);
        }
        int position = startingPosition;
        for (ExamImportQuestionDraft draft : drafts) {
            ImportedCellKey key = new ImportedCellKey(trimNullable(draft.getTopic(), 150),
                    upper(draft.getQuestionType()), upper(draft.getDifficulty()));
            ExamQuestionForm questionForm = new ExamQuestionForm();
            questionForm.setSubjectId(blueprint.getSubject().getId()); questionForm.setGradeLevel(blueprint.getGradeLevel());
            questionForm.setTopic(key.topic()); questionForm.setQuestionType(key.type()); questionForm.setDifficulty(key.difficulty());
            questionForm.setPrompt(draft.getPrompt()); questionForm.setOptionA(draft.getOptionA()); questionForm.setOptionB(draft.getOptionB());
            questionForm.setOptionC(draft.getOptionC()); questionForm.setOptionD(draft.getOptionD());
            questionForm.setCorrectKey(draft.getCorrectKey()); questionForm.setAcceptedAnswers(draft.getAcceptedAnswers());
            questionForm.setExplanation(draft.getExplanation()); questionForm.setDefaultPoints(draft.getPoints());
            ExamQuestion question = saveQuestion(teacherUserId, questionForm);
            ExamBlueprintItem item = new ExamBlueprintItem(); item.setBlueprint(blueprint); item.setMatrixCell(cellsByGroup.get(key));
            item.setQuestion(question); item.setPoints(draft.getPoints().setScale(2, RoundingMode.HALF_UP));
            item.setPosition(++position); items.save(item);
        }
    }

    private record ImportedCellKey(String topic, String type, String difficulty) { }

    @Override @Transactional(readOnly = true)
    public ExamBlueprint blueprintForTeacher(Long teacherUserId, Long blueprintId) {
        return blueprints.findByIdAndOwnerTeacherId(blueprintId, requireTeacher(teacherUserId).getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề nháp của bạn."));
    }

    @Override @Transactional(readOnly = true)
    public List<ExamBlueprintItem> itemsForTeacher(Long teacherUserId, Long blueprintId) {
        blueprintForTeacher(teacherUserId, blueprintId);
        return items.findByBlueprintIdOrderByPositionAsc(blueprintId);
    }

    @Override @Transactional
    public void updateItemPoints(Long teacherUserId, Long blueprintId, Map<Long, BigDecimal> pointUpdates) {
        ExamBlueprint blueprint = blueprintForTeacher(teacherUserId, blueprintId);
        List<ExamBlueprintItem> current = items.findByBlueprintIdOrderByPositionAsc(blueprintId);
        if (pointUpdates == null || pointUpdates.size() != current.size()) throw new IllegalArgumentException("Vui lòng gửi đủ điểm cho toàn bộ câu hỏi.");
        BigDecimal total = BigDecimal.ZERO;
        for (ExamBlueprintItem item : current) {
            BigDecimal point = pointUpdates.get(item.getId());
            if (point == null || point.compareTo(BigDecimal.ZERO) <= 0 || point.scale() > 2)
                throw new IllegalArgumentException("Điểm mỗi câu phải lớn hơn 0 và tối đa hai chữ số thập phân.");
            total = total.add(point); item.setPoints(point);
        }
        if (total.compareTo(blueprint.getTotalPoints()) != 0) throw new IllegalArgumentException("Tổng điểm câu hỏi phải bằng tổng điểm đề " + blueprint.getTotalPoints() + ".");
    }

    @Override @Transactional
    public void replaceItem(Long teacherUserId, Long blueprintId, Long itemId) {
        ExamBlueprint blueprint = blueprintForTeacher(teacherUserId, blueprintId);
        ExamBlueprintItem item = items.findByIdAndBlueprintId(itemId, blueprintId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu trong ma trận."));
        ExamMatrixCell cell = item.getMatrixCell();
        List<Long> used = items.findByBlueprintIdOrderByPositionAsc(blueprintId).stream()
                .filter(i -> !i.getId().equals(itemId)).map(i -> i.getQuestion().getId()).toList();
        List<ExamQuestion> candidates = questions.findByOwnerTeacherIdAndSubjectIdAndGradeLevelAndActiveTrue(
                        requireTeacher(teacherUserId).getId(), blueprint.getSubject().getId(), blueprint.getGradeLevel()).stream()
                .filter(q -> q.getQuestionType().equals(cell.getQuestionType()) && q.getDifficulty().equals(cell.getDifficulty()))
                .filter(q -> cell.getTopic() == null || cell.getTopic().equalsIgnoreCase(Optional.ofNullable(q.getTopic()).orElse("")))
                .filter(q -> !used.contains(q.getId())).collect(Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) throw new IllegalArgumentException("Không còn câu thay thế phù hợp trong ngân hàng.");
        ExamQuestion currentQuestion = item.getQuestion();
        candidates.removeIf(q -> q.getId().equals(currentQuestion.getId()));
        if (candidates.isEmpty()) throw new IllegalArgumentException("Không còn câu thay thế phù hợp trong ngân hàng.");
        item.setQuestion(candidates.get(new Random().nextInt(candidates.size())));
    }

    @Override @Transactional(readOnly = true)
    public List<ClassEntity> classesForTeacher(Long teacherUserId) { return teacherClasses(teacherUserId); }

    @Override @Transactional
    public ExamAssignment assign(Long teacherUserId, Long blueprintId, ExamAssignmentForm form) {
        ExamBlueprint blueprint = blueprintForTeacher(teacherUserId, blueprintId);
        if (form == null || form.getClassId() == null || form.getOpensAt() == null || form.getClosesAt() == null)
            throw new IllegalArgumentException("Chọn lớp và thời gian mở/đóng đề.");
        ClassEntity classEntity = requireTeacherClass(teacherUserId, form.getClassId());
        if (!classEntity.getSubject().getId().equals(blueprint.getSubject().getId())) throw new IllegalArgumentException("Môn học của lớp không khớp với đề.");
        boolean wrongGrade = enrollments.findByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE").stream().filter(this::currentEnrollment)
                .map(ClassStudent::getStudent).filter(Objects::nonNull).map(Student::getGradeLevel).filter(Objects::nonNull)
                .anyMatch(grade -> !grade.isBlank() && !sameGrade(grade, blueprint.getGradeLevel()));
        if (wrongGrade) throw new IllegalArgumentException("Khối lớp của đề không khớp với một hoặc nhiều học sinh đang học trong lớp.");
        LocalDateTime now = now();
        if (!form.getOpensAt().isBefore(form.getClosesAt()) || !form.getClosesAt().isAfter(now)) throw new IllegalArgumentException("Thời gian đóng đề phải sau giờ mở và còn ở tương lai.");
        int duration = form.getDurationMinutes() == null ? blueprint.getDurationMinutes() : form.getDurationMinutes();
        int maxAttempts = form.getMaxAttempts() == null ? 1 : form.getMaxAttempts();
        if (duration < 1 || duration > 240 || maxAttempts < 1 || maxAttempts > 10) throw new IllegalArgumentException("Thời lượng phải từ 1–240 phút, số lượt tối đa từ 1–10.");
        List<ExamBlueprintItem> blueprintItems = items.findByBlueprintIdOrderByPositionAsc(blueprintId);
        if (blueprintItems.isEmpty() || sumPoints(blueprintItems).compareTo(blueprint.getTotalPoints()) != 0)
            throw new IllegalArgumentException("Tổng điểm đề không khớp hoặc đề chưa có câu hỏi.");

        List<SnapshotQuestion> snapshots = blueprintItems.stream().map(this::snapshot).toList();
        ExamAssignment assignment = new ExamAssignment(); assignment.setBlueprint(blueprint); assignment.setClassEntity(classEntity);
        assignment.setCreatedBy(users.findById(teacherUserId).orElseThrow()); assignment.setTitle(blueprint.getTitle());
        assignment.setOpensAt(form.getOpensAt()); assignment.setClosesAt(form.getClosesAt()); assignment.setDurationMinutes(duration);
        assignment.setMaxAttempts(maxAttempts); assignment.setShuffleQuestions(form.isShuffleQuestions());
        assignment.setShuffleOptions(form.isShuffleOptions()); assignment.setTotalPoints(blueprint.getTotalPoints()); assignment.setExamType("ONLINE");
        assignment.setScope(blueprint.getScope());
        assignment.setQuestionsSnapshotJson(writeJson(snapshots)); assignment = assignments.save(assignment);
        notifyClass(assignment, "exam-assigned-" + assignment.getId(), "Có bài thi mới được giao. Hãy đăng nhập cổng học sinh để xem thời gian làm bài.");
        return assignment;
    }

    @Override @Transactional
    public ExamAssignment createDirectAssignment(Long teacherUserId, DirectExamForm form) {
        requireTeacher(teacherUserId);
        if (form == null) throw new IllegalArgumentException("Thiếu thông tin đề thi.");
        String title = trimRequired(form.getTitle(), 150, "Tên đề thi");
        if (form.getClassId() == null || form.getOpensAt() == null || form.getClosesAt() == null)
            throw new IllegalArgumentException("Chọn lớp và thời gian mở/đóng đề.");
        ClassEntity classEntity = requireTeacherClass(teacherUserId, form.getClassId());
        validateAssignmentWindow(form.getOpensAt(), form.getClosesAt());
        int duration = form.getDurationMinutes() == null ? 60 : form.getDurationMinutes();
        int maxAttempts = form.getMaxAttempts() == null ? 1 : form.getMaxAttempts();
        if (duration < 1 || duration > 240 || maxAttempts < 1 || maxAttempts > 10)
            throw new IllegalArgumentException("Thời lượng phải từ 1–240 phút và số lượt tối đa từ 1–10.");
        List<DirectExamQuestionForm> forms = form.getQuestions() == null ? List.of() : form.getQuestions();
        if (forms.isEmpty() || forms.size() > 100) throw new IllegalArgumentException("Đề cần có từ 1 đến 100 câu hỏi.");
        List<SnapshotQuestion> snapshot = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DirectExamQuestionForm question : forms) {
            if (question == null) throw new IllegalArgumentException("Có câu hỏi chưa nhập dữ liệu.");
            String type = upper(question.getType());
            if (!QUESTION_TYPES.contains(type)) throw new IllegalArgumentException("Dạng câu hỏi không hợp lệ.");
            String prompt = trimRequired(question.getPrompt(), 10_000, "Nội dung câu hỏi");
            BigDecimal points = question.getPoints();
            if (points == null || points.compareTo(BigDecimal.ZERO) <= 0 || points.compareTo(BigDecimal.valueOf(100)) > 0 || points.scale() > 2)
                throw new IllegalArgumentException("Điểm mỗi câu phải lớn hơn 0, tối đa 100 và không quá 2 chữ số thập phân.");
            List<Choice> choices = new ArrayList<>();
            String correctKey = null;
            List<String> accepted = List.of();
            if ("MULTIPLE_CHOICE".equals(type)) {
                addChoice(choices, "A", question.getOptionA()); addChoice(choices, "B", question.getOptionB());
                addChoice(choices, "C", question.getOptionC()); addChoice(choices, "D", question.getOptionD());
                String selectedKey = upper(question.getCorrectKey());
                correctKey = selectedKey;
                boolean selectedKeyExists = false;
                for (Choice choice : choices) if (choice.key().equals(selectedKey)) selectedKeyExists = true;
                if (choices.size() < 2 || !selectedKeyExists)
                    throw new IllegalArgumentException("Câu trắc nghiệm cần ít nhất hai lựa chọn và một đáp án đúng.");
            } else if ("TRUE_FALSE".equals(type)) {
                choices = List.of(new Choice("A", "Đúng"), new Choice("B", "Sai"));
                correctKey = upper(question.getCorrectKey());
                if (!Set.of("A", "B").contains(correctKey)) throw new IllegalArgumentException("Chọn đáp án đúng/sai.");
            } else if ("FILL_IN".equals(type)) {
                accepted = Arrays.stream(Optional.ofNullable(question.getAcceptedAnswers()).orElse("").split("\\R"))
                        .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
                if (accepted.isEmpty() || accepted.stream().anyMatch(value -> value.length() > 300))
                    throw new IllegalArgumentException("Câu trả lời ngắn cần ít nhất một đáp án được chấp nhận (mỗi đáp án tối đa 300 ký tự).");
            }
            total = total.add(points);
            snapshot.add(new SnapshotQuestion(null, type, prompt, null, "Không áp dụng", points.setScale(2, RoundingMode.HALF_UP),
                    choices, correctKey, accepted, null));
        }
        if (total.compareTo(BigDecimal.valueOf(999999)) > 0) throw new IllegalArgumentException("Tổng điểm đề vượt giới hạn lưu trữ.");
        ExamAssignment assignment = new ExamAssignment();
        assignment.setBlueprint(null); assignment.setClassEntity(classEntity); assignment.setCreatedBy(users.findById(teacherUserId).orElseThrow());
        assignment.setTitle(title); assignment.setExamType("ONLINE"); assignment.setOpensAt(form.getOpensAt());
        assignment.setClosesAt(form.getClosesAt()); assignment.setDurationMinutes(duration); assignment.setMaxAttempts(maxAttempts);
        assignment.setShuffleQuestions(form.isShuffleQuestions()); assignment.setShuffleOptions(form.isShuffleOptions());
        assignment.setTotalPoints(total.setScale(2, RoundingMode.HALF_UP)); assignment.setQuestionsSnapshotJson(writeJson(snapshot));
        ExamAssignment saved = assignments.save(assignment);
        notifyClass(saved, "exam-assigned-" + saved.getId(), "Có bài thi mới được giao. Hãy đăng nhập cổng học sinh để xem thời gian làm bài.");
        return saved;
    }

    private void validateAssignmentWindow(LocalDateTime opensAt, LocalDateTime closesAt) {
        if (!opensAt.isBefore(closesAt) || !closesAt.isAfter(now()))
            throw new IllegalArgumentException("Thời gian đóng đề phải sau giờ mở và còn ở tương lai.");
    }

    @Override @Transactional(readOnly = true)
    public List<ExamAssignment> assignmentsForTeacher(Long teacherUserId) {
        Set<Long> classIds = teacherClasses(teacherUserId).stream().map(ClassEntity::getId).collect(Collectors.toSet());
        return classIds.stream().flatMap(id -> assignments.findByClassEntityIdOrderByOpensAtDesc(id).stream())
                .distinct().sorted(Comparator.comparing(ExamAssignment::getCreatedAt).reversed()).toList();
    }

    @Override @Transactional(readOnly = true)
    public ExamAssignment assignmentForTeacher(Long teacherUserId, Long assignmentId) { return requireTeacherAssignment(teacherUserId, assignmentId); }

    @Override @Transactional(readOnly = true)
    public List<TeacherExamAttemptView> attemptsForTeacher(Long teacherUserId, Long assignmentId) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF được chấm trong trang bài nộp ảnh.");
        return attempts.findByAssignmentIdOrderByAttemptNumberAsc(assignment.getId()).stream()
                .sorted(Comparator.comparing((ExamAttempt a) -> a.getStudent().getFullName()).thenComparingInt(ExamAttempt::getAttemptNumber))
                .map(a -> teacherAttemptView(a, false)).toList();
    }

    @Override @Transactional(readOnly = true)
    public TeacherExamAttemptView attemptForTeacher(Long teacherUserId, Long assignmentId, Long attemptId) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF không có lượt làm trực tuyến.");
        ExamAttempt attempt = attempts.findById(attemptId).filter(a -> a.getAssignment().getId().equals(assignmentId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi."));
        return teacherAttemptView(attempt, true);
    }

    @Override @Transactional
    public void gradeEssay(Long teacherUserId, Long assignmentId, Long attemptId, int questionNumber, BigDecimal score, String feedback) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF được chấm thủ công theo bài nộp ảnh.");
        ExamAttempt attempt = attempts.findById(attemptId).filter(a -> a.getAssignment().getId().equals(assignment.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi."));
        if (attempt.getStatus().equals(IN_PROGRESS)) throw new IllegalArgumentException("Học sinh chưa nộp lượt thi này.");
        List<SnapshotQuestion> snapshot = snapshot(assignment);
        SnapshotQuestion q = findQuestion(snapshot, questionNumber);
        if (!"ESSAY".equals(q.type())) throw new IllegalArgumentException("Chỉ chấm thủ công câu tự luận.");
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(q.points()) > 0 || score.scale() > 2)
            throw new IllegalArgumentException("Điểm câu tự luận phải nằm trong khoảng 0 đến " + q.points() + ".");
        if (feedback != null && feedback.length() > 1000) throw new IllegalArgumentException("Nhận xét tối đa 1.000 ký tự.");
        ExamAttemptAnswer answer = answers.findByAttemptIdAndQuestionNumber(attemptId, questionNumber).orElseGet(() -> {
            ExamAttemptAnswer created = new ExamAttemptAnswer(); created.setAttempt(attempt); created.setQuestionNumber(questionNumber); return created;
        });
        answer.setScore(score); answer.setCorrect(null); answer.setTeacherFeedback(trimNullable(feedback, 1000)); answers.save(answer);
        finalizeManualGradeIfComplete(attempt, snapshot);
        if (assignment.getResultPublishedAt() != null) notifyGradeUpdate(attempt);
    }

    @Override @Transactional
    public void publishResults(Long teacherUserId, Long assignmentId) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF không dùng luồng công bố điểm trực tuyến.");
        if (assignment.getResultPublishedAt() != null) return;
        if (now().isBefore(assignment.getClosesAt())) throw new IllegalArgumentException("Chỉ công bố sau khi đề đã đóng.");
        List<ExamAttempt> all = attempts.findByAssignmentIdOrderByAttemptNumberAsc(assignmentId);
        for (ExamAttempt attempt : all) if (IN_PROGRESS.equals(attempt.getStatus()) && !attempt.getDeadlineAt().isAfter(now())) submit(attempt, true);
        if (all.stream().anyMatch(a -> IN_PROGRESS.equals(a.getStatus()) || WAITING_REVIEW.equals(a.getStatus())))
            throw new IllegalArgumentException("Còn lượt thi chưa nộp hoặc câu tự luận chưa chấm.");
        assignment.setResultPublishedAt(now());
        notifyClass(assignment, "exam-results-" + assignment.getId(), "Kết quả một bài thi đã được công bố. Hãy đăng nhập cổng để xem.");
    }

    @Override @Transactional(readOnly = true)
    public ExamAssignmentReport reportForTeacher(Long teacherUserId, Long assignmentId) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF không có báo cáo câu hỏi trực tuyến.");
        List<ExamAttempt> all = attempts.findByAssignmentIdOrderByAttemptNumberAsc(assignmentId).stream()
                .filter(a -> !IN_PROGRESS.equals(a.getStatus())).toList();
        List<TeacherExamAttemptView> views = all.stream().sorted(Comparator.comparing((ExamAttempt a) -> a.getStudent().getFullName())
                        .thenComparingInt(ExamAttempt::getAttemptNumber)).map(a -> teacherAttemptView(a, true)).toList();
        Map<String, long[]> topic = new TreeMap<>(), type = new TreeMap<>(), difficulty = new TreeMap<>();
        List<SnapshotQuestion> qs = snapshot(assignment);
        for (ExamAttempt attempt : all) {
            Map<Integer, ExamAttemptAnswer> responseByNumber = answers.findByAttemptIdOrderByQuestionNumberAsc(attempt.getId()).stream()
                    .collect(Collectors.toMap(ExamAttemptAnswer::getQuestionNumber, a -> a));
            for (int index = 0; index < qs.size(); index++) {
                SnapshotQuestion q = qs.get(index);
                if ("ESSAY".equals(q.type())) continue;
                ExamAttemptAnswer answer = responseByNumber.get(index);
                boolean correct = answer != null && Boolean.TRUE.equals(answer.getCorrect());
                accumulate(topic, q.topic() == null || q.topic().isBlank() ? "Chưa phân loại" : q.topic(), correct);
                accumulate(type, q.type(), correct); accumulate(difficulty, q.difficulty(), correct);
            }
        }
        return new ExamAssignmentReport(assignment, views, metric(topic), metric(type), metric(difficulty));
    }

    @Override @Transactional(readOnly = true)
    public List<StudentExamAssignmentView> assignmentsForStudent(Long studentUserId) {
        Student student = studentForUser(studentUserId);
        Set<Long> classIds = enrollments.findByStudentId(student.getId()).stream().filter(this::currentEnrollment)
                .map(e -> e.getClassEntity().getId()).collect(Collectors.toSet());
        return classIds.stream().flatMap(id -> assignments.findByClassEntityIdOrderByOpensAtDesc(id).stream())
                .distinct().filter(a -> !a.isPdfExam()).map(a -> studentAssignmentView(a, student, true))
                .sorted(Comparator.comparing(StudentExamAssignmentView::opensAt).reversed()).toList();
    }

    @Override @Transactional
    public StudentExamAttemptView startOrResume(Long studentUserId, Long assignmentId) {
        Student student = studentForUser(studentUserId);
        requireStudentAssignment(student, assignmentId, true);
        ExamAssignment assignment = assignments.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề thi."));
        requireStudentAssignment(student, assignmentId, true);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF cần nộp ảnh bài làm, không làm bài trực tuyến.");
        Optional<ExamAttempt> active = attempts.findFirstByAssignmentIdAndStudentIdAndStatusOrderByAttemptNumberDesc(assignmentId, student.getId(), IN_PROGRESS);
        if (active.isPresent()) {
            ExamAttempt a = active.get(); if (!a.getDeadlineAt().isAfter(now())) submit(a, true);
            return studentAttemptView(a);
        }
        LocalDateTime now = now();
        if (assignment.getResultPublishedAt() != null) throw new IllegalArgumentException("Đề đã công bố kết quả.");
        if (now.isBefore(assignment.getOpensAt())) throw new IllegalArgumentException("Đề chưa đến giờ mở.");
        if (!now.isBefore(assignment.getClosesAt())) throw new IllegalArgumentException("Đề đã đóng.");
        List<ExamAttempt> previous = attempts.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, student.getId());
        if (previous.size() >= assignment.getMaxAttempts()) throw new IllegalArgumentException("Bạn đã dùng hết số lượt thi.");
        List<SnapshotQuestion> snapshot = snapshot(assignment);
        if (snapshot.isEmpty()) throw new IllegalArgumentException("Đề thi chưa có câu hỏi.");
        List<Integer> order = new ArrayList<>(); for (int i = 0; i < snapshot.size(); i++) order.add(i);
        if (assignment.isShuffleQuestions()) Collections.shuffle(order);
        Map<String, List<String>> optionOrder = new LinkedHashMap<>();
        for (int i = 0; i < snapshot.size(); i++) {
            List<String> keys = snapshot.get(i).options().stream().map(Choice::key).collect(Collectors.toCollection(ArrayList::new));
            if (assignment.isShuffleOptions()) Collections.shuffle(keys);
            optionOrder.put(String.valueOf(i), keys);
        }
        ExamAttempt attempt = new ExamAttempt(); attempt.setAssignment(assignment); attempt.setStudent(student);
        attempt.setAttemptNumber(previous.size() + 1); attempt.setStatus(IN_PROGRESS); attempt.setStartedAt(now);
        LocalDateTime durationEnd = now.plusMinutes(assignment.getDurationMinutes());
        attempt.setDeadlineAt(durationEnd.isBefore(assignment.getClosesAt()) ? durationEnd : assignment.getClosesAt());
        attempt.setQuestionOrderJson(writeJson(order)); attempt.setOptionOrderJson(writeJson(optionOrder));
        return studentAttemptView(attempts.save(attempt));
    }

    @Override @Transactional
    public StudentExamAttemptView attemptForStudent(Long studentUserId, Long attemptId) {
        Student student = studentForUser(studentUserId);
        ExamAttempt attempt = attempts.findById(attemptId).filter(a -> a.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi của bạn."));
        if (IN_PROGRESS.equals(attempt.getStatus()) && !attempt.getDeadlineAt().isAfter(now())) submit(attempt, true);
        return studentAttemptView(attempt);
    }

    @Override @Transactional
    public boolean saveAnswer(Long studentUserId, Long attemptId, int questionNumber, String answerText, String optionKey) {
        Student student = studentForUser(studentUserId);
        ExamAttempt attempt = ownedAttempt(student, attemptId);
        if (!IN_PROGRESS.equals(attempt.getStatus())) return false;
        if (!attempt.getDeadlineAt().isAfter(now())) { submit(attempt, true); return false; }
        List<SnapshotQuestion> snapshot = snapshot(attempt.getAssignment());
        SnapshotQuestion q = findQuestion(snapshot, questionNumber);
        if ("MULTIPLE_CHOICE".equals(q.type()) || "TRUE_FALSE".equals(q.type())) {
            String selectedOption = optionKey;
            if (selectedOption != null && q.options().stream().noneMatch(o -> o.key().equals(selectedOption))) throw new IllegalArgumentException("Phương án không thuộc câu hỏi này.");
            answerText = null;
        } else {
            if (answerText != null && answerText.length() > 10_000) throw new IllegalArgumentException("Câu trả lời tối đa 10.000 ký tự.");
            answerText = answerText == null || answerText.isBlank() ? null : answerText.trim(); optionKey = null;
        }
        ExamAttemptAnswer answer = answers.findByAttemptIdAndQuestionNumber(attemptId, questionNumber).orElseGet(() -> {
            ExamAttemptAnswer created = new ExamAttemptAnswer(); created.setAttempt(attempt); created.setQuestionNumber(questionNumber); return created;
        });
        answer.setAnswerText(answerText); answer.setSelectedOptionKey(optionKey); answers.save(answer);
        return true;
    }

    @Override @Transactional
    public void submitAttempt(Long studentUserId, Long attemptId) {
        Student student = studentForUser(studentUserId);
        ExamAttempt attempt = ownedAttempt(student, attemptId);
        submit(attempt, !attempt.getDeadlineAt().isAfter(now()));
    }

    @Override @Transactional(readOnly = true)
    public List<StudentExamAssignmentView> publishedResultsForStudent(Long studentUserId) {
        Student student = studentForUser(studentUserId);
        return attempts.findByStudentIdOrderByStartedAtDesc(student.getId()).stream()
                .map(ExamAttempt::getAssignment).distinct().filter(a -> !a.isPdfExam()).filter(a -> a.getResultPublishedAt() != null)
                .map(a -> studentAssignmentView(a, student, false)).sorted(Comparator.comparing(StudentExamAssignmentView::closesAt).reversed()).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<StudentExamAssignmentView> publishedResultsForParent(Long parentUserId, Long childId) {
        User parentUser = users.findById(parentUserId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản phụ huynh."));
        if (parentUser.getRole() == null || !"ROLE_PARENT".equals(parentUser.getRole().getName())
                || !Boolean.TRUE.equals(parentUser.getActive()))
            throw new IllegalArgumentException("Tài khoản phụ huynh không hoạt động.");
        List<Student> children = students.findByParentUserId(parentUserId).stream()
                .filter(s -> childId == null || Objects.equals(s.getId(), childId)).toList();
        if (childId != null && children.isEmpty()) throw new IllegalArgumentException("Hồ sơ học sinh không thuộc tài khoản phụ huynh này.");
        List<StudentExamAssignmentView> result = new ArrayList<>();
        for (Student child : children) {
            attempts.findByStudentIdOrderByStartedAtDesc(child.getId()).stream().map(ExamAttempt::getAssignment).distinct()
                    .filter(a -> !a.isPdfExam()).filter(a -> a.getResultPublishedAt() != null).map(a -> studentAssignmentView(a, child, false)).forEach(result::add);
        }
        return result.stream().sorted(Comparator.comparing(StudentExamAssignmentView::closesAt).reversed()).toList();
    }

    @Override @Transactional(readOnly = true)
    public ExamExportDocument exportForTeacher(Long teacherUserId, Long assignmentId, boolean answerKey) {
        ExamAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        if (assignment.isPdfExam()) throw new IllegalArgumentException("Đề PDF không có bản xuất câu hỏi trực tuyến.");
        List<ExamExportQuestion> exported = new ArrayList<>(); int number = 0;
        for (SnapshotQuestion q : snapshot(assignment)) {
            String answer = null;
            if (answerKey) {
                if (q.correctOptionKey() != null) answer = q.correctOptionKey() + ": " + q.options().stream().filter(o -> o.key().equals(q.correctOptionKey())).map(Choice::text).findFirst().orElse("");
                else if (!q.acceptedAnswers().isEmpty()) answer = String.join("; ", q.acceptedAnswers());
            }
            List<ExamExportQuestion.ExamExportOption> options = q.options().stream().map(o -> new ExamExportQuestion.ExamExportOption(o.key(), o.text())).toList();
            exported.add(new ExamExportQuestion(++number, q.type(), q.prompt(), q.points(), options, answer,
                    answerKey ? q.explanation() : null));
        }
        String subtitle = assignment.getClassEntity().getName() + " · " + assignment.getClassEntity().getSubject().getName()
                + (assignment.getScope() == null || assignment.getScope().isBlank() ? "" : " · " + assignment.getScope());
        return new ExamExportDocument((answerKey ? "Đáp án - " : "") + assignment.getTitle(), subtitle, exported);
    }

    @Override @Transactional
    public void autoSubmitExpired() {
        for (ExamAttempt attempt : attempts.findByStatusAndDeadlineAtLessThanEqual(IN_PROGRESS, now())) submit(attempt, true);
    }

    @Override @Transactional
    public void notifyUpcomingDeadlines() {
        LocalDateTime now = now();
        for (ExamAssignment assignment : assignments.findAll()) {
            if (assignment.getResultPublishedAt() != null || !assignment.getClosesAt().isAfter(now)
                    || assignment.getClosesAt().isAfter(now.plusHours(24))) continue;
            Map<Long, List<ExamAttempt>> attemptsByStudent = attempts.findByAssignmentIdOrderByAttemptNumberAsc(assignment.getId()).stream()
                    .collect(Collectors.groupingBy(a -> a.getStudent().getId()));
            List<Long> noAttemptsRemaining = attemptsByStudent.entrySet().stream()
                    .filter(entry -> entry.getValue().size() >= assignment.getMaxAttempts()
                            && entry.getValue().stream().noneMatch(a -> IN_PROGRESS.equals(a.getStatus())))
                    .map(Map.Entry::getKey).toList();
            notifyPendingStudents(assignment, "exam-deadline-" + assignment.getId(),
                    "Một bài thi sắp đến hạn. Hãy đăng nhập cổng học sinh để kiểm tra.", noAttemptsRemaining);
        }
    }

    private void submit(ExamAttempt attempt, boolean automatic) {
        if (!IN_PROGRESS.equals(attempt.getStatus())) return;
        List<SnapshotQuestion> snapshot = snapshot(attempt.getAssignment());
        Map<Integer, ExamAttemptAnswer> existing = answers.findByAttemptIdOrderByQuestionNumberAsc(attempt.getId()).stream()
                .collect(Collectors.toMap(ExamAttemptAnswer::getQuestionNumber, a -> a));
        BigDecimal objective = BigDecimal.ZERO; boolean essay = false;
        for (int number = 0; number < snapshot.size(); number++) {
            SnapshotQuestion q = snapshot.get(number);
            ExamAttemptAnswer answer = existing.get(number);
            if ("ESSAY".equals(q.type())) { essay = true; continue; }
            boolean correct = false;
            if (answer != null) {
                if ("MULTIPLE_CHOICE".equals(q.type()) || "TRUE_FALSE".equals(q.type())) correct = Objects.equals(q.correctOptionKey(), answer.getSelectedOptionKey());
                else correct = ExamScoring.exactFillMatch(answer.getAnswerText(), q.acceptedAnswers());
            }
            ExamAttemptAnswer graded = answer == null ? new ExamAttemptAnswer() : answer;
            if (answer == null) { graded.setAttempt(attempt); graded.setQuestionNumber(number); }
            graded.setCorrect(correct); graded.setScore(correct ? q.points() : BigDecimal.ZERO); answers.save(graded);
            if (correct) objective = objective.add(q.points());
        }
        attempt.setObjectiveScore(objective.setScale(2, RoundingMode.HALF_UP)); attempt.setSubmittedAt(now()); attempt.setAutoSubmitted(automatic);
        if (essay) { attempt.setStatus(WAITING_REVIEW); attempt.setTotalScore(null); }
        else { attempt.setStatus(SUBMITTED); attempt.setManualScore(BigDecimal.ZERO); attempt.setTotalScore(objective.setScale(2, RoundingMode.HALF_UP)); }
    }

    private void finalizeManualGradeIfComplete(ExamAttempt attempt, List<SnapshotQuestion> snapshot) {
        BigDecimal manual = BigDecimal.ZERO;
        for (int i = 0; i < snapshot.size(); i++) if ("ESSAY".equals(snapshot.get(i).type())) {
            ExamAttemptAnswer answer = answers.findByAttemptIdAndQuestionNumber(attempt.getId(), i).orElse(null);
            if (answer == null || answer.getScore() == null) { attempt.setStatus(WAITING_REVIEW); attempt.setTotalScore(null); return; }
            manual = manual.add(answer.getScore());
        }
        attempt.setManualScore(manual.setScale(2, RoundingMode.HALF_UP));
        attempt.setTotalScore(attempt.getObjectiveScore().add(manual).setScale(2, RoundingMode.HALF_UP)); attempt.setStatus(GRADED);
    }

    private StudentExamAttemptView studentAttemptView(ExamAttempt attempt) {
        List<SnapshotQuestion> snapshot = snapshot(attempt.getAssignment());
        List<Integer> order = readJson(attempt.getQuestionOrderJson(), new TypeReference<>() { });
        Map<String, List<String>> optionOrder = readJson(attempt.getOptionOrderJson(), new TypeReference<>() { });
        Map<Integer, ExamAttemptAnswer> saved = answers.findByAttemptIdOrderByQuestionNumberAsc(attempt.getId()).stream()
                .collect(Collectors.toMap(ExamAttemptAnswer::getQuestionNumber, a -> a));
        List<StudentExamQuestionView> views = new ArrayList<>();
        for (Integer index : order) {
            if (index == null || index < 0 || index >= snapshot.size()) continue;
            SnapshotQuestion q = snapshot.get(index); List<String> keys = optionOrder.getOrDefault(String.valueOf(index), List.of());
            Map<String, Choice> byKey = q.options().stream().collect(Collectors.toMap(Choice::key, c -> c));
            List<StudentExamOptionView> options = keys.stream().map(byKey::get).filter(Objects::nonNull)
                    .map(c -> new StudentExamOptionView(c.key(), c.text())).toList();
            ExamAttemptAnswer answer = saved.get(index);
            views.add(new StudentExamQuestionView(index, q.type(), q.prompt(), q.points(), options,
                    answer == null ? null : answer.getAnswerText(), answer == null ? null : answer.getSelectedOptionKey()));
        }
        return new StudentExamAttemptView(attempt.getAssignment().getId(), attempt.getId(), attempt.getAssignment().getTitle(),
                attempt.getAssignment().getClassEntity().getSubject().getName(), attempt.getAssignment().getScope(),
                attempt.getStatus(), attempt.isAutoSubmitted(), attempt.getDeadlineAt(), now(),
                attempt.getDeadlineAt().atZone(APP_ZONE).toInstant().toEpochMilli(), now().atZone(APP_ZONE).toInstant().toEpochMilli(), views);
    }

    private TeacherExamAttemptView teacherAttemptView(ExamAttempt attempt, boolean detailed) {
        List<SnapshotQuestion> snapshot = snapshot(attempt.getAssignment());
        Map<Integer, ExamAttemptAnswer> saved = answers.findByAttemptIdOrderByQuestionNumberAsc(attempt.getId()).stream()
                .collect(Collectors.toMap(ExamAttemptAnswer::getQuestionNumber, a -> a));
        List<TeacherReviewQuestionView> views = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            SnapshotQuestion q = snapshot.get(i); ExamAttemptAnswer answer = saved.get(i);
            String expected = q.correctOptionKey() == null ? String.join("; ", q.acceptedAnswers()) : q.correctOptionKey();
            if (q.correctOptionKey() != null) expected = q.options().stream().filter(o -> o.key().equals(q.correctOptionKey()))
                    .map(o -> o.key() + ": " + o.text()).findFirst().orElse(expected);
            List<StudentExamOptionView> options = detailed ? q.options().stream().map(o -> new StudentExamOptionView(o.key(), o.text())).toList() : List.of();
            views.add(new TeacherReviewQuestionView(i, q.type(), q.prompt(), q.points(), options, expected,
                    detailed && answer != null ? answer.getAnswerText() : null,
                    detailed && answer != null ? answer.getSelectedOptionKey() : null,
                    detailed && answer != null ? answer.getCorrect() : null,
                    detailed && answer != null ? answer.getScore() : null,
                    detailed && answer != null ? answer.getTeacherFeedback() : null));
        }
        return new TeacherExamAttemptView(attempt.getId(), attempt.getStudent(), attempt.getAttemptNumber(), attempt.getStatus(), attempt.isAutoSubmitted(),
                attempt.getStartedAt(), attempt.getSubmittedAt(), attempt.getTotalScore(), views);
    }

    private StudentExamAssignmentView studentAssignmentView(ExamAssignment assignment, Student student, boolean requireScope) {
        if (requireScope) requireStudentAssignment(student, assignment.getId(), false);
        List<ExamAttempt> studentAttempts = attempts.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignment.getId(), student.getId());
        ExamAttempt bestAttempt = assignment.getResultPublishedAt() == null ? null : studentAttempts.stream()
                .filter(a -> a.getTotalScore() != null).max(Comparator.comparing(ExamAttempt::getTotalScore)).orElse(null);
        BigDecimal official = bestAttempt == null ? null : bestAttempt.getTotalScore();
        String teacherFeedback = bestAttempt == null ? null : answers.findByAttemptIdOrderByQuestionNumberAsc(bestAttempt.getId()).stream()
                .filter(a -> a.getTeacherFeedback() != null && !a.getTeacherFeedback().isBlank())
                .map(a -> "Câu " + (a.getQuestionNumber() + 1) + ": " + a.getTeacherFeedback())
                .collect(Collectors.joining(" · "));
        if (teacherFeedback != null && teacherFeedback.isBlank()) teacherFeedback = null;
        String status;
        LocalDateTime now = now();
        if (assignment.getResultPublishedAt() != null) status = "RESULT_PUBLISHED";
        else if (studentAttempts.stream().anyMatch(a -> IN_PROGRESS.equals(a.getStatus()))) status = "IN_PROGRESS";
        else if (now.isBefore(assignment.getOpensAt())) status = "UPCOMING";
        else if (now.isBefore(assignment.getClosesAt()) && studentAttempts.size() < assignment.getMaxAttempts()) status = "OPEN";
        else if (studentAttempts.stream().anyMatch(a -> WAITING_REVIEW.equals(a.getStatus()))) status = "WAITING_RESULT";
        else status = "CLOSED";
        return new StudentExamAssignmentView(assignment.getId(), assignment.getTitle(), assignment.getScope(), assignment.getClassEntity().getName(),
                assignment.getClassEntity().getSubject().getName(), student.getFullName(), assignment.getOpensAt(), assignment.getClosesAt(),
                assignment.getDurationMinutes(), studentAttempts.size(), assignment.getMaxAttempts(), status, official, assignment.getTotalPoints(), teacherFeedback);
    }

    private void notifyClass(ExamAssignment assignment, String eventKey, String message) {
        notifyPendingStudents(assignment, eventKey, message, List.of());
    }

    private void notifyGradeUpdate(ExamAttempt attempt) {
        Student student = attempt.getStudent();
        String eventKey = "exam-grade-updated-" + attempt.getId() + "-" + UUID.randomUUID();
        String message = "Có thông tin mới trong cổng học tập. Hãy đăng nhập để kiểm tra.";
        if (student.getUser() != null)
            notifications.notifyUser(student.getUser().getId(), eventKey, "GENERAL", "Thông báo học tập", message, "/student/exams/results");
        if (student.getParent() != null && student.getParent().getUser() != null)
            notifications.notifyUser(student.getParent().getUser().getId(), eventKey, "GENERAL", "Thông báo học tập", message, "/parent/exams");
    }

    private void notifyPendingStudents(ExamAssignment assignment, String eventKey, String message, List<Long> excludedStudentIds) {
        List<ClassStudent> roster = enrollments.findByClassEntityIdAndStatus(assignment.getClassEntity().getId(), "ACTIVE").stream()
                .filter(this::currentEnrollment).toList();
        Set<Long> excluded = new HashSet<>(excludedStudentIds == null ? List.of() : excludedStudentIds);
        List<Student> targetStudents = roster.stream().map(ClassStudent::getStudent).filter(Objects::nonNull)
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getUser() != null && Boolean.TRUE.equals(s.getUser().getActive()))
                .filter(s -> !excluded.contains(s.getId())).toList();
        List<Long> studentIds = targetStudents.stream().map(Student::getUser).filter(Objects::nonNull).map(User::getId).distinct().toList();
        List<Long> parentIds = targetStudents.stream().map(Student::getParent).filter(Objects::nonNull).map(Parent::getUser)
                .filter(Objects::nonNull).map(User::getId).distinct().toList();
        notifications.notifyUsers(studentIds, eventKey, "GENERAL", "Thông báo bài thi", message, "/student/exams");
        notifications.notifyUsers(parentIds, eventKey, "GENERAL", "Thông báo bài thi", message, "/parent/exams");
    }

    private ExamQuestion toEntity(ExamBlueprintItem item) { return item.getQuestion(); }
    private SnapshotQuestion snapshot(ExamBlueprintItem item) {
        ExamQuestion q = toEntity(item); Key key = readJson(q.getAnswerKeyJson(), new TypeReference<>() { });
        List<Choice> choices = readJson(q.getOptionsJson() == null ? "[]" : q.getOptionsJson(), new TypeReference<>() { });
        return new SnapshotQuestion(q.getId(), q.getQuestionType(), q.getPrompt(), q.getTopic(), q.getDifficulty(), item.getPoints(), choices,
                key.correctOptionKey(), key.acceptedAnswers() == null ? List.of() : key.acceptedAnswers(), q.getExplanation());
    }
    private List<SnapshotQuestion> snapshot(ExamAssignment assignment) {
        return readJson(assignment.getQuestionsSnapshotJson(), new TypeReference<>() { });
    }
    private SnapshotQuestion findQuestion(List<SnapshotQuestion> snapshot, int number) {
        if (number < 0 || number >= snapshot.size()) throw new IllegalArgumentException("Câu hỏi không thuộc đề thi.");
        return snapshot.get(number);
    }
    private ExamAttempt ownedAttempt(Student student, Long id) {
        return attempts.findById(id).filter(a -> a.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi của bạn."));
    }
    private Student requireStudentAssignment(Student student, Long id, boolean requirePublished) {
        ExamAssignment a = assignments.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề thi."));
        boolean enrolled = enrollments.findByClassEntityIdAndStudentId(a.getClassEntity().getId(), student.getId()).filter(this::currentEnrollment).isPresent();
        if (!enrolled) throw new IllegalArgumentException("Đề thi này không thuộc lớp bạn đang học.");
        if (requirePublished && a.getCreatedAt() != null && a.getCreatedAt().isAfter(now())) throw new IllegalArgumentException("Đề thi chưa được giao.");
        return student;
    }
    private ExamAssignment requireTeacherAssignment(Long userId, Long assignmentId) {
        Set<Long> classIds = teacherClasses(userId).stream().map(ClassEntity::getId).collect(Collectors.toSet());
        return assignments.findById(assignmentId).filter(a -> classIds.contains(a.getClassEntity().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề hoặc bạn không phụ trách lớp này."));
    }
    private Teacher requireTeacher(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user.getRole() == null || !"ROLE_TEACHER".equals(user.getRole().getName()) || !Boolean.TRUE.equals(user.getActive()))
            throw new IllegalArgumentException("Chỉ giáo viên đang hoạt động mới được dùng ngân hàng đề.");
        return teachers.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ giáo viên."));
    }
    private ClassEntity requireTeacherClass(Long userId, Long classId) {
        ClassEntity c = classes.findById(classId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp."));
        if (!teacherOwnsClass(userId, c)) throw new IllegalArgumentException("Bạn không phụ trách lớp này.");
        if (!"ACTIVE".equals(c.getStatus())) throw new IllegalArgumentException("Chỉ có thể giao đề cho lớp đang hoạt động.");
        return c;
    }
    private List<ClassEntity> teacherClasses(Long userId) {
        requireTeacher(userId);
        return classes.findAll().stream().filter(c -> teacherOwnsClass(userId, c)).toList();
    }
    private boolean teacherOwnsClass(Long userId, ClassEntity c) {
        return (c.getTeacher() != null && c.getTeacher().getUser() != null && Objects.equals(c.getTeacher().getUser().getId(), userId))
                || (c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser() != null && Objects.equals(c.getAssistantTeacher().getUser().getId(), userId));
    }
    private Student studentForUser(Long userId) {
        Student student = students.findByUserId(userId).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa được liên kết với hồ sơ học sinh đang hoạt động."));
        if (student.getUser() == null || student.getUser().getRole() == null || !"ROLE_STUDENT".equals(student.getUser().getRole().getName())
                || !Boolean.TRUE.equals(student.getUser().getActive())) throw new IllegalArgumentException("Tài khoản học sinh không hoạt động.");
        return student;
    }
    private boolean currentEnrollment(ClassStudent e) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return "ACTIVE".equals(e.getStatus()) && (e.getEnrolledDate() == null || !e.getEnrolledDate().isAfter(today))
                && (e.getDroppedDate() == null || e.getDroppedDate().isAfter(today));
    }
    private BigDecimal sumPoints(List<ExamBlueprintItem> list) { return list.stream().map(ExamBlueprintItem::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP); }
    private LocalDateTime now() { return LocalDateTime.now(APP_ZONE); }
    private String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private boolean sameGrade(String left, String right) {
        return normalizeGrade(left).equals(normalizeGrade(right));
    }
    private String normalizeGrade(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceFirst("^l\\u1edbp\\s*", "").trim();
    }
    private String trimRequired(String value, int max, String label) {
        String v = value == null ? "" : value.trim(); if (v.isBlank() || v.length() > max) throw new IllegalArgumentException(label + " bắt buộc, tối đa " + max + " ký tự."); return v;
    }
    private String trimNullable(String value, int max) {
        String v = value == null ? "" : value.trim(); if (v.isEmpty()) return null;
        if (v.length() > max) throw new IllegalArgumentException("Nội dung không quá " + max + " ký tự."); return v;
    }
    private void addChoice(List<Choice> list, String key, String content) {
        if (content != null && !content.isBlank()) list.add(new Choice(key, trimRequired(content, 2000, "Phương án " + key)));
    }
    private String writeJson(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Không lưu được dữ liệu câu hỏi.", e); } }
    private <T> T readJson(String value, TypeReference<T> type) { try { return mapper.readValue(value, type); } catch (Exception e) { throw new IllegalStateException("Dữ liệu đề thi không hợp lệ.", e); } }
    private String label(String topic, String type, String difficulty) { return (topic == null ? "mọi chủ đề" : topic) + ", " + type + ", " + difficulty; }
    private void accumulate(Map<String, long[]> map, String key, boolean correct) {
        long[] counts = map.computeIfAbsent(key, ignored -> new long[2]); counts[1]++; if (correct) counts[0]++;
    }
    private List<ExamQuestionAccuracy> metric(Map<String, long[]> map) {
        return map.entrySet().stream().map(e -> new ExamQuestionAccuracy(e.getKey(), e.getValue()[0], e.getValue()[1],
                e.getValue()[1] == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(e.getValue()[0] * 100.0 / e.getValue()[1]).setScale(2, RoundingMode.HALF_UP))).toList();
    }
    private static String optionKey(String key) { return key == null ? null : key.toUpperCase(Locale.ROOT); }
    private record Choice(String key, String text) { }
    private record Key(String correctOptionKey, List<String> acceptedAnswers) { }
    private record SnapshotQuestion(Long sourceId, String type, String prompt, String topic, String difficulty,
                                    BigDecimal points, List<Choice> options, String correctOptionKey,
                                    List<String> acceptedAnswers, String explanation) { }
    private record ResolvedCell(String topic, String type, String difficulty, int count, BigDecimal points,
                                List<ExamQuestion> questions) { }
}
