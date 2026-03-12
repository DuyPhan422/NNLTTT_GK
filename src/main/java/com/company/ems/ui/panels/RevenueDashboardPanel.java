package com.company.ems.ui.panels;

import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueDashboardPanel extends JPanel {

    // ── Design tokens ────────────────────────────────────────────────────
    private static final Color BG_PAGE    = new Color(245, 247, 250);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color BORDER_COL = new Color(226, 232, 240);
    private static final Color TEXT_MAIN  = new Color(15,  23,  42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color TEXT_LIGHT = new Color(148, 163, 184);
    private static final Color PRIMARY    = new Color(37,  99,  235);
    private static final Color PRIMARY_BG = new Color(239, 246, 255);
    private static final Color GREEN      = new Color(22,  163,  74);
    private static final Color GREEN_BG   = new Color(240, 253, 244);
    private static final Color AMBER      = new Color(217, 119,   6);
    private static final Color AMBER_BG   = new Color(255, 251, 235);
    private static final Color PURPLE     = new Color(124,  58, 237);
    private static final Color PURPLE_BG  = new Color(245, 243, 255);
    private static final Color RED        = new Color(220,  38,  38);
    private static final Color RED_BG     = new Color(254, 242, 242);
    private static final Color ROW_ODD    = new Color(250, 251, 253);
    private static final Color ROW_SELECT = new Color(239, 246, 255);

    private static final Font FONT_MAIN    = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD,   22);
    private static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD,   14);
    private static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD,   12);

    private static final String[] TX_COLS  = {
        "#", "Học viên", "Số tiền", "Phương thức", "Ngày TT", "Trạng thái", "Mã tham chiếu"
    };
    private static final String[] INV_COLS = {
        "#", "Học viên", "Tổng tiền", "Ngày phát hành", "Trạng thái", "Ghi chú"
    };

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ── Services ─────────────────────────────────────────────────────────
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    // ── UI state ─────────────────────────────────────────────────────────
    private JLabel            lblTotalRevenue, lblPaid, lblPending, lblTxCount;
    private JLabel            lblTxSubCount, lblInvSubCount;
    private DefaultTableModel txModel, invModel;
    private JTable            invTable;
    private JPanel            barChartPanel;
    private List<Invoice>     displayedInvoices = new ArrayList<>();

    public RevenueDashboardPanel(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;

        setLayout(new BorderLayout());
        setBackground(BG_PAGE);

        JPanel inner = new JPanel(new BorderLayout(0, 18));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));
        inner.add(buildKpiRow(),     BorderLayout.NORTH);
        inner.add(buildCenterArea(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_PAGE);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  KPI ROW
    // ════════════════════════════════════════════════════════════════════════

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        lblTotalRevenue = new JLabel("—");
        lblPaid         = new JLabel("—");
        lblPending      = new JLabel("—");
        lblTxCount      = new JLabel("—");

        row.add(buildKpiCard("TỔNG ĐÃ THU",       "Thanh toán hoàn thành",   lblTotalRevenue, PRIMARY, PRIMARY_BG));
        row.add(buildKpiCard("HOÁ ĐƠN ĐÃ TT",     "Tổng tiền đã thanh toán", lblPaid,         GREEN,   GREEN_BG));
        row.add(buildKpiCard("HOÁ ĐƠN CHỜ TT",    "Cần xử lý",               lblPending,      AMBER,   AMBER_BG));
        row.add(buildKpiCard("SỐ GIAO DỊCH",       "Giao dịch hoàn thành",    lblTxCount,      PURPLE,  PURPLE_BG));

        return row;
    }

    private JPanel buildKpiCard(String title, String subtitle,
                                JLabel valueLabel, Color accent, Color accentBg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        // Compound border: outer line → top accent band → inner padding
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 0, 0, 0, accent),
                        new EmptyBorder(14, 20, 16, 20))));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setBorder(new EmptyBorder(0, 0, 6, 0));

        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(TEXT_LIGHT);
        subLbl.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(titleLbl);
        body.add(valueLabel);
        body.add(subLbl);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CENTER AREA
    // ════════════════════════════════════════════════════════════════════════

    private JPanel buildCenterArea() {
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(buildChartCard(),  BorderLayout.NORTH);
        center.add(buildTablesArea(), BorderLayout.CENTER);
        return center;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BAR CHART CARD
    // ════════════════════════════════════════════════════════════════════════

    private JPanel buildChartCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(18, 22, 18, 22)));

        JLabel title = new JLabel("Doanh thu 6 tháng gần nhất");
        title.setFont(FONT_SECTION);
        title.setForeground(TEXT_MAIN);

        JLabel unit = new JLabel("Đơn vị: VNĐ");
        unit.setFont(FONT_SMALL);
        unit.setForeground(TEXT_MUTED);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(unit,  BorderLayout.EAST);

        barChartPanel = new JPanel();
        barChartPanel.setLayout(new BoxLayout(barChartPanel, BoxLayout.Y_AXIS));
        barChartPanel.setBackground(BG_CARD);

        card.add(titleRow,     BorderLayout.NORTH);
        card.add(barChartPanel, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TABLES AREA
    // ════════════════════════════════════════════════════════════════════════

    private JPanel buildTablesArea() {
        // ── Giao dịch ────────────────────────────────────────────────────
        txModel = new DefaultTableModel(TX_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable txTable = buildStyledTable(txModel);
        setColumnWidths(txTable, new int[]{38, 0, 110, 100, 90, 100, 110});
        txTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer(PRIMARY));
        txTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer());

        lblTxSubCount = new JLabel("");
        lblTxSubCount.setFont(FONT_SMALL);
        lblTxSubCount.setForeground(TEXT_MUTED);

        // ── Hoá đơn ──────────────────────────────────────────────────────
        invModel = new DefaultTableModel(INV_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invTable = buildStyledTable(invModel);
        setColumnWidths(invTable, new int[]{38, 0, 110, 106, 110, 0});
        invTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer(PRIMARY));
        invTable.getColumnModel().getColumn(4).setCellRenderer(statusRenderer());
        invTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = invTable.getSelectedRow();
                if (row >= 0 && row < displayedInvoices.size())
                    showInvoiceDetailDialog(displayedInvoices.get(row));
            }
        });
        invTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblInvSubCount = new JLabel("");
        lblInvSubCount.setFont(FONT_SMALL);
        lblInvSubCount.setForeground(TEXT_MUTED);

        JPanel area = new JPanel(new GridLayout(1, 2, 14, 0));
        area.setOpaque(false);
        area.add(buildTableCard("Giao dịch thanh toán gần đây", lblTxSubCount,  txTable));
        area.add(buildTableCard("Hoá đơn",                       lblInvSubCount, invTable));
        return area;
    }

    private JPanel buildTableCard(String title, JLabel badge, JTable table) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER_COL));

        JPanel hdr = new JPanel(new BorderLayout(10, 0));
        hdr.setBackground(new Color(248, 250, 252));
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(11, 16, 11, 16)));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_SECTION);
        titleLbl.setForeground(TEXT_MAIN);
        hdr.add(titleLbl, BorderLayout.WEST);
        hdr.add(badge,    BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        card.add(hdr,    BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TABLE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ODD);
                    c.setForeground(TEXT_MAIN);
                }
                return c;
            }
        };
        tbl.setFont(FONT_MAIN);
        tbl.setRowHeight(34);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(ROW_SELECT);
        tbl.setSelectionForeground(TEXT_MAIN);
        tbl.setFillsViewportHeight(true);

        JTableHeader hdr = tbl.getTableHeader();
        hdr.setFont(FONT_HEADER);
        hdr.setBackground(new Color(248, 250, 252));
        hdr.setForeground(TEXT_MUTED);
        hdr.setPreferredSize(new Dimension(0, 38));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        ((DefaultTableCellRenderer) hdr.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);
        return tbl;
    }

    private void setColumnWidths(JTable tbl, int[] widths) {
        for (int i = 0; i < widths.length && i < tbl.getColumnCount(); i++) {
            if (widths[i] > 0) {
                tbl.getColumnModel().getColumn(i).setMinWidth(widths[i]);
                tbl.getColumnModel().getColumn(i).setMaxWidth(widths[i]);
                tbl.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }
    }

    /** Status column — coloured badge background */
    private TableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                if (!sel) {
                    String s = v == null ? "" : v.toString();
                    switch (s) {
                        case "Hoàn thành": case "Đã thanh toán":
                            lbl.setBackground(GREEN_BG); lbl.setForeground(GREEN); break;
                        case "Chờ thanh toán":
                            lbl.setBackground(AMBER_BG); lbl.setForeground(AMBER); break;
                        case "Đã hủy": case "Hủy":
                            lbl.setBackground(RED_BG);   lbl.setForeground(RED);   break;
                        default:
                            lbl.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ODD);
                            lbl.setForeground(TEXT_MUTED);
                    }
                    lbl.setOpaque(true);
                }
                return lbl;
            }
        };
    }

    /** Amount column — right-aligned, coloured */
    private TableCellRenderer amountRenderer(Color color) {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                lbl.setBorder(new EmptyBorder(0, 0, 0, 10));
                if (!sel) {
                    lbl.setForeground(color);
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ODD);
                }
                return lbl;
            }
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════════════════════════════════════

    public void loadData() {
        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{paymentService.findAll(), invoiceService.findAll()};
            }
            @Override protected void done() {
                try {
                    Object[] res = get();
                    @SuppressWarnings("unchecked") List<Payment> payments = (List<Payment>) res[0];
                    @SuppressWarnings("unchecked") List<Invoice> invoices = (List<Invoice>) res[1];
                    updateUI(payments, invoices);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private void updateUI(List<Payment> payments, List<Invoice> invoices) {
        // ── KPI ──────────────────────────────────────────────────────────
        BigDecimal totalRevenue = sumAmounts(payments.stream()
                .filter(p -> "Hoàn thành".equals(p.getStatus()))
                .map(Payment::getAmount).collect(Collectors.toList()));
        BigDecimal paidInvTotal = sumAmounts(invoices.stream()
                .filter(i -> "Đã thanh toán".equals(i.getStatus()))
                .map(Invoice::getTotalAmount).collect(Collectors.toList()));
        BigDecimal pendingInv = sumAmounts(invoices.stream()
                .filter(i -> "Chờ thanh toán".equals(i.getStatus()))
                .map(Invoice::getTotalAmount).collect(Collectors.toList()));
        long txCount = payments.stream()
                .filter(p -> "Hoàn thành".equals(p.getStatus())).count();

        lblTotalRevenue.setText(formatVnd(totalRevenue));
        lblPaid.setText(formatVnd(paidInvTotal));
        lblPending.setText(formatVnd(pendingInv));
        lblTxCount.setText(txCount + " giao dịch");

        rebuildBarChart(payments);

        // ── Bảng giao dịch ───────────────────────────────────────────────
        txModel.setRowCount(0);
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Payment> sorted = payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50).collect(Collectors.toList());
        int i = 1;
        for (Payment p : sorted) {
            String name = p.getStudent() != null ? p.getStudent().getFullName() : "—";
            String date = p.getPaymentDate() != null ? p.getPaymentDate().format(dtFmt) : "—";
            txModel.addRow(new Object[]{
                i++, name, formatVnd(p.getAmount()),
                p.getPaymentMethod(), date, p.getStatus(),
                p.getReferenceCode() != null ? p.getReferenceCode() : ""
            });
        }
        if (lblTxSubCount != null) lblTxSubCount.setText(sorted.size() + " bản ghi");

        // ── Bảng hoá đơn ─────────────────────────────────────────────────
        invModel.setRowCount(0);
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Invoice> sortedInv = invoices.stream()
                .sorted(Comparator.comparing(Invoice::getIssueDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50).collect(Collectors.toList());
        displayedInvoices = new ArrayList<>(sortedInv);
        int j = 1;
        for (Invoice inv : sortedInv) {
            String name = inv.getStudent() != null ? inv.getStudent().getFullName() : "—";
            String date = inv.getIssueDate() != null ? inv.getIssueDate().format(dFmt) : "—";
            invModel.addRow(new Object[]{
                j++, name, formatVnd(inv.getTotalAmount()),
                date, inv.getStatus(),
                inv.getNote() != null ? inv.getNote() : ""
            });
        }
        if (lblInvSubCount != null) lblInvSubCount.setText(sortedInv.size() + " bản ghi");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BAR CHART
    // ════════════════════════════════════════════════════════════════════════

    private void rebuildBarChart(List<Payment> payments) {
        LocalDate now = LocalDate.now();
        LinkedHashMap<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            monthly.put(m.getYear() + "/" + String.format("%02d", m.getMonthValue()), BigDecimal.ZERO);
        }
        for (Payment p : payments) {
            if (!"Hoàn thành".equals(p.getStatus()) || p.getPaymentDate() == null) continue;
            LocalDate d = p.getPaymentDate().toLocalDate();
            String key = d.getYear() + "/" + String.format("%02d", d.getMonthValue());
            monthly.merge(key, p.getAmount(), BigDecimal::add);
        }

        BigDecimal maxVal = monthly.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (maxVal.compareTo(BigDecimal.ZERO) == 0) maxVal = BigDecimal.ONE;
        final BigDecimal finalMax = maxVal;

        barChartPanel.removeAll();
        barChartPanel.add(Box.createVerticalStrut(8));

        for (Map.Entry<String, BigDecimal> entry : monthly.entrySet()) {
            final double ratio    = entry.getValue().doubleValue() / finalMax.doubleValue();
            final boolean nonZero = entry.getValue().compareTo(BigDecimal.ZERO) > 0;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel monthLbl = new JLabel(entry.getKey());
            monthLbl.setFont(FONT_SMALL);
            monthLbl.setForeground(TEXT_MUTED);
            monthLbl.setPreferredSize(new Dimension(62, 18));
            monthLbl.setHorizontalAlignment(SwingConstants.RIGHT);

            // Custom-drawn track + filled bar
            JPanel bar = new JPanel() {
                @Override protected void paintComponent(Graphics g0) {
                    super.paintComponent(g0);
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int h  = 10;
                    int y0 = (getHeight() - h) / 2;
                    int tw = getWidth();
                    int fw = (int) (tw * ratio);
                    g.setColor(new Color(226, 232, 240));  // track
                    g.fillRoundRect(0, y0, tw, h, h, h);
                    if (fw > 2) {
                        g.setColor(nonZero ? PRIMARY : new Color(203, 213, 225));
                        g.fillRoundRect(0, y0, fw, h, h, h);
                    }
                    g.dispose();
                }
            };
            bar.setOpaque(false);

            JLabel valLbl = new JLabel(formatVnd(entry.getValue()));
            valLbl.setFont(FONT_SMALL);
            valLbl.setForeground(nonZero ? TEXT_MAIN : TEXT_LIGHT);
            valLbl.setPreferredSize(new Dimension(128, 18));

            row.add(monthLbl, BorderLayout.WEST);
            row.add(bar,      BorderLayout.CENTER);
            row.add(valLbl,   BorderLayout.EAST);
            barChartPanel.add(row);
            barChartPanel.add(Box.createVerticalStrut(8));
        }

        barChartPanel.revalidate();
        barChartPanel.repaint();
    }

    private BigDecimal sumAmounts(List<BigDecimal> list) {
        return list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return CURRENCY_FMT.format(amount) + " ₫";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INVOICE DETAIL DIALOG
    // ════════════════════════════════════════════════════════════════════════

    private void showInvoiceDetailDialog(Invoice inv) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog((Frame) owner,
                "Chi tiết Hóa đơn  —  INV-" + String.format("%04d", inv.getInvoiceId()), true);
        dlg.setSize(460, 355);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(owner);

        String st = inv.getStatus() != null ? inv.getStatus() : "";
        Color stColor = switch (st) {
            case "Đã thanh toán"  -> GREEN;
            case "Chờ thanh toán" -> AMBER;
            case "Đã hủy"         -> RED;
            default               -> TEXT_MUTED;
        };
        Color stBg = switch (st) {
            case "Đã thanh toán"  -> GREEN_BG;
            case "Chờ thanh toán" -> AMBER_BG;
            case "Đã hủy"         -> RED_BG;
            default               -> new Color(248, 250, 252);
        };

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CARD);

        // ── Header band ───────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(new Color(248, 250, 252));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(14, 22, 14, 22)));

        JLabel lblCode = new JLabel("INV-" + String.format("%04d", inv.getInvoiceId()));
        lblCode.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblCode.setForeground(TEXT_MAIN);

        JLabel lblSt = new JLabel(st.isEmpty() ? "—" : st);
        lblSt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSt.setForeground(stColor);
        lblSt.setOpaque(true);
        lblSt.setBackground(stBg);
        lblSt.setBorder(new EmptyBorder(4, 12, 4, 12));
        header.add(lblCode, BorderLayout.WEST);
        header.add(lblSt,   BorderLayout.EAST);

        // ── Body ─────────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG_CARD);
        body.setBorder(new EmptyBorder(18, 22, 12, 22));

        String student   = inv.getStudent() != null ? inv.getStudent().getFullName() : "—";
        String issueDate = inv.getIssueDate() != null
                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        String createdAt = inv.getCreatedAt() != null
                ? inv.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        String note = inv.getNote() != null && !inv.getNote().isBlank()
                ? inv.getNote().replace(";", "\n") : "—";

        addDlgRow(body, 0, "Học viên",        student,                        TEXT_MAIN);
        addDlgRow(body, 1, "Ngày phát hành",  issueDate,                      TEXT_MAIN);
        addDlgRow(body, 2, "Tổng tiền",       formatVnd(inv.getTotalAmount()), PRIMARY);
        addDlgRow(body, 3, "Ngày tạo",        createdAt,                      TEXT_MUTED);

        GridBagConstraints lc = dlgGbc(0, 4); lc.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints vc = dlgGbc(1, 4);
        JTextArea noteArea = new JTextArea(note, 3, 0);
        noteArea.setFont(FONT_MAIN);
        noteArea.setForeground(TEXT_MAIN);
        noteArea.setBackground(BG_CARD);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setBorder(null);
        body.add(dlgLabel("Ghi chú", TEXT_MUTED, false), lc);
        body.add(noteArea, vc);

        // ── Footer ────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        JButton btnClose = new JButton("   Đóng   ");
        btnClose.setFont(FONT_BOLD);
        btnClose.setBackground(BG_CARD);
        btnClose.setForeground(TEXT_MAIN);
        btnClose.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void addDlgRow(JPanel grid, int row, String label, String value, Color valColor) {
        grid.add(dlgLabel(label, TEXT_MUTED, false), dlgGbc(0, row));
        grid.add(dlgLabel(value, valColor,   true),  dlgGbc(1, row));
    }

    private GridBagConstraints dlgGbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx  = x; c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, x == 1 ? 10 : 0, 5, 0);
        if (x == 1) { c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; }
        return c;
    }

    private JLabel dlgLabel(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bold ? FONT_BOLD : FONT_MAIN);
        lbl.setForeground(color);
        return lbl;
    }
}

