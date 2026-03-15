package com.company.ems.ui.panels.admin_staff;

import com.company.ems.model.Invoice;
import com.company.ems.model.Payment;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.PaymentService;
import com.company.ems.stream.InvoiceStreamQueries;
import com.company.ems.stream.PaymentStreamQueries;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
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

/**
 * Dashboard doanh thu dành riêng cho Admin.
 */
public class RevenueDashboardPanel extends JPanel {

    private static final String[] INV_COLS = {
            "#", "Mã HV", "Học viên", "Tổng tiền", "Ngày phát hành", "Trạng thái", "Ghi chú"
    };
    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    // ── UI state ────────────────────────────────────────────────────���─────
    private JLabel            lblTotalRevenue, lblPaid, lblPending, lblTxCount;
    private JLabel            lblInvSubCount;
    private DefaultTableModel invModel;
    private JTable            invTable;
    private JPanel            barChartPanel;
    private List<Invoice>     displayedInvoices = new ArrayList<>();
    private TableRowSorter<DefaultTableModel> invSorter;
    private JTextField        invSearchField;

    public RevenueDashboardPanel(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);

        JPanel inner = new JPanel(new BorderLayout(0, 18));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));
        inner.add(buildKpiRow(),     BorderLayout.NORTH);
        inner.add(buildCenterArea(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Theme.BG_PAGE);
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

        row.add(buildKpiCard("TỔNG ĐÃ THU",    "Thanh toán thành công",   lblTotalRevenue, Theme.PRIMARY));
        row.add(buildKpiCard("HOÁ ĐƠN ĐÃ TT",  "Tổng tiền đã thanh toán", lblPaid,         Theme.GREEN));
        row.add(buildKpiCard("HOÁ ĐƠN CHỜ TT", "Cần xử lý",               lblPending,      Theme.AMBER));
        row.add(buildKpiCard("SỐ GIAO DỊCH",   "Giao dịch thành công",    lblTxCount,      Theme.PURPLE));
        return row;
    }

    /**
     * KPI card có top accent band (4 px MatteBorder) + title + value + subtitle.
     * Tương tự ComponentFactory.kpiCard() nhưng có thêm subtitle và top band.
     */
    private JPanel buildKpiCard(String title, String subtitle, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 0, 0, 0, accent),
                        new EmptyBorder(14, 20, 16, 20))));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_KPI_LBL);
        titleLbl.setForeground(Theme.TEXT_MUTED);
        titleLbl.setBorder(new EmptyBorder(0, 0, 6, 0));

        valueLabel.setFont(Theme.FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(Theme.FONT_SMALL);
        subLbl.setForeground(Theme.TEXT_SUB);
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
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(18, 22, 18, 22)));

        JLabel title = new JLabel("Doanh thu 6 tháng gần nhất");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel unit = new JLabel("Đơn vị: VNĐ");
        unit.setFont(Theme.FONT_SMALL);
        unit.setForeground(Theme.TEXT_MUTED);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(unit,  BorderLayout.EAST);

        barChartPanel = new JPanel();
        barChartPanel.setLayout(new BoxLayout(barChartPanel, BoxLayout.Y_AXIS));
        barChartPanel.setBackground(Theme.BG_CARD);

        card.add(titleRow,      BorderLayout.NORTH);
        card.add(barChartPanel, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TABLES AREA
    // ════════════════════════════════════════════════════════════════════════

    private JPanel buildTablesArea() {
        invModel = new DefaultTableModel(INV_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invTable = buildStyledTable(invModel);
        invSorter = new TableRowSorter<>(invModel);
        invTable.setRowSorter(invSorter);
        setColumnWidths(invTable, new int[]{36, 60, 160, 120, 110, 110, 0});
        invTable.getColumnModel().getColumn(3).setCellRenderer(amountRenderer(Theme.PRIMARY));
        invTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer());
        invTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = invTable.getSelectedRow();
                if (row >= 0) {
                    int modelRow = invTable.convertRowIndexToModel(row);
                    if (modelRow >= 0 && modelRow < displayedInvoices.size())
                        showInvoiceDetailDialog(displayedInvoices.get(modelRow));
                }
            }
        });
        invTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblInvSubCount = new JLabel("");
        lblInvSubCount.setFont(Theme.FONT_SMALL);
        lblInvSubCount.setForeground(Theme.TEXT_MUTED);

        invSearchField = buildSearchField(this::filterInv);

        JPanel area = new JPanel(new BorderLayout());
        area.setOpaque(false);
        area.add(buildTableCard("Lịch sử hoá đơn", lblInvSubCount, invTable, invSearchField));
        return area;
    }

    private JTextField buildSearchField(Runnable filter) {
        JTextField tf = ComponentFactory.searchField("Tìm kiếm...");
        tf.setPreferredSize(new Dimension(155, 26));
        tf.setFont(Theme.FONT_SMALL);
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filter.run(); }
            public void removeUpdate(DocumentEvent e)  { filter.run(); }
            public void changedUpdate(DocumentEvent e) { filter.run(); }
        });
        return tf;
    }

    private void filterInv() {
        String q = invSearchField.getText().trim();
        invSorter.setRowFilter(q.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
    }

    private JPanel buildTableCard(String title, JLabel badge, JTable table, JTextField searchField) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel hdr = new JPanel(new BorderLayout(10, 0));
        hdr.setBackground(Theme.BG_HEADER);
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 16, 8, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_SECTION);
        titleLbl.setForeground(Theme.TEXT_MAIN);
        hdr.add(titleLbl, BorderLayout.WEST);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        east.setOpaque(false);
        east.add(searchField);
        east.add(badge);
        hdr.add(east, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_CARD);

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
                    c.setBackground(row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                    c.setForeground(Theme.TEXT_MAIN);
                }
                return c;
            }
        };
        tbl.setFont(Theme.FONT_PLAIN);
        tbl.setRowHeight(34);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(Theme.ROW_SELECT);
        tbl.setSelectionForeground(Theme.TEXT_MAIN);
        tbl.setFillsViewportHeight(true);

        JTableHeader hdr = tbl.getTableHeader();
        hdr.setFont(Theme.FONT_BOLD);
        hdr.setBackground(Theme.BG_HEADER);
        hdr.setForeground(Theme.TEXT_MUTED);
        hdr.setPreferredSize(new Dimension(0, 38));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
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
                            lbl.setBackground(Theme.GREEN_BORDER); lbl.setForeground(Theme.GREEN); break;
                        case "Chờ thanh toán":
                            lbl.setBackground(Theme.AMBER_BORDER); lbl.setForeground(Theme.AMBER); break;
                        case "Đã hủy": case "Hủy":
                            lbl.setBackground(new Color(254, 202, 202)); lbl.setForeground(Theme.RED); break;
                        default:
                            lbl.setBackground(row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
                            lbl.setForeground(Theme.TEXT_MUTED);
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
                    lbl.setBackground(row % 2 == 0 ? Theme.ROW_EVEN : Theme.ROW_ODD);
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
        // ── KPI ───────────────────────────────────────────────────────────
        PaymentStreamQueries.RevenueKpi kpi = PaymentStreamQueries.buildRevenueKpi(payments);
        BigDecimal paidInvTotal = InvoiceStreamQueries.sumPendingDebt(
                InvoiceStreamQueries.filterByStatus(invoices, "Đã thanh toán"), "Đã thanh toán");
        BigDecimal pendingInv = InvoiceStreamQueries.sumPendingDebt(invoices, "Chờ thanh toán");

        lblTotalRevenue.setText(formatVnd(kpi.totalRevenue()));
        lblPaid.setText(formatVnd(paidInvTotal));
        lblPending.setText(formatVnd(pendingInv));
        lblTxCount.setText(kpi.txCount() + " giao dịch");

        rebuildBarChart(payments);

        // ── Bảng hoá đơn ──────────────────────────────────────────────────
        invModel.setRowCount(0);
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Invoice> sortedInv = InvoiceStreamQueries.sortByIssueDateDesc(invoices).stream()
                .limit(50).collect(Collectors.toList());
        displayedInvoices = new ArrayList<>(sortedInv);
        int j = 1;
        for (Invoice inv : sortedInv) {
            String name = inv.getStudent() != null ? inv.getStudent().getFullName() : "—";
            String sid  = inv.getStudent() != null
                    ? String.format("HV%04d", inv.getStudent().getStudentId()) : "—";
            String date = inv.getIssueDate() != null ? inv.getIssueDate().format(dFmt) : "—";
            invModel.addRow(new Object[]{
                    j++, sid, name, formatVnd(inv.getTotalAmount()),
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
        LinkedHashMap<String, BigDecimal> monthly = PaymentStreamQueries.revenueByMonth(payments, 6);

        BigDecimal maxVal = monthly.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (maxVal.compareTo(BigDecimal.ZERO) == 0) maxVal = BigDecimal.ONE;
        final BigDecimal finalMax = maxVal;

        barChartPanel.removeAll();
        barChartPanel.add(Box.createVerticalStrut(8));

        for (Map.Entry<String, BigDecimal> entry : monthly.entrySet()) {
            final double  ratio   = entry.getValue().doubleValue() / finalMax.doubleValue();
            final boolean nonZero = entry.getValue().compareTo(BigDecimal.ZERO) > 0;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel monthLbl = new JLabel(entry.getKey());
            monthLbl.setFont(Theme.FONT_SMALL);
            monthLbl.setForeground(Theme.TEXT_MUTED);
            monthLbl.setPreferredSize(new Dimension(62, 18));
            monthLbl.setHorizontalAlignment(SwingConstants.RIGHT);

            // Custom-drawn track + filled bar (rounded)
            JPanel bar = new JPanel() {
                @Override protected void paintComponent(Graphics g0) {
                    super.paintComponent(g0);
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int h  = 10;
                    int y0 = (getHeight() - h) / 2;
                    int tw = getWidth();
                    int fw = (int) (tw * ratio);
                    g.setColor(Theme.BORDER);
                    g.fillRoundRect(0, y0, tw, h, h, h);
                    if (fw > 2) {
                        g.setColor(nonZero ? Theme.PRIMARY : new Color(203, 213, 225));
                        g.fillRoundRect(0, y0, fw, h, h, h);
                    }
                    g.dispose();
                }
            };
            bar.setOpaque(false);

            JLabel valLbl = new JLabel(formatVnd(entry.getValue()));
            valLbl.setFont(Theme.FONT_SMALL);
            valLbl.setForeground(nonZero ? Theme.TEXT_MAIN : Theme.TEXT_MUTED);
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
            case "Đã thanh toán"  -> Theme.GREEN;
            case "Chờ thanh toán" -> Theme.AMBER;
            case "Đã hủy"         -> Theme.RED;
            default               -> Theme.TEXT_MUTED;
        };
        Color stBg = switch (st) {
            case "Đã thanh toán"  -> Theme.GREEN_BORDER;
            case "Chờ thanh toán" -> Theme.AMBER_BORDER;
            case "Đã hủy"         -> new Color(254, 202, 202);
            default               -> Theme.BG_HEADER;
        };

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CARD);

        // ── Header band ───────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(14, 22, 14, 22)));

        JLabel lblCode = new JLabel("INV-" + String.format("%04d", inv.getInvoiceId()));
        lblCode.setFont(Theme.FONT_TITLE);
        lblCode.setForeground(Theme.TEXT_MAIN);

        JLabel lblSt = new JLabel(st.isEmpty() ? "—" : st);
        lblSt.setFont(Theme.FONT_BADGE);
        lblSt.setForeground(stColor);
        lblSt.setOpaque(true);
        lblSt.setBackground(stBg);
        lblSt.setBorder(new EmptyBorder(4, 12, 4, 12));
        header.add(lblCode, BorderLayout.WEST);
        header.add(lblSt,   BorderLayout.EAST);

        // ── Body ──────────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Theme.BG_CARD);
        body.setBorder(new EmptyBorder(18, 22, 12, 22));

        String student   = inv.getStudent() != null ? inv.getStudent().getFullName() : "—";
        String issueDate = inv.getIssueDate() != null
                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        String createdAt = inv.getCreatedAt() != null
                ? inv.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        String note = inv.getNote() != null && !inv.getNote().isBlank()
                ? inv.getNote().replace(";", "\n") : "—";

        addDlgRow(body, 0, "Học viên",       student,                        Theme.TEXT_MAIN);
        addDlgRow(body, 1, "Ngày phát hành", issueDate,                      Theme.TEXT_MAIN);
        addDlgRow(body, 2, "Tổng tiền",      formatVnd(inv.getTotalAmount()), Theme.PRIMARY);
        addDlgRow(body, 3, "Ngày tạo",       createdAt,                      Theme.TEXT_MUTED);

        GridBagConstraints lc = dlgGbc(0, 4); lc.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints vc = dlgGbc(1, 4);
        JTextArea noteArea = new JTextArea(note, 3, 0);
        noteArea.setFont(Theme.FONT_PLAIN);
        noteArea.setForeground(Theme.TEXT_MAIN);
        noteArea.setBackground(Theme.BG_CARD);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setBorder(null);
        body.add(dlgLabel("Ghi chú", Theme.TEXT_MUTED, false), lc);
        body.add(noteArea, vc);

        // ── Footer ────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(Theme.BG_HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        JButton btnClose = ComponentFactory.secondaryButton("   Đóng   ");
        btnClose.addActionListener(e -> dlg.dispose());
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private void addDlgRow(JPanel grid, int row, String label, String value, Color valColor) {
        grid.add(dlgLabel(label, Theme.TEXT_MUTED, false), dlgGbc(0, row));
        grid.add(dlgLabel(value, valColor, true),          dlgGbc(1, row));
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
        lbl.setFont(bold ? Theme.FONT_BOLD : Theme.FONT_PLAIN);
        lbl.setForeground(color);
        return lbl;
    }

    private BigDecimal sumAmounts(List<BigDecimal> list) {
        return PaymentStreamQueries.sumAmounts(list);
    }

    /** Chấp nhận cả "Đã thanh toán" (mới) lẫn "Hoàn thành" / "Completed" (data cũ) */
    private static boolean isPaid(String status) {
        return PaymentStreamQueries.isPaid(status);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return CURRENCY_FMT.format(amount) + " ₫";
    }
}
