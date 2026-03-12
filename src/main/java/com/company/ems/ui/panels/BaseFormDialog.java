package com.company.ems.ui.panels;

import com.company.ems.ui.common.ComponentFactory;
import com.company.ems.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Lớp cha trừu tượng cho tất cả Form Dialog trong hệ thống.
 *
 * <p>Nguyên tắc:
 * <ul>
 *   <li>Đóng gói toàn bộ boilerplate: modal, layout, nút Lưu/Hủy, lblError, isSaved().</li>
 *   <li>Lớp con chỉ cần implement: buildForm(), validateForm(), commitToEntity(), getEntity().</li>
 *   <li>Tuân thủ Template Method Pattern — flow cố định, logic linh hoạt.</li>
 * </ul>
 *
 * @param <T> Loại entity (Student, Teacher, Course, v.v.)
 */
public abstract class BaseFormDialog<T> extends JDialog {

    /** Label hiển thị lỗi validate — luôn hiển thị (setText " " khi không có lỗi) */
    protected final JLabel lblError;

    /** Flag đánh dấu người dùng đã lưu thành công */
    protected boolean saved = false;

    /**
     * Constructor khởi tạo dialog.
     *
     * @param owner Frame cha
     * @param title Tiêu đề dialog
     */
    protected BaseFormDialog(Frame owner, String title) {
        super(owner, title, true);
        lblError = new JLabel(" ");
        lblError.setFont(Theme.FONT_SMALL);
        lblError.setForeground(Theme.DANGER);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ABSTRACT METHODS — Lớp con PHẢI implement
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Xây dựng panel chứa các field nhập liệu.
     *
     * @return JPanel chứa form
     */
    protected abstract JPanel buildForm();

    /**
     * Kiểm tra tính hợp lệ của dữ liệu nhập vào.
     *
     * @return true nếu hợp lệ, false nếu không (phải gọi setError() trước khi return false)
     */
    protected abstract boolean validateForm();

    /**
     * Gán giá trị từ các field vào entity object.
     */
    protected abstract void commitToEntity();

    /**
     * Trả về entity đã được điền dữ liệu.
     *
     * @return entity object
     */
    public abstract T getEntity();

    // ══════════════════════════════════════════════════════════════════════
    //  FINAL METHODS — Lớp con KHÔNG được override
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Khởi tạo giao diện dialog với layout chuẩn.
     *
     * @param minWidth Độ rộng tối thiểu của dialog
     */
    protected final void initUI(int minWidth) {
        // Content panel chính
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Theme.BG_CARD);
        contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // CENTER: form do lớp con cung cấp
        contentPanel.add(buildForm(), BorderLayout.CENTER);

        // SOUTH: panel chứa error label + nút Lưu/Hủy
        JPanel southPanel = new JPanel(new BorderLayout(0, 12));
        southPanel.setBackground(Theme.BG_CARD);

        // Error label
        southPanel.add(lblError, BorderLayout.NORTH);

        // Panel nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(Theme.BG_CARD);

        JButton btnCancel = ComponentFactory.secondaryButton("Hủy");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = ComponentFactory.primaryButton("Lưu");
        btnSave.addActionListener(e -> doSave());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.add(southPanel, BorderLayout.SOUTH);

        // Thiết lập dialog
        setContentPane(contentPanel);
        pack();
        setMinimumSize(new Dimension(minWidth, getHeight()));
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    /**
     * Xử lý khi người dùng bấm nút Lưu.
     */
    protected final void doSave() {
        lblError.setText(" ");
        if (!validateForm()) {
            return;
        }
        commitToEntity();
        saved = true;
        dispose();
    }

    /**
     * Kiểm tra xem người dùng đã lưu thành công hay chưa.
     *
     * @return true nếu đã lưu, false nếu hủy
     */
    public boolean isSaved() {
        return saved;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPER METHODS — Lớp con có thể sử dụng
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị thông báo lỗi validate.
     *
     * @param msg Nội dung lỗi
     */
    protected void setError(String msg) {
        lblError.setText(msg);
    }

    /**
     * Xóa thông báo lỗi (đặt về khoảng trắng).
     */
    protected void clearError() {
        lblError.setText(" ");
    }
}

