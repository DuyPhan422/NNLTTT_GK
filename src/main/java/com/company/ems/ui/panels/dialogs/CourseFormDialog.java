package com.company.ems.ui.panels.dialogs;

import com.company.ems.model.Course;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.model.enums.CourseLevel;
import com.company.ems.model.enums.DurationUnit;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.base.BaseFormDialog;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Dialog Thêm / Sửa khóa học — tách khỏi CoursePanel (SRP).
 */
public class CourseFormDialog extends BaseFormDialog<Course> {

    private final JTextField tfName;
    private final JTextArea  taDescription;
    private final JComboBox<CourseLevel> cbLevel;
    private final JTextField tfDuration;
    private final JComboBox<DurationUnit> cbDurationUnit;
    private final JTextField tfFee;
    private final JComboBox<ActiveStatus> cbStatus;

    private final Course course;

    public CourseFormDialog(Frame owner, Course existing) {
        super(owner, existing != null ? "Sửa khóa học" : "Thêm khóa học mới");
        this.course = existing != null ? existing : new Course();

        boolean isEdit = existing != null;

        tfName = ComponentFactory.formField();
        taDescription = ComponentFactory.formTextArea(3);
        tfDuration = ComponentFactory.formField();
        tfFee = ComponentFactory.formField();

        if (isEdit) {
            if (existing.getCourseName() != null) tfName.setText(existing.getCourseName());
            if (existing.getDescription() != null) taDescription.setText(existing.getDescription());
            if (existing.getDuration() != null) tfDuration.setText(String.valueOf(existing.getDuration()));
            if (existing.getFee() != null) tfFee.setText(existing.getFee().toPlainString());
        }

        cbLevel = new JComboBox<>(CourseLevel.values());
        cbLevel.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getLevel() != null) {
            cbLevel.setSelectedItem(CourseLevel.fromValue(existing.getLevel()));
        }

        cbDurationUnit = new JComboBox<>(DurationUnit.values());
        cbDurationUnit.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getDurationUnit() != null) {
            cbDurationUnit.setSelectedItem(DurationUnit.fromValue(existing.getDurationUnit()));
        }

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getStatus() != null) {
            cbStatus.setSelectedItem(ActiveStatus.fromValue(existing.getStatus()));
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(620);
    }

    @Override
    protected JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Tên khóa học *", tfName);

        // Mô tả - multi-line
        gbc.gridy  = 2;
        gbc.insets = new Insets(10, 0, 2, 0);
        form.add(ComponentFactory.formLabel("Mô tả"), gbc);

        gbc.gridy  = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        JScrollPane descScroll = new JScrollPane(taDescription);
        descScroll.setPreferredSize(new Dimension(0, 80));
        form.add(descScroll, gbc);

        addRow(form, gbc, 4, "Cấp độ", cbLevel);
        addRow(form, gbc, 5, "Thời lượng", tfDuration);
        addRow(form, gbc, 6, "Đơn vị thời lượng", cbDurationUnit);
        addRow(form, gbc, 7, "Học phí", tfFee);
        addRow(form, gbc, 8, "Trạng thái", cbStatus);

        return form;
    }

    @Override
    protected boolean validateForm() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            setError("Tên khóa học không được để trống.");
            tfName.requestFocus();
            return false;
        }

        if (!tfDuration.getText().trim().isEmpty()) {
            try {
                int duration = Integer.parseInt(tfDuration.getText().trim());
                if (duration <= 0) {
                    setError("Thời lượng phải là số dương.");
                    tfDuration.requestFocus();
                    return false;
                }
            } catch (NumberFormatException ex) {
                setError("Thời lượng phải là số nguyên.");
                tfDuration.requestFocus();
                return false;
            }
        }

        if (!tfFee.getText().trim().isEmpty()) {
            try {
                BigDecimal fee = new BigDecimal(tfFee.getText().trim());
                if (fee.compareTo(BigDecimal.ZERO) < 0) {
                    setError("Học phí không được âm.");
                    tfFee.requestFocus();
                    return false;
                }
            } catch (NumberFormatException ex) {
                setError("Học phí không hợp lệ.");
                tfFee.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    protected void commitToEntity() {
        course.setCourseName(tfName.getText().trim());
        course.setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        course.setLevel(((CourseLevel) cbLevel.getSelectedItem()).getValue());

        Integer duration = null;
        if (!tfDuration.getText().trim().isEmpty()) {
            try {
                duration = Integer.parseInt(tfDuration.getText().trim());
            } catch (NumberFormatException ignored) {}
        }
        course.setDuration(duration);

        course.setDurationUnit(((DurationUnit) cbDurationUnit.getSelectedItem()).getValue());

        BigDecimal fee = BigDecimal.ZERO;
        if (!tfFee.getText().trim().isEmpty()) {
            try {
                fee = new BigDecimal(tfFee.getText().trim());
            } catch (NumberFormatException ignored) {}
        }
        course.setFee(fee);

        course.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Course getEntity() {
        return course;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        int base = row * 2 + 2; // tránh đè vùng mô tả
        gbc.gridy  = base;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        panel.add(ComponentFactory.formLabel(label), gbc);

        gbc.gridy  = base + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);
    }
}

