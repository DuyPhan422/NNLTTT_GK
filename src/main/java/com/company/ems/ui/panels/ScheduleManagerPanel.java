package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.model.enums.ClassStatus;
import com.company.ems.service.ClassService;
import com.company.ems.service.CourseService;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel tổng hợp: bên trái là sidebar phân loại lớp học (theo Khóa học / Trạng thái),
 * bên phải là SchedulePanel hiển thị lịch học của lớp đó.
 *
 * Sidebar hỗ trợ:
 * - Combobox view mode: "Theo Khóa học" (default) / "Theo Trạng thái"
 * - Accordion: click header → mở/đóng danh sách lớp bên dưới
 * - Search: nhập keyword → flat list, xoá keyword → quay lại nhóm
 */
public class ScheduleManagerPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClassService    classService;
    private final CourseService   courseService;
    private final SchedulePanel   schedulePanel;
    private final JPanel          classListPanel;
    private final JTextField      searchField;
    private final JComboBox<String> cbViewMode;

    private List<Class> allClasses;
    private JPanel      activeItem = null;

    public ScheduleManagerPanel(ClassService classService,
                                 CourseService courseService,
                                 ScheduleService scheduleService,
                                 RoomService roomService) {
        this.classService   = classService;
        this.courseService   = courseService;
        this.schedulePanel  = new SchedulePanel(scheduleService, roomService);
        this.classListPanel = new JPanel();
        this.searchField    = new JTextField();
        this.cbViewMode     = new JComboBox<>(new String[]{"Theo Khóa học", "Theo Trạng thái"});

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildClassSidebar(), schedulePanel);
        split.setDividerLocation(280);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setContinuousLayout(true);

        add(split, BorderLayout.CENTER);
        loadClasses();
    }

    // ── Left sidebar: danh sách lớp ──────────────────────────────────────

    private JPanel buildClassSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER));

        // ── Header: title + view mode + search ────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.BG_SIDEBAR);
        header.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));

        JLabel title = new JLabel("Chọn lớp học");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(8));

        // View mode combobox
        cbViewMode.setFont(Theme.FONT_PLAIN);
        cbViewMode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cbViewMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbViewMode.addActionListener(e -> renderSidebar());
        header.add(cbViewMode);
        header.add(Box.createVerticalStrut(8));

        // Search field
        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm tên lớp...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { renderSidebar(); }
        });
        header.add(searchField);
        sidebar.add(header, BorderLayout.NORTH);

        // ── List panel ────────────────────────────────────────────────────
        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        classListPanel.setBackground(Theme.BG_SIDEBAR);

        JScrollPane scroll = new JScrollPane(classListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footer.setBackground(Theme.BG_SIDEBAR);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER));
        JButton refreshBtn = new JButton("↻ Làm mới");
        refreshBtn.setFont(Theme.FONT_SMALL);
        refreshBtn.setForeground(Theme.PRIMARY);
        refreshBtn.setBackground(Theme.BG_SIDEBAR);
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadClasses());
        footer.add(refreshBtn);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    // ── Data ─────────────────────────────────────────────────────────────

    private void loadClasses() {
        try {
            allClasses = classService.findAll();
            renderSidebar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải danh sách lớp: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Render Sidebar ───────────────────────────────────────────────────

    private void renderSidebar() {
        classListPanel.removeAll();
        activeItem = null;

        if (allClasses == null || allClasses.isEmpty()) {
            addEmptyLabel("Không có lớp nào");
            classListPanel.revalidate();
            classListPanel.repaint();
            return;
        }

        String keyword = searchField.getText().trim().toLowerCase();

        if (!keyword.isEmpty()) {
            // Search mode: flat list
            List<Class> filtered = allClasses.stream()
                    .filter(c -> c.getClassName().toLowerCase().contains(keyword))
                    .toList();
            if (filtered.isEmpty()) {
                addEmptyLabel("Không tìm thấy lớp \"" + searchField.getText().trim() + "\"");
            } else {
                filtered.forEach(c -> classListPanel.add(buildClassItem(c)));
            }
        } else {
            // Grouped view
            String viewMode = (String) cbViewMode.getSelectedItem();
            if ("Theo Trạng thái".equals(viewMode)) {
                renderByStatus();
            } else {
                renderByCourse();
            }
        }

        // Glue đẩy tất cả content lên trên, tránh giãn cách đều trong BoxLayout
        classListPanel.add(Box.createVerticalGlue());

        classListPanel.revalidate();
        classListPanel.repaint();
    }

    private void renderByCourse() {
        try {
            List<Course> activeCourses = courseService.findByStatus(ActiveStatus.HOAT_DONG.getValue());

            for (Course course : activeCourses) {
                List<Class> classesOfCourse = allClasses.stream()
                        .filter(c -> c.getCourse() != null
                                && c.getCourse().getCourseId().equals(course.getCourseId()))
                        .toList();
                if (classesOfCourse.isEmpty()) continue;

                classListPanel.add(buildSectionHeader(
                        course.getCourseName() + " (" + classesOfCourse.size() + ")",
                        classesOfCourse));
            }

            // Lớp không thuộc course nào hoặc course inactive
            List<Long> activeCourseIds = activeCourses.stream()
                    .map(Course::getCourseId).toList();
            List<Class> orphans = allClasses.stream()
                    .filter(c -> c.getCourse() == null
                            || !activeCourseIds.contains(c.getCourse().getCourseId()))
                    .toList();
            if (!orphans.isEmpty()) {
                classListPanel.add(buildSectionHeader(
                        "Khác (" + orphans.size() + ")", orphans));
            }
        } catch (Exception e) {
            addEmptyLabel("Lỗi tải khóa học: " + e.getMessage());
        }
    }

    private void renderByStatus() {
        for (ClassStatus status : ClassStatus.values()) {
            List<Class> classesOfStatus = allClasses.stream()
                    .filter(c -> status.getValue().equals(c.getStatus()))
                    .toList();

            classListPanel.add(buildSectionHeader(
                    status.getValue() + " (" + classesOfStatus.size() + ")",
                    classesOfStatus));
        }
    }

    // ── Accordion Section Header ─────────────────────────────────────────

    private JPanel buildSectionHeader(String title, List<Class> classes) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Theme.BG_HEADER);
        headerBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        headerBar.setMinimumSize(new Dimension(0, 36));
        headerBar.setPreferredSize(new Dimension(260, 36));
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        headerBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblIcon = new JLabel("▶ ");
        lblIcon.setFont(Theme.FONT_SMALL);
        lblIcon.setForeground(Theme.TEXT_MUTED);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_BOLD);
        lblTitle.setForeground(Theme.TEXT_MAIN);

        headerBar.add(lblIcon,  BorderLayout.WEST);
        headerBar.add(lblTitle, BorderLayout.CENTER);

        // Content panel (collapsed by default)
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setVisible(false);

        classes.forEach(c -> content.add(buildClassItem(c)));

        // Click header → toggle
        headerBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean expanded = !content.isVisible();
                content.setVisible(expanded);
                lblIcon.setText(expanded ? "▼ " : "▶ ");
                classListPanel.revalidate();
                classListPanel.repaint();
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                headerBar.setBackground(Theme.ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                headerBar.setBackground(Theme.BG_HEADER);
            }
        });

        section.add(headerBar);
        section.add(content);
        return section;
    }

    // ── Class Item ───────────────────────────────────────────────────────

    private JPanel buildClassItem(Class c) {
        JPanel item = new JPanel(new BorderLayout(0, 2));
        item.setOpaque(true);
        item.setBackground(Theme.BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setMinimumSize(new Dimension(0, 64));
        item.setPreferredSize(new Dimension(260, 64));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(c.getClassName());
        lblName.setFont(Theme.FONT_BOLD);
        lblName.setForeground(Theme.TEXT_MAIN);

        JLabel lblStatus = new JLabel(c.getStatus());
        lblStatus.setFont(Theme.FONT_BADGE);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(Theme.classStatusColor(c.getStatus()));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName,   BorderLayout.CENTER);
        top.add(lblStatus, BorderLayout.EAST);

        String dateRange = "";
        if (c.getStartDate() != null) {
            dateRange = c.getStartDate().format(DATE_FMT);
            if (c.getEndDate() != null) dateRange += " – " + c.getEndDate().format(DATE_FMT);
        }
        String subText = (c.getCourse() != null ? c.getCourse().getCourseName() : "")
                + (dateRange.isEmpty() ? "" : "  ·  " + dateRange);
        JLabel lblSub = new JLabel(subText);
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
                schedulePanel.loadForClass(c);
            }
        });

        return item;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void addEmptyLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        classListPanel.add(Box.createVerticalStrut(20));
        classListPanel.add(lbl);
    }
}
