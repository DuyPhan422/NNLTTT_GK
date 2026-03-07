package com.company.ems.ui;

import com.company.ems.service.*;
import com.company.ems.ui.components.HeaderPanel;
import com.company.ems.ui.components.SidebarPanel;
import com.company.ems.ui.panels.*; // Import các panel từ package panels

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final HeaderPanel headerPanel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;

    // ── Tham chiếu đến các panel để có thể refresh ──
    private StudentPanel    studentPanel;
    private TeacherPanel    teacherPanel;
    private CoursePanel     coursePanel;
    private ClassPanel      classPanel;
    private EnrollmentPanel enrollmentPanel;
    private TuitionPanel    tuitionPanel;

    private static final Map<String, String[]> PAGE_META = new HashMap<>();
    static {
        PAGE_META.put("students",    new String[]{"Quản lý Học viên",  "Danh sách toàn bộ học viên"});
        PAGE_META.put("teachers",    new String[]{"Quản lý Giáo viên", "Danh sách giảng viên"});
        PAGE_META.put("courses",     new String[]{"Quản lý Khóa học",  "Danh sách chương trình đào tạo"});
        PAGE_META.put("classes",     new String[]{"Quản lý Lớp học",   "Danh sách lớp đang mở"});
        PAGE_META.put("enrollments", new String[]{"Đăng ký học",       "Quản lý đăng ký học viên"});
        PAGE_META.put("payments",    new String[]{"Thanh toán",        "Quản lý thu chi học phí"});
        PAGE_META.put("schedules",   new String[]{"Lịch học",          "Thời khóa biểu các lớp"});
        PAGE_META.put("attendances", new String[]{"Điểm danh",         "Theo dõi chuyên cần học viên"});
        PAGE_META.put("rooms",       new String[]{"Phòng học",         "Quản lý phòng học"});
        PAGE_META.put("staffs",      new String[]{"Nhân viên",         "Quản lý nhân sự"});
    }

    public MainFrame(StudentService studentService,
                     TeacherService teacherService,
                     CourseService courseService,
                     RoomService roomService,
                     StaffService staffService,
                     ClassService classService,
                     EnrollmentService enrollmentService,
                     InvoiceService invoiceService, // Thêm để tự động tạo hóa đơn
                     PaymentService paymentService) { // Thêm để quản lý thanh toán

        setTitle("Language Center Management System - ADMIN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1024, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar điều hướng
        SidebarPanel sidebar = new SidebarPanel(this::navigateTo);
        add(sidebar, BorderLayout.WEST);

        // Vùng nội dung bên phải
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(248, 250, 252));

        headerPanel = new HeaderPanel("Quản lý Học viên", "Danh sách toàn bộ học viên");
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(248, 250, 252));

        // ============================================
        // ĐĂNG KÝ CÁC PANEL CHÍNH
        // ============================================
        
        // 1. Quản lý cơ bản
        studentPanel    = new StudentPanel(studentService);
        teacherPanel    = new TeacherPanel(teacherService);
        coursePanel     = new CoursePanel(courseService);
        classPanel      = new ClassPanel(classService, courseService, teacherService, roomService, enrollmentService, invoiceService);
        contentPanel.add(studentPanel, "students");
        contentPanel.add(teacherPanel, "teachers");
        contentPanel.add(coursePanel,  "courses");
        contentPanel.add(classPanel,   "classes");
        
        // 2. ✨ Ghi danh học viên (Tự động sync Invoice)
        enrollmentPanel = new EnrollmentPanel(enrollmentService, studentService, classService, invoiceService);
        contentPanel.add(enrollmentPanel, "enrollments");
        
        // 3. ✨ Thu học phí & In biên lai
        tuitionPanel = new TuitionPanel(enrollmentService, invoiceService, paymentService, studentService, true, null);
        contentPanel.add(tuitionPanel, "payments");

        // 4. Kết nối callback refresh toàn bộ sau khi tạo xong tất cả panel
        Runnable refreshAll = this::refreshAllPanels;
        studentPanel.setOnDataChanged(refreshAll);
        teacherPanel.setOnDataChanged(refreshAll);
        coursePanel.setOnDataChanged(refreshAll);
        classPanel.setOnDataChanged(refreshAll);
        enrollmentPanel.setOnDataChanged(refreshAll);
        tuitionPanel.setOnDataChanged(refreshAll);

        // 5. Auto-refresh mỗi 30 giây — đảm bảo dữ liệu luôn mới kể cả khi không thao tác
        javax.swing.Timer autoRefresh = new javax.swing.Timer(30_000, e -> refreshAllPanels());
        autoRefresh.setCoalesce(true);
        autoRefresh.start();

        // 4. Placeholder cho các chức năng còn lại
        java.util.Set<String> handled = java.util.Set.of("students", "teachers", "courses", "classes", "enrollments", "payments");
        PAGE_META.forEach((key, meta) -> {
            if (!handled.contains(key)) contentPanel.add(buildPlaceholder(meta[0]), key);
        });

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
        if (studentPanel    != null) studentPanel.loadData();
        if (teacherPanel    != null) teacherPanel.loadData();
        if (coursePanel     != null) coursePanel.loadData();
        if (classPanel      != null) classPanel.loadData();
        if (enrollmentPanel != null) enrollmentPanel.loadData();
        if (tuitionPanel    != null) tuitionPanel.loadData();
    }

    private JPanel buildPlaceholder(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(248, 250, 252));
        JLabel lbl = new JLabel("🚧  " + title + " — Đang phát triển");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lbl.setForeground(new Color(100, 116, 139));
        p.add(lbl);
        return p;
    }
}