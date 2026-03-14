package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Room;
import com.company.ems.model.Schedule;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * Dialog Thêm / Sửa một buổi học (Schedule) của một lớp.
 * Hỗ trợ chọn ngày/giờ qua JSpinner (DateTimePicker-style) hoặc nhập tay.
 * Tự động tìm phòng trống khi thay đổi ngày/giờ.
 */
public class ScheduleFormDialog extends BaseFormDialog<Schedule> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Date picker (spinner + textfield) ─────────────────────────────────
    private final JSpinner   spinnerDate;
    private final JTextField tfDate;

    // ── Time pickers (spinner + textfield) ─────────────────────────────────
    private final JSpinner   spinnerStartTime;
    private final JTextField tfStartTime;
    private final JSpinner   spinnerEndTime;
    private final JTextField tfEndTime;

    // ── Room ───────────────────────────────────────────────────────────────
    private final JComboBox<Room> cbRoom;
    private final JLabel         lblRoomStatus;

    // ── State ──────────────────────────────────────────────────────────────
    private final Schedule        schedule;
    private final Class           clazz;
    private final ScheduleService scheduleService;
    private final RoomService     roomService;
    private boolean               updatingFields = false;

    public ScheduleFormDialog(Frame owner,
                               Schedule existing,
                               Class clazz,
                               RoomService roomService,
                               ScheduleService scheduleService) {
        super(owner, existing != null ? "Sửa buổi học" : "Thêm buổi học mới");
        this.clazz           = clazz;
        this.scheduleService = scheduleService;
        this.roomService     = roomService;
        this.schedule        = existing != null ? existing : new Schedule();

        boolean isEdit = existing != null;

        // ── Date ──────────────────────────────────────────────────────────
        LocalDate initDate = (isEdit && existing.getStudyDate() != null)
                ? existing.getStudyDate() : LocalDate.now();
        spinnerDate = buildDateSpinner(initDate);
        tfDate      = ComponentFactory.formField();
        tfDate.setText(initDate.format(DATE_FMT));
        tfDate.setPreferredSize(new Dimension(110, 34));

        // ── Start time ────────────────────────────────────────────────────
        LocalTime initStart = (isEdit && existing.getStartTime() != null)
                ? existing.getStartTime() : LocalTime.of(8, 0);
        spinnerStartTime = buildTimeSpinner(initStart);
        tfStartTime      = ComponentFactory.formField();
        tfStartTime.setText(initStart.format(TIME_FMT));
        tfStartTime.setPreferredSize(new Dimension(70, 34));

        // ── End time ──────────────────────────────────────────────────────
        LocalTime initEnd = (isEdit && existing.getEndTime() != null)
                ? existing.getEndTime() : LocalTime.of(10, 0);
        spinnerEndTime = buildTimeSpinner(initEnd);
        tfEndTime      = ComponentFactory.formField();
        tfEndTime.setText(initEnd.format(TIME_FMT));
        tfEndTime.setPreferredSize(new Dimension(70, 34));

        // ── Room ──────────────────────────────────────────────────────────
        cbRoom = new JComboBox<>();
        cbRoom.setFont(Theme.FONT_PLAIN);

        lblRoomStatus = new JLabel(" ");
        lblRoomStatus.setFont(Theme.FONT_SMALL);
        lblRoomStatus.setForeground(Theme.TEXT_MUTED);

        // ── Wire listeners ────────────────────────────────────────────────
        wireSpinnerDateSync();
        wireSpinnerTimeSync(spinnerStartTime, tfStartTime);
        wireSpinnerTimeSync(spinnerEndTime, tfEndTime);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(520);

        // Trigger initial room query after UI is built
        SwingUtilities.invokeLater(this::autoQueryRooms);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD FORM
    // ══════════════════════════════════════════════════════════════════════

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

        addRow(form, gbc, 0, "Ngày học *",       buildDateRow(spinnerDate, tfDate));
        addRow(form, gbc, 1, "Giờ bắt đầu *",    buildTimeRow(spinnerStartTime, tfStartTime));
        addRow(form, gbc, 2, "Giờ kết thúc *",    buildTimeRow(spinnerEndTime, tfEndTime));
        addRow(form, gbc, 3, "Phòng học (tự lọc phòng trống)", cbRoom);

        // Room status hint
        gbc.gridy = 4 * 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        form.add(lblRoomStatus, gbc);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VALIDATE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected boolean validateForm() {
        LocalDate date;
        try {
            date = getDateValue();
        } catch (Exception ex) {
            setError("Ngày học không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfDate.requestFocus();
            return false;
        }

        LocalTime startTime;
        try {
            startTime = getStartTimeValue();
        } catch (Exception ex) {
            setError("Giờ bắt đầu không hợp lệ. Vui lòng nhập HH:mm.");
            tfStartTime.requestFocus();
            return false;
        }

        LocalTime endTime;
        try {
            endTime = getEndTimeValue();
        } catch (Exception ex) {
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

        // Check teacher conflicts
        Room selectedRoom = (Room) cbRoom.getSelectedItem();
        Long excludeId = schedule.getScheduleId();
        StringBuilder conflictMsg = new StringBuilder();

        if (clazz.getTeacher() != null) {
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

    // ══════════════════════════════════════════════════════════════════════
    //  COMMIT
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void commitToEntity() {
        schedule.setClazz(clazz);
        try { schedule.setStudyDate(getDateValue()); } catch (Exception ignored) {}
        try { schedule.setStartTime(getStartTimeValue()); } catch (Exception ignored) {}
        try { schedule.setEndTime(getEndTimeValue()); } catch (Exception ignored) {}
        schedule.setRoom((Room) cbRoom.getSelectedItem());
    }

    @Override
    public Schedule getEntity() {
        return schedule;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AUTO-QUERY AVAILABLE ROOMS
    // ══════════════════════════════════════════════════════════════════════

    private void autoQueryRooms() {
        LocalDate date;
        LocalTime startTime, endTime;
        try {
            date      = getDateValue();
            startTime = getStartTimeValue();
            endTime   = getEndTimeValue();
        } catch (Exception e) {
            lblRoomStatus.setText("⚠ Nhập đúng ngày/giờ để tìm phòng trống");
            lblRoomStatus.setForeground(Theme.TEXT_MUTED);
            return;
        }

        if (date == null || startTime == null || endTime == null) return;
        if (!endTime.isAfter(startTime)) {
            lblRoomStatus.setText("⚠ Giờ kết thúc phải sau giờ bắt đầu");
            lblRoomStatus.setForeground(Theme.DANGER);
            return;
        }

        Long excludeId = schedule.getScheduleId();

        try {
            List<Room> allRooms = roomService.findByStatus(ActiveStatus.HOAT_DONG.getValue());

            List<Room> available = allRooms.stream()
                    .filter(room -> scheduleService.checkRoomConflicts(
                            room.getRoomId(), date, startTime, endTime, excludeId).isEmpty())
                    .toList();

            Room previouslySelected = (Room) cbRoom.getSelectedItem();
            cbRoom.removeAllItems();
            available.forEach(cbRoom::addItem);

            // Restore previous selection if still available
            boolean restored = false;
            if (previouslySelected != null) {
                restored = available.stream()
                        .filter(r -> r.getRoomId().equals(previouslySelected.getRoomId()))
                        .findFirst()
                        .map(r -> { cbRoom.setSelectedItem(r); return true; })
                        .orElse(false);
            }
            if (!restored && clazz.getRoom() != null) {
                // Default to class's default room if available
                restored = available.stream()
                        .filter(r -> r.getRoomId().equals(clazz.getRoom().getRoomId()))
                        .findFirst()
                        .map(r -> { cbRoom.setSelectedItem(r); return true; })
                        .orElse(false);
            }
            // Fallback: chọn phòng đầu tiên nếu chưa chọn được
            if (!restored && !available.isEmpty()) {
                cbRoom.setSelectedIndex(0);
            }

            lblRoomStatus.setText("✅ " + available.size() + " phòng trống");
            lblRoomStatus.setForeground(Theme.GREEN);
        } catch (Exception ex) {
            lblRoomStatus.setText("Lỗi tìm phòng: " + ex.getMessage());
            lblRoomStatus.setForeground(Theme.DANGER);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SPINNER ↔ TEXTFIELD SYNC WIRING
    // ══════════════════════════════════════════════════════════════════════

    private void wireSpinnerDateSync() {
        spinnerDate.addChangeListener(e -> {
            if (updatingFields) return;
            updatingFields = true;
            tfDate.setText(spinnerToLocalDate(spinnerDate).format(DATE_FMT));
            updatingFields = false;
            autoQueryRooms();
        });

        tfDate.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (updatingFields) return;
                try {
                    LocalDate d = LocalDate.parse(tfDate.getText().trim(), DATE_FMT);
                    updatingFields = true;
                    spinnerDate.setValue(localDateToDate(d));
                    updatingFields = false;
                    autoQueryRooms();
                } catch (DateTimeParseException ignored) {}
            }
        });
    }

    private void wireSpinnerTimeSync(JSpinner spinner, JTextField tf) {
        spinner.addChangeListener(e -> {
            if (updatingFields) return;
            updatingFields = true;
            tf.setText(spinnerToLocalTime(spinner).format(TIME_FMT));
            updatingFields = false;
            autoQueryRooms();
        });

        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (updatingFields) return;
                try {
                    LocalTime t = LocalTime.parse(tf.getText().trim(), TIME_FMT);
                    updatingFields = true;
                    spinner.setValue(localTimeToDate(t));
                    updatingFields = false;
                    autoQueryRooms();
                } catch (DateTimeParseException ignored) {}
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VALUE GETTERS (from spinners or textfields)
    // ══════════════════════════════════════════════════════════════════════

    private LocalDate getDateValue() {
        String txt = tfDate.getText().trim();
        if (!txt.isEmpty()) {
            try { return LocalDate.parse(txt, DATE_FMT); }
            catch (DateTimeParseException ignored) {}
        }
        return spinnerToLocalDate(spinnerDate);
    }

    private LocalTime getStartTimeValue() {
        String txt = tfStartTime.getText().trim();
        if (!txt.isEmpty()) {
            try { return LocalTime.parse(txt, TIME_FMT); }
            catch (DateTimeParseException ignored) {}
        }
        return spinnerToLocalTime(spinnerStartTime);
    }

    private LocalTime getEndTimeValue() {
        String txt = tfEndTime.getText().trim();
        if (!txt.isEmpty()) {
            try { return LocalTime.parse(txt, TIME_FMT); }
            catch (DateTimeParseException ignored) {}
        }
        return spinnerToLocalTime(spinnerEndTime);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildDateRow(JSpinner spinner, JTextField tf) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        spinner.setPreferredSize(new Dimension(64, 34));
        JLabel calIcon = new JLabel("📅");
        calIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        row.add(calIcon);
        row.add(spinner);
        JLabel orLbl = new JLabel("hoặc nhập:");
        orLbl.setFont(Theme.FONT_SMALL);
        orLbl.setForeground(Theme.TEXT_MUTED);
        row.add(orLbl);
        row.add(tf);
        return row;
    }

    private JPanel buildTimeRow(JSpinner spinner, JTextField tf) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        spinner.setPreferredSize(new Dimension(70, 34));
        JLabel clockIcon = new JLabel("⏰");
        clockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        row.add(clockIcon);
        row.add(spinner);
        JLabel orLbl = new JLabel("hoặc nhập:");
        orLbl.setFont(Theme.FONT_SMALL);
        orLbl.setForeground(Theme.TEXT_MUTED);
        row.add(orLbl);
        row.add(tf);
        return row;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        panel.add(ComponentFactory.formLabel(label), gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        if (!(field instanceof JPanel)) field.setPreferredSize(new Dimension(0, 36));
        panel.add(field, gbc);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SPINNER BUILDERS & CONVERTERS
    // ══════════════════════════════════════════════════════════════════════

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel(
                localDateToDate(initial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner sp = new JSpinner(model);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setFont(Theme.FONT_PLAIN);
        return sp;
    }

    private JSpinner buildTimeSpinner(LocalTime initial) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, initial.getHour());
        cal.set(java.util.Calendar.MINUTE, initial.getMinute());
        cal.set(java.util.Calendar.SECOND, 0);
        SpinnerDateModel model = new SpinnerDateModel(
                cal.getTime(), null, null, java.util.Calendar.MINUTE);
        JSpinner sp = new JSpinner(model);
        sp.setEditor(new JSpinner.DateEditor(sp, "HH:mm"));
        sp.setFont(Theme.FONT_PLAIN);
        return sp;
    }

    private Date localDateToDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date localTimeToDate(LocalTime lt) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, lt.getHour());
        cal.set(java.util.Calendar.MINUTE, lt.getMinute());
        cal.set(java.util.Calendar.SECOND, 0);
        return cal.getTime();
    }

    private LocalDate spinnerToLocalDate(JSpinner sp) {
        Date d = (Date) sp.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime spinnerToLocalTime(JSpinner sp) {
        Date d = (Date) sp.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                .withSecond(0).withNano(0);
    }
}
