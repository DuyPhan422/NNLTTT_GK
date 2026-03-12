package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Room;
import com.company.ems.model.Schedule;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

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
public class ScheduleFormDialog extends BaseFormDialog<Schedule> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextField   tfDate;
    private final JTextField   tfStartTime;
    private final JTextField   tfEndTime;
    private final JComboBox<Room> cbRoom;

    private final Schedule        schedule;
    private final Class           clazz;
    private final ScheduleService scheduleService;

    public ScheduleFormDialog(Frame owner,
                               Schedule existing,
                               Class clazz,
                               List<Room> rooms,
                               ScheduleService scheduleService) {
        super(owner, existing != null ? "Sửa buổi học" : "Thêm buổi học mới");
        this.clazz           = clazz;
        this.scheduleService = scheduleService;
        this.schedule        = existing != null ? existing : new Schedule();

        boolean isEdit = existing != null;

        tfDate      = ComponentFactory.formField();
        tfStartTime = ComponentFactory.formField();
        tfEndTime   = ComponentFactory.formField();

        if (isEdit) {
            if (existing.getStudyDate() != null) tfDate.setText(existing.getStudyDate().format(DATE_FMT));
            if (existing.getStartTime() != null) tfStartTime.setText(existing.getStartTime().format(TIME_FMT));
            if (existing.getEndTime() != null) tfEndTime.setText(existing.getEndTime().format(TIME_FMT));
        }

        cbRoom = new JComboBox<>(rooms.toArray(new Room[0]));
        cbRoom.setFont(Theme.FONT_PLAIN);
        cbRoom.insertItemAt(null, 0);
        cbRoom.setSelectedIndex(0);
        if (isEdit && existing.getRoom() != null) {
            cbRoom.setSelectedItem(existing.getRoom());
        } else if (clazz.getRoom() != null) {
            cbRoom.setSelectedItem(clazz.getRoom());
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(480);
    }

    @Override
    protected JPanel buildForm() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        // Class info header
        JLabel lblClass = new JLabel("Lớp: " + clazz.getClassName());
        lblClass.setFont(Theme.FONT_BOLD);
        lblClass.setForeground(Theme.PRIMARY);
        wrapper.add(lblClass, BorderLayout.NORTH);

        // Form fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Ngày học (dd/MM/yyyy) *", tfDate);
        addRow(form, gbc, 1, "Giờ bắt đầu (HH:mm) *",  tfStartTime);
        addRow(form, gbc, 2, "Giờ kết thúc (HH:mm) *",  tfEndTime);
        addRow(form, gbc, 3, "Phòng học",                cbRoom);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    @Override
    protected boolean validateForm() {
        LocalDate date;
        try {
            date = LocalDate.parse(tfDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            setError("Ngày học không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfDate.requestFocus();
            return false;
        }

        LocalTime startTime;
        try {
            startTime = LocalTime.parse(tfStartTime.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            setError("Giờ bắt đầu không hợp lệ. Vui lòng nhập HH:mm.");
            tfStartTime.requestFocus();
            return false;
        }

        LocalTime endTime;
        try {
            endTime = LocalTime.parse(tfEndTime.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            setError("Giờ kết thúc không hợp lệ. Vui lòng nhập HH:mm.");
            tfEndTime.requestFocus();
            return false;
        }

        if (!endTime.isAfter(startTime)) {
            setError("Giờ kết thúc phải sau giờ bắt đầu.");
            tfEndTime.requestFocus();
            return false;
        }

        if (clazz.getStartDate() != null && date.isBefore(clazz.getStartDate())) {
            setError("Ngày học không được trước ngày khai giảng lớp ("
                    + clazz.getStartDate().format(DATE_FMT) + ").");
            tfDate.requestFocus();
            return false;
        }
        if (clazz.getEndDate() != null && date.isAfter(clazz.getEndDate())) {
            setError("Ngày học không được sau ngày kết thúc lớp ("
                    + clazz.getEndDate().format(DATE_FMT) + ").");
            tfDate.requestFocus();
            return false;
        }

        // Check conflicts
        Long excludeId = schedule.getScheduleId();
        Room selectedRoom = (Room) cbRoom.getSelectedItem();
        StringBuilder conflictMsg = new StringBuilder();

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
            } catch (Exception ignored) {}
        }

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
            } catch (Exception ignored) {}
        }

        if (!conflictMsg.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    conflictMsg + "\n\nBạn có muốn lưu mặc dù có xung đột không?",
                    "Phát hiện xung đột lịch",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return choice == JOptionPane.YES_OPTION;
        }

        return true;
    }

    @Override
    protected void commitToEntity() {
        schedule.setClazz(clazz);

        try {
            schedule.setStudyDate(LocalDate.parse(tfDate.getText().trim(), DATE_FMT));
        } catch (DateTimeParseException ignored) {}

        try {
            schedule.setStartTime(LocalTime.parse(tfStartTime.getText().trim(), TIME_FMT));
        } catch (DateTimeParseException ignored) {}

        try {
            schedule.setEndTime(LocalTime.parse(tfEndTime.getText().trim(), TIME_FMT));
        } catch (DateTimeParseException ignored) {}

        schedule.setRoom((Room) cbRoom.getSelectedItem());
    }

    @Override
    public Schedule getEntity() {
        return schedule;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        panel.add(ComponentFactory.formLabel(label), gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(0, 36));
        panel.add(field, gbc);
    }
}

