package com.company.ems.ui.panels.student;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.model.Student;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;
import com.company.ems.service.StudentService;
import com.company.ems.stream.InvoiceStreamQueries;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import com.company.ems.stream.EnrollmentStreamQueries;

public class StudentTuitionPanel extends JPanel {

    private static final DateTimeFormatter FMT_FULL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Services & state ─────────────────────────────────────────────────────
    private final InvoiceService    invoiceService;
    private final PaymentService    paymentService;
    private final EnrollmentService enrollmentService;
    private final StudentService    studentService;
    private final Long              loggedInStudentId;

    private List<Enrollment> pendingEnrollments = new ArrayList<>();
    private String           selectedPaymentMethod = "Tiền mặt";
    private Runnable         onDataChanged;

    /** Callback từ StudentMainFrame để reload các panel khác sau khi thanh toán. */
    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    // ── Constructor ──────────────────────────────────────────────────────────
    public StudentTuitionPanel(InvoiceService invoiceService, PaymentService paymentService,
                               EnrollmentService enrollmentService, StudentService studentService,
                               Long loggedInStudentId) {
        this.invoiceService    = invoiceService;
        this.paymentService    = paymentService;
        this.enrollmentService = enrollmentService;
        this.studentService    = studentService;
        this.loggedInStudentId = loggedInStudentId;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        loadData();
    }

    // ── Load / rebuild ───────────────────────────────────────────────────────
    public void loadData() {
        // [REFACTORED]: Dùng SQL thay vì findAll().stream() để tránh tải toàn bộ dữ liệu lên RAM
        pendingEnrollments = enrollmentService.findByStudentIdAndStatus(loggedInStudentId, "Đã đăng ký");

        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        Student student = studentService.findById(loggedInStudentId);

        // [REFACTORED]: Dùng Class StreamQueries
        BigDecimal totalDebt = EnrollmentStreamQueries.calculateTotalFee(pendingEnrollments);

        JPanel topSection = new JPanel(new GridLayout(1, 2, 20, 0));
        topSection.setBackground(Theme.BG_PAGE);
        topSection.add(buildStudentInfoCard(student, totalDebt));
        topSection.add(buildPaymentMethodCard());

        JPanel container = new JPanel(new BorderLayout(0, 28));
        container.setOpaque(false);
        container.add(topSection, BorderLayout.NORTH);
        container.add(buildHistorySection(), BorderLayout.CENTER);

        JScrollPane scrollMain = new JScrollPane(container);
        scrollMain.setBorder(null);
        scrollMain.getViewport().setOpaque(false);
        scrollMain.setOpaque(false);
        add(scrollMain, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LEFT CARD – Thông tin học viên
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildStudentInfoCard(Student s, BigDecimal totalDebt) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(new EmptyBorder(26, 26, 26, 26));

        addSectionHeader(card, "THÔNG TIN HỌC VIÊN");
        card.add(Box.createVerticalStrut(18));

        addInfoRow(card, "Mã học viên (MSSV)", s != null ? String.valueOf(s.getStudentId()) : "—");
        card.add(Box.createVerticalStrut(14));
        addInfoRow(card, "Họ và tên học viên",  s != null ? s.getFullName() : "—");
        card.add(Box.createVerticalStrut(14));
        addInfoRow(card, "Email / Liên hệ",
                s != null && s.getEmail() != null ? s.getEmail()
                        : (s != null && s.getPhone() != null ? s.getPhone() : "—"));
        card.add(Box.createVerticalStrut(18));

        // Dòng số tiền nợ
        JLabel lblDebtTitle = new JLabel("Số tiền nợ học phí");
        lblDebtTitle.setFont(Theme.FONT_SMALL);
        lblDebtTitle.setForeground(new Color(148, 163, 184));
        lblDebtTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblDebtTitle);
        card.add(Box.createVerticalStrut(6));

        JPanel debtRow = new JPanel(new BorderLayout(10, 0));
        debtRow.setOpaque(false);
        debtRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        debtRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean hasDebt = !pendingEnrollments.isEmpty();
        JLabel lblAmt = new JLabel(hasDebt
                ? String.format("%,.0f vnd", totalDebt)
                : "0 đ  —  Không có khoản nợ");
        lblAmt.setFont(new Font("Segoe UI", Font.BOLD, hasDebt ? 22 : 15));
        lblAmt.setForeground(hasDebt ? Color.WHITE : new Color(74, 222, 128));
        if (hasDebt) {
            lblAmt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblAmt.setToolTipText("Nhấp để xem chi tiết các khoản phí");
            lblAmt.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { showFeeDetailDialog(); }
                @Override public void mouseEntered(MouseEvent e) { lblAmt.setForeground(new Color(147, 197, 253)); }
                @Override public void mouseExited(MouseEvent e)  { lblAmt.setForeground(Color.WHITE); }
            });
        }
        debtRow.add(lblAmt, BorderLayout.WEST);

        if (hasDebt) {
            JButton btnArrow = ComponentFactory.primaryButton("→");
            btnArrow.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btnArrow.addActionListener(e -> showFeeDetailDialog());
            debtRow.add(btnArrow, BorderLayout.EAST);
        }
        card.add(debtRow);
        return card;
    }

    private void addSectionHeader(JPanel parent, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL_BOLD);
        lbl.setForeground(new Color(148, 163, 184));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
    }

    private void addInfoRow(JPanel parent, String label, String value) {
        JLabel lblLbl = new JLabel(label);
        lblLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLbl.setForeground(new Color(148, 163, 184));
        lblLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(Theme.FONT_PLAIN);
        lblVal.setForeground(Color.WHITE);
        lblVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lblLbl);
        parent.add(Box.createVerticalStrut(3));
        parent.add(lblVal);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RIGHT CARD – Chọn hình thức thanh toán
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildPaymentMethodCard() {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(22, 22, 22, 22)));

        JLabel lblHeader = new JLabel("HÌNH THỨC THANH TOÁN");
        lblHeader.setFont(Theme.FONT_BOLD);
        lblHeader.setForeground(new Color(30, 41, 59));
        card.add(lblHeader, BorderLayout.NORTH);

        JLabel lblMethod = new JLabel("🏦  Chuyển khoản ngân hàng");
        lblMethod.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMethod.setForeground(Theme.PRIMARY);
        lblMethod.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.PRIMARY, 2, true),
                new EmptyBorder(8, 14, 8, 14)));
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        center.setOpaque(false);
        center.add(lblMethod);
        card.add(center, BorderLayout.CENTER);

        boolean hasDebt = !pendingEnrollments.isEmpty();
        JButton btnPay = hasDebt
                ? ComponentFactory.primaryButton("THANH TOÁN")
                : ComponentFactory.secondaryButton("THANH TOÁN");
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPay.setEnabled(hasDebt);
        btnPay.setBorder(new EmptyBorder(12, 24, 12, 24));
        btnPay.addActionListener(e -> showFeeDetailDialog());
        card.add(btnPay, BorderLayout.SOUTH);

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG – CÁC KHOẢN PHÍ (checkbox chọn môn)
    // ══════════════════════════════════════════════════════════════════════════
    private void showFeeDetailDialog() {
        if (pendingEnrollments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có khoản nợ nào cần thanh toán.");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "CÁC KHOẢN PHÍ", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.BG_CARD);

        String[] cols = {"Mã", "Tên", "Loại phí", "Số tiền (VND)", "Chọn"};
        DefaultTableModel mdl = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int c) {
                return c == 4 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };

        pendingEnrollments.forEach(e -> mdl.addRow(new Object[]{
                String.valueOf(e.getEnrollmentId()),
                e.getClazz().getCourse().getCourseName() + " – " + e.getClazz().getClassName(),
                "[Học phí học kỳ]",
                String.format("%,.0f", e.getClazz().getCourse().getFee()),
                Boolean.TRUE
        }));

        JTable tbl = buildStyledTable(mdl);
        tbl.setRowSelectionAllowed(false);
        tbl.setCellSelectionEnabled(false);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(50);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(330);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(120);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(110);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(55);

        DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
        rightR.setHorizontalAlignment(SwingConstants.RIGHT);
        tbl.getColumnModel().getColumn(3).setCellRenderer(rightR);

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(new LineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BG_CARD);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(Theme.BG_CARD);
        footer.setBorder(new EmptyBorder(12, 16, 14, 16));

        JLabel lblTotal = new JLabel();
        lblTotal.setFont(Theme.FONT_BOLD);
        lblTotal.setForeground(Theme.DANGER);
        refreshTotalLabel(mdl, lblTotal);
        mdl.addTableModelListener(e -> refreshTotalLabel(mdl, lblTotal));

        JPanel totalInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        totalInfo.setOpaque(false);
        totalInfo.add(new JLabel("Tổng tiền đã chọn: "));
        totalInfo.add(lblTotal);
        footer.add(totalInfo, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnClose = ComponentFactory.secondaryButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());

        JButton btnConfirm = ComponentFactory.primaryButton("THANH TOÁN");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirm.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnConfirm.addActionListener(e -> {
            List<Enrollment> chosen = IntStream.range(0, mdl.getRowCount())
                    .filter(i -> Boolean.TRUE.equals(mdl.getValueAt(i, 4)))
                    .mapToObj(pendingEnrollments::get)
                    .toList();
            if (chosen.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ít nhất một môn học để thanh toán.");
                return;
            }
            dialog.dispose();
            openBankTransferDialog(chosen);
        });

        btnPanel.add(btnClose);
        btnPanel.add(btnConfirm);
        footer.add(btnPanel, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);

        int tableH = Math.min(pendingEnrollments.size() * 40 + 48, 320);
        dialog.setSize(720, tableH + 100);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void refreshTotalLabel(DefaultTableModel mdl, JLabel lbl) {
        BigDecimal total = IntStream.range(0, mdl.getRowCount())
                .filter(i -> Boolean.TRUE.equals(mdl.getValueAt(i, 4)))
                .mapToObj(i -> pendingEnrollments.get(i).getClazz().getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lbl.setText(String.format("%,.0f VND", total));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BANK TRANSFER DIALOG
    // ══════════════════════════════════════════════════════════════════════════
    private void openBankTransferDialog(List<Enrollment> chosen) {
        BigDecimal total = EnrollmentStreamQueries.calculateTotalFee(chosen);
        String code = String.format("HVMS%d%06d", loggedInStudentId,
                System.currentTimeMillis() % 1_000_000L);
        Window owner = SwingUtilities.getWindowAncestor(this);
        BankTransferDialog dlg = new BankTransferDialog(owner, total, code,
                () -> {
                    selectedPaymentMethod = "Chuy\u1ec3n kho\u1ea3n";
                    processPayment(chosen);
                });
        dlg.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROCESS PAYMENT
    // ══════════════════════════════════════════════════════════════════════════
    private void processPayment(List<Enrollment> chosen) {
        Student student = chosen.get(0).getStudent();
        if (student != null && !"Hoạt động".equals(student.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Tài khoản của bạn đang bị khóa. Không thể thanh toán.",
                    "Tài khoản không hoạt động", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal total = EnrollmentStreamQueries.calculateTotalFee(chosen);

        String enrollIds   = EnrollmentStreamQueries.joinEnrollmentIds(chosen, ",");
        String courseNames = EnrollmentStreamQueries.joinCourseNames(chosen, "; ");
        String note        = "eids:" + enrollIds + "|" + courseNames.substring(0, Math.min(courseNames.length(), 180));

        Invoice inv = new Invoice();
        inv.setStudent(student);
        inv.setTotalAmount(total);
        inv.setIssueDate(LocalDate.now());
        inv.setStatus("Đã thanh toán");
        inv.setNote(note);
        invoiceService.save(inv);

        Payment p = new Payment();
        p.setInvoice(inv);
        p.setStudent(student);
        p.setAmount(total);
        p.setPaymentMethod(selectedPaymentMethod);
        p.setPaymentDate(LocalDate.now().atStartOfDay());
        p.setStatus("Đã thanh toán");
        paymentService.save(p);

        Set<Long> chosenIds = EnrollmentStreamQueries.mapToEnrollmentIds(chosen);
        for (Enrollment e : chosen) {
            e.setStatus("Đã thanh toán");
            enrollmentService.update(e);
        }

        List<Enrollment> remaining = EnrollmentStreamQueries.filterRemaining(pendingEnrollments, chosenIds);
        
        // Cần cập nhật thêm InvoiceRepository cho đoạn này
        Invoice pendingInv = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null
                          && i.getStudent().getStudentId().equals(loggedInStudentId)
                          && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);
        if (pendingInv != null) {
            if (remaining.isEmpty()) {
                invoiceService.delete(pendingInv.getInvoiceId());
            } else {
                BigDecimal remainTotal = EnrollmentStreamQueries.calculateTotalFee(remaining);
                String remainIds   = EnrollmentStreamQueries.joinEnrollmentIds(remaining, ",");
                String remainNames = EnrollmentStreamQueries.joinCourseNames(remaining, "; ");
                pendingInv.setTotalAmount(remainTotal);
                pendingInv.setNote("eids:" + remainIds + "|" + remainNames.substring(0, Math.min(remainNames.length(), 180)));
                invoiceService.update(pendingInv);
            }
        }

        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        JOptionPane.showMessageDialog(owner,
                "\u2705  Thanh to\u00e1n th\u00e0nh c\u00f4ng!\nPh\u01b0\u01a1ng th\u1ee9c: " + selectedPaymentMethod.toLowerCase()
                + "\nS\u1ed1 ti\u1ec1n: " + String.format("%,.0f VND", total),
                "Th\u00e0nh c\u00f4ng", JOptionPane.INFORMATION_MESSAGE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DANH SÁCH HÓA ĐƠN
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildHistorySection() {
        JPanel section = new JPanel(new BorderLayout(0, 12));
        section.setOpaque(false);

        JLabel lblHdr = new JLabel("DANH SÁCH HÓA ĐƠN");
        lblHdr.setFont(Theme.FONT_HEADER);
        lblHdr.setForeground(new Color(30, 41, 59));
        lblHdr.setBorder(new EmptyBorder(0, 0, 6, 0));
        section.add(lblHdr, BorderLayout.NORTH);

        // [REFACTORED]: Dùng SQL thay vì list stream filter và sort.
        List<Invoice> invoices = invoiceService.findByStudentIdAndStatusOrderByCreatedAtDesc(loggedInStudentId, "Đã thanh toán");


        if (invoices.isEmpty()) {
            JLabel lbl = new JLabel("Chưa có hóa đơn nào.");
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lbl.setForeground(Theme.TEXT_MUTED);
            section.add(lbl, BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel mdl = new DefaultTableModel(
                new String[]{"_id", "Mã hóa đơn", "Ngày tạo", "Ngày hết hạn", "Tổng", "Thanh toán"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Invoice i : invoices) {
            String created = i.getCreatedAt() != null
                    ? i.getCreatedAt().format(FMT_FULL) : i.getIssueDate().format(FMT_DATE);
            String due = i.getIssueDate().plusDays(7).format(FMT_DATE);
            mdl.addRow(new Object[]{
                i.getInvoiceId(), "INV" + String.format("%04d", i.getInvoiceId()),
                created, due, String.format("%,.0f", i.getTotalAmount()), mapStatus(i.getStatus())
            });
        }

        JTable tbl = buildStyledTable(mdl);
        tbl.setRowSelectionAllowed(false);
        tbl.setCellSelectionEnabled(false);
        TableStyler.hideColumn(tbl, 0);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(165);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(130);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(130);

        tbl.getTableHeader().setBackground(new Color(30, 41, 59));
        tbl.getTableHeader().setForeground(Color.WHITE);
        tbl.getTableHeader().setFont(Theme.FONT_BOLD);

        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String st = (String) mdl.getValueAt(row, 5);
                boolean paid = "Đã thanh toán".equals(st);
                setBackground(paid ? Theme.BG_CARD : Theme.AMBER_TINT);
                setForeground(Theme.TEXT_MAIN);
                if (col == 5) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setForeground(paid ? Theme.GREEN : Theme.AMBER);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setHorizontalAlignment(col == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                }
                return this;
            }
        });

        tbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = tbl.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    Long id = (Long) mdl.getValueAt(row, 0);
                    InvoiceStreamQueries.findById(invoices, id)
                            .ifPresent(inv -> showInvoiceDetailDialog(inv));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(new LineBorder(Theme.BORDER));
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG – Chi tiết hóa đơn
    // ══════════════════════════════════════════════════════════════════════════
    private void showInvoiceDetailDialog(Invoice inv) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner,
                "Chi tiết hóa đơn – INV" + String.format("%04d", inv.getInvoiceId()),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.BG_CARD);

        // Header
        JPanel header = new JPanel(new GridLayout(1, 3, 0, 0));
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel lblInvNo = new JLabel("INV" + String.format("%04d", inv.getInvoiceId()));
        lblInvNo.setFont(Theme.FONT_TITLE);
        lblInvNo.setForeground(Color.WHITE);

        String dateStr = inv.getCreatedAt() != null
                ? inv.getCreatedAt().format(FMT_FULL) : inv.getIssueDate().format(FMT_DATE);
        JLabel lblDate = new JLabel("Ngày: " + dateStr, SwingConstants.CENTER);
        lblDate.setFont(Theme.FONT_SMALL);
        lblDate.setForeground(new Color(148, 163, 184));

        String statusStr = mapStatus(inv.getStatus());
        JLabel lblStatus = new JLabel(statusStr, SwingConstants.RIGHT);
        lblStatus.setFont(Theme.FONT_BOLD);
        lblStatus.setForeground("Đã thanh toán".equals(statusStr)
                ? new Color(134, 239, 172) : new Color(253, 224, 132));

        header.add(lblInvNo);
        header.add(lblDate);
        header.add(lblStatus);
        dialog.add(header, BorderLayout.NORTH);

        // Table
        List<String[]> items = parseInvoiceItems(inv);
        DefaultTableModel mdl = new DefaultTableModel(
                new String[]{"STT", "Môn học / Lớp", "Loại phí", "Số tiền (VND)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < items.size(); i++) {
            mdl.addRow(new Object[]{i + 1, items.get(i)[0], "[Học phí học kỳ]", items.get(i)[1]});
        }

        JTable tbl = buildStyledTable(mdl);
        tbl.setRowSelectionAllowed(false);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(45);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(300);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(130);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(130);

        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(SwingConstants.CENTER);
        tbl.getColumnModel().getColumn(0).setCellRenderer(centerR);
        DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
        rightR.setHorizontalAlignment(SwingConstants.RIGHT);
        tbl.getColumnModel().getColumn(3).setCellRenderer(rightR);

        dialog.add(new JScrollPane(tbl), BorderLayout.CENTER);

        // Total + Buttons
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Theme.BG_CARD);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setBackground(Theme.BG_PAGE);
        totalRow.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel lblTotalTitle = new JLabel("TỔNG CỘNG");
        lblTotalTitle.setFont(Theme.FONT_BOLD);
        lblTotalTitle.setForeground(new Color(30, 41, 59));
        JLabel lblTotalAmt = new JLabel(String.format("%,.0f VND", inv.getTotalAmount()));
        lblTotalAmt.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotalAmt.setForeground(Theme.DANGER);
        totalRow.add(lblTotalTitle, BorderLayout.WEST);
        totalRow.add(lblTotalAmt,   BorderLayout.EAST);
        south.add(totalRow, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setBackground(Theme.BG_CARD);
        btnPanel.setBorder(new EmptyBorder(0, 12, 4, 12));

        JButton btnPrint = ComponentFactory.primaryButton("🖨 In hóa đơn");
        btnPrint.addActionListener(e -> generateInvoicePdf(inv, items, dialog));

        JButton btnClose = ComponentFactory.secondaryButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnPrint);
        btnPanel.add(btnClose);
        south.add(btnPanel, BorderLayout.SOUTH);
        dialog.add(south, BorderLayout.SOUTH);

        int h = Math.min(120 + items.size() * 42 + 140, 560);
        dialog.setSize(680, h);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // XUẤT PDF HÓA ĐƠN
    // ══════════════════════════════════════════════════════════════════════════
    private void generateInvoicePdf(Invoice inv, List<String[]> items, JDialog owner) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu hóa đơn PDF");
            chooser.setSelectedFile(new File("INV" + String.format("%04d", inv.getInvoiceId()) + ".pdf"));
            chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
            if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;

            File outFile = chooser.getSelectedFile();
            if (!outFile.getName().toLowerCase().endsWith(".pdf"))
                outFile = new File(outFile.getAbsolutePath() + ".pdf");

            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4, 50, 50, 60, 40);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new FileOutputStream(outFile));
            doc.open();

            String[] fontCandidates = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\calibri.ttf",
                "C:\\Windows\\Fonts\\tahoma.ttf"
            };
            String fontPath = null;
            for (String fp : fontCandidates) {
                if (new File(fp).exists()) { fontPath = fp; break; }
            }
            if (fontPath == null) throw new Exception("Không tìm thấy font hỗ trợ tiếng Việt trên máy.");

            com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                    fontPath, com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
            com.lowagie.text.Font fTitle  = new com.lowagie.text.Font(bf, 22, com.lowagie.text.Font.BOLD,   new Color(30, 41, 59));
            com.lowagie.text.Font fSub    = new com.lowagie.text.Font(bf, 11, com.lowagie.text.Font.NORMAL, Color.GRAY);
            com.lowagie.text.Font fHdr    = new com.lowagie.text.Font(bf, 11, com.lowagie.text.Font.BOLD,   Color.WHITE);
            com.lowagie.text.Font fBody   = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.NORMAL, new Color(15, 23, 42));
            com.lowagie.text.Font fBold   = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.BOLD,   new Color(15, 23, 42));
            com.lowagie.text.Font fMuted  = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.NORMAL, Color.GRAY);
            com.lowagie.text.Font fTotal  = new com.lowagie.text.Font(bf, 14, com.lowagie.text.Font.BOLD,   new Color(220, 38, 38));
            com.lowagie.text.Font fTLabel = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.BOLD,   new Color(30, 41, 59));

            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("HÓA ĐƠN HỌC PHÍ", fTitle);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            com.lowagie.text.Paragraph invNo = new com.lowagie.text.Paragraph(
                    "INV" + String.format("%04d", inv.getInvoiceId()), fSub);
            invNo.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            invNo.setSpacingAfter(16);
            doc.add(invNo);

            Student stu = inv.getStudent();
            if (stu != null) {
                com.lowagie.text.Font fInfoLabel = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.BOLD,   new Color(30, 41, 59));
                com.lowagie.text.Font fInfoValue = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.NORMAL, new Color(15, 23, 42));
                Color infoBg      = new Color(241, 245, 249);
                Color borderInfoC = new Color(226, 232, 240);

                com.lowagie.text.Paragraph stuHeading = new com.lowagie.text.Paragraph(
                        "THÔNG TIN HỌC VIÊN",
                        new com.lowagie.text.Font(bf, 11, com.lowagie.text.Font.BOLD, Theme.PRIMARY));
                stuHeading.setSpacingAfter(4);
                doc.add(stuHeading);

                com.lowagie.text.pdf.PdfPTable stuTbl = new com.lowagie.text.pdf.PdfPTable(4);
                stuTbl.setWidthPercentage(100);
                stuTbl.setWidths(new float[]{22f, 28f, 22f, 28f});
                stuTbl.setSpacingAfter(14);

                java.util.function.BiConsumer<String, String> addInfo = (label, value) -> {
                    com.lowagie.text.pdf.PdfPCell lCell = new com.lowagie.text.pdf.PdfPCell(
                            new com.lowagie.text.Phrase(label, fInfoLabel));
                    lCell.setBackgroundColor(infoBg);
                    lCell.setPadding(7); lCell.setBorderColor(borderInfoC); lCell.setBorderWidth(0.5f);
                    stuTbl.addCell(lCell);
                    com.lowagie.text.pdf.PdfPCell vCell = new com.lowagie.text.pdf.PdfPCell(
                            new com.lowagie.text.Phrase(value != null && !value.isEmpty() ? value : "—", fInfoValue));
                    vCell.setBackgroundColor(Color.WHITE);
                    vCell.setPadding(7); vCell.setBorderColor(borderInfoC); vCell.setBorderWidth(0.5f);
                    stuTbl.addCell(vCell);
                };

                String stuCode = String.format("HV%04d", stu.getStudentId());
                String dob     = stu.getDateOfBirth() != null ? stu.getDateOfBirth().format(FMT_DATE) : "";
                String gender  = stu.getGender() != null ? switch (stu.getGender()) {
                    case "Male"   -> "Nam";
                    case "Female" -> "Nữ";
                    default       -> stu.getGender();
                } : "";

                addInfo.accept("Họ tên:",           stu.getFullName());
                addInfo.accept("Mã học viên:",      stuCode);
                addInfo.accept("Ngày sinh:",         dob);
                addInfo.accept("Giới tính:",         gender);
                addInfo.accept("Email:",             stu.getEmail() != null ? stu.getEmail() : "");
                addInfo.accept("Điện thoại:",        stu.getPhone() != null ? stu.getPhone() : "");
                doc.add(stuTbl);
            }

            String dateStr2 = inv.getCreatedAt() != null
                    ? inv.getCreatedAt().format(FMT_FULL) : inv.getIssueDate().format(FMT_DATE);
            com.lowagie.text.pdf.PdfPTable infoTbl = new com.lowagie.text.pdf.PdfPTable(2);
            infoTbl.setWidthPercentage(100);
            infoTbl.setSpacingAfter(16);
            com.lowagie.text.pdf.PdfPCell cDate = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("Ngày tạo: " + dateStr2, fMuted));
            cDate.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            infoTbl.addCell(cDate);
            com.lowagie.text.pdf.PdfPCell cStatus = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(mapStatus(inv.getStatus()), fBold));
            cStatus.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cStatus.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            infoTbl.addCell(cStatus);
            doc.add(infoTbl);

            float[] colW = {5f, 52f, 22f, 21f};
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(colW);
            table.setWidthPercentage(100);
            table.setWidths(colW);
            table.setSpacingAfter(0);

            Color navyBg = new Color(30, 41, 59);
            String[] hdrLabels = {"STT", "Môn học / Lớp", "Loại phí", "Số tiền (VND)"};
            int[] hdrAligns = {com.lowagie.text.Element.ALIGN_CENTER, com.lowagie.text.Element.ALIGN_LEFT,
                               com.lowagie.text.Element.ALIGN_LEFT, com.lowagie.text.Element.ALIGN_RIGHT};
            for (int i = 0; i < hdrLabels.length; i++) {
                com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(hdrLabels[i], fHdr));
                c.setBackgroundColor(navyBg);
                c.setPadding(8);
                c.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                c.setHorizontalAlignment(hdrAligns[i]);
                table.addCell(c);
            }

            Color[] rowBg = {Color.WHITE, new Color(248, 250, 252)};
            int[] cellAligns = {com.lowagie.text.Element.ALIGN_CENTER, com.lowagie.text.Element.ALIGN_LEFT,
                                com.lowagie.text.Element.ALIGN_LEFT, com.lowagie.text.Element.ALIGN_RIGHT};
            for (int r = 0; r < items.size(); r++) {
                Color bg    = rowBg[r % 2];
                String[] row = items.get(r);
                String[] cellTexts = {String.valueOf(r + 1), row[0], "[Học phí học kỳ]", row[1]};
                for (int c = 0; c < cellTexts.length; c++) {
                    com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                            new com.lowagie.text.Phrase(cellTexts[c], fBody));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(7);
                    cell.setBorderColor(new Color(226, 232, 240));
                    cell.setBorderWidth(0.5f);
                    cell.setHorizontalAlignment(cellAligns[c]);
                    table.addCell(cell);
                }
            }
            doc.add(table);

            com.lowagie.text.pdf.PdfPTable totalTbl = new com.lowagie.text.pdf.PdfPTable(2);
            totalTbl.setWidthPercentage(100);
            totalTbl.setSpacingBefore(0);
            totalTbl.setSpacingAfter(30);
            Color sumBg   = new Color(241, 245, 249);
            Color borderC = new Color(226, 232, 240);
            com.lowagie.text.pdf.PdfPCell cTLabel = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("TỔNG CỘNG", fTLabel));
            cTLabel.setBackgroundColor(sumBg); cTLabel.setPadding(10);
            cTLabel.setBorderColor(borderC); cTLabel.setBorderWidth(0.5f);
            totalTbl.addCell(cTLabel);
            com.lowagie.text.pdf.PdfPCell cTAmt = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(String.format("%,.0f VND", inv.getTotalAmount()), fTotal));
            cTAmt.setBackgroundColor(sumBg); cTAmt.setPadding(10);
            cTAmt.setBorderColor(borderC); cTAmt.setBorderWidth(0.5f);
            cTAmt.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            totalTbl.addCell(cTAmt);
            doc.add(totalTbl);

            com.lowagie.text.Paragraph note2 = new com.lowagie.text.Paragraph(
                    "Đây là hóa đơn điện tử tự động tạo bởi hệ thống quản lý trung tâm ngoại ngữ.", fMuted);
            note2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(note2);

            doc.close();

            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().open(outFile);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner,
                    "Không thể tạo file PDF:\n" + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String[]> parseInvoiceItems(Invoice inv) {
        List<String[]> items = new ArrayList<>();
        String note = inv.getNote();

        if (note != null && note.startsWith("eids:")) {
            try {
                String eidsPart = note.contains("|")
                        ? note.substring(5, note.indexOf("|")) : note.substring(5);
                for (String idStr : eidsPart.split(",")) {
                    long eid = Long.parseLong(idStr.trim());
                    enrollmentService.findAll().stream()
                            .filter(e -> e.getEnrollmentId() == eid)
                            .findFirst()
                            .ifPresent(e -> items.add(new String[]{
                                e.getClazz().getCourse().getCourseName() + " – " + e.getClazz().getClassName(),
                                String.format("%,.0f VND", e.getClazz().getCourse().getFee())
                            }));
                }
            } catch (Exception ignored) {}
        }

        if (items.isEmpty() && inv.getStudent() != null) {
            Long sid = inv.getStudent().getStudentId();
            enrollmentService.findAll().stream()
                    .filter(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(sid))
                    .forEach(e -> {
                        try {
                            items.add(new String[]{
                                e.getClazz().getCourse().getCourseName() + " – " + e.getClazz().getClassName(),
                                String.format("%,.0f VND", e.getClazz().getCourse().getFee())
                            });
                        } catch (Exception ignored) {}
                    });
        }

        if (items.isEmpty()) {
            items.add(new String[]{"Học phí tổng hợp", String.format("%,.0f VND", inv.getTotalAmount())});
        }
        return items;
    }

    private String mapStatus(String s) {
        if (s == null) return "—";
        return switch (s) {
            case "Đã thanh toán", "Paid"          -> "Đã thanh toán";
            case "Chờ thanh toán", "Issued"        -> "Chờ thanh toán";
            default                                -> s;
        };
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        TableStyler.applyDefaults(tbl);
        tbl.setIntercellSpacing(new Dimension(12, 1));
        tbl.setShowVerticalLines(false);
        tbl.setSelectionBackground(new Color(224, 242, 254));
        return tbl;
    }
}

