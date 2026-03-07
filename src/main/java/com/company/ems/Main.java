package com.company.ems;

import com.company.ems.repo.jpa.*;
import com.company.ems.service.*;
import com.company.ems.ui.UI;
import com.company.ems.ui.MainFrame;
import com.company.ems.ui.StudentMainFrame;

import javax.swing.*;

public class Main {
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
        
        // --- CẦN THÊM 2 DÒNG NÀY ĐỂ FIX LỖI ---
        InvoiceService invoiceService = new InvoiceService(new JpaInvoiceRepository());
        PaymentService paymentService = new PaymentService(new JpaPaymentRepository());

        SwingUtilities.invokeLater(() -> {
            // ✅ true = Admin Frame | false = Student Frame
            boolean isTestAdmin = true;  // ⬅️ ĐỔI THÀNH FALSE ĐỂ TEST STUDENT

            if (isTestAdmin) {
                // ADMIN: Quản lý toàn bộ hệ thống
                MainFrame frame = new MainFrame(
                        studentService, teacherService,
                        courseService, roomService, staffService,
                        classService,
                        enrollmentService,
                        invoiceService, 
                        paymentService  
                );
                frame.setVisible(true);
            } else {
                // STUDENT: Giao diện học viên (Test với Student ID = 3)
                Long testStudentId = 3L; 
                StudentMainFrame studentFrame = new StudentMainFrame(
                        studentService, 
                        invoiceService, 
                        paymentService, 
                        enrollmentService, 
                        classService, 
                        testStudentId
                );
                studentFrame.setVisible(true);
            }
        });
    }
}