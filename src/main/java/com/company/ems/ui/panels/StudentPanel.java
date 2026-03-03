package com.company.ems.ui.panels;

import com.company.ems.model.Student;
import com.company.ems.service.StudentService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel quản lý Học viên — TEMPLATE mẫu cho tất cả panel CRUD khác.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────┐
 * │  [Search bar]                  [+ Thêm mới]     │  ← toolbar
 * ├─────────────────────────────────────────────────┤
 * │              JTable (danh sách)                 │  ← content
 * ├─────────────────────────────────────────────────┤
 * │  Tổng: N bản ghi         [Sửa]  [Xóa]          │  ← action bar
 * └─────────────────────────────────────────────────┘
 */
public class StudentPanel extends JPanel {

    // ── Design tokens ────────────────────────────────
    private static final Color BG_PAGE      = new Color(248, 250, 252);
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER= new Color(29, 78, 216);
    private static final Color DANGER       = new Color(220, 38, 38);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color ROW_EVEN     = Color.WHITE;
    private static final Color ROW_ODD      = new Color(248, 250, 252);
    private static final Color ROW_SELECT   = new Color(219, 234, 254);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã HV", "Họ và tên", "Ngày sinh", "Giới tính", "Điện thoại", "Email", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── State ─────────────────────────────────────────
    private final StudentService studentService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    public StudentPanel(StudentService studentService) {
        this.studentService = studentService;
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

    // ══════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.setFont(FONT_MAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên, số điện thoại...");
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

    // ══════════════════════════════════════════════════
    //  TABLE
    // ══════════════════════════════════════════════════

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) {
                    case 0 -> Long.class;   // hidden ID
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

        // Ẩn cột ID kỹ thuật — vẫn giữ trong model để thao tác
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);
        return t;
    }

    // ══════════════════════════════════════════════════
    //  DATA OPERATIONS
    // ══════════════════════════════════════════════════

    private void loadData() {
        try {
            List<Student> list = studentService.findAll();
            tableModel.setRowCount(0);
            int index = 1;
            for (Student s : list) {
                String code = s.getStudentId() != null
                        ? String.format("HV%04d", s.getStudentId())
                        : "";
                tableModel.addRow(new Object[]{
                    s.getStudentId(),   // hidden technical ID
                    index++,
                    code,
                    s.getFullName(),
                    s.getDateOfBirth() != null ? s.getDateOfBirth().format(DATE_FMT) : "",
                    s.getGender()  != null ? s.getGender()  : "",
                    s.getPhone()   != null ? s.getPhone()   : "",
                    s.getEmail()   != null ? s.getEmail()   : "",
                    s.getStatus()
                });
            }
            statusLabel.setText("Tổng: " + list.size() + " học viên");
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private void filterTable(String keyword) {
        sorter.setRowFilter(keyword.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + keyword, 3, 6, 7));
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " bản ghi");
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { showWarning("Vui lòng chọn một học viên để sửa."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(studentService.findById(id));
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { showWarning("Vui lòng chọn một học viên để xóa."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        Long   id   = (Long)   tableModel.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa học viên \"" + name + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            studentService.delete(id);
            loadData();
            showSuccess("Đã xóa học viên \"" + name + "\" thành công.");
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════
    //  DIALOG — delegate hoàn toàn cho StudentFormDialog
    // ══════════════════════════════════════════════════

    private void openDialog(Student existing) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dlg = new StudentFormDialog(owner, existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;

        try {
            if (existing != null) {
                studentService.update(dlg.getStudent());
                showSuccess("Cập nhật học viên thành công.");
            } else {
                studentService.save(dlg.getStudent());
                showSuccess("Thêm học viên mới thành công.");
            }
            loadData();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════
    //  BUTTON FACTORIES
    // ══════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ══════════════════════════════════════════════════

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

