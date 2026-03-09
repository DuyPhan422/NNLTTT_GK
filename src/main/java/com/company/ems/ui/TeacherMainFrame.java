package com.company.ems.ui;

import com.company.ems.model.Teacher;
import com.company.ems.service.*;
import com.company.ems.ui.panels.ResultTeacherPanel;
import com.company.ems.ui.panels.ScheduleTeacherPanel;
import com.company.ems.ui.panels.attendance.AttendanceTeacherPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Cửa sổ chính dành cho Giáo viên.
 *
 * Tabs (theo thứ tự sidebar):
 *   1. Lịch dạy   : ScheduleTeacherPanel  ✅
 *   2. Điểm danh  : AttendanceTeacherPanel ✅
 *   3. Nhập điểm  : ResultTeacherPanel     ✅
 *   4. Lớp số     : Placeholder (tương lai)
 */
public class TeacherMainFrame extends JFrame {

    private static final Color BG_SIDEBAR  = new Color(15,  23,  42);
    private static final Color BG_CONTENT  = new Color(248, 250, 252);
    private static final Color ITEM_ACTIVE = new Color(37,  99,  235);
    private static final Color TEXT_NAV    = new Color(203, 213, 225);
    private static final Color DANGER      = new Color(220, 38,  38);

    private final Teacher  currentTeacher;
    private final Runnable onLogout;

    private final JPanel     contentPanel;
    private final CardLayout cardLayout;
    private final JLabel     headerTitle;

    // ── Panels (giữ reference để có thể gọi loadData khi switch tab) ────
    private final ScheduleTeacherPanel   schedulePanel;
    private final AttendanceTeacherPanel attendancePanel;
    private final ResultTeacherPanel     resultPanel;

    public TeacherMainFrame(TeacherService teacherService,
                            ClassService classService,
                            ScheduleService scheduleService,
                            AttendanceService attendanceService,
                            ResultService resultService,
                            RoomService roomService,
                            Teacher teacher,
                            Runnable onLogout) {
        this.currentTeacher = teacher;
        this.onLogout       = onLogout;

        setTitle("Language Center — Giáo viên | " + teacher.getFullName());
        setSize(1280, 760);
        setMinimumSize(new Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Header ───────────────────────────────────────────────────────
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG_CONTENT);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 16));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        headerTitle = new JLabel("Lịch dạy");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerTitle.setForeground(new Color(15, 23, 42));
        headerPanel.add(headerTitle);
        rightPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Content (CardLayout) ─────────────────────────────────────────
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_CONTENT);

        // 1. Lịch dạy
        schedulePanel = new ScheduleTeacherPanel(scheduleService, classService, roomService, teacher);
        contentPanel.add(schedulePanel, "schedule");

        // 2. Điểm danh
        attendancePanel = new AttendanceTeacherPanel(attendanceService, classService, teacher);
        contentPanel.add(attendancePanel, "attendance");

        // 3. Nhập điểm
        resultPanel = new ResultTeacherPanel(resultService, classService, teacher);
        contentPanel.add(resultPanel, "results");

        // 4. Lớp số — placeholder
        contentPanel.add(buildPlaceholder("🚧 Lớp học số — đang phát triển"), "my_classes");

        rightPanel.add(contentPanel, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.WEST);
        add(rightPanel,     BorderLayout.CENTER);

        // Mặc định hiển thị Lịch dạy
        cardLayout.show(contentPanel, "schedule");
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        // Logo
        JLabel lblLogo = new JLabel("🎓 Language Center");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);

        // Tên giáo viên
        JLabel lblName = new JLabel(currentTeacher.getFullName());
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setForeground(new Color(148, 163, 184));
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(lblName);

        // Badge
        JLabel lblRole = new JLabel("  GIÁO VIÊN  ");
        lblRole.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblRole.setForeground(new Color(147, 197, 253));
        lblRole.setOpaque(true);
        lblRole.setBackground(new Color(30, 58, 138));
        lblRole.setBorder(new EmptyBorder(2, 6, 2, 6));
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(lblRole);
        sidebar.add(Box.createRigidArea(new Dimension(0, 24)));

        // Menu — thứ tự: Lịch dạy → Điểm danh → Nhập điểm → Lớp số
        // {icon+label, cardKey, headerTitle}
        String[][] menus = {
            {"📅  Lịch dạy",     "schedule",   "Lịch dạy"},
            {"✅  Điểm danh",    "attendance", "Điểm danh"},
            {"⭐  Nhập điểm",    "results",    "Nhập kết quả học tập"},
            {"💻  Lớp học số",   "my_classes", "Lớp học số"},
        };

        ButtonGroup group = new ButtonGroup();
        for (String[] m : menus) {
            JToggleButton btn = buildNavButton(m[0]);
            btn.addActionListener(e -> {
                cardLayout.show(contentPanel, m[1]);
                headerTitle.setText(m[2]);
                // Refresh panel khi switch
                switch (m[1]) {
                    case "schedule"   -> schedulePanel.loadData();
                    case "attendance" -> {} // AttendancePanel refresh khi chọn lớp
                    case "results"    -> {} // ResultPanel refresh khi chọn lớp
                }
            });
            group.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
            if (m[1].equals("schedule")) btn.setSelected(true);
        }

        sidebar.add(Box.createVerticalGlue());

        // Nút đăng xuất
        JButton btnLogout = new JButton("⏻  Đăng xuất");
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(DANGER);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnLogout.setBackground(new Color(185, 28, 28)); }
            public void mouseExited (java.awt.event.MouseEvent e) { btnLogout.setBackground(DANGER); }
        });
        btnLogout.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                dispose();
                if (onLogout != null) onLogout.run();
            }
        });
        sidebar.add(btnLogout);

        return sidebar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JToggleButton buildNavButton(String label) {
        JToggleButton btn = new JToggleButton(label);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBackground(BG_SIDEBAR);
        btn.setForeground(TEXT_NAV);
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addChangeListener(e -> {
            btn.setBackground(btn.isSelected() ? ITEM_ACTIVE : BG_SIDEBAR);
            btn.setForeground(btn.isSelected() ? Color.WHITE  : TEXT_NAV);
        });
        return btn;
    }

    private JPanel buildPlaceholder(String message) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_CONTENT);
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("🚧");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(new Color(100, 116, 139));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);
        inner.add(Box.createRigidArea(new Dimension(0, 12)));
        inner.add(lbl);
        p.add(inner);
        return p;
    }
}

