package com.company.ems.ui.panels.student;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.InvoiceService;
import com.company.ems.service.StudentService;
import com.company.ems.ui.UI;
import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.TableStyler;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentClassPanel extends JPanel {

    private final EnrollmentService enrollmentService;
    private final ClassService classService;
    private final InvoiceService invoiceService;
    private final Student currentStudent;

    private DefaultTableModel tableModel;
    private JTable table;
    private Runnable onDataChanged;

    /** Đăng ký callback refresh khi ghi danh / hủy lớp thành công. */
    public void setOnDataChanged(Runnable r) {
        this.onDataChanged = r;
    }

    public StudentClassPanel(EnrollmentService enrollmentService, ClassService classService,
            StudentService studentService, InvoiceService invoiceService, Long studentId) {
        this.enrollmentService = enrollmentService;
        this.classService = classService;
        this.invoiceService = invoiceService;
        this.currentStudent = studentService.findById(studentId);

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        syncInvoiceWithEnrollments();
        loadData();
    }

    private void initComponents() {
        // ── Toolbar ───────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel lblTitle = new JLabel("Danh sách Lớp học của tôi");
        lblTitle.setFont(Theme.FONT_HEADER);
        lblTitle.setForeground(Theme.TEXT_MAIN);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = ComponentFactory.dangerButton("❌ Hủy lớp đang chọn");
        btnCancel.addActionListener(e -> handleCancelEnrollment());

        JButton btnRegister = ComponentFactory.primaryButton("+ Đăng ký lớp mới");
        btnRegister.addActionListener(e -> showRegisterDialog());

        btnPanel.add(btnCancel);
        btnPanel.add(btnRegister);

        toolbar.add(lblTitle, BorderLayout.WEST);
        toolbar.add(btnPanel, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────
        String[] cols = { "ID", "STT", "Mã Lớp", "Tên Lớp", "Khóa học", "Học phí", "Ngày KG", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        TableStyler.applyDefaults(table);
        TableStyler.fitTableColumns(table);
        TableStyler.hideColumn(table, 0);

        UI.alignColumn(table, 1, SwingConstants.LEFT);
        UI.alignColumn(table, 5, SwingConstants.RIGHT);

        add(TableStyler.scrollPane(table), BorderLayout.CENTER);
    }

    // ── Invoice sync ──────────────────────────────────────────────────────

    private void syncInvoiceWithEnrollments() {
        if (currentStudent == null)
            return;

        List<Enrollment> enrolledList = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                        && e.getStudent().getStudentId().equals(currentStudent.getStudentId())
                        && "Đã đăng ký".equals(e.getStatus()))
                .toList();

        BigDecimal totalFee = enrolledList.stream()
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice pendingInv = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null
                        && i.getStudent().getStudentId().equals(currentStudent.getStudentId())
                        && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        if (totalFee.compareTo(BigDecimal.ZERO) <= 0) {
            if (pendingInv != null)
                invoiceService.delete(pendingInv.getInvoiceId());
            return;
        }

        if (pendingInv != null) {
            pendingInv.setTotalAmount(totalFee);
            pendingInv.setNote("Tổng học phí cho " + enrolledList.size() + " lớp đang đăng ký.");
            invoiceService.update(pendingInv);
        } else {
            Invoice newInv = new Invoice();
            newInv.setStudent(currentStudent);
            newInv.setTotalAmount(totalFee);
            newInv.setIssueDate(LocalDate.now());
            newInv.setStatus("Chờ thanh toán");
            newInv.setNote("Học phí tổng hợp đợt đăng ký mới.");
            invoiceService.save(newInv);
        }
    }

    // ── Data ──────────────────────────────────────────────────────────────

    public void loadData() {
        tableModel.setRowCount(0);
        if (currentStudent == null)
            return;

        List<Enrollment> myEnrollments = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                        && e.getStudent().getStudentId().equals(currentStudent.getStudentId()))
                .toList();

        int[] stt = { 1 };
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        myEnrollments.forEach(e -> {
            Class c = e.getClazz();
            tableModel.addRow(new Object[] {
                    e.getEnrollmentId(), stt[0]++,
                    "CLS" + c.getClassId(),
                    c.getClassName(),
                    c.getCourse().getCourseName(),
                    String.format("%,.0f", c.getCourse().getFee()),
                    c.getStartDate() != null ? c.getStartDate().format(fmt) : "",
                    e.getStatus()
            });
        });
        SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
    }

    // ── Register dialog ───────────────────────────────────────────────────

    private void showRegisterDialog() {
        if (currentStudent == null || !"Hoạt động".equals(currentStudent.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Tài khoản của bạn đang bị khóa. Không thể đăng ký lớp học.",
                    "Tài khoản không hoạt động", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Long> myClassIds = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null
                        && e.getStudent().getStudentId().equals(currentStudent.getStudentId()))
                .map(e -> e.getClazz().getClassId()).toList();

        java.util.Map<Long, Long> enrollCounts = enrollmentService.findAll().stream()
                .filter(e -> e.getClazz() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.getClazz().getClassId(), java.util.stream.Collectors.counting()));

        List<Class> availableClasses = classService.findAll().stream()
                .filter(c -> "Mở lớp".equals(c.getStatus())
                        && !myClassIds.contains(c.getClassId())
                        && (c.getMaxStudent() == null || c.getMaxStudent() <= 0
                                || enrollCounts.getOrDefault(c.getClassId(), 0L) < c.getMaxStudent()))
                .toList();

        if (availableClasses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có lớp nào đang mở để đăng ký!");
            return;
        }

        java.util.Map<Course, List<Class>> courseMap = availableClasses.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Class::getCourse, java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        List<Course> courseList = new java.util.ArrayList<>(courseMap.keySet());

        // ── Dialog ────────────────────────────────────────────────────────
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Đăng ký Khóa học", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.BG_CARD);

        // Header
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Theme.PRIMARY);
        headerBar.setBorder(new EmptyBorder(14, 22, 14, 22));
        JLabel lblDialogTitle = new JLabel("Chọn Khóa học  →  Chọn Lớp học");
        lblDialogTitle.setFont(Theme.FONT_TITLE);
        lblDialogTitle.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel(courseList.size() + " khóa học  ·  " + availableClasses.size() + " lớp trống");
        lblSub.setFont(Theme.FONT_SMALL);
        lblSub.setForeground(Theme.TEXT_SUB);
        headerBar.add(lblDialogTitle, BorderLayout.WEST);
        headerBar.add(lblSub, BorderLayout.EAST);
        dialog.add(headerBar, BorderLayout.NORTH);

        // LEFT: Course table
        String[] courseCols = { "_id", "Tên Khóa học", "Cấp độ", "Học phí (VND)", "Số lớp trống" };
        DefaultTableModel courseModel = new DefaultTableModel(courseCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        courseList.forEach(c -> courseModel.addRow(new Object[] {
                c.getCourseId(), c.getCourseName(),
                c.getLevel() != null ? c.getLevel() : "—",
                String.format("%,.0f", c.getFee()),
                courseMap.get(c).size() + " lớp"
        }));
        JTable courseTbl = new JTable(courseModel);
        TableStyler.applyDefaults(courseTbl);
        courseTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.fitTableColumns(courseTbl);
        TableStyler.hideColumn(courseTbl, 0);

        JPanel leftHeader = buildSectionHeader("📚  Khóa học",
                BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER));
        JScrollPane courseScroll = new JScrollPane(courseTbl);
        courseScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        courseScroll.getViewport().setBackground(Theme.BG_CARD);
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        leftPanel.add(courseScroll, BorderLayout.CENTER);

        // RIGHT: Class table
        String[] classCols = { "_id", "Tên Lớp", "Ngày khai giảng", "Ngày kết thúc", "Phòng", "Sĩ số" };
        DefaultTableModel classModel = new DefaultTableModel(classCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable classTbl = new JTable(classModel);
        TableStyler.applyDefaults(classTbl);
        classTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.fitTableColumns(classTbl);
        TableStyler.hideColumn(classTbl, 0);

        JPanel rightCards = new JPanel(new CardLayout());
        rightCards.add(ComponentFactory.emptyState("← Chọn một khóa học bên trái để xem danh sách lớp"), "placeholder");
        JScrollPane classScroll = TableStyler.scrollPaneNoBorder(classTbl);
        classScroll.getViewport().setBackground(Theme.BG_CARD);
        rightCards.add(classScroll, "table");
        ((CardLayout) rightCards.getLayout()).show(rightCards, "placeholder");

        JLabel lblRight = new JLabel("🏫  Lớp học");
        lblRight.setFont(Theme.FONT_BOLD);
        lblRight.setForeground(Theme.TEXT_MAIN);
        JPanel rightHeader = buildSectionHeader("🏫  Lớp học",
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        // keep a reference to the label inside header to update it
        JLabel rightHeaderLabel = (JLabel) rightHeader.getComponent(0);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        rightPanel.add(rightCards, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(380);
        split.setDividerSize(4);
        split.setBorder(null);
        dialog.add(split, BorderLayout.CENTER);

        @SuppressWarnings("unchecked")
        final List<Class>[] currentClasses = new List[] { java.util.Collections.emptyList() };
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Click course → update class table
        courseTbl.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting())
                return;
            int sel = courseTbl.getSelectedRow();
            if (sel < 0)
                return;
            Course selectedCourse = courseList.get(sel);
            List<Class> classes = courseMap.get(selectedCourse);
            currentClasses[0] = classes;
            classModel.setRowCount(0);
            classes.forEach(c -> {
                long cur = enrollCounts.getOrDefault(c.getClassId(), 0L);
                String slot = (c.getMaxStudent() != null && c.getMaxStudent() > 0)
                        ? cur + "/" + c.getMaxStudent()
                        : cur + "/∞";
                classModel.addRow(new Object[] {
                        c.getClassId(), c.getClassName(),
                        c.getStartDate() != null ? c.getStartDate().format(fmt) : "—",
                        c.getEndDate() != null ? c.getEndDate().format(fmt) : "—",
                        c.getRoom() != null ? c.getRoom().getRoomName() : "—",
                        slot
                });
            });
            ((CardLayout) rightCards.getLayout()).show(rightCards, "table");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(classTbl, 80, 260));
            rightHeaderLabel.setText("🏫  Lớp học  —  " + selectedCourse.getCourseName());
        });

        // Double-click → enroll immediately
        classTbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && classTbl.getSelectedRow() >= 0) {
                    int row = classTbl.getSelectedRow();
                    dialog.dispose();
                    doEnroll(currentClasses[0].get(row));
                }
            }
        });

        // Footer
        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        hintBar.setBackground(Theme.BG_HEADER);
        JLabel lblHint = new JLabel("💡  Nhấp đúp vào lớp học để đăng ký ngay, hoặc chọn rồi bấm Đăng ký");
        lblHint.setFont(Theme.FONT_SMALL);
        lblHint.setForeground(Theme.TEXT_MUTED);
        hintBar.add(lblHint);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setBackground(Theme.BG_CARD);

        JButton btnClose = ComponentFactory.secondaryButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());

        JButton btnOk = ComponentFactory.primaryButton("  Đăng ký  ");
        btnOk.addActionListener(e -> {
            int row = classTbl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn một khóa học rồi chọn một lớp học!");
                return;
            }
            dialog.dispose();
            doEnroll(currentClasses[0].get(row));
        });
        btnBar.add(btnClose);
        btnBar.add(btnOk);

        JPanel footerWrap = new JPanel(new BorderLayout());
        footerWrap.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        footerWrap.add(hintBar, BorderLayout.WEST);
        footerWrap.add(btnBar, BorderLayout.EAST);
        dialog.add(footerWrap, BorderLayout.SOUTH);

        dialog.setSize(1040, 560);
        dialog.setMinimumSize(new Dimension(820, 450));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Tạo section header cho left/right panel trong dialog. */
    private JPanel buildSectionHeader(String text, javax.swing.border.Border outerBorder) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
                outerBorder,
                new EmptyBorder(8, 16, 8, 16)));
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_MAIN);
        header.add(lbl);
        return header;
    }

    /** Thực hiện ghi danh sau khi user xác nhận. */
    private void doEnroll(Class selected) {
        long curCount = enrollmentService.findAll().stream()
                .filter(e -> e.getClazz() != null
                        && e.getClazz().getClassId().equals(selected.getClassId()))
                .count();
        if (selected.getMaxStudent() != null && selected.getMaxStudent() > 0
                && curCount >= selected.getMaxStudent()) {
            JOptionPane.showMessageDialog(this,
                    "<html>Lớp <b>" + selected.getClassName() + "</b> đã đầy ("
                            + curCount + "/" + selected.getMaxStudent() + " học viên).<br>"
                            + "Vui lòng chọn lớp khác.</html>",
                    "Lớp đã đầy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Enrollment e = new Enrollment();
        e.setStudent(currentStudent);
        e.setClazz(selected);
        e.setEnrollmentDate(LocalDate.now());
        e.setStatus("Đã đăng ký");
        e.setResult("Chưa có");
        enrollmentService.save(e);

        syncInvoiceWithEnrollments();
        if (onDataChanged != null)
            onDataChanged.run();
        else
            loadData();
        JOptionPane.showMessageDialog(this,
                "<html>✅  Đăng ký thành công!<br>Lớp: <b>" + selected.getClassName()
                        + "</b><br>Học phí đã được cập nhật vào tab Thanh toán.</html>",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleCancelEnrollment() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lớp muốn hủy!");
            return;
        }

        Long envId = (Long) tableModel.getValueAt(row, 0);
        Enrollment env = enrollmentService.findById(envId);

        if ("Đã thanh toán".equals(env.getStatus()) || "Hoàn thành".equals(env.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Lớp \"" + (env.getClazz() != null ? env.getClazz().getClassName() : "")
                            + "\" đã thanh toán — không thể hủy!",
                    "Không thể hủy", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int res = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy đăng ký lớp này?",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            enrollmentService.delete(envId);
            syncInvoiceWithEnrollments();
            if (onDataChanged != null)
                onDataChanged.run();
            else
                loadData();
            JOptionPane.showMessageDialog(this, "Đã hủy thành công!");
        }
    }
}
