package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Room;
import com.company.ems.model.Teacher;
import com.company.ems.model.enums.ClassStatus;

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
public class ClassFormDialog extends JDialog {

    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER= new Color(29, 78, 216);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color INFO_COLOR   = new Color(59, 130, 246);
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField        tfName;
    private final JComboBox<Course>  cbCourse;
    private final JComboBox<Teacher> cbTeacher;
    private final JComboBox<Room>    cbRoom;

    // ── Date inputs: JSpinner (calendar picker) + JTextField (manual) side by side ──
    private final JSpinner   spinnerStart;
    private final JSpinner   spinnerEnd;
    private final JTextField tfStartDate;
    private final JTextField tfEndDate;

    private final JTextField    tfMaxStudent;
    private final JComboBox<ClassStatus> cbStatus;
    // Hint label shown when endDate is auto-computed
    private final JLabel        lblEndHint;

    private boolean saved = false;
    private final Class clazz;

    // Guard to avoid recursive listener calls
    private boolean updatingDates = false;

    public ClassFormDialog(Frame owner,
                           Class existing,
                           List<Course> courses,
                           List<Teacher> teachers,
                           List<Room> rooms) {
        super(owner, existing != null ? "Sửa lớp học" : "Thêm lớp học mới", true);
        this.clazz = existing != null ? existing : new Class();
        boolean isEdit = existing != null;

        tfName = createField(isEdit && existing.getClassName() != null ? existing.getClassName() : "");

        cbCourse = new JComboBox<>(courses.toArray(new Course[0]));
        cbCourse.setFont(FONT_MAIN);
        if (isEdit && existing.getCourse() != null) {
            Long tid = existing.getCourse().getCourseId();
            for (int i = 0; i < cbCourse.getItemCount(); i++) {
                Course c = cbCourse.getItemAt(i);
                if (c != null && tid.equals(c.getCourseId())) { cbCourse.setSelectedIndex(i); break; }
            }
        }

        cbTeacher = new JComboBox<>(teachers.toArray(new Teacher[0]));
        cbTeacher.setFont(FONT_MAIN);
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
        cbRoom.setFont(FONT_MAIN);
        cbRoom.insertItemAt(null, 0);
        cbRoom.setSelectedIndex(0);
        if (isEdit && existing.getRoom() != null) {
            Long rid = existing.getRoom().getRoomId();
            for (int i = 0; i < cbRoom.getItemCount(); i++) {
                Room r = cbRoom.getItemAt(i);
                if (r != null && rid.equals(r.getRoomId())) { cbRoom.setSelectedIndex(i); break; }
            }
        }

        // ── StartDate ──────────────────────────────────────────────────────
        LocalDate initStart = (isEdit && existing.getStartDate() != null)
                ? existing.getStartDate() : LocalDate.now();
        spinnerStart = buildDateSpinner(initStart);
        tfStartDate  = createField(initStart.format(DATE_FMT));
        tfStartDate.setPreferredSize(new Dimension(110, 34));

        // ── EndDate ────────────────────────────────────────────────────────
        LocalDate initEnd = (isEdit && existing.getEndDate() != null)
                ? existing.getEndDate() : null;
        spinnerEnd  = buildDateSpinner(initEnd != null ? initEnd : initStart.plusWeeks(12));
        tfEndDate   = createField(initEnd != null ? initEnd.format(DATE_FMT) : "");
        tfEndDate.setPreferredSize(new Dimension(110, 34));

        lblEndHint = new JLabel(" ");
        lblEndHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEndHint.setForeground(INFO_COLOR);

        tfMaxStudent = createField(isEdit && existing.getMaxStudent() != null
                ? String.valueOf(existing.getMaxStudent()) : "");

        cbStatus = new JComboBox<>(ClassStatus.values());
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) {
             cbStatus.setSelectedItem(ClassStatus.fromValue(existing.getStatus()));
        }

        // ── Wire listeners ─────────────────────────────────────────────────
        // Spinner → sync text field
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

        // Text field → sync spinner (on focus lost)
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

        // Course change → recalc end date
        cbCourse.addActionListener(e -> recalcEndDate());

        buildUI();
        pack();
        if (getWidth() < 680) setSize(680, getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ── Auto-compute end date ─────────────────────────────────────────────

    private void recalcEndDate() {
        Course course = (Course) cbCourse.getSelectedItem();
        LocalDate start;
        try {
            start = LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return;
        }
        if (course == null || course.getDuration() == null) return;

        int duration       = course.getDuration();
        String unit        = course.getDurationUnit() != null ? course.getDurationUnit() : "Week";
        LocalDate endDate;
        String hint;

        if ("Week".equalsIgnoreCase(unit)) {
            endDate = start.plusWeeks(duration);
            hint    = "✦ Tự động: " + duration + " tuần từ ngày bắt đầu";
        } else {
            // "Hour" — không đủ thông tin để tính ngày chính xác
            endDate = null;
            hint    = "⚠ Khoá học tính theo giờ — vui lòng nhập ngày kết thúc thủ công";
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

    // ── Build UI ──────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0 — Tên lớp
        addRow(form, gbc, 0, "Tên lớp *", tfName, null);
        // Row 1 — Khóa học
        addRow(form, gbc, 1, "Khóa học *", cbCourse, null);
        // Row 2 — Giáo viên
        addRow(form, gbc, 2, "Giáo viên", cbTeacher, null);
        // Row 3 — Phòng học
        addRow(form, gbc, 3, "Phòng học", cbRoom, null);
        // Row 4 — Ngày bắt đầu (spinner + text)
        addRow(form, gbc, 4, "Ngày bắt đầu *", buildDateRow(spinnerStart, tfStartDate), null);
        // Row 5 — Ngày kết thúc (spinner + text + hint)
        addRow(form, gbc, 5, "Ngày kết thúc", buildDateRow(spinnerEnd, tfEndDate), lblEndHint);
        // Row 6 — Số học viên tối đa
        addRow(form, gbc, 6, "Số học viên tối đa", tfMaxStudent, null);
        // Row 7 — Trạng thái
        addRow(form, gbc, 7, "Trạng thái", cbStatus, null);

        content.add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());
        JButton saveBtn = createPrimaryButton(clazz.getClassId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    /** Kết hợp spinner và text field vào một hàng ngang */
    private JPanel buildDateRow(JSpinner spinner, JTextField tf) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        spinner.setPreferredSize(new Dimension(64, 34));
        // Show calendar icon label as hint
        JLabel calIcon = new JLabel("📅");
        calIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        row.add(calIcon);
        row.add(spinner);
        JLabel orLbl = new JLabel("hoặc nhập:");
        orLbl.setFont(FONT_SMALL);
        orLbl.setForeground(TEXT_MUTED);
        row.add(orLbl);
        row.add(tf);
        return row;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String labelText, JComponent field, JComponent extra) {
        gbc.gridy  = row * 3;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 3 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        if (!(field instanceof JPanel)) field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);

        if (extra != null) {
            gbc.gridy  = row * 3 + 2;
            gbc.insets = new Insets(2, 0, 0, 0);
            panel.add(extra, gbc);
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) { showWarning("Tên lớp không được để trống."); tfName.requestFocus(); return; }

        Course course = (Course) cbCourse.getSelectedItem();
        if (course == null) { showWarning("Vui lòng chọn khóa học."); cbCourse.requestFocus(); return; }

        LocalDate startDate;
        try {
            startDate = LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            showWarning("Ngày bắt đầu không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfStartDate.requestFocus();
            return;
        }

        LocalDate endDate = null;
        String endTxt = tfEndDate.getText().trim();
        if (!endTxt.isEmpty()) {
            try {
                endDate = LocalDate.parse(endTxt, DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Ngày kết thúc không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfEndDate.requestFocus();
                return;
            }
        }

        Integer maxStudent = null;
        String maxTxt = tfMaxStudent.getText().trim();
        if (!maxTxt.isEmpty()) {
            try {
                maxStudent = Integer.parseInt(maxTxt);
                if (maxStudent <= 0) {
                    showWarning("Số học viên tối đa phải là số dương."); tfMaxStudent.requestFocus(); return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Số học viên tối đa phải là số nguyên."); tfMaxStudent.requestFocus(); return;
            }
        }

        clazz.setClassName(name);
        clazz.setCourse(course);
        clazz.setTeacher((Teacher) cbTeacher.getSelectedItem());
        clazz.setRoom((Room) cbRoom.getSelectedItem());
        clazz.setStartDate(startDate);
        clazz.setEndDate(endDate);
        clazz.setMaxStudent(maxStudent);
        clazz.setStatus(((ClassStatus) cbStatus.getSelectedItem()).getValue());

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Class getClazz()  { return clazz; }

    // ── Date spinner helpers ──────────────────────────────────────────────

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel(
                localDateToDate(initial), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner sp = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(sp, "dd/MM");
        sp.setEditor(editor);
        sp.setFont(FONT_MAIN);
        return sp;
    }

    private Date localDateToDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate spinnerToLocalDate(JSpinner sp) {
        Date d = (Date) sp.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private JTextField createField(String value) {
        JTextField tf = new JTextField(value);
        tf.setFont(FONT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return tf;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD); btn.setForeground(Color.WHITE); btn.setBackground(PRIMARY);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_HOVER); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN); btn.setForeground(TEXT_MAIN); btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(241, 245, 249)); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}

