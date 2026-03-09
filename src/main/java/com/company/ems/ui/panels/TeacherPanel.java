package com.company.ems.ui.panels;

import com.company.ems.model.Teacher;
import com.company.ems.service.TeacherService;
import com.company.ems.ui.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Panel quản lý Giáo viên — áp dụng cùng kiến trúc và design với StudentPanel.
 * Tuân thủ SOLID bằng cách:
 * - Tách form nhập liệu ra TeacherFormDialog (SRP).
 * - Phụ thuộc vào abstraction TeacherService, không thao tác trực tiếp EntityManager (DIP).
 */
public class TeacherPanel extends JPanel {

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
            "ID", "STT", "Mã GV", "Họ và tên", "Điện thoại", "Email", "Chuyên môn", "Trạng thái"
    };

    private final TeacherService teacherService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private Runnable onDataChanged;

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public TeacherPanel(TeacherService teacherService) {
        this.teacherService = teacherService;
        this.tableModel     = buildTableModel();
        this.table          = buildTable();
        this.statusLabel    = new JLabel();
        this.searchField    = new JTextField();

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

        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.setFont(FONT_MAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên, chuyên môn, số điện thoại...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(searchField.getText().trim()); }
        });

        JButton addBtn = createPrimaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openDialog(null));

        toolbar.add(searchField, BorderLayout.WEST);
        toolbar.add(addBtn,      BorderLayout.EAST);
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
                    case 0 -> Long.class;    // hidden ID
                    case 1 -> Integer.class; // STT
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
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

        // Ẩn cột ID kỹ thuật
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);
        UI.alignColumn(t, 1, SwingConstants.LEFT);

        // ── Độ rộng cột ────────────────────────────────
        var cm = t.getColumnModel();
        cm.getColumn(1).setMinWidth(40);  cm.getColumn(1).setMaxWidth(55);   cm.getColumn(1).setPreferredWidth(50);  // STT
        cm.getColumn(2).setMinWidth(70);  cm.getColumn(2).setMaxWidth(100);  cm.getColumn(2).setPreferredWidth(88);  // Mã GV
        cm.getColumn(3).setPreferredWidth(180);                                                                        // Họ và tên
        cm.getColumn(4).setMinWidth(90);  cm.getColumn(4).setMaxWidth(140);  cm.getColumn(4).setPreferredWidth(115); // Điện thoại
        cm.getColumn(5).setPreferredWidth(170);                                                                        // Email
        cm.getColumn(6).setPreferredWidth(150);                                                                        // Chuyên môn
        cm.getColumn(7).setMinWidth(90);  cm.getColumn(7).setMaxWidth(130);  cm.getColumn(7).setPreferredWidth(110); // Trạng thái

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);

        // Double-click → mở dialog sửa
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) editSelected();
            }
        });
        return t;
    }

    public void loadData() {
        try {
            List<Teacher> list = teacherService.findAll();
            tableModel.setRowCount(0);
            int[] idx = {1};
            list.forEach((Teacher t) -> {
                String code = t.getTeacherId() != null
                        ? String.format("GV%04d", t.getTeacherId())
                        : "";
                tableModel.addRow(new Object[]{
                        t.getTeacherId(),
                        idx[0]++,
                        code,
                        t.getFullName(),
                        t.getPhone()     != null ? t.getPhone()     : "",
                        t.getEmail()     != null ? t.getEmail()     : "",
                        t.getSpecialty() != null ? t.getSpecialty() : "",
                        t.getStatus()
                });
            });
            statusLabel.setText("Tổng: " + list.size() + " giáo viên");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private void filterTable(String keyword) {
        sorter.setRowFilter(keyword.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + keyword, 3, 4, 5, 6));
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " bản ghi");
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một giáo viên để sửa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(teacherService.findById(id));
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một giáo viên để xóa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        Long   id   = (Long)   tableModel.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa giáo viên \"" + name + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            teacherService.delete(id);
            showSuccess("Đã xóa giáo viên \"" + name + "\" thành công.");
            notifyDataChanged();
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    private void openDialog(Teacher existing) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        TeacherFormDialog dlg = new TeacherFormDialog(owner, existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;

        try {
            if (existing != null) {
                teacherService.update(dlg.getTeacher());
                showSuccess("Cập nhật giáo viên thành công.");
            } else {
                teacherService.save(dlg.getTeacher());
                showSuccess("Thêm giáo viên mới thành công.");
            }
            notifyDataChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) onDataChanged.run(); else loadData();
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

