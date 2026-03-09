package com.company.ems.ui.panels;

import com.company.ems.model.Staff;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog Thêm / Sửa nhân viên — tách riêng khỏi StaffPanel (SRP).
 * Dùng:
 *   StaffFormDialog dlg = new StaffFormDialog(owner, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Staff result = dlg.getStaff();
 */
public class StaffFormDialog extends JDialog {

    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37,  99, 235);
    private static final Color PRIMARY_H    = new Color(29,  78, 216);
    private static final Color DANGER       = new Color(220, 38,  38);
    private static final Color TEXT_MAIN    = new Color(15,  23,  42);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font  FONT_LABEL   = new Font("Segoe UI", Font.BOLD,  12);

    // ── Fields ───────────────────────────────────────────────────────────
    private final JTextField    tfName;
    private final JTextField    tfPhone;
    private final JTextField    tfEmail;
    private final JComboBox<String> cbRole;
    private final JComboBox<String> cbStatus;
    private final JLabel        lblError;

    // ── Output ───────────────────────────────────────────────────────────
    private boolean saved = false;
    private final Staff staff;

    public StaffFormDialog(Frame owner, Staff existing) {
        super(owner, existing != null ? "Sửa nhân viên" : "Thêm nhân viên mới", true);
        this.staff = existing != null ? existing : new Staff();

        tfName  = createField(existing != null && existing.getFullName() != null ? existing.getFullName() : "");
        tfPhone = createField(existing != null && existing.getPhone()    != null ? existing.getPhone()    : "");
        tfEmail = createField(existing != null && existing.getEmail()    != null ? existing.getEmail()    : "");

        cbRole   = new JComboBox<>(new String[]{"Admin", "Consultant", "Accountant", "Manager", "Other"});
        cbStatus = new JComboBox<>(new String[]{"Active", "Inactive"});
        cbRole.setFont(FONT_MAIN);
        cbStatus.setFont(FONT_MAIN);

        if (existing != null) {
            if (existing.getRole()   != null) cbRole.setSelectedItem(existing.getRole());
            if (existing.getStatus() != null) cbStatus.setSelectedItem(existing.getStatus());
        }

        lblError = new JLabel(" ");
        lblError.setForeground(DANGER);
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        buildUI();
        pack();
        setMinimumSize(new Dimension(520, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        // ── Form ──────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 0, 6, 12);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // Hàng 1 — Họ tên (full width)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        form.add(label("Họ và tên *"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
        form.add(tfName, gbc);

        // Hàng 2 — Điện thoại | Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        form.add(label("Điện thoại"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        form.add(tfPhone, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(label("Email"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(tfEmail, gbc);

        // Hàng 3 — Vai trò | Trạng thái
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(label("Vai trò"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        form.add(cbRole, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(label("Trạng thái"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(cbStatus, gbc);

        content.add(form, BorderLayout.CENTER);

        // ── Bottom: error + buttons ───────────────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(lblError, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(FONT_MAIN);
        btnCancel.setForeground(TEXT_MAIN);
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(FONT_BOLD);
        btnSave.setForeground(Color.WHITE);
        btnSave.setBackground(PRIMARY);
        btnSave.setBorderPainted(false);
        btnSave.setFocusPainted(false);
        btnSave.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSave.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnSave.setBackground(PRIMARY_H); }
            public void mouseExited (java.awt.event.MouseEvent e) { btnSave.setBackground(PRIMARY); }
        });
        btnSave.addActionListener(e -> doSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void doSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            lblError.setText("⚠  Họ và tên không được để trống.");
            return;
        }

        staff.setFullName(name);
        staff.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        staff.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        staff.setRole((String) cbRole.getSelectedItem());
        staff.setStatus((String) cbStatus.getSelectedItem());

        saved = true;
        dispose();
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public boolean isSaved() { return saved; }
    public Staff   getStaff() { return staff; }

    // ── Helpers ───────────────────────────────────────────────────────────
    private JTextField createField(String value) {
        JTextField tf = new JTextField(value);
        tf.setFont(FONT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }
}

