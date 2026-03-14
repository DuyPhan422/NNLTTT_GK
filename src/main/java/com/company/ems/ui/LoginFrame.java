package com.company.ems.ui;

import com.company.ems.model.UserAccount;
import com.company.ems.service.*;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.common.ComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

/**
 * Cửa sổ đăng nhập — Điểm khởi đầu duy nhất của ứng dụng.
 *
 * Luồng:
 *   Login thành công → switch(role)
 *     "Admin"   → MainFrame
 *     "Teacher" → TeacherMainFrame (truyền teacher object)
 *     "Student" → StudentMainFrame (truyền student.id)
 *     "Staff"   → MainFrame (dùng chung với Admin, giới hạn quyền sau)
 */
public class LoginFrame extends JFrame {

    // ── Design tokens ────────────────────────────────────────────────────
    private static final Color BG_LEFT = new Color(15, 23, 42);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 14);

    private final JTextField     tfUsername = new JTextField();
    private final JPasswordField tfPassword = new JPasswordField();
    private final JButton        btnLogin   = ComponentFactory.primaryButton("Đăng nhập");
    private final JLabel         lblError   = new JLabel(" ");
    private final JCheckBox      cbShowPass = new JCheckBox("Hiện mật khẩu");

    // ── Services ─────────────────────────────────────────────────────────
    private final UserAccountService userAccountService;
    private final StudentService     studentService;
    private final TeacherService     teacherService;
    private final CourseService      courseService;
    private final RoomService        roomService;
    private final StaffService       staffService;
    private final ClassService       classService;
    private final EnrollmentService  enrollmentService;
    private final InvoiceService     invoiceService;
    private final PaymentService     paymentService;
    private final ScheduleService    scheduleService;
    private final AttendanceService  attendanceService;
    private final ResultService      resultService;

    public LoginFrame(UserAccountService userAccountService,
                      StudentService studentService,
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
                      ResultService resultService) {
        this.userAccountService = userAccountService;
        this.studentService     = studentService;
        this.teacherService     = teacherService;
        this.courseService      = courseService;
        this.roomService        = roomService;
        this.staffService       = staffService;
        this.classService       = classService;
        this.enrollmentService  = enrollmentService;
        this.invoiceService     = invoiceService;
        this.paymentService     = paymentService;
        this.scheduleService    = scheduleService;
        this.attendanceService  = attendanceService;
        this.resultService      = resultService;

        setTitle("Language Center — Đăng nhập");
        setSize(900, 560);
        setMinimumSize(new Dimension(760, 480));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        add(buildLeftPanel());
        add(buildRightPanel());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════════════════════

    /** Bên trái: Banner thương hiệu */
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(30, 41, 59)); // Slate-800
        p.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // Custom drawn text logo instead of Emoji
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(getWidth() / 2 - 32, 0, 64, 64, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("LC", getWidth() / 2 - fm.stringWidth("LC") / 2, 42);
                g2.dispose();
            }
        };
        logoPanel.setPreferredSize(new Dimension(80, 80));
        logoPanel.setMaximumSize(new Dimension(80, 80));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Language Center");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("<html><div style='text-align:center;color:#94a3b8'>"
                + "Hệ thống quản lý trung tâm<br>ngoại ngữ toàn vẹn và thông minh</div></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel features = new JPanel();
        features.setOpaque(false);
        features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
        String[] items = {
            "✓  Quản lý học viên & lớp học",
            "✓  Khung chương trình đào tạo",
            "✓  Theo dõi chuyên cần & kết quả",
            "✓  Tài chính & Thanh toán"
        };
        for (String item : items) {
            JLabel lbl = new JLabel(item);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lbl.setForeground(new Color(148, 163, 184)); // Slate-400
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(new EmptyBorder(6, 0, 6, 0));
            features.add(lbl);
        }
        features.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(logoPanel);
        inner.add(Box.createRigidArea(new Dimension(0, 24)));
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 16)));
        inner.add(sub);
        inner.add(Box.createRigidArea(new Dimension(0, 40)));
        inner.add(features);

        p.add(inner);
        return p;
    }

    /** Bên phải: Form đăng nhập */
    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(40, 60, 40, 60));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Đăng nhập");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitle.setForeground(new Color(15, 23, 42)); // Slate-900
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Chào mừng trở lại! Vui lòng nhập thông tin của bạn.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblSub.setForeground(new Color(100, 116, 139)); // Slate-500
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Username
        JLabel lblUser = new JLabel("Tên đăng nhập");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(new Color(51, 65, 85)); // Slate-700
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleInput(tfUsername, "Nhập tên đăng nhập...");

        // Password
        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPass.setForeground(new Color(51, 65, 85));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleInput(tfPassword, "Nhập mật khẩu...");

        // Show password checkbox
        cbShowPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbShowPass.setForeground(new Color(100, 116, 139));
        cbShowPass.setOpaque(false);
        cbShowPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbShowPass.setFocusPainted(false);
        cbShowPass.addActionListener(e ->
                tfPassword.setEchoChar(cbShowPass.isSelected() ? '\0' : '•'));

        // Error label
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblError.setForeground(Theme.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Login button
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> doLogin());

        // Enter key
        KeyAdapter enterLogin = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        tfUsername.addKeyListener(enterLogin);
        tfPassword.addKeyListener(enterLogin);

        // Hint tài khoản demo
        JLabel lblHint = new JLabel(
                "<html><div style='text-align:center;color:#94a3b8;font-size:11px'>"
                + "Demo — admin/admin123 · teacher/teacher123<br>student/student123 · staff/staff123"
                + "</div></html>");
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblTitle);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(lblSub);
        form.add(Box.createRigidArea(new Dimension(0, 40)));
        form.add(lblUser);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(tfUsername);
        form.add(Box.createRigidArea(new Dimension(0, 20)));
        form.add(lblPass);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(tfPassword);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(cbShowPass);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(lblError);
        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(btnLogin);
        form.add(Box.createRigidArea(new Dimension(0, 24)));
        form.add(lblHint);

        p.add(form);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOGIC ĐĂNG NHẬP
    // ══════════════════════════════════════════════════════════════════════

    private void doLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(tfPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang xác thực...");
        lblError.setText(" ");

        // Chạy trên background thread — tránh đơ UI
        SwingWorker<Optional<UserAccount>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<UserAccount> doInBackground() {
                return userAccountService.login(username, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                try {
                    Optional<UserAccount> result = get();
                    if (result.isPresent()) {
                        onLoginSuccess(result.get());
                    } else {
                        showError("Tên đăng nhập hoặc mật khẩu không đúng.");
                        tfPassword.setText("");
                        tfPassword.requestFocus();
                    }
                } catch (Exception ex) {
                    showError("Lỗi kết nối hệ thống: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Login thành công → route đến đúng Frame theo role */
    private void onLoginSuccess(UserAccount account) {
        dispose();

        switch (account.getRole()) {

            case "Quản trị" -> {
                Runnable onLogout = () -> {
                    LoginFrame newLogin = new LoginFrame(
                            userAccountService, studentService, teacherService, courseService,
                            roomService, staffService, classService, enrollmentService,
                            invoiceService, paymentService, scheduleService, attendanceService, resultService
                    );
                    newLogin.setVisible(true);
                };
                MainFrame frame = new MainFrame(
                        studentService, teacherService, courseService,
                        roomService, staffService, classService,
                        enrollmentService, invoiceService, paymentService,
                        scheduleService, attendanceService, resultService,
                        userAccountService, onLogout
                );
                frame.setTitle("Language Center — " + account.getRole()
                        + " | " + account.getUsername());
                frame.setVisible(true);
            }

            case "Nhân viên" -> {
                Runnable onLogout = () -> {
                    LoginFrame newLogin = new LoginFrame(
                            userAccountService, studentService, teacherService, courseService,
                            roomService, staffService, classService, enrollmentService,
                            invoiceService, paymentService, scheduleService, attendanceService, resultService
                    );
                    newLogin.setVisible(true);
                };
                StaffMainFrame frame = new StaffMainFrame(
                        studentService, teacherService, courseService,
                        roomService, staffService, classService,
                        enrollmentService, invoiceService, paymentService,
                        scheduleService, attendanceService, resultService, onLogout
                );
                frame.setTitle("Language Center — " + account.getRole()
                        + " | " + account.getUsername());
                frame.setVisible(true);
            }

            case "Giáo viên" -> {
                if (account.getTeacher() == null) {
                    showFatalError("Tài khoản giáo viên không liên kết với hồ sơ giảng viên.");
                    return;
                }
                Runnable onLogoutTeacher = () -> {
                    LoginFrame newLogin = new LoginFrame(
                            userAccountService, studentService, teacherService, courseService,
                            roomService, staffService, classService, enrollmentService,
                            invoiceService, paymentService, scheduleService, attendanceService, resultService
                    );
                    newLogin.setVisible(true);
                };
                TeacherMainFrame frame = new TeacherMainFrame(
                        teacherService, classService, scheduleService,
                        attendanceService, resultService, roomService,
                        account.getTeacher(), onLogoutTeacher
                );
                frame.setVisible(true);
            }

            case "Học viên" -> {
                if (account.getStudent() == null) {
                    showFatalError("Tài khoản học viên không liên kết với hồ sơ học viên.");
                    return;
                }
                Runnable onLogoutStudent = () -> {
                    LoginFrame newLogin = new LoginFrame(
                            userAccountService, studentService, teacherService, courseService,
                            roomService, staffService, classService, enrollmentService,
                            invoiceService, paymentService, scheduleService, attendanceService, resultService
                    );
                    newLogin.setVisible(true);
                };
                StudentMainFrame frame = new StudentMainFrame(
                        studentService, invoiceService, paymentService,
                        enrollmentService, classService, attendanceService,
                        scheduleService, resultService,
                        account.getStudent().getStudentId(), onLogoutStudent
                );
                frame.setVisible(true);
            }

            default -> showFatalError("Role không hợp lệ: " + account.getRole());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void styleInput(JTextField c, String placeholder) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        c.setBackground(new Color(248, 250, 252)); // Slate-50
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true), // Slate-300 rounded
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        c.putClientProperty("JTextField.placeholderText", placeholder);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        c.setPreferredSize(new Dimension(300, 46));
    }

    private void showError(String msg) {
        lblError.setText("⚠  " + msg);
    }

    private void showFatalError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        // Mở lại LoginFrame thay vì crash
        LoginFrame newLogin = new LoginFrame(
                userAccountService, studentService, teacherService, courseService,
                roomService, staffService, classService, enrollmentService,
                invoiceService, paymentService, scheduleService, attendanceService, resultService
        );
        newLogin.setVisible(true);
    }
}

