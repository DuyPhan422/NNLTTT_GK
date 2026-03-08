package com.company.ems;

import com.company.ems.model.Student;
import com.company.ems.model.Teacher;
import com.company.ems.repo.jpa.*;
import com.company.ems.service.*;
import com.company.ems.ui.UI;
import com.company.ems.ui.MainFrame;
import com.company.ems.ui.StudentMainFrame;
import com.company.ems.ui.panels.attendance.AttendanceAdminPanel;
import com.company.ems.ui.panels.attendance.AttendanceStudentPanel;
import com.company.ems.ui.panels.attendance.AttendanceTeacherPanel;

import javax.swing.*;
import java.awt.*;

public class Main {
    // ══════════════════════════════════════════════════════════════════════
    //  🔧 BIẾN TEST — Thay đổi để xem giao diện theo role
    //
    //  Chế độ:
    //    "MAIN"         → chạy toàn bộ ứng dụng (MainFrame) của Admin
    //    "STUDENT_FULL" → chạy toàn bộ ứng dụng (StudentMainFrame) của Học viên
    //    "ADMIN"        → chỉ mở AttendanceAdminPanel   (Admin dashboard)
    //    "TEACHER"      → chỉ mở AttendanceTeacherPanel (Teacher điểm danh)
    //    "STUDENT"      → chỉ mở AttendanceStudentPanel (Student xem chuyên cần)
    // ══════════════════════════════════════════════════════════════════════
    static final String TEST_ROLE   = "ADMIN_FULL";
    static final Long   TEACHER_ID  = null;
    static final Long   STUDENT_ID  = 3L;
    public static void main(String[] args) {
        UI.initLookAndFeel();

        // 1. Khởi tạo toàn bộ Repositories và Services (Dependency Injection)
        StudentService studentService = new StudentService(new JpaStudentRepository());
        TeacherService teacherService = new TeacherService(new JpaTeacherRepository());
        CourseService  courseService  = new CourseService(new JpaCourseRepository());
        RoomService    roomService    = new RoomService(new JpaRoomRepository());
        StaffService   staffService   = new StaffService(new JpaStaffRepository());
        ClassService   classService   = new ClassService(new JpaClassRepository());
        EnrollmentService enrollmentService = new EnrollmentService(new JpaEnrollmentRepository());
        InvoiceService invoiceService = new InvoiceService(new JpaInvoiceRepository());
        PaymentService paymentService = new PaymentService(new JpaPaymentRepository());
        ScheduleService scheduleService = new ScheduleService(new JpaScheduleRepository());
        AttendanceService attendanceService = new AttendanceService(new JpaAttendanceRepository());

        SwingUtilities.invokeLater(() -> {
            switch (TEST_ROLE) {

                case "ADMIN" -> {
                    JFrame frame = buildTestFrame("🛡️ Attendance — Admin View", 1200, 720);
                    frame.add(new AttendanceAdminPanel(attendanceService, classService, studentService));
                    frame.setVisible(true);
                }

                case "TEACHER" -> {
                    Teacher teacher = (TEACHER_ID != null) ? teacherService.findById(TEACHER_ID) : null;
                    JFrame frame = buildTestFrame(
                            "👨‍🏫 Attendance — Teacher View"
                                    + (teacher != null ? " | " + teacher.getFullName() : " | (Tất cả lớp)"),
                            1200, 720);
                    frame.add(new AttendanceTeacherPanel(attendanceService, classService, teacher));
                    frame.setVisible(true);
                }

                case "STUDENT" -> {
                    Student student = (STUDENT_ID != null) ? studentService.findById(STUDENT_ID) : null;
                    JFrame frame = buildTestFrame(
                            "🎓 Attendance — Student View"
                                    + (student != null ? " | " + student.getFullName() : ""),
                            1100, 700);
                    frame.add(new AttendanceStudentPanel(attendanceService, student));
                    frame.setVisible(true);
                }

                case "STUDENT_FULL" -> {
                    // Giao diện Full của học viên (Tính năng của bạn kia)
                    Long testStudentId = (STUDENT_ID != null) ? STUDENT_ID : 3L;
                    StudentMainFrame studentFrame = new StudentMainFrame(
                            studentService, invoiceService, paymentService,
                            enrollmentService, classService, testStudentId
                    );
                    studentFrame.setVisible(true);
                }

                default -> {
                    // "MAIN" → Giao diện Full của Admin
                    // LƯU Ý: Số lượng tham số ở đây phải khớp với hàm khởi tạo MainFrame thực tế
                    MainFrame frame = new MainFrame(
                            studentService, teacherService,
                            courseService, roomService, staffService,
                            classService,
                            enrollmentService,
                            invoiceService,
                            paymentService,
                            scheduleService
                    );
                    frame.setVisible(true);
                }
            }
        });
    }


    /** Tạo JFrame test đơn giản, chuẩn hóa để dễ tái dùng */
    private static JFrame buildTestFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        return frame;
    }
}