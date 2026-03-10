package com.company.ems.ui.panels;

import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard doanh thu dành riêng cho Admin.
 * Hiển thị: KPI tổng quan, biểu đồ doanh thu theo tháng (bar text), danh sách giao dịch gần đây.
 */
public class RevenueDashboardPanel extends JPanel {

    // ── Design tokens ────────────────────────────────────────────────────
    private static final Color BG_PAGE    = new Color(248, 250, 252);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BORDER_COL = new Color(226, 232, 240);
    private static final Color TEXT_MAIN  = new Color(15,  23,  42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY    = new Color(37,  99,  235);
    private static final Color GREEN      = new Color(22,  163, 74);
    private static final Color AMBER      = new Color(217, 119, 6);
    private static final Color PURPLE     = new Color(124, 58,  237);
    private static final Color ROW_EVEN   = Color.WHITE;
    private static final Color ROW_ODD    = new Color(248, 250, 252);
    private static final Color ROW_SELECT = new Color(219, 234, 254);

    private static final Font FONT_MAIN    = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD,   26);
    private static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD,   14);

    private static final String[] TX_COLS = {
        "STT", "Học viên", "Số tiền", "Phương thức", "Ngày thanh toán", "Trạng thái", "Mã tham chiếu"
    };
    private static final String[] INV_COLS = {
        "STT", "Học viên", "Tổng tiền", "Ngày phát hành", "Trạng thái", "Ghi chú"
    };

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ── Services ─────────────────────────────────────────────────────────
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    // ── UI ────────────────────────────────────────────────────────────────
    private JLabel lblTotalRevenue, lblPaid, lblPending, lblTxCount;
    private DefaultTableModel txModel;
    private DefaultTableModel invModel;
    private JPanel barChartPanel;

    public RevenueDashboardPanel(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;

        setLayout(new BorderLayout(0, 16));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        add(buildKpiRow(),     BorderLayout.NORTH);
        add(buildCenterArea(), BorderLayout.CENTER);

        loadData();
    }

    // ──────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ──────────────────────────────────────────────────────────────────────

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 8, 0));

        lblTotalRevenue = new JLabel("—");
        lblPaid         = new JLabel("—");
        lblPending      = new JLabel("—");
        lblTxCount      = new JLabel("—");

        row.add(buildKpiCard("💰 Tổng đã thu (TT)",     lblTotalRevenue, PRIMARY));
        row.add(buildKpiCard("📄 Hóa đơn đã thanh toán", lblPaid,         GREEN));
        row.add(buildKpiCard("⏳ Hoá đơn chờ thanh toán", lblPending,   AMBER));
        row.add(buildKpiCard("🔢 Số giao dịch",         lblTxCount,     PURPLE));

        return row;
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(16, 20, 16, 20)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_KPI_LBL);
        lbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        card.add(lbl,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCenterArea() {
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);

        // Bar chart doanh thu 6 tháng gần nhất
        barChartPanel = new JPanel();
        barChartPanel.setLayout(new BoxLayout(barChartPanel, BoxLayout.Y_AXIS));
        barChartPanel.setBackground(BG_CARD);
        barChartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(16, 20, 16, 20)));

        JLabel chartTitle = new JLabel("📊  Doanh thu 6 tháng gần nhất (VNĐ)");
        chartTitle.setFont(FONT_SECTION);
        chartTitle.setForeground(TEXT_MAIN);
        chartTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        barChartPanel.add(chartTitle);
        barChartPanel.add(Box.createVerticalStrut(12));

        JScrollPane chartScroll = new JScrollPane(barChartPanel);
        chartScroll.setPreferredSize(new Dimension(0, 200));
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        chartScroll.getViewport().setBackground(BG_CARD);

        // Bảng giao dịch gần đây + hoá đơn chia đôi
        JSplitPane tables = buildTableSplit();

        center.add(chartScroll, BorderLayout.NORTH);
        center.add(tables,      BorderLayout.CENTER);
        return center;
    }

    private JSplitPane buildTableSplit() {
        // -- Bảng giao dịch --
        txModel = new DefaultTableModel(TX_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable txTable = buildStyledTable(txModel);
        txTable.getColumnModel().getColumn(0).setMaxWidth(45);

        JPanel txCard = buildTableCard("💳  Giao dịch thanh toán gần đây", txTable);

        // -- Bảng hoá đơn --
        invModel = new DefaultTableModel(INV_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable invTable = buildStyledTable(invModel);
        invTable.getColumnModel().getColumn(0).setMaxWidth(45);

        JPanel invCard = buildTableCard("🧾  Hoá đơn", invTable);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, txCard, invCard);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private JPanel buildTableCard(String title, JTable table) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(12, 16, 12, 16)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_MAIN);
        card.add(lbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(ROW_SELECT);
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                }
                c.setForeground(TEXT_MAIN);
                return c;
            }
        };
        tbl.setFont(FONT_MAIN);
        tbl.setRowHeight(32);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(ROW_SELECT);
        tbl.setSelectionForeground(TEXT_MAIN);
        tbl.setFillsViewportHeight(true);

        JTableHeader hdr = tbl.getTableHeader();
        hdr.setFont(FONT_BOLD);
        hdr.setBackground(new Color(241, 245, 249));
        hdr.setForeground(TEXT_MUTED);
        hdr.setPreferredSize(new Dimension(0, 36));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        return tbl;
    }

    // ──────────────────────────────────────────────────────────────────────
    // DATA
    // ──────────────────────────────────────────────────────────────────────

    public void loadData() {
        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override
            protected Object[] doInBackground() {
                List<Payment> payments = paymentService.findAll();
                List<Invoice> invoices = invoiceService.findAll();
                return new Object[]{payments, invoices};
            }

            @Override
            protected void done() {
                try {
                    Object[] result  = get();
                    @SuppressWarnings("unchecked")
                    List<Payment> payments = (List<Payment>) result[0];
                    @SuppressWarnings("unchecked")
                    List<Invoice> invoices = (List<Invoice>) result[1];
                    updateUI(payments, invoices);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void updateUI(List<Payment> payments, List<Invoice> invoices) {
        // ── KPI ──────────────────────────────────────────────────────────
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> "Hoàn thành".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingInv = invoices.stream()
                .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long txCount = payments.stream()
                .filter(p -> "Hoàn thành".equals(p.getStatus()))
                .count();

        // Tổng tiền thực thu (payments hoàn thành)
        BigDecimal paidInvoiceTotal = invoices.stream()
                .filter(i -> "Đã thanh toán".equals(i.getStatus()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalRevenue.setText(formatVnd(totalRevenue));
        lblPaid.setText(formatVnd(paidInvoiceTotal));
        lblPending.setText(formatVnd(pendingInv));
        lblTxCount.setText(txCount + " giao dịch");

        // ── Bar chart 6 tháng ────────────────────────────────────────────
        rebuildBarChart(payments);

        // ── Bảng giao dịch (50 mới nhất) ────────────────────────────────
        txModel.setRowCount(0);
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Payment> sorted = payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .collect(Collectors.toList());
        int stt = 1;
        for (Payment p : sorted) {
            String studentName = p.getStudent() != null ? p.getStudent().getFullName() : "—";
            String dateStr = p.getPaymentDate() != null ? p.getPaymentDate().format(dtFmt) : "—";
            txModel.addRow(new Object[]{
                stt++,
                studentName,
                formatVnd(p.getAmount()),
                p.getPaymentMethod(),
                dateStr,
                p.getStatus(),
                p.getReferenceCode() != null ? p.getReferenceCode() : ""
            });
        }

        // ── Bảng hoá đơn (50 mới nhất) ──────────────────────────────────
        invModel.setRowCount(0);
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Invoice> sortedInv = invoices.stream()
                .sorted(Comparator.comparing(Invoice::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .collect(Collectors.toList());
        int s2 = 1;
        for (Invoice inv : sortedInv) {
            String studentName = inv.getStudent() != null ? inv.getStudent().getFullName() : "—";
            String dateStr = inv.getIssueDate() != null ? inv.getIssueDate().format(dFmt) : "—";
            invModel.addRow(new Object[]{
                s2++,
                studentName,
                formatVnd(inv.getTotalAmount()),
                dateStr,
                inv.getStatus(),
                inv.getNote() != null ? inv.getNote() : ""
            });
        }
    }

    private void rebuildBarChart(List<Payment> payments) {
        // Xoá bar cũ (giữ lại title + strut đầu)
        while (barChartPanel.getComponentCount() > 2) {
            barChartPanel.remove(barChartPanel.getComponentCount() - 1);
        }

        // Tính doanh thu theo tháng (6 tháng gần nhất)
        LocalDate now = LocalDate.now();
        LinkedHashMap<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            monthlyRevenue.put(m.getYear() + "/" + String.format("%02d", m.getMonthValue()), BigDecimal.ZERO);
        }
        for (Payment p : payments) {
            if (!"Hoàn thành".equals(p.getStatus()) || p.getPaymentDate() == null) continue;
            LocalDate d = p.getPaymentDate().toLocalDate();
            String key = d.getYear() + "/" + String.format("%02d", d.getMonthValue());
            if (monthlyRevenue.containsKey(key)) {
                monthlyRevenue.merge(key, p.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal maxVal = monthlyRevenue.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (maxVal.compareTo(BigDecimal.ZERO) == 0) maxVal = BigDecimal.ONE;

        int barMaxWidth = 420;
        for (Map.Entry<String, BigDecimal> entry : monthlyRevenue.entrySet()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel monthLbl = new JLabel(entry.getKey());
            monthLbl.setFont(FONT_SMALL);
            monthLbl.setForeground(TEXT_MUTED);
            monthLbl.setPreferredSize(new Dimension(70, 20));

            int barWidth = maxVal.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : (int) (entry.getValue().doubleValue() / maxVal.doubleValue() * barMaxWidth);

            JPanel bar = new JPanel();
            bar.setBackground(PRIMARY);
            bar.setPreferredSize(new Dimension(Math.max(barWidth, 2), 18));
            bar.setMaximumSize(new Dimension(Math.max(barWidth, 2), 18));

            JLabel valLbl = new JLabel(" " + formatVnd(entry.getValue()));
            valLbl.setFont(FONT_SMALL);
            valLbl.setForeground(TEXT_MAIN);

            JPanel barRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            barRow.setOpaque(false);
            barRow.add(bar);
            barRow.add(valLbl);

            row.add(monthLbl, BorderLayout.WEST);
            row.add(barRow,   BorderLayout.CENTER);
            barChartPanel.add(row);
            barChartPanel.add(Box.createVerticalStrut(4));
        }

        barChartPanel.revalidate();
        barChartPanel.repaint();
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return CURRENCY_FMT.format(amount) + " ₫";
    }
}
