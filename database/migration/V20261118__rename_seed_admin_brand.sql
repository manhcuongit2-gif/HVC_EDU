-- Rename only the default seeded administrator display name; retain accounts and email addresses.
USE `educenter_db`;

UPDATE `users`
SET `full_name` = 'Quản Trị Viên Luyện Thi Hùng Cường'
WHERE `username` = 'admin'
  AND `full_name` = 'Quản Trị Viên EduCenter';
