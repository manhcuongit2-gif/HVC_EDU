package com.educenter.controller.admin;

import com.educenter.entity.Room;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Danh sách phòng học
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("rooms", roomService.findAll());
        return "admin/rooms/list";
    }

    // Form thêm mới phòng học
    @GetMapping("/add")
    public String showAddForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("room", new Room());
        return "admin/rooms/form";
    }

    // Xử lý thêm mới phòng học
    @PostMapping("/add")
    public String addRoom(@Valid @ModelAttribute("room") Room room,
                          BindingResult result,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (roomService.existsByName(room.getName())) {
            result.rejectValue("name", "error.room", "Tên phòng học đã tồn tại trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            return "admin/rooms/form";
        }

        room.setActive(true);
        roomService.save(room);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng học \"" + room.getName() + "\" thành công!");
        return "redirect:/admin/rooms";
    }

    // Form sửa phòng học
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Room room = roomService.findById(id).orElse(null);
        if (room == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phòng học với ID: " + id);
            return "redirect:/admin/rooms";
        }
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("room", room);
        return "admin/rooms/form";
    }

    // Xử lý cập nhật phòng học
    @PostMapping("/edit/{id}")
    public String updateRoom(@PathVariable Long id,
                             @Valid @ModelAttribute("room") Room room,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Room existing = roomService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phòng học!");
            return "redirect:/admin/rooms";
        }

        if (!existing.getName().equals(room.getName()) && roomService.existsByName(room.getName())) {
            result.rejectValue("name", "error.room", "Tên phòng học đã tồn tại trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            room.setId(id);
            return "admin/rooms/form";
        }

        existing.setName(room.getName());
        existing.setCapacity(room.getCapacity());
        existing.setLocation(room.getLocation());
        existing.setDescription(room.getDescription());
        roomService.save(existing);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng học \"" + existing.getName() + "\" thành công!");
        return "redirect:/admin/rooms";
    }

    // Bật/Tắt trạng thái hoạt động
    @GetMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Room room = roomService.findById(id).orElse(null);
        if (room == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phòng học!");
            return "redirect:/admin/rooms";
        }
        roomService.toggleActive(id);
        String status = room.getActive() ? "Đã khóa" : "Đã kích hoạt";
        redirectAttributes.addFlashAttribute("successMessage", status + " phòng học \"" + room.getName() + "\"!");
        return "redirect:/admin/rooms";
    }
}
