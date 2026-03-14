package com.company.ems.ui.panels.dialogs;

import com.company.ems.model.Staff;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.model.enums.StaffRole;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.base.BaseFormDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog Thêm / Sửa nhân viên — tách riêng khỏi StaffPanel (SRP).
 * Dùng:
 *   StaffFormDialog dlg = new StaffFormDialog(owner, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Staff result = dlg.getEntity();
 */
public class StaffFormDialog extends BaseFormDialog<Staff> {

    // ── Fields ───────────────────────────────────────────────────────────
    private final JTextField    tfName;
    private final JTextField    tfPhone;
    private final JTextField    tfEmail;
    private final JComboBox<StaffRole> cbRole;
    private final JComboBox<ActiveStatus> cbStatus;

    // ── Output ───────────────────────────────────────────────────────────
    private final Staff staff;

    public StaffFormDialog(Frame owner, Staff existing) {
        super(owner, existing != null ? "Sửa nhân viên" : "Thêm nhân viên mới");
        this.staff = existing != null ? existing : new Staff();

        tfName  = ComponentFactory.formField();
        tfPhone = ComponentFactory.formField();
        tfEmail = ComponentFactory.formField();

        if (existing != null) {
            if (existing.getFullName() != null) tfName.setText(existing.getFullName());
            if (existing.getPhone() != null) tfPhone.setText(existing.getPhone());
            if (existing.getEmail() != null) tfEmail.setText(existing.getEmail());
        }

        cbRole   = new JComboBox<>(StaffRole.values());
        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbRole.setFont(Theme.FONT_PLAIN);
        cbStatus.setFont(Theme.FONT_PLAIN);

        if (existing != null) {
            if (existing.getRole()   != null) cbRole.setSelectedItem(StaffRole.fromValue(existing.getRole()));
            if (existing.getStatus() != null) cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(520);
    }

    @Override
    protected JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 0, 6, 12);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // Hàng 1 — Họ tên (full width)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        form.add(ComponentFactory.formLabel("Họ và tên *"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
        form.add(tfName, gbc);

        // Hàng 2 — Điện thoại | Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        form.add(ComponentFactory.formLabel("Điện thoại"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        form.add(tfPhone, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(ComponentFactory.formLabel("Email"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(tfEmail, gbc);

        // Hàng 3 — Vai trò | Trạng thái
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(ComponentFactory.formLabel("Vai trò"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        form.add(cbRole, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(ComponentFactory.formLabel("Trạng thái"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(cbStatus, gbc);

        return form;
    }

    @Override
    protected boolean validateForm() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            setError("⚠  Họ và tên không được để trống.");
            tfName.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void commitToEntity() {
        staff.setFullName(tfName.getText().trim());
        staff.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        staff.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        staff.setRole(((StaffRole) cbRole.getSelectedItem()).getValue());
        staff.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Staff getEntity() {
        return staff;
    }
}

