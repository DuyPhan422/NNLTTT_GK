package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Room;
import com.company.ems.model.Schedule;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel quản lý lịch học (Schedule) của một lớp cụ thể.
 * Được nhúng bên phải ScheduleManagerPanel.
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
    private TableRowSorter<DefaultTableModel> sorter;

    public SchedulePanel(ScheduleService scheduleService, RoomService roomService) {
        this.scheduleService = scheduleService;
        this.roomService     = roomService;

        this.tableModel   = buildTableModel();
        this.table        = buildTable();
        this.statusLabel  = new JLabel("Chọn một lớp bên trái để xem lịch học");
        this.lblClassInfo = new JLabel("Chưa chọn lớp");
        this.addBtn       = ComponentFactory.primaryButton("+ Thêm buổi học");
        addBtn.setEnabled(false);
        addBtn.addActionListener(e -> openDialog(null));

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
        addBtn.setEnabled(true);
        loadData();
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

        JButton editBtn   = ComponentFactory.secondaryButton("✏️ Sửa buổi học");
        JButton deleteBtn = ComponentFactory.dangerButton("🗑️ Xóa buổi học");
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

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

        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);
        return t;
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
        } catch (Exception e) {
            showError("Không thể tải lịch học: " + e.getMessage());
        }
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { showWarning("Vui lòng chọn một buổi học để sửa."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(scheduleService.findById(id));
    }

    private void deleteSelected() {
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
        try {
            List<Room> rooms = roomService.findAll();
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            ScheduleFormDialog dlg = new ScheduleFormDialog(owner, existing, currentClass, rooms, scheduleService);
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

