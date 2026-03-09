package com.company.ems.model.enums;

/** Trạng thái chung Active/Inactive — dùng cho students, teachers, courses, rooms, staffs, branches */
public enum ActiveStatus {
    HOAT_DONG("Hoạt động"),
    KHONG_HOAT_DONG("Không hoạt động");

    private final String value;

    ActiveStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static ActiveStatus fromValue(String v) {
        if (v == null) return HOAT_DONG;
        for (ActiveStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return HOAT_DONG;
    }

    @Override
    public String toString() { return value; }
}

