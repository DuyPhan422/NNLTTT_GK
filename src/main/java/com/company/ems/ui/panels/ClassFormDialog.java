package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Room;
import com.company.ems.model.Teacher;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField tfName;
    private final JComboBox<Course> cbCourse;
    private final JComboBox<Teacher> cbTeacher;
    private final JComboBox<Room> cbRoom;
    private final JTextField tfStartDate;
    private final JTextField tfEndDate;
    private final JTextField tfMaxStudent;
    private final JComboBox<String> cbStatus;

    private boolean saved = false;
    private final Class clazz;

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
            Long targetId = existing.getCourse().getCourseId();
            for (int i = 0; i < cbCourse.getItemCount(); i++) {
                Course c = cbCourse.getItemAt(i);
                if (c != null && targetId.equals(c.getCourseId())) { cbCourse.setSelectedIndex(i); break; }
            }
        }

        cbTeacher = new JComboBox<>(teachers.toArray(new Teacher[0]));
        cbTeacher.setFont(FONT_MAIN);
        if (isEdit && existing.getTeacher() != null) {
            Long targetId = existing.getTeacher().getTeacherId();
            for (int i = 0; i < cbTeacher.getItemCount(); i++) {
                Teacher tc = cbTeacher.getItemAt(i);
                if (tc != null && targetId.equals(tc.getTeacherId())) { cbTeacher.setSelectedIndex(i); break; }
            }
        }

        cbRoom = new JComboBox<>(rooms.toArray(new Room[0]));
        cbRoom.setFont(FONT_MAIN);
        cbRoom.insertItemAt(null, 0);
        cbRoom.setSelectedIndex(0);
        if (isEdit && existing.getRoom() != null) {
            Long targetId = existing.getRoom().getRoomId();
            for (int i = 0; i < cbRoom.getItemCount(); i++) {
                Room r = cbRoom.getItemAt(i);
                if (r != null && targetId.equals(r.getRoomId())) { cbRoom.setSelectedIndex(i); break; }
            }
        }

        tfStartDate = createField(isEdit && existing.getStartDate() != null
                ? existing.getStartDate().format(DATE_FMT) : "");
        tfEndDate = createField(isEdit && existing.getEndDate() != null
                ? existing.getEndDate().format(DATE_FMT) : "");

        tfMaxStudent = createField(isEdit && existing.getMaxStudent() != null
                ? String.valueOf(existing.getMaxStudent()) : "");

        cbStatus = new JComboBox<>(new String[]{"Lên kế hoạch", "Mở lớp", "Đang diễn ra", "Hoàn thành", "Hủy lớp"});
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) {
            cbStatus.setSelectedItem(existing.getStatus());
        }

        buildUI();
        pack();
        if (getWidth() < 640) {
            setSize(640, getHeight());
        }
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addRow(form, gbc, 0, "Tên lớp *", tfName);
        addRow(form, gbc, 1, "Khóa học *", cbCourse);
        addRow(form, gbc, 2, "Giáo viên", cbTeacher);
        addRow(form, gbc, 3, "Phòng học", cbRoom);
        addRow(form, gbc, 4, "Ngày bắt đầu (dd/MM/yyyy) *", tfStartDate);
        addRow(form, gbc, 5, "Ngày kết thúc (dd/MM/yyyy)", tfEndDate);
        addRow(form, gbc, 6, "Số học viên tối đa", tfMaxStudent);
        addRow(form, gbc, 7, "Trạng thái", cbStatus);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createPrimaryButton(clazz.getClassId() != null ? "Lưu thay đổi" : "Thêm mới");
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Tên lớp không được để trống.");
            tfName.requestFocus();
            return;
        }

        Course course = (Course) cbCourse.getSelectedItem();
        if (course == null) {
            showWarning("Vui lòng chọn khóa học.");
            cbCourse.requestFocus();
            return;
        }

        LocalDate startDate;
        try {
            startDate = LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            showWarning("Ngày bắt đầu không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
            tfStartDate.requestFocus();
            return;
        }

        LocalDate endDate = null;
        if (!tfEndDate.getText().trim().isEmpty()) {
            try {
                endDate = LocalDate.parse(tfEndDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Ngày kết thúc không hợp lệ. Vui lòng nhập dd/MM/yyyy.");
                tfEndDate.requestFocus();
                return;
            }
        }

        Integer maxStudent = null;
        if (!tfMaxStudent.getText().trim().isEmpty()) {
            try {
                maxStudent = Integer.parseInt(tfMaxStudent.getText().trim());
                if (maxStudent <= 0) {
                    showWarning("Số học viên tối đa phải là số dương.");
                    tfMaxStudent.requestFocus();
                    return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Số học viên tối đa phải là số nguyên.");
                tfMaxStudent.requestFocus();
                return;
            }
        }

        clazz.setClassName(name);
        clazz.setCourse(course);
        clazz.setTeacher((Teacher) cbTeacher.getSelectedItem());
        clazz.setRoom((Room) cbRoom.getSelectedItem());
        clazz.setStartDate(startDate);
        clazz.setEndDate(endDate);
        clazz.setMaxStudent(maxStudent != null ? maxStudent : 0);
        clazz.setStatus((String) cbStatus.getSelectedItem());

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Class getClazz()  { return clazz; }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 2;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(150, 36));
        panel.add(field, gbc);
    }

    private JTextField createField(String value) {
        JTextField tf = new JTextField(value);
        tf.setFont(FONT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return tf;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_HOVER); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(241, 245, 249)); }
            public void mouseExited (java.awt.event.MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}

