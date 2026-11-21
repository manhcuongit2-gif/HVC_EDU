package com.educenter.service.impl;

import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountSettingsService {
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final long MAX_AVATAR_PIXELS = 20_000_000L;
    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;
    private final Path uploadDirectory;

    public AccountSettingsService(UserRepository users, StudentRepository students, PasswordEncoder passwordEncoder,
                                  @Value("${app.storage.upload-dir:./uploads}") String uploadDirectory) {
        this.users = users;
        this.students = students;
        this.passwordEncoder = passwordEncoder;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String email, String phone, String address) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
        String normalizedAddress = address == null || address.isBlank() ? null : address.trim();
        if (normalizedName.isBlank() || normalizedName.length() > 100)
            throw new IllegalArgumentException("Họ và tên bắt buộc, tối đa 100 ký tự.");
        if (normalizedEmail.length() > 100 || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Email không hợp lệ hoặc vượt quá 100 ký tự.");
        if (normalizedPhone != null && normalizedPhone.length() > 20)
            throw new IllegalArgumentException("Số điện thoại tối đa 20 ký tự.");
        if (normalizedAddress != null && normalizedAddress.length() > 255)
            throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");
        if (users.existsByEmailAndIdNot(normalizedEmail, userId))
            throw new IllegalArgumentException("Email đã được một tài khoản khác sử dụng.");

        user.setFullName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setAddress(normalizedAddress);
        users.save(user);
        students.findByUserId(userId).ifPresent(student -> {
            student.setFullName(normalizedName);
            student.setPhone(normalizedPhone);
            students.save(student);
        });
        return user;
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword, String confirmation) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (currentPassword == null || currentPassword.isBlank() || !passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 72)
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 6 đến 72 ký tự.");
        if (!newPassword.equals(confirmation)) throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
    }

    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        validateImage(file);
        try {
            BufferedImage image = decodeSafe(file.getBytes());
            Path directory = avatarDirectory();
            ensureNotPublicStatic(directory);
            Files.createDirectories(directory);
            directory = directory.toRealPath();
            ensureNotPublicStatic(directory);
            String filename = UUID.randomUUID() + ".png";
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ.");
            if (!ImageIO.write(image, "png", target.toFile())) throw new IllegalArgumentException("Không thể chuẩn hóa ảnh tải lên.");
            String old = user.getAvatarFilename();
            user.setAvatarFilename(filename);
            users.save(user);
            deleteOldAvatar(old, filename);
            return filename;
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc ảnh. Hãy chọn lại ảnh JPG hoặc PNG hợp lệ.");
        }
    }

    @Transactional
    public void removeAvatar(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        String old = user.getAvatarFilename();
        user.setAvatarFilename(null);
        users.save(user);
        deleteOldAvatar(old, null);
    }

    public Path resolveOwnAvatar(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        String filename = user.getAvatarFilename();
        if (filename == null || !filename.matches("[0-9a-fA-F-]{36}\\.png"))
            throw new IllegalArgumentException("Tài khoản chưa có ảnh đại diện.");
        Path directory = avatarDirectory();
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path))
            throw new IllegalArgumentException("Ảnh đại diện không còn trên máy chủ.");
        return path;
    }

    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_BYTES)
            throw new IllegalArgumentException("Ảnh đại diện phải có dung lượng từ 1 byte đến 5 MB.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, JPEG hoặc PNG.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47)
                return "png";
            if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff)
                return "jpeg";
            throw new IllegalArgumentException("Nội dung tệp không phải ảnh JPG/PNG hợp lệ.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc ảnh tải lên.");
        }
    }

    private BufferedImage decodeSafe(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            if (input == null) throw new IllegalArgumentException("Ảnh không đúng định dạng.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Ảnh không đúng định dạng.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_AVATAR_PIXELS)
                    throw new IllegalArgumentException("Kích thước ảnh vượt giới hạn an toàn.");
                BufferedImage image = reader.read(0);
                if (image == null) throw new IllegalArgumentException("Không thể giải mã ảnh.");
                return image;
            } finally { reader.dispose(); }
        }
    }

    private Path avatarDirectory() { return uploadDirectory.resolve("avatars").normalize(); }

    private void ensureNotPublicStatic(Path directory) {
        Path sourceStatic = Path.of("src/main/resources/static").toAbsolutePath().normalize();
        Path buildStatic = Path.of("target/classes/static").toAbsolutePath().normalize();
        if (directory.startsWith(sourceStatic) || directory.startsWith(buildStatic))
            throw new IllegalStateException("Thư mục ảnh đại diện phải nằm ngoài thư mục static công khai.");
    }

    private void deleteOldAvatar(String oldFilename, String currentFilename) {
        if (oldFilename == null || oldFilename.equals(currentFilename) || !oldFilename.matches("[0-9a-fA-F-]{36}\\.png")) return;
        Path directory = avatarDirectory();
        Path oldPath = directory.resolve(oldFilename).normalize();
        if (!oldPath.startsWith(directory)) return;
        try { Files.deleteIfExists(oldPath); } catch (IOException ignored) { }
    }
}
