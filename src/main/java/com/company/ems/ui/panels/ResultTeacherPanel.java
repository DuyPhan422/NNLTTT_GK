package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.model.Teacher;
import com.company.ems.service.ClassService;
import com.company.ems.service.ResultService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Nhập điểm dành cho Teacher.
 * Đồng bộ layout với AttendanceTeacherPanel (SRP + OCP).
 *
 * Layout: Sidebar chọn lớp | Bảng nhập điểm theo lớp
 * Luồng: Chọn lớp → Load DS học viên enrolled
 *        → Nhập điểm (editable) → Xếp loại tự động → Lưu
 */
public class ResultTeacherPanel extends JPanel {

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
    private static final Color BLUE        = new Color(59,  130, 246);
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

    private static final String[] TABLE_COLS = {"STT", "Họ và tên", "Điểm (0–10)", "Xếp loại", "Nhận xét"};

    // ── Services ──────────────────────────────────────────────────────────
    private final ResultService resultService;
    private final ClassService  classService;
    private final Teacher       currentTeacher;

    // ── State ─────────────────────────────────────────────────────────────
    private List<Class>   allClasses    = new ArrayList<>();
    private Class         selectedClass = null;
    private List<Result>  currentSheet  = new ArrayList<>();
    private JPanel        activeClassItem = null;

    // ── UI ────────────────────────────────────────────────────────────────
    private final JPanel            classListPanel;
    private final JTextField        searchField;
    private final JLabel            lblClassName;
    private final JLabel            lblStats;
    private final JButton           btnSaveAll;
    private final JButton           btnClearAll;
    private final JLabel            statusLabel;
    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JPanel            rightPanel;
    private final JPanel            emptyState;

    public ResultTeacherPanel(ResultService resultService,
                              ClassService classService,
                              Teacher currentTeacher) {
        this.resultService  = resultService;
        this.classService   = classService;
        this.currentTeacher = currentTeacher;

        classListPanel = new JPanel();
        searchField    = new JTextField();
        lblClassName   = new JLabel("Chưa chọn lớp");
        lblStats       = new JLabel(" ");
        btnSaveAll     = createPrimaryButton("💾 Lưu tất cả điểm");
        btnClearAll    = createSecondaryButton("🔄 Xóa trắng");
        statusLabel    = new JLabel();
        tableModel     = buildTableModel();
        table          = buildTable();
        emptyState     = buildEmptyState();
        rightPanel     = new JPanel(new BorderLayout());

        btnSaveAll .addActionListener(e -> saveResults());
        btnClearAll.addActionListener(e -> clearAll());

        rightPanel.setBackground(BG_PAGE);
        rightPanel.add(emptyState, BorderLayout.CENTER);

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

        JLabel title = new JLabel(currentTeacher != null ? "Lớp của tôi" : "Tất cả lớp");
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
        // Tính % đã nhập điểm để hiển thị trên sidebar
        JPanel item = new JPanel(new BorderLayout(0, 3));
        item.setOpaque(true);
        item.setBackground(BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        item.setMinimumSize(new Dimension(0, 62));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName   = new JLabel(c.getClassName());
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
        top.add(lblName,   BorderLayout.CENTER);
        top.add(lblStatus, BorderLayout.EAST);

        String courseName = c.getCourse() != null ? c.getCourse().getCourseName() : "";
        JLabel lblSub = new JLabel(courseName.length() > 30
                ? courseName.substring(0, 28) + "…" : courseName);
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

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PAGE);
        JLabel lbl = new JLabel("← Chọn một lớp để nhập điểm");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    private JPanel buildResultContent() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_PAGE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildResultHeader(), BorderLayout.NORTH);
        content.add(buildTableCard(),    BorderLayout.CENTER);
        content.add(buildActionBar(),    BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildResultHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        lblClassName.setFont(FONT_HEADER);
        lblClassName.setForeground(TEXT_MAIN);
        header.add(lblClassName, BorderLayout.NORTH);

        // Stats badges
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statsRow.setOpaque(false);
        statsRow.add(lblStats);
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
        left.add(btnClearAll);

        // Legend
        JLabel legendLbl = new JLabel(
                "<html><span style='color:#22c55e;'>A+≥9.0</span> &nbsp;"
                + "<span style='color:#3b82f6;'>A≥8.5</span> &nbsp;"
                + "<span style='color:#f59e0b;'>B+≥7.0</span> &nbsp;"
                + "<span style='color:#ef4444;'>D/F&lt;5.0</span></html>");
        legendLbl.setFont(FONT_SMALL);
        left.add(Box.createHorizontalStrut(12));
        left.add(legendLbl);

        bar.add(left,       BorderLayout.WEST);
        bar.add(btnSaveAll, BorderLayout.EAST);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 2 || c == 4; // Điểm + Nhận xét
            }
            @Override public java.lang.Class<?> getColumnClass(int c) {
                return String.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean sel = isRowSelected(row);
                c.setBackground(sel ? ROW_SELECT : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
                c.setForeground(TEXT_MAIN);

                // Tô màu cột Xếp loại
                if (col == 3 && !sel && c instanceof JLabel lbl) {
                    String grade = lbl.getText();
                    lbl.setFont(FONT_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setForeground(gradeColor(grade));
                }
                return c;
            }
        };

        t.setFont(FONT_MAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        // Cột điểm — custom editor chỉ nhận số 0–10
        t.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                String val = ((JTextField) getComponent()).getText().trim();
                if (!val.isEmpty()) {
                    try {
                        double d = Double.parseDouble(val);
                        if (d < 0 || d > 10) {
                            JOptionPane.showMessageDialog(table,
                                    "Điểm phải từ 0 đến 10.", "Lỗi nhập liệu",
                                    JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(table,
                                "Vui lòng nhập số hợp lệ (vd: 8.5).", "Lỗi nhập liệu",
                                JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                }
                return super.stopCellEditing();
            }
        });


        // Column widths
        t.getColumnModel().getColumn(0).setPreferredWidth(50);  t.getColumnModel().getColumn(0).setMaxWidth(55);
        t.getColumnModel().getColumn(1).setPreferredWidth(220);
        t.getColumnModel().getColumn(2).setPreferredWidth(110); t.getColumnModel().getColumn(2).setMaxWidth(130);
        t.getColumnModel().getColumn(3).setPreferredWidth(90);  t.getColumnModel().getColumn(3).setMaxWidth(100);
        t.getColumnModel().getColumn(4).setPreferredWidth(260);

        return t;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void loadClasses() {
        try {
            allClasses = (currentTeacher != null)
                    ? classService.findByTeacherId(currentTeacher.getTeacherId())
                    : classService.findAll();
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

        rightPanel.removeAll();
        rightPanel.add(buildResultContent(), BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();

        loadResultSheet();
    }

    private void loadResultSheet() {
        if (selectedClass == null) return;
        try {
            List<Student> enrolled = classService.findEnrolledStudents(selectedClass.getClassId());
            if (enrolled.isEmpty()) {
                tableModel.setRowCount(0);
                statusLabel.setText("Lớp chưa có học viên đăng ký.");
                return;
            }

            currentSheet = resultService.prepareResultSheet(selectedClass, enrolled);

            // Rebuild table model listener after reload
            rebuildTableModelListeners();

            tableModel.setRowCount(0);
            for (int i = 0; i < currentSheet.size(); i++) {
                Result res = currentSheet.get(i);
                String scoreStr = res.getScore() != null
                        ? res.getScore().stripTrailingZeros().toPlainString() : "";
                String grade    = res.getGrade()   != null ? res.getGrade()   : "";
                String comment  = res.getComment() != null ? res.getComment() : "";
                tableModel.addRow(new Object[]{i + 1,
                        res.getStudent().getFullName(), scoreStr, grade, comment});
            }

            statusLabel.setText(enrolled.size() + " học viên");
            updateStats();

        } catch (Exception e) {
            showError("Không thể tải danh sách: " + e.getMessage());
        }
    }

    /**
     * Rebuild table listener sau mỗi lần reload để tránh listener leak.
     * Listener tự động cập nhật cột Xếp loại khi Điểm thay đổi.
     */
    private void rebuildTableModelListeners() {
        // Remove all existing listeners
        for (javax.swing.event.TableModelListener l : tableModel.getTableModelListeners()) {
            tableModel.removeTableModelListener(l);
        }
        // Re-add single listener
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 2 && e.getFirstRow() >= 0) {
                int row = e.getFirstRow();
                String scoreStr = (String) tableModel.getValueAt(row, 2);
                String grade = "";
                if (scoreStr != null && !scoreStr.isBlank()) {
                    try {
                        double d = Double.parseDouble(scoreStr.trim());
                        grade = ResultService.autoGrade(d);
                    } catch (NumberFormatException ignored) {}
                }
                // Temporarily remove listener to avoid recursion
                javax.swing.event.TableModelListener[] listeners = tableModel.getTableModelListeners();
                for (javax.swing.event.TableModelListener l : listeners) {
                    tableModel.removeTableModelListener(l);
                }
                tableModel.setValueAt(grade, row, 3);
                rebuildTableModelListeners(); // restore
                updateStats();
            }
        });
    }

    private void saveResults() {
        if (currentSheet.isEmpty()) {
            showWarning("Không có dữ liệu để lưu. Hãy chọn lớp trước.");
            return;
        }
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        // Sync table → currentSheet
        for (int i = 0; i < currentSheet.size(); i++) {
            Result res = currentSheet.get(i);
            String scoreStr = (String) tableModel.getValueAt(i, 2);
            String grade    = (String) tableModel.getValueAt(i, 3);
            String comment  = (String) tableModel.getValueAt(i, 4);

            if (scoreStr != null && !scoreStr.isBlank()) {
                try {
                    res.setScore(new BigDecimal(scoreStr.trim()));
                } catch (NumberFormatException ignored) {}
            } else {
                res.setScore(null);
            }
            res.setGrade(grade != null && !grade.isBlank() ? grade : null);
            res.setComment(comment != null && !comment.isBlank() ? comment : null);
        }

        try {
            resultService.saveAll(currentSheet);
            showSuccess("Đã lưu điểm thành công (" + currentSheet.size() + " học viên).");
            loadResultSheet(); // reload để lấy ID mới nếu vừa insert
        } catch (Exception e) {
            showError("Lỗi khi lưu: " + e.getMessage());
        }
    }

    private void clearAll() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa trắng toàn bộ điểm đang hiển thị?\n(Chưa lưu xuống DB)",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("", i, 2);
            tableModel.setValueAt("", i, 3);
        }
        updateStats();
    }

    private void updateStats() {
        long entered = 0;
        long total   = tableModel.getRowCount();
        for (int i = 0; i < total; i++) {
            String s = (String) tableModel.getValueAt(i, 2);
            if (s != null && !s.isBlank()) entered++;
        }
        lblStats.setText(String.format("Đã nhập: %d / %d học viên", entered, total));
        lblStats.setForeground(entered == total && total > 0 ? GREEN : TEXT_MUTED);
    }

    // ── Color helpers ─────────────────────────────────────────────────────

    private Color gradeColor(String grade) {
        if (grade == null) return TEXT_MUTED;
        return switch (grade) {
            case "A+", "A", "A-" -> GREEN;
            case "B+", "B", "B-" -> BLUE;
            case "C"              -> AMBER;
            case "D", "F"         -> RED;
            default               -> TEXT_MUTED;
        };
    }

    private Color statusColor(String s) {
        if (s == null) return TEXT_MUTED;
        return switch (s) {
            case "Open", "Ongoing" -> GREEN;
            case "Planned"         -> new Color(59, 130, 246);
            case "Cancelled"       -> RED;
            default                -> TEXT_MUTED;
        };
    }

    // ── Button factories ──────────────────────────────────────────────────

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD); btn.setForeground(Color.WHITE); btn.setBackground(PRIMARY);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_H); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN); btn.setForeground(TEXT_MAIN); btn.setBackground(BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Notifications ─────────────────────────────────────────────────────

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

