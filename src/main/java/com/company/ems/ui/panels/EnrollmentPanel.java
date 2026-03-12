package com.company.ems.ui.panels;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.StudentService;
import com.company.ems.ui.UI;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    private static final String[] COLUMNS = {
        "_sid", "STT", "Mã HV", "Họ và tên", "Số môn", "Ngày GD gần nhất", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnrollmentService enrollmentService;
    private final StudentService    studentService;
    private final ClassService      classService;
    private final InvoiceService    invoiceService;

    private final DefaultTableModel              tableModel;
    private final JTable                         table;
    private final JLabel                         statusLabel;
    private final JTextField                     searchField;
    private       TableRowSorter<DefaultTableModel> sorter;
    private       Runnable                       onDataChanged;

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public EnrollmentPanel(EnrollmentService enrollmentService, StudentService studentService,
                           ClassService classService, InvoiceService invoiceService) {
        this.enrollmentService = enrollmentService;
        this.studentService    = studentService;
        this.classService      = classService;
        this.invoiceService    = invoiceService;

        this.tableModel  = buildTableModel();
        this.table       = buildTable();
        this.statusLabel = new JLabel();
        this.searchField = new JTextField();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadData();
    }

    // ── Build UI ──────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên HV, mã HV, lớp...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(searchField.getText().trim()); }
        });

        JButton addBtn = ComponentFactory.primaryButton("+ Thêm mới");
        addBtn.addActionListener(e -> openAddDialog());

        toolbar.add(searchField, BorderLayout.WEST);
        toolbar.add(addBtn,      BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        return scroll;
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
        JButton detailBtn = ComponentFactory.secondaryButton("Chi tiết");
        JButton deleteBtn = ComponentFactory.dangerButton("Xóa tất cả");
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
                c.setBackground(isRowSelected(row) ? Theme.ROW_SELECT
                        : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                c.setForeground(Theme.TEXT_MAIN);
                return c;
            }
        };
        t.setFont(Theme.FONT_PLAIN);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Theme.BG_CARD);

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        var baseRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, value, isSel, hasFocus, row, col) -> {
            Component comp = baseRenderer.getTableCellRendererComponent(tbl, value, isSel, hasFocus, row, col);
            if (comp instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return comp;
        });

        // Ẩn cột _sid
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        // Renderer cột Trạng thái
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? Theme.ROW_SELECT : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                setForeground(s.contains("Đã thanh toán") && !s.contains("Đã đăng ký") ? Theme.GREEN
                            : s.contains("Đã đăng ký") ? Theme.PRIMARY : Theme.TEXT_MAIN);
                setFont(Theme.FONT_BOLD);
                return this;
            }
        });

        var cm = t.getColumnModel();
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(90);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setPreferredWidth(70);
        cm.getColumn(5).setPreferredWidth(150);
        cm.getColumn(6).setPreferredWidth(220);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);

        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && t.getSelectedRow() >= 0) openDetailForSelected();
            }
        });
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return t;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            tableModel.setRowCount(0);

            Map<Long, List<Enrollment>> grouped = enrollmentService.findAll().stream()
                    .filter(e -> e.getStudent() != null)
                    .collect(Collectors.groupingBy(e -> e.getStudent().getStudentId()));

            List<Map.Entry<Long, List<Enrollment>>> entries = grouped.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, List<Enrollment>>, LocalDate>comparing(
                        entry -> entry.getValue().stream()
                            .map(Enrollment::getEnrollmentDate).filter(d -> d != null)
                            .max(Comparator.naturalOrder()).orElse(LocalDate.MIN),
                        Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            int[] idx = {1};
            entries.forEach(entry -> {
                Long sid             = entry.getKey();
                List<Enrollment> enrs = entry.getValue();
                Student student      = enrs.get(0).getStudent();
                String latestDate    = enrs.stream()
                        .map(Enrollment::getEnrollmentDate).filter(d -> d != null)
                        .max(Comparator.naturalOrder()).map(d -> d.format(DATE_FMT)).orElse("");
                String statusSummary = enrs.stream()
                        .collect(Collectors.groupingBy(e -> e.getStatus() != null ? e.getStatus() : "—",
                                Collectors.counting()))
                        .entrySet().stream()
                        .map(se -> se.getValue() + " " + se.getKey())
                        .collect(Collectors.joining(", "));
                tableModel.addRow(new Object[]{sid, idx[0]++,
                        String.format("HV%04d", sid), student.getFullName(),
                        enrs.size(), latestDate, statusSummary});
            });

            int totalEnrs = grouped.values().stream().mapToInt(List::size).sum();
            statusLabel.setText("Tổng: " + grouped.size() + " học viên / " + totalEnrs + " ghi danh");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));

        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    private void filterTable(String keyword) {
        sorter.setRowFilter(keyword.isEmpty() ? null : RowFilter.regexFilter("(?i)" + keyword, 2, 3, 4));
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " bản ghi");
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private Long getSelectedSid() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return (Long) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
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
        if (enrs.isEmpty()) { showWarning("Không tìm thấy ghi danh nào."); return; }

        long paidCount = enrs.stream()
                .filter(e -> "Đã thanh toán".equals(e.getStatus()) || "Hoàn thành".equals(e.getStatus()))
                .count();
        if (paidCount > 0) {
            showWarning("Học viên này có " + paidCount
                    + " ghi danh đã thanh toán/hoàn thành — không thể xóa.");
            return;
        }

        Student student = enrs.get(0).getStudent();
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa tất cả " + enrs.size() + " ghi danh của " + student.getFullName() + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            enrs.forEach(e -> enrollmentService.delete(e.getEnrollmentId()));
            syncInvoiceForStudent(student);
            showSuccess("Đã xóa " + enrs.size() + " ghi danh.");
            notifyDataChanged();
        } catch (Exception e) {
            showError("Không thể xóa: " + e.getMessage());
        }
    }

    private void openAddDialog() {
        try {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            EnrollmentFormDialog dlg = new EnrollmentFormDialog(owner, null, studentService,
                    classService.findAll(), enrollmentService.findAll());
            dlg.setVisible(true);
            if (!dlg.isSaved()) return;
            Enrollment saved = dlg.getEnrollment();
            enrollmentService.save(saved);
            try { syncInvoiceForStudent(saved.getStudent()); }
            catch (Exception ex) { System.err.println("Lỗi đồng bộ hóa đơn: " + ex.getMessage()); }
            showSuccess("Đã ghi danh và đồng bộ hóa đơn.");
            notifyDataChanged();
        } catch (Exception e) { showError("Lỗi: " + e.getMessage()); }
    }

    private void showStudentDetailDialog(Long sid) {
        List<Enrollment> enrs = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid))
                .sorted(Comparator.comparing(Enrollment::getEnrollmentDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (enrs.isEmpty()) { showWarning("Không tìm thấy ghi danh."); return; }

        Student student = enrs.get(0).getStudent();
        String code     = String.format("HV%04d", sid);
        Frame owner     = (Frame) SwingUtilities.getWindowAncestor(this);

        JDialog dlg = new JDialog(owner, "Chi tiết: " + student.getFullName() + " (" + code + ")", true);
        dlg.setSize(820, 500);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.TEXT_MAIN);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel(student.getFullName() + "  |  " + code);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel(enrs.size() + " ghi danh");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_SUB);
        header.add(sub, BorderLayout.EAST);

        // Detail table
        String[] cols = {"_eid", "STT", "Môn học", "Lớp", "Ngày GD", "Trạng thái", "Kết quả"};
        DefaultTableModel detailModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public java.lang.Class<?> getColumnClass(int c) {
                return c == 0 ? Long.class : String.class;
            }
        };
        int[] di = {1};
        enrs.forEach(e -> {
            String course = (e.getClazz() != null && e.getClazz().getCourse() != null)
                    ? e.getClazz().getCourse().getCourseName() : "";
            String clazz  = e.getClazz() != null ? e.getClazz().getClassName() : "";
            String date   = e.getEnrollmentDate() != null ? e.getEnrollmentDate().format(DATE_FMT) : "";
            detailModel.addRow(new Object[]{e.getEnrollmentId(), di[0]++, course, clazz, date,
                    e.getStatus() != null ? e.getStatus() : "",
                    e.getResult() != null ? e.getResult() : ""});
        });

        JTable detailTable = new JTable(detailModel);
        detailTable.setFont(Theme.FONT_PLAIN);
        detailTable.setRowHeight(36);
        detailTable.setShowGrid(false);
        detailTable.setIntercellSpacing(new Dimension(0, 0));
        detailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        detailTable.getColumnModel().getColumn(0).setMinWidth(0);
        detailTable.getColumnModel().getColumn(0).setMaxWidth(0);
        detailTable.getColumnModel().getColumn(0).setWidth(0);

        detailTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? Theme.ROW_SELECT : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                setForeground("Đã thanh toán".equals(s) ? Theme.GREEN
                            : "Đã đăng ký".equals(s) ? Theme.PRIMARY : Theme.TEXT_MAIN);
                setFont(Theme.FONT_BOLD);
                return this;
            }
        });
        detailTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val != null ? val.toString() : "";
                setBackground(sel ? Theme.ROW_SELECT : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                setForeground("Đạt".equals(s) ? Theme.GREEN
                            : "Không đạt".equals(s) ? Theme.DANGER : Theme.TEXT_MUTED);
                setFont(Theme.FONT_BOLD);
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
        detailScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // Button bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton addBtn   = ComponentFactory.primaryButton("+ Thêm môn");
        JButton editBtn  = ComponentFactory.secondaryButton("Sửa");
        JButton delBtn   = ComponentFactory.dangerButton("Xóa dòng");
        JButton closeBtn = ComponentFactory.secondaryButton("Đóng");

        addBtn.addActionListener(ev -> {
            dlg.dispose();
            try {
                EnrollmentFormDialog form = new EnrollmentFormDialog(owner, null, studentService,
                        classService.findAll(), enrollmentService.findAll(), student);
                form.setVisible(true);
                if (!form.isSaved()) { showStudentDetailDialog(sid); return; }
                enrollmentService.save(form.getEnrollment());
                try { syncInvoiceForStudent(form.getEnrollment().getStudent()); }
                catch (Exception ex) { System.err.println("Sync lỗi: " + ex.getMessage()); }
                notifyDataChanged();
                showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        editBtn.addActionListener(ev -> {
            int vr = detailTable.getSelectedRow();
            if (vr < 0) { showWarning("Hãy chọn một dòng trước."); return; }
            int mr = detailTable.convertRowIndexToModel(vr);
            if ("Đã thanh toán".equals(detailModel.getValueAt(mr, 5))) {
                showWarning("Đăng ký này đã thanh toán — không thể sửa."); return;
            }
            Long eid = (Long) detailModel.getValueAt(mr, 0);
            dlg.dispose();
            try {
                Enrollment target = enrollmentService.findById(eid);
                EnrollmentFormDialog form = new EnrollmentFormDialog(owner, target, studentService,
                        classService.findAll(), enrollmentService.findAll());
                form.setVisible(true);
                if (!form.isSaved()) { showStudentDetailDialog(sid); return; }
                enrollmentService.update(form.getEnrollment());
                notifyDataChanged();
                showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        delBtn.addActionListener(ev -> {
            int vr = detailTable.getSelectedRow();
            if (vr < 0) { showWarning("Hãy chọn một dòng trước."); return; }
            int mr = detailTable.convertRowIndexToModel(vr);
            if ("Đã thanh toán".equals(detailModel.getValueAt(mr, 5))) {
                showWarning("Đăng ký này đã thanh toán — không thể xóa."); return;
            }
            Long eid = (Long) detailModel.getValueAt(mr, 0);
            if (JOptionPane.showConfirmDialog(dlg, "Xóa ghi danh này?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            try {
                Enrollment target = enrollmentService.findById(eid);
                Student st = target.getStudent();
                enrollmentService.delete(eid);
                syncInvoiceForStudent(st);
                notifyDataChanged();
                dlg.dispose();
                boolean stillHas = enrollmentService.findAll().stream()
                        .anyMatch(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid));
                if (stillHas) showStudentDetailDialog(sid);
            } catch (Exception ex) { showError("Lỗi: " + ex.getMessage()); }
        });

        closeBtn.addActionListener(ev -> dlg.dispose());
        btnBar.add(addBtn); btnBar.add(editBtn); btnBar.add(delBtn); btnBar.add(closeBtn);

        dlg.add(header,       BorderLayout.NORTH);
        dlg.add(detailScroll, BorderLayout.CENTER);
        dlg.add(btnBar,       BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void notifyDataChanged() {
        if (onDataChanged != null) onDataChanged.run();
        else loadData();
    }

    private void syncInvoiceForStudent(Student student) {
        if (student == null) return;
        List<Enrollment> enrolledList = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                        && e.getStudent().getStudentId().equals(student.getStudentId())
                        && "Đã đăng ký".equals(e.getStatus()))
                .toList();

        java.math.BigDecimal totalFee = enrolledList.stream()
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        com.company.ems.model.Invoice pendingInv = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null
                        && i.getStudent().getStudentId().equals(student.getStudentId())
                        && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        if (totalFee.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (pendingInv != null) invoiceService.delete(pendingInv.getInvoiceId());
            return;
        }
        if (pendingInv != null) {
            pendingInv.setTotalAmount(totalFee);
            pendingInv.setNote("Tổng học phí cho " + enrolledList.size() + " lớp đang đăng ký.");
            invoiceService.update(pendingInv);
        } else {
            com.company.ems.model.Invoice newInv = new com.company.ems.model.Invoice();
            newInv.setStudent(student);
            newInv.setTotalAmount(totalFee);
            newInv.setIssueDate(java.time.LocalDate.now());
            newInv.setStatus("Chờ thanh toán");
            newInv.setNote("Học phí tổng hợp cho " + enrolledList.size() + " lớp.");
            invoiceService.save(newInv);
        }
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

