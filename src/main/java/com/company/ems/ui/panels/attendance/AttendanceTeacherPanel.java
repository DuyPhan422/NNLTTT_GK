package com.company.ems.ui.panels.attendance;

import com.company.ems.model.Attendance;
import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.model.Teacher;
import com.company.ems.service.AttendanceService;
import com.company.ems.service.ClassService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel điểm danh dành cho Teacher.
 * Layout: Sidebar chọn lớp (chỉ lớp mình dạy) | Panel điểm danh theo buổi
 *
 * Luồng: Chọn lớp → Chọn ngày buổi học → Load DS học viên enrolled
 *        → Tick Present/Absent/Late → Lưu hàng loạt
 */
public class AttendanceTeacherPanel extends JPanel {

    // ── Design tokens ─────────────────────────────────────────────────────
    private static final Color BG_PAGE     = new Color(248, 250, 252);
    private static final Color BG_SIDEBAR  = Color.WHITE;
    private static final Color BG_CARD     = Color.WHITE;
    private static final Color BORDER_COL  = new Color(226, 232, 240);
    private static final Color PRIMARY     = new Color(37,  99,  235);
    private static final Color PRIMARY_H   = new Color(29,  78,  216);
    private static final Color GREEN       = new Color(22,  163, 74);
    private static final Color AMBER       = new Color(217, 119, 6);
    private static final Color RED         = new Color(220, 38,  38);
    private static final Color TEXT_MAIN   = new Color(15,  23,  42);
    private static final Color TEXT_MUTED  = new Color(100, 116, 139);
    private static final Color ITEM_HOVER  = new Color(239, 246, 255);
    private static final Color ITEM_ACTIVE = new Color(219, 234, 254);
    private static final Color ROW_EVEN    = Color.WHITE;
    private static final Color ROW_ODD     = new Color(248, 250, 252);
    private static final Color ROW_SELECT  = new Color(219, 234, 254);

    private static final Font FONT_MAIN   = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,   14);

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            java.util.Locale.forLanguageTag("vi"));

    private static final String[] STATUS_OPTIONS = {"Present", "Absent", "Late"};
    private static final String[] TABLE_COLS = {"STT", "Họ và tên", "Trạng thái", "Ghi chú"};

    // ── Services ──────────────────────────────────────────────────────────
    private final AttendanceService attendanceService;
    private final ClassService      classService;
    private final Teacher           currentTeacher; // null = xem tất cả (dev mode admin)

    // ── State ─────────────────────────────────────────────────────────────
    private List<Class>      allClasses     = new ArrayList<>();
    private Class            selectedClass  = null;
    private List<Attendance> currentSheet   = new ArrayList<>(); // sheet đang chỉnh sửa
    private JPanel           activeClassItem = null;

    // ── UI components ─────────────────────────────────────────────────────
    private final JPanel     classListPanel;
    private final JTextField searchField;
    // Right side
    private final JLabel     lblClassName;
    private final JLabel     lblDateDisplay;
    private final JSpinner   datePicker;
    private final JButton    btnMarkAll;
    private final JButton    btnSave;
    private final JLabel     statusLabel;
    private final JLabel     statPresent;
    private final JLabel     statAbsent;
    private final JLabel     statLate;
    private final DefaultTableModel tableModel;
    private final JTable     table;
    private final JPanel     rightPanel;
    private final JPanel     emptyState;

    public AttendanceTeacherPanel(AttendanceService attendanceService,
                                  ClassService classService,
                                  Teacher currentTeacher) {
        this.attendanceService = attendanceService;
        this.classService      = classService;
        this.currentTeacher    = currentTeacher;

        // Build components
        classListPanel = new JPanel();
        searchField    = new JTextField();
        lblClassName   = new JLabel("Chưa chọn lớp");
        lblDateDisplay = new JLabel("");
        datePicker     = buildDateSpinner(LocalDate.now());
        btnMarkAll     = createSecondaryButton("✅ Đánh dấu tất cả Có mặt");
        btnSave        = createPrimaryButton("💾 Lưu điểm danh");
        statusLabel    = new JLabel();
        statPresent    = statBadge("Có mặt: 0", GREEN);
        statAbsent     = statBadge("Vắng: 0",   RED);
        statLate       = statBadge("Trễ: 0",    AMBER);
        tableModel     = buildTableModel();
        table          = buildTable();
        emptyState     = buildEmptyState();
        rightPanel     = buildRightPanel();

        btnMarkAll.addActionListener(e -> markAll("Present"));
        btnSave   .addActionListener(e -> saveAttendance());

        setLayout(new BorderLayout());
        setBackground(BG_PAGE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), rightPanel);
        split.setDividerLocation(260);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        loadClasses();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COL));

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(BG_SIDEBAR);
        header.setBorder(new EmptyBorder(16, 12, 12, 12));

        JLabel title = new JLabel(currentTeacher != null
                ? "Lớp của tôi" : "Tất cả lớp");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);

        searchField.setFont(FONT_MAIN);
        searchField.setPreferredSize(new Dimension(0, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm tên lớp...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { filterClasses(); }
        });
        header.add(searchField, BorderLayout.SOUTH);
        sidebar.add(header, BorderLayout.NORTH);

        // List
        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        classListPanel.setBackground(BG_SIDEBAR);
        JScrollPane scroll = new JScrollPane(classListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footer.setBackground(BG_SIDEBAR);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COL));
        JButton refreshBtn = new JButton("↻");
        refreshBtn.setFont(FONT_SMALL);
        refreshBtn.setForeground(PRIMARY);
        refreshBtn.setBackground(BG_SIDEBAR);
        refreshBtn.setBorder(new EmptyBorder(4, 8, 4, 8));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setToolTipText("Làm mới danh sách lớp");
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadClasses());
        footer.add(refreshBtn);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel buildClassItem(Class c) {
        JPanel item = new JPanel(new BorderLayout(0, 3));
        item.setOpaque(true);
        item.setBackground(BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        item.setMinimumSize(new Dimension(0, 62));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(c.getClassName());
        lblName.setFont(FONT_BOLD);
        lblName.setForeground(TEXT_MAIN);

        JLabel lblStatus = new JLabel(c.getStatus());
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(statusColor(c.getStatus()));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(new EmptyBorder(2, 6, 2, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName, BorderLayout.CENTER);
        top.add(lblStatus, BorderLayout.EAST);

        String teacher = c.getTeacher() != null ? c.getTeacher().getFullName() : "Chưa phân công";
        JLabel lblSub = new JLabel(teacher);
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(TEXT_MUTED);

        item.add(top,    BorderLayout.NORTH);
        item.add(lblSub, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (item != activeClassItem) item.setBackground(ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (item != activeClassItem) item.setBackground(BG_SIDEBAR);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeClassItem != null) activeClassItem.setBackground(BG_SIDEBAR);
                activeClassItem = item;
                item.setBackground(ITEM_ACTIVE);
                selectClass(c);
            }
        });
        return item;
    }

    // ── Right panel ───────────────────────────────────────────────────────

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);
        panel.add(emptyState, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PAGE);
        JLabel lbl = new JLabel("← Chọn một lớp để bắt đầu điểm danh");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    private JPanel buildAttendanceContent() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_PAGE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildAttendanceHeader(), BorderLayout.NORTH);
        content.add(buildTableCard(),        BorderLayout.CENTER);
        content.add(buildActionBar(),        BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildAttendanceHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Class name + date picker row
        lblClassName.setFont(FONT_HEADER);
        lblClassName.setForeground(TEXT_MAIN);

        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.add(lblClassName, BorderLayout.WEST);

        // Date picker group
        JPanel dateGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dateGroup.setOpaque(false);
        JLabel dateLbl = new JLabel("Ngày:");
        dateLbl.setFont(FONT_SMALL);
        dateLbl.setForeground(TEXT_MUTED);
        dateGroup.add(dateLbl);
        dateGroup.add(datePicker);
        JButton loadBtn = createPrimaryButton("Tải danh sách");
        loadBtn.addActionListener(e -> loadAttendanceSheet());
        dateGroup.add(loadBtn);
        titleRow.add(dateGroup, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statsRow.setOpaque(false);
        statsRow.add(statPresent);
        statsRow.add(statAbsent);
        statsRow.add(statLate);
        statsRow.add(Box.createHorizontalStrut(12));
        statsRow.add(statusLabel);
        header.add(statsRow, BorderLayout.SOUTH);

        return header;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(btnMarkAll);

        JButton btnMarkAbsent = createSecondaryButton("❌ Đánh dấu tất cả Vắng");
        btnMarkAbsent.addActionListener(e -> markAll("Absent"));
        left.add(btnMarkAbsent);

        bar.add(left,    BorderLayout.WEST);
        bar.add(btnSave, BorderLayout.EAST);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
            @Override public java.lang.Class<?> getColumnClass(int c) {
                return c == 0 ? java.lang.Integer.class : java.lang.String.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean selected = isRowSelected(row);
                c.setBackground(selected ? ROW_SELECT : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
                c.setForeground(TEXT_MAIN);
                return c;
            }
        };
        t.setFont(FONT_MAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(BG_CARD);

        // Cột Trạng thái dùng JComboBox
        JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
        statusCombo.setFont(FONT_MAIN);
        t.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(statusCombo));

        // Custom renderer cho cột trạng thái — tô màu
        t.getColumnModel().getColumn(2).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable tbl, Object val,
                            boolean isSel, boolean hasFocus, int row, int col) {
                        super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                        setHorizontalAlignment(CENTER);
                        if (!isSel) {
                            String s = val != null ? val.toString() : "";
                            setForeground(switch (s) {
                                case "Present" -> GREEN;
                                case "Absent"  -> RED;
                                case "Late"    -> AMBER;
                                default        -> TEXT_MAIN;
                            });
                            setFont(FONT_BOLD);
                        }
                        return this;
                    }
                });

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        // Column widths
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(0).setMaxWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(220);
        t.getColumnModel().getColumn(2).setPreferredWidth(120);
        t.getColumnModel().getColumn(3).setPreferredWidth(200);

        // Sync thay đổi status về currentSheet + cập nhật stats
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 2) updateStats();
        });

        return t;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void loadClasses() {
        try {
            if (currentTeacher != null) {
                allClasses = classService.findByTeacherId(currentTeacher.getTeacherId());
            } else {
                allClasses = classService.findAll();
            }
            renderClassList(allClasses);
        } catch (Exception e) {
            showError("Không thể tải danh sách lớp: " + e.getMessage());
        }
    }

    private void filterClasses() {
        if (allClasses == null) return;
        String kw = searchField.getText().trim().toLowerCase();
        renderClassList(kw.isEmpty() ? allClasses
                : allClasses.stream()
                        .filter(c -> c.getClassName().toLowerCase().contains(kw))
                        .toList());
    }

    private void renderClassList(List<Class> classes) {
        classListPanel.removeAll();
        if (classes.isEmpty()) {
            JLabel lbl = new JLabel("Không có lớp nào", SwingConstants.CENTER);
            lbl.setFont(FONT_SMALL);
            lbl.setForeground(TEXT_MUTED);
            classListPanel.add(lbl);
        } else {
            classes.stream().map(this::buildClassItem).forEach(classListPanel::add);
        }
        classListPanel.revalidate();
        classListPanel.repaint();
    }

    private void selectClass(Class c) {
        selectedClass = c;
        lblClassName.setText("Lớp: " + c.getClassName());

        // Rebuild right panel với attendance content
        rightPanel.removeAll();
        rightPanel.add(buildAttendanceContent(), BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();

        loadAttendanceSheet();
    }

    private void loadAttendanceSheet() {
        if (selectedClass == null) return;

        LocalDate date = getSpinnerDate(datePicker);
        lblDateDisplay.setText(date.format(LABEL_FMT));

        // ── Chặn điểm danh cho buổi tương lai ────────────────────────────
        if (date.isAfter(LocalDate.now())) {
            tableModel.setRowCount(0);
            currentSheet.clear();
            statusLabel.setText("⚠ Không thể điểm danh cho buổi học chưa diễn ra.");
            // Disable nút lưu & đánh dấu — enable lại khi chọn ngày hợp lệ
            btnSave.setEnabled(false);
            btnMarkAll.setEnabled(false);
            updateStats();
            return;
        }
        // Ngày hợp lệ → enable lại
        btnSave.setEnabled(true);
        btnMarkAll.setEnabled(true);

        try {
            // Dùng service query trong 1 transaction riêng — tránh lazy load trên detached entity
            List<Student> enrolled = classService.findEnrolledStudents(selectedClass.getClassId());

            if (enrolled.isEmpty()) {
                tableModel.setRowCount(0);
                statusLabel.setText("Lớp chưa có học viên đăng ký.");
                updateStats();
                return;
            }

            // Chuẩn bị sheet: load existing hoặc tạo mới Present
            currentSheet = attendanceService.prepareAttendanceSheet(selectedClass, date, enrolled);

            // Render vào table
            tableModel.setRowCount(0);
            for (int i = 0; i < currentSheet.size(); i++) {
                Attendance a = currentSheet.get(i);
                tableModel.addRow(new Object[]{
                        i + 1,
                        a.getStudent().getFullName(),
                        a.getStatus(),
                        a.getNote() != null ? a.getNote() : ""
                });
            }

            statusLabel.setText("Ngày: " + date.format(DATE_FMT)
                    + "  |  " + currentSheet.size() + " học viên");
            updateStats();

        } catch (Exception e) {
            showError("Không thể tải danh sách điểm danh: " + e.getMessage());
        }
    }


    private void markAll(String status) {
        if (currentSheet.isEmpty()) return;
        // Commit editing đang dở
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(status, i, 2);
        }
        updateStats();
    }

    private void saveAttendance() {
        if (currentSheet.isEmpty()) {
            showWarning("Không có dữ liệu để lưu. Hãy tải danh sách trước.");
            return;
        }
        // Commit editing đang dở
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        // Đồng bộ lại status + note từ table vào currentSheet
        for (int i = 0; i < currentSheet.size(); i++) {
            Attendance a = currentSheet.get(i);
            String status = (String) tableModel.getValueAt(i, 2);
            String note   = (String) tableModel.getValueAt(i, 3);
            a.setStatus(status != null ? status : "Present");
            a.setNote(note != null && !note.isBlank() ? note : null);
        }

        try {
            attendanceService.saveAll(currentSheet);
            showSuccess("Đã lưu điểm danh thành công ("
                    + currentSheet.size() + " học viên).");
            // Reload để lấy ID mới từ DB (cho các record vừa insert)
            loadAttendanceSheet();
        } catch (Exception e) {
            showError("Lỗi khi lưu: " + e.getMessage());
        }
    }

    private void updateStats() {
        long present = 0, absent = 0, late = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String s = (String) tableModel.getValueAt(i, 2);
            if      ("Present".equals(s)) present++;
            else if ("Absent" .equals(s)) absent++;
            else if ("Late"   .equals(s)) late++;
        }
        statPresent.setText("Có mặt: " + present);
        statAbsent .setText("Vắng: "   + absent);
        statLate   .setText("Trễ: "    + late);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel(
                java.sql.Date.valueOf(initial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner sp = new JSpinner(model);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setFont(FONT_MAIN);
        sp.setPreferredSize(new Dimension(120, 32));
        return sp;
    }

    private LocalDate getSpinnerDate(JSpinner sp) {
        java.util.Date d = (java.util.Date) sp.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel statBadge(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(Color.WHITE);
        lbl.setBackground(color);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(3, 10, 3, 10));
        return lbl;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_H); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY);   }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static Color statusColor(String s) {
        if (s == null) return TEXT_MUTED;
        return switch (s) {
            case "Open", "Ongoing" -> GREEN;
            case "Planned"         -> new Color(59, 130, 246);
            case "Cancelled"       -> RED;
            default                -> TEXT_MUTED;
        };
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thành công",  JOptionPane.INFORMATION_MESSAGE);
    }
    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo",    JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi",         JOptionPane.ERROR_MESSAGE);
    }
}

