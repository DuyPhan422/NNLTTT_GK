-- =====================================================
-- SCRIPT DỌN DẸP DỮ LIỆU CŨ ĐỂ TEST LẠI HỆ THỐNG
-- Chạy script này trước khi test để tránh lỗi duplicate Bill
-- =====================================================

-- 1. XÓA DỮ LIỆU CŨ CỦA STUDENT ID = 3 (Hoặc thay số khác)
SET @test_student_id = 3;

-- Xóa Payments (phải xóa trước vì có FK tới Invoices)
DELETE FROM payments WHERE student_id = @test_student_id;

-- Xóa Invoices
DELETE FROM invoices WHERE student_id = @test_student_id;

-- Xóa Enrollments
DELETE FROM enrollments WHERE student_id = @test_student_id;

-- Xóa Attendances (nếu có)
DELETE FROM attendances WHERE student_id = @test_student_id;

-- =====================================================
-- 2. KIỂM TRA LẠI SAU KHI XÓA
-- =====================================================
SELECT 'Invoices còn lại:' AS info;
SELECT * FROM invoices WHERE student_id = @test_student_id;

SELECT 'Enrollments còn lại:' AS info;
SELECT * FROM enrollments WHERE student_id = @test_student_id;

SELECT 'Payments còn lại:' AS info;
SELECT * FROM payments WHERE student_id = @test_student_id;

-- =====================================================
-- 3. KIỂM TRA DỮ LIỆU CẦN THIẾT ĐỂ TEST
-- =====================================================

-- 3.1. Kiểm tra Student tồn tại không
SELECT 'Student Info:' AS info;
SELECT student_id, full_name, email, status 
FROM students 
WHERE student_id = @test_student_id;

-- Nếu không có → Tạo mới hoặc đổi ID trong Main.java
-- INSERT INTO students (full_name, email, phone, registration_date, status) 
-- VALUES ('Test Student', 'test@example.com', '0123456789', CURDATE(), 'Active');

-- 3.2. Kiểm tra có lớp "Open" hoặc "Planned" không
SELECT 'Classes Available for Registration:' AS info;
SELECT class_id, class_name, status, start_date, 
       (SELECT course_name FROM courses WHERE course_id = classes.course_id) AS course_name,
       (SELECT fee FROM courses WHERE course_id = classes.course_id) AS fee
FROM classes 
WHERE status IN ('Open', 'Planned')
ORDER BY class_id;

-- Nếu không có → Tạo mới hoặc đổi status
-- UPDATE classes SET status = 'Open' WHERE class_id IN (1, 2, 3);

-- 3.3. Kiểm tra có Course không
SELECT 'Courses Available:' AS info;
SELECT course_id, course_name, level, fee, status 
FROM courses 
WHERE status = 'Active'
ORDER BY course_id;

-- =====================================================
-- 4. TẠO DỮ LIỆU MẪU (NẾU CẦN)
-- =====================================================

-- 4.1. Tạo Courses nếu chưa có
-- INSERT INTO courses (course_name, description, level, duration, duration_unit, fee, status)
-- VALUES 
--   ('IELTS Cơ bản', 'Khóa học IELTS cho người mới bắt đầu', 'Beginner', 12, 'Week', 5000000.00, 'Active'),
--   ('TOEIC 450+', 'Khóa học TOEIC đạt 450 điểm trở lên', 'Intermediate', 10, 'Week', 4500000.00, 'Active'),
--   ('Giao tiếp Nâng cao', 'Luyện giao tiếp tiếng Anh thực tế', 'Advanced', 8, 'Week', 6000000.00, 'Active');

-- 4.2. Tạo Rooms nếu chưa có
-- INSERT INTO rooms (room_name, capacity, location, status)
-- VALUES 
--   ('Phòng A101', 30, 'Tầng 1', 'Active'),
--   ('Phòng A102', 25, 'Tầng 1', 'Active'),
--   ('Phòng B201', 35, 'Tầng 2', 'Active');

-- 4.3. Tạo Teachers nếu chưa có
-- INSERT INTO teachers (full_name, phone, email, specialty, hire_date, status)
-- VALUES 
--   ('John Smith', '0901234567', 'john@example.com', 'IELTS', '2024-01-01', 'Active'),
--   ('Sarah Johnson', '0902345678', 'sarah@example.com', 'TOEIC', '2024-01-15', 'Active');

-- 4.4. Tạo Classes
-- INSERT INTO classes (class_name, course_id, teacher_id, start_date, end_date, max_student, room_id, status)
-- VALUES 
--   ('IELTS-2024-01', 1, 1, '2024-05-01', '2024-07-31', 30, 1, 'Open'),
--   ('TOEIC-2024-02', 2, 2, '2024-05-15', '2024-07-15', 25, 2, 'Open'),
--   ('GIAOTIEP-2024-03', 3, 1, '2024-06-01', '2024-07-31', 35, 3, 'Planned');

-- =====================================================
-- 5. XÓA TOÀN BỘ DỮ LIỆU TEST (CHỈ DÙNG KHI CẦN RESET HOÀN TOÀN)
-- =====================================================
-- ⚠️ CẢNH BÁO: Script này sẽ XÓA HẾT dữ liệu test! Cẩn thận!
-- 
-- DELETE FROM payments;
-- DELETE FROM invoices;
-- DELETE FROM enrollments;
-- DELETE FROM attendances;
-- ALTER TABLE payments AUTO_INCREMENT = 1;
-- ALTER TABLE invoices AUTO_INCREMENT = 1;
-- ALTER TABLE enrollments AUTO_INCREMENT = 1;
-- ALTER TABLE attendances AUTO_INCREMENT = 1;

-- =====================================================
-- 6. QUERY KIỂM TRA DUPLICATE BILLS
-- =====================================================
-- Tìm học viên nào có nhiều hơn 1 Bill "Issued" (Lỗi!)
SELECT student_id, COUNT(*) AS total_bills
FROM invoices 
WHERE status = 'Issued'
GROUP BY student_id
HAVING COUNT(*) > 1;

-- Xóa Bill duplicate (giữ lại Bill mới nhất)
-- DELETE i1 FROM invoices i1
-- INNER JOIN invoices i2 
-- WHERE i1.student_id = i2.student_id 
--   AND i1.status = 'Issued' 
--   AND i2.status = 'Issued'
--   AND i1.invoice_id < i2.invoice_id;

-- =====================================================
-- KẾT THÚC SCRIPT
-- =====================================================
COMMIT;
