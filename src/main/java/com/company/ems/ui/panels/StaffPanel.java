package com.company.ems.ui.panels;

import com.company.ems.model.Staff;
import com.company.ems.service.StaffService;
import com.company.ems.ui.UI;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * Panel quản lý Nhân viên.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Staff.
 */
public class StaffPanel extends BaseCrudPanel<Staff> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã NV", "Họ và tên", "Vai trò", "Điện thoại", "Email", "Trạng thái"
    };

    private final StaffService      staffService;
    private final JComboBox<String> filterRole;

    public StaffPanel(StaffService staffService) {
        super(COLUMNS);
        this.staffService = staffService;
        this.filterRole   = (JComboBox<String>) buildExtraFilters()[0];
        loadData();
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName() { return "nhân viên"; }
    @Override protected String getSearchPlaceholder() { return "Tìm theo tên, vai trò, số điện thoại..."; }
    @Override protected int[]  getSearchColumns()     { return new int[]{3, 4, 5, 6}; }

    @Override
    @SuppressWarnings("rawtypes")
    protected JComboBox[] buildExtraFilters() {
        JComboBox<String> combo = new JComboBox<>(
            new String[]{"Tất cả", "Consultant", "Accountant", "Manager", "Other"});
        combo.setFont(Theme.FONT_PLAIN);
        combo.addActionListener(e -> applyFilters());
        return new JComboBox[]{combo};
    }

    @Override
    protected void applyFilters() {
        String kw   = searchField.getText().trim();
        String role = filterRole != null ? (String) filterRole.getSelectedItem() : "Tất cả";

        RowFilter<DefaultTableModel, Object> textFilter = kw.isEmpty()
                ? null : RowFilter.regexFilter("(?i)" + kw, 3, 4, 5, 6);
        RowFilter<DefaultTableModel, Object> roleFilter =
                (role == null || "Tất cả".equals(role))
                ? null : RowFilter.regexFilter("(?i)^" + role + "$", 4);

        if      (textFilter == null && roleFilter == null) sorter.setRowFilter(null);
        else if (textFilter == null)                       sorter.setRowFilter(roleFilter);
        else if (roleFilter == null)                       sorter.setRowFilter(textFilter);
        else sorter.setRowFilter(RowFilter.andFilter(List.of(textFilter, roleFilter)));

        statusLabel.setText("Hiển thị: " + table.getRowCount() + " nhân viên");
    }

    @Override
    protected Staff getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : staffService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        staffService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Staff> list = staffService.findAll();
            tableModel.setRowCount(0);
            int[] idx = {1};
            list.forEach(s -> tableModel.addRow(new Object[]{
                s.getStaffId(),
                idx[0]++,
                s.getStaffId() != null ? String.format("NV%04d", s.getStaffId()) : "",
                s.getFullName(),
                s.getRole()   != null ? s.getRole()   : "",
                s.getPhone()  != null ? s.getPhone()  : "",
                s.getEmail()  != null ? s.getEmail()  : "",
                s.getStatus() != null ? s.getStatus() : ""
            }));
            var cm = table.getColumnModel();
            cm.getColumn(1).setPreferredWidth(50);  cm.getColumn(1).setMaxWidth(55);
            cm.getColumn(2).setPreferredWidth(88);  cm.getColumn(2).setMaxWidth(100);
            cm.getColumn(3).setPreferredWidth(180);
            cm.getColumn(4).setPreferredWidth(110); cm.getColumn(4).setMaxWidth(130);
            cm.getColumn(5).setPreferredWidth(115); cm.getColumn(5).setMaxWidth(140);
            cm.getColumn(6).setPreferredWidth(170);
            cm.getColumn(7).setPreferredWidth(110); cm.getColumn(7).setMaxWidth(130);

            statusLabel.setText("Tổng: " + list.size() + " nhân viên");
            applyFilters();
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    @Override
    protected void openDialog(Staff existing) {
        StaffFormDialog dlg = new StaffFormDialog(ownerFrame(), existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;
        try {
            if (existing != null) staffService.update(dlg.getStaff());
            else                  staffService.save(dlg.getStaff());
            showSuccess(existing != null ? "Cập nhật nhân viên thành công." : "Thêm nhân viên mới thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }
}

