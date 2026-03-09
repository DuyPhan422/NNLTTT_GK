package com.company.ems.model.enums;

/** Đơn vị thời lượng khóa học — dùng cho courses.duration_unit */
public enum DurationUnit {
    GIO("Giờ"),
    TUAN("Tuần"),
    THANG("Tháng");

    private final String value;

    DurationUnit(String value) { this.value = value; }

    public String getValue() { return value; }

    public static DurationUnit fromValue(String v) {
        if (v == null) return TUAN;
        for (DurationUnit u : values()) {
            if (u.value.equals(v)) return u;
        }
        return TUAN;
    }

    @Override
    public String toString() { return value; }
}

