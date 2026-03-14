package com.company.ems.ui.panels.student;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.ResultService;
import com.company.ems.service.ResultService.RankedResult;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ClassService  classService;
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

    public ResultStudentPanel(ResultService resultService, ClassService classService, Student currentStudent) {
        this.resultService  = resultService;
        this.classService   = classService;
        this.currentStudent = currentStudent;

        kpiClasses = ComponentFactory.kpiValueLabel();
        kpiScored  = ComponentFactory.kpiValueLabel();
        kpiGpa     = ComponentFactory.kpiValueLabel();
        kpiPass    = ComponentFactory.kpiValueLabel();

        tableModel = buildTableModel();
        table      = buildTable();
        lblStatus  = new JLabel(" ");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_MUTED);
        lblStatus.setBorder(new EmptyBorder(8, 0, 0, 0));

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(lblStatus,        BorderLayout.SOUTH);

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
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);
        titleRow.add(title, BorderLayout.WEST);

        JButton btnRefresh = ComponentFactory.navButton("↻ Làm mới");
        btnRefresh.addActionListener(e -> loadData());
        titleRow.add(btnRefresh, BorderLayout.EAST);

        wrapper.add(titleRow,    BorderLayout.NORTH);
        wrapper.add(buildKpiRow(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.add(ComponentFactory.kpiCard("Tổng lớp đã học", kpiClasses, Theme.PRIMARY));
        row.add(ComponentFactory.kpiCard("Đã có điểm",      kpiScored,  Theme.BLUE));
        row.add(ComponentFactory.kpiCard("GPA trung bình",  kpiGpa,     Theme.GREEN));
        row.add(ComponentFactory.kpiCard("Đạt / Chưa đạt", kpiPass,    Theme.AMBER));
        return row;
    }

    // ── Table card ────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // Legend bar
        JPanel legendBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        legendBar.setBackground(Theme.BG_HEADER);
        legendBar.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));

        JLabel formula = new JLabel(
                "<html><b>Công thức:</b> Điểm tổng = QT1×25% + QT2×25% + Cuối kỳ×50%</html>");
        formula.setFont(Theme.FONT_SMALL);
        formula.setForeground(Theme.TEXT_MUTED);
        legendBar.add(formula);

        legendBar.add(new JLabel("  |  "));
        legendBar.add(ComponentFactory.gradeBadge("A+ ≥9.0"));
        legendBar.add(ComponentFactory.gradeBadge("A ≥8.5"));
        legendBar.add(ComponentFactory.gradeBadge("B+ ≥7.0"));
        legendBar.add(ComponentFactory.gradeBadge("C ≥5.0"));
        legendBar.add(ComponentFactory.gradeBadge("F <4.0"));

        card.add(legendBar, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        card.add(TableStyler.scrollPaneNoBorder(table), BorderLayout.CENTER);
        return card;
    }

    // ── Table model & table ───────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public  java.lang.Class<?> getColumnClass(int c) { return String.class; }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        TableStyler.applyDefaults(t);
        t.setRowHeight(42);

        // Column-specific renderers
        t.getColumnModel().getColumn(C_QT1)  .setCellRenderer(TableStyler.centeredRenderer(Theme.COL_QT_EVEN,  Theme.COL_QT_ODD));
        t.getColumnModel().getColumn(C_QT2)  .setCellRenderer(TableStyler.centeredRenderer(Theme.COL_QT_EVEN,  Theme.COL_QT_ODD));
        t.getColumnModel().getColumn(C_CK)   .setCellRenderer(TableStyler.centeredRenderer(Theme.COL_CK_EVEN,  Theme.COL_CK_ODD));
        t.getColumnModel().getColumn(C_TOTAL).setCellRenderer(TableStyler.centeredBoldRenderer(Theme.COL_TOT_EVEN, Theme.COL_TOT_ODD));
        t.getColumnModel().getColumn(C_GRADE).setCellRenderer(TableStyler.gradeRenderer());
        t.getColumnModel().getColumn(C_RANK) .setCellRenderer(TableStyler.rankRenderer());

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
            // Chỉ hiện kết quả của lớp đã thanh toán
            Set<Long> paidClassIds = classService.findPaidClassesByStudentId(currentStudent.getStudentId())
                    .stream().map(Class::getClassId).collect(Collectors.toSet());

            results = resultService.findByStudentIdWithRanking(currentStudent.getStudentId()).stream()
                    .filter(rr -> rr.result().getClazz() != null
                            && paidClassIds.contains(rr.result().getClazz().getClassId()))
                    .toList();
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
}

