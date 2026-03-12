package com.company.ems.ui.panels;

import com.company.ems.model.Student;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.model.enums.Gender;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialog Thêm / Sửa học viên — tách riêng khỏi StudentPanel (SRP).
 * Cách dùng:
 *   StudentFormDialog dlg = new StudentFormDialog(parentFrame, existing); // null = thêm mới
 *   dlg.setVisible(true);
 *   if (dlg.isSaved()) Student result = dlg.getEntity();
 */
public class StudentFormDialog extends BaseFormDialog<Student> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Form fields ───────────────────────────────────
    private final JTextField      tfName;
    private final JTextField      tfDob;
    private final JComboBox<Gender> cbGender;
    private final JTextField      tfPhone;
    private final JTextField      tfEmail;
    private final JTextField      tfAddress;
    private final JComboBox<ActiveStatus> cbStatus;

    // ── Output ────────────────────────────────────────
    private final Student student;

    public StudentFormDialog(Frame owner, Student existing) {
        super(owner, existing != null ? "Sửa học viên" : "Thêm học viên mới");
        this.student = existing != null ? existing : new Student();

        boolean isEdit = existing != null;

        // Khởi tạo fields với ComponentFactory
        tfName    = ComponentFactory.formField();
        tfDob     = ComponentFactory.formField();
        tfPhone   = ComponentFactory.formField();
        tfEmail   = ComponentFactory.formField();
        tfAddress = ComponentFactory.formField();

        // Set giá trị nếu là edit
        if (isEdit) {
            tfName.setText(existing.getFullName());
            if (existing.getDateOfBirth() != null) tfDob.setText(existing.getDateOfBirth().format(DATE_FMT));
            if (existing.getPhone() != null) tfPhone.setText(existing.getPhone());
            if (existing.getEmail() != null) tfEmail.setText(existing.getEmail());
            if (existing.getAddress() != null) tfAddress.setText(existing.getAddress());
        }

        cbGender = new JComboBox<>(Gender.values());
        cbGender.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getGender() != null) {
            cbGender.setSelectedItem(Gender.fromValue(existing.getGender()));
        }

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(Theme.FONT_PLAIN);
        if (isEdit) {
            cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(480);
    }

    @Override
    protected JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Họ và tên *",            tfName);
        addRow(form, gbc, 1, "Ngày sinh (dd/MM/yyyy)", tfDob);
        addRow(form, gbc, 2, "Giới tính",              cbGender);
        addRow(form, gbc, 3, "Điện thoại",             tfPhone);
        addRow(form, gbc, 4, "Email",                  tfEmail);
        addRow(form, gbc, 5, "Địa chỉ",               tfAddress);
        addRow(form, gbc, 6, "Trạng thái",             cbStatus);

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

        if (!tfDob.getText().trim().isEmpty()) {
            try {
                LocalDate.parse(tfDob.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                setError("Định dạng ngày sinh không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfDob.requestFocus();
                return false;
            }
        }

        // Xác nhận trước khi lưu
        String confirmMsg = student.getStudentId() != null
                ? "Bạn có chắc muốn lưu các thay đổi cho học viên \"" + name + "\"?"
                : "Bạn có chắc muốn thêm học viên mới \"" + name + "\"?";
        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    @Override
    protected void commitToEntity() {
        student.setFullName(tfName.getText().trim());

        LocalDate dob = null;
        if (!tfDob.getText().trim().isEmpty()) {
            try {
                dob = LocalDate.parse(tfDob.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ignored) {}
        }
        student.setDateOfBirth(dob);

        student.setGender(((Gender) cbGender.getSelectedItem()).getValue());
        student.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        student.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        student.setAddress(tfAddress.getText().trim().isEmpty() ? null : tfAddress.getText().trim());
        student.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Student getEntity() {
        return student;
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

