package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.service.ClassService;
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
 * Panel tổng hợp: bên trái là danh sách lớp học để chọn,
 * bên phải là SchedulePanel hiển thị lịch học của lớp đó.
 */
public class ScheduleManagerPanel extends JPanel {

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

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(Theme.BG_SIDEBAR);
        header.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));

        JLabel title = new JLabel("Chọn lớp học");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);

        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setPreferredSize(new Dimension(0, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
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
            lbl.setFont(Theme.FONT_SMALL);
            lbl.setForeground(Theme.TEXT_MUTED);
            classListPanel.add(lbl);
        } else {
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
}
