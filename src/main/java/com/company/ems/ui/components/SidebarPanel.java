package com.company.ems.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Sidebar điều hướng bên trái — dùng chung cho MainFrame (Admin).
 * Nhận 2 callback: onNavigate (chuyển panel) và onLogout (đăng xuất).
 */
public class SidebarPanel extends JPanel {

    private static final Color BG_COLOR      = new Color(30,  41,  59);
    private static final Color ITEM_HOVER    = new Color(51,  65,  85);
    private static final Color ITEM_ACTIVE   = new Color(37,  99,  235);
    private static final Color TEXT_COLOR    = new Color(203, 213, 225);
    private static final Color TEXT_ACTIVE   = Color.WHITE;
    private static final Color DIVIDER_COLOR = new Color(51,  65,  85);
    private static final Color DANGER        = new Color(220, 38,  38);
    private static final Color DANGER_HOVER  = new Color(185, 28,  28);

    private String activeItem;
    private final Consumer<String> onNavigate;
    private final String[][]       menuItems;

    // Menu đầy đủ (dành cho Admin) — dùng làm mặc định
    public static final String[][] DEFAULT_MENU_ITEMS = {
        {"👥",  "Học viên",   "students"},
        {"👨", "Giáo viên",  "teachers"},
        {"🧑", "Nhân viên",  "staffs"},
        {"📚",  "Khóa học",   "courses"},
        {"🏫",  "Lớp học",    "classes"},
        {"📋",  "Đăng ký",    "enrollments"},
        {"💰",  "Thanh toán", "payments"},
        {"🗓️", "Lịch học",   "schedules"},
        {"✅",  "Điểm danh",  "attendances"},
        {"🚪",  "Phòng học",  "rooms"},
    };

    /** Constructor dùng menu mặc định (backward-compatible). */
    public SidebarPanel(Consumer<String> onNavigate, Runnable onLogout) {
        this(DEFAULT_MENU_ITEMS, onNavigate, onLogout);
    }

    /** Constructor nhận menu tùy chỉnh — dùng cho Staff / các role khác. */
    public SidebarPanel(String[][] menuItems, Consumer<String> onNavigate, Runnable onLogout) {
        this.menuItems  = menuItems;
        this.onNavigate = onNavigate;
        // Khởi tạo activeItem từ mục đầu tiên trong menu để highlight đúng ngay khi mở
        this.activeItem = (menuItems != null && menuItems.length > 0) ? menuItems[0][2] : "students";
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(220, 0));

        // ── Logo ──────────────────────────────────────────────────────────
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(BG_COLOR);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        JLabel logoLabel = new JLabel("🎓 Language Center");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoPanel.add(logoLabel, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER_COLOR);
        logoPanel.add(sep, BorderLayout.SOUTH);
        add(logoPanel, BorderLayout.NORTH);

        // ── Menu items ────────────────────────────────────────────────────
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(BG_COLOR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        for (String[] item : this.menuItems) {
            menuPanel.add(createMenuItem(item[0], item[1], item[2]));
        }
        menuPanel.add(Box.createVerticalGlue());
        add(menuPanel, BorderLayout.CENTER);

        // ── Nút Đăng xuất ────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 16, 8));

        JButton btnLogout = new JButton("⏻  Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(DANGER);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(0, 40));
        btnLogout.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnLogout.setBackground(DANGER_HOVER); }
            public void mouseExited (MouseEvent e) { btnLogout.setBackground(DANGER); }
        });
        btnLogout.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                    SwingUtilities.getWindowAncestor(SidebarPanel.this),
                    "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION && onLogout != null) onLogout.run();
        });
        bottomPanel.add(btnLogout, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createMenuItem(String icon, String label, String panelName) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        item.setBackground(BG_COLOR);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inner.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textLabel.setForeground(panelName.equals(activeItem) ? TEXT_ACTIVE : TEXT_COLOR);

        inner.add(iconLabel);
        inner.add(textLabel);
        item.add(inner, BorderLayout.CENTER);

        JPanel indicator = new JPanel();
        indicator.setPreferredSize(new Dimension(4, 0));
        indicator.setBackground(panelName.equals(activeItem) ? ITEM_ACTIVE : BG_COLOR);
        item.add(indicator, BorderLayout.WEST);

        if (panelName.equals(activeItem)) item.setBackground(new Color(37, 99, 235, 30));

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                setActiveItem(panelName);
                onNavigate.accept(panelName);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (!panelName.equals(activeItem)) {
                    item.setBackground(ITEM_HOVER);
                    inner.setBackground(ITEM_HOVER);
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!panelName.equals(activeItem)) {
                    item.setBackground(BG_COLOR);
                    inner.setBackground(new Color(0, 0, 0, 0));
                    inner.setOpaque(false);
                }
            }
        });
        return item;
    }

    public void setActiveItem(String panelName) {
        this.activeItem = panelName;
        Component menuPanel = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (menuPanel instanceof JPanel mp) {
            mp.removeAll();
            for (String[] item : this.menuItems) mp.add(createMenuItem(item[0], item[1], item[2]));
            mp.add(Box.createVerticalGlue());
            mp.revalidate();
            mp.repaint();
        }
    }
}
