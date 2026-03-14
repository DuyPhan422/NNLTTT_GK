package com.company.ems.ui.panels.student;

import com.company.ems.model.Class;
import com.company.ems.model.Schedule;
import com.company.ems.model.enums.ClassStatus;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.dialogs.ScheduleFormDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel quản lý lịch học (Schedule) của một lớp cụ thể.
 * Được nhúng bên phải ScheduleManagerPanel.
 *
 * Hỗ trợ:
 * - CRUD theo trạng thái lớp (Lên kế hoạch: full; Mở lớp/Đang diễn ra: thêm+sửa; Hoàn thành/Hủy: read-only)
 * - Double-click dòng → mở modal sửa
 * - Combobox chọn tuần (khi course đơn vị Tuần/Tháng)
 */
public class SchedulePanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] COLUMNS = {
            "ID", "STT", "Ngày học", "Thứ", "Giờ bắt đầu", "Giờ kết thúc", "Phòng học", "Thời lượng"
    };

    private final ScheduleService scheduleService;
    private final RoomService     roomService;
    private Class currentClass;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JLabel lblClassInfo;
    private final JButton addBtn;
    private final JButton editBtn;
    private final JButton deleteBtn;
    private final JComboBox<String> cbWeek;
    private TableRowSorter<DefaultTableModel> sorter;

    // Week ranges for filter — index 0 = "Tất cả", index N = [startDate, endDate]
    private List<LocalDate[]> weekRanges;

    public SchedulePanel(ScheduleService scheduleService, RoomService roomService) {
        this.scheduleService = scheduleService;
        this.roomService     = roomService;

        this.tableModel   = buildTableModel();
        this.table        = buildTable();
        this.statusLabel  = new JLabel("Chọn một lớp bên trái để xem lịch học");
        this.lblClassInfo = new JLabel("Chưa chọn lớp");
        this.addBtn       = ComponentFactory.primaryButton("+ Thêm buổi học");
        this.editBtn      = ComponentFactory.secondaryButton("✏️ Sửa buổi học");
        this.deleteBtn    = ComponentFactory.dangerButton("🗑️ Xóa buổi học");
        this.cbWeek       = new JComboBox<>();

        addBtn.setEnabled(false);
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        addBtn.addActionListener(e -> openDialog(null));
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

        cbWeek.setFont(Theme.FONT_PLAIN);
        cbWeek.setVisible(false);
        cbWeek.addActionListener(e -> filterByWeek());

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
    }

    /** Gọi từ ScheduleManagerPanel khi người dùng chọn một lớp */
    public void loadForClass(Class clazz) {
        this.currentClass = clazz;
        String teacher = clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : "—";
        String room    = clazz.getRoom()    != null ? clazz.getRoom().getRoomName()    : "—";
        String course  = clazz.getCourse()  != null ? clazz.getCourse().getCourseName(): "—";
        lblClassInfo.setText("Lớp: " + clazz.getClassName()
                + "   |   Khóa học: " + course
                + "   |   Giáo viên: " + teacher
                + "   |   Phòng mặc định: " + room);

        updateButtonsByClassStatus(clazz.getStatus());
        buildWeekSelector();
        loadData();
    }

    // ── CRUD Permission by Class Status ──────────────────────────────────

    private void updateButtonsByClassStatus(String status) {
        ClassStatus cs = ClassStatus.fromValue(status);
        switch (cs) {
            case LEN_KE_HOACH -> {
                addBtn.setEnabled(true);  addBtn.setVisible(true);
                editBtn.setEnabled(true); editBtn.setVisible(true);
                deleteBtn.setEnabled(true); deleteBtn.setVisible(true);
            }
            case MO_LOP, DANG_DIEN_RA -> {
                addBtn.setEnabled(true);  addBtn.setVisible(true);
                editBtn.setEnabled(true); editBtn.setVisible(true);
                deleteBtn.setEnabled(false); deleteBtn.setVisible(false);
            }
            case HOAN_THANH, HUY_LOP -> {
                addBtn.setEnabled(false); addBtn.setVisible(false);
                editBtn.setEnabled(false); editBtn.setVisible(false);
                deleteBtn.setEnabled(false); deleteBtn.setVisible(false);
            }
        }
    }

    // ── Build UI ──────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel infoBar = new JPanel(new BorderLayout());
        infoBar.setBackground(Theme.TODAY_BG);
        infoBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.TODAY_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        lblClassInfo.setFont(Theme.FONT_SMALL);
        lblClassInfo.setForeground(Theme.PRIMARY);
        infoBar.add(lblClassInfo, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        toolbar.add(cbWeek, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);

        header.add(infoBar,  BorderLayout.NORTH);
        header.add(toolbar,  BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public java.lang.Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 0 -> Long.class;
                    case 1 -> Integer.class;
                    default -> String.class;
                };
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                c.setForeground(Theme.TEXT_MAIN);
                return c;
            }
        };

        t.setFont(Theme.FONT_PLAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Theme.BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        var baseRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, value, isSel, hasFocus, row, col) -> {
            Component comp = baseRenderer.getTableCellRendererComponent(tbl, value, isSel, hasFocus, row, col);
            if (comp instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return comp;
        });

        // Ẩn cột ID (index 0)
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        // Chỉnh cột STT sát lề trái
        t.getColumnModel().getColumn(1).setMinWidth(50);
        t.getColumnModel().getColumn(1).setMaxWidth(70);
        t.getColumnModel().getColumn(1).setPreferredWidth(50);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);

        // Double-click → mở modal sửa
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) {
                    editSelected();
                }
            }
        });

        return t;
    }

    // ── Week Selector ─────────────────────────────────────────────────────

    private void buildWeekSelector() {
        cbWeek.removeAllItems();
        weekRanges = new ArrayList<>();

        if (currentClass == null || currentClass.getCourse() == null) {
            cbWeek.setVisible(false);
            return;
        }

        String durationUnit = currentClass.getCourse().getDurationUnit();
        if (durationUnit == null || "Giờ".equals(durationUnit)) {
            cbWeek.setVisible(false);
            return;
        }

        LocalDate start = currentClass.getStartDate();
        LocalDate end   = currentClass.getEndDate();
        if (start == null || end == null) {
            cbWeek.setVisible(false);
            return;
        }

        cbWeek.addItem("Tất cả tuần");
        weekRanges.add(null); // index 0 = all

        int weekNum = 1;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDate weekEnd = cursor.plusDays(6).isAfter(end) ? end : cursor.plusDays(6);
            cbWeek.addItem("Tuần " + weekNum + " (" + cursor.format(DATE_FMT) + " – " + weekEnd.format(DATE_FMT) + ")");
            weekRanges.add(new LocalDate[]{cursor, weekEnd});
            cursor = cursor.plusWeeks(1);
            weekNum++;
        }
        cbWeek.setVisible(true);
    }

    private void filterByWeek() {
        int selected = cbWeek.getSelectedIndex();
        if (selected <= 0 || weekRanges == null || selected >= weekRanges.size()) {
            sorter.setRowFilter(null);
        } else {
            LocalDate[] range = weekRanges.get(selected);
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    String dateStr = (String) entry.getValue(2);
                    try {
                        LocalDate d = LocalDate.parse(dateStr, DATE_FMT);
                        return !d.isBefore(range[0]) && !d.isAfter(range[1]);
                    } catch (Exception e) {
                        return true;
                    }
                }
            });
        }
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " buổi học");
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void loadData() {
        if (currentClass == null) return;
        try {
            List<Schedule> list = scheduleService.findByClassId(currentClass.getClassId());
            tableModel.setRowCount(0);

            var index = new java.util.concurrent.atomic.AtomicInteger(1);
            list.stream()
                .map(s -> {
                    String dayOfWeek = s.getStudyDate() != null
                            ? getDayOfWeekVi(s.getStudyDate().getDayOfWeek()) : "";
                    long minutes = (s.getStartTime() != null && s.getEndTime() != null)
                            ? Duration.between(s.getStartTime(), s.getEndTime()).toMinutes() : 0;
                    return new Object[]{
                            s.getScheduleId(),
                            index.getAndIncrement(),
                            s.getStudyDate()  != null ? s.getStudyDate().format(DATE_FMT) : "",
                            dayOfWeek,
                            s.getStartTime()  != null ? s.getStartTime().format(TIME_FMT) : "",
                            s.getEndTime()    != null ? s.getEndTime().format(TIME_FMT)   : "",
                            s.getRoom()       != null ? s.getRoom().getRoomName()          : "—",
                            minutes > 0 ? minutes + " phút" : ""
                    };
                })
                .forEach(tableModel::addRow);

            statusLabel.setText("Tổng: " + list.size() + " buổi học");

            // Re-apply week filter if selected
            if (cbWeek.isVisible() && cbWeek.getSelectedIndex() > 0) {
                filterByWeek();
            }
        } catch (Exception e) {
            showError("Không thể tải lịch học: " + e.getMessage());
        }
    }

    private void editSelected() {
        if (currentClass == null) return;
        // Block editing for completed/cancelled
        ClassStatus cs = ClassStatus.fromValue(currentClass.getStatus());
        if (cs == ClassStatus.HOAN_THANH || cs == ClassStatus.HUY_LOP) return;

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { showWarning("Vui lòng chọn một buổi học để sửa."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(scheduleService.findById(id));
    }

    private void deleteSelected() {
        if (currentClass == null) return;
        // Only allow delete for "Lên kế hoạch"
        ClassStatus cs = ClassStatus.fromValue(currentClass.getStatus());
        if (cs != ClassStatus.LEN_KE_HOACH) return;

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { showWarning("Vui lòng chọn một buổi học để xóa."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long   id   = (Long)   tableModel.getValueAt(modelRow, 0);
        String date = (String) tableModel.getValueAt(modelRow, 2);
        String time = (String) tableModel.getValueAt(modelRow, 4);

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa buổi học ngày " + date + " lúc " + time + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            scheduleService.delete(id);
            loadData();
            showSuccess("Đã xóa buổi học thành công.");
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    private void openDialog(Schedule existing) {
        if (currentClass == null) { showWarning("Vui lòng chọn lớp học trước."); return; }
        // Block dialog for completed/cancelled
        ClassStatus cs = ClassStatus.fromValue(currentClass.getStatus());
        if (cs == ClassStatus.HOAN_THANH || cs == ClassStatus.HUY_LOP) return;

        try {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            ScheduleFormDialog dlg = new ScheduleFormDialog(owner, existing, currentClass, roomService, scheduleService);
            dlg.setVisible(true);
            if (!dlg.isSaved()) return;

            if (existing != null) {
                scheduleService.update(dlg.getEntity());
                showSuccess("Cập nhật buổi học thành công.");
            } else {
                scheduleService.save(dlg.getEntity());
                showSuccess("Thêm buổi học mới thành công.");
            }
            loadData();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String getDayOfWeekVi(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "Thứ Hai";
            case TUESDAY   -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY  -> "Thứ Năm";
            case FRIDAY    -> "Thứ Sáu";
            case SATURDAY  -> "Thứ Bảy";
            case SUNDAY    -> "Chủ Nhật";
        };
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo",   JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi",        JOptionPane.ERROR_MESSAGE);
    }
}
