package com.company.ems.model.enums;

/** Trạng thái điểm danh — dùng cho attendances.status */
public enum AttendanceStatus {
    CO_MAT("Có mặt"),
    VANG("Vắng"),
    DI_TRE("Đi trễ");

    private final String value;

    AttendanceStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    /** Lấy enum từ chuỗi DB — trả về CO_MAT nếu không khớp */
    public static AttendanceStatus fromValue(String v) {
        if (v == null) return CO_MAT;
        for (AttendanceStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return CO_MAT;
    }

    @Override
    public String toString() { return value; }
}

