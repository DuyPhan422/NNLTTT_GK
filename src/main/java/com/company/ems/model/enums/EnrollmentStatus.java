package com.company.ems.model.enums;

/** Trạng thái ghi danh — dùng cho enrollments.status */
public enum EnrollmentStatus {
    DA_DANG_KY("Đã đăng ký"),
    DA_HUY("Đã hủy"),
    DA_THANH_TOAN("Đã thanh toán");

    private final String value;

    EnrollmentStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static EnrollmentStatus fromValue(String v) {
        if (v == null) return DA_DANG_KY;
        for (EnrollmentStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return DA_DANG_KY;
    }

    @Override
    public String toString() { return value; }
}

