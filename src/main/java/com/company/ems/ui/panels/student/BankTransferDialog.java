package com.company.ems.ui.panels.student;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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

    private static final Color C_BLUE   = new Color( 37,  99, 235);
    private static final Color C_NAVY   = new Color( 30,  41,  59);
    private static final Color C_GREEN  = new Color( 22, 163,  74);
    private static final Color C_RED    = new Color(220,  38,  38);
    private static final Color C_MUTED  = new Color(100, 116, 139);
    private static final Color C_BORDER = new Color(226, 232, 240);
    private static final Color C_ROW_A  = new Color(248, 250, 252);

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
        super(owner, "Chuy\u1EC3n kho\u1EA3n ng\u00E2n h\u00E0ng", ModalityType.APPLICATION_MODAL);
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
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // Accent top bar
        JPanel topAccent = new JPanel();
        topAccent.setBackground(C_BLUE);
        topAccent.setPreferredSize(new Dimension(0, 5));
        root.add(topAccent, BorderLayout.NORTH);

        // ── Centre wrapper so everything is horizontally centred
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setBackground(Color.WHITE);
        centre.setBorder(new EmptyBorder(18, 28, 8, 28));
        root.add(centre, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        // fixed width so the info table never floats right
        body.setMaximumSize(new Dimension(416, Integer.MAX_VALUE));
        body.setPreferredSize(new Dimension(416, 0));
        centre.add(body);

        // Title
        JLabel lblTitle = new JLabel("Chuy\u1EC3n kho\u1EA3n ng\u00E2n h\u00E0ng", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(C_NAVY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        body.add(lblTitle);

        JLabel lblSub = new JLabel("Qu\u00E9t m\u00E3 QR b\u1EB1ng app ng\u00E2n h\u00E0ng b\u1EA5t k\u1EF3", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(C_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        body.add(lblSub);
        body.add(Box.createVerticalStrut(14));

        // QR card – centred via wrapper
        JPanel qrWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qrWrap.setBackground(Color.WHITE);
        qrWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        qrWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel qrCard = new JPanel(new BorderLayout());
        qrCard.setBackground(Color.WHITE);
        qrCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)));
        qrCard.setPreferredSize(new Dimension(276, 276));

        qrLabel = new JLabel("\u0110ang t\u1EA3i m\u00E3 QR...", SwingConstants.CENTER);
        qrLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        qrLabel.setForeground(C_MUTED);
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
        infoPanel.setBorder(new LineBorder(C_BORDER, 1, true));

        addRow(infoPanel, "Ng\u00E2n h\u00E0ng",          BANK_NAME,                          false, C_ROW_A);
        addRow(infoPanel, "Ch\u1EE7 t\u00E0i kho\u1EA3n", ACCOUNT_NAME,                       false, Color.WHITE);
        addRow(infoPanel, "S\u1ED1 t\u00E0i kho\u1EA3n",  ACCOUNT_NO,                         false, C_ROW_A);
        addRow(infoPanel, "S\u1ED1 ti\u1EC1n",             String.format("%,.0f VND", amount),  true,  Color.WHITE);
        addRow(infoPanel, "N\u1ED9i dung CK",              paymentCode,                        false, C_ROW_A);
        ((JLabel) infoPanel.getComponent(9)).setForeground(C_BLUE);
        ((JLabel) infoPanel.getComponent(9)).setFont(new Font("Segoe UI", Font.BOLD, 13));

        body.add(infoPanel);
        body.add(Box.createVerticalStrut(12));

        // Status
        lblStatus = new JLabel("\u23F3  \u0110ang t\u1EA3i m\u00E3 QR...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(C_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        body.add(lblStatus);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, C_BORDER));

        JButton btnCancel = new JButton("H\u1EE7y thanh to\u00E1n");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setBackground(new Color(220, 38, 38));
        btnCancel.setOpaque(true);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setBorder(new EmptyBorder(9, 28, 9, 28));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> { stopAll(); dispose(); });
        footer.add(btnCancel);

        root.add(footer, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, String label, String value, boolean redValue, Color bg) {
        JLabel lbl = new JLabel("  " + label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(C_MUTED);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(new EmptyBorder(9, 8, 9, 4));
        p.add(lbl);

        JLabel val = new JLabel(value + "  ");
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(redValue ? C_RED : C_NAVY);
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
                            lblStatus.setText("\u23F3  \u0110ang \u0111\u1EE3i x\u00E1c nh\u1EADn giao d\u1ECBch...");
                            startCountdown();
                        });
                        return;
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    qrLabel.setText("<html><center>Kh\u00F4ng t\u1EA3i \u0111\u01B0\u1EE3c QR.</center></html>");
                    startCountdown();
                });
            } catch (Exception ignored) {
                SwingUtilities.invokeLater(() -> {
                    qrLabel.setText("<html><center>Kh\u00F4ng t\u1EA3i \u0111\u01B0\u1EE3c QR.</center></html>");
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
            lblStatus.setForeground(C_GREEN);
            lblStatus.setText("\u2705  Giao d\u1ECBch th\u00E0nh c\u00F4ng!");
        });
        javax.swing.Timer t = new javax.swing.Timer(1200, e -> {
            dispose();
            if (onConfirmed != null) onConfirmed.run();
        });
        t.setRepeats(false);
        t.start();
    }

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
