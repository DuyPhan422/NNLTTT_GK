package com.company.ems.ui.panels;

import com.company.ems.model.Class;
import com.company.ems.model.Enrollment;
import com.company.ems.model.Student;
import com.company.ems.model.enums.EnrollmentResult;
import com.company.ems.model.enums.EnrollmentStatus;
import com.company.ems.service.StudentService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnrollmentFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Design tokens (Đồng bộ)
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY      = new Color(37, 99, 235);
    private static final Color PRIMARY_HOVER= new Color(29, 78, 216);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color DANGER       = new Color(220, 38, 38);
    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);

    private final JTextField tfStudentCode;
    private final JLabel lblStudentName;
    private final JComboBox<Class> cbClass;
    private final JTextField tfEnrollmentDate;
    private final JComboBox<EnrollmentStatus> cbStatus;
    private final JComboBox<EnrollmentResult> cbResult;

    private boolean saved = false;
    private final boolean isEdit;
    private final boolean studentLocked;
    private final Enrollment enrollment;
    private final StudentService studentService;
    private Student currentStudent;
    private final List<Enrollment> allEnrollments;

    /** Convenience: no locked student (student field is free-entry). */
    public EnrollmentFormDialog(Frame owner, Enrollment existing, StudentService studentService,
                                List<Class> classes, List<Enrollment> allEnrollments) {
        this(owner, existing, studentService, classes, allEnrollments, null);
    }

    /**
     * @param lockedStudent Pre-fill and lock the student field.
     *                      Pass null to allow free text entry.
     */
    public EnrollmentFormDialog(Frame owner, Enrollment existing, StudentService studentService,
                                List<Class> classes, List<Enrollment> allEnrollments,
                                Student lockedStudent) {
	super(owner, existing != null ? "Sửa Ghi danh" : "Thêm Ghi danh mới", true);
	this.enrollment = existing != null ? existing : new Enrollment();
	this.studentService = studentService;
	this.allEnrollments = allEnrollments;
        this.isEdit = existing != null;
        this.studentLocked = lockedStudent != null;
        this.currentStudent = lockedStudent != null ? lockedStudent
                            : (existing != null ? existing.getStudent() : null);

        boolean isEdit = this.isEdit;

        // Ô nhập Mã Học Viên
        String prefillCode = lockedStudent != null
                ? String.format("HV%04d", lockedStudent.getStudentId())
                : (isEdit && existing.getStudent() != null
                        ? String.format("HV%04d", existing.getStudent().getStudentId()) : "");
        tfStudentCode = createField(prefillCode);
        if (studentLocked) {
            tfStudentCode.setEditable(false);
            tfStudentCode.setBackground(new Color(241, 245, 249));
        }

        String prefillName = lockedStudent != null ? "✅ " + lockedStudent.getFullName()
                : (isEdit && existing.getStudent() != null
                        ? "✅ " + existing.getStudent().getFullName()
                        : "Nhập mã để kiểm tra (VD: HV0001, 1)");
        lblStudentName = new JLabel(prefillName);
        lblStudentName.setFont(FONT_SMALL);
        lblStudentName.setForeground((studentLocked || isEdit) ? new Color(22, 163, 74) : TEXT_MUTED);

        if (!studentLocked) {
            tfStudentCode.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { checkStudentCode(); }
            });
        }

        // Combo Môn học — filter already-enrolled and full classes when student is locked (add mode)
        List<Class> availableClasses = classes;
        if (studentLocked && lockedStudent != null) {
            Set<Long> enrolledClassIds = allEnrollments.stream()
                    .filter(e -> e.getStudent() != null
                              && e.getStudent().getStudentId().equals(lockedStudent.getStudentId()))
                    .map(e -> e.getClazz().getClassId())
                    .collect(Collectors.toSet());
            // Count active enrollments per class to detect full classes
            Map<Long, Long> countPerClass = allEnrollments.stream()
                    .filter(e -> e.getClazz() != null)
                    .collect(Collectors.groupingBy(e -> e.getClazz().getClassId(), Collectors.counting()));
            availableClasses = classes.stream()
                    .filter(c -> !enrolledClassIds.contains(c.getClassId()))
                    .filter(c -> c.getMaxStudent() == null || c.getMaxStudent() <= 0
                              || countPerClass.getOrDefault(c.getClassId(), 0L) < c.getMaxStudent())
                    .collect(Collectors.toList());
        }
        final Map<Long, Long> classCounts = allEnrollments.stream()
                .filter(e -> e.getClazz() != null)
                .collect(Collectors.groupingBy(e -> e.getClazz().getClassId(), Collectors.counting()));
        cbClass = new JComboBox<>(availableClasses.toArray(new Class[0]));
        cbClass.setFont(FONT_MAIN);
        cbClass.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof com.company.ems.model.Class) {
                    com.company.ems.model.Class c = (com.company.ems.model.Class) value;
                    String course = (c.getCourse() != null) ? c.getCourse().getCourseName() : c.getClassName();
                    long cur = classCounts.getOrDefault(c.getClassId(), 0L);
                    String slot = (c.getMaxStudent() != null && c.getMaxStudent() > 0)
                            ? cur + "/" + c.getMaxStudent() + " HV"
                            : cur + " HV";
                    setText(course + "  (\u2022 L\u1EDBp " + c.getClassName() + "  |  " + slot + ")");
                }
                return this;
            }
        });
        if (isEdit && existing.getClazz() != null) cbClass.setSelectedItem(existing.getClazz());

        tfEnrollmentDate = createField(isEdit && existing.getEnrollmentDate() != null
                ? existing.getEnrollmentDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT));

        cbStatus = new JComboBox<>(EnrollmentStatus.values());
        cbStatus.setFont(FONT_MAIN);
        if (isEdit && existing.getStatus() != null) cbStatus.setSelectedItem(EnrollmentStatus.fromValue(existing.getStatus()));

        cbResult = new JComboBox<>(EnrollmentResult.values());
        cbResult.setFont(FONT_MAIN);
        if (isEdit && existing.getResult() != null) cbResult.setSelectedItem(EnrollmentResult.fromValue(existing.getResult()));

        buildUI();
        pack();
        if (getWidth() < 500) setSize(500, getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void checkStudentCode() {
        String code = tfStudentCode.getText().trim();
        if (code.isEmpty()) {
            lblStudentName.setText("Vui lòng nhập mã Học viên!");
            lblStudentName.setForeground(DANGER);
            currentStudent = null;
            return;
        }
        try {
            // Lọc lấy số (HV0001 -> 1)
            Long id = Long.parseLong(code.replaceAll("[^0-9]", ""));
            Student s = studentService.findById(id);
            if (s != null) {
                if (!"Hoạt động".equals(s.getStatus())) {
                    lblStudentName.setText("⛔ Tài khoản học viên \"" + s.getFullName() + "\" đang bị khóa!");
                    lblStudentName.setForeground(DANGER);
                    currentStudent = null;
                    return;
                }
                lblStudentName.setText("✅ " + s.getFullName());
                lblStudentName.setForeground(new Color(22, 163, 74)); // Xanh lá
                currentStudent = s;
            } else {
                lblStudentName.setText("❌ Không tìm thấy Học viên trong hệ thống!");
                lblStudentName.setForeground(DANGER);
                currentStudent = null;
            }
        } catch (Exception ex) {
            lblStudentName.setText("❌ Mã không hợp lệ!");
            lblStudentName.setForeground(DANGER);
            currentStudent = null;
        }
    }

    private void buildUI() {
        if (isEdit) buildEditUI(); else buildAddUI();
    }

    private void buildEditUI() {
        // ── Navy header: student name + class info ────────────────────
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        String stuName = currentStudent != null ? currentStudent.getFullName() : "";
        String stuCode = currentStudent != null
                ? String.format("HV%04d", currentStudent.getStudentId()) : "";
        JLabel nameLbl = new JLabel(stuName + "  |  " + stuCode);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(Color.WHITE);

        String classInfo = "";
        if (enrollment.getClazz() != null) {
            String course = enrollment.getClazz().getCourse() != null
                    ? enrollment.getClazz().getCourse().getCourseName()
                    : enrollment.getClazz().getClassName();
            classInfo = course + "  \u2022  " + enrollment.getClazz().getClassName();
        }
        JLabel classLbl = new JLabel(classInfo.isEmpty() ? " " : classInfo);
        classLbl.setFont(FONT_SMALL);
        classLbl.setForeground(new Color(148, 163, 184));

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(nameLbl);
        headerText.add(Box.createVerticalStrut(5));
        headerText.add(classLbl);
        header.add(headerText, BorderLayout.CENTER);

        // ── Form: only the 3 editable fields ─────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG_CARD);
        body.setBorder(BorderFactory.createEmptyBorder(22, 24, 8, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        addRow(body, gbc, 0, "Ng\u00e0y ghi danh (dd/MM/yyyy) *", tfEnrollmentDate);
        addRow(body, gbc, 1, "Tr\u1ea1ng th\u00e1i", cbStatus);
        addRow(body, gbc, 2, "K\u1ebft qu\u1ea3", cbResult);

        // ── Button bar ────────────────────────────────────────────────
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setBackground(BG_CARD);
        btnBar.setBorder(BorderFactory.createEmptyBorder(12, 24, 16, 24));
        JButton cancelBtn = createSecondaryButton("H\u1ee7y");
        cancelBtn.addActionListener(e -> dispose());
        JButton saveBtn = createPrimaryButton("L\u01b0u thay \u0111\u1ed5i");
        saveBtn.addActionListener(e -> onSave());
        btnBar.add(cancelBtn);
        btnBar.add(saveBtn);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.add(header, BorderLayout.NORTH);
        content.add(body,   BorderLayout.CENTER);
        content.add(btnBar, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void buildAddUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JPanel pnlStudent = new JPanel(new BorderLayout(8, 0));
        pnlStudent.setOpaque(false);
        pnlStudent.add(tfStudentCode, BorderLayout.CENTER);
        if (!studentLocked) {
            JButton btnCheck = createSecondaryButton("Ki\u1ec3m tra");
            btnCheck.addActionListener(e -> checkStudentCode());
            pnlStudent.add(btnCheck, BorderLayout.EAST);
        }

        addRowWithLabel(form, gbc, 0, "M\u00e3 H\u1ecdc vi\u00ean *", pnlStudent, lblStudentName);
        addRow(form, gbc, 1, "M\u00f4n h\u1ecdc *", cbClass);
        addRow(form, gbc, 2, "Ng\u00e0y ghi danh (dd/MM/yyyy) *", tfEnrollmentDate);

        JLabel infoNote = new JLabel(
            "\u2139\ufe0f Tr\u1ea1ng th\u00e1i: \u0110\u00e3 \u0111\u0103ng k\u00fd  \u00b7  "
            + "H\u00f3a \u0111\u01a1n h\u1ecdc ph\u00ed s\u1ebd \u0111\u01b0\u1ee3c t\u1ea1o t\u1ef1 \u0111\u1ed9ng.");
        infoNote.setFont(FONT_SMALL);
        infoNote.setForeground(new Color(37, 99, 235));
        gbc.gridy  = 9;
        gbc.insets = new Insets(14, 0, 0, 0);
        form.add(infoNote, gbc);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        JButton cancelBtn = createSecondaryButton("H\u1ee7y");
        cancelBtn.addActionListener(e -> dispose());
        JButton saveBtn = createPrimaryButton("Th\u00eam m\u1edbi");
        saveBtn.addActionListener(e -> onSave());
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void onSave() {
        if (isEdit) {
            // Edit mode: student & class are fixed, only validate date
            LocalDate enDate;
            try {
                enDate = LocalDate.parse(tfEnrollmentDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Ng\u00e0y ghi danh kh\u00f4ng h\u1ee3p l\u1ec7 (dd/MM/yyyy).");
                tfEnrollmentDate.requestFocus();
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "L\u01b0u thay \u0111\u1ed5i cho h\u1ecdc vi\u00ean \u201c"
                    + (currentStudent != null ? currentStudent.getFullName() : "") + "\u201d?",
                    "X\u00e1c nh\u1eadn", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            enrollment.setEnrollmentDate(enDate);
            enrollment.setStatus(((EnrollmentStatus) cbStatus.getSelectedItem()).getValue());
            enrollment.setResult(((EnrollmentResult) cbResult.getSelectedItem()).getValue());
        } else {
            // Add mode: validate student, class, date, no duplicate
            if (!studentLocked) checkStudentCode();
            if (currentStudent == null) {
                showWarning("Vui l\u00f2ng nh\u1eadp m\u00e3 H\u1ecDc vi\u00ean h\u1ee3p l\u1ec7 v\u00e0 ki\u1ec3m tra.");
                tfStudentCode.requestFocus();
                return;
            }
            Class clazz = (Class) cbClass.getSelectedItem();
            if (clazz == null) {
                showWarning("Vui l\u00f2ng ch\u1ecdn l\u1edbp h\u1ecdc.");
                cbClass.requestFocus();
                return;
            }
            // Final full-class guard
            long curCount = allEnrollments.stream()
                    .filter(e -> e.getClazz() != null && e.getClazz().getClassId().equals(clazz.getClassId()))
                    .count();
            if (clazz.getMaxStudent() != null && clazz.getMaxStudent() > 0 && curCount >= clazz.getMaxStudent()) {
                showWarning("L\u1EDBp " + clazz.getClassName() + " \u0111\u00e3 \u0111\u1ea7y ("
                    + curCount + "/" + clazz.getMaxStudent() + " h\u1ecdc vi\u00ean).");
                return;
            }
            boolean isDuplicate = allEnrollments.stream()
                    .anyMatch(e -> e.getStudent() != null && e.getClazz() != null
                               && e.getStudent().getStudentId().equals(currentStudent.getStudentId())
                               && e.getClazz().getClassId().equals(clazz.getClassId()));
            if (isDuplicate) {
                showWarning("H\u1ecdc vi\u00ean " + currentStudent.getFullName()
                    + " \u0111\u00e3 c\u00f3 trong l\u1edbp " + clazz.getClassName() + " r\u1ed3i!");
                return;
            }
            LocalDate enDate;
            try {
                enDate = LocalDate.parse(tfEnrollmentDate.getText().trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                showWarning("Ng\u00e0y ghi danh kh\u00f4ng h\u1ee3p l\u1ec7 (dd/MM/yyyy).");
                tfEnrollmentDate.requestFocus();
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Th\u00eam ghi danh cho h\u1ecdc vi\u00ean \u201c" + currentStudent.getFullName() + "\u201d?",
                    "X\u00e1c nh\u1eadn", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            enrollment.setStudent(currentStudent);
            enrollment.setClazz(clazz);
            enrollment.setEnrollmentDate(enDate);
            enrollment.setStatus(EnrollmentStatus.DA_DANG_KY.getValue());
            enrollment.setResult(EnrollmentResult.CHUA_CO.getValue());
        }
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Enrollment getEnrollment() { return enrollment; }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy  = row * 3;
        gbc.insets = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy  = row * 3 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(0, 36));
        panel.add(field, gbc);
    }

    private void addRowWithLabel(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field, JLabel hint) {
        addRow(panel, gbc, row, label, field);
        gbc.gridy  = row * 3 + 2;
        gbc.insets = new Insets(2, 0, 0, 0);
        panel.add(hint, gbc);
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
