-- =========================================================
-- MIS TRUNG TAM NGOAI NGU - MySQL 8+
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
  specialty    VARCHAR(100) NULL, -- IELTS, TOEIC, GiaoTiep...
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
  duration     INT NULL,            -- so gio / so tuan (tuy quy uoc)
  duration_unit ENUM('Giờ','Tuần','Tháng') NULL DEFAULT 'Tuần',
  fee          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  status       ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================
-- 4) OPERATIONS: Room
-- =========================
CREATE TABLE rooms (
  room_id    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  room_name  VARCHAR(100) NOT NULL,
  capacity   INT NOT NULL DEFAULT 0,
  location   VARCHAR(150) NULL,
  status     ENUM('Hoạt động','Không hoạt động') NOT NULL DEFAULT 'Hoạt động',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_rooms_name UNIQUE (room_name)
);

-- =========================
-- 5) ACADEMIC: Class
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
    ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_classes_course   ON classes(course_id);
CREATE INDEX idx_classes_teacher  ON classes(teacher_id);
CREATE INDEX idx_classes_room     ON classes(room_id);
CREATE INDEX idx_classes_dates    ON classes(start_date, end_date);

-- =========================
-- 6) ACADEMIC: Enrollment (Student <-> Class)
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
-- 7) FINANCE: Invoice
-- =========================
CREATE TABLE invoices (
  invoice_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  student_id     BIGINT UNSIGNED NOT NULL,
  total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  issue_date     DATE NOT NULL DEFAULT (CURRENT_DATE),
  status         ENUM('Bản nháp','Chờ thanh toán','Đã thanh toán','Đã hủy') NOT NULL DEFAULT 'Chờ thanh toán',
  note           VARCHAR(255) NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_invoices_student
    FOREIGN KEY (student_id) REFERENCES students(student_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE INDEX idx_invoices_student ON invoices(student_id);
CREATE INDEX idx_invoices_issue   ON invoices(issue_date);

-- =========================
-- 8) FINANCE: Payment
-- (theo file: PaymentID, StudentID, EnrollmentID, Amount, PaymentDate, Method, Status)
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
-- 9) OPERATIONS: Schedule
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
-- 10) OPERATIONS: Attendance
-- (Trong file: AttendanceID, StudentID, ClassID, Date, Status)
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
-- 11) SYSTEM: Staff
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
-- 12) SYSTEM: User Account
-- (Role: Admin/Teacher/Student; RelatedID trỏ 1 trong 3 bảng)
-- MySQL không FK động được, nên dùng 3 FK nullable + CHECK.
-- =========================
-- =========================
-- 12) SYSTEM: User Account (FIX LỖI 3823 CHUẨN XÁC)
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

  -- ĐÃ SỬA: Bỏ hoàn toàn ON UPDATE CASCADE / ON DELETE CASCADE. 
  -- Mặc định MySQL sẽ dùng RESTRICT (An toàn tuyệt đối cho CHECK constraint)
  CONSTRAINT fk_user_accounts_teacher
    FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id),

  CONSTRAINT fk_user_accounts_student
    FOREIGN KEY (student_id) REFERENCES students(student_id),

  CONSTRAINT fk_user_accounts_staff
    FOREIGN KEY (staff_id) REFERENCES staffs(staff_id),

  -- Ràng buộc CHECK này giờ đã an toàn và hợp lệ
  CONSTRAINT chk_user_accounts_related
    CHECK (
      (role='Giáo viên' AND teacher_id IS NOT NULL AND student_id IS NULL AND staff_id IS NULL) OR
      (role='Học viên' AND student_id IS NOT NULL AND teacher_id IS NULL AND staff_id IS NULL) OR
      (role='Nhân viên'   AND staff_id   IS NOT NULL AND teacher_id IS NULL AND student_id IS NULL) OR
      (role='Quản trị'   AND teacher_id IS NULL AND student_id IS NULL AND staff_id IS NULL)
    )
);

-- =========================
-- 13) ACADEMIC: Result
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
-- (OPTIONAL) Một số bảng mở rộng gợi ý trong tài liệu:
-- Branch, Promotion, PlacementTest, Certificate, Notification
-- Bạn có thể bật dùng nếu muốn triển khai bản "đầy đủ".
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

ALTER TABLE rooms
  ADD COLUMN branch_id BIGINT UNSIGNED NULL,
  ADD CONSTRAINT fk_rooms_branch
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE classes
  ADD COLUMN branch_id BIGINT UNSIGNED NULL,
  ADD CONSTRAINT fk_classes_branch
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
    ON UPDATE CASCADE ON DELETE SET NULL;

CREATE TABLE promotions (
  promotion_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  promo_name     VARCHAR(150) NOT NULL,
  discount_type  ENUM('Percent','Amount') NOT NULL DEFAULT 'Percent',
  discount_value DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  start_date     DATE NULL,
  end_date       DATE NULL,
  status         ENUM('Active','Inactive') NOT NULL DEFAULT 'Active'
);

ALTER TABLE invoices
  ADD COLUMN promotion_id BIGINT UNSIGNED NULL,
  ADD CONSTRAINT fk_invoices_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id)
    ON UPDATE CASCADE ON DELETE SET NULL;

CREATE TABLE placement_tests (
  test_id     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  student_id  BIGINT UNSIGNED NOT NULL,
  test_date   DATE NOT NULL DEFAULT (CURRENT_DATE),
  score       DECIMAL(5,2) NULL,
  suggested_level ENUM('Beginner','Intermediate','Advanced') NULL,
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
  target_role     ENUM('All','Student','Teacher','Staff') NOT NULL DEFAULT 'All',
  created_by_user BIGINT UNSIGNED NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user
    FOREIGN KEY (created_by_user) REFERENCES user_accounts(user_id)
    ON UPDATE CASCADE ON DELETE SET NULL
);
