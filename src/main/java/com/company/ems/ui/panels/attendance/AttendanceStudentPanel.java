package com.company.ems.ui.panels.attendance;

import com.company.ems.model.Attendance;
import com.company.ems.model.Student;
import com.company.ems.service.AttendanceService;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AttendanceStudentPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DAY_FMT  =
            DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("vi"));

    // ── Services / state ──────────────────────────────────────────────────
    private final AttendanceService attendanceService;
    private final Student           currentStudent;

    private List<Attendance>            allAttendances = new ArrayList<>();
    private Map<Long, List<Attendance>> byClass        = new LinkedHashMap<>();
    private Long                        selectedClassId = null;
    private JPanel                      activeItem      = null;

    // ── KPI labels ────────────────────────────────────────────────────────
    private final JLabel kpiTotal;
    private final JLabel kpiPresent;
    private final JLabel kpiAbsent;
    private final JLabel kpiLate;
    private final JLabel kpiRate;

    // ── Main area ─────────────────────────────────────────────────────────
    private final JPanel classSidebarList;
    private final JPanel contentArea;

    public AttendanceStudentPanel(AttendanceService attendanceService,
                                  Student currentStudent) {
        this.attendanceService = attendanceService;
        this.currentStudent    = currentStudent;

        kpiTotal   = kpiVal("—");
        kpiPresent = kpiVal("—");
        kpiAbsent  = kpiVal("—");
        kpiLate    = kpiVal("—");
        kpiRate    = kpiVal("—");

        classSidebarList = new JPanel();
        classSidebarList.setLayout(new BoxLayout(classSidebarList, BoxLayout.Y_AXIS));
        classSidebarList.setBackground(Theme.BG_SIDEBAR);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Theme.BG_PAGE);
        contentArea.add(buildEmptyState(), BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);

        loadData();
    }

    // ── Build UI ──────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Title
        String name = currentStudent != null ? currentStudent.getFullName() : "Học viên";
        JLabel title = new JLabel("Chuyên cần của: " + name);
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);

        // KPI cards
        header.add(buildKpiRow(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        row.add(buildKpiCard("📅 Tổng buổi",    kpiTotal,   Theme.BLUE));
        row.add(buildKpiCard("✅ Có mặt",        kpiPresent, Theme.GREEN));
        row.add(buildKpiCard("❌ Vắng mặt",      kpiAbsent,  Theme.RED));
        row.add(buildKpiCard("⏰ Đi trễ",        kpiLate,    Theme.AMBER));
        row.add(buildKpiCard("📊 Tỉ lệ có mặt", kpiRate,    Theme.BLUE));
        return row;
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(14, 18, 14, 18)));

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(0, 3));
        card.add(stripe, BorderLayout.NORTH);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_KPI_LBL);
        lbl.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(Theme.FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        JPanel inner = new JPanel(new BorderLayout(0, 3));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(8, 0, 0, 0));
        inner.add(lbl,        BorderLayout.NORTH);
        inner.add(valueLabel, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildClassSidebar(), contentArea);
        split.setDividerLocation(220);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setContinuousLayout(true);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildClassSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER));

        JPanel sideHeader = new JPanel(new BorderLayout());
        sideHeader.setBackground(Theme.BG_SIDEBAR);
        sideHeader.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel lbl = new JLabel("Lớp đang học");
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_MAIN);
        sideHeader.add(lbl, BorderLayout.CENTER);
        sidebar.add(sideHeader, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(classSidebarList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PAGE);
        JLabel lbl = new JLabel("← Chọn một lớp để xem lịch sử điểm danh");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        if (currentStudent == null) return;
        try {
            allAttendances = attendanceService.findByStudentId(currentStudent.getStudentId());

            byClass = allAttendances.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getClazz().getClassId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            updateKpiAll();
            renderClassSidebar();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải dữ liệu: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateKpiAll() {
        long total   = allAttendances.size();
        long present = allAttendances.stream().filter(a -> "Có mặt".equals(a.getStatus())).count();
        long absent  = allAttendances.stream().filter(a -> "Vắng"  .equals(a.getStatus())).count();
        long late    = allAttendances.stream().filter(a -> "Đi trễ".equals(a.getStatus())).count();
        double rate  = total > 0 ? (present * 100.0 / total) : 0.0;

        kpiTotal  .setText(String.valueOf(total));
        kpiPresent.setText(String.valueOf(present));
        kpiAbsent .setText(String.valueOf(absent));
        kpiLate   .setText(String.valueOf(late));
        kpiRate   .setText(String.format("%.0f%%", rate));
    }

    private void renderClassSidebar() {
        classSidebarList.removeAll();

        if (byClass.isEmpty()) {
            JLabel lbl = new JLabel("Chưa có dữ liệu", SwingConstants.CENTER);
            lbl.setFont(Theme.FONT_SMALL);
            lbl.setForeground(Theme.TEXT_MUTED);
            classSidebarList.add(lbl);
        } else {
            byClass.forEach((classId, records) -> {
                String className = records.get(0).getClazz().getClassName();
                long present = records.stream().filter(a -> "Có mặt".equals(a.getStatus())).count();
                double rate  = records.isEmpty() ? 0 : (present * 100.0 / records.size());
                classSidebarList.add(buildClassSidebarItem(classId, className, records.size(), rate));
            });
        }
        classSidebarList.revalidate();
        classSidebarList.repaint();
    }

    private JPanel buildClassSidebarItem(Long classId, String className,
                                          int totalSessions, double rate) {
        JPanel item = new JPanel(new BorderLayout(0, 2));
        item.setOpaque(true);
        item.setBackground(Theme.BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        item.setMinimumSize(new Dimension(0, 58));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(className);
        lblName.setFont(Theme.FONT_BOLD);
        lblName.setForeground(Theme.TEXT_MAIN);

        Color rateColor = rate >= 85 ? Theme.GREEN : rate >= 70 ? Theme.AMBER : Theme.RED;
        JLabel lblRate  = new JLabel(String.format("%.0f%%", rate));
        lblRate.setFont(Theme.FONT_BADGE);
        lblRate.setForeground(rateColor);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName, BorderLayout.CENTER);
        top.add(lblRate, BorderLayout.EAST);

        JLabel lblSub = new JLabel(totalSessions + " buổi");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_MUTED);

        item.add(top,    BorderLayout.NORTH);
        item.add(lblSub, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (item != activeItem) item.setBackground(Theme.ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (item != activeItem) item.setBackground(Theme.BG_SIDEBAR);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeItem != null) activeItem.setBackground(Theme.BG_SIDEBAR);
                activeItem = item;
                item.setBackground(Theme.ITEM_ACTIVE);
                showClassDetail(classId, className);
            }
        });
        return item;
    }

    private void showClassDetail(Long classId, String className) {
        selectedClassId = classId;
        List<Attendance> records = byClass.getOrDefault(classId, List.of());

        long total   = records.size();
        long present = records.stream().filter(a -> "Có mặt".equals(a.getStatus())).count();
        long absent  = records.stream().filter(a -> "Vắng"  .equals(a.getStatus())).count();
        long late    = records.stream().filter(a -> "Đi trễ".equals(a.getStatus())).count();
        double rate  = total > 0 ? (present * 100.0 / total) : 0.0;

        kpiTotal  .setText(String.valueOf(total));
        kpiPresent.setText(String.valueOf(present));
        kpiAbsent .setText(String.valueOf(absent));
        kpiLate   .setText(String.valueOf(late));
        kpiRate   .setText(String.format("%.0f%%", rate));

        String[] cols = {"Ngày", "Thứ", "Trạng thái", "Ghi chú"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        records.stream()
               .sorted(Comparator.comparing(Attendance::getAttendDate).reversed())
               .forEach(a -> model.addRow(new Object[]{
                       a.getAttendDate().format(DATE_FMT),
                       capitalize(a.getAttendDate().format(DAY_FMT)),
                       a.getStatus(),
                       a.getNote() != null ? a.getNote() : ""
               }));

        JTable timeline = buildTimelineTable(model);
        JPanel progressPanel = buildProgressBar(rate);

        JPanel detail = new JPanel(new BorderLayout(0, 12));
        detail.setBackground(Theme.BG_PAGE);
        detail.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblClass = new JLabel("Lớp: " + className
                + "   |   " + total + " buổi đã điểm danh");
        lblClass.setFont(Theme.FONT_BOLD);
        lblClass.setForeground(Theme.TEXT_MAIN);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(lblClass,      BorderLayout.NORTH);
        top.add(progressPanel, BorderLayout.SOUTH);

        detail.add(top, BorderLayout.NORTH);

        timeline.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(timeline);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        detail.add(scroll, BorderLayout.CENTER);

        contentArea.removeAll();
        contentArea.add(detail, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JTable buildTimelineTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean sel = isRowSelected(row);
                c.setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                c.setForeground(Theme.TEXT_MAIN);

                if (col == 2 && c instanceof JLabel lbl && !sel) {
                    String s = lbl.getText();
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setFont(Theme.FONT_BOLD);
                    lbl.setForeground(switch (s) {
                        case "Có mặt" -> Theme.GREEN;
                        case "Vắng"   -> Theme.RED;
                        case "Đi trễ" -> Theme.AMBER;
                        default        -> Theme.TEXT_MAIN;
                    });
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

        javax.swing.table.JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 40));

        t.getColumnModel().getColumn(0).setPreferredWidth(110);
        t.getColumnModel().getColumn(1).setPreferredWidth(120);
        t.getColumnModel().getColumn(2).setPreferredWidth(100);
        t.getColumnModel().getColumn(3).setPreferredWidth(200);
        return t;
    }

    private JPanel buildProgressBar(double rate) {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(Theme.BG_CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel lblTitle = new JLabel("Tỉ lệ chuyên cần");
        lblTitle.setFont(Theme.FONT_SMALL);
        lblTitle.setForeground(Theme.TEXT_MUTED);
        lblTitle.setPreferredSize(new Dimension(130, 0));

        JPanel track = new JPanel(null);
        track.setBackground(Theme.BORDER);
        track.setPreferredSize(new Dimension(0, 12));
        track.setBorder(BorderFactory.createEmptyBorder());

        Color fillColor = rate >= 85 ? Theme.GREEN : rate >= 70 ? Theme.AMBER : Theme.RED;
        JPanel fill = new JPanel();
        fill.setBackground(fillColor);
        track.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int w = (int) (track.getWidth() * rate / 100.0);
                fill.setBounds(0, 0, w, track.getHeight());
            }
        });
        track.add(fill);

        JLabel lblPct = new JLabel(String.format("%.1f%%", rate));
        lblPct.setFont(Theme.FONT_BOLD);
        lblPct.setForeground(fillColor);
        lblPct.setPreferredSize(new Dimension(55, 0));
        lblPct.setHorizontalAlignment(SwingConstants.RIGHT);

        wrapper.add(lblTitle, BorderLayout.WEST);
        wrapper.add(track,    BorderLayout.CENTER);
        wrapper.add(lblPct,   BorderLayout.EAST);
        return wrapper;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JLabel kpiVal(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_KPI_VAL);
        return lbl;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

