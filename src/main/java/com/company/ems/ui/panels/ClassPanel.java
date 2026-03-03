package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Room;
import com.company.ems.model.Teacher;
import com.company.ems.service.ClassService;
import com.company.ems.service.CourseService;
import com.company.ems.service.RoomService;
import com.company.ems.service.TeacherService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Panel quản lý Lớp học.
 */
public class ClassPanel extends JPanel {

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
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] COLUMNS = {
            "ID", "STT", "Mã lớp", "Tên lớp", "Khóa học", "Giáo viên", "Phòng học", "Bắt đầu", "Kết thúc", "Trạng thái"
    };

    private final ClassService classService;
    private final CourseService courseService;
    private final TeacherService teacherService;
    private final RoomService roomService;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final JComboBox<String> filterStatus;
    private TableRowSorter<DefaultTableModel> sorter;

    public ClassPanel(ClassService classService,
                      CourseService courseService,
                      TeacherService teacherService,
                      RoomService roomService) {
        this.classService = classService;
        this.courseService = courseService;
        this.teacherService = teacherService;
        this.roomService = roomService;

        this.tableModel   = buildTableModel();
        this.table        = buildTable();
        this.statusLabel  = new JLabel();
        this.searchField  = new JTextField();
        this.filterStatus = new JComboBox<>(new String[]{"Tất cả", "Planned", "Open", "Ongoing", "Completed", "Cancelled"});

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
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên lớp hoặc khóa học...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { applyFilters(); }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        filterStatus.setFont(FONT_MAIN);
        filterStatus.addActionListener(e -> applyFilters());

        filters.add(searchField);
        filters.add(filterStatus);

        JButton addBtn = createPrimaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openDialog(null));

        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(addBtn,  BorderLayout.EAST);
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
            @Override public java.lang.Class<?> getColumnClass(int c) {
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
            List<Class> list = classService.findAll();
            tableModel.setRowCount(0);

            int index = 1;
            for (Class c : list) {
                String code = c.getClassId() != null
                        ? String.format("L%04d", c.getClassId())
                        : "";
                tableModel.addRow(new Object[]{
                        c.getClassId(),   // hidden technical ID
                        index++,
                        code,
                        c.getClassName(),
                        c.getCourse()  != null ? c.getCourse().getCourseName()  : "",
                        c.getTeacher() != null ? c.getTeacher().getFullName()   : "",
                        c.getRoom()    != null ? c.getRoom().getRoomName()      : "",
                        c.getStartDate() != null ? c.getStartDate().format(DATE_FMT) : "",
                        c.getEndDate()   != null ? c.getEndDate().format(DATE_FMT)   : "",
                        c.getStatus()
                });
            }

            statusLabel.setText("Tổng: " + list.size() + " lớp học");
            applyFilters();
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = (String) filterStatus.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String className  = String.valueOf(entry.getValue(3));
                String courseName = String.valueOf(entry.getValue(4));
                String statusVal  = String.valueOf(entry.getValue(9));

                boolean matchKeyword = keyword.isEmpty()
                        || className.toLowerCase().contains(keyword)
                        || courseName.toLowerCase().contains(keyword);
                boolean matchStatus = "Tất cả".equals(status)
                        || Objects.equals(statusVal, status);
                return matchKeyword && matchStatus;
            }
        });

        statusLabel.setText("Hiển thị: " + table.getRowCount() + " lớp học");
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một lớp để sửa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        openDialog(classService.findById(id));
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("Vui lòng chọn một lớp để xóa.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        Long   id   = (Long)   tableModel.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa lớp \"" + name + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            classService.delete(id);
            loadData();
            showSuccess("Đã xóa lớp \"" + name + "\" thành công.");
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    private void openDialog(Class existing) {
        try {
            List<Course>  courses  = courseService.findAll();
            List<Teacher> teachers = teacherService.findAll();
            List<Room>    rooms    = roomService.findAll();

            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            ClassFormDialog dlg = new ClassFormDialog(owner, existing, courses, teachers, rooms);
            dlg.setVisible(true);
            if (!dlg.isSaved()) return;

            if (existing != null) {
                classService.update(dlg.getClazz());
                showSuccess("Cập nhật lớp học thành công.");
            } else {
                classService.save(dlg.getClazz());
                showSuccess("Thêm lớp học mới thành công.");
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

