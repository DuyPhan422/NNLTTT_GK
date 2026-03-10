package com.company.ems.ui.panels.student;

import com.company.ems.model.Class;
import com.company.ems.model.Course;
import com.company.ems.model.Enrollment;
import com.company.ems.model.Invoice;
import com.company.ems.model.Student;
import com.company.ems.service.ClassService;
import com.company.ems.service.EnrollmentService;
import com.company.ems.service.StudentService;
import com.company.ems.service.InvoiceService; // THÊM
import com.company.ems.ui.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentClassPanel extends JPanel {

    private final EnrollmentService enrollmentService;
    private final ClassService classService;
    private final StudentService studentService;
    private final InvoiceService invoiceService; // THÊM
    private final Student currentStudent;
    
    private DefaultTableModel tableModel;
    private JTable table;
    private Runnable onDataChanged;

    /** Đăng ký callback refresh khi ghi danh / hủy lớp thành công. */
    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public StudentClassPanel(EnrollmentService enrollmentService, ClassService classService, 
                             StudentService studentService, InvoiceService invoiceService, Long studentId) {
        this.enrollmentService = enrollmentService;
        this.classService = classService;
        this.studentService = studentService;
        this.invoiceService = invoiceService;
        this.currentStudent = studentService.findById(studentId);

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        syncInvoiceWithEnrollments(); // ✅ Tự động tạo/cập nhật Invoice khi load
        loadData();
    }

    private void initComponents() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel lblTitle = new JLabel("Danh sách Lớp học của tôi");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnRegister = new JButton("+ Đăng ký lớp mới");
        btnRegister.setBackground(new Color(37, 99, 235));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.addActionListener(e -> showRegisterDialog());

        JButton btnCancel = new JButton("❌ Hủy lớp đang chọn");
        btnCancel.setBackground(new Color(220, 38, 38));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.addActionListener(e -> handleCancelEnrollment());

        btnPanel.add(btnCancel);
        btnPanel.add(btnRegister);

        toolbar.add(lblTitle, BorderLayout.WEST);
        toolbar.add(btnPanel, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] cols = {"ID", "STT", "Mã Lớp", "Tên Lớp", "Khóa học", "Học phí", "Ngày KG", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Ẩn cột ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        UI.alignColumn(table, 1, SwingConstants.LEFT);
        UI.alignColumn(table, 5, SwingConstants.RIGHT); // Căn phải cột tiền

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // LOGIC QUAN TRỌNG: Đồng bộ hóa danh sách lớp vào DUY NHẤT 1 Bill nợ
    private void syncInvoiceWithEnrollments() {
        if (currentStudent == null) return;

        // 1. Tìm tất cả lớp đang ở trạng thái "Enrolled" (chưa đóng tiền/chưa học xong)
        List<Enrollment> enrolledList = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null 
                          && e.getStudent().getStudentId().equals(currentStudent.getStudentId()) 
                          && "Đã đăng ký".equals(e.getStatus()))
                .toList();

        // ✅ DEBUG LOG
        System.out.println("🔍 [SYNC INVOICE] Student ID: " + currentStudent.getStudentId() 
                         + " | Số lớp Enrolled: " + enrolledList.size());

        // 2. Tính tổng tiền học phí của các lớp này
        BigDecimal totalFee = enrolledList.stream()
                .map(e -> e.getClazz().getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("💰 [SYNC INVOICE] Tổng học phí: " + totalFee);

        // 3. Tìm hóa đơn "Chờ thanh toán" (Issued) hiện có của học viên
        Invoice pendingInv = invoiceService.findAll().stream()
                .filter(i -> i.getStudent() != null 
                          && i.getStudent().getStudentId().equals(currentStudent.getStudentId()) 
                          && "Chờ thanh toán".equals(i.getStatus()))
                .findFirst().orElse(null);

        System.out.println("📄 [SYNC INVOICE] Bill hiện tại: " + (pendingInv != null ? "CÓ (ID=" + pendingInv.getInvoiceId() + ")" : "CHƯA CÓ"));

        // Trường hợp không còn lớp nào (hủy hết)
        if (totalFee.compareTo(BigDecimal.ZERO) <= 0) {
            if (pendingInv != null) {
                invoiceService.delete(pendingInv.getInvoiceId());
                System.out.println("🗑️ [SYNC INVOICE] Đã xóa Bill vì không còn lớp nào.");
            }
            return;
        }

        if (pendingInv != null) {
            // NẾU ĐÃ CÓ BILL NỢ: Cập nhật lại số tiền tổng mới
            pendingInv.setTotalAmount(totalFee);
            pendingInv.setNote("Tổng học phí cho " + enrolledList.size() + " lớp đang đăng ký.");
            invoiceService.update(pendingInv);
            System.out.println("✅ [SYNC INVOICE] Đã CẬP NHẬT Bill ID=" + pendingInv.getInvoiceId() + " với số tiền: " + totalFee);
        } else {
            // NẾU CHƯA CÓ BILL NỢ: Tạo mới 1 bill cho tất cả các lớp đã chọn
            Invoice newInv = new Invoice();
            newInv.setStudent(currentStudent);
            newInv.setTotalAmount(totalFee);
            newInv.setIssueDate(LocalDate.now());
            newInv.setStatus("Chờ thanh toán");
            newInv.setNote("Học phí tổng hợp đợt đăng ký mới.");
            invoiceService.save(newInv);
            System.out.println("✨ [SYNC INVOICE] Đã TẠO MỚI Bill với số tiền: " + totalFee);
        }
    }

    public void loadData() {
        tableModel.setRowCount(0);
        if (currentStudent == null) return;

        List<Enrollment> myEnrollments = enrollmentService.findAll().stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getStudentId().equals(currentStudent.getStudentId()))
                .toList();

        int[] stt = {1};
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        myEnrollments.forEach(e -> {
            Class c = e.getClazz();
            tableModel.addRow(new Object[]{
                e.getEnrollmentId(), stt[0]++, "CLS" + c.getClassId(), c.getClassName(), c.getCourse().getCourseName(),
                String.format("%,.0f", c.getCourse().getFee()),
                c.getStartDate() != null ? c.getStartDate().format(fmt) : "",
                e.getStatus()
            });
        });
        SwingUtilities.invokeLater(() -> UI.autoResizeColumns(table));
    }

    private void showRegisterDialog() {
        // Kiểm tra trạng thái tài khoản
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

        // Nhóm các lớp theo khóa học
        java.util.Map<Course, List<Class>> courseMap = availableClasses.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Class::getCourse,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        List<Course> courseList = new java.util.ArrayList<>(courseMap.keySet());

        // ── Dialog ────────────────────────────────────────────────────────────
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Đăng ký Khóa học", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(new Color(37, 99, 235));
        headerBar.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JLabel lblTitle = new JLabel("Chọn Khóa học  →  Chọn Lớp học");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel(courseList.size() + " khóa học  ·  " + availableClasses.size() + " lớp trống");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(191, 219, 254));
        headerBar.add(lblTitle, BorderLayout.WEST);
        headerBar.add(lblSub, BorderLayout.EAST);
        dialog.add(headerBar, BorderLayout.NORTH);

        // ── LEFT: Bảng khóa học ───────────────────────────────────────────────
        String[] courseCols = {"_id", "Tên Khóa học", "Cấp độ", "Học phí (VND)", "Số lớp trống"};
        DefaultTableModel courseModel = new DefaultTableModel(courseCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        courseList.forEach(c -> courseModel.addRow(new Object[]{
                c.getCourseId(),
                c.getCourseName(),
                c.getLevel() != null ? c.getLevel() : "—",
                String.format("%,.0f", c.getFee()),
                courseMap.get(c).size() + " lớp"
        }));
        JTable courseTbl = new JTable(courseModel);
        courseTbl.setRowHeight(42);
        courseTbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        courseTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTbl.setSelectionBackground(new Color(219, 234, 254));
        courseTbl.setSelectionForeground(new Color(15, 23, 42));
        courseTbl.setGridColor(new Color(226, 232, 240));
        courseTbl.setShowVerticalLines(false);
        courseTbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        courseTbl.getColumnModel().getColumn(0).setMinWidth(0);
        courseTbl.getColumnModel().getColumn(0).setMaxWidth(0);
        styleTblHeader(courseTbl);

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setBackground(new Color(241, 245, 249));
        leftHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JLabel lblLeft = new JLabel("📚  Khóa học");
        lblLeft.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLeft.setForeground(new Color(30, 41, 59));
        leftHeader.add(lblLeft);

        JScrollPane courseScroll = new JScrollPane(courseTbl);
        courseScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240)));
        courseScroll.getViewport().setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        leftPanel.add(courseScroll, BorderLayout.CENTER);

        // ── RIGHT: Bảng lớp học (hiện sau khi chọn khóa) ─────────────────────
        String[] classCols = {"_id", "Tên Lớp", "Ngày khai giảng", "Ngày kết thúc", "Phòng", "Sĩ số"};
        DefaultTableModel classModel = new DefaultTableModel(classCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable classTbl = new JTable(classModel);
        classTbl.setRowHeight(42);
        classTbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        classTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classTbl.setSelectionBackground(new Color(209, 250, 229));
        classTbl.setSelectionForeground(new Color(15, 23, 42));
        classTbl.setGridColor(new Color(226, 232, 240));
        classTbl.setShowVerticalLines(false);
        classTbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        classTbl.getColumnModel().getColumn(0).setMinWidth(0);
        classTbl.getColumnModel().getColumn(0).setMaxWidth(0);
        styleTblHeader(classTbl);

        // Placeholder khi chưa chọn khóa
        JPanel rightCards = new JPanel(new CardLayout());
        JPanel placeholder = new JPanel(new GridBagLayout());
        placeholder.setBackground(new Color(248, 250, 252));
        JLabel lblPh = new JLabel("← Chọn một khóa học bên trái để xem danh sách lớp");
        lblPh.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblPh.setForeground(new Color(100, 116, 139));
        placeholder.add(lblPh);
        JScrollPane classScroll = new JScrollPane(classTbl);
        classScroll.setBorder(BorderFactory.createEmptyBorder());
        classScroll.getViewport().setBackground(Color.WHITE);
        rightCards.add(placeholder, "placeholder");
        rightCards.add(classScroll, "table");
        ((CardLayout) rightCards.getLayout()).show(rightCards, "placeholder");

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.setBackground(new Color(241, 245, 249));
        rightHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JLabel lblRight = new JLabel("🏫  Lớp học");
        lblRight.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRight.setForeground(new Color(30, 41, 59));
        rightHeader.add(lblRight);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        rightPanel.add(rightCards, BorderLayout.CENTER);

        // ── Split pane ────────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(380);
        split.setDividerSize(4);
        split.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(split, BorderLayout.CENTER);

        // Tham chiếu lớp hiện tại để nút Đăng ký truy cập
        @SuppressWarnings("unchecked")
        final List<Class>[] currentClasses = new List[]{java.util.Collections.emptyList()};

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Click khóa học → cập nhật bảng lớp bên phải
        courseTbl.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int sel = courseTbl.getSelectedRow();
            if (sel < 0) return;
            Course selectedCourse = courseList.get(sel);
            List<Class> classes = courseMap.get(selectedCourse);
            currentClasses[0] = classes;
            classModel.setRowCount(0);
            classes.forEach(c -> {
                long cur = enrollCounts.getOrDefault(c.getClassId(), 0L);
                String slot = (c.getMaxStudent() != null && c.getMaxStudent() > 0)
                        ? cur + "/" + c.getMaxStudent() : cur + "/∞";
                classModel.addRow(new Object[]{
                        c.getClassId(),
                        c.getClassName(),
                        c.getStartDate() != null ? c.getStartDate().format(fmt) : "—",
                        c.getEndDate()   != null ? c.getEndDate().format(fmt)   : "—",
                        c.getRoom() != null ? c.getRoom().getRoomName() : "—",
                        slot
                });
            });
            ((CardLayout) rightCards.getLayout()).show(rightCards, "table");
            SwingUtilities.invokeLater(() -> UI.autoResizeColumns(classTbl, 80, 260));
            // Cập nhật header bên phải
            lblRight.setText("🏫  Lớp học  —  " + selectedCourse.getCourseName());
        });

        // Double-click lớp → đăng ký ngay
        classTbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && classTbl.getSelectedRow() >= 0) {
                    int row = classTbl.getSelectedRow();
                    dialog.dispose();
                    doEnroll(currentClasses[0].get(row));
                }
            }
        });

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        hintBar.setBackground(new Color(241, 245, 249));
        JLabel lblHint = new JLabel("💡  Nhấp đúp vào lớp học để đăng ký ngay, hoặc chọn rồi bấm Đăng ký");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(new Color(100, 116, 139));
        hintBar.add(lblHint);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setBackground(Color.WHITE);

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dialog.dispose());

        JButton btnOk = new JButton("  Đăng ký  ");
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setBackground(new Color(37, 99, 235));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFocusPainted(false);
        btnOk.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnOk.addActionListener(e -> {
            int row = classTbl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng chọn một khóa học rồi chọn một lớp học!");
                return;
            }
            dialog.dispose();
            doEnroll(currentClasses[0].get(row));
        });
        btnBar.add(btnClose);
        btnBar.add(btnOk);

        JPanel footerWrap = new JPanel(new BorderLayout());
        footerWrap.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        footerWrap.add(hintBar, BorderLayout.WEST);
        footerWrap.add(btnBar,  BorderLayout.EAST);
        dialog.add(footerWrap, BorderLayout.SOUTH);

        dialog.setSize(1040, 560);
        dialog.setMinimumSize(new Dimension(820, 450));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void styleTblHeader(JTable tbl) {
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tbl.getTableHeader().setBackground(new Color(248, 250, 252));
        tbl.getTableHeader().setForeground(new Color(71, 85, 105));
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tbl.getTableHeader().setReorderingAllowed(false);
    }

    /** Thực hiện ghi danh sau khi user xác nhận. */
    private void doEnroll(Class selected) {
        // Final full-class guard (race condition protection)
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

        syncInvoiceWithEnrollments(); // TỰ ĐỘNG CẬP NHẬT BILL
        if (onDataChanged != null) onDataChanged.run(); else loadData();
        JOptionPane.showMessageDialog(this,
                "<html>✅  Đăng ký thành công!<br>Lớp: <b>" + selected.getClassName()
                + "</b><br>Học phí đã được cập nhật vào tab Thanh toán.</html>",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleCancelEnrollment() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn l\u1edbp mu\u1ed1n h\u1ee7y!");
            return;
        }

        Long envId = (Long) tableModel.getValueAt(row, 0);
        Enrollment env = enrollmentService.findById(envId);

        // Nếu đã thanh toán → không được hủy
        if ("\u0110\u00e3 thanh to\u00e1n".equals(env.getStatus())
                || "Ho\u00e0n th\u00e0nh".equals(env.getStatus())) {
            JOptionPane.showMessageDialog(this,
                "L\u1EDBp \u201c" + (env.getClazz() != null ? env.getClazz().getClassName() : "") + "\u201d \u0111\u00E3 thanh to\u00E1n \u2014 kh\u00F4ng th\u1EC3 h\u1EE7y!",
                "Kh\u00F4ng th\u1EC3 h\u1EE7y", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int res = JOptionPane.showConfirmDialog(this,
                "B\u1EA1n c\u00F3 ch\u1EAFc ch\u1EAFn mu\u1ED1n h\u1EE7y \u0111\u0103ng k\u00FD l\u1EDBp n\u00E0y?",
                "X\u00E1c nh\u1EADn h\u1EE7y", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            enrollmentService.delete(envId);
            syncInvoiceWithEnrollments();
            if (onDataChanged != null) onDataChanged.run(); else loadData();
            JOptionPane.showMessageDialog(this, "\u0110\u00E3 h\u1EE7y th\u00E0nh c\u00F4ng!");
        }
    }
}