package com.company.ems.ui.panels.attendance;

import com.company.ems.service.AttendanceService;
import com.company.ems.service.ClassService;
import com.company.ems.service.StudentService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttendanceAdminPanel extends JPanel {

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

        kpiTotalClasses  = kpiValueLabel("—");
        kpiTotalStudents = kpiValueLabel("—");
        kpiAvgRate       = kpiValueLabel("—");
        kpiWorstClass    = kpiValueLabel("—");

        tableModel = new DefaultTableModel(TABLE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c >= 2 && c <= 5) ? Long.class : String.class;
            }
        };
        table = buildTable();

        spinnerFrom = buildDateSpinner(LocalDate.now().withDayOfMonth(1));
        spinnerTo   = buildDateSpinner(LocalDate.now());

        statusLabel = new JLabel();
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        lastUpdated = new JLabel();
        lastUpdated.setFont(Theme.FONT_SMALL);
        lastUpdated.setForeground(Theme.TEXT_MUTED);

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_PAGE);
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

        JLabel title = new JLabel("Tổng quan Điểm danh");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        top.add(title);

        top.add(buildKpiRow());
        top.add(Box.createVerticalStrut(20));
        top.add(buildFilterBar());
        top.add(Box.createVerticalStrut(16));

        return top;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        row.add(buildKpiCard("📚 Tổng lớp học",   kpiTotalClasses,  Theme.PRIMARY));
        row.add(buildKpiCard("👥 Học viên Active", kpiTotalStudents, Theme.GREEN));
        row.add(buildKpiCard("✅ TB có mặt",       kpiAvgRate,       Theme.AMBER));
        row.add(buildKpiCard("⚠️ Vắng nhiều nhất", kpiWorstClass,    Theme.RED));
        return row;
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(16, 20, 16, 20)));

        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(0, 4));
        card.add(stripe, BorderLayout.NORTH);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_KPI_LBL);
        lbl.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(Theme.FONT_KPI_VAL);
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
        bar.setBackground(Theme.BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        left.add(labelMuted("Từ ngày:"));
        left.add(spinnerFrom);
        left.add(labelMuted("Đến ngày:"));
        left.add(spinnerTo);

        JButton applyBtn = ComponentFactory.primaryButton("Áp dụng");
        applyBtn.addActionListener(e -> loadData());
        left.add(applyBtn);

        JButton refreshBtn = ComponentFactory.secondaryButton("↻ Làm mới");
        refreshBtn.addActionListener(e -> loadData());
        left.add(refreshBtn);

        bar.add(left,        BorderLayout.WEST);
        bar.add(lastUpdated, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Chi tiết theo lớp học");
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_MAIN);
        header.add(lbl,         BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        section.add(header, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean selected = isRowSelected(row);
                c.setBackground(selected ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                c.setForeground(Theme.TEXT_MAIN);

                if (col == 6 && !selected && c instanceof JLabel lbl) {
                    String val = lbl.getText().replace("%", "").trim();
                    try {
                        double rate = Double.parseDouble(val);
                        if      (rate >= 85) lbl.setForeground(Theme.GREEN);
                        else if (rate >= 70) lbl.setForeground(Theme.AMBER);
                        else                 lbl.setForeground(Theme.RED);
                        lbl.setFont(Theme.FONT_BOLD);
                    } catch (NumberFormatException ignored) {}
                }
                return c;
            }
        };
        t.setFont(Theme.FONT_PLAIN);
        t.setRowHeight(38);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Theme.BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        javax.swing.table.DefaultTableCellRenderer centerRenderer =
                new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i <= 6; i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
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
            int totalClasses  = classService.findAll().size();
            int totalStudents = studentService.findByStatus("Hoạt động").size();
            kpiTotalClasses .setText(String.valueOf(totalClasses));
            kpiTotalStudents.setText(String.valueOf(totalStudents));

            List<Object[]> summaryRows = attendanceService.getAbsenceSummaryByClass();
            tableModel.setRowCount(0);

            double sumRate   = 0;
            double worstRate = Double.MAX_VALUE;
            String worstClass = "—";

            for (Object[] row : summaryRows) {
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
                            ? className.substring(0, 14) + "…" : className;
                }
            }

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
        spinner.setFont(Theme.FONT_PLAIN);
        spinner.setPreferredSize(new Dimension(120, 32));
        return spinner;
    }

    private LocalDate getSpinnerDate(JSpinner spinner) {
        java.util.Date d = (java.util.Date) spinner.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel labelMuted(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        return lbl;
    }

    private JLabel kpiValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_KPI_VAL);
        return lbl;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long   l) return l;
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }
}
