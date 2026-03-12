package com.company.ems.ui.panels.student;

import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.service.ResultService;
import com.company.ems.service.ResultService.RankedResult;

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
 * Bảng điểm cá nhân của Student.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  KPI: Tổng lớp | Đã có điểm | GPA trung bình | Đạt / Chưa đạt     │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  Bảng: Lớp | Khoá | QT1 | QT2 | Cuối kỳ | Điểm tổng | Xếp loại   │
 * │             | Hạng trong lớp | Nhận xét                            │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class ResultStudentPanel extends JPanel {

    // ── Design tokens ─────────────────────────────────────────────────────
    private static final Color BG_PAGE    = new Color(248, 250, 252);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BORDER_COL = new Color(226, 232, 240);
    private static final Color PRIMARY    = new Color(37,  99,  235);
    private static final Color GREEN      = new Color(22,  163, 74);
    private static final Color AMBER      = new Color(217, 119, 6);
    private static final Color RED        = new Color(220, 38,  38);
    private static final Color BLUE       = new Color(59,  130, 246);
    private static final Color PURPLE     = new Color(124, 58,  237);
    private static final Color TEXT_MAIN  = new Color(15,  23,  42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color ROW_EVEN   = Color.WHITE;
    private static final Color ROW_ODD    = new Color(248, 250, 252);
    private static final Color ROW_SELECT = new Color(219, 234, 254);

    private static final Font FONT_MAIN   = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,   15);
    private static final Font FONT_KPI_V  = new Font("Segoe UI", Font.BOLD,   26);
    private static final Font FONT_KPI_L  = new Font("Segoe UI", Font.PLAIN,  11);

    // COL indices
    private static final int C_STT     = 0;
    private static final int C_CLASS   = 1;
    private static final int C_COURSE  = 2;
    private static final int C_QT1     = 3;
    private static final int C_QT2     = 4;
    private static final int C_CK      = 5;
    private static final int C_TOTAL   = 6;
    private static final int C_GRADE   = 7;
    private static final int C_RANK    = 8;
    private static final int C_COMMENT = 9;

    private static final String[] TABLE_COLS = {
        "STT", "Tên lớp", "Khoá học",
        "QT1 (25%)", "QT2 (25%)", "Cuối kỳ (50%)",
        "Điểm tổng", "Xếp loại",
        "Hạng / SS", "Nhận xét"
    };

    // ── Services ──────────────────────────────────────────────────────────
    private final ResultService resultService;
    private final Student       currentStudent;

    // ── KPI labels ────────────────────────────────────────────────────────
    private final JLabel kpiClasses;
    private final JLabel kpiScored;
    private final JLabel kpiGpa;
    private final JLabel kpiPass;

    // ── Table ─────────────────────────────────────────────────────────────
    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JLabel            lblStatus;

    // ── Data ──────────────────────────────────────────────────────────────
    private List<RankedResult> results = new ArrayList<>();

    public ResultStudentPanel(ResultService resultService, Student currentStudent) {
        this.resultService  = resultService;
        this.currentStudent = currentStudent;

        kpiClasses = kpiVal("—");
        kpiScored  = kpiVal("—");
        kpiGpa     = kpiVal("—");
        kpiPass    = kpiVal("—");

        tableModel = buildTableModel();
        table      = buildTable();
        lblStatus  = new JLabel(" ");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setBorder(new EmptyBorder(8, 0, 0, 0));

        setLayout(new BorderLayout(0, 16));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeader(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(lblStatus,       BorderLayout.SOUTH);

        loadData();
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("📊 Bảng điểm của tôi");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_MAIN);
        titleRow.add(title, BorderLayout.WEST);

        JButton btnRefresh = new JButton("↻ Làm mới");
        btnRefresh.setFont(FONT_SMALL);
        btnRefresh.setForeground(PRIMARY);
        btnRefresh.setBackground(BG_CARD);
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(4, 12, 4, 12)));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadData());
        titleRow.add(btnRefresh, BorderLayout.EAST);

        wrapper.add(titleRow, BorderLayout.NORTH);

        // KPI row
        wrapper.add(buildKpiRow(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.add(buildKpiCard("Tổng lớp đã học",   kpiClasses, PRIMARY));
        row.add(buildKpiCard("Đã có điểm",         kpiScored,  BLUE));
        row.add(buildKpiCard("GPA trung bình",      kpiGpa,     GREEN));
        row.add(buildKpiCard("Đạt / Chưa đạt",     kpiPass,    AMBER));
        return row;
    }

    private JPanel buildKpiCard(String label, JLabel valLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(14, 18, 14, 18)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_KPI_L);
        lbl.setForeground(TEXT_MUTED);

        valLabel.setFont(FONT_KPI_V);
        valLabel.setForeground(accent);

        card.add(lbl,      BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    private JLabel kpiVal(String v) {
        JLabel l = new JLabel(v);
        l.setFont(FONT_KPI_V);
        return l;
    }

    // ── Table card ────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER_COL));

        // Legend bar
        JPanel legendBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        legendBar.setBackground(new Color(241, 245, 249));
        legendBar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COL));

        JLabel formula = new JLabel(
                "<html><b>Công thức:</b> Điểm tổng = QT1×25% + QT2×25% + Cuối kỳ×50%</html>");
        formula.setFont(FONT_SMALL);
        formula.setForeground(TEXT_MUTED);
        legendBar.add(formula);

        legendBar.add(new JLabel("  |  "));
        legendBar.add(gradeBadge("A+ ≥9.0", GREEN));
        legendBar.add(gradeBadge("A ≥8.5",  new Color(34, 197, 94)));
        legendBar.add(gradeBadge("B+ ≥7.0", BLUE));
        legendBar.add(gradeBadge("C ≥5.0",  AMBER));
        legendBar.add(gradeBadge("F <4.0",  RED));

        card.add(legendBar, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CARD);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JLabel gradeBadge(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        return l;
    }

    // ── Table model & table ───────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public java.lang.Class<?> getColumnClass(int c) { return String.class; }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean sel = isRowSelected(row);

                // Column-based background tints
                Color bg;
                if (sel) {
                    bg = ROW_SELECT;
                } else if (col == C_QT1 || col == C_QT2) {
                    bg = row % 2 == 0 ? new Color(255, 251, 235) : new Color(254, 243, 199);
                } else if (col == C_CK) {
                    bg = row % 2 == 0 ? new Color(240, 253, 244) : new Color(220, 252, 231);
                } else if (col == C_TOTAL) {
                    bg = row % 2 == 0 ? new Color(239, 246, 255) : new Color(219, 234, 254);
                } else {
                    bg = row % 2 == 0 ? ROW_EVEN : ROW_ODD;
                }
                c.setBackground(bg);
                c.setForeground(TEXT_MAIN);

                if (c instanceof JLabel lbl) {
                    // Grade col
                    if (col == C_GRADE) {
                        lbl.setFont(FONT_BOLD);
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                        lbl.setForeground(gradeColor(lbl.getText()));
                    }
                    // Total col bold + centered
                    if (col == C_TOTAL) {
                        lbl.setFont(FONT_BOLD);
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    // Rank col
                    if (col == C_RANK) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                        lbl.setForeground(PURPLE);
                        lbl.setFont(FONT_BOLD);
                    }
                    // Numeric score cols centered
                    if (col == C_QT1 || col == C_QT2 || col == C_CK) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                return c;
            }
        };

        t.setFont(FONT_MAIN);
        t.setRowHeight(42);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(BG_CARD);
        t.setSelectionBackground(ROW_SELECT);

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        // Column widths
        t.getColumnModel().getColumn(C_STT)    .setPreferredWidth(42);  t.getColumnModel().getColumn(C_STT).setMaxWidth(50);
        t.getColumnModel().getColumn(C_CLASS)  .setPreferredWidth(180);
        t.getColumnModel().getColumn(C_COURSE) .setPreferredWidth(180);
        t.getColumnModel().getColumn(C_QT1)    .setPreferredWidth(80);  t.getColumnModel().getColumn(C_QT1).setMaxWidth(95);
        t.getColumnModel().getColumn(C_QT2)    .setPreferredWidth(80);  t.getColumnModel().getColumn(C_QT2).setMaxWidth(95);
        t.getColumnModel().getColumn(C_CK)     .setPreferredWidth(95);  t.getColumnModel().getColumn(C_CK).setMaxWidth(110);
        t.getColumnModel().getColumn(C_TOTAL)  .setPreferredWidth(90);  t.getColumnModel().getColumn(C_TOTAL).setMaxWidth(100);
        t.getColumnModel().getColumn(C_GRADE)  .setPreferredWidth(75);  t.getColumnModel().getColumn(C_GRADE).setMaxWidth(85);
        t.getColumnModel().getColumn(C_RANK)   .setPreferredWidth(80);  t.getColumnModel().getColumn(C_RANK).setMaxWidth(95);
        t.getColumnModel().getColumn(C_COMMENT).setPreferredWidth(240);

        return t;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            results = resultService.findByStudentIdWithRanking(currentStudent.getStudentId());
            renderTable();
            updateKpi();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải bảng điểm: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < results.size(); i++) {
            RankedResult rr  = results.get(i);
            Result       res = rr.result();

            String className  = res.getClazz() != null ? res.getClazz().getClassName() : "—";
            String courseName = (res.getClazz() != null && res.getClazz().getCourse() != null)
                    ? res.getClazz().getCourse().getCourseName() : "—";

            String rankStr = (res.getScore() != null && rr.totalInClass() > 0)
                    ? rr.rank() + " / " + rr.totalInClass() : "—";

            tableModel.addRow(new Object[]{
                    i + 1,
                    className,
                    courseName,
                    fmt(res.getScore1()),
                    fmt(res.getScore2()),
                    fmt(res.getFinalScore()),
                    fmt(res.getScore()),
                    res.getGrade()   != null ? res.getGrade()   : "—",
                    rankStr,
                    res.getComment() != null ? res.getComment() : ""
            });
        }
        lblStatus.setText("Tổng " + results.size() + " lớp đã tham gia");
    }

    private void updateKpi() {
        long totalClasses = results.size();
        long scored = results.stream()
                .filter(rr -> rr.result().getScore() != null)
                .count();

        // GPA: trung bình các điểm tổng khác null
        double gpa = results.stream()
                .filter(rr -> rr.result().getScore() != null)
                .mapToDouble(rr -> rr.result().getScore().doubleValue())
                .average()
                .orElse(0.0);

        long pass = results.stream()
                .filter(rr -> rr.result().getScore() != null
                        && rr.result().getScore().doubleValue() >= 5.0)
                .count();
        long fail = scored - pass;

        kpiClasses.setText(String.valueOf(totalClasses));
        kpiScored .setText(scored + " / " + totalClasses);
        kpiGpa    .setText(scored > 0 ? String.format("%.2f", gpa) : "—");
        kpiPass   .setText(pass + " đạt / " + fail + " chưa");
        kpiPass.setFont(new Font("Segoe UI", Font.BOLD, 18));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String fmt(BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : "—";
    }

    private Color gradeColor(String g) {
        if (g == null || g.equals("—")) return TEXT_MUTED;
        return switch (g) {
            case "A+", "A", "A-" -> GREEN;
            case "B+", "B", "B-" -> BLUE;
            case "C"              -> AMBER;
            case "D", "F"         -> RED;
            default               -> TEXT_MUTED;
        };
    }
}

