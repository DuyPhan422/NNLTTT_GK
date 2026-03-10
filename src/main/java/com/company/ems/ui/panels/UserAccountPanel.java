package com.company.ems.ui.panels;

import com.company.ems.model.UserAccount;
import com.company.ems.service.UserAccountService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Panel quản lý tài khoản hệ thống — chỉ Admin được truy cập.
 * Chức năng: xem danh sách, bật/tắt tài khoản, đổi mật khẩu.
 */
public class UserAccountPanel extends JPanel {

    // ── Design tokens ─────────────────────────────────────────────────────
    private static final Color BG_PAGE       = new Color(248, 250, 252);
    private static final Color BG_CARD       = Color.WHITE;
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);
    private static final Color PRIMARY       = new Color(37,  99,  235);
    private static final Color PRIMARY_HOVER = new Color(29,  78,  216);
    private static final Color AMBER         = new Color(217, 119, 6);
    private static final Color TEXT_MAIN     = new Color(15,  23,  42);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color ROW_EVEN      = Color.WHITE;
    private static final Color ROW_ODD       = new Color(248, 250, 252);
    private static final Color ROW_SELECT    = new Color(219, 234, 254);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private static final String[] COLUMNS = {
        "ID", "STT", "Tên đăng nhập", "Vai trò", "Liên kết với", "Trạng thái", "Ngày tạo"
    };

    // ── Dependencies ──────────────────────────────────────────────────────
    private final UserAccountService userAccountService;

    // ── UI ────────────────────────────────────────────────────────────────
    private final DefaultTableModel             tableModel;
    private final JTable                        table;
    private final JLabel                        statusLabel;
    private final JTextField                    searchField;
    private final JComboBox<String>             filterRole;
    private       TableRowSorter<DefaultTableModel> sorter;
    private       Runnable                      onDataChanged;

    private List<UserAccount> allAccounts;

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public UserAccountPanel(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
        this.tableModel   = buildTableModel();
        this.table        = buildTable();
        this.statusLabel  = new JLabel();
        this.searchField  = new JTextField();
        this.filterRole   = new JComboBox<>(new String[]{
            "Tất cả", "Quản trị", "Giáo viên", "Học viên", "Nhân viên"
        });

        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
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

        searchField.setPreferredSize(new Dimension(280, 38));
        searchField.setFont(FONT_MAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText",
                "Tìm tên đăng nhập, tên người, vai trò...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        filterRole.setFont(FONT_MAIN);
        filterRole.setPreferredSize(new Dimension(150, 38));
        filterRole.addActionListener(e -> applyFilters());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(searchField);
        leftPanel.add(Box.createHorizontalStrut(12));
        leftPanel.add(new JLabel("Vai trò: "));
        leftPanel.add(filterRole);

        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);

        toolbar.add(leftPanel,   BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTableCard() {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder()));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton btnToggle = createBtn("⏸  Bật / Tắt tài khoản", AMBER, new Color(180, 90, 0));
        JButton btnPwd    = createBtn("🔑  Đặt lại mật khẩu",   PRIMARY, PRIMARY_HOVER);

        btnToggle.addActionListener(e -> toggleSelectedAccount());
        btnPwd.addActionListener(e -> resetPasswordForSelected());

        bar.add(btnToggle);
        bar.add(btnPwd);
        return bar;
    }

    private JButton createBtn(String text, Color bg, Color hover) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 36));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(bg);    }
        });
        return btn;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Long.class : String.class; }
        };
    }

    private JTable buildTable() {
        JTable tbl = new JTable(tableModel) {
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
        tbl.setRowHeight(36);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(ROW_SELECT);
        tbl.setSelectionForeground(TEXT_MAIN);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.setFillsViewportHeight(true);

        JTableHeader hdr = tbl.getTableHeader();
        hdr.setFont(FONT_BOLD);
        hdr.setBackground(new Color(241, 245, 249));
        hdr.setForeground(TEXT_MUTED);
        hdr.setPreferredSize(new Dimension(0, 40));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Ẩn cột ID
        tbl.getColumnModel().getColumn(0).setMinWidth(0);
        tbl.getColumnModel().getColumn(0).setMaxWidth(0);
        tbl.getColumnModel().getColumn(0).setWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        tbl.setRowSorter(sorter);
        return tbl;
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        SwingWorker<List<UserAccount>, Void> worker = new SwingWorker<>() {
            @Override protected List<UserAccount> doInBackground() {
                return userAccountService.findAll();
            }
            @Override protected void done() {
                try {
                    allAccounts = get();
                    populateTable(allAccounts);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<UserAccount> accounts) {
        tableModel.setRowCount(0);
        int stt = 1;
        for (UserAccount ua : accounts) {
            String linkedTo  = resolveLinkedName(ua);
            String status    = Boolean.TRUE.equals(ua.getIsActive()) ? "Hoạt động" : "Bị khóa";
            String createdAt = ua.getCreatedAt() != null
                    ? ua.getCreatedAt().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "—";
            tableModel.addRow(new Object[]{
                ua.getUserId(),
                stt++,
                ua.getUsername(),
                ua.getRole(),
                linkedTo,
                status,
                createdAt
            });
        }
        statusLabel.setText("Tổng: " + accounts.size() + " tài khoản");
        applyFilters();
    }

    private String resolveLinkedName(UserAccount ua) {
        try {
            if (ua.getTeacher() != null) return "GV: " + ua.getTeacher().getFullName();
            if (ua.getStudent() != null) return "HV: " + ua.getStudent().getFullName();
            if (ua.getStaff()   != null) return "NV: " + ua.getStaff().getFullName();
        } catch (Exception ignored) {}
        return "— (Admin)";
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String role    = (String) filterRole.getSelectedItem();

        java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();

        if (!keyword.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(keyword), 2, 3, 4));
        }
        if (role != null && !"Tất cả".equals(role)) {
            filters.add(RowFilter.regexFilter("(?i)^" + java.util.regex.Pattern.quote(role) + "$", 3));
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        statusLabel.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " tài khoản");
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private UserAccount getSelectedAccount() {
        if (allAccounts == null) {
            JOptionPane.showMessageDialog(this, "Dữ liệu chưa sẵn sàng. Vui lòng thử lại.",
                    "Chú ý", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản.",
                    "Chú ý", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        return allAccounts.stream().filter(a -> a.getUserId().equals(id)).findFirst().orElse(null);
    }

    private void toggleSelectedAccount() {
        UserAccount ua = getSelectedAccount();
        if (ua == null) return;

        // Không cho khoá tài khoản admin đang chạy (không tracking session ở đây,
        // nên chỉ cảnh báo nếu role = Quản trị)
        if ("Quản trị".equals(ua.getRole())) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Đây là tài khoản Quản trị. Bạn vẫn muốn thay đổi trạng thái?",
                    "Cảnh báo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        boolean newStatus = !Boolean.TRUE.equals(ua.getIsActive());
        ua.setIsActive(newStatus);
        try {
            userAccountService.update(ua);
            loadData();
            if (onDataChanged != null) onDataChanged.run();
            JOptionPane.showMessageDialog(this,
                    "Tài khoản \"" + ua.getUsername() + "\" đã được "
                    + (newStatus ? "kích hoạt" : "khóa") + ".",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi cập nhật: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ua.setIsActive(!newStatus); // rollback local
        }
    }

    private void resetPasswordForSelected() {
        UserAccount ua = getSelectedAccount();
        if (ua == null) return;

        JPasswordField pwdField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.add(new JLabel("Mật khẩu mới cho \"" + ua.getUsername() + "\":"));
        form.add(pwdField);
        form.add(new JLabel("Xác nhận mật khẩu:"));
        form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Đặt lại mật khẩu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newPwd     = new String(pwdField.getPassword());
        String confirmPwd = new String(confirmField.getPassword());

        if (newPwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không được để trống.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPwd.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ua.setPasswordHash(UserAccountService.hashPassword(newPwd));
        try {
            userAccountService.update(ua);
            JOptionPane.showMessageDialog(this,
                    "Đã đặt lại mật khẩu cho \"" + ua.getUsername() + "\" thành công.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi đổi mật khẩu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
