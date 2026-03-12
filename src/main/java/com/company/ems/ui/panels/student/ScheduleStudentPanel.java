package com.company.ems.ui.panels.student;

import com.company.ems.model.Class;
import com.company.ems.model.Schedule;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.ScheduleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thời khóa biểu cho Student — Weekly Grid View (Read-Only).
 * Giống ScheduleTeacherPanel nhưng KHÔNG có chức năng dời lịch.
 * Chỉ hiển thị lịch của các lớp học viên đang tham gia.
 */
public class ScheduleStudentPanel extends JPanel {

    // ── Design tokens ─────────────────────────────────────────────────────
    private static final Color BG_PAGE      = new Color(248, 250, 252);
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BG_HEADER    = new Color(241, 245, 249);
    private static final Color BORDER_COL   = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37,  99,  235);
    private static final Color TEXT_MAIN    = new Color(15,  23,  42);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color TODAY_COL    = new Color(239, 246, 255);
    private static final Color TODAY_BORDER = new Color(147, 197, 253);

    private static final Color[] CLASS_COLORS = {
        new Color(219, 234, 254), new Color(220, 252, 231), new Color(254, 243, 199),
        new Color(252, 231, 243), new Color(237, 233, 254), new Color(255, 237, 213)
    };
    private static final Color[] CLASS_BORDER_COLORS = {
        new Color(147, 197, 253), new Color(134, 239, 172), new Color(253, 224, 71),
        new Color(249, 168, 212), new Color(196, 181, 253), new Color(253, 186, 116)
    };

    private static final Font FONT_MAIN   = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,   12);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,   15);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private record SessionBand(String name, int startHour, int endHour) {}
    private static final List<SessionBand> SESSIONS = List.of(
            new SessionBand("Sáng\n07:00–12:00",  7, 12),
            new SessionBand("Trưa\n12:00–17:00",  12, 17),
            new SessionBand("Chiều\n17:00–21:00", 17, 21)
    );
    private static final String[] DOW_LABELS = {"Thứ 2","Thứ 3","Thứ 4","Thứ 5","Thứ 6","Thứ 7"};

    // ── Services ──────────────────────────────────────────────────────────
    private final ScheduleService scheduleService;
    private final ClassService    classService;
    private final Student         currentStudent;

    // ── State ─────────────────────────────────────────────────────────────
    private LocalDate              weekStart;
    private List<Schedule>         weekSchedules  = new ArrayList<>();
    private List<Class>            studentClasses = new ArrayList<>();
    private final Map<Long, Integer> classColorMap = new LinkedHashMap<>();

    // ── UI ────────────────────────────────────────────────────────────────
    private final JLabel            lblWeekRange;
    private final JComboBox<String> cbFilterClass;
    private final JPanel            gridPanel;
    private final JLabel            lblStatus;

    public ScheduleStudentPanel(ScheduleService scheduleService,
                                ClassService classService,
                                Student currentStudent) {
        this.scheduleService = scheduleService;
        this.classService    = classService;
        this.currentStudent  = currentStudent;
        this.weekStart       = LocalDate.now().with(DayOfWeek.MONDAY);

        lblWeekRange  = new JLabel();
        lblWeekRange.setFont(FONT_TITLE);
        lblWeekRange.setForeground(TEXT_MAIN);

        cbFilterClass = new JComboBox<>();
        cbFilterClass.setFont(FONT_MAIN);
        cbFilterClass.addActionListener(e -> renderGrid());

        gridPanel  = new JPanel();
        lblStatus  = new JLabel(" ");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(TEXT_MUTED);

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildGrid(),    BorderLayout.CENTER);
        add(lblStatus,      BorderLayout.SOUTH);

        loadData();
    }

    // ── Top bar ───────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 10));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Row 1 — Week navigation
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navRow.setOpaque(false);

        JButton btnPrev  = navBtn("◀ Tuần trước");
        btnPrev.addActionListener(e -> { weekStart = weekStart.minusWeeks(1); loadData(); });

        JButton btnToday = navBtn("Tuần này");
        btnToday.addActionListener(e -> { weekStart = LocalDate.now().with(DayOfWeek.MONDAY); loadData(); });

        JButton btnNext  = navBtn("Tuần sau ▶");
        btnNext.addActionListener(e -> { weekStart = weekStart.plusWeeks(1); loadData(); });

        navRow.add(btnPrev);
        navRow.add(lblWeekRange);
        navRow.add(btnToday);
        navRow.add(btnNext);
        bar.add(navRow, BorderLayout.NORTH);

        // Row 2 — Filter + readonly notice
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JLabel filterLbl = new JLabel("Lọc lớp:");
        filterLbl.setFont(FONT_SMALL);
        filterLbl.setForeground(TEXT_MUTED);

        cbFilterClass.setPreferredSize(new Dimension(200, 30));
        filterRow.add(filterLbl);
        filterRow.add(cbFilterClass);

        JButton btnRefresh = navBtn("↻ Làm mới");
        btnRefresh.addActionListener(e -> loadData());
        filterRow.add(btnRefresh);

        bar.add(filterRow, BorderLayout.SOUTH);
        return bar;
    }

    // ── Grid ──────────────────────────────────────────────────────────────

    private JScrollPane buildGrid() {
        gridPanel.setBackground(BG_CARD);
        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private void renderGrid() {
        gridPanel.removeAll();

        String selectedClassName = (String) cbFilterClass.getSelectedItem();
        List<Schedule> filtered = weekSchedules.stream()
                .filter(s -> selectedClassName == null
                        || "Tất cả lớp".equals(selectedClassName)
                        || s.getClazz().getClassName().equals(selectedClassName))
                .collect(Collectors.toList());

        int cols = 7;
        int rows = SESSIONS.size() + 1;
        gridPanel.setLayout(new GridLayout(rows, cols, 0, 0));

        // ── Header row ───────────────────────────────────────────────────
        JPanel corner = new JPanel();
        corner.setBackground(BG_HEADER);
        corner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COL));
        gridPanel.add(corner);

        for (int d = 0; d < 6; d++) {
            LocalDate day     = weekStart.plusDays(d);
            boolean isToday   = day.equals(LocalDate.now());

            JPanel dayHeader = new JPanel(new BorderLayout());
            dayHeader.setBackground(isToday ? TODAY_COL : BG_HEADER);
            dayHeader.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, d < 5 ? 1 : 0, BORDER_COL),
                    new EmptyBorder(6, 8, 6, 8)));

            JLabel dowLbl = new JLabel(DOW_LABELS[d], SwingConstants.CENTER);
            dowLbl.setFont(FONT_BOLD);
            dowLbl.setForeground(isToday ? PRIMARY : TEXT_MAIN);

            JLabel dateLbl = new JLabel(day.format(DATE_FMT), SwingConstants.CENTER);
            dateLbl.setFont(FONT_SMALL);
            dateLbl.setForeground(isToday ? PRIMARY : TEXT_MUTED);

            dayHeader.add(dowLbl,  BorderLayout.CENTER);
            dayHeader.add(dateLbl, BorderLayout.SOUTH);
            gridPanel.add(dayHeader);
        }

        // ── Session rows ─────────────────────────────────────────────────
        for (SessionBand session : SESSIONS) {
            JPanel sessionLbl = new JPanel(new GridBagLayout());
            sessionLbl.setBackground(BG_HEADER);
            sessionLbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COL));
            sessionLbl.setPreferredSize(new Dimension(80, 110));

            JLabel sLbl = new JLabel("<html><div style='text-align:center;'>"
                    + session.name().replace("\n", "<br>") + "</div></html>",
                    SwingConstants.CENTER);
            sLbl.setFont(FONT_SMALL);
            sLbl.setForeground(TEXT_MUTED);
            sessionLbl.add(sLbl);
            gridPanel.add(sessionLbl);

            for (int d = 0; d < 6; d++) {
                LocalDate day = weekStart.plusDays(d);

                List<Schedule> cellSchedules = filtered.stream()
                        .filter(s -> s.getStudyDate().equals(day)
                                && s.getStartTime().getHour() >= session.startHour()
                                && s.getStartTime().getHour() < session.endHour())
                        .sorted(Comparator.comparing(Schedule::getStartTime))
                        .collect(Collectors.toList());

                boolean isToday   = day.equals(LocalDate.now());
                boolean lastDay   = (d == 5);
                boolean lastSess  = (SESSIONS.indexOf(session) == SESSIONS.size() - 1);

                JPanel cell = new JPanel();
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBackground(isToday ? TODAY_COL : BG_CARD);
                cell.setBorder(BorderFactory.createMatteBorder(
                        0, 0, lastSess ? 0 : 1, lastDay ? 0 : 1, BORDER_COL));
                cell.setMinimumSize(new Dimension(120, 110));
                cell.setPreferredSize(new Dimension(160, 110));

                for (Schedule sch : cellSchedules) {
                    cell.add(Box.createVerticalStrut(4));
                    cell.add(buildScheduleBloc(sch));
                }
                if (!cellSchedules.isEmpty()) cell.add(Box.createVerticalStrut(4));
                gridPanel.add(cell);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();

        LocalDate weekEnd = weekStart.plusDays(5);
        lblStatus.setText("Tuần " + weekStart.format(FULL_FMT) + " – " + weekEnd.format(FULL_FMT)
                + "  |  " + filtered.size() + " buổi học");
    }

    /** Tạo bloc hiển thị — không có click reschedule */
    private JPanel buildScheduleBloc(Schedule sch) {
        Long classId   = sch.getClazz().getClassId();
        int  colorIdx  = classColorMap.getOrDefault(classId, 0) % CLASS_COLORS.length;
        Color bg       = CLASS_COLORS[colorIdx];
        Color border   = CLASS_BORDER_COLORS[colorIdx];

        JPanel bloc = new JPanel(new BorderLayout(0, 2));
        bloc.setBackground(bg);
        bloc.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(4, 6, 4, 6)));
        bloc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        bloc.setAlignmentX(Component.LEFT_ALIGNMENT);

        String className = sch.getClazz().getClassName();
        JLabel lblClass = new JLabel(className.length() > 18
                ? className.substring(0, 17) + "…" : className);
        lblClass.setFont(FONT_BOLD);
        lblClass.setForeground(PRIMARY);
        bloc.add(lblClass, BorderLayout.NORTH);

        String timeStr = sch.getStartTime().format(TIME_FMT) + " – " + sch.getEndTime().format(TIME_FMT);
        String roomStr = sch.getRoom() != null ? "📍 " + sch.getRoom().getRoomName() : "📍 Chưa có phòng";
        JLabel lblDetail = new JLabel("<html><span style='color:#475569'>" + timeStr
                + "</span><br><span style='color:#64748b'>" + roomStr + "</span></html>");
        lblDetail.setFont(FONT_SMALL);
        bloc.add(lblDetail, BorderLayout.CENTER);

        // Hover effect only (no click action)
        bloc.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { bloc.setBackground(border); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { bloc.setBackground(bg); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { showBlocDetail(sch); }
        });

        return bloc;
    }

    /** Popup thông tin buổi học (read-only) */
    private void showBlocDetail(Schedule sch) {
        String teacher = sch.getClazz().getTeacher() != null
                ? sch.getClazz().getTeacher().getFullName() : "Chưa phân công";
        String room = sch.getRoom() != null ? sch.getRoom().getRoomName() : "Chưa có phòng";

        JOptionPane.showMessageDialog(this,
                "<html><b>" + sch.getClazz().getClassName() + "</b><br><br>"
                + "📅 Ngày: "  + sch.getStudyDate().format(FULL_FMT) + "<br>"
                + "⏰ Giờ: "   + sch.getStartTime().format(TIME_FMT) + " – " + sch.getEndTime().format(TIME_FMT) + "<br>"
                + "📍 Phòng: " + room + "<br>"
                + "👨‍🏫 Giáo viên: " + teacher + "</html>",
                "Chi tiết buổi học", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            // Lấy các lớp mà học viên đang tham gia
            studentClasses = classService.findByStudentId(currentStudent.getStudentId());

            classColorMap.clear();
            int[] idx = {0};
            studentClasses.forEach(c -> classColorMap.put(c.getClassId(), idx[0]++));

            // Cập nhật filter combo
            String prevFilter = (String) cbFilterClass.getSelectedItem();
            cbFilterClass.removeAllItems();
            cbFilterClass.addItem("Tất cả lớp");
            studentClasses.stream().map(Class::getClassName).forEach(cbFilterClass::addItem);
            if (prevFilter != null) cbFilterClass.setSelectedItem(prevFilter);

            // Load lịch tuần
            LocalDate weekEnd = weekStart.plusDays(5);
            List<Schedule> allWeek = scheduleService.findByDateRange(weekStart, weekEnd);

            Set<Long> myClassIds = studentClasses.stream()
                    .map(Class::getClassId)
                    .collect(Collectors.toSet());

            weekSchedules = allWeek.stream()
                    .filter(s -> s.getClazz() != null && myClassIds.contains(s.getClazz().getClassId()))
                    .collect(Collectors.toList());

            lblWeekRange.setText("Tuần: " + weekStart.format(FULL_FMT)
                    + "  –  " + weekStart.plusDays(5).format(FULL_FMT));

            renderGrid();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải lịch học: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Button factory ────────────────────────────────────────────────────

    private JButton navBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setForeground(PRIMARY);
        btn.setBackground(BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(5, 12, 5, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(239, 246, 255)); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(BG_CARD); }
        });
        return btn;
    }
}

