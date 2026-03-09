package com.company.ems;

import com.company.ems.repo.jpa.*;
import com.company.ems.service.*;
import com.company.ems.ui.LoginFrame;
import com.company.ems.ui.UI;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        UI.initLookAndFeel();

        // ── Khởi tạo toàn bộ Repositories & Services (Manual DI) ────────
        StudentService    studentService    = new StudentService(new JpaStudentRepository());
        TeacherService    teacherService    = new TeacherService(new JpaTeacherRepository());
        CourseService     courseService     = new CourseService(new JpaCourseRepository());
        RoomService       roomService       = new RoomService(new JpaRoomRepository());
        StaffService      staffService      = new StaffService(new JpaStaffRepository());
        ClassService      classService      = new ClassService(new JpaClassRepository());
        EnrollmentService enrollmentService = new EnrollmentService(new JpaEnrollmentRepository());
        InvoiceService    invoiceService    = new InvoiceService(new JpaInvoiceRepository());
        PaymentService    paymentService    = new PaymentService(new JpaPaymentRepository());
        ScheduleService   scheduleService   = new ScheduleService(new JpaScheduleRepository());
        AttendanceService attendanceService = new AttendanceService(new JpaAttendanceRepository());
        UserAccountService userAccountService = new UserAccountService(new JpaUserAccountRepository());

        // ── Mở LoginFrame — điểm vào duy nhất của ứng dụng ─────────────
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame(
                    userAccountService,
                    studentService, teacherService, courseService,
                    roomService, staffService, classService,
                    enrollmentService, invoiceService, paymentService,
                    scheduleService, attendanceService
            );
            login.setVisible(true);
        });
    }
}