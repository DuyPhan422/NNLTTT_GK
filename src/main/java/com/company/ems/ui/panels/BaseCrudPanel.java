package com.company.ems.ui.panels;

import com.company.ems.ui.UI;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Lớp cha Abstract cho TẤT CẢ panel CRUD trong ứng dụng.
 *
 * <p>Nguyên lý áp dụng:
 * <ul>
 *   <li><b>SRP</b>: Lớp này chỉ lo layout + CRUD-flow chung; lớp con lo data-specific.</li>
 *   <li><b>OCP</b>: Đóng với sửa đổi, mở để mở rộng qua subclass và abstract methods.</li>
 *   <li><b>LSP</b>: Mọi subclass đều có thể thay thế BaseCrudPanel mà không phá vỡ contract.</li>
 *   <li><b>DRY</b>: Không có dòng code nào bị viết lại ở nhiều hơn 1 panel.</li>
 *   <li><b>Template Method Pattern</b>: Định nghĩa flow, lớp con fill "chỗ trống".</li>
 * </ul>
 *
 * <p>Layout cố định:
 * <pre>
 * ┌──────────────────────────────────────────────────┐
 * │  [Search bar] [filters...]     [+ Thêm mới]      │  ← toolbar (NORTH)
 * ├──────────────────────────────────────────────────┤
 * │              JTable (danh sách)                  │  ← content (CENTER)
 * ├──────────────────────────────────────────────────┤
 * │  Tổng: N bản ghi         [✏️ Sửa]  [🗑️ Xóa]     │  ← action bar (SOUTH)
 * └──────────────────────────────────────────────────┘
 * </pre>
 *
 * @param <T> kiểu entity (Student, Teacher, Staff, Room, Course, ...)
 */
public abstract class BaseCrudPanel<T> extends JPanel {

    // ── Shared UI state ───────────────────────────────────────────────────
    protected final DefaultTableModel               tableModel;
    protected final JTable                          table;
    protected final JLabel                          statusLabel;
    protected final JTextField                      searchField;
    protected       TableRowSorter<DefaultTableModel> sorter;
    protected       Runnable                        onDataChanged;

    /** Callback khi dữ liệu thay đổi (dùng để thông báo MainFrame cập nhật badge, v.v.) */
    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param columns tên các cột — index 0 luôn là ID kỹ thuật (sẽ bị ẩn)
     */
    protected BaseCrudPanel(String[] columns) {
        this.tableModel  = buildTableModel(columns);
        this.table       = buildTable();
        this.statusLabel = new JLabel();
        this.searchField = new JTextField();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        // loadData() được gọi sau khi subclass đã gán xong các field service.
        // Subclass phải tự gọi loadData() cuối constructor của mình.
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ABSTRACT — Lớp con BẮT BUỘC phải implement
    // ══════════════════════════════════════════════════════════════════════

    /** Tải dữ liệu từ service, map entity → row, cập nhật tableModel và statusLabel. */
    public abstract void loadData();

    /** Mở dialog thêm mới (existing = null) hoặc sửa. */
    protected abstract void openDialog(T existing);

    /**
     * Xóa entity theo id.
     * Ném RuntimeException nếu xóa thất bại (sẽ được bắt ở {@link #deleteSelected()})
     */
    protected abstract void deleteEntity(Long id);

    /** Trả về entity đang được chọn (dùng cho editSelected). */
    protected abstract T getSelectedEntity();

    /** Tên hiển thị của entity: "học viên", "giáo viên", "phòng học", ... */
    protected abstract String getEntityDisplayName();

    /**
     * Placeholder text cho ô tìm kiếm.
     * Ví dụ: "Tìm theo tên, số điện thoại..."
     */
    protected abstract String getSearchPlaceholder();

    /**
     * Các index cột dùng để lọc theo từ khóa tìm kiếm.
     * Ví dụ: {3, 4, 6} tương ứng với cột Họ tên, Phone, Email.
     */
    protected abstract int[] getSearchColumns();

    /**
     * Trả về các JComboBox filter bổ sung nằm cạnh search field.
     * Trả về null hoặc mảng rỗng nếu không có filter thêm.
     */
    @SuppressWarnings("rawtypes")
    protected JComboBox[] buildExtraFilters() { return null; }

    /**
     * Áp dụng filter tổng hợp (keyword + extra filters) lên {@link #sorter}.
     * Mặc định: lọc keyword theo {@link #getSearchColumns()}.
     * Override nếu cần kết hợp nhiều filter.
     */
    protected void applyFilters() {
        String kw = searchField.getText().trim();
        sorter.setRowFilter(kw.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + kw, getSearchColumns()));
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " bản ghi");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI — dùng chung, không cần override
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Search field
        searchField.setPreferredSize(new Dimension(280, 38));
        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", getSearchPlaceholder());
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        // Filters panel (search + extra)
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtersPanel.setOpaque(false);
        filtersPanel.add(searchField);
        @SuppressWarnings("rawtypes") JComboBox[] extras = buildExtraFilters();
        if (extras != null) {
            for (JComboBox<?> c : extras) filtersPanel.add(c);
        }

        // Add button
        JButton addBtn = ComponentFactory.primaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openDialog(null));

        toolbar.add(filtersPanel, BorderLayout.WEST);
        toolbar.add(addBtn,       BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTableCard() {
        return TableStyler.scrollPane(table);
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton editBtn   = ComponentFactory.secondaryButton("✏️ Sửa");
        JButton deleteBtn = ComponentFactory.dangerButton("🗑️ Xóa");
        editBtn.addActionListener(e   -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE — dùng chung
    // ══════════════════════════════════════════════════════════════════════

    private DefaultTableModel buildTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 0 -> Long.class;    // hidden ID
                    case 1 -> Integer.class; // STT
                    default -> String.class;
                };
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setFocusable(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.applyDefaults(t);
        TableStyler.fitTableColumns(t);

        // Header căn trái
        var baseRenderer = t.getTableHeader().getDefaultRenderer();
        t.getTableHeader().setDefaultRenderer((tbl, val, sel, focus, row, col) -> {
            Component c = baseRenderer.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
            if (c instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return c;
        });

        // Ẩn cột ID (index 0)
        TableStyler.hideColumn(t, 0);
        UI.alignColumn(t, 1, SwingConstants.LEFT);

        sorter = TableStyler.attachSorter(t, tableModel, 0, 1);

        // Double-click → sửa
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) editSelected();
            }
        });
        return t;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CRUD FLOW — Template Method Pattern
    // ══════════════════════════════════════════════════════════════════════

    /** Lấy ID (Long) của dòng đang chọn trong model. */
    protected Long getSelectedId() {
        int view = table.getSelectedRow();
        if (view < 0) return null;
        return (Long) tableModel.getValueAt(table.convertRowIndexToModel(view), 0);
    }

    /** Lấy tên hiển thị (cột 3) của dòng đang chọn — dùng trong confirm xóa. */
    protected String getSelectedName() {
        int view = table.getSelectedRow();
        if (view < 0) return "";
        return (String) tableModel.getValueAt(table.convertRowIndexToModel(view), 3);
    }

    protected void editSelected() {
        if (table.getSelectedRow() < 0) {
            showWarning("Vui lòng chọn một " + getEntityDisplayName() + " để sửa.");
            return;
        }
        openDialog(getSelectedEntity());
    }

    protected void deleteSelected() {
        if (table.getSelectedRow() < 0) {
            showWarning("Vui lòng chọn một " + getEntityDisplayName() + " để xóa.");
            return;
        }
        String name = getSelectedName();
        Long   id   = getSelectedId();

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa " + getEntityDisplayName() + " \"" + name + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            deleteEntity(id);
            showSuccess("Đã xóa " + getEntityDisplayName() + " \"" + name + "\" thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    protected void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run(); else loadData();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS — dùng chung
    // ══════════════════════════════════════════════════════════════════════

    protected Frame ownerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    protected void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    protected void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo",   JOptionPane.WARNING_MESSAGE);
    }
    protected void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi",        JOptionPane.ERROR_MESSAGE);
    }
}

