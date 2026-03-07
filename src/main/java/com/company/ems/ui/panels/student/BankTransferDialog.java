package com.company.ems.ui.panels.student;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Dialog chuyển khoản tự động:
 *  1. Hiển thị QR VietQR của tài khoản HDBank
 *  2. Poll Gmail mỗi 5 giây — khi HDBank gửi email thông báo giao dịch
 *     chứa mã HVMS..., hệ thống tự xác nhận và ghi hóa đơn.
 *
 * ĐIỀU KIỆN: Bật "Thông báo giao dịch qua email" trong app HDBank
 *             (Cài đặt → Thông báo → Email giao dịch → nhập phanngocduypnd@gmail.com)
 *
 * Nếu muốn dùng Casso thay Gmail: đăng ký tại casso.vn → lấy API key
 * → điền vào CASSO_API_KEY (Casso sẽ được ưu tiên hơn Gmail).
 */
public class BankTransferDialog extends JDialog {

    // ─── Tài khoản nhận tiền (HDBank) ────────────────────────────────────
    private static final String BANK_ID       = "HDB";
    private static final String BANK_NAME     = "HDBank";
    private static final String ACCOUNT_NO    = "140704070008650";
    private static final String ACCOUNT_NAME  = "PHAN NGOC DUY";

    // ─── Gmail nhận email thông báo từ HDBank ─────────────────────────────
    // Điều kiện: kích hoạt "Thông báo giao dịch qua email" trong app HDBank
    private static final String GMAIL_USER     = "phanngocduypnd@gmail.com";
    private static final String GMAIL_APP_PASS = "myepzdbjecmlvyha";

    // ─── Casso (để trống = dùng Gmail; điền = dùng Casso thay thế) ────────
    private static final String CASSO_API_KEY  = "";

    // ─── Màu sắc ──────────────────────────────────────────────────────────
    private static final Color C_BLUE   = new Color(37,  99,  235);
    private static final Color C_NAVY   = new Color(30,  41,  59);
    private static final Color C_GREEN  = new Color(22,  163, 74);
    private static final Color C_RED    = new Color(220, 38,  38);
    private static final Color C_MUTED  = new Color(100, 116, 139);
    private static final Color C_BORDER = new Color(226, 232, 240);

    // ─── State ────────────────────────────────────────────────────────────
    private final BigDecimal       amount;
    private final String           paymentCode;
    private final Runnable         onConfirmed;
    private       javax.swing.Timer pollTimer;
    private       JLabel           lblStatus;
    private       JLabel           qrLabel;
    private volatile boolean       confirmed = false;

    /**
     * @param owner       cửa sổ cha
     * @param amount      số tiền cần thanh toán
     * @param paymentCode mã duy nhất ghi vào nội dung chuyển khoản (vd: HVMS3042831)
     * @param onConfirmed callback sau khi phát hiện giao dịch thành công
     */
    public BankTransferDialog(Window owner, BigDecimal amount,
                               String paymentCode, Runnable onConfirmed) {
        super(owner, "Chuyển khoản ngân hàng", ModalityType.APPLICATION_MODAL);
        this.amount      = amount;
        this.paymentCode = paymentCode;
        this.onConfirmed = onConfirmed;

        buildUI();
        loadQRAsync();
        startPolling();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { stopPolling(); }
        });
        setSize(460, 570);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(C_NAVY);
        hdr.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel lblTitle = new JLabel("CHUYỂN KHOẢN NGÂN HÀNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(Color.WHITE);
        hdr.add(lblTitle, BorderLayout.WEST);
        root.add(hdr, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(16, 28, 8, 28));

        // QR placeholder
        qrLabel = new JLabel("Đang tải mã QR...", SwingConstants.CENTER);
        qrLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        qrLabel.setForeground(C_MUTED);
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        qrLabel.setPreferredSize(new Dimension(220, 220));
        qrLabel.setMaximumSize(new Dimension(220, 220));
        qrLabel.setBorder(new LineBorder(C_BORDER, 1));
        body.add(qrLabel);
        body.add(Box.createVerticalStrut(18));

        // Thông tin tài khoản
        JPanel info = new JPanel(new GridLayout(5, 2, 6, 10));
        info.setOpaque(false);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 185));
        addInfoRow(info, "Ngân hàng",    BANK_NAME,    false, false);
        addInfoRow(info, "Số tài khoản", ACCOUNT_NO,   false, false);
        addInfoRow(info, "Tên tài khoản", ACCOUNT_NAME, false, false);
        addInfoRow(info, "Số tiền",      String.format("%,.0f VND", amount), false, true);
        addInfoRow(info, "Nội dung CK",  paymentCode,  true,  false);
        body.add(info);

        body.add(Box.createVerticalStrut(14));

        // Trạng thái tự động
        lblStatus = new JLabel("⏳  Đang tự động theo dõi giao dịch...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(C_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(lblStatus);

        root.add(body, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(0, 8, 2, 8));

        JButton btnManual = new JButton("Xác nhận thủ công");
        btnManual.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnManual.setForeground(C_MUTED);
        btnManual.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(C_BORDER, 1),
                new EmptyBorder(5, 12, 5, 12)));
        btnManual.setFocusPainted(false);
        btnManual.setToolTipText("Dùng khi hệ thống tự động chưa phát hiện");
        btnManual.addActionListener(e -> confirmAndClose());

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> { stopPolling(); dispose(); });

        footer.add(btnManual);
        footer.add(btnCancel);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void addInfoRow(JPanel p, String label, String value,
                             boolean highlight, boolean redValue) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(C_MUTED);
        p.add(lbl);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(highlight ? C_BLUE : (redValue ? C_RED : C_NAVY));
        p.add(val);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tải QR VietQR (bất đồng bộ)
    // ─────────────────────────────────────────────────────────────────────
    private void loadQRAsync() {
        new Thread(() -> {
            try {
                String note   = URLEncoder.encode(paymentCode, StandardCharsets.UTF_8);
                String name   = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
                String urlStr = String.format(
                        "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                        BANK_ID, ACCOUNT_NO, amount.longValue(), note, name);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    if (img != null) {
                        Image scaled = img.getScaledInstance(210, 210, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> {
                            qrLabel.setIcon(new ImageIcon(scaled));
                            qrLabel.setText(null);
                        });
                        return;
                    }
                }
                SwingUtilities.invokeLater(() -> qrLabel.setText("<html><center>Không tải được QR.<br>Nhập thủ công thông tin bên dưới.</center></html>"));
            } catch (Exception ignored) {
                SwingUtilities.invokeLater(() -> qrLabel.setText("<html><center>Không tải được QR.<br>Nhập thủ công thông tin bên dưới.</center></html>"));
            }
        }, "qr-loader").start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Polling – ưu tiên Casso nếu có API key, còn lại dùng Gmail IMAP
    // ─────────────────────────────────────────────────────────────────────
    private void startPolling() {
        pollTimer = new javax.swing.Timer(5000, e -> {
            if (!CASSO_API_KEY.isEmpty()) checkCasso();
            else checkGmail();
        });
        pollTimer.setInitialDelay(4000);
        pollTimer.start();
    }

    private void stopPolling() {
        if (pollTimer != null) { pollTimer.stop(); pollTimer = null; }
    }

    // ─── Casso (nếu có API key) ───────────────────────────────────────────
    private void checkCasso() {
        new Thread(() -> {
            try {
                URL u = new URL("https://oauth.casso.vn/v2/transactions?page=1&pageSize=20");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Authorization", "Apikey " + CASSO_API_KEY);
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                if (c.getResponseCode() != 200) return;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                if (sb.toString().contains(paymentCode)) {
                    SwingUtilities.invokeLater(this::confirmAndClose);
                }
            } catch (Exception ignored) {}
        }, "casso-poll").start();
    }

    // ─── Gmail IMAP ────────────────────────────────────────────────────────
    // HDBank gửi email thông báo khi có giao dịch → email body chứa nội dung CK
    // → phát hiện paymentCode → xác nhận tự động
    private void checkGmail() {
        new Thread(() -> {
            Store  store = null;
            Folder inbox = null;
            try {
                Properties props = new Properties();
                props.put("mail.imaps.host", "imap.gmail.com");
                props.put("mail.imaps.port", "993");
                props.put("mail.imaps.ssl.enable", "true");
                props.put("mail.imaps.connectiontimeout", "5000");
                props.put("mail.imaps.timeout", "5000");

                Session session = Session.getInstance(props);
                store = session.getStore("imaps");
                store.connect("imap.gmail.com", GMAIL_USER, GMAIL_APP_PASS);

                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                int total = inbox.getMessageCount();
                if (total == 0) return;
                // Chỉ đọc 15 email gần nhất để nhanh
                int from = Math.max(1, total - 14);
                Message[] msgs = inbox.getMessages(from, total);

                for (Message msg : msgs) {
                    // Bỏ qua email cũ hơn 20 phút
                    if (msg.getSentDate() != null) {
                        long ageMs = System.currentTimeMillis() - msg.getSentDate().getTime();
                        if (ageMs > 20 * 60 * 1000L) continue;
                    }
                    String body = extractText(msg);
                    if (body != null && body.contains(paymentCode)) {
                        SwingUtilities.invokeLater(this::confirmAndClose);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Kết nối fail → thử lại lần poll tiếp theo
            } finally {
                try { if (inbox != null && inbox.isOpen()) inbox.close(false); } catch (Exception ignored) {}
                try { if (store  != null) store.close(); } catch (Exception ignored) {}
            }
        }, "gmail-poll").start();
    }

    /** Trích nội dung text từ message (hỗ trợ plain text và multipart). */
    private String extractText(Message msg) {
        try {
            Object content = msg.getContent();
            if (content instanceof String s) return s;
            if (content instanceof MimeMultipart mp) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                        Object pc = part.getContent();
                        sb.append(pc instanceof String ? (String) pc : pc.toString());
                    }
                }
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Xác nhận
    // ─────────────────────────────────────────────────────────────────────
    private void confirmAndClose() {
        if (confirmed) return;
        confirmed = true;
        stopPolling();
        lblStatus.setForeground(C_GREEN);
        lblStatus.setText("✅  Giao dịch xác nhận thành công!");
        javax.swing.Timer t = new javax.swing.Timer(1200, e -> {
            dispose();
            if (onConfirmed != null) onConfirmed.run();
        });
        t.setRepeats(false);
        t.start();
    }
}
