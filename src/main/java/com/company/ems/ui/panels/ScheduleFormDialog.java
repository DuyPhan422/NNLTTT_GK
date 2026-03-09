package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Room;
import com.company.ems.model.Schedule;
import com.company.ems.service.ScheduleService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Dialog Thêm / Sửa một buổi học (Schedule) của một lớp.
 * Tự động kiểm tra trùng phòng và trùng giáo viên trước khi lưu.
 */
public class ScheduleFormDialog extends JDialog {

    private static final Color BG_CARD       = Color.WHITE;
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);
    private static final Color PRIMARY       = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color TEXT_MAIN     = new Color(15, 23, 42);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextField   tfDate;
    private final JTextField   tfStartTime;
    private final JTextField   tfEndTime;
    private final JComboBox<Room> cbRoom;

    private final Schedule        schedule;
    private final Class           clazz;
    private final ScheduleService scheduleService;
    private boolean saved = false;

    public ScheduleFormDialog(Frame owner,
                               Schedule existing,
                               Class clazz,
                               List<Room> rooms,
                               ScheduleService scheduleService) {
        super(owner, existing != null ? "Sửa buổi học" : "Thêm buổi học mới", true);
        this.clazz           = clazz;
        this.scheduleService = scheduleService;
        this.schedule        = existing != null ? existing : new Schedule();

        boolean isEdit = existing != null;

        tfDate      = createField(isEdit && existing.getStudyDate() != null
                ? existing.getStudyDate().format(DATE_FMT) : "");
        tfStartTime = createField(isEdit && existing.getStartTime() != null
                ? existing.getStartTime().format(TIME_FMT) : "");
        tfEndTime   = createField(isEdit && existing.getEndTime() != null
                ? existing.getEndTime().format(TIME_FMT) : "");

        cbRoom = new JComboBox<>(rooms.toArray(new Room[0]));
        cbRoom.setFont(FONT_MAIN);
        cbRoom.insertItemAt(null, 0);
        cbRoom.setSelectedIndex(0);
        if (isEdit && existing.getRoom() != null) {
            cbRoom.setSelectedItem(existing.getRoom());
        } else if (clazz.getRoom() != null) {
            // Pre-fill phòng mặc định của lớp
            cbRoom.setSelectedItem(clazz.getRoom());
        }

        buildUI();
        pack();
        if (getWidth() < 480) setSize(480, getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        // Tiêu đề lớp (readonly info)
        JLabel lblClass = new JLabel("Lớp: " + clazz.getClassName());
        lblClass.setFont(FONT_BOLD);
        lblClass.setForeground(PRIMARY);
        lblClass.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        content.add(lblClass, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Ngày học (dd/MM/yyyy) *", tfDate);
        addRow(form, gbc, 1, "Giờ bắt đầu (HH:mm) *",  tfStartTime);
        addRow(form, gbc, 2, "Giờ kết thúc (HH:mm) *",  tfEndTime);
        addRow(form, gbc, 3, "Phòng học",                cbRoom);

        content.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(schedule.getScheduleId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        // 1. Parse ngày
        LocalDate date;
        try {
            date = LocalDate.parse(tfDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            showWarning("Ngày học không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfDate.requestFocus();
            return;
        }

        // 2. Parse giờ bắt đầu
        LocalTime startTime;
        try {
            startTime = LocalTime.parse(tfStartTime.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            showWarning("Giờ bắt đầu không hợp lệ. Vui lòng nhập HH:mm.");
            tfStartTime.requestFocus();
            return;
        }

        // 3. Parse giờ kết thúc
        LocalTime endTime;
        try {
            endTime = LocalTime.parse(tfEndTime.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            showWarning("Giờ kết thúc không hợp lệ. Vui lòng nhập HH:mm.");
            tfEndTime.requestFocus();
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showWarning("Giờ kết thúc phải sau giờ bắt đầu.");
            tfEndTime.requestFocus();
            return;
        }

        // 4. Kiểm tra ngày học nằm trong khoảng lớp học
        if (clazz.getStartDate() != null && date.isBefore(clazz.getStartDate())) {
            showWarning("Ngày học không được trước ngày khai giảng lớp ("
                    + clazz.getStartDate().format(DATE_FMT) + ").");
            tfDate.requestFocus();
            return;
        }
        if (clazz.getEndDate() != null && date.isAfter(clazz.getEndDate())) {
            showWarning("Ngày học không được sau ngày kết thúc lớp ("
                    + clazz.getEndDate().format(DATE_FMT) + ").");
            tfDate.requestFocus();
            return;
        }

        Long excludeId = schedule.getScheduleId(); // null khi thêm mới
        Room selectedRoom = (Room) cbRoom.getSelectedItem();
        StringBuilder conflictMsg = new StringBuilder();

        // 5. Kiểm tra trùng phòng
        if (selectedRoom != null) {
            try {
                List<Schedule> roomConflicts = scheduleService.checkRoomConflicts(
                        selectedRoom.getRoomId(), date, startTime, endTime, excludeId);
                if (!roomConflicts.isEmpty()) {
                    Schedule c = roomConflicts.get(0);
                    conflictMsg.append("⚠️ Phòng \"").append(selectedRoom.getRoomName())
                               .append("\" đã có lịch buổi khác (")
                               .append(c.getStartTime().format(TIME_FMT))
                               .append(" – ").append(c.getEndTime().format(TIME_FMT))
                               .append(", lớp: ").append(c.getClazz().getClassName()).append(").");
                }
            } catch (Exception ex) {
                // Không chặn lưu nếu kiểm tra lỗi
            }
        }

        // 6. Kiểm tra trùng giáo viên
        if (conflictMsg.isEmpty() && clazz.getTeacher() != null) {
            try {
                List<Schedule> teacherConflicts = scheduleService.checkTeacherConflicts(
                        clazz.getTeacher().getTeacherId(), date, startTime, endTime, excludeId);
                if (!teacherConflicts.isEmpty()) {
                    Schedule c = teacherConflicts.get(0);
                    conflictMsg.append("⚠️ Giáo viên \"").append(clazz.getTeacher().getFullName())
                               .append("\" đã có lịch dạy buổi khác (")
                               .append(c.getStartTime().format(TIME_FMT))
                               .append(" – ").append(c.getEndTime().format(TIME_FMT))
                               .append(", lớp: ").append(c.getClazz().getClassName()).append(").");
                }
            } catch (Exception ex) {
                // Không chặn lưu nếu kiểm tra lỗi
            }
        }

        // 7. Nếu có xung đột → cảnh báo, cho phép bỏ qua
        if (!conflictMsg.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    conflictMsg + "\n\nBạn có muốn lưu mặc dù có xung đột không?",
                    "Phát hiện xung đột lịch",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        // 8. Gán dữ liệu và đóng dialog
        schedule.setClazz(clazz);
        schedule.setStudyDate(date);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setRoom(selectedRoom);

        saved = true;
        dispose();
    }

    public boolean isSaved()      { return saved; }
    public Schedule getSchedule() { return schedule; }

    // ── Helpers ───────────────────────────────────────────────────────────

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

