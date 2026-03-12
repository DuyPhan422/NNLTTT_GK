package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Schedule;
import com.company.ems.model.Teacher;
import com.company.ems.service.ClassService;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel Lịch dạy cho Teacher — Weekly Grid View.
 */
public class ScheduleTeacherPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale            VI         = Locale.forLanguageTag("vi");

    private record SessionBand(String name, int startHour, int endHour) {}
    private static final List<SessionBand> SESSIONS = List.of(
            new SessionBand("Sáng\n07:00–12:00",   7, 12),
            new SessionBand("Trưa\n12:00–17:00",   12, 17),
            new SessionBand("Chiều\n17:00–21:00",  17, 21)
    );

    private static final String[] DOW_LABELS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};

    // ── Services ──────────────────────────────────────────────────────────
    private final ScheduleService scheduleService;
    private final ClassService    classService;
    private final RoomService     roomService;
    private final Teacher         currentTeacher;

    // ── State ─────────────────────────────────────────────────────────────
    private LocalDate              weekStart;
    private List<Schedule>         weekSchedules  = new ArrayList<>();
    private List<Class>            teacherClasses = new ArrayList<>();
    private final Map<Long, Integer> classColorMap = new LinkedHashMap<>();

    // ── UI ────────────────────────────────────────────────────────────────
    private final JLabel            lblWeekRange;
    private final JComboBox<String> cbFilterClass;
    private final JPanel            gridPanel;
    private final JLabel            lblStatus;

    public ScheduleTeacherPanel(ScheduleService scheduleService,
                                ClassService classService,
                                RoomService roomService,
                                Teacher currentTeacher) {
        this.scheduleService = scheduleService;
        this.classService    = classService;
        this.roomService     = roomService;
        this.currentTeacher  = currentTeacher;
        this.weekStart       = LocalDate.now().with(DayOfWeek.MONDAY);

        lblWeekRange = new JLabel();
        lblWeekRange.setFont(Theme.FONT_TITLE);
        lblWeekRange.setForeground(Theme.TEXT_MAIN);

        cbFilterClass = new JComboBox<>();
        cbFilterClass.setFont(Theme.FONT_PLAIN);
        cbFilterClass.addActionListener(e -> renderGrid());

        gridPanel = new JPanel();
        lblStatus  = new JLabel(" ");
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_MUTED);

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_PAGE);
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

        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navRow.setOpaque(false);

        JButton btnPrev  = ComponentFactory.navButton("◀ Tuần trước");
        JButton btnToday = ComponentFactory.navButton("Tuần này");
        JButton btnNext  = ComponentFactory.navButton("Tuần sau ▶");

        btnPrev .addActionListener(e -> { weekStart = weekStart.minusWeeks(1); loadData(); });
        btnToday.addActionListener(e -> { weekStart = LocalDate.now().with(DayOfWeek.MONDAY); loadData(); });
        btnNext .addActionListener(e -> { weekStart = weekStart.plusWeeks(1); loadData(); });

        navRow.add(btnPrev);
        navRow.add(lblWeekRange);
        navRow.add(btnToday);
        navRow.add(btnNext);
        bar.add(navRow, BorderLayout.NORTH);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JLabel filterLbl = new JLabel("Lọc lớp:");
        filterLbl.setFont(Theme.FONT_SMALL);
        filterLbl.setForeground(Theme.TEXT_MUTED);

        cbFilterClass.setPreferredSize(new Dimension(200, 30));
        filterRow.add(filterLbl);
        filterRow.add(cbFilterClass);

        JButton btnRefresh = ComponentFactory.navButton("↻ Làm mới");
        btnRefresh.addActionListener(e -> loadData());
        filterRow.add(btnRefresh);
        bar.add(filterRow, BorderLayout.SOUTH);

        return bar;
    }

    // ── Grid ──────────────────────────────────────────────────────────────

    private JScrollPane buildGrid() {
        gridPanel.setBackground(Theme.BG_CARD);
        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
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

        LocalDate weekEnd = weekStart.plusDays(5);

        // ── Header row ────────────────────────────────────────────────────
        JPanel corner = new JPanel();
        corner.setBackground(Theme.BG_HEADER);
        corner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER));
        gridPanel.add(corner);

        for (int d = 0; d < 6; d++) {
            LocalDate day = weekStart.plusDays(d);
            boolean isToday = day.equals(LocalDate.now());

            JPanel dayHeader = new JPanel(new BorderLayout());
            dayHeader.setBackground(isToday ? Theme.TODAY_BG : Theme.BG_HEADER);
            dayHeader.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, d < 5 ? 1 : 0, Theme.BORDER),
                    new EmptyBorder(6, 8, 6, 8)));

            JLabel dowLbl = new JLabel(DOW_LABELS[d], SwingConstants.CENTER);
            dowLbl.setFont(Theme.FONT_BOLD);
            dowLbl.setForeground(isToday ? Theme.PRIMARY : Theme.TEXT_MAIN);

            JLabel dateLbl = new JLabel(day.format(DATE_FMT), SwingConstants.CENTER);
            dateLbl.setFont(Theme.FONT_SMALL);
            dateLbl.setForeground(isToday ? Theme.PRIMARY : Theme.TEXT_MUTED);

            dayHeader.add(dowLbl,  BorderLayout.CENTER);
            dayHeader.add(dateLbl, BorderLayout.SOUTH);
            gridPanel.add(dayHeader);
        }

        // ── Session rows ──────────────────────────────────────────────────
        for (SessionBand session : SESSIONS) {
            JPanel sessionLbl = new JPanel(new GridBagLayout());
            sessionLbl.setBackground(Theme.BG_HEADER);
            sessionLbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER));
            sessionLbl.setPreferredSize(new Dimension(80, 110));

            JLabel sLbl = new JLabel("<html><div style='text-align:center;'>"
                    + session.name().replace("\n", "<br>") + "</div></html>",
                    SwingConstants.CENTER);
            sLbl.setFont(Theme.FONT_SMALL);
            sLbl.setForeground(Theme.TEXT_MUTED);
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
                boolean lastSession = (SESSIONS.indexOf(session) == SESSIONS.size() - 1);

                JPanel cell = new JPanel();
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBackground(isToday ? Theme.TODAY_BG : Theme.BG_CARD);
                cell.setBorder(BorderFactory.createMatteBorder(
                        0, 0, lastSession ? 0 : 1, lastDay ? 0 : 1, Theme.BORDER));
                cell.setMinimumSize(new Dimension(120, 110));
                cell.setPreferredSize(new Dimension(160, 110));

                if (cellSchedules.isEmpty()) {
                    gridPanel.add(cell);
                    continue;
                }

                for (Schedule sch : cellSchedules) {
                    JPanel bloc = buildScheduleBloc(sch);
                    cell.add(Box.createVerticalStrut(4));
                    cell.add(bloc);
                }
                cell.add(Box.createVerticalStrut(4));
                gridPanel.add(cell);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();

        int total = filtered.size();
        lblStatus.setText("Tuần " + weekStart.format(FULL_FMT) + " – " + weekEnd.format(FULL_FMT)
                + "  |  " + total + " buổi học");
    }

    private JPanel buildScheduleBloc(Schedule sch) {
        Long classId  = sch.getClazz().getClassId();
        int colorIdx  = classColorMap.getOrDefault(classId, 0) % Theme.BLOC_BG.length;

        Color bg     = Theme.BLOC_BG[colorIdx];
        Color border = Theme.BLOC_BORDER[colorIdx];

        JPanel bloc = new JPanel(new BorderLayout(0, 2));
        bloc.setBackground(bg);
        bloc.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(4, 6, 4, 6)));
        bloc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        bloc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bloc.setAlignmentX(Component.LEFT_ALIGNMENT);

        String className = sch.getClazz().getClassName();
        JLabel lblClass = new JLabel(className.length() > 18
                ? className.substring(0, 17) + "…" : className);
        lblClass.setFont(Theme.FONT_BOLD);
        lblClass.setForeground(Theme.PRIMARY);
        bloc.add(lblClass, BorderLayout.NORTH);

        String timeStr = sch.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                + " – " + sch.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String roomStr = sch.getRoom() != null ? "📍 " + sch.getRoom().getRoomName() : "📍 Chưa có phòng";
        JLabel lblDetail = new JLabel("<html><span style='color:#475569'>" + timeStr
                + "</span><br><span style='color:#64748b'>" + roomStr + "</span></html>");
        lblDetail.setFont(Theme.FONT_SMALL);
        bloc.add(lblDetail, BorderLayout.CENTER);

        bloc.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { showBlocPopup(bloc, sch, e); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { bloc.setBackground(border); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { bloc.setBackground(bg); }
        });

        return bloc;
    }

    private void showBlocPopup(JPanel bloc, Schedule sch, java.awt.event.MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Theme.BG_CARD);

        JMenuItem info = new JMenuItem("<html><b>" + sch.getClazz().getClassName() + "</b><br>"
                + "<span style='color:#64748b;font-size:10px'>"
                + sch.getStudyDate().format(FULL_FMT) + "  "
                + sch.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                + "–" + sch.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                + "</span></html>");
        info.setEnabled(false);
        info.setFont(Theme.FONT_SMALL);
        popup.add(info);
        popup.addSeparator();

        JMenuItem rescheduleItem = new JMenuItem("📅  Dời lịch buổi này...");
        rescheduleItem.setFont(Theme.FONT_PLAIN);
        rescheduleItem.addActionListener(ae -> openRescheduleDialog(sch));
        popup.add(rescheduleItem);

        popup.show(bloc, e.getX(), e.getY());
    }

    private void openRescheduleDialog(Schedule sch) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        RescheduleDialog dlg = new RescheduleDialog(owner, sch, scheduleService, roomService);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            teacherClasses = (currentTeacher != null)
                    ? classService.findByTeacherId(currentTeacher.getTeacherId())
                    : classService.findAll();

            classColorMap.clear();
            int[] colorCounter = {0};
            teacherClasses.forEach(c -> classColorMap.put(c.getClassId(), colorCounter[0]++));

            String prevFilter = (String) cbFilterClass.getSelectedItem();
            cbFilterClass.removeAllItems();
            cbFilterClass.addItem("Tất cả lớp");
            teacherClasses.stream().map(Class::getClassName).forEach(cbFilterClass::addItem);
            if (prevFilter != null) cbFilterClass.setSelectedItem(prevFilter);

            LocalDate weekEnd = weekStart.plusDays(5);
            List<Schedule> allWeek = scheduleService.findByDateRange(weekStart, weekEnd);

            Set<Long> myClassIds = teacherClasses.stream()
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
                    "Không thể tải dữ liệu lịch: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}

