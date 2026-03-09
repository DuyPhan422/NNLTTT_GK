package com.company.ems.ui.panels;

import com.company.ems.model.Student;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.model.enums.Gender;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialog Thêm / Sửa học viên — tách riêng khỏi StudentPanel (SRP).
 * Cách dùng:
 *   StudentFormDialog dlg = new StudentFormDialog(parentFrame, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Student result = dlg.getStudent();
 */
public class StudentFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Design tokens (đồng bộ với StudentPanel) ─────
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY    = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color TEXT_MAIN  = new Color(15, 23, 42);
    private static final Font  FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Form fields ───────────────────────────────────
    private final JTextField      tfName;
    private final JTextField      tfDob;
    private final JComboBox<Gender> cbGender;
    private final JTextField      tfPhone;
    private final JTextField      tfEmail;
    private final JTextField      tfAddress;
    private final JComboBox<ActiveStatus> cbStatus;

    // ── Output ────────────────────────────────────────
    private boolean saved = false;
    private final Student student; // entity được trả về sau khi save

    public StudentFormDialog(Frame owner, Student existing) {
        super(owner, existing != null ? "Sửa học viên" : "Thêm học viên mới", true);
        this.student = existing != null ? existing : new Student();

        boolean isEdit = existing != null;

        // Khởi tạo fields với giá trị hiện tại nếu là edit
        tfName    = createField(isEdit ? existing.getFullName() : "");
        tfDob     = createField(isEdit && existing.getDateOfBirth() != null
                        ? existing.getDateOfBirth().format(DATE_FMT) : "");
        tfPhone   = createField(isEdit && existing.getPhone()   != null ? existing.getPhone()   : "");
        tfEmail   = createField(isEdit && existing.getEmail()   != null ? existing.getEmail()   : "");
        tfAddress = createField(isEdit && existing.getAddress() != null ? existing.getAddress() : "");

        cbGender = new JComboBox<>(Gender.values());

        cbGender.setFont(FONT_MAIN);
        if (isEdit && existing.getGender() != null) cbGender.setSelectedItem(Gender.fromValue(existing.getGender()));

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(FONT_MAIN);
        if (isEdit) cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));

        setSize(480, 500);
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        // ── Form ──────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Họ và tên *",            tfName);
        addRow(form, gbc, 1, "Ngày sinh (dd/MM/yyyy)", tfDob);
        addRow(form, gbc, 2, "Giới tính",              cbGender);
        addRow(form, gbc, 3, "Điện thoại",             tfPhone);
        addRow(form, gbc, 4, "Email",                  tfEmail);
        addRow(form, gbc, 5, "Địa chỉ",               tfAddress);
        addRow(form, gbc, 6, "Trạng thái",             cbStatus);

        content.add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(student.getStudentId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        // ── Validation ────────────────────────────────
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Họ và tên không được để trống.");
            tfName.requestFocus();
            return;
        }

        LocalDate dob = null;
        if (!tfDob.getText().trim().isEmpty()) {
            try {
                dob = LocalDate.parse(tfDob.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Định dạng ngày sinh không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfDob.requestFocus();
                return;
            }
        }

        // ── Xác nhận trước khi lưu ────────────────────────────
        String confirmMsg = student.getStudentId() != null
                ? "Bạn có chắc muốn lưu các thay đổi cho học viên \"" + name + "\"?"
                : "Bạn có chắc muốn thêm học viên mới \"" + name + "\"?";
        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // ── Gán vào entity ────────────────────────────
        student.setFullName(name);
        student.setDateOfBirth(dob);
        student.setGender(((Gender) cbGender.getSelectedItem()).getValue());
        student.setPhone(tfPhone.getText().trim().isEmpty()   ? null : tfPhone.getText().trim());
        student.setEmail(tfEmail.getText().trim().isEmpty()   ? null : tfEmail.getText().trim());
        student.setAddress(tfAddress.getText().trim().isEmpty() ? null : tfAddress.getText().trim());
        student.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());

        saved = true;
        dispose();
    }

    // ── Getters ───────────────────────────────────────
    public boolean isSaved()    { return saved; }
    public Student getStudent() { return student; }

    // ══════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        // Dùng width=150 thay 0: GridBagLayout sẽ tự stretch theo HORIZONTAL fill,
        // nhưng JComboBox cần width > 0 để FlatLaf tính đúng vị trí popup
        field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);
    }

    private JTextField createField(String value) {
        JTextField tf = new JTextField(value);
        tf.setFont(FONT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return tf;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(241, 245, 249)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}
