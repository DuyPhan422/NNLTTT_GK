package com.company.ems.ui.panels;

import com.company.ems.model.Course;
import com.company.ems.service.CourseService;
import com.company.ems.ui.UI;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel quản lý Khóa học.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Course.
 */
public class CoursePanel extends BaseCrudPanel<Course> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã KH", "Tên khóa học", "Cấp độ", "Thời lượng", "Học phí", "Trạng thái"
    };

    private final CourseService     courseService;
    private final JComboBox<String> filterLevel;
    private final JComboBox<String> filterStatus;
    private final NumberFormat      currencyFmt =
            NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));

    public CoursePanel(CourseService courseService) {
        super(COLUMNS);
        this.courseService = courseService;
        JComboBox[] extras = buildExtraFilters();
        this.filterLevel   = (JComboBox<String>) extras[0];
        this.filterStatus  = (JComboBox<String>) extras[1];
        loadData();
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName() { return "khóa học"; }
    @Override protected String getSearchPlaceholder() { return "Tìm theo tên khóa học..."; }
    @Override protected int[]  getSearchColumns()     { return new int[]{3, 4}; }

    @Override
    @SuppressWarnings("rawtypes")
    protected JComboBox[] buildExtraFilters() {
        JComboBox<String> level  = new JComboBox<>(new String[]{"Tất cả", "Cơ bản", "Trung cấp", "Nâng cao"});
        JComboBox<String> status = new JComboBox<>(new String[]{"Tất cả", "Hoạt động", "Không hoạt động"});
        level.setFont(Theme.FONT_PLAIN);
        status.setFont(Theme.FONT_PLAIN);
        level.addActionListener(e  -> applyFilters());
        status.addActionListener(e -> applyFilters());
        return new JComboBox[]{level, status};
    }

    @Override
    protected void applyFilters() {
        String keyword = searchField.getText().trim();
        String level   = filterLevel  != null ? (String) filterLevel.getSelectedItem()  : "Tất cả";
        String status  = filterStatus != null ? (String) filterStatus.getSelectedItem() : "Tất cả";

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String name      = String.valueOf(entry.getValue(3));
                String levelVal  = String.valueOf(entry.getValue(4));
                String statusVal = String.valueOf(entry.getValue(7));
                boolean matchKw     = keyword.isEmpty() || name.toLowerCase().contains(keyword.toLowerCase());
                boolean matchLevel  = "Tất cả".equals(level)  || levelVal.equalsIgnoreCase(level);
                boolean matchStatus = "Tất cả".equals(status) || statusVal.equalsIgnoreCase(status);
                return matchKw && matchLevel && matchStatus;
            }
        });
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " khóa học");
    }

    @Override
    protected Course getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : courseService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        courseService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Course> list = courseService.findAll();
            tableModel.setRowCount(0);
            AtomicInteger idx = new AtomicInteger(1);
            list.forEach(c -> tableModel.addRow(new Object[]{
                c.getCourseId(),
                idx.getAndIncrement(),
                c.getCourseId() != null ? String.format("KH%04d", c.getCourseId()) : "",
                c.getCourseName(),
                c.getLevel()  != null ? c.getLevel() : "",
                formatDuration(c.getDuration(), c.getDurationUnit()),
                formatFee(c.getFee()),
                c.getStatus()
            }));
            var cm = table.getColumnModel();
            cm.getColumn(1).setPreferredWidth(50);  cm.getColumn(1).setMaxWidth(55);
            cm.getColumn(2).setPreferredWidth(88);  cm.getColumn(2).setMaxWidth(100);
            cm.getColumn(3).setPreferredWidth(220);
            cm.getColumn(4).setPreferredWidth(90);  cm.getColumn(4).setMaxWidth(110);
            cm.getColumn(5).setPreferredWidth(90);  cm.getColumn(5).setMaxWidth(110);
            cm.getColumn(6).setPreferredWidth(130); cm.getColumn(6).setMaxWidth(160);
            cm.getColumn(7).setPreferredWidth(110); cm.getColumn(7).setMaxWidth(130);

            statusLabel.setText("Tổng: " + list.size() + " khóa học");
            applyFilters();
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String formatDuration(Integer duration, String unit) {
        if (duration == null) return "";
        return duration + " " + ("Hour".equals(unit) ? "giờ" : "tuần");
    }

    private String formatFee(BigDecimal fee) {
        return fee == null ? "" : currencyFmt.format(fee);
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    @Override
    protected void openDialog(Course existing) {
        CourseFormDialog dlg = new CourseFormDialog(ownerFrame(), existing);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;
        try {
            if (existing != null) courseService.update(dlg.getCourse());
            else                  courseService.save(dlg.getCourse());
            showSuccess(existing != null ? "Cập nhật khóa học thành công." : "Thêm khóa học mới thành công.");
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }
}
