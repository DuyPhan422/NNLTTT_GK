package com.company.ems.ui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Factory tạo các Swing component tái sử dụng theo chuẩn design system.
 *
 * <p>Nguyên tắc:
 * <ul>
 *   <li>Mọi panel PHẢI dùng các method này — không tự tạo JButton thủ công.</li>
 *   <li>Thêm biến thể mới bằng cách thêm method mới (OCP), không sửa code cũ.</li>
 *   <li>Mọi method đều là {@code static} — không cần khởi tạo instance (utility class).</li>
 * </ul>
 */
public final class ComponentFactory {

    private ComponentFactory() {}

    // ══════════════════════════════════════════════════════════════════════
    //  BUTTONS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Nút chính (xanh dương đậm, chữ trắng) — dùng cho hành động lưu / xác nhận.
     */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(Theme.PRIMARY);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(hoverListener(btn, Theme.PRIMARY, Theme.PRIMARY_H));
        return btn;
    }

    /**
     * Nút phụ (viền, nền trắng) — dùng cho xóa trắng / huỷ bỏ không nguy hiểm.
     */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_PLAIN);
        btn.setForeground(Theme.TEXT_MAIN);
        btn.setBackground(Theme.BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Nút nguy hiểm (đỏ) — dùng cho xóa / hủy lớp.
     */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(Theme.DANGER);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(hoverListener(btn, Theme.DANGER, Theme.DANGER_H));
        return btn;
    }

    /**
     * Nút điều hướng tuần (outline nhạt) — dùng trong ScheduleTeacherPanel/StudentPanel.
     */
    public static JButton navButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_PLAIN);
        btn.setForeground(Theme.PRIMARY);
        btn.setBackground(Theme.BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(5, 12, 5, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(hoverListener(btn, Theme.BG_CARD, Theme.ITEM_HOVER));
        return btn;
    }

    /**
     * Nút icon nhỏ (làm mới, refresh) trong sidebar footer.
     */
    public static JButton iconButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_SMALL);
        btn.setForeground(Theme.PRIMARY);
        btn.setBackground(Theme.BG_SIDEBAR);
        btn.setBorder(new EmptyBorder(4, 8, 4, 8));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  KPI CARDS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tạo một KPI card hoàn chỉnh.
     *
     * @param label    nhãn mô tả (vd: "Tổng lớp")
     * @param valueRef JLabel giá trị — caller giữ reference để update sau
     * @param accent   màu số liệu
     */
    public static JPanel kpiCard(String label, JLabel valueRef, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(14, 18, 14, 18)));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(Theme.FONT_KPI_LBL);
        lblLabel.setForeground(Theme.TEXT_MUTED);

        valueRef.setFont(Theme.FONT_KPI_VAL);
        valueRef.setForeground(accent);

        card.add(lblLabel, BorderLayout.NORTH);
        card.add(valueRef, BorderLayout.CENTER);
        return card;
    }

    /**
     * Tạo JLabel dùng làm giá trị KPI (khởi tạo với "—").
     */
    public static JLabel kpiValueLabel() {
        JLabel l = new JLabel("—");
        l.setFont(Theme.FONT_KPI_VAL);
        return l;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BADGES / TAGS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Badge trạng thái nhỏ (nền màu, chữ trắng) — dùng trong sidebar class list.
     */
    public static JLabel statusBadge(String text, Color bg) {
        JLabel badge = new JLabel(text);
        badge.setFont(Theme.FONT_BADGE);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(Color.WHITE);
        badge.setBorder(new EmptyBorder(2, 6, 2, 6));
        return badge;
    }

    /**
     * Badge xếp loại (không có nền, chỉ in đậm màu) — dùng trong bảng kết quả.
     */
    public static JLabel gradeBadge(String text) {
        JLabel badge = new JLabel(text);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(Theme.gradeColor(text));
        return badge;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EMPTY STATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Panel "chưa chọn / trống" — dùng ở right-side trước khi user chọn item.
     *
     * @param message vd: "← Chọn một lớp để nhập điểm"
     */
    public static JPanel emptyState(String message) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PAGE);
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SEARCH FIELD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * JTextField chuẩn với placeholder — dùng trong toolbar tìm kiếm.
     */
    public static JTextField searchField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(Theme.FONT_PLAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORM COMPONENTS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * JTextField cho form nhập liệu với placeholder.
     */
    public static JTextField formField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(Theme.FONT_PLAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    /**
     * JTextField cho form nhập liệu không có placeholder.
     */
    public static JTextField formField() {
        return formField("");
    }

    /**
     * JLabel nhãn cho field trong form.
     */
    public static JLabel formLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL_BOLD);
        lbl.setForeground(Theme.TEXT_MUTED);
        return lbl;
    }

    /**
     * JTextArea cho form nhập liệu nhiều dòng.
     */
    public static JTextArea formTextArea(int rows) {
        JTextArea ta = new JTextArea(rows, 0);
        ta.setFont(Theme.FONT_PLAIN);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }


    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR CLASS ITEM
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tạo một item lớp học trong sidebar (dùng cho ResultTeacherPanel, AttendanceTeacherPanel, v.v.).
     *
     * @param className  tên lớp
     * @param subText    dòng phụ (tên khoá học)
     * @param statusText trạng thái lớp
     * @param onClick    lambda khi click
     * @param activeRef  mảng 1 phần tử giữ reference panel đang active (hack để deactivate cũ)
     */
    public static JPanel classListItem(String className,
                                       String subText,
                                       String statusText,
                                       Runnable onClick,
                                       JPanel[] activeRef) {
        JPanel item = new JPanel(new BorderLayout(0, 3));
        item.setOpaque(true);
        item.setBackground(Theme.BG_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        item.setMinimumSize(new Dimension(0, 62));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(className);
        lblName.setFont(Theme.FONT_BOLD);
        lblName.setForeground(Theme.TEXT_MAIN);

        JLabel badge = statusBadge(statusText, Theme.classStatusColor(statusText));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblName, BorderLayout.CENTER);
        top.add(badge,   BorderLayout.EAST);

        String trimmed = subText != null && subText.length() > 30
                ? subText.substring(0, 28) + "…" : (subText != null ? subText : "");
        JLabel lblSub = new JLabel(trimmed);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_MUTED);

        item.add(top,    BorderLayout.NORTH);
        item.add(lblSub, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (item != activeRef[0]) item.setBackground(Theme.ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (item != activeRef[0]) item.setBackground(Theme.BG_SIDEBAR);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (activeRef[0] != null) activeRef[0].setBackground(Theme.BG_SIDEBAR);
                activeRef[0] = item;
                item.setBackground(Theme.ITEM_ACTIVE);
                onClick.run();
            }
        });
        return item;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR WRAPPER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tạo sidebar panel hoàn chỉnh gồm: header + search + list + footer refresh.
     *
     * @param title       tiêu đề sidebar (vd: "Lớp của tôi")
     * @param searchField JTextField tìm kiếm (caller giữ reference để add listener)
     * @param listPanel   JPanel chứa các item (BoxLayout Y)
     * @param onRefresh   callback khi bấm nút ↻
     */
    public static JPanel sidebar(String title,
                                  JTextField searchField,
                                  JPanel listPanel,
                                  Runnable onRefresh) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBackground(Theme.BG_SIDEBAR);
        header.setBorder(new EmptyBorder(16, 12, 12, 12));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_HEADER);
        lblTitle.setForeground(Theme.TEXT_MAIN);
        header.add(lblTitle,     BorderLayout.NORTH);
        header.add(searchField,  BorderLayout.SOUTH);
        sidebar.add(header, BorderLayout.NORTH);

        // List scroll
        listPanel.setBackground(Theme.BG_SIDEBAR);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_SIDEBAR);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(scroll, BorderLayout.CENTER);

        // Footer refresh
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footer.setBackground(Theme.BG_SIDEBAR);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        JButton btnRefresh = iconButton("↻", "Làm mới danh sách");
        btnRefresh.addActionListener(e -> onRefresh.run());
        footer.add(btnRefresh);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Tạo hover mouse listener đổi background khi vào/ra. */
    private static java.awt.event.MouseAdapter hoverListener(JButton btn,
                                                              Color normal,
                                                              Color hover) {
        return new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(normal); }
        };
    }
}

