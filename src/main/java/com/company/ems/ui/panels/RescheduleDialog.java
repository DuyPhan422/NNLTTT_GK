package com.company.ems.ui.panels;

import com.company.ems.model.Room;
import com.company.ems.model.Schedule;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog Dời lịch buổi học (Reschedule).
 * SRP: chỉ đảm nhận UI chọn ngày/giờ mới + tìm phòng trống + xác nhận update.
 *
 * Flow:
 *  1. Chọn ngày mới (spinner)
 *  2. Chọn giờ bắt đầu - giờ kết thúc (spinner)
 *  3. Bấm "Tìm phòng trống" → query checkRoomConflicts → hiện danh sách phòng available
 *  4. Chọn phòng → bấm "Xác nhận" → scheduleService.update()
 */
public class RescheduleDialog extends JDialog {

    // ── Design tokens → Theme / ComponentFactory ──────────────────────────
    private static final Color WARN_BG     = new Color(254, 252, 232);
    private static final Color WARN_BORDER = new Color(253, 224, 71);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Services ──────────────────────────────────────────────────────────
    private final ScheduleService scheduleService;
    private final RoomService     roomService;

    // ── State ─────────────────────────────────────────────────────────────
    private final Schedule originalSchedule;
    private boolean        saved = false;

    // ── UI ────────────────────────────────────────────────────────────────
    private final JSpinner          spinnerDate;
    private final JSpinner          spinnerStart;
    private final JSpinner          spinnerEnd;
    private final JComboBox<Room>   cbRoom;
    private final JLabel            lblStatus;
    private final JButton           btnFindRooms;
    private final JButton           btnConfirm;

    public RescheduleDialog(Frame owner, Schedule schedule,
                            ScheduleService scheduleService, RoomService roomService) {
        super(owner, "Dời lịch buổi học", true);
        this.scheduleService  = scheduleService;
        this.roomService      = roomService;
        this.originalSchedule = schedule;

        // Initialize spinners with current schedule values
        spinnerDate  = buildDateSpinner(schedule.getStudyDate());
        spinnerStart = buildTimeSpinner(schedule.getStartTime());
        spinnerEnd   = buildTimeSpinner(schedule.getEndTime());

        cbRoom = new JComboBox<>();
        cbRoom.setFont(Theme.FONT_PLAIN);
        cbRoom.setEnabled(false); // enabled after "Tìm phòng trống"

        lblStatus = new JLabel(" ");
        lblStatus.setFont(Theme.FONT_SMALL);

        btnFindRooms = buildPrimaryBtn("🔍 Tìm phòng trống");
        btnFindRooms.addActionListener(e -> findAvailableRooms());

        btnConfirm = buildPrimaryBtn("✅ Xác nhận dời lịch");
        btnConfirm.setEnabled(false);
        btnConfirm.setBackground(Theme.GREEN);
        btnConfirm.addActionListener(e -> doReschedule());

        buildUI();
        pack();
        setMinimumSize(new Dimension(500, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ── UI builder ────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG_CARD);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── Warning banner ────────────────────────────────────────────────
        JPanel warn = new JPanel(new BorderLayout(8, 0));
        warn.setBackground(WARN_BG);
        warn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WARN_BORDER),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel warnLbl = new JLabel("<html><b>⚠ Dời lịch buổi học</b><br>"
                + "<span style='color:#78716c;'>Buổi gốc: "
                + originalSchedule.getStudyDate().format(DATE_FMT)
                + " &nbsp;|&nbsp; "
                + originalSchedule.getStartTime().format(TIME_FMT)
                + " – " + originalSchedule.getEndTime().format(TIME_FMT)
                + " &nbsp;|&nbsp; Phòng: "
                + (originalSchedule.getRoom() != null ? originalSchedule.getRoom().getRoomName() : "Chưa có")
                + "</span></html>");
        warnLbl.setFont(Theme.FONT_SMALL);
        warn.add(warnLbl, BorderLayout.CENTER);
        root.add(warn, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(6, 0, 6, 0);

        // Row 0 — Ngày mới
        g.gridy = 0; form.add(lbl("Ngày học mới"), g);
        g.gridy = 1; form.add(spinnerDate, g);

        // Row 1 — Giờ bắt đầu + kết thúc
        g.gridy = 2; form.add(lbl("Khung giờ"), g);
        g.gridy = 3;
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        timeRow.setOpaque(false);
        timeRow.add(spinnerStart);
        timeRow.add(new JLabel("→"));
        timeRow.add(spinnerEnd);
        timeRow.add(Box.createHorizontalStrut(10));
        timeRow.add(btnFindRooms);
        form.add(timeRow, g);

        // Row 2 — Phòng học
        g.gridy = 4; form.add(lbl("Phòng học trống"), g);
        g.gridy = 5; form.add(cbRoom, g);

        // Row 3 — Status
        g.gridy = 6; form.add(lblStatus, g);

        root.add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton btnCancel = ComponentFactory.secondaryButton("Hủy");
        btnCancel.addActionListener(e -> dispose());

        btnRow.add(btnCancel);
        btnRow.add(btnConfirm);
        root.add(btnRow, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Business logic ────────────────────────────────────────────────────

    private void findAvailableRooms() {
        LocalDate  date  = spinnerToLocalDate(spinnerDate);
        LocalTime  start = spinnerToLocalTime(spinnerStart);
        LocalTime  end   = spinnerToLocalTime(spinnerEnd);

        if (!end.isAfter(start)) {
            showStatus("⚠ Giờ kết thúc phải sau giờ bắt đầu.", Theme.DANGER);
            return;
        }

        try {
            // Lấy tất cả phòng Active
            List<Room> allRooms = roomService.findByStatus(com.company.ems.model.enums.ActiveStatus.HOAT_DONG.getValue());

            // Lọc phòng không có conflict — Stream API
            List<Room> availableRooms = allRooms.stream()
                    .filter(room -> {
                        List<Schedule> conflicts = scheduleService.checkRoomConflicts(
                                room.getRoomId(), date, start, end,
                                originalSchedule.getScheduleId()); // exclude chính nó
                        return conflicts.isEmpty();
                    })
                    .collect(Collectors.toList());

            cbRoom.removeAllItems();
            if (availableRooms.isEmpty()) {
                showStatus("❌ Không có phòng trống trong khung giờ này.", Theme.DANGER);
                cbRoom.setEnabled(false);
                btnConfirm.setEnabled(false);
            } else {
                availableRooms.forEach(cbRoom::addItem);
                // Pre-select phòng cũ nếu vẫn available
                if (originalSchedule.getRoom() != null) {
                    Long oldRoomId = originalSchedule.getRoom().getRoomId();
                    availableRooms.stream()
                            .filter(r -> r.getRoomId().equals(oldRoomId))
                            .findFirst()
                            .ifPresent(cbRoom::setSelectedItem);
                }
                cbRoom.setEnabled(true);
                btnConfirm.setEnabled(true);
                showStatus("✅ Tìm thấy " + availableRooms.size() + " phòng trống.", Theme.GREEN);
            }
        } catch (Exception ex) {
            showStatus("Lỗi khi tìm phòng: " + ex.getMessage(), Theme.DANGER);
        }
    }

    private void doReschedule() {
        LocalDate newDate  = spinnerToLocalDate(spinnerDate);
        LocalTime newStart = spinnerToLocalTime(spinnerStart);
        LocalTime newEnd   = spinnerToLocalTime(spinnerEnd);
        Room      newRoom  = (Room) cbRoom.getSelectedItem();

        if (!newEnd.isAfter(newStart)) {
            showStatus("⚠ Giờ kết thúc phải sau giờ bắt đầu.", Theme.DANGER);
            return;
        }

        // Re-check conflict at confirm time (optimistic locking guard)
        if (newRoom != null) {
            List<Schedule> conflicts = scheduleService.checkRoomConflicts(
                    newRoom.getRoomId(), newDate, newStart, newEnd,
                    originalSchedule.getScheduleId());
            if (!conflicts.isEmpty()) {
                showStatus("⚠ Phòng đã bị đặt sau khi tìm kiếm. Vui lòng tìm lại.", Theme.DANGER);
                btnConfirm.setEnabled(false);
                return;
            }
        }

        try {
            originalSchedule.setStudyDate(newDate);
            originalSchedule.setStartTime(newStart);
            originalSchedule.setEndTime(newEnd);
            originalSchedule.setRoom(newRoom);
            scheduleService.update(originalSchedule);
            saved = true;
            dispose();
        } catch (Exception ex) {
            showStatus("Lỗi khi lưu: " + ex.getMessage(), Theme.DANGER);
        }
    }

    public boolean isSaved() { return saved; }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel m = new SpinnerDateModel(
                Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner sp = new JSpinner(m);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setFont(Theme.FONT_PLAIN);
        sp.setPreferredSize(new Dimension(140, 34));
        return sp;
    }

    private JSpinner buildTimeSpinner(LocalTime initial) {
        // Use SpinnerDateModel and show only HH:mm
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, initial.getHour());
        cal.set(java.util.Calendar.MINUTE, initial.getMinute());
        cal.set(java.util.Calendar.SECOND, 0);
        SpinnerDateModel m = new SpinnerDateModel(
                cal.getTime(), null, null, java.util.Calendar.MINUTE);
        JSpinner sp = new JSpinner(m);
        sp.setEditor(new JSpinner.DateEditor(sp, "HH:mm"));
        sp.setFont(Theme.FONT_PLAIN);
        sp.setPreferredSize(new Dimension(80, 34));
        return sp;
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

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private JButton buildPrimaryBtn(String text) {
        return ComponentFactory.primaryButton(text);
    }

    private void showStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}

