package com.company.ems.ui.panels.attendance;

import com.company.ems.service.AttendanceService;
import com.company.ems.service.ClassService;
import com.company.ems.service.StudentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel điểm danh dành cho Admin.
 * Mục tiêu: tổng quan toàn trung tâm — không điểm danh thủ công.
 * Layout:
 *   ┌──────────────────────────────────────────────────┐
 *   │  KPI Cards: Tổng lớp | Tổng HV | TB có mặt | Vắng nhiều nhất │
 *   ├──────────────────────────────────────────────────┤
 *   │  Filter bar: Từ ngày — Đến ngày  [Áp dụng]       │
 *   ├──────────────────────────────────────────────────┤
 *   │  Bảng: Lớp học | Khóa | Tổng buổi | Có mặt | Vắng | Trễ | Tỉ lệ % │
 *   └──────────────────────────────────────────────────┘
 */
public class AttendanceAdminPanel extends JPanel {

    // ── Design tokens ────────────────────────────────────────────────────
    private static final Color BG_PAGE     = new Color(248, 250, 252);
    private static final Color BG_CARD     = Color.WHITE;
    private static final Color BORDER_COL  = new Color(226, 232, 240);
    private static final Color TEXT_MAIN   = new Color(15,  23,  42);
    private static final Color TEXT_MUTED  = new Color(100, 116, 139);
    private static final Color PRIMARY     = new Color(37,  99,  235);
    private static final Color GREEN       = new Color(22,  163, 74);
    private static final Color AMBER       = new Color(217, 119, 6);
    private static final Color RED         = new Color(220, 38,  38);
    private static final Color ROW_EVEN    = Color.WHITE;
    private static final Color ROW_ODD     = new Color(248, 250, 252);
    private static final Color ROW_SELECT  = new Color(219, 234, 254);

    private static final Font FONT_MAIN    = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD,   28);
    private static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD,   15);

    private static final String[] TABLE_COLS = {
        "Lớp học", "Khóa học", "Tổng lượt", "Có mặt", "Vắng", "Trễ", "Tỉ lệ có mặt"
    };

    // ── Services ─────────────────────────────────────────────────────────
    private final AttendanceService attendanceService;
    private final ClassService      classService;
    private final StudentService    studentService;

    // ── UI components ─────────────────────────────────────────────────────
    private final JLabel kpiTotalClasses;
    private final JLabel kpiTotalStudents;
    private final JLabel kpiAvgRate;
    private final JLabel kpiWorstClass;
    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JLabel            statusLabel;
    private final JSpinner          spinnerFrom;
    private final JSpinner          spinnerTo;
    private final JLabel            lastUpdated;

    public AttendanceAdminPanel(AttendanceService attendanceService,
                                ClassService classService,
                                StudentService studentService) {
        this.attendanceService = attendanceService;
        this.classService      = classService;
        this.studentService    = studentService;

        // KPI labels
        kpiTotalClasses  = kpiValueLabel("—");
        kpiTotalStudents = kpiValueLabel("—");
        kpiAvgRate       = kpiValueLabel("—");
        kpiWorstClass    = kpiValueLabel("—");

        // Table
        tableModel = new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c >= 2 && c <= 5) ? Long.class : String.class;
            }
        };
        table = buildTable();

        // Filter
        spinnerFrom = buildDateSpinner(LocalDate.now().withDayOfMonth(1));
        spinnerTo   = buildDateSpinner(LocalDate.now());

        statusLabel  = new JLabel();
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);

        lastUpdated = new JLabel();
        lastUpdated.setFont(FONT_SMALL);
        lastUpdated.setForeground(TEXT_MUTED);

        // Layout
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(buildTopSection(),   BorderLayout.NORTH);
        add(buildTableSection(), BorderLayout.CENTER);

        loadData();
    }

    // ── Build UI ─────────────────────────────────────────────────────────

    private JPanel buildTopSection() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        // Page title
        JLabel title = new JLabel("Tổng quan Điểm danh");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_MAIN);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        top.add(title);

        // KPI row
        top.add(buildKpiRow());
        top.add(Box.createVerticalStrut(20));

        // Filter bar
        top.add(buildFilterBar());
        top.add(Box.createVerticalStrut(16));

        return top;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        row.add(buildKpiCard("📚 Tổng lớp học",   kpiTotalClasses,  PRIMARY));
        row.add(buildKpiCard("👥 Học viên Active", kpiTotalStudents, GREEN));
        row.add(buildKpiCard("✅ TB có mặt",       kpiAvgRate,       AMBER));
        row.add(buildKpiCard("⚠️ Vắng nhiều nhất", kpiWorstClass,    RED));
        return row;
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(16, 20, 16, 20)));

        // Accent stripe top
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(0, 4));
        card.add(stripe, BorderLayout.NORTH);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_KPI_LBL);
        lbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accentColor);

        JPanel inner = new JPanel(new BorderLayout(0, 4));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(10, 0, 0, 0));
        inner.add(lbl,        BorderLayout.NORTH);
        inner.add(valueLabel, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(12, 16, 12, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        left.add(labelMuted("Từ ngày:"));
        left.add(spinnerFrom);
        left.add(labelMuted("Đến ngày:"));
        left.add(spinnerTo);

        JButton applyBtn = new JButton("Áp dụng");
        applyBtn.setFont(FONT_BOLD);
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setBackground(PRIMARY);
        applyBtn.setBorder(new EmptyBorder(6, 16, 6, 16));
        applyBtn.setFocusPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> loadData());
        left.add(applyBtn);

        JButton refreshBtn = new JButton("↻ Làm mới");
        refreshBtn.setFont(FONT_MAIN);
        refreshBtn.setForeground(PRIMARY);
        refreshBtn.setBackground(BG_CARD);
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(5, 12, 5, 12)));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadData());
        left.add(refreshBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(lastUpdated, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        // Sub-header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Chi tiết theo lớp học");
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_MAIN);
        header.add(lbl, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        section.add(header, BorderLayout.NORTH);

        // Table
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(BG_CARD);
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean selected = isRowSelected(row);
                c.setBackground(selected ? ROW_SELECT : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
                c.setForeground(TEXT_MAIN);

                // Tô màu cột "Tỉ lệ có mặt" theo ngưỡng
                if (col == 6 && !selected && c instanceof JLabel lbl) {
                    String val = lbl.getText().replace("%", "").trim();
                    try {
                        double rate = Double.parseDouble(val);
                        if      (rate >= 85) lbl.setForeground(GREEN);
                        else if (rate >= 70) lbl.setForeground(AMBER);
                        else                 lbl.setForeground(RED);
                        lbl.setFont(FONT_BOLD);
                    } catch (NumberFormatException ignored) {}
                }
                return c;
            }
        };
        t.setFont(FONT_MAIN);
        t.setRowHeight(38);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        // Căn giữa các cột số
        javax.swing.table.DefaultTableCellRenderer centerRenderer =
                new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i <= 6; i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        // Cột tỉ lệ rộng hơn
        t.getColumnModel().getColumn(0).setPreferredWidth(160);
        t.getColumnModel().getColumn(1).setPreferredWidth(160);
        t.getColumnModel().getColumn(6).setPreferredWidth(120);

        return t;
    }

    // ── Data ─────────────────────────────────────────────────────────────

    private void loadData() {
        LocalDate from = getSpinnerDate(spinnerFrom);
        LocalDate to   = getSpinnerDate(spinnerTo);

        try {
            // ── KPI: tổng lớp + học viên
            int totalClasses  = classService.findAll().size();
            int totalStudents = studentService.findByStatus("Active").size();
            kpiTotalClasses .setText(String.valueOf(totalClasses));
            kpiTotalStudents.setText(String.valueOf(totalStudents));

            // ── Bảng chi tiết: [className, courseName, total, absent, present, late]
            List<Object[]> summaryRows = attendanceService.getAbsenceSummaryByClass();
            tableModel.setRowCount(0);

            double sumRate   = 0;
            double worstRate = Double.MAX_VALUE;
            String worstClass = "—";

            for (Object[] row : summaryRows) {
                // [className, courseName, total, absent, present, late]
                String className  = (String) row[0];
                String courseName = (String) row[1];
                long   total      = toLong(row[2]);
                long   absent     = toLong(row[3]);
                long   present    = toLong(row[4]);
                long   late       = toLong(row[5]);
                double rate       = total > 0 ? (present * 100.0 / total) : 0.0;
                String rateStr    = String.format("%.1f%%", rate);

                tableModel.addRow(new Object[]{
                        className, courseName, total, present, absent, late, rateStr
                });

                sumRate += rate;
                if (rate < worstRate && total > 0) {
                    worstRate  = rate;
                    worstClass = className.length() > 14
                            ? className.substring(0, 14) + "…"
                            : className;
                }
            }

            // ── KPI update
            int rowCount = tableModel.getRowCount();
            double avgRate = rowCount > 0 ? sumRate / rowCount : 0.0;
            kpiAvgRate  .setText(String.format("%.0f%%", avgRate));
            kpiWorstClass.setText(worstClass.equals("—") ? "—" :
                    String.format("%.0f%%", worstRate));

            statusLabel.setText("Tổng: " + rowCount + " lớp có dữ liệu điểm danh");
            lastUpdated.setText("Cập nhật lúc " +
                    java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải dữ liệu: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel(
                java.sql.Date.valueOf(initial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setFont(FONT_MAIN);
        spinner.setPreferredSize(new Dimension(120, 32));
        return spinner;
    }

    private LocalDate getSpinnerDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel labelMuted(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    private JLabel kpiValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_KPI_VAL);
        return lbl;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long   l) return l;
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }
}

