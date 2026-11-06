package com.educenter.controller.admin;

import com.educenter.dto.ScheduleDTO;
import com.educenter.entity.Schedule;
import com.educenter.service.ClassService;
import com.educenter.service.RoomService;
import com.educenter.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    private final ScheduleService scheduleService;
    private final ClassService classService;
    private final RoomService roomService;

    public AdminScheduleController(ScheduleService scheduleService,
                                   ClassService classService,
                                   RoomService roomService) {
        this.scheduleService = scheduleService;
        this.classService = classService;
        this.roomService = roomService;
    }

    @GetMapping
    public String listSchedules(Model model) {
        List<Schedule> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        return "admin/schedules/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long classId, Model model) {
        ScheduleDTO dto = new ScheduleDTO();
        if (classId != null) {
            dto.setClassId(classId);
        }
        dto.setDayOfWeek(2); // Default Thứ Hai
        dto.setStartTime(LocalTime.of(17, 30));
        dto.setEndTime(LocalTime.of(19, 30));
        dto.setActive(true);

        model.addAttribute("scheduleDTO", dto);
        model.addAttribute("classes", classService.getActiveClasses());
        model.addAttribute("rooms", roomService.findAllActive());
        model.addAttribute("isEdit", false);
        return "admin/schedules/form";
    }

    @PostMapping("/add")
    public String addSchedule(@Valid @ModelAttribute("scheduleDTO") ScheduleDTO dto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("classes", classService.getActiveClasses());
            model.addAttribute("rooms", roomService.findAllActive());
            model.addAttribute("isEdit", false);
            return "admin/schedules/form";
        }

        try {
            scheduleService.saveSchedule(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Xếp lịch học thành công! Không có xung đột trùng lịch.");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("classes", classService.getActiveClasses());
            model.addAttribute("rooms", roomService.findAllActive());
            model.addAttribute("isEdit", false);
            return "admin/schedules/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Schedule s = scheduleService.getScheduleById(id);
            ScheduleDTO dto = new ScheduleDTO();
            dto.setId(s.getId());
            dto.setClassId(s.getClassEntity().getId());
            dto.setRoomId(s.getRoom().getId());
            dto.setDayOfWeek(s.getDayOfWeek());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setActive(s.getActive());

            model.addAttribute("scheduleDTO", dto);
            model.addAttribute("classes", classService.getActiveClasses());
            model.addAttribute("rooms", roomService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/schedules/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/schedules";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @Valid @ModelAttribute("scheduleDTO") ScheduleDTO dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("classes", classService.getActiveClasses());
            model.addAttribute("rooms", roomService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/schedules/form";
        }

        try {
            scheduleService.updateSchedule(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch học thành công!");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("classes", classService.getActiveClasses());
            model.addAttribute("rooms", roomService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/schedules/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.deleteSchedule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa lịch học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }
}
