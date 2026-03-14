package com.company.ems.ui.panels.teacher;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.model.Teacher;
import com.company.ems.service.ClassService;
import com.company.ems.service.ResultService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

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
 * Panel Nhập điểm dành cho Teacher — 3 cột thành phần 25%-25%-50%.
 *
 * Cột: STT | Họ tên | QT1 (25%) | QT2 (25%) | Cuối kỳ (50%) | Tổng | Xếp loại | Nhận xét
 */
public class ResultTeacherPanel extends JPanel {

    // COL indices
    private static final int COL_STT     = 0;
    private static final int COL_NAME    = 1;
    private static final int COL_QT1     = 2;
    private static final int COL_QT2     = 3;
    private static final int COL_CK      = 4;
    private static final int COL_TOTAL_I = 5;
    private static final int COL_GRADE   = 6;
    private static final int COL_COMMENT = 7;

    private static final String[] TABLE_COLS = {
        "STT", "Họ và tên",
        "QT1 (25%)", "QT2 (25%)", "Cuối kỳ (50%)",
        "Điểm tổng", "Xếp loại", "Nhận xét"
    };

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
        btnSaveAll     = ComponentFactory.primaryButton("💾 Lưu tất cả điểm");
        btnClearAll    = ComponentFactory.secondaryButton("🔄 Xóa trắng");
        statusLabel    = new JLabel();
        tableModel     = buildTableModel();
        table          = buildTable();
        emptyState     = buildEmptyState();
        rightPanel     = new JPanel(new BorderLayout());

        btnSaveAll .addActionListener(e -> saveResults());
        btnClearAll.addActionListener(e -> clearAll());

        rightPanel.setBackground(Theme.BG_PAGE);
        rightPanel.add(emptyState, BorderLayout.CENTER);

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

        JLabel lblName   = new JLabel(c.getClassName());
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

        String courseName = c.getCourse() != null ? c.getCourse().getCourseName() : "";
        JLabel lblSub = new JLabel(courseName.length() > 30
                ? courseName.substring(0, 28) + "…" : courseName);
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

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PAGE);
        JLabel lbl = new JLabel("← Chọn một lớp để nhập điểm");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    private JPanel buildResultContent() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(Theme.BG_PAGE);
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

        lblClassName.setFont(Theme.FONT_HEADER);
        lblClassName.setForeground(Theme.TEXT_MAIN);
        header.add(lblClassName, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statsRow.setOpaque(false);

        JLabel formulaLbl = new JLabel(
                "<html><span style='background:#fef3c7;padding:2px 6px;border-radius:4px;'>"
                + "Tổng = QT1×25% + QT2×25% + Cuối kỳ×50%</span></html>");
        formulaLbl.setFont(Theme.FONT_SMALL);
        statsRow.add(formulaLbl);
        statsRow.add(lblStats);
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
        left.add(btnClearAll);

        JLabel legendLbl = new JLabel(
                "<html><span style='color:#22c55e;'>A+≥9.0</span> &nbsp;"
                + "<span style='color:#3b82f6;'>A≥8.5</span> &nbsp;"
                + "<span style='color:#f59e0b;'>B+≥7.0</span> &nbsp;"
                + "<span style='color:#ef4444;'>F&lt;4.0</span></html>");
        legendLbl.setFont(Theme.FONT_SMALL);
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
                return c == COL_QT1 || c == COL_QT2 || c == COL_CK || c == COL_COMMENT;
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
                Color baseBg;
                if (sel) {
                    baseBg = Theme.ROW_SELECT;
                } else if (col == COL_QT1 || col == COL_QT2) {
                    baseBg = row % 2 == 0 ? Theme.COL_QT_EVEN : Theme.COL_QT_ODD;
                } else if (col == COL_CK) {
                    baseBg = row % 2 == 0 ? Theme.COL_CK_EVEN : Theme.COL_CK_ODD;
                } else if (col == COL_TOTAL_I) {
                    baseBg = row % 2 == 0 ? Theme.COL_TOT_EVEN : Theme.COL_TOT_ODD;
                } else {
                    baseBg = row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD;
                }
                c.setBackground(baseBg);
                c.setForeground(Theme.TEXT_MAIN);

                if (col == COL_GRADE && !sel && c instanceof JLabel lbl) {
                    lbl.setFont(Theme.FONT_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setForeground(Theme.gradeColor(lbl.getText()));
                }
                if (col == COL_TOTAL_I && c instanceof JLabel lbl) {
                    lbl.setFont(Theme.FONT_BOLD);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        };

        t.setFont(Theme.FONT_PLAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Theme.BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        DefaultCellEditor scoreEditor = new DefaultCellEditor(new JTextField()) {
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
        };
        t.getColumnModel().getColumn(COL_QT1).setCellEditor(scoreEditor);
        t.getColumnModel().getColumn(COL_QT2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override public boolean stopCellEditing() {
                String val = ((JTextField) getComponent()).getText().trim();
                if (!val.isEmpty()) {
                    try { double d = Double.parseDouble(val);
                        if (d < 0 || d > 10) { JOptionPane.showMessageDialog(table, "Điểm phải từ 0 đến 10.", "Lỗi", JOptionPane.WARNING_MESSAGE); return false; }
                    } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(table, "Nhập số hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE); return false; }
                }
                return super.stopCellEditing();
            }
        });
        t.getColumnModel().getColumn(COL_CK).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override public boolean stopCellEditing() {
                String val = ((JTextField) getComponent()).getText().trim();
                if (!val.isEmpty()) {
                    try { double d = Double.parseDouble(val);
                        if (d < 0 || d > 10) { JOptionPane.showMessageDialog(table, "Điểm phải từ 0 đến 10.", "Lỗi", JOptionPane.WARNING_MESSAGE); return false; }
                    } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(table, "Nhập số hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE); return false; }
                }
                return super.stopCellEditing();
            }
        });

        t.getColumnModel().getColumn(COL_STT)    .setPreferredWidth(45);  t.getColumnModel().getColumn(COL_STT).setMaxWidth(50);
        t.getColumnModel().getColumn(COL_NAME)   .setPreferredWidth(200);
        t.getColumnModel().getColumn(COL_QT1)    .setPreferredWidth(90);  t.getColumnModel().getColumn(COL_QT1).setMaxWidth(110);
        t.getColumnModel().getColumn(COL_QT2)    .setPreferredWidth(90);  t.getColumnModel().getColumn(COL_QT2).setMaxWidth(110);
        t.getColumnModel().getColumn(COL_CK)     .setPreferredWidth(110); t.getColumnModel().getColumn(COL_CK).setMaxWidth(130);
        t.getColumnModel().getColumn(COL_TOTAL_I).setPreferredWidth(90);  t.getColumnModel().getColumn(COL_TOTAL_I).setMaxWidth(100);
        t.getColumnModel().getColumn(COL_GRADE)  .setPreferredWidth(80);  t.getColumnModel().getColumn(COL_GRADE).setMaxWidth(90);
        t.getColumnModel().getColumn(COL_COMMENT).setPreferredWidth(240);

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
            rebuildTableModelListeners();

            tableModel.setRowCount(0);
            for (int i = 0; i < currentSheet.size(); i++) {
                Result res = currentSheet.get(i);
                tableModel.addRow(new Object[]{
                        i + 1,
                        res.getStudent().getFullName(),
                        fmtScore(res.getScore1()),
                        fmtScore(res.getScore2()),
                        fmtScore(res.getFinalScore()),
                        fmtScore(res.getScore()),
                        res.getGrade()   != null ? res.getGrade()   : "",
                        res.getComment() != null ? res.getComment() : ""
                });
            }

            statusLabel.setText(enrolled.size() + " học viên");
            updateStats();

        } catch (Exception e) {
            showError("Không thể tải danh sách: " + e.getMessage());
        }
    }

    private void rebuildTableModelListeners() {
        for (javax.swing.event.TableModelListener l : tableModel.getTableModelListeners()) {
            tableModel.removeTableModelListener(l);
        }
        tableModel.addTableModelListener(e -> {
            int col = e.getColumn();
            int row = e.getFirstRow();
            if (row < 0) return;
            if (col == COL_QT1 || col == COL_QT2 || col == COL_CK) {
                BigDecimal s1 = parseBD((String) tableModel.getValueAt(row, COL_QT1));
                BigDecimal s2 = parseBD((String) tableModel.getValueAt(row, COL_QT2));
                BigDecimal sf = parseBD((String) tableModel.getValueAt(row, COL_CK));
                BigDecimal total = ResultService.calcTotal(s1, s2, sf);
                String grade = (total != null)
                        ? ResultService.autoGrade(total.doubleValue()) : "";

                javax.swing.event.TableModelListener[] listeners = tableModel.getTableModelListeners();
                for (javax.swing.event.TableModelListener l : listeners) tableModel.removeTableModelListener(l);

                tableModel.setValueAt(total != null ? total.toPlainString() : "", row, COL_TOTAL_I);
                tableModel.setValueAt(grade, row, COL_GRADE);

                rebuildTableModelListeners();
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

        for (int i = 0; i < currentSheet.size(); i++) {
            Result res = currentSheet.get(i);

            BigDecimal s1 = parseBD((String) tableModel.getValueAt(i, COL_QT1));
            BigDecimal s2 = parseBD((String) tableModel.getValueAt(i, COL_QT2));
            BigDecimal sf = parseBD((String) tableModel.getValueAt(i, COL_CK));
            BigDecimal total = ResultService.calcTotal(s1, s2, sf);
            String grade    = (String) tableModel.getValueAt(i, COL_GRADE);
            String comment  = (String) tableModel.getValueAt(i, COL_COMMENT);

            res.setScore1(s1);
            res.setScore2(s2);
            res.setFinalScore(sf);
            res.setScore(total);
            res.setGrade(grade != null && !grade.isBlank() ? grade : null);
            res.setComment(comment != null && !comment.isBlank() ? comment : null);
        }

        try {
            resultService.saveAll(currentSheet);
            showSuccess("Đã lưu điểm thành công (" + currentSheet.size() + " học viên).");
            loadResultSheet();
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
            tableModel.setValueAt("", i, COL_QT1);
            tableModel.setValueAt("", i, COL_QT2);
            tableModel.setValueAt("", i, COL_CK);
            tableModel.setValueAt("", i, COL_TOTAL_I);
            tableModel.setValueAt("", i, COL_GRADE);
        }
        updateStats();
    }

    private void updateStats() {
        long entered = 0, total = tableModel.getRowCount();
        for (int i = 0; i < total; i++) {
            String s = (String) tableModel.getValueAt(i, COL_TOTAL_I);
            if (s != null && !s.isBlank()) entered++;
        }
        lblStats.setText(String.format("  Đã có điểm tổng: %d / %d học viên", entered, total));
        lblStats.setForeground(entered == total && total > 0 ? Theme.GREEN : Theme.TEXT_MUTED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String fmtScore(BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : "";
    }

    private static BigDecimal parseBD(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
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

