package com.company.ems.ui.panels;

import com.company.ems.model.Room;
import com.company.ems.service.RoomService;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel quản lý Phòng học.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Room.
 */
public class RoomPanel extends BaseCrudPanel<Room> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã phòng", "Tên phòng", "Sức chứa", "Vị trí", "Trạng thái"
    };

    private final RoomService       roomService;
    private final JComboBox<String> filterStatus;

    public RoomPanel(RoomService roomService) {
        super(COLUMNS);
        this.roomService  = roomService;
        this.filterStatus = (JComboBox<String>) buildExtraFilters()[0];
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName() { return "phòng học"; }
    @Override protected String getSearchPlaceholder() { return "Tìm theo tên phòng hoặc vị trí..."; }
    @Override protected int[]  getSearchColumns()     { return new int[]{3, 5}; }

    @Override
    @SuppressWarnings("rawtypes")
    protected JComboBox[] buildExtraFilters() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"Tất cả", "Active", "Inactive"});
        combo.setFont(Theme.FONT_PLAIN);
        combo.addActionListener(e -> applyFilters());
        return new JComboBox[]{combo};
    }

    @Override
    protected void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = filterStatus != null ? (String) filterStatus.getSelectedItem() : "Tất cả";

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String name      = entry.getStringValue(3).toLowerCase();
                String location  = entry.getStringValue(5).toLowerCase();
                String statusVal = entry.getStringValue(6);
                boolean matchKw     = keyword.isEmpty() || name.contains(keyword) || location.contains(keyword);
                boolean matchStatus = "Tất cả".equals(status) || statusVal.equals(status);
                return matchKw && matchStatus;
            }
        });
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " phòng học");
    }

    @Override
    protected Room getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : roomService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        roomService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Room> list = roomService.findAll();
            tableModel.setRowCount(0);
            AtomicInteger idx = new AtomicInteger(1);
            list.forEach(r -> tableModel.addRow(new Object[]{
                r.getRoomId(),
                idx.getAndIncrement(),
                r.getRoomId()   != null ? String.format("P%03d", r.getRoomId()) : "",
                r.getRoomName(),
                r.getCapacity() != null ? r.getCapacity() : 0,
                r.getLocation() != null ? r.getLocation() : "",
                r.getStatus()
            }));
            statusLabel.setText("Tổng: " + list.size() + " phòng học");
            applyFilters();
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    @Override
    protected void openDialog(Room existing) {
        RoomFormDialog dlg = new RoomFormDialog(ownerFrame(), existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;
        try {
            if (existing != null) roomService.update(dlg.getRoom());
            else                  roomService.save(dlg.getRoom());
            showSuccess(existing != null ? "Cập nhật phòng học thành công." : "Thêm phòng học mới thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }
}
