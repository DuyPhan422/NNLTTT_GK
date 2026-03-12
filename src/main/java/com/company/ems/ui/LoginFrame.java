package com.company.ems.ui;

import com.company.ems.model.UserAccount;
import com.company.ems.service.*;

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
    private static final Color BG_LEFT    = new Color(15,  23,  42);
    private static final Color BG_RIGHT   = new Color(248, 250, 252);
    private static final Color PRIMARY    = new Color(37,  99,  235);
    private static final Color PRIMARY_H  = new Color(29,  78,  216);
    private static final Color DANGER     = new Color(220, 38,  38);
    private static final Color BORDER_COL = new Color(226, 232, 240);
    private static final Color TEXT_MAIN  = new Color(15,  23,  42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  28);
    private static final Font FONT_SUB   = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BTN   = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Components ───────────────────────────────────────────────────────
    private final JTextField     tfUsername = new JTextField();
    private final JPasswordField tfPassword = new JPasswordField();
    private final JButton        btnLogin   = new JButton("Đăng nhập");
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
        p.setBackground(BG_LEFT);
        p.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("🎓");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Language Center");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("<html><div style='text-align:center;color:#94a3b8'>"
                + "Hệ thống quản lý trung tâm<br>ngoại ngữ toàn diện</div></html>");
        sub.setFont(FONT_SUB);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel features = new JPanel();
        features.setOpaque(false);
        features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
        String[] items = {
            "✅  Quản lý học viên & lớp học",
            "✅  Đăng ký & thanh toán học phí",
            "✅  Điểm danh & theo dõi chuyên cần",
            "✅  Kết quả học tập"
        };
        for (String item : items) {
            JLabel lbl = new JLabel(item);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(new Color(148, 163, 184));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
            features.add(lbl);
        }
        features.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createRigidArea(new Dimension(0, 16)));
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 12)));
        inner.add(sub);
        inner.add(Box.createRigidArea(new Dimension(0, 32)));
        inner.add(features);

        p.add(inner);
        return p;
    }

    /** Bên phải: Form đăng nhập */
    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_RIGHT);
        p.setBorder(new EmptyBorder(40, 50, 40, 50));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Đăng nhập");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT_MAIN);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Nhập thông tin tài khoản để tiếp tục");
        lblSub.setFont(FONT_SUB);
        lblSub.setForeground(TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Username
        JLabel lblUser = new JLabel("Tên đăng nhập");
        lblUser.setFont(FONT_LABEL);
        lblUser.setForeground(TEXT_MAIN);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleInput(tfUsername);
        tfUsername.putClientProperty("JTextField.placeholderText", "Nhập tên đăng nhập...");
        tfUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Password
        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(FONT_LABEL);
        lblPass.setForeground(TEXT_MAIN);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleInput(tfPassword);
        tfPassword.putClientProperty("JTextField.placeholderText", "Nhập mật khẩu...");
        tfPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Show password checkbox
        cbShowPass.setFont(FONT_SMALL);
        cbShowPass.setForeground(TEXT_MUTED);
        cbShowPass.setOpaque(false);
        cbShowPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbShowPass.addActionListener(e ->
                tfPassword.setEchoChar(cbShowPass.isSelected() ? '\0' : '•'));

        // Error label
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Login button
        btnLogin.setFont(FONT_BTN);
        btnLogin.setBackground(PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnLogin.setBackground(PRIMARY_H); }
            public void mouseExited (MouseEvent e) { btnLogin.setBackground(PRIMARY); }
        });
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
                "<html><span style='color:#94a3b8'>"
                + "Demo — admin/admin123 · teacher/teacher123 · student/student123"
                + "</span></html>");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblTitle);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(lblSub);
        form.add(Box.createRigidArea(new Dimension(0, 32)));
        form.add(lblUser);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(tfUsername);
        form.add(Box.createRigidArea(new Dimension(0, 18)));
        form.add(lblPass);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(tfPassword);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(cbShowPass);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(lblError);
        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(btnLogin);
        form.add(Box.createRigidArea(new Dimension(0, 16)));
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

    private void styleInput(JComponent c) {
        c.setFont(FONT_INPUT);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
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

