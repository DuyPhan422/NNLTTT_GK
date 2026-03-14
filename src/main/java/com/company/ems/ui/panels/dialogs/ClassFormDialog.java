package com.company.ems.ui.panels.dialogs;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Room;
import com.company.ems.model.Teacher;
import com.company.ems.model.enums.ClassStatus;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;
import com.company.ems.ui.panels.base.BaseFormDialog;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * Dialog Thêm / Sửa lớp học.
 */
public class ClassFormDialog extends BaseFormDialog<Class> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField        tfName;
    private final JComboBox<Course>  cbCourse;
    private final JComboBox<Teacher> cbTeacher;
    private final JComboBox<Room>    cbRoom;

    // Date inputs: JSpinner (calendar picker) + JTextField (manual) side by side
    private final JSpinner   spinnerStart;
    private final JSpinner   spinnerEnd;
    private final JTextField tfStartDate;
    private final JTextField tfEndDate;

    private final JTextField    tfMaxStudent;
    private final JComboBox<ClassStatus> cbStatus;
    private final JLabel        lblEndHint;

    private final Class clazz;
    private boolean updatingDates = false;

    public ClassFormDialog(Frame owner,
                           Class existing,
                           List<Course> courses,
                           List<Teacher> teachers,
                           List<Room> rooms) {
        super(owner, existing != null ? "Sửa lớp học" : "Thêm lớp học mới");
        this.clazz = existing != null ? existing : new Class();
        boolean isEdit = existing != null;

        tfName = ComponentFactory.formField();
        if (isEdit && existing.getClassName() != null) tfName.setText(existing.getClassName());

        cbCourse = new JComboBox<>(courses.toArray(new Course[0]));
        cbCourse.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getCourse() != null) {
            Long cid = existing.getCourse().getCourseId();
            for (int i = 0; i < cbCourse.getItemCount(); i++) {
                Course c = cbCourse.getItemAt(i);
                if (c != null && cid.equals(c.getCourseId())) { cbCourse.setSelectedIndex(i); break; }
            }
        }

        cbTeacher = new JComboBox<>(teachers.toArray(new Teacher[0]));
        cbTeacher.setFont(Theme.FONT_PLAIN);
        cbTeacher.insertItemAt(null, 0);
        cbTeacher.setSelectedIndex(0);
        if (isEdit && existing.getTeacher() != null) {
            Long tid = existing.getTeacher().getTeacherId();
            for (int i = 0; i < cbTeacher.getItemCount(); i++) {
                Teacher tc = cbTeacher.getItemAt(i);
                if (tc != null && tid.equals(tc.getTeacherId())) { cbTeacher.setSelectedIndex(i); break; }
            }
        }

        cbRoom = new JComboBox<>(rooms.toArray(new Room[0]));
        cbRoom.setFont(Theme.FONT_PLAIN);
        cbRoom.insertItemAt(null, 0);
        cbRoom.setSelectedIndex(0);
        if (isEdit && existing.getRoom() != null) {
            Long rid = existing.getRoom().getRoomId();
            for (int i = 0; i < cbRoom.getItemCount(); i++) {
                Room r = cbRoom.getItemAt(i);
                if (r != null && rid.equals(r.getRoomId())) { cbRoom.setSelectedIndex(i); break; }
            }
        }

        // StartDate
        LocalDate initStart = (isEdit && existing.getStartDate() != null)
                ? existing.getStartDate() : LocalDate.now();
        spinnerStart = buildDateSpinner(initStart);
        tfStartDate  = ComponentFactory.formField();
        tfStartDate.setText(initStart.format(DATE_FMT));
        tfStartDate.setPreferredSize(new Dimension(110, 34));

        // EndDate
        LocalDate initEnd = (isEdit && existing.getEndDate() != null)
                ? existing.getEndDate() : null;
        spinnerEnd  = buildDateSpinner(initEnd != null ? initEnd : initStart.plusWeeks(12));
        tfEndDate   = ComponentFactory.formField();
        if (initEnd != null) tfEndDate.setText(initEnd.format(DATE_FMT));
        tfEndDate.setPreferredSize(new Dimension(110, 34));

        lblEndHint = new JLabel(" ");
        lblEndHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEndHint.setForeground(Theme.BLUE);

        tfMaxStudent = ComponentFactory.formField();
        if (isEdit && existing.getMaxStudent() != null) tfMaxStudent.setText(String.valueOf(existing.getMaxStudent()));

        cbStatus = new JComboBox<>(ClassStatus.values());
        cbStatus.setFont(Theme.FONT_PLAIN);
        if (isEdit && existing.getStatus() != null) {
             cbStatus.setSelectedItem(ClassStatus.fromValue(existing.getStatus()));
        }

        // Wire listeners
        spinnerStart.addChangeListener(e -> {
            if (updatingDates) return;
            LocalDate d = spinnerToLocalDate(spinnerStart);
            updatingDates = true;
            tfStartDate.setText(d.format(DATE_FMT));
            updatingDates = false;
            recalcEndDate();
        });
        spinnerEnd.addChangeListener(e -> {
            if (updatingDates) return;
            LocalDate d = spinnerToLocalDate(spinnerEnd);
            updatingDates = true;
            tfEndDate.setText(d != null ? d.format(DATE_FMT) : "");
            updatingDates = false;
        });

        tfStartDate.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (updatingDates) return;
                try {
                    LocalDate d = LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
                    updatingDates = true;
                    spinnerStart.setValue(localDateToDate(d));
                    updatingDates = false;
                    recalcEndDate();
                } catch (DateTimeParseException ignored) {}
            }
        });
        tfEndDate.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (updatingDates) return;
                try {
                    LocalDate d = LocalDate.parse(tfEndDate.getText().trim(), DATE_FMT);
                    updatingDates = true;
                    spinnerEnd.setValue(localDateToDate(d));
                    updatingDates = false;
                } catch (DateTimeParseException ignored) {}
            }
        });

        cbCourse.addActionListener(e -> recalcEndDate());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI(680);
    }

    private void recalcEndDate() {
        Course course = (Course) cbCourse.getSelectedItem();
        LocalDate start;
        try {
            start = LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return;
        }
        if (course == null || course.getDuration() == null) return;

        int duration = course.getDuration();
        String unit = course.getDurationUnit() != null ? course.getDurationUnit() : "Week";
        LocalDate endDate;
        String hint;

        if ("Week".equalsIgnoreCase(unit)) {
            endDate = start.plusWeeks(duration);
            hint = "✦ Tự động: " + duration + " tuần từ ngày bắt đầu";
        } else {
            endDate = null;
            hint = "⚠ Khoá học tính theo giờ — vui lòng nhập ngày kết thúc thủ công";
        }

        updatingDates = true;
        if (endDate != null) {
            tfEndDate.setText(endDate.format(DATE_FMT));
            spinnerEnd.setValue(localDateToDate(endDate));
        } else {
            tfEndDate.setText("");
        }
        lblEndHint.setText(hint);
        updatingDates = false;
    }

    @Override
    protected JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Tên lớp *", tfName, null);
        addRow(form, gbc, 1, "Khóa học *", cbCourse, null);
        addRow(form, gbc, 2, "Giáo viên", cbTeacher, null);
        addRow(form, gbc, 3, "Phòng học", cbRoom, null);
        addRow(form, gbc, 4, "Ngày bắt đầu *", buildDateRow(spinnerStart, tfStartDate), null);
        addRow(form, gbc, 5, "Ngày kết thúc", buildDateRow(spinnerEnd, tfEndDate), lblEndHint);
        addRow(form, gbc, 6, "Số học viên tối đa", tfMaxStudent, null);
        addRow(form, gbc, 7, "Trạng thái", cbStatus, null);

        return form;
    }

    @Override
    protected boolean validateForm() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            setError("Tên lớp không được để trống.");
            tfName.requestFocus();
            return false;
        }

        Course course = (Course) cbCourse.getSelectedItem();
        if (course == null) {
            setError("Vui lòng chọn khóa học.");
            cbCourse.requestFocus();
            return false;
        }

        try {
            LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            setError("Ngày bắt đầu không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfStartDate.requestFocus();
            return false;
        }

        String endTxt = tfEndDate.getText().trim();
        if (!endTxt.isEmpty()) {
            try {
                LocalDate.parse(endTxt, DATE_FMT);
            } catch (DateTimeParseException ex) {
                setError("Ngày kết thúc không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfEndDate.requestFocus();
                return false;
            }
        }

        String maxTxt = tfMaxStudent.getText().trim();
        if (!maxTxt.isEmpty()) {
            try {
                int maxStudent = Integer.parseInt(maxTxt);
                if (maxStudent <= 0) {
                    setError("Số học viên tối đa phải là số dương.");
                    tfMaxStudent.requestFocus();
                    return false;
                }
            } catch (NumberFormatException ex) {
                setError("Số học viên tối đa phải là số nguyên.");
                tfMaxStudent.requestFocus();
                return false;
            }
        }

        return true;
    }

    @Override
    protected void commitToEntity() {
        clazz.setClassName(tfName.getText().trim());
        clazz.setCourse((Course) cbCourse.getSelectedItem());
        clazz.setTeacher((Teacher) cbTeacher.getSelectedItem());
        clazz.setRoom((Room) cbRoom.getSelectedItem());

        try {
            clazz.setStartDate(LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT));
        } catch (DateTimeParseException ignored) {}

        String endTxt = tfEndDate.getText().trim();
        if (!endTxt.isEmpty()) {
            try {
                clazz.setEndDate(LocalDate.parse(endTxt, DATE_FMT));
            } catch (DateTimeParseException ignored) {}
        }

        String maxTxt = tfMaxStudent.getText().trim();
        if (!maxTxt.isEmpty()) {
            try {
                clazz.setMaxStudent(Integer.parseInt(maxTxt));
            } catch (NumberFormatException ignored) {}
        }

        clazz.setStatus(((ClassStatus) cbStatus.getSelectedItem()).getValue());
    }

    @Override
    public Class getEntity() {
        return clazz;
    }

    private JPanel buildDateRow(JSpinner spinner, JTextField tf) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        spinner.setPreferredSize(new Dimension(64, 34));
        JLabel calIcon = new JLabel("📅");
        calIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        row.add(calIcon);
        row.add(spinner);
        JLabel orLbl = new JLabel("hoặc nhập:");
        orLbl.setFont(Theme.FONT_SMALL);
        orLbl.setForeground(Theme.TEXT_MUTED);
        row.add(orLbl);
        row.add(tf);
        return row;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String labelText, JComponent field, JComponent extra) {
        gbc.gridy = row * 3;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        panel.add(ComponentFactory.formLabel(labelText), gbc);

        gbc.gridy = row * 3 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        if (!(field instanceof JPanel)) field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);

        if (extra != null) {
            gbc.gridy = row * 3 + 2;
            gbc.insets = new Insets(2, 0, 0, 0);
            panel.add(extra, gbc);
        }
    }

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel(
                localDateToDate(initial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner sp = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(sp, "dd/MM");
        sp.setEditor(editor);
        sp.setFont(Theme.FONT_PLAIN);
        return sp;
    }

    private Date localDateToDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate spinnerToLocalDate(JSpinner sp) {
        Date d = (Date) sp.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

