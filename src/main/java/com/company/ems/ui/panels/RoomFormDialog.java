package com.company.ems.ui.panels;

import com.company.ems.model.Room;
import com.company.ems.model.enums.ActiveStatus;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog Thêm / Sửa phòng học.
 */
public class RoomFormDialog extends JDialog {

    private static final Color BG_CARD       = Color.WHITE;
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);
    private static final Color PRIMARY       = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color TEXT_MAIN     = new Color(15, 23, 42);
    private static final Font  FONT_MAIN     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD     = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 12);

    private final JTextField      tfName;
    private final JTextField      tfCapacity;
    private final JTextField      tfLocation;
    private final JComboBox<ActiveStatus> cbStatus;

    private boolean saved = false;
    private final Room room;

    public RoomFormDialog(Frame owner, Room existing) {
        super(owner, existing != null ? "Sửa phòng học" : "Thêm phòng học mới", true);
        this.room = existing != null ? existing : new Room();
        boolean isEdit = existing != null;

        tfName     = createField(isEdit && existing.getRoomName() != null ? existing.getRoomName() : "");
        tfCapacity = createField(isEdit && existing.getCapacity() != null ? String.valueOf(existing.getCapacity()) : "");
        tfLocation = createField(isEdit && existing.getLocation() != null ? existing.getLocation() : "");

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));

        buildUI();
        pack();
        if (getWidth() < 480) setSize(480, getHeight());
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Tên phòng học *",   tfName);
        addRow(form, gbc, 1, "Sức chứa (chỗ)",    tfCapacity);
        addRow(form, gbc, 2, "Vị trí / Tầng",     tfLocation);
        addRow(form, gbc, 3, "Trạng thái",         cbStatus);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(room.getRoomId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Tên phòng học không được để trống.");
            tfName.requestFocus();
            return;
        }

        int capacity = 0;
        if (!tfCapacity.getText().trim().isEmpty()) {
            try {
                capacity = Integer.parseInt(tfCapacity.getText().trim());
                if (capacity < 0) {
                    showWarning("Sức chứa phải là số không âm.");
                    tfCapacity.requestFocus();
                    return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Sức chứa phải là số nguyên.");
                tfCapacity.requestFocus();
                return;
            }
        }

        room.setRoomName(name);
        room.setCapacity(capacity);
        room.setLocation(tfLocation.getText().trim().isEmpty() ? null : tfLocation.getText().trim());
        room.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Room getRoom()    { return room; }

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
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return tf;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD); btn.setForeground(Color.WHITE); btn.setBackground(PRIMARY);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_HOVER); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN); btn.setForeground(TEXT_MAIN); btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
