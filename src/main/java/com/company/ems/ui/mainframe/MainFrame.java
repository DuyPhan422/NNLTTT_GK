package com.company.ems.ui.mainframe;

import com.company.ems.service.*;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.components.HeaderPanel;
import com.company.ems.ui.components.SidebarPanel;
import com.company.ems.ui.panels.admin_staff.*;
import com.company.ems.ui.panels.admin_staff.AttendanceAdminPanel;
import com.company.ems.ui.panels.student.StudentPanel;
import com.company.ems.ui.panels.teacher.TeacherPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final HeaderPanel headerPanel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;

    // ── Tham chiếu đến các panel để có thể refresh ──
    private final StudentPanel studentPanel;
    private final TeacherPanel teacherPanel;
    private final CoursePanel           coursePanel;
    private final ClassPanel            classPanel;
    private final EnrollmentPanel       enrollmentPanel;
    private final TuitionPanel tuitionPanel;
    private final StaffPanel staffPanel;
    private final RevenueDashboardPanel dashboardPanel;
    private final UserAccountPanel      accountPanel;
    private final ResultAdminPanel resultPanel;

    // Menu Admin đầy đủ (bao gồm Dashboard và Tài khoản)
    private static final String[][] ADMIN_MENU = {
        {"📊",  "Tổng quan",   "dashboard"},
        {"👥",  "Học viên",    "students"},
        {"👨",  "Giáo viên",   "teachers"},
        {"🧑",  "Nhân viên",   "staffs"},
        {"📚",  "Khóa học",    "courses"},
        {"🏫",  "Lớp học",     "classes"},
        {"📋",  "Đăng ký",     "enrollments"},
        {"💰",  "Thanh toán",  "payments"},
        {"🗓️", "Lịch học",    "schedules"},
        {"✅",  "Điểm danh",   "attendances"},
        {"✎",  "Kết quả",     "results"},
        {"🚪",  "Phòng học",   "rooms"},
        {"👤",  "Tài khoản",   "accounts"},
    };

    private static final Map<String, String[]> PAGE_META = new HashMap<>();
    static {
        PAGE_META.put("dashboard",   new String[]{"Dashboard Doanh thu",   "Tổng quan doanh thu & giao dịch"});
        PAGE_META.put("students",    new String[]{"Quản lý Học viên",      "Danh sách toàn bộ học viên"});
        PAGE_META.put("teachers",    new String[]{"Quản lý Giáo viên",     "Danh sách giảng viên"});
        PAGE_META.put("courses",     new String[]{"Quản lý Khóa học",      "Danh sách chương trình đào tạo"});
        PAGE_META.put("classes",     new String[]{"Quản lý Lớp học",       "Danh sách lớp đang mở"});
        PAGE_META.put("enrollments", new String[]{"Đăng ký học",           "Quản lý đăng ký học viên"});
        PAGE_META.put("payments",    new String[]{"Thanh toán",            "Quản lý thu chi học phí"});
        PAGE_META.put("schedules",   new String[]{"Lịch học",              "Thời khóa biểu các lớp"});
        PAGE_META.put("attendances", new String[]{"Điểm danh",             "Theo dõi chuyên cần học viên"});
        PAGE_META.put("results",     new String[]{"Kết quả học tập",       "Điểm số và xếp loại toàn trung tâm"});
        PAGE_META.put("rooms",       new String[]{"Phòng học",             "Quản lý phòng học"});
        PAGE_META.put("staffs",      new String[]{"Nhân viên",             "Quản lý nhân sự"});
        PAGE_META.put("accounts",    new String[]{"Quản lý Tài khoản",     "Danh sách tài khoản hệ thống"});
    }

    public MainFrame(StudentService studentService,
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
                     UserAccountService userAccountService,
                     Runnable onLogout) {
        setTitle("Language Center Management System - ADMIN");
        
        // Auto-sync class statuses on startup
        if (classService != null) {
            classService.syncAutoClassStatuses();
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1024, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar Admin — menu đầy đủ
        SidebarPanel sidebar = new SidebarPanel(ADMIN_MENU, this::navigateTo, () -> {
            dispose();
            if (onLogout != null) onLogout.run();
        });
        add(sidebar, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Theme.BG_PAGE);

        headerPanel = new HeaderPanel("Dashboard Doanh thu", "Tổng quan doanh thu & giao dịch");
        headerPanel.setUserInfo("Quản trị");
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BG_PAGE);

        // ── Khởi tạo panels ─────────────────────────────────────────────
        dashboardPanel  = new RevenueDashboardPanel(paymentService, invoiceService);
        studentPanel    = new StudentPanel(studentService);
        teacherPanel    = new TeacherPanel(teacherService);
        coursePanel     = new CoursePanel(courseService);
        classPanel      = new ClassPanel(classService, courseService, teacherService, roomService, enrollmentService, invoiceService);
        enrollmentPanel = new EnrollmentPanel(enrollmentService, studentService, classService, invoiceService);
        tuitionPanel    = new TuitionPanel(enrollmentService, invoiceService, paymentService, studentService, true, null);
        staffPanel      = new StaffPanel(staffService);
        accountPanel    = new UserAccountPanel(userAccountService);
        resultPanel     = new ResultAdminPanel(resultService, classService, false);

        // ── Đăng ký CardLayout ──────────────────────────────────────────
        contentPanel.add(dashboardPanel,  "dashboard");
        contentPanel.add(studentPanel,    "students");
        contentPanel.add(teacherPanel,    "teachers");
        contentPanel.add(coursePanel,     "courses");
        contentPanel.add(classPanel,      "classes");
        contentPanel.add(enrollmentPanel, "enrollments");
        contentPanel.add(tuitionPanel,    "payments");
        contentPanel.add(new RoomPanel(roomService), "rooms");
        contentPanel.add(new ScheduleManagerPanel(classService, courseService, scheduleService, roomService), "schedules");
        contentPanel.add(staffPanel, "staffs");
        contentPanel.add(new AttendanceAdminPanel(attendanceService, classService, studentService), "attendances");
        contentPanel.add(resultPanel, "results");
        contentPanel.add(accountPanel, "accounts");

        // ── Callbacks ───────────────────────────────────────────────────
        Runnable refreshAll = this::refreshAllPanels;
        studentPanel.setOnDataChanged(refreshAll);
        teacherPanel.setOnDataChanged(refreshAll);
        coursePanel.setOnDataChanged(refreshAll);
        classPanel.setOnDataChanged(refreshAll);
        enrollmentPanel.setOnDataChanged(refreshAll);
        tuitionPanel.setOnDataChanged(refreshAll);
        staffPanel.setOnDataChanged(refreshAll);
        resultPanel.setOnDataChanged(refreshAll);

        javax.swing.Timer autoRefresh = new javax.swing.Timer(30_000, e -> refreshAllPanels());
        autoRefresh.setCoalesce(true);
        autoRefresh.start();

        // Hiển thị dashboard khi khởi động
        cardLayout.show(contentPanel, "dashboard");

        rightPanel.add(contentPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void navigateTo(String panelName) {
        cardLayout.show(contentPanel, panelName);
        String[] meta = PAGE_META.getOrDefault(panelName, new String[]{panelName, ""});
        headerPanel.setTitle(meta[0], meta[1]);
        // Refresh panel vừa navigate tới để đảm bảo dữ liệu luôn mới nhất
        refreshAllPanels();
    }

    /** Refresh dữ liệu tất cả panel — được gọi mỗi khi có thay đổi ở bất kỳ panel nào. */
    private void refreshAllPanels() {
        dashboardPanel.loadData();
        studentPanel.loadData();
        teacherPanel.loadData();
        coursePanel.loadData();
        classPanel.loadData();
        enrollmentPanel.loadData();
        tuitionPanel.loadData();
        staffPanel.loadData();
        accountPanel.loadData();
        resultPanel.loadData();
    }

}