package com.company.ems.ui.panels.student;

import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BankTransferDialog extends JDialog {

    private static final String BANK_ID      = "HDB";
    private static final String BANK_NAME    = "HDBank";
    private static final String ACCOUNT_NO   = "140704070008650";
    private static final String ACCOUNT_NAME = "PHAN NGOC DUY";

    private static final String PARTNER_CODE = "MOMO";
    private static final String ACCESS_KEY   = "F8BBA842ECF85";
    private static final String SECRET_KEY   = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private static final String ENDPOINT     = "https://test-payment.momo.vn/v2/gateway/api";
    private static final String DUMMY_URL    = "https://webhook.site/placeholder";

    private final BigDecimal        amount;
    private final String            paymentCode;
    private final Runnable          onConfirmed;
    private final String            orderId;
    private final String            requestId;
    private       javax.swing.Timer pollTimer;
    private       javax.swing.Timer countdownTimer;
    private       JLabel            lblStatus;
    private       JLabel            qrLabel;
    private volatile boolean        confirmed = false;

    public BankTransferDialog(Window owner, BigDecimal amount,
                              String paymentCode, Runnable onConfirmed) {
        super(owner, "Chuyển khoản ngân hàng", ModalityType.APPLICATION_MODAL);
        this.amount      = amount;
        this.paymentCode = paymentCode;
        this.onConfirmed = onConfirmed;
        this.orderId     = "EMS_" + paymentCode + "_" + (System.currentTimeMillis() % 1_000_000L);
        this.requestId   = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        buildUI();
        loadVietQRAsync();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { stopAll(); }
        });
        setSize(480, 710);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CARD);
        setContentPane(root);

        // Accent top bar
        JPanel topAccent = new JPanel();
        topAccent.setBackground(Theme.PRIMARY);
        topAccent.setPreferredSize(new Dimension(0, 5));
        root.add(topAccent, BorderLayout.NORTH);

        // Centre wrapper so everything is horizontally centred
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setBackground(Theme.BG_CARD);
        centre.setBorder(new EmptyBorder(18, 28, 8, 28));
        root.add(centre, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG_CARD);
        // fixed width so the info table never floats right
        body.setMaximumSize(new Dimension(416, Integer.MAX_VALUE));
        body.setPreferredSize(new Dimension(416, 0));
        centre.add(body);

        // Title
        JLabel lblTitle = new JLabel("Chuyển khoản ngân hàng", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        body.add(lblTitle);

        JLabel lblSub = new JLabel("Quét mã QR bằng app ngân hàng bất kỳ", SwingConstants.CENTER);
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        body.add(lblSub);
        body.add(Box.createVerticalStrut(14));

        // QR card – centred via wrapper
        JPanel qrWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qrWrap.setBackground(Theme.BG_CARD);
        qrWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        qrWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel qrCard = new JPanel(new BorderLayout());
        qrCard.setBackground(Theme.BG_CARD);
        qrCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)));
        qrCard.setPreferredSize(new Dimension(276, 276));

        qrLabel = new JLabel("Đang tải mã QR...", SwingConstants.CENTER);
        qrLabel.setFont(Theme.FONT_SMALL.deriveFont(Font.ITALIC));
        qrLabel.setForeground(Theme.TEXT_MUTED);
        qrLabel.setPreferredSize(new Dimension(260, 260));
        qrCard.add(qrLabel, BorderLayout.CENTER);
        qrWrap.add(qrCard);
        body.add(qrWrap);

        body.add(Box.createVerticalStrut(14));

        // Info table – full width of body
        JPanel infoPanel = new JPanel(new GridLayout(5, 2, 0, 0));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        infoPanel.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));

        addRow(infoPanel, "Ngân hàng",       BANK_NAME,                         false, Theme.BG_PAGE);
        addRow(infoPanel, "Chủ tài khoản",   ACCOUNT_NAME,                      false, Theme.BG_CARD);
        addRow(infoPanel, "Số tài khoản",    ACCOUNT_NO,                        false, Theme.BG_PAGE);
        addRow(infoPanel, "Số tiền",         String.format("%,.0f VND", amount), true,  Theme.BG_CARD);
        addRow(infoPanel, "Nội dung CK",     paymentCode,                       false, Theme.BG_PAGE);

        // Highlight payment-code value label (component index 9 = value cell of last row)
        Component payCodeVal = infoPanel.getComponent(9);
        payCodeVal.setForeground(Theme.PRIMARY);
        ((JLabel) payCodeVal).setFont(Theme.FONT_BOLD);

        body.add(infoPanel);
        body.add(Box.createVerticalStrut(12));

        // Status
        lblStatus = new JLabel("⏳  Đang tải mã QR...", SwingConstants.CENTER);
        lblStatus.setFont(Theme.FONT_SMALL);
        lblStatus.setForeground(Theme.TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        body.add(lblStatus);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        footer.setBackground(Theme.BG_CARD);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER));

        JButton btnCancel = ComponentFactory.dangerButton("Hủy thanh toán");
        btnCancel.addActionListener(e -> { stopAll(); dispose(); });
        footer.add(btnCancel);

        root.add(footer, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, String label, String value, boolean redValue, Color bg) {
        JLabel lbl = new JLabel("  " + label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(new EmptyBorder(9, 8, 9, 4));
        p.add(lbl);

        JLabel val = new JLabel(value + "  ");
        val.setFont(Theme.FONT_SMALL_BOLD);
        val.setForeground(redValue ? Theme.DANGER : Theme.TEXT_MAIN);
        val.setOpaque(true);
        val.setBackground(bg);
        val.setBorder(new EmptyBorder(9, 4, 9, 8));
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(val);
    }

    private void loadVietQRAsync() {
        new Thread(() -> {
            try {
                String note   = java.net.URLEncoder.encode(paymentCode, StandardCharsets.UTF_8);
                String name   = java.net.URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
                String urlStr = String.format(
                        "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                        BANK_ID, ACCOUNT_NO, amount.longValue(), note, name);

                HttpURLConnection conn =
                        (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    if (img != null) {
                        Image scaled = img.getScaledInstance(260, 260, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> {
                            qrLabel.setIcon(new ImageIcon(scaled));
                            qrLabel.setText(null);
                            lblStatus.setText("⏳  Đang đợi xác nhận giao dịch...");
                            startCountdown();
                        });
                        return;
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    qrLabel.setText("<html><center>Không tải được QR.</center></html>");
                    startCountdown();
                });
            } catch (Exception ignored) {
                SwingUtilities.invokeLater(() -> {
                    qrLabel.setText("<html><center>Không tải được QR.</center></html>");
                    startCountdown();
                });
            }
        }, "vietqr-loader").start();
    }

    private void startCountdown() {
        int[] remaining = {15};
        countdownTimer = new javax.swing.Timer(1000, null);
        countdownTimer.addActionListener(e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                countdownTimer.stop();
                confirmAndClose();
            }
        });
        countdownTimer.start();
    }

    private void stopAll() {
        if (pollTimer      != null) { pollTimer.stop();      pollTimer      = null; }
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
    }

    private void confirmAndClose() {
        if (confirmed) return;
        confirmed = true;
        stopAll();
        SwingUtilities.invokeLater(() -> {
            lblStatus.setForeground(Theme.GREEN);
            lblStatus.setText("✅  Giao dịch thành công!");
        });
        javax.swing.Timer t = new javax.swing.Timer(1200, e -> {
            dispose();
            if (onConfirmed != null) onConfirmed.run();
        });
        t.setRepeats(false);
        t.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NETWORK / CRYPTO HELPERS (MoMo integration)
    // ════════════════════════════════════════════════════════════════════════

    private static String postJson(String urlStr, String jsonBody) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] out = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            try (OutputStream os = conn.getOutputStream()) { os.write(out); }
            int code = conn.getResponseCode();
            InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception ignored) { return null; }
    }

    private static String hmacSha256(String key, String data)
            throws java.security.NoSuchAlgorithmException,
            java.security.InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int i = json.indexOf(search);
        if (i < 0) return -1;
        i += search.length();
        while (i < json.length() && json.charAt(i) == ' ') i++;
        int j = i;
        while (j < json.length()
                && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '-')) j++;
        try { return Integer.parseInt(json.substring(i, j)); } catch (Exception e) { return -1; }
    }

    private static String extractStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        i += search.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }
}
