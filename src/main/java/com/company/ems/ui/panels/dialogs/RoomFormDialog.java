package com.company.ems.ui.panels.dialogs;

import com.company.ems.model.Room;
import com.company.ems.model.enums.ActiveStatus;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.base.BaseFormDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog Thêm / Sửa phòng học.
 */
public class RoomFormDialog extends BaseFormDialog<Room> {

    private final JTextField      tfName;
    private final JTextField      tfCapacity;
    private final JTextField      tfLocation;
    private final JComboBox<ActiveStatus> cbStatus;

    private final Room room;

    public RoomFormDialog(Frame owner, Room existing) {
        super(owner, existing != null ? "Sửa phòng học" : "Thêm phòng học mới");
        this.room = existing != null ? existing : new Room();
        boolean isEdit = existing != null;

        tfName     = ComponentFactory.formField();
        tfCapacity = ComponentFactory.formField();
        tfLocation = ComponentFactory.formField();

        if (isEdit) {
            if (existing.getRoomName() != null) tfName.setText(existing.getRoomName());
            if (existing.getCapacity() != null) tfCapacity.setText(String.valueOf(existing.getCapacity()));
            if (existing.getLocation() != null) tfLocation.setText(existing.getLocation());
        }

        cbStatus = new JComboBox<>(ActiveStatus.values());
        cbStatus.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getStatus() != null) {
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Tên phòng học *",   tfName);
        addRow(form, gbc, 1, "Sức chứa (chỗ)",    tfCapacity);
        addRow(form, gbc, 2, "Vị trí / Tầng",     tfLocation);
        addRow(form, gbc, 3, "Trạng thái",         cbStatus);

        return form;
    }

    @Override
    protected boolean validateForm() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            setError("Tên phòng học không được để trống.");
            tfName.requestFocus();
            return false;
        }

        if (!tfCapacity.getText().trim().isEmpty()) {
            try {
                int capacity = Integer.parseInt(tfCapacity.getText().trim());
                if (capacity < 0) {
                    setError("Sức chứa phải là số không âm.");
                    tfCapacity.requestFocus();
                    return false;
                }
            } catch (NumberFormatException ex) {
                setError("Sức chứa phải là số nguyên.");
                tfCapacity.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    protected void commitToEntity() {
        room.setRoomName(tfName.getText().trim());
        
        int capacity = 0;
        if (!tfCapacity.getText().trim().isEmpty()) {
            try {
                capacity = Integer.parseInt(tfCapacity.getText().trim());
            } catch (NumberFormatException ignored) {}
        }
        room.setCapacity(capacity);
        
        room.setLocation(tfLocation.getText().trim().isEmpty() ? null : tfLocation.getText().trim());
        room.setStatus(((ActiveStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Room getEntity() {
        return room;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = ComponentFactory.formLabel(label);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(0, 36));
        panel.add(field, gbc);
    }
}

