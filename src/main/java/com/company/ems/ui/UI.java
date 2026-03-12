package com.company.ems.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Tiện ích UI — helper cho JTable (căn cột, tự động độ rộng).
 */
public final class UI {
    private UI() {}


    /**
     * Căn lề cho cột dữ liệu trong bảng.
     */
    public static void alignColumn(JTable table, int columnIndex, int alignment) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(alignment);
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
    }

    /**
     * Tự động điều chỉnh độ rộng từng cột theo nội dung thực tế.
     * Gọi sau khi đã nạp dữ liệu vào bảng (loadData).
     *
     * @param minWidth độ rộng tối thiểu mỗi cột (px)
     * @param maxWidth độ rộng tối đa mỗi cột (px)
     */
    public static void autoResizeColumns(JTable table, int minWidth, int maxWidth) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            var column = table.getColumnModel().getColumn(col);
            // Bỏ qua cột ẩn
            if (column.getMaxWidth() == 0) continue;

            // Đo từ header
            TableCellRenderer hRenderer = table.getTableHeader().getDefaultRenderer();
            Component hComp = hRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, -1, col);
            int width = hComp.getPreferredSize().width + 16;

            // Đo từ dữ liệu
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer r = table.getCellRenderer(row, col);
                Component c = r.getTableCellRendererComponent(
                        table, table.getValueAt(row, col), false, false, row, col);
                width = Math.max(width, c.getPreferredSize().width + 16);
            }

            column.setPreferredWidth(Math.max(minWidth, Math.min(maxWidth, width)));
        }
    }

    /** Phiên bản mặc định: min=48, max=280. */
    public static void autoResizeColumns(JTable table) {
        autoResizeColumns(table, 48, 280);
    }
}
