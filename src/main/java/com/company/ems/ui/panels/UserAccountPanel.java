package com.company.ems.ui.panels;

import com.company.ems.model.UserAccount;
import com.company.ems.service.UserAccountService;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel quản lý tài khoản hệ thống — chỉ Admin được truy cập.
 * Không kế thừa BaseCrudPanel vì action bar đặc thù (Toggle + Reset Password thay vì Sửa/Xóa).
 * SRP: chỉ lo hiển thị + điều phối; business logic ở UserAccountService.
 */
public class UserAccountPanel extends JPanel {

    private static final String[] COLUMNS = {
        "ID", "STT", "Tên đăng nhập", "Vai trò", "Liên kết với", "Trạng thái", "Ngày tạo"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserAccountService        userAccountService;
    private final DefaultTableModel         tableModel;
    private final JTable                    table;
    private final JLabel                    statusLabel;
    private final JTextField                searchField;
    private final JComboBox<String>         filterRole;
    private       TableRowSorter<DefaultTableModel> sorter;
    private       Runnable                  onDataChanged;
    private       List<UserAccount>         allAccounts;

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public UserAccountPanel(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
        this.tableModel  = buildTableModel();
        this.table       = buildTable();
        this.statusLabel = new JLabel();
        this.searchField = new JTextField();
        this.filterRole  = new JComboBox<>(new String[]{
            "Tất cả", "Quản trị", "Giáo viên", "Học viên", "Nhân viên"
        });

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
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
        searchField.setFont(Theme.FONT_PLAIN);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText",
                "Tìm tên đăng nhập, tên người, vai trò...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        filterRole.setFont(Theme.FONT_PLAIN);
        filterRole.setPreferredSize(new Dimension(150, 38));
        filterRole.addActionListener(e -> applyFilters());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(searchField);
        left.add(Box.createHorizontalStrut(12));
        left.add(new JLabel("Vai trò: "));
        left.add(filterRole);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        toolbar.add(left,        BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTableCard() {
        return TableStyler.scrollPane(table);
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Dùng ComponentFactory thay vì tự tạo button thủ công
        JButton btnToggle = ComponentFactory.navButton("⏸  Bật / Tắt tài khoản");
        JButton btnPwd    = ComponentFactory.primaryButton("🔑  Đặt lại mật khẩu");

        btnToggle.addActionListener(e -> toggleSelectedAccount());
        btnPwd.addActionListener(e    -> resetPasswordForSelected());

        bar.add(btnToggle);
        bar.add(btnPwd);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Long.class : String.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        TableStyler.applyDefaults(t);
        TableStyler.hideColumn(t, 0);

        sorter = TableStyler.attachSorter(t, tableModel, 0, 1);
        return t;
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
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private void populateTable(List<UserAccount> accounts) {
        tableModel.setRowCount(0);
        int stt = 1;
        for (UserAccount ua : accounts) {
            tableModel.addRow(new Object[]{
                ua.getUserId(),
                stt++,
                ua.getUsername(),
                ua.getRole(),
                resolveLinkedName(ua),
                Boolean.TRUE.equals(ua.getIsActive()) ? "Hoạt động" : "Bị khóa",
                ua.getCreatedAt() != null
                    ? ua.getCreatedAt().toLocalDate().format(DATE_FMT) : "—"
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

        List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
        if (!keyword.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(keyword), 2, 3, 4));
        if (role != null && !"Tất cả".equals(role))
            filters.add(RowFilter.regexFilter("(?i)^" + java.util.regex.Pattern.quote(role) + "$", 3));

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        statusLabel.setText("Hiển thị " + table.getRowCount() + " / " + tableModel.getRowCount() + " tài khoản");
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private UserAccount getSelectedAccount() {
        if (allAccounts == null) {
            JOptionPane.showMessageDialog(this, "Dữ liệu chưa sẵn sàng.", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản.", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Long id = (Long) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        return allAccounts.stream().filter(a -> a.getUserId().equals(id)).findFirst().orElse(null);
    }

    private void toggleSelectedAccount() {
        UserAccount ua = getSelectedAccount();
        if (ua == null) return;

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
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ua.setIsActive(!newStatus);
        }
    }

    private void resetPasswordForSelected() {
        UserAccount ua = getSelectedAccount();
        if (ua == null) return;

        JPasswordField pwdField     = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.add(new JLabel("Mật khẩu mới cho \"" + ua.getUsername() + "\":"));
        form.add(pwdField);
        form.add(new JLabel("Xác nhận mật khẩu:"));
        form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Đặt lại mật khẩu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newPwd  = new String(pwdField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (newPwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPwd.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPwd.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ua.setPasswordHash(UserAccountService.hashPassword(newPwd));
        try {
            userAccountService.update(ua);
            JOptionPane.showMessageDialog(this,
                    "Đã đặt lại mật khẩu cho \"" + ua.getUsername() + "\" thành công.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi đổi mật khẩu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
