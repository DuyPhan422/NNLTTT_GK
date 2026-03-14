package com.company.ems.ui.panels.dialogs;

import com.company.ems.model.Teacher;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.base.BaseFormDialog;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialog Thêm / Sửa giáo viên — tách riêng khỏi TeacherPanel (SRP).
 * Cách dùng:
 *   TeacherFormDialog dlg = new TeacherFormDialog(parentFrame, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Teacher result = dlg.getEntity();
 */
public class TeacherFormDialog extends BaseFormDialog<Teacher> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Form fields
    private final JTextField tfName;
    private final JTextField tfPhone;
    private final JTextField tfEmail;
    private final JTextField tfSpecialty;
    private final JTextField tfHireDate;
    private final JComboBox<ActiveStatus> cbStatus;

    // Output
    private final Teacher teacher;

    public TeacherFormDialog(Frame owner, Teacher existing) {
        super(owner, existing != null ? "Sửa giáo viên" : "Thêm giáo viên mới");
        this.teacher = existing != null ? existing : new Teacher();

        boolean isEdit = existing != null;

        tfName      = ComponentFactory.formField();
        tfPhone     = ComponentFactory.formField();
        tfEmail     = ComponentFactory.formField();
        tfSpecialty = ComponentFactory.formField();
        tfHireDate  = ComponentFactory.formField();

        if (isEdit) {
            if (existing.getFullName() != null) tfName.setText(existing.getFullName());
            if (existing.getPhone() != null) tfPhone.setText(existing.getPhone());
            if (existing.getEmail() != null) tfEmail.setText(existing.getEmail());
            if (existing.getSpecialty() != null) tfSpecialty.setText(existing.getSpecialty());
            if (existing.getHireDate() != null) tfHireDate.setText(existing.getHireDate().format(DATE_FMT));
        }

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getStatus() != null) {
            cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(600);
    }

    @Override
    protected JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Họ và tên *",            tfName);
        addRow(form, gbc, 1, "Điện thoại",             tfPhone);
        addRow(form, gbc, 2, "Email",                  tfEmail);
        addRow(form, gbc, 3, "Chuyên môn",             tfSpecialty);
        addRow(form, gbc, 4, "Ngày vào làm (dd/MM/yyyy)", tfHireDate);
        addRow(form, gbc, 5, "Trạng thái",             cbStatus);

        return form;
    }

    @Override
    protected boolean validateForm() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            setError("Họ và tên không được để trống.");
            tfName.requestFocus();
            return false;
        }

        if (!tfHireDate.getText().trim().isEmpty()) {
            try {
                LocalDate.parse(tfHireDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                setError("Định dạng ngày vào làm không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfHireDate.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    protected void commitToEntity() {
        teacher.setFullName(tfName.getText().trim());
        teacher.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        teacher.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        teacher.setSpecialty(tfSpecialty.getText().trim().isEmpty() ? null : tfSpecialty.getText().trim());
        
        LocalDate hireDate = null;
        if (!tfHireDate.getText().trim().isEmpty()) {
            try {
                hireDate = LocalDate.parse(tfHireDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ignored) {}
        }
        teacher.setHireDate(hireDate);
        teacher.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Teacher getEntity() {
        return teacher;
    }

    // ══════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = ComponentFactory.formLabel(label);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);
    }
}

