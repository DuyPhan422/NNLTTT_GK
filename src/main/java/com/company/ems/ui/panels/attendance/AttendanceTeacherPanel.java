package com.company.ems.ui.panels.attendance;

import com.company.ems.model.Attendance;
import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.model.Teacher;
import com.company.ems.service.AttendanceService;
import com.company.ems.service.ClassService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

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

public class AttendanceTeacherPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            java.util.Locale.forLanguageTag("vi"));

    private static final String[] STATUS_OPTIONS = {"Present", "Absent", "Late"};
    private static final String[] TABLE_COLS = {"STT", "Họ và tên", "Trạng thái", "Ghi chú"};

    // ── Services ──────────────────────────────────────────────────────────
    private final AttendanceService attendanceService;
    private final ClassService      classService;
    private final Teacher           currentTeacher;

    // ── State ─────────────────────────────────────────────────────────────
    private List<Class>      allClasses     = new ArrayList<>();
    private Class            selectedClass  = null;
    private List<Attendance> currentSheet   = new ArrayList<>();
    private JPanel           activeClassItem = null;

    // ── UI components ─────────────────────────────────────────────────────
    private final JPanel     classListPanel;
    private final JTextField searchField;
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

        classListPanel = new JPanel();
        searchField    = new JTextField();
        lblClassName   = new JLabel("Chưa chọn lớp");
        lblDateDisplay = new JLabel("");
        datePicker     = buildDateSpinner(LocalDate.now());
        btnMarkAll     = ComponentFactory.secondaryButton("✅ Đánh dấu tất cả Có mặt");
        btnSave        = ComponentFactory.primaryButton("💾 Lưu điểm danh");
        statusLabel    = new JLabel();
        statPresent    = statBadge("Có mặt: 0", Theme.GREEN);
        statAbsent     = statBadge("Vắng: 0",   Theme.RED);
        statLate       = statBadge("Trễ: 0",    Theme.AMBER);
        tableModel     = buildTableModel();
        table          = buildTable();
        emptyState     = buildEmptyState();
        rightPanel     = buildRightPanel();

        btnMarkAll.addActionListener(e -> markAll("Present"));
        btnSave   .addActionListener(e -> saveAttendance());

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);

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
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER));

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(Theme.BG_SIDEBAR);
        header.setBorder(new EmptyBorder(16, 12, 12, 12));

        JLabel title = new JLabel(currentTeacher != null ? "Lớp của tôi" : "Tất cả lớp");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);

        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setPreferredSize(new Dimension(0, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(4, 8, 4, 8)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm tên lớp...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { filterClasses(); }
        });
        header.add(searchField, BorderLayout.SOUTH);
        sidebar.add(header, BorderLayout.NORTH);

        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        classListPanel.setBackground(Theme.BG_SIDEBAR);
        JScrollPane scroll = new JScrollPane(classListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footer.setBackground(Theme.BG_SIDEBAR);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER));
        JButton refreshBtn = new JButton("↻");
        refreshBtn.setFont(Theme.FONT_SMALL);
        refreshBtn.setForeground(Theme.PRIMARY);
        refreshBtn.setBackground(Theme.BG_SIDEBAR);
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
        item.setBackground(Theme.BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        item.setMinimumSize(new Dimension(0, 62));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(c.getClassName());
        lblName.setFont(Theme.FONT_BOLD);
        lblName.setForeground(Theme.TEXT_MAIN);

        JLabel lblStatus = new JLabel(c.getStatus());
        lblStatus.setFont(Theme.FONT_BADGE);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(Theme.classStatusColor(c.getStatus()));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(new EmptyBorder(2, 6, 2, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName,   BorderLayout.CENTER);
        top.add(lblStatus, BorderLayout.EAST);

        String teacher = c.getTeacher() != null ? c.getTeacher().getFullName() : "Chưa phân công";
        JLabel lblSub = new JLabel(teacher);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_MUTED);

        item.add(top,    BorderLayout.NORTH);
        item.add(lblSub, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (item != activeClassItem) item.setBackground(Theme.ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (item != activeClassItem) item.setBackground(Theme.BG_SIDEBAR);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeClassItem != null) activeClassItem.setBackground(Theme.BG_SIDEBAR);
                activeClassItem = item;
                item.setBackground(Theme.ITEM_ACTIVE);
                selectClass(c);
            }
        });
        return item;
    }

    // ── Right panel ───────────────────────────────────────────────────────

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_PAGE);
        panel.add(emptyState, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PAGE);
        JLabel lbl = new JLabel("← Chọn một lớp để bắt đầu điểm danh");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    private JPanel buildAttendanceContent() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(Theme.BG_PAGE);
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

        lblClassName.setFont(Theme.FONT_HEADER);
        lblClassName.setForeground(Theme.TEXT_MAIN);

        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.add(lblClassName, BorderLayout.WEST);

        JPanel dateGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dateGroup.setOpaque(false);
        JLabel dateLbl = new JLabel("Ngày:");
        dateLbl.setFont(Theme.FONT_SMALL);
        dateLbl.setForeground(Theme.TEXT_MUTED);
        dateGroup.add(dateLbl);
        dateGroup.add(datePicker);
        JButton loadBtn = ComponentFactory.primaryButton("Tải danh sách");
        loadBtn.addActionListener(e -> loadAttendanceSheet());
        dateGroup.add(loadBtn);
        titleRow.add(dateGroup, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

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
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(btnMarkAll);

        JButton btnMarkAbsent = ComponentFactory.secondaryButton("❌ Đánh dấu tất cả Vắng");
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
                c.setBackground(selected ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                c.setForeground(Theme.TEXT_MAIN);
                return c;
            }
        };
        t.setFont(Theme.FONT_PLAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Theme.BG_CARD);

        JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
        statusCombo.setFont(Theme.FONT_PLAIN);
        t.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(statusCombo));

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
                                case "Present" -> Theme.GREEN;
                                case "Absent"  -> Theme.RED;
                                case "Late"    -> Theme.AMBER;
                                default        -> Theme.TEXT_MAIN;
                            });
                            setFont(Theme.FONT_BOLD);
                        }
                        return this;
                    }
                });

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(0).setMaxWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(220);
        t.getColumnModel().getColumn(2).setPreferredWidth(120);
        t.getColumnModel().getColumn(3).setPreferredWidth(200);

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
            lbl.setFont(Theme.FONT_SMALL);
            lbl.setForeground(Theme.TEXT_MUTED);
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

        if (date.isAfter(LocalDate.now())) {
            tableModel.setRowCount(0);
            currentSheet.clear();
            statusLabel.setText("⚠ Không thể điểm danh cho buổi học chưa diễn ra.");
            btnSave.setEnabled(false);
            btnMarkAll.setEnabled(false);
            updateStats();
            return;
        }
        btnSave.setEnabled(true);
        btnMarkAll.setEnabled(true);

        try {
            List<Student> enrolled = classService.findEnrolledStudents(selectedClass.getClassId());

            if (enrolled.isEmpty()) {
                tableModel.setRowCount(0);
                statusLabel.setText("Lớp chưa có học viên đăng ký.");
                updateStats();
                return;
            }

            currentSheet = attendanceService.prepareAttendanceSheet(selectedClass, date, enrolled);

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
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

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
        sp.setFont(Theme.FONT_PLAIN);
        sp.setPreferredSize(new Dimension(120, 32));
        return sp;
    }

    private LocalDate getSpinnerDate(JSpinner sp) {
        java.util.Date d = (java.util.Date) sp.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel statBadge(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Color.WHITE);
        lbl.setBackground(color);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(3, 10, 3, 10));
        return lbl;
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

