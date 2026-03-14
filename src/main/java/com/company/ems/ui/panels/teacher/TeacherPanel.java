package com.company.ems.ui.panels.teacher;

import com.company.ems.model.Teacher;
import com.company.ems.service.TeacherService;
import com.company.ems.ui.UI;
import com.company.ems.ui.panels.base.BaseCrudPanel;
import com.company.ems.ui.panels.dialogs.TeacherFormDialog;

import javax.swing.*;
import java.util.List;

/**
 * Panel quản lý Giáo viên.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Teacher.
 */
public class TeacherPanel extends BaseCrudPanel<Teacher> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã GV", "Họ và tên",
        "Điện thoại", "Email", "Chuyên môn", "Trạng thái"
    };

    private final TeacherService teacherService;

    public TeacherPanel(TeacherService teacherService) {
        super(COLUMNS);
        this.teacherService = teacherService;
        loadData();
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName() { return "giáo viên"; }
    @Override protected String getSearchPlaceholder() { return "Tìm theo tên, chuyên môn, số điện thoại..."; }
    @Override protected int[]  getSearchColumns()     { return new int[]{3, 4, 5, 6}; }

    @Override
    protected Teacher getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : teacherService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        teacherService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Teacher> list = teacherService.findAll();
            tableModel.setRowCount(0);
            int[] idx = {1};
            list.forEach(t -> tableModel.addRow(new Object[]{
                t.getTeacherId(),
                idx[0]++,
                t.getTeacherId() != null ? String.format("GV%04d", t.getTeacherId()) : "",
                t.getFullName(),
                t.getPhone()     != null ? t.getPhone()     : "",
                t.getEmail()     != null ? t.getEmail()     : "",
                t.getSpecialty() != null ? t.getSpecialty() : "",
                t.getStatus()
            }));
            var cm = table.getColumnModel();
            cm.getColumn(1).setPreferredWidth(50);  cm.getColumn(1).setMaxWidth(55);
            cm.getColumn(2).setPreferredWidth(88);  cm.getColumn(2).setMaxWidth(100);
            cm.getColumn(3).setPreferredWidth(180);
            cm.getColumn(4).setPreferredWidth(115); cm.getColumn(4).setMaxWidth(140);
            cm.getColumn(5).setPreferredWidth(170);
            cm.getColumn(6).setPreferredWidth(150);
            cm.getColumn(7).setPreferredWidth(110); cm.getColumn(7).setMaxWidth(130);

            statusLabel.setText("Tổng: " + list.size() + " giáo viên");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    @Override
    protected void openDialog(Teacher existing) {
        TeacherFormDialog dlg = new TeacherFormDialog(ownerFrame(), existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;
        try {
            if (existing != null) teacherService.update(dlg.getEntity());
            else                  teacherService.save(dlg.getEntity());
            showSuccess(existing != null ? "Cập nhật thành công." : "Thêm mới thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }
}

