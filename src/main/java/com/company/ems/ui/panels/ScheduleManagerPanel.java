package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.service.ClassService;
import com.company.ems.service.RoomService;
import com.company.ems.service.ScheduleService;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel tổng hợp: bên trái là danh sách lớp học để chọn,
 * bên phải là SchedulePanel hiển thị lịch học của lớp đó.
 *
 * Đây là panel được đăng ký vào MainFrame với key "schedules".
 */
public class ScheduleManagerPanel extends JPanel {

    private static final Color BG_PAGE      = new Color(248, 250, 252);
    private static final Color BG_SIDEBAR   = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color ITEM_HOVER   = new Color(239, 246, 255);
    private static final Color ITEM_ACTIVE  = new Color(219, 234, 254);

    private static final Font FONT_MAIN   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClassService    classService;
    private final SchedulePanel   schedulePanel;
    private final JPanel          classListPanel;
    private final JTextField      searchField;

    private List<Class> allClasses;
    private JPanel      activeItem = null;

    public ScheduleManagerPanel(ClassService classService,
                                 ScheduleService scheduleService,
                                 RoomService roomService) {
        this.classService   = classService;
        this.schedulePanel  = new SchedulePanel(scheduleService, roomService);
        this.classListPanel = new JPanel();
        this.searchField    = new JTextField();

        setLayout(new BorderLayout());
        setBackground(BG_PAGE);

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
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COLOR));

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(BG_SIDEBAR);
        header.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));

        JLabel title = new JLabel("Chọn lớp học");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);

        searchField.setFont(FONT_MAIN);
        searchField.setPreferredSize(new Dimension(0, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm tên lớp...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { filterClasses(); }
        });
        header.add(searchField, BorderLayout.SOUTH);
        sidebar.add(header, BorderLayout.NORTH);

        // Danh sách lớp
        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        classListPanel.setBackground(BG_SIDEBAR);

        JScrollPane scroll = new JScrollPane(classListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);

        // Footer: nút làm mới
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footer.setBackground(BG_SIDEBAR);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COLOR));
        JButton refreshBtn = new JButton("↻ Làm mới");
        refreshBtn.setFont(FONT_SMALL);
        refreshBtn.setForeground(PRIMARY);
        refreshBtn.setBackground(BG_SIDEBAR);
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
            renderClassList(allClasses);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải danh sách lớp: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterClasses() {
        if (allClasses == null) return;
        String kw = searchField.getText().trim().toLowerCase();
        // Stream + predicate filter, gọn hơn if/else
        renderClassList(kw.isEmpty()
                ? allClasses
                : allClasses.stream()
                        .filter(c -> c.getClassName().toLowerCase().contains(kw))
                        .toList());
    }

    private void renderClassList(List<Class> classes) {
        classListPanel.removeAll();
        activeItem = null;
        if (classes.isEmpty()) {
            JLabel lbl = new JLabel("Không có lớp nào", SwingConstants.CENTER);
            lbl.setFont(FONT_SMALL);
            lbl.setForeground(TEXT_MUTED);
            classListPanel.add(lbl);
        } else {
            // Stream thay cho for-loop
            classes.stream()
                   .map(this::buildClassItem)
                   .forEach(classListPanel::add);
        }
        classListPanel.revalidate();
        classListPanel.repaint();
    }

    private JPanel buildClassItem(Class c) {
        JPanel item = new JPanel(new BorderLayout(0, 2));
        item.setOpaque(true);
        item.setBackground(BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setMinimumSize(new Dimension(0, 64));
        item.setPreferredSize(new Dimension(260, 64));
        item.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Tên lớp + badge trạng thái
        JLabel lblName = new JLabel(c.getClassName());
        lblName.setFont(FONT_BOLD);
        lblName.setForeground(TEXT_MAIN);

        JLabel lblStatus = new JLabel(c.getStatus());
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(statusColor(c.getStatus()));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName,   BorderLayout.CENTER);
        top.add(lblStatus, BorderLayout.EAST);

        // Sub-info: khóa học + ngày
        String dateRange = "";
        if (c.getStartDate() != null) {
            dateRange = c.getStartDate().format(DATE_FMT);
            if (c.getEndDate() != null) dateRange += " – " + c.getEndDate().format(DATE_FMT);
        }
        String subText = (c.getCourse() != null ? c.getCourse().getCourseName() : "")
                + (dateRange.isEmpty() ? "" : "  ·  " + dateRange);
        JLabel lblSub = new JLabel(subText);
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(TEXT_MUTED);

        item.add(top,    BorderLayout.NORTH);
        item.add(lblSub, BorderLayout.CENTER);

        // Hover & click
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (item != activeItem) item.setBackground(ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (item != activeItem) item.setBackground(BG_SIDEBAR);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeItem != null) activeItem.setBackground(BG_SIDEBAR);
                activeItem = item;
                item.setBackground(ITEM_ACTIVE);
                schedulePanel.loadForClass(c);
            }
        });

        return item;
    }

    private static Color statusColor(String status) {
        if (status == null) return new Color(100, 116, 139);
        return switch (status) {
            case "Open", "Ongoing" -> new Color(34, 197, 94);
            case "Planned"         -> new Color(59, 130, 246);
            case "Cancelled"       -> new Color(220, 38, 38);
            default                -> new Color(100, 116, 139); // Completed
        };
    }
}

