package com.company.ems.ui.panels;

import com.company.ems.model.Teacher;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialog Thêm / Sửa giáo viên — tách riêng khỏi TeacherPanel (SRP).
 * Cách dùng:
 *   TeacherFormDialog dlg = new TeacherFormDialog(parentFrame, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Teacher result = dlg.getTeacher();
 */
public class TeacherFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Design tokens (đồng bộ với StudentPanel)
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER= new Color(29, 78, 216);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);

    // Form fields
    private final JTextField tfName;
    private final JTextField tfPhone;
    private final JTextField tfEmail;
    private final JTextField tfSpecialty;
    private final JTextField tfHireDate;
    private final JComboBox<String> cbStatus;

    // Output
    private boolean saved = false;
    private final Teacher teacher;

    public TeacherFormDialog(Frame owner, Teacher existing) {
        super(owner, existing != null ? "Sửa giáo viên" : "Thêm giáo viên mới", true);
        this.teacher = existing != null ? existing : new Teacher();

        boolean isEdit = existing != null;

        tfName      = createField(isEdit && existing.getFullName() != null ? existing.getFullName() : "");
        tfPhone     = createField(isEdit && existing.getPhone() != null ? existing.getPhone() : "");
        tfEmail     = createField(isEdit && existing.getEmail() != null ? existing.getEmail() : "");
        tfSpecialty = createField(isEdit && existing.getSpecialty() != null ? existing.getSpecialty() : "");
        tfHireDate  = createField(isEdit && existing.getHireDate() != null
                ? existing.getHireDate().format(DATE_FMT) : "");

        cbStatus = new JComboBox<>(new String[]{"Active", "Inactive"});
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) {
            cbStatus.setSelectedItem(existing.getStatus());
        }

        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Họ và tên *",            tfName);
        addRow(form, gbc, 1, "Điện thoại",             tfPhone);
        addRow(form, gbc, 2, "Email",                  tfEmail);
        addRow(form, gbc, 3, "Chuyên môn",             tfSpecialty);
        addRow(form, gbc, 4, "Ngày vào làm (dd/MM/yyyy)", tfHireDate);
        addRow(form, gbc, 5, "Trạng thái",             cbStatus);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(teacher.getTeacherId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Họ và tên không được để trống.");
            tfName.requestFocus();
            return;
        }

        LocalDate hireDate = null;
        if (!tfHireDate.getText().trim().isEmpty()) {
            try {
                hireDate = LocalDate.parse(tfHireDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Định dạng ngày vào làm không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfHireDate.requestFocus();
                return;
            }
        }

        teacher.setFullName(name);
        teacher.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        teacher.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        teacher.setSpecialty(tfSpecialty.getText().trim().isEmpty() ? null : tfSpecialty.getText().trim());
        teacher.setHireDate(hireDate);
        teacher.setStatus((String) cbStatus.getSelectedItem());

        saved = true;
        dispose();
    }

    public boolean isSaved()   { return saved; }
    public Teacher getTeacher(){ return teacher; }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(0, 36));
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
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
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
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}

