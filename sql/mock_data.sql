-- =========================
-- 0) CLEAN SAMPLE (optional)
-- =========================
SET SQL_SAFE_UPDATES = 0;   -- TẮT chế độ an toàn
SET FOREIGN_KEY_CHECKS = 0; -- Tắt kiểm tra khóa ngoại để xóa không bị vướng

DELETE FROM notifications;
DELETE FROM certificates;
DELETE FROM placement_tests;
DELETE FROM promotions;
DELETE FROM results;
DELETE FROM attendances;
DELETE FROM schedules;
DELETE FROM payments;
DELETE FROM invoices;
DELETE FROM enrollments;
DELETE FROM classes;
DELETE FROM rooms;
DELETE FROM branches;
DELETE FROM user_accounts;
DELETE FROM staffs;
DELETE FROM teachers;
DELETE FROM courses;
DELETE FROM students;

SET FOREIGN_KEY_CHECKS = 1; -- Bật lại kiểm tra khóa ngoại
SET SQL_SAFE_UPDATES = 1;   -- BẬT LẠI chế độ an toàn

-- =========================
-- 1) Branches (optional but recommended)
-- =========================
INSERT INTO branches(branch_name, address, phone, status) VALUES
('Cơ sở Quận 1', '12 Nguyễn Huệ, Q.1, TP.HCM', '0909000001', 'Hoạt động'),
('Cơ sở Thủ Đức', '88 Võ Văn Ngân, TP.Thủ Đức, TP.HCM', '0909000002', 'Hoạt động');

-- =========================
-- 2) Rooms
-- =========================
INSERT INTO rooms(room_name, capacity, location, status, branch_id) VALUES
('P101', 25, 'Tầng 1', 'Hoạt động', 1),
('P202', 30, 'Tầng 2', 'Hoạt động', 1),
('TD-01', 22, 'Khu A', 'Hoạt động', 2),
('TD-02', 28, 'Khu B', 'Hoạt động', 2);

-- =========================
-- 3) Courses
-- =========================
INSERT INTO courses(course_name, description, level, duration, duration_unit, fee, status) VALUES
('English Communication A1', 'Giao tiếp cơ bản A1, luyện phản xạ', 'Cơ bản', 8, 'Tuần', 2500000, 'Hoạt động'),
('IELTS Foundation', 'Nền tảng IELTS: từ vựng + ngữ pháp + kỹ năng', 'Trung cấp', 10, 'Tuần', 4500000, 'Hoạt động'),
('TOEIC 650+', 'Luyện đề TOEIC mục tiêu 650+', 'Trung cấp', 8, 'Tuần', 3200000, 'Hoạt động');

-- =========================
-- 4) Teachers
-- =========================
INSERT INTO teachers(full_name, phone, email, specialty, hire_date, status) VALUES
('Nguyễn Minh Anh', '0911000001', 'minhanh.teacher@center.vn', 'GiaoTiep', '2024-06-15', 'Hoạt động'),
('Trần Quốc Huy',  '0911000002', 'quochuy.teacher@center.vn',  'IELTS',    '2023-10-01', 'Hoạt động'),
('Lê Thu Trang',   '0911000003', 'thutrang.teacher@center.vn', 'TOEIC',    '2024-02-20', 'Hoạt động');

-- =========================
-- 5) Staffs
-- =========================
INSERT INTO staffs(full_name, role, phone, email, status) VALUES
('Phạm Hoài Nam', 'Quản lý',     '0922000001', 'nam.manager@center.vn', 'Hoạt động'),
('Võ Ngọc Linh',  'Tư vấn',  '0922000002', 'linh.consult@center.vn','Hoạt động'),
('Đặng Hải Yến',  'Kế toán',  '0922000003', 'yen.acc@center.vn',     'Hoạt động');

-- =========================
-- 6) Students (10 học viên)
-- =========================
INSERT INTO students(full_name, date_of_birth, gender, phone, email, address, registration_date, status) VALUES
('Phạm Gia Bảo',   '2006-04-10', 'Nam',   '0933000001', 'baopg01@mail.com',  'Q.1, TP.HCM', '2026-01-05', 'Hoạt động'),
('Nguyễn Mỹ Linh', '2005-09-21', 'Nữ', '0933000002', 'linhnm02@mail.com', 'Q.3, TP.HCM', '2026-01-06', 'Hoạt động'),
('Trần Đức Long',  '2004-11-02', 'Nam',   '0933000003', 'longtd03@mail.com', 'Q.5, TP.HCM', '2026-01-06', 'Hoạt động'),
('Lê Ngọc Hân',    '2006-07-15', 'Nữ', '0933000004', 'hanln04@mail.com',  'TP.Thủ Đức',  '2026-01-07', 'Hoạt động'),
('Võ Khánh Vy',    '2005-02-18', 'Nữ', '0933000005', 'vyvk05@mail.com',   'TP.Thủ Đức',  '2026-01-07', 'Hoạt động'),
('Đỗ Minh Khang',  '2003-12-30', 'Nam',   '0933000006', 'khangdm06@mail.com','Q.7, TP.HCM', '2026-01-08', 'Hoạt động'),
('Phan Thảo Nguyên','2004-05-09','Nữ', '0933000007', 'nguyenpt07@mail.com','Q.10, TP.HCM','2026-01-09', 'Hoạt động'),
('Bùi Anh Tuấn',   '2002-08-28', 'Nam',   '0933000008', 'tuanba08@mail.com', 'Q.Bình Thạnh','2026-01-10', 'Hoạt động'),
('Ngô Quỳnh Chi',  '2005-01-12', 'Nữ', '0933000009', 'chingq09@mail.com', 'Q.Tân Bình',  '2026-01-10', 'Hoạt động'),
('Lý Quốc Bảo',    '2003-03-03', 'Nam',   '0933000010', 'baolq10@mail.com',  'Q.Gò Vấp',    '2026-01-11', 'Hoạt động');

-- =========================
-- 7) Classes
-- (class_name, course_id, teacher_id, start_date, end_date, max_student, room_id, status, branch_id)
-- =========================
INSERT INTO classes(class_name, course_id, teacher_id, start_date, end_date, max_student, room_id, status, branch_id) VALUES
('COM-A1-0201', 1, 1, '2026-02-10', '2026-04-05', 25, 1, 'Đang diễn ra', 1),
('IELTS-F-0201',2, 2, '2026-02-12', '2026-04-20', 30, 2, 'Đang diễn ra', 1),
('TOEIC-650-0201',3,3,'2026-02-15', '2026-04-10', 28, 4, 'Đang diễn ra', 2),
-- Lớp mới mở đăng ký (để test chức năng đăng ký)
('COM-A1-0301', 1, 1, '2026-03-10', '2026-05-05', 25, 3, 'Mở lớp', 2),
('IELTS-F-0301', 2, 2, '2026-03-12', '2026-05-20', 30, 1, 'Mở lớp', 1),
('TOEIC-650-0301', 3, 3, '2026-03-15', '2026-05-10', 28, 2, 'Mở lớp', 1);

-- =========================
-- 8) Schedules (mỗi lớp 4 buổi mẫu)
-- =========================
-- COM-A1-0201: T3/T5 18:30-20:30
INSERT INTO schedules(class_id, study_date, start_time, end_time, room_id) VALUES
(1, '2026-02-10', '18:30:00', '20:30:00', 1),
(1, '2026-02-12', '18:30:00', '20:30:00', 1),
(1, '2026-02-17', '18:30:00', '20:30:00', 1),
(1, '2026-02-19', '18:30:00', '20:30:00', 1);

-- IELTS-F-0201: T2/T4 19:00-21:00
INSERT INTO schedules(class_id, study_date, start_time, end_time, room_id) VALUES
(2, '2026-02-12', '19:00:00', '21:00:00', 2),
(2, '2026-02-16', '19:00:00', '21:00:00', 2),
(2, '2026-02-18', '19:00:00', '21:00:00', 2),
(2, '2026-02-23', '19:00:00', '21:00:00', 2);

-- TOEIC-650-0201: CN 08:00-10:00
INSERT INTO schedules(class_id, study_date, start_time, end_time, room_id) VALUES
(3, '2026-02-15', '08:00:00', '10:00:00', 4),
(3, '2026-02-22', '08:00:00', '10:00:00', 4),
(3, '2026-03-01', '08:00:00', '10:00:00', 4),
(3, '2026-03-08', '08:00:00', '10:00:00', 4);

-- =========================
-- 9) Enrollments (ghi danh)
-- =========================
-- Lớp COM (student 1..5)
INSERT INTO enrollments(student_id, class_id, enrollment_date, status, result) VALUES
(1, 1, '2026-02-01', 'Đã đăng ký', 'Chưa có'),
(2, 1, '2026-02-01', 'Đã đăng ký', 'Chưa có'),
(3, 1, '2026-02-02', 'Đã đăng ký', 'Chưa có'),
(4, 1, '2026-02-02', 'Đã đăng ký', 'Chưa có'),
(5, 1, '2026-02-03', 'Đã đăng ký', 'Chưa có');

-- Lớp IELTS (student 6..9)
INSERT INTO enrollments(student_id, class_id, enrollment_date, status, result) VALUES
(6, 2, '2026-02-03', 'Đã đăng ký', 'Chưa có'),
(7, 2, '2026-02-04', 'Đã đăng ký', 'Chưa có'),
(8, 2, '2026-02-05', 'Đã đăng ký', 'Chưa có'),
(9, 2, '2026-02-05', 'Đã đăng ký', 'Chưa có');

-- Lớp TOEIC (student 10 + 3 + 7)
INSERT INTO enrollments(student_id, class_id, enrollment_date, status, result) VALUES
(10, 3, '2026-02-06', 'Đã đăng ký', 'Chưa có'),
(3,  3, '2026-02-06', 'Đã đăng ký', 'Chưa có'),
(7,  3, '2026-02-07', 'Đã đăng ký', 'Chưa có');

-- =========================
-- 10) Promotions (optional)
-- =========================
INSERT INTO promotions(promo_name, discount_type, discount_value, start_date, end_date, status) VALUES
('Tết 2026 -10%', 'Percent', 10.00, '2026-01-01', '2026-02-28', 'Hoạt động'),
('Giảm 200k',     'Amount',  200000, '2026-02-01', '2026-03-31', 'Hoạt động');

-- =========================
-- 11) Invoices (mỗi học viên 1 hóa đơn)
-- =========================
-- total_amount có thể là học phí (courses.fee), demo: COM=2.5m, IELTS=4.5m, TOEIC=3.2m
INSERT INTO invoices(student_id, total_amount, issue_date, status, note, promotion_id) VALUES
(1, 2500000, '2026-02-01', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', 1),
(2, 2500000, '2026-02-01', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', 1),
(3, 2500000, '2026-02-02', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', NULL),
(4, 2500000, '2026-02-02', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', NULL),
(5, 2500000, '2026-02-03', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', 2),

(6, 4500000, '2026-02-03', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', 1),
(7, 4500000, '2026-02-04', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', NULL),
(8, 4500000, '2026-02-05', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', 2),
(9, 4500000, '2026-02-05', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', NULL),

(10,3200000, '2026-02-06', 'Chờ thanh toán', 'Học phí lớp TOEIC-650-0201', 2);

-- =========================
-- 12) Payments (thanh toán)
-- - demo: một số học viên trả đủ, một số trả 2 lần
-- =========================
-- Helper: mapping enrollment_id theo thứ tự insert ở trên:
-- 1..5  : COM (students 1..5)
-- 6..9  : IELTS (students 6..9)
-- 10..12: TOEIC (student 10, 3, 7)

-- Student 1: trả đủ
INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
(1, 1, 1, 2500000, '2026-02-01 10:15:00', 'Chuyển khoản', 'Hoàn thành', 'VCB-0001');

-- Student 2: trả 2 lần
INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
(2, 2, 2, 1500000, '2026-02-01 11:00:00', 'Tiền mặt', 'Hoàn thành', 'CASH-0002-A'),
(2, 2, 2, 1000000, '2026-02-05 16:30:00', 'Momo', 'Hoàn thành', 'MOMO-0002-B');

-- Student 6: trả đủ IELTS
INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
(6, 6, 6, 4500000, '2026-02-03 09:20:00', 'Chuyển khoản', 'Hoàn thành', 'ACB-0006');

-- Student 7: trả 2 lần IELTS
INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
(7, 7, 7, 2500000, '2026-02-04 14:00:00', 'Thẻ ngân hàng', 'Hoàn thành', 'VISA-0007-A'),
(7, 7, 7, 2000000, '2026-02-10 14:10:00', 'ZaloPay', 'Hoàn thành', 'ZALO-0007-B');

-- Student 10: trả đủ TOEIC
INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
(10, 10, 10, 3200000, '2026-02-06 18:00:00', 'Tiền mặt', 'Hoàn thành', 'CASH-0010');

-- Cập nhật trạng thái invoice Paid cho những người trả đủ (demo)
UPDATE invoices SET status='Đã thanh toán' WHERE invoice_id IN (1,6,10);
-- Student 2 và 7 trả đủ sau 2 lần (demo: set Paid)
UPDATE invoices SET status='Đã thanh toán' WHERE invoice_id IN (2,7);

-- =========================
-- 13) Attendance (điểm danh mẫu cho 2 buổi đầu)
-- =========================
-- Lớp COM (class_id=1), buổi 2026-02-10 và 2026-02-12
INSERT INTO attendances(student_id, class_id, attend_date, status, note) VALUES
(1,1,'2026-02-10','Có mặt',NULL),
(2,1,'2026-02-10','Đi trễ','Đến trễ 10 phút'),
(3,1,'2026-02-10','Có mặt',NULL),
(4,1,'2026-02-10','Vắng','Bận việc gia đình'),
(5,1,'2026-02-10','Có mặt',NULL),

(1,1,'2026-02-12','Có mặt',NULL),
(2,1,'2026-02-12','Có mặt',NULL),
(3,1,'2026-02-12','Có mặt',NULL),
(4,1,'2026-02-12','Có mặt',NULL),
(5,1,'2026-02-12','Đi trễ','Kẹt xe');

-- Lớp IELTS (class_id=2), buổi 2026-02-12
INSERT INTO attendances(student_id, class_id, attend_date, status, note) VALUES
(6,2,'2026-02-12','Có mặt',NULL),
(7,2,'2026-02-12','Có mặt',NULL),
(8,2,'2026-02-12','Vắng','Ốm'),
(9,2,'2026-02-12','Có mặt',NULL);

-- Lớp TOEIC (class_id=3), buổi 2026-02-15
INSERT INTO attendances(student_id, class_id, attend_date, status, note) VALUES
(10,3,'2026-02-15','Có mặt',NULL),
(3, 3,'2026-02-15','Có mặt',NULL),
(7, 3,'2026-02-15','Đi trễ','Đến trễ 5 phút');

-- =========================
-- 14) Results (điểm cuối khóa demo)
-- =========================
-- demo cho 1 vài học viên (giả lập đã hoàn tất)
INSERT INTO results(student_id, class_id, score, grade, comment) VALUES
(1, 1, 86.50, 'A',  'Phản xạ tốt, phát âm rõ'),
(2, 1, 78.00, 'B+', 'Tiến bộ rõ rệt sau 2 tuần'),
(6, 2, 80.50, 'B+', 'Writing cần luyện thêm cấu trúc'),
(10,3, 75.00, 'B',  'Nghe Part 3-4 cần cải thiện');

-- =========================
-- 15) Placement tests (optional)
-- =========================
INSERT INTO placement_tests(student_id, test_date, score, suggested_level, note) VALUES
(4, '2026-01-20', 45.00, 'Cơ bản', 'Nên học A1 trước'),
(8, '2026-01-22', 62.50, 'Trung cấp', 'Phù hợp IELTS Foundation');

-- =========================
-- 16) Certificates (optional)
-- =========================
INSERT INTO certificates(student_id, class_id, cert_name, issue_date, serial_no) VALUES
(1, 1, 'Certificate of Completion - Communication A1', '2026-04-06', 'CERT-COM-A1-0001');

-- =========================
-- 17) User accounts (demo)
-- password_hash: dùng placeholder. Khi làm app, hash bằng BCrypt/Argon2
-- =========================
INSERT INTO user_accounts(username, password_hash, role, teacher_id, student_id, staff_id, is_active) VALUES
('admin',  '$2a$10$PLACEHOLDER_HASH_ADMIN', 'Quản trị',   NULL, NULL, NULL, 1),
('t.minhanh', '$2a$10$PLACEHOLDER_HASH_T1', 'Giáo viên', 1,    NULL, NULL, 1),
('t.quochuy',  '$2a$10$PLACEHOLDER_HASH_T2', 'Giáo viên', 2,    NULL, NULL, 1),
('t.thutrang', '$2a$10$PLACEHOLDER_HASH_T3', 'Giáo viên', 3,    NULL, NULL, 1),
('s.baopg01',   '$2a$10$PLACEHOLDER_HASH_S1', 'Học viên', NULL, 1,   NULL, 1),
('s.linh02',    '$2a$10$PLACEHOLDER_HASH_S2', 'Học viên', NULL, 2,   NULL, 1),
('staff.linh',  '$2a$10$PLACEHOLDER_HASH_ST', 'Nhân viên',   NULL, NULL, 2,    1);

-- =========================
-- 18) Notifications (optional)
-- =========================
INSERT INTO notifications(title, content, target_role, created_by_user, created_at) VALUES
('Khai giảng tháng 2/2026', 'Các lớp COM/IELTS/TOEIC bắt đầu từ ngày 10–15/02/2026. Vui lòng đến trước 10 phút.', 'All', 1, '2026-02-08 09:00:00'),
('Quy định nghỉ học', 'Nếu vắng mặt, học viên báo trước cho tư vấn viên để được hỗ trợ học bù.', 'Học viên', 1, '2026-02-09 10:00:00');
