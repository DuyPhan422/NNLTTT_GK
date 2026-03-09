package com.company.ems.model.enums;

/** Trạng thái lớp học — dùng cho classes.status */
public enum ClassStatus {
    LEN_KE_HOACH("Lên kế hoạch"),
    MO_LOP("Mở lớp"),
    DANG_DIEN_RA("Đang diễn ra"),
    HOAN_THANH("Hoàn thành"),
    HUY_LOP("Hủy lớp");

    private final String value;

    ClassStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static ClassStatus fromValue(String v) {
        if (v == null) return LEN_KE_HOACH;
        for (ClassStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return LEN_KE_HOACH;
    }

    @Override
    public String toString() { return value; }
}

