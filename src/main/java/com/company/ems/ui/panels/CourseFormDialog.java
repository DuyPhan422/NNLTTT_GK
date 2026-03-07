package com.company.ems.ui.panels;

import com.company.ems.model.Course;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Dialog Thêm / Sửa khóa học — tách khỏi CoursePanel (SRP).
 */
public class CourseFormDialog extends JDialog {

    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER= new Color(29, 78, 216);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);

    private final JTextField tfName;
    private final JTextArea  taDescription;
    private final JComboBox<String> cbLevel;
    private final JTextField tfDuration;
    private final JComboBox<String> cbDurationUnit;
    private final JTextField tfFee;
    private final JComboBox<String> cbStatus;

    private boolean saved = false;
    private final Course course;

    public CourseFormDialog(Frame owner, Course existing) {
        super(owner, existing != null ? "Sửa khóa học" : "Thêm khóa học mới", true);
        this.course = existing != null ? existing : new Course();

        boolean isEdit = existing != null;

        tfName = createField(isEdit && existing.getCourseName() != null ? existing.getCourseName() : "");

        taDescription = new JTextArea(isEdit && existing.getDescription() != null ? existing.getDescription() : "");
        taDescription.setFont(FONT_MAIN);
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);
        taDescription.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        cbLevel = new JComboBox<>(new String[]{"Cơ bản", "Trung cấp", "Nâng cao"});
        cbLevel.setFont(FONT_MAIN);
        if (isEdit && existing.getLevel() != null) {
            cbLevel.setSelectedItem(existing.getLevel());
        }

        tfDuration = createField(isEdit && existing.getDuration() != null
                ? String.valueOf(existing.getDuration()) : "");

        cbDurationUnit = new JComboBox<>(new String[]{"Giờ","Tuần", "Tháng"});
        cbDurationUnit.setFont(FONT_MAIN);
        if (isEdit && existing.getDurationUnit() != null) {
            cbDurationUnit.setSelectedItem(existing.getDurationUnit());
        }

        tfFee = createField(isEdit && existing.getFee() != null
                ? existing.getFee().toPlainString() : "");

        cbStatus = new JComboBox<>(new String[]{"Hoạt động", "Không hoạt động"});
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) {
            cbStatus.setSelectedItem(existing.getStatus());
        }

        buildUI();
        pack();
        // Mở rộng chiều ngang để ô \"Mô tả\" thoáng hơn
        if (getWidth() < 620) {
            setSize(620, getHeight());
        }
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

        addRow(form, gbc, 0, "Tên khóa học *", tfName);

        gbc.gridy  = 2;
        gbc.insets = new Insets(10, 0, 2, 0);
        JLabel lblDesc = new JLabel("Mô tả");
        lblDesc.setFont(FONT_SMALL);
        lblDesc.setForeground(TEXT_MUTED);
        form.add(lblDesc, gbc);

        gbc.gridy  = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        JScrollPane descScroll = new JScrollPane(taDescription);
        descScroll.setPreferredSize(new Dimension(0, 80));
        form.add(descScroll, gbc);

        addRow(form, gbc, 4, "Cấp độ", cbLevel);
        addRow(form, gbc, 5, "Thời lượng", tfDuration);
        addRow(form, gbc, 6, "Đơn vị thời lượng", cbDurationUnit);
        addRow(form, gbc, 7, "Học phí", tfFee);
        addRow(form, gbc, 8, "Trạng thái", cbStatus);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(course.getCourseId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Tên khóa học không được để trống.");
            tfName.requestFocus();
            return;
        }

        Integer duration = null;
        if (!tfDuration.getText().trim().isEmpty()) {
            try {
                duration = Integer.parseInt(tfDuration.getText().trim());
                if (duration <= 0) {
                    showWarning("Thời lượng phải là số dương.");
                    tfDuration.requestFocus();
                    return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Thời lượng phải là số nguyên.");
                tfDuration.requestFocus();
                return;
            }
        }

        BigDecimal fee = BigDecimal.ZERO;
        if (!tfFee.getText().trim().isEmpty()) {
            try {
                fee = new BigDecimal(tfFee.getText().trim());
                if (fee.compareTo(BigDecimal.ZERO) < 0) {
                    showWarning("Học phí không được âm.");
                    tfFee.requestFocus();
                    return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Học phí không hợp lệ.");
                tfFee.requestFocus();
                return;
            }
        }

        course.setCourseName(name);
        course.setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        course.setLevel((String) cbLevel.getSelectedItem());
        course.setDuration(duration);
        course.setDurationUnit((String) cbDurationUnit.getSelectedItem());
        course.setFee(fee);
        course.setStatus((String) cbStatus.getSelectedItem());

        saved = true;
        dispose();
    }

    public boolean isSaved()  { return saved; }
    public Course getCourse() { return course; }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        int base = row * 2 + 2; // tránh đè vùng mô tả
        gbc.gridy  = base;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = base + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
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
            public void mouseExited (java.awt.event.MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}

