package com.company.ems.ui.panels.admin_staff;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.model.Student;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;
import com.company.ems.service.StudentService;
import com.company.ems.stream.EnrollmentStreamQueries;
import com.company.ems.stream.InvoiceStreamQueries;
import com.company.ems.ui.UI;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TuitionPanel extends JPanel {

    private static final String[] COLUMNS = {
        "_sid", "STT", "Mã HV", "Họ và tên", "HĐ chờ TT", "Tổng nợ", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnrollmentService enrollmentService;
    private final InvoiceService    invoiceService;
    private final PaymentService    paymentService;
    private final StudentService    studentService;
    private final boolean           isAdmin;
    private final Long              loggedInStudentId;

    private final DefaultTableModel              tableModel;
    private final JTable                         table;
    private final JLabel                         statusLabel;
    private final JTextField                     searchField;
    private       TableRowSorter<DefaultTableModel> sorter;
    private       Runnable                       onDataChanged;

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public TuitionPanel(EnrollmentService enrollmentService, InvoiceService invoiceService,
                        PaymentService paymentService, StudentService studentService,
                        boolean isAdmin, Long loggedInStudentId) {
        this.enrollmentService = enrollmentService;
        this.invoiceService    = invoiceService;
        this.paymentService    = paymentService;
        this.studentService    = studentService;
        this.isAdmin           = isAdmin;
        this.loggedInStudentId = loggedInStudentId;

        this.tableModel  = buildTableModel();
        this.table       = buildTable();
        this.statusLabel = new JLabel();
        this.searchField = new JTextField();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadData();
    }

    // ── Build UI ──────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel(isAdmin
                ? "Quản lý Học phí & Hóa đơn" : "Học phí & Biên lai của tôi");
        title.setFont(Theme.FONT_HEADER);
        bar.add(title, BorderLayout.WEST);

        searchField.setPreferredSize(new Dimension(320, 38));
        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText",
                "Nhập mã HV để tra nợ (VD: HV0004)...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(searchField.getText().trim()); }
        });
        bar.add(searchField, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        return scroll;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
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
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
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
        TableStyler.fitTableColumns(t);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Theme.BG_CARD);
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JTableHeader header = t.getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        var baseR = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, v, s, f, row, col) -> {
            Component c = baseR.getTableCellRendererComponent(tbl, v, s, f, row, col);
            if (c instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return c;
        });

        // Ẩn cột _sid
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        // Renderer cột Tổng nợ
        t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? Theme.ROW_SELECT : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                String s = val != null ? val.toString() : "";
                setForeground(s.equals("0 VND") || s.isEmpty() ? Theme.GREEN : Theme.AMBER);
                setFont(Theme.FONT_BOLD);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        });

        // Renderer cột Trạng thái
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? Theme.ROW_SELECT : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                String s = val != null ? val.toString() : "";
                setForeground("Sạch nợ".equals(s) ? Theme.GREEN
                            : "Còn nợ".equals(s) ? Theme.AMBER : Theme.TEXT_MUTED);
                setFont(Theme.FONT_BOLD);
                return this;
            }
        });

        var cm = t.getColumnModel();
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(90);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setPreferredWidth(90);
        cm.getColumn(5).setPreferredWidth(150);
        cm.getColumn(6).setPreferredWidth(120);

        sorter = new TableRowSorter<>(tableModel);
        t.setRowSorter(sorter);

        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && t.getSelectedRow() >= 0) openStudentInvoiceDetail();
            }
        });
        return t;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            tableModel.setRowCount(0);

            // Lấy hoá đơn theo vai trò (stream: filter căn bản trên tập đã lấy)
            List<Invoice> allInvoices = invoiceService.findAll().stream()
                    .filter(i -> i.getStudent() != null)
                    .collect(Collectors.toList());

            if (!isAdmin) {
                allInvoices = allInvoices.stream()
                        .filter(i -> i.getStudent().getStudentId().equals(loggedInStudentId))
                        .collect(Collectors.toList());
            }

            // Gom nhóm hoá đơn theo student (stream: groupingBy)
            Map<Long, List<Invoice>> grouped = InvoiceStreamQueries.groupByStudentId(allInvoices);

            // Đồng bộ học phí thực tế từ enrollment
            String daoDangKy = com.company.ems.model.enums.EnrollmentStatus.DA_DANG_KY.getValue();
            String choThanhToan = com.company.ems.model.enums.InvoiceStatus.CHO_THANH_TOAN.getValue();

            // Lấy thẳng từ service (không findAll().stream().filter())
            List<Enrollment> pendingEnrollments = enrollmentService.findByStudentIdAndStatus(
                    isAdmin ? null : loggedInStudentId, daoDangKy);
            // Nếu là admin, lấy tất cả pending enrollments bằng findAll() + filter theo trạng thái
            if (isAdmin) {
                pendingEnrollments = enrollmentService.findAll().stream()
                        .filter(e -> e.getStudent() != null && daoDangKy.equals(e.getStatus()))
                        .collect(Collectors.toList());
            }

            // Gom nhóm enrollment chờ theo student (stream: groupingBy)
            Map<Long, List<Enrollment>> enrollByStudent = pendingEnrollments.stream()
                    .filter(e -> e.getStudent() != null)
                    .collect(Collectors.groupingBy(e -> e.getStudent().getStudentId()));

            enrollByStudent.forEach((sid, enrs) -> {
                Student student = enrs.get(0).getStudent();
                // Tính tổng học phí bằng EnrollmentStreamQueries (stream: map + reduce)
                BigDecimal correctFee = EnrollmentStreamQueries.calculateTotalFee(enrs);
                List<Invoice> studentInvoices = grouped.get(sid);
                // Tìm hóa đơn chờ thanh toán bằng InvoiceStreamQueries (stream: filter + findFirst)
                Invoice pendingInv = studentInvoices == null ? null :
                        InvoiceStreamQueries.findFirstByStatus(studentInvoices, choThanhToan).orElse(null);
                if (pendingInv == null) {
                    if (correctFee.compareTo(BigDecimal.ZERO) <= 0) return;
                    try {
                        Invoice newInv = new Invoice();
                        newInv.setStudent(student);
                        newInv.setTotalAmount(correctFee);
                        newInv.setIssueDate(LocalDate.now());
                        newInv.setStatus(choThanhToan);
                        newInv.setNote("Học phí tổng hợp cho " + enrs.size() + " lớp.");
                        invoiceService.save(newInv);
                        grouped.computeIfAbsent(sid, k -> new ArrayList<>()).add(newInv);
                    } catch (Exception ignored) {}
                } else if (pendingInv.getTotalAmount().compareTo(correctFee) != 0) {
                    try {
                        pendingInv.setTotalAmount(correctFee);
                        pendingInv.setNote("Học phí tổng hợp cho " + enrs.size() + " lớp (đã cập nhật).");
                        invoiceService.update(pendingInv);
                    } catch (Exception ignored) {}
                }
            });
            grouped.forEach((sid, invList) -> {
                if (enrollByStudent.containsKey(sid)) return;
                // Xóa hóa đơn chờ nếu không còn enrollment nào
                InvoiceStreamQueries.filterByStatus(invList, choThanhToan)
                        .forEach(i -> { try { invoiceService.delete(i.getInvoiceId()); } catch (Exception ignored) {} });
            });

            // Sắp xếp giảm dần theo tổng nợ (stream: sorted + comparing)
            List<Map.Entry<Long, List<Invoice>>> entries = grouped.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, List<Invoice>>, BigDecimal>comparing(
                        entry -> InvoiceStreamQueries.sumPendingDebt(entry.getValue(), choThanhToan),
                        Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            int[] idx = {1};
            entries.forEach(entry -> {
                Long sid             = entry.getKey();
                List<Invoice> invs   = entry.getValue();
                Student student      = invs.get(0).getStudent();
                // Đếm và tính tổng nợ bằng InvoiceStreamQueries (stream: count + sum)
                long pendingCount    = InvoiceStreamQueries.countByStatus(invs, choThanhToan);
                BigDecimal totalDebt = InvoiceStreamQueries.sumPendingDebt(invs, choThanhToan);
                String debtStr = totalDebt.compareTo(BigDecimal.ZERO) == 0
                        ? "0 VND" : String.format("%,.0f VND", totalDebt);
                String status  = totalDebt.compareTo(BigDecimal.ZERO) == 0 ? "Sạch nợ" : "Còn nợ";
                tableModel.addRow(new Object[]{sid, idx[0]++,
                        String.format("HV%04d", sid), student.getFullName(),
                        (int) pendingCount, debtStr, status});
            });

            // Tính tổng nợ toàn hệ thống (stream: flatMap + sumPendingDebt)
            BigDecimal systemDebt = entries.stream().flatMap(e -> e.getValue().stream())
                    .filter(i -> choThanhToan.equals(i.getStatus()))
                    .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            statusLabel.setText(String.format(
                    "Tổng: %d học viên có hóa đơn  |  Tổng nợ toàn hệ thống: %,.0f VND",
                    grouped.size(), systemDebt));

            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterTable(String keyword) {
        if (keyword.isEmpty()) { sorter.setRowFilter(null); return; }
        String num = keyword.replaceAll("(?i)^hv0*", "");
        sorter.setRowFilter(RowFilter.regexFilter("(?i)(" + keyword + "|HV0*" + num + ")", 2, 3));
    }

    // ── Detail Dialog ─────────────────────────────────────────────────────

    private void openStudentInvoiceDetail() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        Long sid = (Long) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        showStudentInvoiceDialog(sid);
    }

    private void showStudentInvoiceDialog(Long sid) {
        String daThanhToan   = com.company.ems.model.enums.InvoiceStatus.DA_THANH_TOAN.getValue();
        String choThanhToan  = com.company.ems.model.enums.InvoiceStatus.CHO_THANH_TOAN.getValue();
        String daDangKy      = com.company.ems.model.enums.EnrollmentStatus.DA_DANG_KY.getValue();

        // Dùng service để lấy hoá đơn đúng student (không findAll().stream().filter())
        List<Invoice> allInvs = InvoiceStreamQueries.sortByIssueDateDesc(
                invoiceService.findAll().stream()
                        .filter(i -> i.getStudent() != null && i.getStudent().getStudentId().equals(sid))
                        .collect(Collectors.toList()));

        // Dùng service để lấy enrollment chờ theo student (không findAll().stream().filter())
        List<Enrollment> pendingEnrs = enrollmentService.findByStudentIdAndStatus(sid, daDangKy);

        // Lọc toàn bộ hoá đơn theo trạng thái bằng InvoiceStreamQueries (stream: filter)
        List<Invoice> paidInvs = InvoiceStreamQueries.filterByStatus(allInvs, daThanhToan);
        Invoice pendingInv = InvoiceStreamQueries.findFirstByStatus(allInvs, choThanhToan).orElse(null);

        if (allInvs.isEmpty() && pendingEnrs.isEmpty()) return;

        Student student = !allInvs.isEmpty() ? allInvs.get(0).getStudent() : pendingEnrs.get(0).getStudent();
        String code     = String.format("HV%04d", sid);
        Frame owner     = (Frame) SwingUtilities.getWindowAncestor(this);

        JDialog dlg = new JDialog(owner, "Hóa đơn: " + student.getFullName() + " (" + code + ")", true);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.TEXT_MAIN);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel(student.getFullName() + "  |  " + code);
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        // Tính tổng nợ bằng EnrollmentStreamQueries (stream: map + reduce)
        BigDecimal totalDebt = EnrollmentStreamQueries.calculateTotalFee(pendingEnrs);
        JLabel debtLbl = new JLabel("Còn nợ: " + String.format("%,.0f VND", totalDebt));
        debtLbl.setFont(Theme.FONT_BOLD);
        debtLbl.setForeground(totalDebt.compareTo(BigDecimal.ZERO) == 0 ? Theme.GREEN : Theme.AMBER);
        header.add(debtLbl, BorderLayout.EAST);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG_CARD);
        body.setBorder(new EmptyBorder(14, 16, 6, 16));

        // Bảng các khoản CHỜ THANH TOÁN
        if (!pendingEnrs.isEmpty()) {
            JLabel lbl1 = new JLabel("  KHOẢN CHỜ THANH TOÁN");
            lbl1.setFont(Theme.FONT_SMALL);
            lbl1.setForeground(Theme.AMBER);
            lbl1.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lbl1);
            body.add(Box.createVerticalStrut(6));

            String[] pendCols = {"STT", "Khóa học", "Lớp", "Học phí (VND)", "Ngày ĐK"};
            DefaultTableModel pendModel = new DefaultTableModel(pendCols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            int[] pi = {1};
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            pendingEnrs.forEach(e -> pendModel.addRow(new Object[]{
                    pi[0]++,
                    e.getClazz().getCourse().getCourseName(),
                    e.getClazz().getClassName(),
                    String.format("%,.0f", e.getClazz().getCourse().getFee()),
                    e.getEnrollmentDate() != null ? e.getEnrollmentDate().format(fmt) : ""
            }));
            pendModel.addRow(new Object[]{"", "TỔNG CỘNG", "", String.format("%,.0f", totalDebt), ""});

            JTable pendTable = new JTable(pendModel);
            pendTable.setFont(Theme.FONT_PLAIN);
            pendTable.setRowHeight(34);
            pendTable.setShowGrid(false);
            pendTable.setIntercellSpacing(new Dimension(0, 0));
            pendTable.setEnabled(false);
            pendTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    boolean isTotalRow = row == t.getRowCount() - 1;
                    setBackground(isTotalRow ? Theme.AMBER_TINT
                            : row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                    setForeground(isTotalRow ? Theme.AMBER : (col == 3 ? Theme.AMBER : Theme.TEXT_MAIN));
                    setFont(isTotalRow ? Theme.FONT_BOLD : (col == 3 ? Theme.FONT_BOLD : Theme.FONT_PLAIN));
                    setHorizontalAlignment(col == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                    return this;
                }
            });
            pendTable.getColumnModel().getColumn(0).setPreferredWidth(40);
            pendTable.getColumnModel().getColumn(1).setPreferredWidth(220);
            pendTable.getColumnModel().getColumn(2).setPreferredWidth(160);
            pendTable.getColumnModel().getColumn(3).setPreferredWidth(130);
            pendTable.getColumnModel().getColumn(4).setPreferredWidth(100);

            JScrollPane ps = new JScrollPane(pendTable);
            ps.setAlignmentX(Component.LEFT_ALIGNMENT);
            ps.setPreferredSize(new Dimension(0, Math.min(pendingEnrs.size() * 34 + 70, 220)));
            ps.setBorder(BorderFactory.createLineBorder(Theme.AMBER_BORDER));
            body.add(ps);
            body.add(Box.createVerticalStrut(16));
        }

        // Bảng lịch sử đã thanh toán
        if (!paidInvs.isEmpty()) {
            JLabel lbl2 = new JLabel("  LỊCH SỬ ĐÃ THANH TOÁN");
            lbl2.setFont(Theme.FONT_SMALL);
            lbl2.setForeground(Theme.GREEN);
            lbl2.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lbl2);
            body.add(Box.createVerticalStrut(6));

            String[] paidCols = {"_iid", "STT", "Mã HĐ", "Số tiền (VND)", "Ngày lập", "Các môn học"};
            DefaultTableModel paidModel = new DefaultTableModel(paidCols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public java.lang.Class<?> getColumnClass(int c) {
                    return c == 0 ? Long.class : String.class;
                }
            };
            int[] hi = {1};
            paidInvs.forEach(inv -> {
                String note = inv.getNote() != null ? inv.getNote() : "";
                if (note.contains("|")) note = note.substring(note.indexOf('|') + 1).trim();
                note = note.replace(";", " · ");
                paidModel.addRow(new Object[]{
                        inv.getInvoiceId(), hi[0]++,
                        "INV" + String.format("%04d", inv.getInvoiceId()),
                        String.format("%,.0f", inv.getTotalAmount()),
                        inv.getIssueDate() != null ? inv.getIssueDate().format(DATE_FMT) : "",
                        note.isEmpty() ? "—" : note
                });
            });

            JTable paidTable = new JTable(paidModel);
            paidTable.setFont(Theme.FONT_PLAIN);
            paidTable.setRowHeight(34);
            paidTable.setShowGrid(false);
            paidTable.setIntercellSpacing(new Dimension(0, 0));
            paidTable.setEnabled(false);
            paidTable.getColumnModel().getColumn(0).setMinWidth(0);
            paidTable.getColumnModel().getColumn(0).setMaxWidth(0);
            paidTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    setBackground(row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                    setForeground(col == 3 ? Theme.GREEN : Theme.TEXT_MAIN);
                    setFont(col == 3 ? Theme.FONT_BOLD : Theme.FONT_PLAIN);
                    setHorizontalAlignment(col == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                    return this;
                }
            });
            paidTable.getColumnModel().getColumn(1).setPreferredWidth(40);
            paidTable.getColumnModel().getColumn(2).setPreferredWidth(90);
            paidTable.getColumnModel().getColumn(3).setPreferredWidth(130);
            paidTable.getColumnModel().getColumn(4).setPreferredWidth(100);
            paidTable.getColumnModel().getColumn(5).setPreferredWidth(340);

            JScrollPane hs = new JScrollPane(paidTable);
            hs.setAlignmentX(Component.LEFT_ALIGNMENT);
            hs.setPreferredSize(new Dimension(0, Math.min(paidInvs.size() * 34 + 50, 180)));
            hs.setBorder(BorderFactory.createLineBorder(Theme.GREEN_BORDER));
            body.add(hs);
        }

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(null);
        bodyScroll.getViewport().setBackground(Theme.BG_CARD);

        // Button bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.setBorder(new EmptyBorder(10, 12, 10, 12));

        if (isAdmin && !pendingEnrs.isEmpty()) {
            JButton cashBtn = ComponentFactory.primaryButton("✓ Xác nhận Thu tiền mặt");
            cashBtn.setBackground(Theme.GREEN);
            cashBtn.addActionListener(ev -> {
                // Dùng EnrollmentStreamQueries cho joining (stream: map + joining)
                String courseList = pendingEnrs.stream()
                        .map(e -> e.getClazz().getCourse().getCourseName()
                                + " (" + e.getClazz().getClassName() + ")")
                        .collect(Collectors.joining("<br>• "));
                String msg = String.format(
                        "<html>Xác nhận <b>thu tiền mặt</b> cho học viên <b>%s</b><br><br>"
                        + "Các môn:<br>• %s<br><br>Tổng: <b style='color:darkorange;'>%,.0f VND</b></html>",
                        student.getFullName(), courseList, totalDebt);
                if (JOptionPane.showConfirmDialog(dlg, msg, "Xác nhận Thu tiền mặt",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return;

                Invoice invToConfirm = pendingInv != null ? pendingInv : new Invoice();
                if (pendingInv == null) {
                    invToConfirm.setStudent(student);
                    invToConfirm.setIssueDate(LocalDate.now());
                    invToConfirm.setStatus(com.company.ems.model.enums.InvoiceStatus.CHO_THANH_TOAN.getValue());
                }
                // Dùng EnrollmentStreamQueries cho joining (stream: map + joining)
                String enrollIds   = EnrollmentStreamQueries.joinEnrollmentIds(pendingEnrs, ",");
                String courseNames = EnrollmentStreamQueries.joinCourseNames(pendingEnrs, "; ");
                invToConfirm.setTotalAmount(totalDebt);
                invToConfirm.setNote("eids:" + enrollIds + "|" + courseNames);
                invToConfirm.setStatus(com.company.ems.model.enums.InvoiceStatus.DA_THANH_TOAN.getValue());
                if (pendingInv == null) invoiceService.save(invToConfirm);
                else invoiceService.update(invToConfirm);

                Payment p = new Payment();
                p.setInvoice(invToConfirm);
                p.setStudent(student);
                p.setAmount(totalDebt);
                p.setPaymentMethod(com.company.ems.model.enums.PaymentMethod.TIEN_MAT.getValue());
                p.setPaymentDate(LocalDate.now().atStartOfDay());
                p.setStatus(com.company.ems.model.enums.PaymentStatus.HOAN_THANH.getValue());
                paymentService.save(p);

                pendingEnrs.forEach(e -> {
                    e.setStatus(com.company.ems.model.enums.EnrollmentStatus.DA_THANH_TOAN.getValue());
                    enrollmentService.update(e);
                });

                if (onDataChanged != null) onDataChanged.run(); else loadData();
                dlg.dispose();
                JOptionPane.showMessageDialog(owner, "Đã xác nhận thanh toán tiền mặt thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            });
            btnBar.add(cashBtn);
        }

        JButton closeBtn = ComponentFactory.secondaryButton("Đóng");
        closeBtn.addActionListener(ev -> dlg.dispose());
        btnBar.add(closeBtn);

        dlg.add(header,     BorderLayout.NORTH);
        dlg.add(bodyScroll, BorderLayout.CENTER);
        dlg.add(btnBar,     BorderLayout.SOUTH);

        int h = 180 + pendingEnrs.size() * 34 + paidInvs.size() * 34 + 120;
        dlg.setSize(820, Math.min(Math.max(h, 300), 620));
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}

