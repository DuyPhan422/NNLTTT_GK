-- =========================================================
-- MIS TRUNG TAM NGOAI NGU - BẢN HOÀN THIỆN TIẾNG VIỆT 100%
-- =========================================================

DROP DATABASE IF EXISTS mis_language_center;
CREATE DATABASE mis_language_center
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE mis_language_center;

-- =========================
-- 1) CORE: Student
-- =========================
CREATE TABLE students (
                          student_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          full_name         VARCHAR(150) NOT NULL,
                          date_of_birth     DATE NULL,
                          gender            ENUM('Nam','Nữ','Khác') NULL,
                          phone             VARCHAR(20) NULL,
                          email             VARCHAR(150) NULL,
                          address           VARCHAR(255) NULL,
                          registration_date DATE NOT NULL DEFAULT (CURRENT_DATE),
                          status            ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                          created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT uq_students_email UNIQUE (email),
                          CONSTRAINT uq_students_phone UNIQUE (phone)
);

-- =========================
-- 2) CORE: Teacher
-- =========================
CREATE TABLE teachers (
                          teacher_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          full_name    VARCHAR(150) NOT NULL,
                          phone        VARCHAR(20) NULL,
                          email        VARCHAR(150) NULL,
                          specialty    VARCHAR(100) NULL,
                          hire_date    DATE NULL,
                          status       ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                          created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT uq_teachers_email UNIQUE (email),
                          CONSTRAINT uq_teachers_phone UNIQUE (phone)
);

-- =========================
-- 3) CORE: Course
-- =========================
CREATE TABLE courses (
                         course_id    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                         course_name  VARCHAR(200) NOT NULL,
                         description  TEXT NULL,
                         level        ENUM('Cơ bản','Trung cấp','Nâng cao') NULL,
                         duration     INT NULL,
                         duration_unit ENUM('Giờ','Tuần','Tháng') NULL DEFAULT 'Tuần',
                         fee          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                         status       ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                         created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================
-- 4) MỞ RỘNG: Branch (Đưa lên trước Room/Class để nối FK)
-- =========================
CREATE TABLE branches (
                          branch_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          branch_name VARCHAR(150) NOT NULL,
                          address     VARCHAR(255) NULL,
                          phone       VARCHAR(20) NULL,
                          status      ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                          created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT uq_branches_name UNIQUE (branch_name)
);

-- =========================
-- 5) OPERATIONS: Room
-- =========================
CREATE TABLE rooms (
                       room_id    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                       room_name  VARCHAR(100) NOT NULL,
                       capacity   INT NOT NULL DEFAULT 0,
                       location   VARCHAR(150) NULL,
                       status     ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                       branch_id  BIGINT UNSIGNED NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT uq_rooms_name UNIQUE (room_name),
                       CONSTRAINT fk_rooms_branch
                           FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
                               ON UPDATE CASCADE ON DELETE SET NULL
);

-- =========================
-- 6) ACADEMIC: Class
-- =========================
CREATE TABLE classes (
                         class_id      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                         class_name    VARCHAR(150) NOT NULL,
                         course_id     BIGINT UNSIGNED NOT NULL,
                         teacher_id    BIGINT UNSIGNED NULL,
                         start_date    DATE NOT NULL,
                         end_date      DATE NULL,
                         max_student   INT NOT NULL DEFAULT 0,
                         room_id       BIGINT UNSIGNED NULL,
                         branch_id     BIGINT UNSIGNED NULL,
                         status        ENUM('Lên kế hoạch','Mở lớp','Đang diễn ra','Hoàn thành','Hủy lớp') NOT NULL DEFAULT 'Lên kế hoạch',
                         created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT fk_classes_course
                             FOREIGN KEY (course_id) REFERENCES courses(course_id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,

                         CONSTRAINT fk_classes_teacher
                             FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)
                                 ON UPDATE CASCADE ON DELETE SET NULL,

                         CONSTRAINT fk_classes_room
                             FOREIGN KEY (room_id) REFERENCES rooms(room_id)
                                 ON UPDATE CASCADE ON DELETE SET NULL,

                         CONSTRAINT fk_classes_branch
                             FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
                                 ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_classes_course   ON classes(course_id);
CREATE INDEX idx_classes_teacher  ON classes(teacher_id);
CREATE INDEX idx_classes_room     ON classes(room_id);
CREATE INDEX idx_classes_dates    ON classes(start_date, end_date);

-- =========================
-- 7) ACADEMIC: Enrollment
-- =========================
CREATE TABLE enrollments (
                             enrollment_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             student_id      BIGINT UNSIGNED NOT NULL,
                             class_id        BIGINT UNSIGNED NOT NULL,
                             enrollment_date DATE NOT NULL DEFAULT (CURRENT_DATE),
                             status          ENUM('Đã đăng ký','Đã hủy','Đã thanh toán') NOT NULL DEFAULT 'Đã đăng ký',
                             result          ENUM('Đạt','Không đạt','Chưa có') NOT NULL DEFAULT 'Chưa có',
                             created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT fk_enrollments_student
                                 FOREIGN KEY (student_id) REFERENCES students(student_id)
                                     ON UPDATE CASCADE ON DELETE RESTRICT,

                             CONSTRAINT fk_enrollments_class
                                 FOREIGN KEY (class_id) REFERENCES classes(class_id)
                                     ON UPDATE CASCADE ON DELETE RESTRICT,

                             CONSTRAINT uq_enrollments_student_class UNIQUE (student_id, class_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_class   ON enrollments(class_id);

-- =========================
-- 8) MỞ RỘNG: Promotions
-- =========================
CREATE TABLE promotions (
                            promotion_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                            promo_name     VARCHAR(150) NOT NULL,
                            discount_type  ENUM('Percent','Amount') NOT NULL DEFAULT 'Percent',
                            discount_value DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                            start_date     DATE NULL,
                            end_date       DATE NULL,
                            status         ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động'
);

-- =========================
-- 9) FINANCE: Invoice
-- =========================
CREATE TABLE invoices (
                          invoice_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          student_id     BIGINT UNSIGNED NOT NULL,
                          total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                          issue_date     DATE NOT NULL DEFAULT (CURRENT_DATE),
                          status         ENUM('Bản nháp','Chờ thanh toán','Đã thanh toán','Đã hủy') NOT NULL DEFAULT 'Chờ thanh toán',
                          note           VARCHAR(255) NULL,
                          promotion_id   BIGINT UNSIGNED NULL,
                          created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT fk_invoices_student
                              FOREIGN KEY (student_id) REFERENCES students(student_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,

                          CONSTRAINT fk_invoices_promotion
                              FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id)
                                  ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_invoices_student ON invoices(student_id);
CREATE INDEX idx_invoices_issue   ON invoices(issue_date);

-- =========================
-- 10) FINANCE: Payment
-- =========================
CREATE TABLE payments (
                          payment_id      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          student_id      BIGINT UNSIGNED NOT NULL,
                          enrollment_id   BIGINT UNSIGNED NULL,
                          invoice_id      BIGINT UNSIGNED NULL,
                          amount          DECIMAL(15,2) NOT NULL,
                          payment_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          payment_method  ENUM('Tiền mặt','Chuyển khoản','Momo','ZaloPay','Thẻ ngân hàng','Khác') NOT NULL DEFAULT 'Tiền mặt',
                          status          ENUM('Chờ xử lý','Hoàn thành','Thất bại','Đã hoàn tiền') NOT NULL DEFAULT 'Hoàn thành',
                          reference_code  VARCHAR(100) NULL,
                          created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_payments_student
                              FOREIGN KEY (student_id) REFERENCES students(student_id)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,

                          CONSTRAINT fk_payments_enrollment
                              FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
                                  ON UPDATE CASCADE ON DELETE SET NULL,

                          CONSTRAINT fk_payments_invoice
                              FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
                                  ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_payments_student    ON payments(student_id);
CREATE INDEX idx_payments_enrollment ON payments(enrollment_id);
CREATE INDEX idx_payments_invoice    ON payments(invoice_id);
CREATE INDEX idx_payments_date       ON payments(payment_date);

-- =========================
-- 11) OPERATIONS: Schedule
-- =========================
CREATE TABLE schedules (
                           schedule_id  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                           class_id     BIGINT UNSIGNED NOT NULL,
                           study_date   DATE NOT NULL,
                           start_time   TIME NOT NULL,
                           end_time     TIME NOT NULL,
                           room_id      BIGINT UNSIGNED NULL,
                           created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_schedules_class
                               FOREIGN KEY (class_id) REFERENCES classes(class_id)
                                   ON UPDATE CASCADE ON DELETE CASCADE,

                           CONSTRAINT fk_schedules_room
                               FOREIGN KEY (room_id) REFERENCES rooms(room_id)
                                   ON UPDATE CASCADE ON DELETE SET NULL,

                           CONSTRAINT uq_schedules_class_time UNIQUE (class_id, study_date, start_time, end_time)
);

CREATE INDEX idx_schedules_class_date ON schedules(class_id, study_date);

-- =========================
-- 12) OPERATIONS: Attendance
-- =========================
CREATE TABLE attendances (
                             attendance_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             student_id    BIGINT UNSIGNED NOT NULL,
                             class_id      BIGINT UNSIGNED NOT NULL,
                             attend_date   DATE NOT NULL,
                             status        ENUM('Có mặt','Vắng','Đi trễ') NOT NULL DEFAULT 'Có mặt',
                             note          VARCHAR(255) NULL,
                             created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_attendances_student
                                 FOREIGN KEY (student_id) REFERENCES students(student_id)
                                     ON UPDATE CASCADE ON DELETE RESTRICT,

                             CONSTRAINT fk_attendances_class
                                 FOREIGN KEY (class_id) REFERENCES classes(class_id)
                                     ON UPDATE CASCADE ON DELETE CASCADE,

                             CONSTRAINT uq_attendances UNIQUE (student_id, class_id, attend_date)
);

CREATE INDEX idx_attendances_class_date   ON attendances(class_id, attend_date);
CREATE INDEX idx_attendances_student_date ON attendances(student_id, attend_date);

-- =========================
-- 13) SYSTEM: Staff
-- =========================
CREATE TABLE staffs (
                        staff_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                        full_name  VARCHAR(150) NOT NULL,
                        role       ENUM('Quản trị','Tư vấn','Kế toán','Quản lý','Khác') NOT NULL DEFAULT 'Khác',
                        phone      VARCHAR(20) NULL,
                        email      VARCHAR(150) NULL,
                        status     ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uq_staffs_email UNIQUE (email),
                        CONSTRAINT uq_staffs_phone UNIQUE (phone)
);

-- =========================
-- 14) SYSTEM: User Account
-- =========================
CREATE TABLE user_accounts (
                               user_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               username       VARCHAR(80) NOT NULL,
                               password_hash  VARCHAR(255) NOT NULL,
                               role           ENUM('Quản trị','Giáo viên','Học viên','Nhân viên') NOT NULL,
                               teacher_id     BIGINT UNSIGNED NULL,
                               student_id     BIGINT UNSIGNED NULL,
                               staff_id       BIGINT UNSIGNED NULL,
                               is_active      TINYINT(1) NOT NULL DEFAULT 1,
                               created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               CONSTRAINT uq_user_accounts_username UNIQUE (username),

                               CONSTRAINT fk_user_accounts_teacher
                                   FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id),
                               CONSTRAINT fk_user_accounts_student
                                   FOREIGN KEY (student_id) REFERENCES students(student_id),
                               CONSTRAINT fk_user_accounts_staff
                                   FOREIGN KEY (staff_id) REFERENCES staffs(staff_id),

                               CONSTRAINT chk_user_accounts_related
                                   CHECK (
                                       (role='Giáo viên' AND teacher_id IS NOT NULL AND student_id IS NULL AND staff_id IS NULL) OR
                                       (role='Học viên' AND student_id IS NOT NULL AND teacher_id IS NULL AND staff_id IS NULL) OR
                                       (role='Nhân viên'   AND staff_id   IS NOT NULL AND teacher_id IS NULL AND student_id IS NULL) OR
                                       (role='Quản trị'   AND teacher_id IS NULL AND student_id IS NULL AND staff_id IS NULL)
                                       )
);

-- =========================
-- 15) ACADEMIC: Result
-- =========================
CREATE TABLE results (
                         result_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                         student_id  BIGINT UNSIGNED NOT NULL,
                         class_id    BIGINT UNSIGNED NOT NULL,
                         score       DECIMAL(5,2) NULL,
                         grade       VARCHAR(10) NULL,
                         comment     VARCHAR(255) NULL,
                         created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT fk_results_student
                             FOREIGN KEY (student_id) REFERENCES students(student_id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,

                         CONSTRAINT fk_results_class
                             FOREIGN KEY (class_id) REFERENCES classes(class_id)
                                 ON UPDATE CASCADE ON DELETE CASCADE,

                         CONSTRAINT uq_results UNIQUE (student_id, class_id)
);

CREATE INDEX idx_results_class ON results(class_id);

-- =========================
-- 16) Các bảng mở rộng khác
-- =========================
CREATE TABLE placement_tests (
                                 test_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                 student_id  BIGINT UNSIGNED NOT NULL,
                                 test_date   DATE NOT NULL DEFAULT (CURRENT_DATE),
                                 score       DECIMAL(5,2) NULL,
                                 suggested_level ENUM('Cơ bản','Trung cấp','Nâng cao') NULL,
                                 note        VARCHAR(255) NULL,
                                 CONSTRAINT fk_placement_student
                                     FOREIGN KEY (student_id) REFERENCES students(student_id)
                                         ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE certificates (
                              certificate_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                              student_id     BIGINT UNSIGNED NOT NULL,
                              class_id       BIGINT UNSIGNED NULL,
                              cert_name      VARCHAR(150) NOT NULL,
                              issue_date     DATE NOT NULL DEFAULT (CURRENT_DATE),
                              serial_no      VARCHAR(80) NULL,
                              CONSTRAINT fk_cert_student
                                  FOREIGN KEY (student_id) REFERENCES students(student_id)
                                      ON UPDATE CASCADE ON DELETE CASCADE,
                              CONSTRAINT fk_cert_class
                                  FOREIGN KEY (class_id) REFERENCES classes(class_id)
                                      ON UPDATE CASCADE ON DELETE SET NULL,
                              CONSTRAINT uq_cert_serial UNIQUE (serial_no)
);

CREATE TABLE notifications (
                               notification_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               title           VARCHAR(200) NOT NULL,
                               content         TEXT NOT NULL,
                               target_role     ENUM('Tất cả','Học viên','Giáo viên','Nhân viên') NOT NULL DEFAULT 'Tất cả',
                               created_by_user BIGINT UNSIGNED NULL,
                               created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_notifications_user
                                   FOREIGN KEY (created_by_user) REFERENCES user_accounts(user_id)
                                       ON UPDATE CASCADE ON DELETE SET NULL
);

-- =========================================================
-- BƯỚC INSERT DỮ LIỆU MẪU (MOCK DATA)
-- =========================================================

INSERT INTO branches(branch_name, address, phone, status) VALUES
                                                              ('Cơ sở Quận 1', '12 Nguyễn Huệ, Q.1, TP.HCM', '0909000001', 'Hoạt động'),
                                                              ('Cơ sở Thủ Đức', '88 Võ Văn Ngân, TP.Thủ Đức, TP.HCM', '0909000002', 'Hoạt động');

INSERT INTO rooms(room_name, capacity, location, status, branch_id) VALUES
                                                                        ('P101', 25, 'Tầng 1', 'Hoạt động', 1),
                                                                        ('P202', 30, 'Tầng 2', 'Hoạt động', 1),
                                                                        ('TD-01', 22, 'Khu A', 'Hoạt động', 2),
                                                                        ('TD-02', 28, 'Khu B', 'Hoạt động', 2);

INSERT INTO courses(course_name, description, level, duration, duration_unit, fee, status) VALUES
                                                                                               ('English Communication A1', 'Giao tiếp cơ bản A1, luyện phản xạ', 'Cơ bản', 8, 'Tuần', 2500000, 'Hoạt động'),
                                                                                               ('IELTS Foundation', 'Nền tảng IELTS: từ vựng + ngữ pháp + kỹ năng', 'Trung cấp', 10, 'Tuần', 4500000, 'Hoạt động'),
                                                                                               ('TOEIC 650+', 'Luyện đề TOEIC mục tiêu 650+', 'Trung cấp', 8, 'Tuần', 3200000, 'Hoạt động');

INSERT INTO teachers(full_name, phone, email, specialty, hire_date, status) VALUES
                                                                                ('Nguyễn Minh Anh', '0911000001', 'minhanh.teacher@center.vn', 'GiaoTiep', '2024-06-15', 'Hoạt động'),
                                                                                ('Trần Quốc Huy',  '0911000002', 'quochuy.teacher@center.vn',  'IELTS',    '2023-10-01', 'Hoạt động'),
                                                                                ('Lê Thu Trang',   '0911000003', 'thutrang.teacher@center.vn', 'TOEIC',    '2024-02-20', 'Hoạt động');

INSERT INTO staffs(full_name, role, phone, email, status) VALUES
                                                              ('Phạm Hoài Nam', 'Quản lý',     '0922000001', 'nam.manager@center.vn', 'Hoạt động'),
                                                              ('Võ Ngọc Linh',  'Tư vấn',  '0922000002', 'linh.consult@center.vn','Hoạt động'),
                                                              ('Đặng Hải Yến',  'Kế toán',  '0922000003', 'yen.acc@center.vn',     'Hoạt động');

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

INSERT INTO classes(class_name, course_id, teacher_id, start_date, end_date, max_student, room_id, status, branch_id) VALUES
                                                                                                                          ('COM-A1-0201', 1, 1, '2026-02-10', '2026-04-05', 25, 1, 'Đang diễn ra', 1),
                                                                                                                          ('IELTS-F-0201',2, 2, '2026-02-12', '2026-04-20', 30, 2, 'Đang diễn ra', 1),
                                                                                                                          ('TOEIC-650-0201',3,3,'2026-02-15', '2026-04-10', 28, 4, 'Đang diễn ra', 2),
                                                                                                                          ('COM-A1-0301', 1, 1, '2026-03-10', '2026-05-05', 25, 3, 'Mở lớp', 2),
                                                                                                                          ('IELTS-F-0301', 2, 2, '2026-03-12', '2026-05-20', 30, 1, 'Mở lớp', 1),
                                                                                                                          ('TOEIC-650-0301', 3, 3, '2026-03-15', '2026-05-10', 28, 2, 'Mở lớp', 1);

INSERT INTO schedules(class_id, study_date, start_time, end_time, room_id) VALUES
                                                                               (1, '2026-02-10', '18:30:00', '20:30:00', 1),
                                                                               (1, '2026-02-12', '18:30:00', '20:30:00', 1),
                                                                               (1, '2026-02-17', '18:30:00', '20:30:00', 1),
                                                                               (1, '2026-02-19', '18:30:00', '20:30:00', 1),
                                                                               (2, '2026-02-12', '19:00:00', '21:00:00', 2),
                                                                               (2, '2026-02-16', '19:00:00', '21:00:00', 2),
                                                                               (2, '2026-02-18', '19:00:00', '21:00:00', 2),
                                                                               (2, '2026-02-23', '19:00:00', '21:00:00', 2),
                                                                               (3, '2026-02-15', '08:00:00', '10:00:00', 4),
                                                                               (3, '2026-02-22', '08:00:00', '10:00:00', 4),
                                                                               (3, '2026-03-01', '08:00:00', '10:00:00', 4),
                                                                               (3, '2026-03-08', '08:00:00', '10:00:00', 4);

INSERT INTO enrollments(student_id, class_id, enrollment_date, status, result) VALUES
                                                                                   (1, 1, '2026-02-01', 'Đã đăng ký', 'Chưa có'),
                                                                                   (2, 1, '2026-02-01', 'Đã đăng ký', 'Chưa có'),
                                                                                   (3, 1, '2026-02-02', 'Đã đăng ký', 'Chưa có'),
                                                                                   (4, 1, '2026-02-02', 'Đã đăng ký', 'Chưa có'),
                                                                                   (5, 1, '2026-02-03', 'Đã đăng ký', 'Chưa có'),
                                                                                   (6, 2, '2026-02-03', 'Đã đăng ký', 'Chưa có'),
                                                                                   (7, 2, '2026-02-04', 'Đã đăng ký', 'Chưa có'),
                                                                                   (8, 2, '2026-02-05', 'Đã đăng ký', 'Chưa có'),
                                                                                   (9, 2, '2026-02-05', 'Đã đăng ký', 'Chưa có'),
                                                                                   (10, 3, '2026-02-06', 'Đã đăng ký', 'Chưa có'),
                                                                                   (3,  3, '2026-02-06', 'Đã đăng ký', 'Chưa có'),
                                                                                   (7,  3, '2026-02-07', 'Đã đăng ký', 'Chưa có');

INSERT INTO promotions(promo_name, discount_type, discount_value, start_date, end_date, status) VALUES
                                                                                                    ('Tết 2026 -10%', 'Percent', 10.00, '2026-01-01', '2026-02-28', 'Hoạt động'),
                                                                                                    ('Giảm 200k',     'Amount',  200000, '2026-02-01', '2026-03-31', 'Hoạt động');

INSERT INTO invoices(student_id, total_amount, issue_date, status, note, promotion_id) VALUES
                                                                                           (1, 2500000, '2026-02-01', 'Đã thanh toán', 'Học phí lớp COM-A1-0201', 1),
                                                                                           (2, 2500000, '2026-02-01', 'Đã thanh toán', 'Học phí lớp COM-A1-0201', 1),
                                                                                           (3, 2500000, '2026-02-02', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', NULL),
                                                                                           (4, 2500000, '2026-02-02', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', NULL),
                                                                                           (5, 2500000, '2026-02-03', 'Chờ thanh toán', 'Học phí lớp COM-A1-0201', 2),
                                                                                           (6, 4500000, '2026-02-03', 'Đã thanh toán', 'Học phí lớp IELTS-F-0201', 1),
                                                                                           (7, 4500000, '2026-02-04', 'Đã thanh toán', 'Học phí lớp IELTS-F-0201', NULL),
                                                                                           (8, 4500000, '2026-02-05', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', 2),
                                                                                           (9, 4500000, '2026-02-05', 'Chờ thanh toán', 'Học phí lớp IELTS-F-0201', NULL),
                                                                                           (10,3200000, '2026-02-06', 'Đã thanh toán', 'Học phí lớp TOEIC-650-0201', 2);

INSERT INTO payments(student_id, enrollment_id, invoice_id, amount, payment_date, payment_method, status, reference_code) VALUES
                                                                                                                              (1, 1, 1, 2500000, '2026-02-01 10:15:00', 'Chuyển khoản', 'Hoàn thành', 'VCB-0001'),
                                                                                                                              (2, 2, 2, 1500000, '2026-02-01 11:00:00', 'Tiền mặt', 'Hoàn thành', 'CASH-0002-A'),
                                                                                                                              (2, 2, 2, 1000000, '2026-02-05 16:30:00', 'Momo', 'Hoàn thành', 'MOMO-0002-B'),
                                                                                                                              (6, 6, 6, 4500000, '2026-02-03 09:20:00', 'Chuyển khoản', 'Hoàn thành', 'ACB-0006'),
                                                                                                                              (7, 7, 7, 2500000, '2026-02-04 14:00:00', 'Thẻ ngân hàng', 'Hoàn thành', 'VISA-0007-A'),
                                                                                                                              (7, 7, 7, 2000000, '2026-02-10 14:10:00', 'ZaloPay', 'Hoàn thành', 'ZALO-0007-B'),
                                                                                                                              (10, 10, 10, 3200000, '2026-02-06 18:00:00', 'Tiền mặt', 'Hoàn thành', 'CASH-0010');

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
                                                                             (5,1,'2026-02-12','Đi trễ','Kẹt xe'),
                                                                             (6,2,'2026-02-12','Có mặt',NULL),
                                                                             (7,2,'2026-02-12','Có mặt',NULL),
                                                                             (8,2,'2026-02-12','Vắng','Ốm'),
                                                                             (9,2,'2026-02-12','Có mặt',NULL),
                                                                             (10,3,'2026-02-15','Có mặt',NULL),
                                                                             (3, 3,'2026-02-15','Có mặt',NULL),
                                                                             (7, 3,'2026-02-15','Đi trễ','Đến trễ 5 phút');

INSERT INTO results(student_id, class_id, score, grade, comment) VALUES
                                                                     (1, 1, 86.50, 'A',  'Phản xạ tốt, phát âm rõ'),
                                                                     (2, 1, 78.00, 'B+', 'Tiến bộ rõ rệt sau 2 tuần'),
                                                                     (6, 2, 80.50, 'B+', 'Writing cần luyện thêm cấu trúc'),
                                                                     (10,3, 75.00, 'B',  'Nghe Part 3-4 cần cải thiện');

INSERT INTO placement_tests(student_id, test_date, score, suggested_level, note) VALUES
                                                                                     (4, '2026-01-20', 45.00, 'Cơ bản', 'Nên học A1 trước'),
                                                                                     (8, '2026-01-22', 62.50, 'Trung cấp', 'Phù hợp IELTS Foundation');

INSERT INTO certificates(student_id, class_id, cert_name, issue_date, serial_no) VALUES
    (1, 1, 'Certificate of Completion - Communication A1', '2026-04-06', 'CERT-COM-A1-0001');

INSERT INTO user_accounts(username, password_hash, role, teacher_id, student_id, staff_id, is_active) VALUES
                                                                                                          ('admin',  '$2a$12$wWEuaITc2pwyxTB08SO.uO9FUVcQ2Ub93RKC7TQ6mVDPoQ0GJs0pu', 'Quản trị',   NULL, NULL, NULL, 1),
                                                                                                          ('t.minhanh', '$2a$12$Z8a84Yk.aB/zOxoBZgeWxOcrgNTszpQg8ESRduJnDwJnf.PXkEIsa', 'Giáo viên', 1,    NULL, NULL, 1),
                                                                                                          ('t.quochuy',  '$2a$12$Z8a84Yk.aB/zOxoBZgeWxOcrgNTszpQg8ESRduJnDwJnf.PXkEIsa', 'Giáo viên', 2,    NULL, NULL, 1),
                                                                                                          ('t.thutrang', '$2a$12$Z8a84Yk.aB/zOxoBZgeWxOcrgNTszpQg8ESRduJnDwJnf.PXkEIsa', 'Giáo viên', 3,    NULL, NULL, 1),
                                                                                                          ('s.baopg01',   '$2a$12$jmwXL7JB/sAH/p04/mEXguSrj7kOlt3GSH9/Df9tm6RmTsQp4Ppce', 'Học viên', NULL, 1,   NULL, 1),
                                                                                                          ('s.linh02',    '$2a$12$jmwXL7JB/sAH/p04/mEXguSrj7kOlt3GSH9/Df9tm6RmTsQp4Ppce', 'Học viên', NULL, 2,   NULL, 1),
                                                                                                          ('staff.linh',  '$2a$12$/JFq89QglrtOIxdPq/tyB.1oCSaYRSDwaj5WgP4j3E12msw1Riz/u', 'Nhân viên',   NULL, NULL, 2,    1);

INSERT INTO notifications(title, content, target_role, created_by_user, created_at) VALUES
                                                                                        ('Khai giảng tháng 2/2026', 'Các lớp COM/IELTS/TOEIC bắt đầu từ ngày 10–15/02/2026.', 'Tất cả', 1, '2026-02-08 09:00:00'),
                                                                                        ('Quy định nghỉ học', 'Nếu vắng mặt, báo trước để được học bù.', 'Học viên', 1, '2026-02-09 10:00:00');