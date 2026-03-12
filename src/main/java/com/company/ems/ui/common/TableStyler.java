package com.company.ems.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * Tiện ích cấu hình JTable theo chuẩn design system.
 * SRP: chỉ chứa UI styling, không có business logic.
 * Mọi JTable trong ứng dụng gọi {@link #applyDefaults(JTable)} sau khi khởi tạo.
 */
public final class TableStyler {

    private TableStyler() {}

    // ─── Core setup ──────────────────────────────────────────────────────

    /** Áp dụng toàn bộ style mặc định: font, rowHeight, grid, colors, header. */
    public static void applyDefaults(JTable table) {
        table.setFont(Theme.FONT_PLAIN);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.BG_CARD);
        table.setSelectionBackground(Theme.ROW_SELECT);
        table.setSelectionForeground(Theme.TEXT_MAIN);
        table.setFillsViewportHeight(true);

        var header = table.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        header.setReorderingAllowed(false);
    }

    /**
     * Ẩn cột theo index (width = 0, không resize được).
     * Dùng cho cột ID kỹ thuật luôn ở index 0.
     */
    public static void hideColumn(JTable table, int colIndex) {
        var col = table.getColumnModel().getColumn(colIndex);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
        col.setResizable(false);
    }

    /**
     * Gắn TableRowSorter và trả về để caller dùng cho setRowFilter.
     *
     * @param unsortableCols các cột không cho sort (vd: STT, cột ẩn)
     */
    public static TableRowSorter<DefaultTableModel> attachSorter(
            JTable table, DefaultTableModel model, int... unsortableCols) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        for (int col : unsortableCols) sorter.setSortable(col, false);
        table.setRowSorter(sorter);
        return sorter;
    }

    // ─── Scroll wrappers ─────────────────────────────────────────────────

    /** JScrollPane có viền chuẩn — dùng khi table là thành phần chính của panel. */
    public static JScrollPane scrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Theme.BG_CARD);
        return sp;
    }

    /** JScrollPane không viền — dùng khi table nằm bên trong card đã có border. */
    public static JScrollPane scrollPaneNoBorder(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(Theme.BG_CARD);
        return sp;
    }

    // ─── Cell renderers ──────────────────────────────────────────────────

    /**
     * Renderer row-striping chuẩn (even/odd + select highlight).
     * Dùng cho bảng CRUD thông thường không có column tint đặc biệt.
     */
    public static DefaultTableCellRenderer stripedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                setForeground(Theme.TEXT_MAIN);
                return this;
            }
        };
    }

    /**
     * Renderer cho cột Xếp loại — in đậm, tô màu theo grade, căn giữa.
     */
    public static DefaultTableCellRenderer gradeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setFont(Theme.FONT_BOLD);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                setForeground(sel ? Theme.TEXT_MAIN : Theme.gradeColor(v != null ? v.toString() : ""));
                return this;
            }
        };
    }

    /**
     * Renderer căn giữa + in đậm với màu nền riêng — dùng cho cột Điểm tổng.
     */
    public static DefaultTableCellRenderer centeredBoldRenderer(Color evenBg, Color oddBg) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setFont(Theme.FONT_BOLD);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? evenBg : oddBg));
                setForeground(Theme.TEXT_MAIN);
                return this;
            }
        };
    }

    /**
     * Renderer căn giữa với màu nền riêng — dùng cho cột điểm thành phần QT1/QT2/CK.
     */
    public static DefaultTableCellRenderer centeredRenderer(Color evenBg, Color oddBg) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? evenBg : oddBg));
                setForeground(Theme.TEXT_MAIN);
                return this;
            }
        };
    }

    /**
     * Renderer cho cột Hạng — in đậm, màu tím, căn giữa.
     */
    public static DefaultTableCellRenderer rankRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setFont(Theme.FONT_BOLD);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                setForeground(sel ? Theme.TEXT_MAIN : Theme.PURPLE);
                return this;
            }
        };
    }
}

