package com.company.ems.ui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        table.setRowHeight(44); // Tăng khoảng trống dòng để "thở" hơn
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.BG_CARD);
        table.setSelectionBackground(Theme.ROW_SELECT);
        table.setSelectionForeground(Theme.TEXT_MAIN);
        table.setFillsViewportHeight(true);

        var header = table.getTableHeader();
        header.setFont(Theme.FONT_SMALL_BOLD);
        header.setBackground(Theme.BG_PAGE); // Nền xám nhạt hiện đại hơn
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 48)); // Header rộng rãi
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        header.setReorderingAllowed(false);
    }

    /**
     * Tự động dãn cột để lấp đầy JScrollPane (nếu tổng bảng nhỏ hơn viewport).
     * Thuật toán sẽ phân bổ pixel dư vào các cột có nội dung dài (cột linh hoạt).
     */
    public static void fitTableColumns(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.PARENT_CHANGED) != 0) {
                if (table.getParent() instanceof JViewport viewport) {
                    viewport.addComponentListener(new java.awt.event.ComponentAdapter() {
                        @Override
                        public void componentResized(java.awt.event.ComponentEvent evt) {
                            resizeToFit(table, viewport);
                        }
                    });
                    SwingUtilities.invokeLater(() -> resizeToFit(table, viewport));
                }
            }
        });
    }

    private static void resizeToFit(JTable table, JViewport viewport) {
        int width = viewport.getWidth();
        var cm = table.getColumnModel();
        int totalPref = 0;
        int stretchCols = 0;

        for (int i = 0; i < cm.getColumnCount(); i++) {
            var col = cm.getColumn(i);
            if (col.getMinWidth() == 0 && col.getMaxWidth() == 0) continue; // Cột ID ẩn

            Object origObj = table.getClientProperty("prefW_" + i);
            int orig;
            if (origObj instanceof Integer io) {
                orig = io;
            } else {
                orig = col.getPreferredWidth();
                table.putClientProperty("prefW_" + i, orig);
            }

            totalPref += orig;
            if (col.getMaxWidth() >= 1000) stretchCols++;
        }

        if (width > totalPref && stretchCols > 0) {
            int extra = (width - totalPref) / stretchCols;
            for (int i = 0; i < cm.getColumnCount(); i++) {
                var col = cm.getColumn(i);
                if (col.getMinWidth() == 0 && col.getMaxWidth() == 0) continue;

                Object origObj = table.getClientProperty("prefW_" + i);
                int orig = (origObj instanceof Integer io) ? io : col.getPreferredWidth();

                if (col.getMaxWidth() >= 1000) {
                    col.setPreferredWidth(orig + extra);
                } else {
                    col.setPreferredWidth(orig);
                }
            }
        } else {
            // Restore originals
            for (int i = 0; i < cm.getColumnCount(); i++) {
                var col = cm.getColumn(i);
                Object origObj = table.getClientProperty("prefW_" + i);
                if (origObj instanceof Integer orig && orig > 0) {
                    col.setPreferredWidth(orig);
                }
            }
        }
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
     */
    public static DefaultTableCellRenderer stripedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                setBackground(sel ? Theme.ROW_SELECT : (row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD));
                setForeground(Theme.TEXT_MAIN);
                setBorder(new EmptyBorder(0, 10, 0, 10)); // Thêm khoảng trắng viền
                return this;
            }
        };
    }

    /**
     * Renderer cho cột Xếp loại.
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
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
    }

    /**
     * Renderer căn giữa + in đậm với màu nền riêng.
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
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
    }

    /**
     * Renderer căn giữa với màu nền riêng.
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
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
    }

    /**
     * Renderer cho cột Hạng — in đậm, màu tím.
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
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
    }
}
