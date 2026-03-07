package com.company.ems.ui;

import com.company.ems.model.Student;
import com.company.ems.service.*;
import com.company.ems.ui.panels.student.StudentClassPanel;
import com.company.ems.ui.panels.student.StudentTuitionPanel;

import javax.swing.*;
import java.awt.*;

public class StudentMainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel headerTitle;

    private final StudentService studentService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final EnrollmentService enrollmentService;
    private final ClassService classService;
    private final Long loggedInStudentId;

    // ✅ Giữ reference để gọi loadData() khi switch tab
    private StudentClassPanel classPanel;
    private StudentTuitionPanel tuitionPanel;

    public StudentMainFrame(StudentService studentService, 
                            InvoiceService invoiceService, 
                            PaymentService paymentService, 
                            EnrollmentService enrollmentService, 
                            ClassService classService,
                            Long loggedInStudentId) {
        this.studentService = studentService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.enrollmentService = enrollmentService;
        this.classService = classService;
        this.loggedInStudentId = loggedInStudentId;

        // Cấu hình cửa sổ (Đồng bộ kích thước với Admin)
        setTitle("EMS - Cổng Học Viên");
        setSize(1280, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 2. Main Content (Khu vực nội dung bên phải)
        JPanel mainArea = new JPanel(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        headerTitle = new JLabel("Hồ sơ Cá nhân");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerTitle.setForeground(new Color(15, 23, 42));
        headerPanel.add(headerTitle);
        mainArea.add(headerPanel, BorderLayout.NORTH);

        // Content panel dùng CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(248, 250, 252));

        // Tab Hồ sơ
        contentPanel.add(buildProfilePanel(studentService.findById(loggedInStudentId)), "profile");
        
        // ✅ Tab Lớp học - gán vào field
        classPanel = new StudentClassPanel(
                enrollmentService, classService, studentService, invoiceService, loggedInStudentId);
        contentPanel.add(classPanel, "my_classes");
        
        // ✅ Tab Học phí - gán vào field
        tuitionPanel = new StudentTuitionPanel(invoiceService, paymentService, enrollmentService, studentService, loggedInStudentId);
        contentPanel.add(tuitionPanel, "tuition");

        // ✅ Kết nối cross-refresh: khi đăng ký/hủy lớp => refresh cả tab học phí và ngược lại
        Runnable refreshStudentPanels = () -> {
            classPanel.loadData();
            tuitionPanel.loadData();
        };
        classPanel.setOnDataChanged(refreshStudentPanels);
        tuitionPanel.setOnDataChanged(refreshStudentPanels);

        // Auto-refresh mỗi 30 giây — đảm bảo dữ liệu luôn cập nhật kể cả khi không thao tác
        javax.swing.Timer autoRefresh = new javax.swing.Timer(30_000, e -> refreshStudentPanels.run());
        autoRefresh.setCoalesce(true);
        autoRefresh.start();
        
        // Các tab đang phát triển
        contentPanel.add(buildPlaceholder("Thời khóa biểu", "🚧 Đang phát triển tính năng lịch học cá nhân..."), "schedule");
        contentPanel.add(buildPlaceholder("Bảng điểm", "🚧 Đang phát triển tính năng xem kết quả học tập..."), "results");
        
        mainArea.add(contentPanel, BorderLayout.CENTER);
        add(mainArea, BorderLayout.CENTER);

        // 1. Sidebar - gọi SAU khi classPanel và tuitionPanel đã được khởi tạo
        add(buildSidebar(), BorderLayout.WEST);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(15, 23, 42)); 
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel lblLogo = new JLabel("EMS STUDENT");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        String[][] menus = {
                {"👤  Hồ sơ Cá nhân", "profile", "Hồ sơ Cá nhân"},
                {"📚  Lớp học của tôi", "my_classes", "Lớp học của tôi"},
                {"💲  Học phí & Biên lai", "tuition", "Tra cứu Học phí"},
                {"📅  Thời khóa biểu", "schedule", "Thời khóa biểu"},
                {"⭐  Kết quả học tập", "results", "Bảng điểm"}
        };

        ButtonGroup group = new ButtonGroup();
        for (String[] m : menus) {
            JToggleButton btn = new JToggleButton(m[0]);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(15, 23, 42));
            btn.setForeground(new Color(203, 213, 225));
            btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                cardLayout.show(contentPanel, m[1]);
                headerTitle.setText(m[2]);
                // ✅ Tự động refresh khi bấm vào tab Lớp học hoặc Học phí
                if (m[1].equals("my_classes")) classPanel.loadData();
                if (m[1].equals("tuition"))    tuitionPanel.loadData();
            });
            
            group.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

            if (m[1].equals("profile")) btn.setSelected(true);
        }

        sidebar.add(Box.createVerticalGlue());

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(220, 38, 38));
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Đã đăng xuất hệ thống!");
            dispose();
        });
        sidebar.add(btnLogout);

        return sidebar;
    }

    private JPanel buildProfilePanel(Student s) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 30));
        pnl.setBackground(new Color(248, 250, 252));

        if (s == null) {
            pnl.add(new JLabel("Không tìm thấy dữ liệu học viên!"));
            return pnl;
        }

        JPanel card = new JPanel(new GridLayout(7, 2, 20, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        Font lblFont = new Font("Segoe UI", Font.BOLD, 14);
        Font valFont = new Font("Segoe UI", Font.PLAIN, 15);
        Color muted = new Color(100, 116, 139);

        String[][] data = {
                {"Mã Học viên:", "HV" + String.format("%04d", s.getStudentId())},
                {"Họ và tên:", s.getFullName()},
                {"Ngày sinh:", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "---"},
                {"Giới tính:", s.getGender() != null ? s.getGender() : "---"},
                {"Số điện thoại:", s.getPhone() != null ? s.getPhone() : "---"},
                {"Email:", s.getEmail() != null ? s.getEmail() : "---"},
                {"Trạng thái:", s.getStatus()}
        };

        for (String[] row : data) {
            JLabel lbl = new JLabel(row[0]); lbl.setFont(lblFont); lbl.setForeground(muted);
            JLabel val = new JLabel(row[1]); val.setFont(valFont);
            card.add(lbl); card.add(val);
        }

        pnl.add(card);
        return pnl;
    }

    private JPanel buildPlaceholder(String title, String message) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(248, 250, 252));
        JLabel lbl = new JLabel("<html><div style='text-align:center;'><h2 style='color:#3b82f6;'>" + title + "</h2><p style='color:#64748b; font-size:14px;'>" + message + "</p></div></html>");
        p.add(lbl);
        return p;
    }
}