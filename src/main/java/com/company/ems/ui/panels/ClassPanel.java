package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Room;
import com.company.ems.model.Teacher;
import com.company.ems.service.*;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Panel quản lý Lớp học.
 * Kế thừa {@link BaseCrudPanel} — chỉ chứa logic đặc thù của Class.
 */
public class ClassPanel extends BaseCrudPanel<Class> {

    private static final String[] COLUMNS = {
        "ID", "STT", "Mã lớp", "Tên lớp", "Khóa học", "Giáo viên",
        "Phòng học", "SL học viên", "Bắt đầu", "Kết thúc", "Trạng thái"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClassService      classService;
    private final CourseService     courseService;
    private final TeacherService    teacherService;
    private final RoomService       roomService;
    private final EnrollmentService enrollmentService;
    private final InvoiceService    invoiceService;
    private final JComboBox<String> filterStatus;

    public ClassPanel(ClassService classService,
                      CourseService courseService,
                      TeacherService teacherService,
                      RoomService roomService,
                      EnrollmentService enrollmentService,
                      InvoiceService invoiceService) {
        super(COLUMNS);
        this.classService      = classService;
        this.courseService     = courseService;
        this.teacherService    = teacherService;
        this.roomService       = roomService;
        this.enrollmentService = enrollmentService;
        this.invoiceService    = invoiceService;
        this.filterStatus      = (JComboBox<String>) buildExtraFilters()[0];
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    @Override protected String getEntityDisplayName() { return "lớp học"; }
    @Override protected String getSearchPlaceholder() { return "Tìm theo tên lớp hoặc khóa học..."; }
    @Override protected int[]  getSearchColumns()     { return new int[]{3, 4}; }

    @Override
    @SuppressWarnings("rawtypes")
    protected JComboBox[] buildExtraFilters() {
        JComboBox<String> combo = new JComboBox<>(new String[]{
            "Tất cả", "Lên kế hoạch", "Mở lớp", "Đang diễn ra", "Hoàn thành", "Hủy lớp"
        });
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
                String className  = String.valueOf(entry.getValue(3)).toLowerCase();
                String courseName = String.valueOf(entry.getValue(4)).toLowerCase();
                String statusVal  = String.valueOf(entry.getValue(10));
                boolean matchKw     = keyword.isEmpty() || className.contains(keyword) || courseName.contains(keyword);
                boolean matchStatus = "Tất cả".equals(status) || statusVal.equals(status);
                return matchKw && matchStatus;
            }
        });
        statusLabel.setText("Hiển thị: " + table.getRowCount() + " lớp học");
    }

    @Override
    protected Class getSelectedEntity() {
        Long id = getSelectedId();
        return id == null ? null : classService.findById(id);
    }

    @Override
    protected void deleteEntity(Long id) {
        classService.delete(id);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    @Override
    public void loadData() {
        try {
            List<Class> list = classService.findAll();
            tableModel.setRowCount(0);

            Map<Long, Long> enrollCounts = enrollmentService.findAll().stream()
                    .filter(e -> e.getClazz() != null && !"Hủy".equals(e.getStatus()))
                    .collect(Collectors.groupingBy(e -> e.getClazz().getClassId(), Collectors.counting()));

            AtomicInteger idx = new AtomicInteger(1);
            list.forEach(c -> {
                long cur = enrollCounts.getOrDefault(c.getClassId(), 0L);
                String slHV = cur + " / " + (c.getMaxStudent() != null && c.getMaxStudent() > 0
                        ? String.valueOf(c.getMaxStudent()) : "∞");
                tableModel.addRow(new Object[]{
                    c.getClassId(),
                    idx.getAndIncrement(),
                    c.getClassId() != null ? String.format("L%04d", c.getClassId()) : "",
                    c.getClassName(),
                    c.getCourse()  != null ? c.getCourse().getCourseName()  : "",
                    c.getTeacher() != null ? c.getTeacher().getFullName()   : "",
                    c.getRoom()    != null ? c.getRoom().getRoomName()      : "",
                    slHV,
                    c.getStartDate() != null ? c.getStartDate().format(DATE_FMT) : "",
                    c.getEndDate()   != null ? c.getEndDate().format(DATE_FMT)   : "",
                    c.getStatus()
                });
            });
            statusLabel.setText("Tổng: " + list.size() + " lớp học");
            applyFilters();
        } catch (Exception e) {
            showError("Không thể tải dữ liệu: " + e.getMessage());
        }
    }

    // ── Dialog & Business Logic ───────────────────────────────────────────

    @Override
    protected void openDialog(Class existing) {
        try {
            List<Course>  courses  = courseService.findAll();
            List<Teacher> teachers = teacherService.findAll();
            List<Room>    rooms    = roomService.findAll();

            ClassFormDialog dlg = new ClassFormDialog(ownerFrame(), existing, courses, teachers, rooms);
            dlg.setVisible(true);
            if (!dlg.isSaved()) return;

            if (existing != null) {
                String oldStatus = existing.getStatus();
                classService.update(dlg.getClazz());
                if (!"Hủy lớp".equals(oldStatus) && "Hủy lớp".equals(dlg.getClazz().getStatus())) {
                    cancelClassEnrollments(dlg.getClazz());
                }
                showSuccess("Cập nhật lớp học thành công.");
            } else {
                classService.save(dlg.getClazz());
                showSuccess("Thêm lớp học mới thành công.");
            }
            notifyChanged();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    /** Hủy tất cả ghi danh trong lớp và cập nhật hóa đơn cho từng học viên. */
    private void cancelClassEnrollments(Class clazz) {
        try {
            List<Enrollment> enrollments = enrollmentService.findAll().stream()
                    .filter(e -> e.getClazz() != null
                              && e.getClazz().getClassId().equals(clazz.getClassId())
                              && "Đã đăng ký".equals(e.getStatus()))
                    .toList();

            Set<Long> affectedStudentIds = new HashSet<>();
            enrollments.forEach(e -> {
                e.setStatus("Đã hủy");
                enrollmentService.update(e);
                if (e.getStudent() != null) affectedStudentIds.add(e.getStudent().getStudentId());
            });
            affectedStudentIds.forEach(this::recalcInvoice);
        } catch (Exception ex) {
            System.err.println("[Hủy lớp] Lỗi cập nhật ghi danh: " + ex.getMessage());
        }
    }

    private void recalcInvoice(Long studentId) {
        BigDecimal total = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                          && e.getStudent().getStudentId().equals(studentId)
                          && "Đã đăng ký".equals(e.getStatus()))
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice pending = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null
                          && i.getStudent().getStudentId().equals(studentId)
                          && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            if (pending != null) invoiceService.delete(pending.getInvoiceId());
        } else if (pending != null) {
            pending.setTotalAmount(total);
            invoiceService.update(pending);
        }
    }
}

