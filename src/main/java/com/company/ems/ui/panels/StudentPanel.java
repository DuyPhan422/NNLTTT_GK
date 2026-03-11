package com.company.ems.ui.panels;

import com.company.ems.model.Student;
import com.company.ems.service.StudentService;
import com.company.ems.ui.UI;
import com.company.ems.ui.panels.StudentFormDialog;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel quản lý Học viên.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Student.
 */
public class StudentPanel extends BaseCrudPanel<Student> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã HV", "Họ và tên", "Ngày sinh",
        "Giới tính", "Điện thoại", "Email", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentService studentService;

    public StudentPanel(StudentService studentService) {
        super(COLUMNS);
        this.studentService = studentService;
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName()  { return "học viên"; }
    @Override protected String getSearchPlaceholder()  { return "Tìm theo tên, số điện thoại..."; }
    @Override protected int[]  getSearchColumns()      { return new int[]{3, 6, 7}; }

    @Override
    protected Student getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : studentService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        studentService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Student> list = studentService.findAll();
            tableModel.setRowCount(0);
            int[] idx = {1};
            list.forEach(s -> tableModel.addRow(new Object[]{
                s.getStudentId(),
                idx[0]++,
                s.getStudentId() != null ? String.format("HV%04d", s.getStudentId()) : "",
                s.getFullName(),
                s.getDateOfBirth() != null ? s.getDateOfBirth().format(DATE_FMT) : "",
                s.getGender()  != null ? s.getGender()  : "",
                s.getPhone()   != null ? s.getPhone()   : "",
                s.getEmail()   != null ? s.getEmail()   : "",
                s.getStatus()
            }));
            // Độ rộng cột đặc thù
            var cm = table.getColumnModel();
            cm.getColumn(1).setPreferredWidth(50);  cm.getColumn(1).setMaxWidth(55);
            cm.getColumn(2).setPreferredWidth(88);  cm.getColumn(2).setMaxWidth(100);
            cm.getColumn(3).setPreferredWidth(180);
            cm.getColumn(4).setPreferredWidth(105); cm.getColumn(4).setMaxWidth(120);
            cm.getColumn(5).setPreferredWidth(75);  cm.getColumn(5).setMaxWidth(90);
            cm.getColumn(6).setPreferredWidth(115); cm.getColumn(6).setMaxWidth(140);
            cm.getColumn(7).setPreferredWidth(170);
            cm.getColumn(8).setPreferredWidth(110); cm.getColumn(8).setMaxWidth(130);

            statusLabel.setText("Tổng: " + list.size() + " học viên");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    @Override
    protected void openDialog(Student existing) {
        StudentFormDialog dlg = new StudentFormDialog(ownerFrame(), existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;
        try {
            if (existing != null) studentService.update(dlg.getStudent());
            else                  studentService.save(dlg.getStudent());
            showSuccess(existing != null ? "Cập nhật thành công." : "Thêm mới thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }
}

