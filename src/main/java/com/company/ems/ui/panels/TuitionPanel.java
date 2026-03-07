package com.company.ems.ui.panels;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.model.Student;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;
import com.company.ems.service.StudentService;
import com.company.ems.ui.UI;

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

    private static final Color BG_PAGE      = new Color(248, 250, 252);
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color ROW_EVEN     = Color.WHITE;
    private static final Color ROW_ODD      = new Color(248, 250, 252);
    private static final Color ROW_SELECT   = new Color(219, 234, 254);
    private static final Color C_GREEN      = new Color(22, 163, 74);
    private static final Color C_ORANGE     = new Color(234, 88, 12);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    // _sid | STT | Ma HV | Ho ten | HD cho TT | Tong no | Trang thai
    private static final String[] COLUMNS = {
        "_sid", "STT",
        "M\u00e3 HV",
        "H\u1ecd v\u00e0 t\u00ean",
        "H\u0110 ch\u1edd TT",
        "T\u1ed5ng n\u1ee3",
        "Tr\u1ea1ng th\u00e1i"
    };

    private final EnrollmentService enrollmentService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final StudentService studentService;
    private final boolean isAdmin;
    private final Long loggedInStudentId;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Runnable onDataChanged;
    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public TuitionPanel(EnrollmentService enrollmentService, InvoiceService invoiceService,
                        PaymentService paymentService,
                        StudentService studentService, boolean isAdmin, Long loggedInStudentId) {
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
        setBackground(BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadData();
    }

    // â”€â”€ UI Builders â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // "Quan ly Hoc phi & Hoa don" / "Hoc phi & Bien lai cua toi"
        JLabel title = new JLabel(isAdmin
            ? "Qu\u1ea3n l\u00fd H\u1ecdc ph\u00ed & H\u00f3a \u0111\u01a1n"
            : "H\u1ecdc ph\u00ed & Bi\u00ean lai c\u1ee7a t\u00f4i");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        bar.add(title, BorderLayout.WEST);

        // "Nhap ma HV de tra no (VD: HV0004)..."
        searchField.setPreferredSize(new Dimension(320, 38));
        searchField.setFont(FONT_MAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText",
            "Nh\u1eadp m\u00e3 HV \u0111\u1ec3 tra n\u1ee3 (VD: HV0004)...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                filterTable(searchField.getText().trim());
            }
        });
        bar.add(searchField, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTableCard() {
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);
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
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? ROW_SELECT
                        : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
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
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JTableHeader header = t.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        var baseR = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, v, s, f, row, col) -> {
            Component c = baseR.getTableCellRendererComponent(tbl, v, s, f, row, col);
            if (c instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return c;
        });

        // Hide _sid column
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setWidth(0);

        // "Tong no" column renderer â€” orange if debt, green if clear
        t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? ROW_SELECT : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                String s = val != null ? val.toString() : "";
                setForeground(s.equals("0 VND") || s.isEmpty() ? C_GREEN : C_ORANGE);
                setFont(FONT_BOLD);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        });

        // "Trang thai" column renderer
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? ROW_SELECT : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                String s = val != null ? val.toString() : "";
                // "Sach no" green, "Con no" orange
                setForeground("S\u1ea1ch n\u1ee3".equals(s) ? C_GREEN
                            : "C\u00f2n n\u1ee3".equals(s) ? C_ORANGE : TEXT_MUTED);
                setFont(FONT_BOLD);
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
                if (e.getClickCount() == 1 && t.getSelectedRow() >= 0)
                    openStudentInvoiceDetail();
            }
        });
        return t;
    }

    // â”€â”€ Data â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void loadData() {
        try {
            tableModel.setRowCount(0);

            List<Invoice> allInvoices = invoiceService.findAll().stream()
                    .filter(i -> i.getStudent() != null)
                    .collect(Collectors.toList());

            if (!isAdmin) {
                allInvoices = allInvoices.stream()
                        .filter(i -> i.getStudent().getStudentId().equals(loggedInStudentId))
                        .collect(Collectors.toList());
            }

            // Group invoices by student
            Map<Long, List<Invoice>> grouped = allInvoices.stream()
                    .collect(Collectors.groupingBy(i -> i.getStudent().getStudentId()));

            // â”€â”€ Äá»“ng bá»™ há»c phÃ­ thá»±c táº¿ tá»« enrollment vÃ o invoice "Chờ thanh toán" â”€â”€
            // Cháº¡y má»—i láº§n loadData: Ä‘áº£m báº£o giÃ¡ luÃ´n khá»›p dÃ¹ há»c phÃ­ khoÃ¡ há»c vá»«a thay Ä‘á»•i.
            List<Enrollment> pendingEnrollments = enrollmentService.findAll().stream()
                    .filter(e -> e.getStudent() != null && "\u0110\u00e3 \u0111\u0103ng k\u00fd".equals(e.getStatus()))
                    .collect(Collectors.toList());

            // Group enrollments by student
            Map<Long, List<Enrollment>> enrollByStudent = pendingEnrollments.stream()
                    .collect(Collectors.groupingBy(e -> e.getStudent().getStudentId()));

            enrollByStudent.forEach((sid, enrs) -> {
                Student student = enrs.get(0).getStudent();
                BigDecimal correctFee = enrs.stream()
                        .map(e -> e.getClazz().getCourse().getFee())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Invoice> studentInvoices = grouped.get(sid);
                Invoice pendingInv = studentInvoices == null ? null :
                        studentInvoices.stream()
                                .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                                .findFirst().orElse(null);

                if (pendingInv == null) {
                    if (correctFee.compareTo(BigDecimal.ZERO) <= 0) return;
                    try {
                        Invoice newInv = new Invoice();
                        newInv.setStudent(student);
                        newInv.setTotalAmount(correctFee);
                        newInv.setIssueDate(LocalDate.now());
                        newInv.setStatus("Chờ thanh toán");
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
                invList.stream()
                        .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                        .forEach(i -> {
                            try { invoiceService.delete(i.getInvoiceId()); }
                            catch (Exception ignored) {}
                        });
            });

            // Sort: most debt first
            List<Map.Entry<Long, List<Invoice>>> entries = grouped.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, List<Invoice>>, BigDecimal>comparing(
                        entry -> entry.getValue().stream()
                            .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                            .map(Invoice::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                        Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            int[] idx = {1};
            entries.forEach(entry -> {
                Long sid = entry.getKey();
                List<Invoice> invs = entry.getValue();
                Student student = invs.get(0).getStudent();

                long pendingCount = invs.stream()
                        .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                        .count();
                BigDecimal totalDebt = invs.stream()
                        .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                        .map(Invoice::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String debtStr = totalDebt.compareTo(BigDecimal.ZERO) == 0
                        ? "0 VND"
                        : String.format("%,.0f VND", totalDebt);
                String status = totalDebt.compareTo(BigDecimal.ZERO) == 0
                        ? "Sạch nợ"
                        : "Còn nợ";

                tableModel.addRow(new Object[]{
                    sid, idx[0]++,
                    String.format("HV%04d", sid),
                    student.getFullName(),
                    (int) pendingCount,
                    debtStr, status
                });
            });

            BigDecimal systemDebt = entries.stream()
                    .flatMap(e -> e.getValue().stream())
                    .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statusLabel.setText(String.format(
                "T\u1ed5ng: %d h\u1ecdc vi\u00ean c\u00f3 h\u00f3a \u0111\u01a1n  |  T\u1ed5ng n\u1ee3 to\u00e0n h\u1ec7 th\u1ed1ng: %,.0f VND",
                grouped.size(), systemDebt));

            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Kh\u00f4ng th\u1ec3 t\u1ea3i d\u1eef li\u1ec7u: " + e.getMessage(),
                "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterTable(String keyword) {
        if (keyword.isEmpty()) { sorter.setRowFilter(null); return; }
        String num = keyword.replaceAll("(?i)^hv0*", "");
        sorter.setRowFilter(RowFilter.regexFilter("(?i)(" + keyword + "|HV0*" + num + ")", 2, 3));
    }

    // â”€â”€ Student invoice detail dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void openStudentInvoiceDetail() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        Long sid = (Long) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        showStudentInvoiceDialog(sid);
    }

    private void showStudentInvoiceDialog(Long sid) {
        List<Invoice> allInvs = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null
                          && i.getStudent().getStudentId().equals(sid))
                .sorted(Comparator.comparing(Invoice::getIssueDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // Láº¥y cÃ¡c enrollment "\u0110\u00e3 \u0111\u0103ng k\u00fd" (chÆ°a thanh toÃ¡n) cá»§a há»c viÃªn
        List<Enrollment> pendingEnrs = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                          && e.getStudent().getStudentId().equals(sid)
                          && "\u0110\u00e3 \u0111\u0103ng k\u00fd".equals(e.getStatus()))
                .collect(Collectors.toList());

        // Láº¥y cÃ¡c hÃ³a Ä‘Æ¡n Ä‘Ã£ thanh toÃ¡n
        List<Invoice> paidInvs = allInvs.stream()
                .filter(i -> "Đã thanh toán".equals(i.getStatus()))
                .collect(Collectors.toList());

        // TÃ¬m invoice "Chờ thanh toán" Ä‘á»ƒ dÃ¹ng khi confirm
        Invoice pendingInv = allInvs.stream()
                .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        if (allInvs.isEmpty() && pendingEnrs.isEmpty()) return;

        Student student = !allInvs.isEmpty()
                ? allInvs.get(0).getStudent()
                : pendingEnrs.get(0).getStudent();
        String code = String.format("HV%04d", sid);

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner,
                "H\u00f3a \u0111\u01a1n: " + student.getFullName() + " (" + code + ")", true);
        dlg.setLayout(new BorderLayout());

        // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel titleLbl = new JLabel(student.getFullName() + "  |  " + code);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        BigDecimal totalDebt = pendingEnrs.stream()
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        JLabel debtLbl = new JLabel("Còn nợ: " + String.format("%,.0f VND", totalDebt));
        debtLbl.setFont(FONT_BOLD);
        debtLbl.setForeground(totalDebt.compareTo(BigDecimal.ZERO) == 0 ? C_GREEN : C_ORANGE);
        header.add(debtLbl, BorderLayout.EAST);

        // â”€â”€ Body â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(14, 16, 6, 16));

        // â”€ Báº£ng 1: CÃ¡c khoáº£n CHá»œ THANH TOÃN (tá»«ng mÃ´n cá»¥ thá»ƒ) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (!pendingEnrs.isEmpty()) {
            JLabel lbl1 = new JLabel("  KHO\u1ea2N CH\u1edc THANH TO\u00c1N");
            lbl1.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl1.setForeground(C_ORANGE);
            lbl1.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lbl1);
            body.add(Box.createVerticalStrut(6));

            String[] pendCols = {"STT", "Khóa học", "L\u1edbp", "Học phí (VND)", "Ngày ĐK"};
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
            // DÃ²ng tá»•ng
            pendModel.addRow(new Object[]{
                "", "T\u1ed4NG C\u1ed8NG", "", String.format("%,.0f", totalDebt), ""
            });

            JTable pendTable = new JTable(pendModel);
            pendTable.setFont(FONT_MAIN);
            pendTable.setRowHeight(34);
            pendTable.setShowGrid(false);
            pendTable.setIntercellSpacing(new Dimension(0, 0));
            pendTable.setEnabled(false);

            // CÄƒn pháº£i cá»™t há»c phÃ­
            DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
            rightR.setHorizontalAlignment(SwingConstants.RIGHT);
            pendTable.getColumnModel().getColumn(3).setCellRenderer(rightR);

            // TÃ´ Ä‘áº­m dÃ²ng tá»•ng + mÃ u cam cho há»c phÃ­ tá»«ng dÃ²ng
            pendTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    boolean isTotalRow = row == t.getRowCount() - 1;
                    setBackground(isTotalRow ? new Color(255, 247, 237) : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
                    setForeground(isTotalRow ? C_ORANGE : TEXT_MAIN);
                    setFont(isTotalRow ? FONT_BOLD : (col == 3 ? FONT_BOLD : FONT_MAIN));
                    if (col == 3 && !isTotalRow) setForeground(C_ORANGE);
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
            ps.setBorder(BorderFactory.createLineBorder(new Color(254, 215, 170)));
            body.add(ps);
            body.add(Box.createVerticalStrut(16));
        }

        // â”€ Báº£ng 2: Lá»‹ch sá»­ Ä‘Ã£ thanh toÃ¡n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (!paidInvs.isEmpty()) {
            JLabel lbl2 = new JLabel("  L\u1ecbCH S\u1eec \u0110\u00c3 THANH TO\u00c1N");
            lbl2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl2.setForeground(C_GREEN);
            lbl2.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lbl2);
            body.add(Box.createVerticalStrut(6));

            String[] paidCols = {"_iid", "STT", "Mã HĐ", "S\u1ed1 ti\u1ec1n (VND)", "Ngày lập", "Các môn học"};
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
            paidTable.setFont(FONT_MAIN);
            paidTable.setRowHeight(34);
            paidTable.setShowGrid(false);
            paidTable.setIntercellSpacing(new Dimension(0, 0));
            paidTable.setEnabled(false);

            // áº¨n _iid
            paidTable.getColumnModel().getColumn(0).setMinWidth(0);
            paidTable.getColumnModel().getColumn(0).setMaxWidth(0);

            paidTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    setForeground(col == 3 ? C_GREEN : TEXT_MAIN);
                    setFont(col == 3 ? FONT_BOLD : FONT_MAIN);
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
            hs.setBorder(BorderFactory.createLineBorder(new Color(187, 247, 208)));
            body.add(hs);
        }

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(null);
        bodyScroll.getViewport().setBackground(Color.WHITE);

        // â”€â”€ Button bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.setBorder(new EmptyBorder(10, 12, 10, 12));

        if (isAdmin && !pendingEnrs.isEmpty()) {
            JButton cashBtn = new JButton("\u2713 X\u00e1c nh\u1eadn Thu ti\u1ec1n m\u1eb7t");
            cashBtn.setBackground(new Color(22, 163, 74));
            cashBtn.setForeground(Color.WHITE);
            cashBtn.setFont(FONT_BOLD);
            cashBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            cashBtn.setFocusPainted(false);
            cashBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            cashBtn.addActionListener(ev -> {
                String courseList = pendingEnrs.stream()
                        .map(e -> e.getClazz().getCourse().getCourseName()
                               + " (" + e.getClazz().getClassName() + ")")
                        .collect(Collectors.joining("<br>\u2022 "));
                String msg = String.format(
                    "<html>Xác nhận <b>thu tiền mặt</b> cho học viên <b>%s</b><br><br>"
                    + "C\u00e1c m\u00f4n:<br>\u2022 %s<br><br>"
                    + "T\u1ed5ng: <b style='color:darkorange;'>%,.0f VND</b></html>",
                    student.getFullName(), courseList, totalDebt);

                int ok = JOptionPane.showConfirmDialog(dlg, msg,
                        "Xác nhận Thu tiền mặt",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;

                // DÃ¹ng invoice "Chờ thanh toán" hiá»‡n cÃ³ hoáº·c táº¡o má»›i
                Invoice invToConfirm = pendingInv;
                if (invToConfirm == null) {
                    invToConfirm = new Invoice();
                    invToConfirm.setStudent(student);
                    invToConfirm.setIssueDate(LocalDate.now());
                    invToConfirm.setStatus("Chờ thanh toán");
                }
                String enrollIds = pendingEnrs.stream()
                        .map(e -> String.valueOf(e.getEnrollmentId()))
                        .collect(Collectors.joining(","));
                String courseNames = pendingEnrs.stream()
                        .map(e -> e.getClazz().getCourse().getCourseName())
                        .collect(Collectors.joining("; "));
                invToConfirm.setTotalAmount(totalDebt);
                invToConfirm.setNote("eids:" + enrollIds + "|" + courseNames);
                invToConfirm.setStatus("Đã thanh toán");
                if (pendingInv == null) invoiceService.save(invToConfirm);
                else invoiceService.update(invToConfirm);

                Payment p = new Payment();
                p.setInvoice(invToConfirm);
                p.setStudent(student);
                p.setAmount(totalDebt);
                p.setPaymentMethod("Cash");
                p.setPaymentDate(LocalDate.now().atStartOfDay());
                p.setStatus("Completed");
                paymentService.save(p);

                pendingEnrs.forEach(e -> {
                    e.setStatus("Đã thanh toán");
                    enrollmentService.update(e);
                });

                if (onDataChanged != null) onDataChanged.run(); else loadData();
                dlg.dispose();
                JOptionPane.showMessageDialog(owner,
                        "Đã xác nhận thanh toán tiền mặt thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            });
            btnBar.add(cashBtn);
        }

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(FONT_MAIN);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
