package com.company.ems.ui;

import com.company.ems.service.*;
import com.company.ems.ui.components.HeaderPanel;
import com.company.ems.ui.components.SidebarPanel;
import com.company.ems.ui.panels.StudentPanel;
import com.company.ems.ui.panels.TeacherPanel;
import com.company.ems.ui.panels.CoursePanel;
import com.company.ems.ui.panels.ClassPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final HeaderPanel headerPanel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;

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

    // ── Constructor nhận dependencies từ ngoài (DI) ──
    public MainFrame(StudentService studentService,
                     TeacherService teacherService,
                     CourseService courseService,
                     RoomService roomService,
                     StaffService staffService,
                     ClassService classService) {

        setTitle("Language Center Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1024, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar
        SidebarPanel sidebar = new SidebarPanel(this::navigateTo);
        add(sidebar, BorderLayout.WEST);

        // Vùng bên phải = Header + Content
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(248, 250, 252));

        headerPanel = new HeaderPanel("Quản lý Học viên", "Danh sách toàn bộ học viên");
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(248, 250, 252));

        // Đăng ký các panel — truyền service đã được inject vào
        contentPanel.add(new StudentPanel(studentService), "students");
        contentPanel.add(new TeacherPanel(teacherService), "teachers");
        contentPanel.add(new CoursePanel(courseService), "courses");
        contentPanel.add(new ClassPanel(classService, courseService, teacherService, roomService), "classes");

        // Placeholder cho các panel chưa làm
        for (String key : PAGE_META.keySet()) {
            if (!key.equals("students")
                    && !key.equals("teachers")
                    && !key.equals("courses")
                    && !key.equals("classes")) {
                contentPanel.add(buildPlaceholder(PAGE_META.get(key)[0]), key);
            }
        }

        rightPanel.add(contentPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void navigateTo(String panelName) {
        cardLayout.show(contentPanel, panelName);
        String[] meta = PAGE_META.getOrDefault(panelName, new String[]{panelName, ""});
        headerPanel.setTitle(meta[0], meta[1]);
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
