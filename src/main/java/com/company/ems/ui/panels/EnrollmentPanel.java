package com.company.ems.ui.panels;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.StudentService;
import com.company.ems.ui.UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class EnrollmentPanel extends JPanel {

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

    private static final Color C_GREEN  = new Color(22, 163, 74);
    private static final Color C_BLUE   = new Color(37, 99, 235);

    // Grouped: _sid hidden, STT, Mã HV, Họ tên, Số môn, Ngày GD gần nhất, Trạng thái
    private static final String[] COLUMNS = {
        "_sid", "STT", "Mã HV", "Họ và tên", "Số môn", "Ngày GD gần nhất", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── State ─────────────────────────────────────────
    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final ClassService classService;
    private final InvoiceService invoiceService;
    
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private Runnable onDataChanged;

    /** Đăng ký callback refresh toàn bộ khi dữ liệu thay đổi. */
    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public EnrollmentPanel(EnrollmentService enrollmentService, StudentService studentService, ClassService classService, InvoiceService invoiceService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.classService = classService;
        this.invoiceService = invoiceService;
        
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
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên HV, mã HV, lớp...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(searchField.getText().trim()); }
        });

        JButton addBtn = createPrimaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openAddDialog());

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

        JButton detailBtn = createSecondaryButton("Chi tiết");
        JButton deleteBtn = createDangerButton("Xóa tất cả");
        detailBtn.addActionListener(e -> openDetailForSelected());
        deleteBtn.addActionListener(e -> deleteAllForSelected());

        btnPanel.add(detailBtn);
        btnPanel.add(deleteBtn);
        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public java.lang.Class<?> getColumnClass(int c) {
                return c == 0 ? Long.class : (c == 1 || c == 4) ? Integer.class : String.class;
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
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

        // Ẩn cột _sid
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        // Status color renderer
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? ROW_SELECT : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground(s.contains("Đã thanh toán") && !s.contains("Đã đăng ký") ? C_GREEN
                            : s.contains("Đã đăng ký") ? C_BLUE : TEXT_MAIN);
                setFont(FONT_BOLD);
                return this;
            }
        });

        // Column widths (7 cols now)
        var cm = t.getColumnModel();
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(90);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setPreferredWidth(70);
        cm.getColumn(5).setPreferredWidth(150);
        cm.getColumn(6).setPreferredWidth(220);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);

        // Single-click → mở chi tiết học viên
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && t.getSelectedRow() >= 0)
                    openDetailForSelected();
            }
        });
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return t;
    }

    public void loadData() {
        try {
            tableModel.setRowCount(0);

            // Group by student using lambdas
            Map<Long, List<Enrollment>> grouped = enrollmentService.findAll().stream()
                    .filter((Enrollment e) -> e.getStudent() != null)
                    .collect(Collectors.groupingBy((Enrollment e) -> e.getStudent().getStudentId()));

            // Sort by most recent enrollment date descending
            List<Map.Entry<Long, List<Enrollment>>> entries = grouped.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, List<Enrollment>>, LocalDate>comparing(
                        entry -> entry.getValue().stream()
                            .map(Enrollment::getEnrollmentDate)
                            .filter(d -> d != null)
                            .max(Comparator.naturalOrder())
                            .orElse(LocalDate.MIN),
                        Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            int[] idx = {1};
            entries.forEach(entry -> {
                Long sid = entry.getKey();
                List<Enrollment> enrs = entry.getValue();
                Student student = enrs.get(0).getStudent();

                String code = String.format("HV%04d", sid);
                int count   = enrs.size();

                String latestDate = enrs.stream()
                        .map(Enrollment::getEnrollmentDate)
                        .filter(d -> d != null)
                        .max(Comparator.naturalOrder())
                        .map(d -> d.format(DATE_FMT))
                        .orElse("");

                String statusSummary = enrs.stream()
                        .collect(Collectors.groupingBy(
                            e -> e.getStatus() != null ? e.getStatus() : "—",
                            Collectors.counting()))
                        .entrySet().stream()
                        .map(se -> se.getValue() + " " + se.getKey())
                        .collect(Collectors.joining(", "));

                tableModel.addRow(new Object[]{sid, idx[0]++, code,
                        student.getFullName(), count, latestDate, statusSummary});
            });

            int totalEnrs = grouped.values().stream().mapToInt(List::size).sum();
            statusLabel.setText("Tổng: " + grouped.size() + " học viên / " + totalEnrs + " ghi danh");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));

        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private void filterTable(String keyword) {
        // Tìm kiếm theo Mã HV, Tên HV, hoặc Tên Lớp (Cột 2, 3, 4)
        sorter.setRowFilter(keyword.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + keyword, 2, 3, 4));
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " bản ghi");
    }

    // ── Lấy sid từ dòng được chọn ─────────────────────
    private Long getSelectedSid() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (Long) tableModel.getValueAt(modelRow, 0);
    }

    private void openDetailForSelected() {
        Long sid = getSelectedSid();
        if (sid == null) { showWarning("Vui lòng chọn một học viên."); return; }
        showStudentDetailDialog(sid);
    }

    private void deleteAllForSelected() {
        Long sid = getSelectedSid();
        if (sid == null) { showWarning("Vui lòng chọn một học viên."); return; }

        List<Enrollment> enrs = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid))
                .collect(Collectors.toList());
        if (enrs.isEmpty()) { showWarning("Kh\u00f4ng t\u00ecm th\u1ea5y ghi danh n\u00e0o."); return; }

        // Không cho xóa nếu có bất kỳ ghi danh nào đã thanh toán
        long paidCount = enrs.stream()
                .filter(e -> "\u0110\u00e3 thanh to\u00e1n".equals(e.getStatus())
                          || "Ho\u00e0n th\u00e0nh".equals(e.getStatus()))
                .count();
        if (paidCount > 0) {
            showWarning("H\u1ecdc vi\u00ean n\u00e0y c\u00f3 " + paidCount
                + " ghi danh \u0111\u00e3 thanh to\u00e1n/ho\u00e0n th\u00e0nh \u2014 kh\u00f4ng th\u1ec3 x\u00f3a.");
            return;
        }

        Student student = enrs.get(0).getStudent();
        int ok = JOptionPane.showConfirmDialog(this,
                "X\u00f3a t\u1ea5t c\u1ea3 " + enrs.size() + " ghi danh c\u1ee7a " + student.getFullName() + "?",
                "X\u00e1c nh\u1eadn x\u00f3a", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            enrs.forEach(e -> enrollmentService.delete(e.getEnrollmentId()));
            syncInvoiceForStudent(student);
            showSuccess("\u0110\u00e3 x\u00f3a " + enrs.size() + " ghi danh.");
            notifyDataChanged();
        } catch (Exception e) {
            showError("Kh\u00f4ng th\u1ec3 x\u00f3a: " + e.getMessage());
        }
    }

    private void openAddDialog() {
        try {
            List<Class> classes = classService.findAll();
            List<Enrollment> allEnrollments = enrollmentService.findAll();
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            EnrollmentFormDialog dlg = new EnrollmentFormDialog(owner, null, studentService, classes, allEnrollments);
            dlg.setVisible(true);
            if (!dlg.isSaved()) return;
            Enrollment saved = dlg.getEnrollment();
            enrollmentService.save(saved);
            try { syncInvoiceForStudent(saved.getStudent()); }
            catch (Exception ex) { System.err.println("Lỗi đồng bộ hóa đơn: " + ex.getMessage()); }
            showSuccess("Đã ghi danh và đồng bộ hóa đơn.");
            notifyDataChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void showStudentDetailDialog(Long sid) {
        List<Enrollment> enrs = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid))
                .sorted(Comparator.comparing(Enrollment::getEnrollmentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (enrs.isEmpty()) { showWarning("Không tìm thấy ghi danh."); return; }

        Student student = enrs.get(0).getStudent();
        String code = String.format("HV%04d", sid);

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Chi tiết: " + student.getFullName() + " (" + code + ")", true);
        dlg.setSize(820, 500);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel(student.getFullName() + "  |  " + code);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel(enrs.size() + " ghi danh");
        sub.setFont(FONT_SMALL); sub.setForeground(new Color(148, 163, 184));
        header.add(sub, BorderLayout.EAST);

        // ── Detail table ──────────────────────────────
        String[] cols = {"_eid", "STT", "Môn học", "Lớp", "Ngày GD", "Trạng thái", "Kết quả"};
        DefaultTableModel detailModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public java.lang.Class<?> getColumnClass(int c) { return c == 0 ? Long.class : String.class; }
        };
        int[] di = {1};
        enrs.forEach(e -> {
            String course  = (e.getClazz() != null && e.getClazz().getCourse() != null)
                    ? e.getClazz().getCourse().getCourseName() : "";
            String clazz   = e.getClazz() != null ? e.getClazz().getClassName() : "";
            String date    = e.getEnrollmentDate() != null ? e.getEnrollmentDate().format(DATE_FMT) : "";
            String status  = e.getStatus() != null ? e.getStatus() : "";
            String result  = e.getResult() != null ? e.getResult() : "";
            detailModel.addRow(new Object[]{e.getEnrollmentId(), di[0]++, course, clazz, date, status, result});
        });

        JTable detailTable = new JTable(detailModel);
        detailTable.setFont(FONT_MAIN);
        detailTable.setRowHeight(36);
        detailTable.setShowGrid(false);
        detailTable.setIntercellSpacing(new Dimension(0, 0));
        detailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // hide _eid
        detailTable.getColumnModel().getColumn(0).setMinWidth(0);
        detailTable.getColumnModel().getColumn(0).setMaxWidth(0);
        detailTable.getColumnModel().getColumn(0).setWidth(0);
        // status color renderer (col 5)
        detailTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? ROW_SELECT : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground("Đã thanh toán".equals(s) ? C_GREEN : "Đã đăng ký".equals(s) ? C_BLUE : TEXT_MAIN);
                setFont(FONT_BOLD);
                return this;
            }
        });
        // result color renderer (col 6)
        detailTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? ROW_SELECT : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground("Đạt".equals(s) ? C_GREEN : "Không đạt".equals(s) ? new Color(220, 38, 38) : TEXT_MUTED);
                setFont(FONT_BOLD);
                return this;
            }
        });
        var dc = detailTable.getColumnModel();
        dc.getColumn(1).setPreferredWidth(40);
        dc.getColumn(2).setPreferredWidth(180);
        dc.getColumn(3).setPreferredWidth(130);
        dc.getColumn(4).setPreferredWidth(90);
        dc.getColumn(5).setPreferredWidth(130);
        dc.getColumn(6).setPreferredWidth(90);
        JScrollPane detailScroll = new JScrollPane(detailTable);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // ── Button bar ────────────────────────────────
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton addBtn  = createPrimaryButton("+ Thêm môn");
        JButton editBtn = createSecondaryButton("Sửa");
        JButton delBtn  = createDangerButton("Xóa dòng");
        JButton closeBtn = createSecondaryButton("Đóng");

        addBtn.addActionListener(ev -> {
            dlg.dispose();
            try {
                List<Class> classes = classService.findAll();
                List<Enrollment> all = enrollmentService.findAll();
                EnrollmentFormDialog form = new EnrollmentFormDialog(owner, null, studentService, classes, all, student);
                form.setVisible(true);
                if (!form.isSaved()) { showStudentDetailDialog(sid); return; }
                Enrollment saved = form.getEnrollment();
                enrollmentService.save(saved);
                try { syncInvoiceForStudent(saved.getStudent()); }
                catch (Exception ex) { System.err.println("Sync lỗi: " + ex.getMessage()); }
                notifyDataChanged();
                showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        editBtn.addActionListener(ev -> {
            int vr = detailTable.getSelectedRow();
            if (vr < 0) { showWarning("Đối với dòng cần sửa, hãy chọn một dòng trước."); return; }
            int modelRow = detailTable.convertRowIndexToModel(vr);
            String rowStatus = (String) detailModel.getValueAt(modelRow, 5);
            if ("Đã thanh toán".equals(rowStatus)) {
                showWarning("Đăng ký này đã <b>thanh toán</b> — không thể sửa.");
                return;
            }
            Long eid = (Long) detailModel.getValueAt(modelRow, 0);
            dlg.dispose();
            try {
                List<Class> classes = classService.findAll();
                List<Enrollment> all = enrollmentService.findAll();
                Enrollment target = enrollmentService.findById(eid);
                EnrollmentFormDialog form = new EnrollmentFormDialog(owner, target, studentService, classes, all);
                form.setVisible(true);
                if (!form.isSaved()) { showStudentDetailDialog(sid); return; }
                enrollmentService.update(form.getEnrollment());
                notifyDataChanged();
                showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        delBtn.addActionListener(ev -> {
            int vr = detailTable.getSelectedRow();
            if (vr < 0) { showWarning("Đối với dòng cần xóa, hãy chọn một dòng trước."); return; }
            int modelRow2 = detailTable.convertRowIndexToModel(vr);
            String rowStatus2 = (String) detailModel.getValueAt(modelRow2, 5);
            if ("Đã thanh toán".equals(rowStatus2)) {
                showWarning("Đăng ký này đã <b>thanh toán</b> — không thể xóa.");
                return;
            }
            Long eid = (Long) detailModel.getValueAt(modelRow2, 0);
            int ok = JOptionPane.showConfirmDialog(dlg, "Xóa ghi danh này?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            try {
                Enrollment target = enrollmentService.findById(eid);
                Student st = target.getStudent();
                enrollmentService.delete(eid);
                syncInvoiceForStudent(st);
                notifyDataChanged();
                dlg.dispose();
                // Reopen if student still has enrollments
                boolean stillHas = enrollmentService.findAll().stream()
                        .anyMatch(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid));
                if (stillHas) showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        closeBtn.addActionListener(ev -> dlg.dispose());

        btnBar.add(addBtn); btnBar.add(editBtn); btnBar.add(delBtn); btnBar.add(closeBtn);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(detailScroll, BorderLayout.CENTER);
        dlg.add(btnBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
    
    // ══════════════════════════════════════════════════
    //  REFRESH HELPER
    // ══════════════════════════════════════════════════

    private void notifyDataChanged() {
        if (onDataChanged != null) {
            onDataChanged.run();
        } else {
            loadData();
        }
    }

    // ✅ THÊM METHOD TỔNG HỢP BILL (Logic giống StudentClassPanel)
    private void syncInvoiceForStudent(com.company.ems.model.Student student) {
        if (student == null) return;

        // 1. Tìm tất cả lớp đang ở trạng thái "Enrolled" của học viên này
        List<Enrollment> enrolledList = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null 
                          && e.getStudent().getStudentId().equals(student.getStudentId()) 
                          && "Đã đăng ký".equals(e.getStatus()))
                .toList();

        // ✅ DEBUG LOG
        System.out.println("🔍 [ADMIN SYNC] Student ID: " + student.getStudentId() 
                         + " | Số lớp Enrolled: " + enrolledList.size());

        // 2. Tính tổng tiền học phí của các lớp này
        java.math.BigDecimal totalFee = enrolledList.stream()
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        System.out.println("💰 [ADMIN SYNC] Tổng học phí: " + totalFee);

        // 3. Tìm hóa đơn "Chờ thanh toán" (Issued) hiện có của học viên
        com.company.ems.model.Invoice pendingInv = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null 
                          && i.getStudent().getStudentId().equals(student.getStudentId()) 
                          && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        System.out.println("📄 [ADMIN SYNC] Bill hiện tại: " + (pendingInv != null ? "CÓ (ID=" + pendingInv.getInvoiceId() + ")" : "CHƯA CÓ"));

        // Trường hợp không còn lớp nào (hủy hết)
        if (totalFee.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (pendingInv != null) {
                invoiceService.delete(pendingInv.getInvoiceId());
                System.out.println("🗑️ [ADMIN SYNC] Đã xóa Bill vì không còn lớp nào.");
            }
            return;
        }

        if (pendingInv != null) {
            // NẾU ĐÃ CÓ BILL NỢ: Cập nhật lại số tiền tổng mới
            pendingInv.setTotalAmount(totalFee);
            pendingInv.setNote("Tổng học phí cho " + enrolledList.size() + " lớp đang đăng ký.");
            invoiceService.update(pendingInv);
            System.out.println("✅ [ADMIN SYNC] Đã CẬP NHẬT Bill ID=" + pendingInv.getInvoiceId() + " với số tiền: " + totalFee);
        } else {
            // NẾU CHƯA CÓ BILL NỢ: Tạo mới 1 bill cho tất cả các lớp đã chọn
            com.company.ems.model.Invoice newInv = new com.company.ems.model.Invoice();
            newInv.setStudent(student);
            newInv.setTotalAmount(totalFee);
            newInv.setIssueDate(java.time.LocalDate.now());
            newInv.setStatus("Chờ thanh toán");
            newInv.setNote("Học phí tổng hợp cho " + enrolledList.size() + " lớp.");
            invoiceService.save(newInv);
            System.out.println("✨ [ADMIN SYNC] Đã TẠO MỚI Bill với số tiền: " + totalFee);
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