package com.company.ems.model.enums;

/** Kết quả ghi danh — dùng cho enrollments.result */
public enum EnrollmentResult {
    DAT("Đạt"),
    KHONG_DAT("Không đạt"),
    CHUA_CO("Chưa có");

    private final String value;

    EnrollmentResult(String value) { this.value = value; }

    public String getValue() { return value; }

    public static EnrollmentResult fromValue(String v) {
        if (v == null) return CHUA_CO;
        for (EnrollmentResult r : values()) {
            if (r.value.equals(v)) return r;
        }
        return CHUA_CO;
    }

    @Override
    public String toString() { return value; }
}

