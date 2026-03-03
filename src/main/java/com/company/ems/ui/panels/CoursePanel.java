package com.company.ems.ui.panels;

import com.company.ems.model.Course;
import com.company.ems.service.CourseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Panel quản lý Khóa học — CRUD đầy đủ cho entity Course.
 */
public class CoursePanel extends JPanel {

    private static final Color BG_PAGE       = new Color(248, 250, 252);
    private static final Color BG_CARD       = Color.WHITE;
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);
    private static final Color PRIMARY       = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color DANGER        = new Color(220, 38, 38);
    private static final Color TEXT_MAIN     = new Color(15, 23, 42);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color ROW_EVEN      = Color.WHITE;
    private static final Color ROW_ODD       = new Color(248, 250, 252);
    private static final Color ROW_SELECT    = new Color(219, 234, 254);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private static final String[] COLUMNS = {
            "ID", "STT", "Mã KH", "Tên khóa học", "Cấp độ", "Thời lượng", "Học phí", "Trạng thái"
    };

    private final CourseService courseService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final JComboBox<String> filterLevel;
    private final JComboBox<String> filterStatus;
    private TableRowSorter<DefaultTableModel> sorter;

    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public CoursePanel(CourseService courseService) {
        this.courseService = courseService;
        this.tableModel    = buildTableModel();
        this.table         = buildTable();
        this.statusLabel   = new JLabel();
        this.searchField   = new JTextField();
        this.filterLevel   = new JComboBox<>(new String[]{"Tất cả", "Beginner", "Intermediate", "Advanced"});
        this.filterStatus  = new JComboBox<>(new String[]{"Tất cả", "Active", "Inactive"});

        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadData();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        searchField.setPreferredSize(new Dimension(260, 38));
        searchField.setFont(FONT_MAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên khóa học...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        filterLevel.setFont(FONT_MAIN);
        filterStatus.setFont(FONT_MAIN);
        filterLevel.addActionListener(e -> applyFilters());
        filterStatus.addActionListener(e -> applyFilters());

        filters.add(searchField);
        filters.add(filterLevel);
        filters.add(filterStatus);

        JButton addBtn = createPrimaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openDialog(null));

        toolbar.add(filters,  BorderLayout.WEST);
        toolbar.add(addBtn,   BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton editBtn   = createSecondaryButton("✏️ Sửa");
        JButton deleteBtn = createDangerButton("🗑️ Xóa");
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 0 -> Long.class;     // hidden ID
                    case 1 -> Integer.class;  // STT
                    default -> String.class;
                };
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? ROW_SELECT : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
                c.setForeground(TEXT_MAIN);
                return c;
            }
        };

        t.setFont(FONT_MAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        var baseRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            Component comp = baseRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
            if (comp instanceof JLabel lbl) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return comp;
        });

        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);
        return t;
    }

    private void loadData() {
        try {
            List<Course> list = courseService.findAll();
            tableModel.setRowCount(0);

            int index = 1;
            for (Course c : list) {
                String code = c.getCourseId() != null
                        ? String.format("KH%04d", c.getCourseId())
                        : "";
                tableModel.addRow(new Object[]{
                        c.getCourseId(),   // hidden technical ID
                        index++,
                        code,
                        c.getCourseName(),
                        c.getLevel() != null ? c.getLevel() : "",
                        formatDuration(c.getDuration(), c.getDurationUnit()),
                        formatFee(c.getFee()),
                        c.getStatus()
                });
            }

            statusLabel.setText("Tổng: " + list.size() + " khóa học");
            applyFilters();
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private String formatDuration(Integer duration, String unit) {
        if (duration == null) return "";
        String u = unit != null ? unit : "";
        return duration + " " + ("Hour".equals(u) ? "giờ" : "tuần");
    }

    private String formatFee(BigDecimal fee) {
        if (fee == null) return "";
        return currencyFormat.format(fee);
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String level   = (String) filterLevel.getSelectedItem();
        String status  = (String) filterStatus.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String nameVal   = String.valueOf(entry.getValue(3));
                String levelVal  = String.valueOf(entry.getValue(4));
                String statusVal = String.valueOf(entry.getValue(7));

                boolean matchKeyword = keyword.isEmpty()
                        || nameVal.toLowerCase().contains(keyword.toLowerCase());
                boolean matchLevel = "Tất cả".equals(level)
                        || levelVal.equalsIgnoreCase(level);
                boolean matchStatus = "Tất cả".equals(status)
                        || statusVal.equalsIgnoreCase(status);

                return matchKeyword && matchLevel && matchStatus;
            }
        });

        statusLabel.setText("Hiển thị: " + table.getRowCount() + " khóa học");
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một khóa học để sửa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(courseService.findById(id));
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một khóa học để xóa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        Long   id   = (Long)   tableModel.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa khóa học \"" + name + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            courseService.delete(id);
            loadData();
            showSuccess("Đã xóa khóa học \"" + name + "\" thành công.");
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    private void openDialog(Course existing) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        CourseFormDialog dlg = new CourseFormDialog(owner, existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;

        try {
            if (existing != null) {
                courseService.update(dlg.getCourse());
                showSuccess("Cập nhật khóa học thành công.");
            } else {
                courseService.save(dlg.getCourse());
                showSuccess("Thêm khóa học mới thành công.");
            }
            loadData();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD); btn.setForeground(Color.WHITE); btn.setBackground(PRIMARY);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_HOVER); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN); btn.setForeground(TEXT_MAIN); btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(241, 245, 249)); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN); btn.setForeground(DANGER); btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202)),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(254, 242, 242)); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo",   JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi",        JOptionPane.ERROR_MESSAGE);
    }
}

