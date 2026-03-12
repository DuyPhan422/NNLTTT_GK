package com.company.ems.ui.common;

import java.awt.Color;
import java.awt.Font;

/**
 * Bảng màu và font chữ dùng chung cho toàn bộ UI.
 *
 * <p>Nguyên tắc sử dụng:
 * <ul>
 *   <li>Tất cả panel/dialog PHẢI dùng các hằng số này — KHÔNG được khai báo Color/Font cục bộ.</li>
 *   <li>Nếu cần thêm màu mới, chỉ thêm vào đây (OCP: mở rộng ở một nơi duy nhất).</li>
 * </ul>
 */
public final class Theme {

    private Theme() {}

    // ══════════════════════════════════════════════════════════════════════
    //  BACKGROUND
    // ══════════════════════════════════════════════════════════════════════

    /** Nền trang chính (xám rất nhạt) */
    public static final Color BG_PAGE    = new Color(248, 250, 252);
    /** Nền card / bảng / dialog */
    public static final Color BG_CARD    = Color.WHITE;
    /** Nền sidebar */
    public static final Color BG_SIDEBAR = Color.WHITE;
    /** Nền header cột lưới (schedule grid) */
    public static final Color BG_HEADER  = new Color(241, 245, 249);

    // ══════════════════════════════════════════════════════════════════════
    //  BORDER
    // ══════════════════════════════════════════════════════════════════════

    public static final Color BORDER     = new Color(226, 232, 240);

    // ══════════════════════════════════════════════════════════════════════
    //  BRAND / INTERACTIVE
    // ══════════════════════════════════════════════════════════════════════

    public static final Color PRIMARY    = new Color(37,  99,  235);
    public static final Color PRIMARY_H  = new Color(29,  78,  216);   // hover
    public static final Color DANGER     = new Color(220, 38,  38);
    public static final Color DANGER_H   = new Color(185, 28,  28);

    // ══════════════════════════════════════════════════════════════════════
    //  SEMANTIC
    // ══════════════════════════════════════════════════════════════════════

    public static final Color GREEN      = new Color(22,  163, 74);
    public static final Color BLUE       = new Color(59,  130, 246);
    public static final Color AMBER      = new Color(217, 119, 6);
    public static final Color RED        = new Color(220, 38,  38);
    public static final Color PURPLE     = new Color(124, 58,  237);

    /** Nền row tổng cộng — vàng nhạt */
    public static final Color AMBER_TINT   = new Color(255, 247, 237);
    /** Viền bảng chờ thanh toán — cam nhạt */
    public static final Color AMBER_BORDER = new Color(254, 215, 170);
    /** Viền bảng lịch sử đã thanh toán — xanh lá nhạt */
    public static final Color GREEN_BORDER = new Color(187, 247, 208);
    /** Màu chú thích phụ trên header dialog (slate-400) */
    public static final Color TEXT_SUB     = new Color(148, 163, 184);

    // ══════════════════════════════════════════════════════════════════════
    //  TEXT
    // ══════════════════════════════════════════════════════════════════════

    public static final Color TEXT_MAIN  = new Color(15,  23,  42);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);

    // ══════════════════════════════════════════════════════════════════════
    //  LIST / SIDEBAR ITEMS
    // ══════════════════════════════════════════════════════════════════════

    public static final Color ITEM_HOVER  = new Color(239, 246, 255);
    public static final Color ITEM_ACTIVE = new Color(219, 234, 254);

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE ROWS
    // ══════════════════════════════════════════════════════════════════════

    public static final Color ROW_EVEN   = Color.WHITE;
    public static final Color ROW_ODD    = new Color(248, 250, 252);
    public static final Color ROW_SELECT = new Color(219, 234, 254);

    // ══════════════════════════════════════════════════════════════════════
    //  SCHEDULE GRID — "today" highlight
    // ══════════════════════════════════════════════════════════════════════

    public static final Color TODAY_BG     = new Color(239, 246, 255);
    public static final Color TODAY_BORDER = new Color(147, 197, 253);

    // ══════════════════════════════════════════════════════════════════════
    //  SCORE COLUMN TINTS  (ResultTeacherPanel / ResultStudentPanel)
    // ══════════════════════════════════════════════════════════════════════

    public static final Color COL_QT_EVEN  = new Color(255, 251, 235);
    public static final Color COL_QT_ODD   = new Color(254, 243, 199);
    public static final Color COL_CK_EVEN  = new Color(240, 253, 244);
    public static final Color COL_CK_ODD   = new Color(220, 252, 231);
    public static final Color COL_TOT_EVEN = new Color(239, 246, 255);
    public static final Color COL_TOT_ODD  = new Color(219, 234, 254);

    // ══════════════════════════════════════════════════════════════════════
    //  SCHEDULE BLOC COLORS  (class event blocks on weekly grid)
    // ══════════════════════════════════════════════════════════════════════

    public static final Color[] BLOC_BG = {
        new Color(219, 234, 254), new Color(220, 252, 231), new Color(254, 243, 199),
        new Color(252, 231, 243), new Color(237, 233, 254), new Color(255, 237, 213)
    };
    public static final Color[] BLOC_BORDER = {
        new Color(147, 197, 253), new Color(134, 239, 172), new Color(253, 224, 71),
        new Color(249, 168, 212), new Color(196, 181, 253), new Color(253, 186, 116)
    };

    // ══════════════════════════════════════════════════════════════════════
    //  FONTS
    // ══════════════════════════════════════════════════════════════════════

    public static final Font FONT_PLAIN      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD       = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_HEADER     = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD,  26);
    public static final Font FONT_KPI_LBL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  10);

    // ══════════════════════════════════════════════════════════════════════
    //  GRADE → COLOR mapping  (dùng chung giữa Teacher & Student panels)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Trả về màu hiển thị ứng với xếp loại.
     * Tách ra ở đây để tránh switch lặp lại ở nhiều Panel.
     */
    public static Color gradeColor(String grade) {
        if (grade == null || grade.isBlank() || grade.equals("—")) return TEXT_MUTED;
        return switch (grade) {
            case "A+", "A", "A-" -> GREEN;
            case "B+", "B", "B-" -> BLUE;
            case "C"              -> AMBER;
            case "D", "F"         -> RED;
            default               -> TEXT_MUTED;
        };
    }

    /**
     * Trả về màu badge trạng thái lớp học.
     */
    public static Color classStatusColor(String status) {
        if (status == null) return TEXT_MUTED;
        return switch (status) {
            case "Open", "Ongoing", "Đang diễn ra", "Mở lớp" -> GREEN;
            case "Planned", "Lên kế hoạch"                    -> BLUE;
            case "Cancelled", "Hủy lớp"                       -> RED;
            default                                            -> TEXT_MUTED;
        };
    }
}

