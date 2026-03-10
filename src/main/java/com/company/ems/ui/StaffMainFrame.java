package com.company.ems.ui;

import com.company.ems.service.*;
import com.company.ems.ui.components.HeaderPanel;
import com.company.ems.ui.components.SidebarPanel;
import com.company.ems.ui.panels.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Frame dành riêng cho role Nhân viên.
 * Bị giới hạn so với Admin: không có Nhân viên, Điểm danh tổng quan, Tài khoản, Dashboard doanh thu.
 */
public class StaffMainFrame extends JFrame {

    private final HeaderPanel headerPanel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;

    // ── Panel refs ──────────────────────────────────────────────────────
    private final StudentPanel    studentPanel;
    private final TeacherPanel    teacherPanel;
    private final CoursePanel     coursePanel;
    private final ClassPanel      classPanel;
    private final EnrollmentPanel enrollmentPanel;
    private final TuitionPanel    tuitionPanel;
    private final ResultAdminPanel resultPanel;

    // Menu Staff: không có staffs, attendances (tổng quan), accounts, dashboard
    private static final String[][] STAFF_MENU = {
        {"👥",  "Học viên",   "students"},
        {"👨",  "Giáo viên",  "teachers"},
        {"📚",  "Khóa học",   "courses"},
        {"🏫",  "Lớp học",    "classes"},
        {"📋",  "Đăng ký",    "enrollments"},
        {"💰",  "Thanh toán", "payments"},
        {"✎",  "Kết quả",   "results"},
        {"🗓️", "Lịch học",   "schedules"},
        {"🚪",  "Phòng học",  "rooms"},
    };

    private static final Map<String, String[]> PAGE_META = new HashMap<>();
    static {
        PAGE_META.put("students",    new String[]{"Quản lý Học viên",  "Danh sách toàn bộ học viên"});
        PAGE_META.put("teachers",    new String[]{"Quản lý Giáo viên", "Danh sách giảng viên"});
        PAGE_META.put("courses",     new String[]{"Quản lý Khóa học",  "Danh sách chương trình đào tạo"});
        PAGE_META.put("classes",     new String[]{"Quản lý Lớp học",   "Danh sách lớp đang mở"});
        PAGE_META.put("enrollments", new String[]{"Đăng ký học",       "Quản lý đăng ký học viên"});
        PAGE_META.put("payments",    new String[]{"Thanh toán",        "Quản lý thu chi học phí"});
        PAGE_META.put("results",     new String[]{"Kết quả học tập",   "Điểm số và xếp loại học viên"});
        PAGE_META.put("schedules",   new String[]{"Lịch học",          "Thời khóa biểu các lớp"});
        PAGE_META.put("rooms",       new String[]{"Phòng học",         "Quản lý phòng học"});
    }

    public StaffMainFrame(StudentService studentService,
                          TeacherService teacherService,
                          CourseService courseService,
                          RoomService roomService,
                          StaffService staffService,
                          ClassService classService,
                          EnrollmentService enrollmentService,
                          InvoiceService invoiceService,
                          PaymentService paymentService,
                          ScheduleService scheduleService,
                          AttendanceService attendanceService,
                          ResultService resultService,
                          Runnable onLogout) {

        setTitle("Language Center Management System - STAFF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1024, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar với menu giới hạn cho Staff
        SidebarPanel sidebar = new SidebarPanel(STAFF_MENU, this::navigateTo, () -> {
            dispose();
            if (onLogout != null) onLogout.run();
        });
        add(sidebar, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(248, 250, 252));

        headerPanel = new HeaderPanel("Quản lý Học viên", "Danh sách toàn bộ học viên");
        headerPanel.setUserInfo("Nhân viên");
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(248, 250, 252));

        // ── Panels mà Staff được phép truy cập ──────────────────────────
        studentPanel    = new StudentPanel(studentService);
        teacherPanel    = new TeacherPanel(teacherService);
        coursePanel     = new CoursePanel(courseService);
        classPanel      = new ClassPanel(classService, courseService, teacherService, roomService, enrollmentService, invoiceService);
        enrollmentPanel = new EnrollmentPanel(enrollmentService, studentService, classService, invoiceService);
        tuitionPanel    = new TuitionPanel(enrollmentService, invoiceService, paymentService, studentService, true, null);
        resultPanel     = new ResultAdminPanel(resultService, classService, true);

        contentPanel.add(studentPanel,    "students");
        contentPanel.add(teacherPanel,    "teachers");
        contentPanel.add(coursePanel,     "courses");
        contentPanel.add(classPanel,      "classes");
        contentPanel.add(enrollmentPanel, "enrollments");
        contentPanel.add(tuitionPanel,    "payments");
        contentPanel.add(resultPanel,     "results");
        contentPanel.add(new RoomPanel(roomService), "rooms");
        contentPanel.add(new ScheduleManagerPanel(classService, scheduleService, roomService), "schedules");

        Runnable refreshAll = this::refreshAllPanels;
        studentPanel.setOnDataChanged(refreshAll);
        teacherPanel.setOnDataChanged(refreshAll);
        coursePanel.setOnDataChanged(refreshAll);
        classPanel.setOnDataChanged(refreshAll);
        enrollmentPanel.setOnDataChanged(refreshAll);
        tuitionPanel.setOnDataChanged(refreshAll);
        resultPanel.setOnDataChanged(refreshAll);

        javax.swing.Timer autoRefresh = new javax.swing.Timer(30_000, e -> refreshAllPanels());
        autoRefresh.setCoalesce(true);
        autoRefresh.start();

        rightPanel.add(contentPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void navigateTo(String panelName) {
        cardLayout.show(contentPanel, panelName);
        String[] meta = PAGE_META.getOrDefault(panelName, new String[]{panelName, ""});
        headerPanel.setTitle(meta[0], meta[1]);
        refreshAllPanels();
    }

    private void refreshAllPanels() {
        studentPanel.loadData();
        teacherPanel.loadData();
        coursePanel.loadData();
        classPanel.loadData();
        enrollmentPanel.loadData();
        tuitionPanel.loadData();
        resultPanel.loadData();
    }
}
