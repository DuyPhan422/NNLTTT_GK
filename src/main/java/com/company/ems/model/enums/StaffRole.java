package com.company.ems.model.enums;

/** Vai trò nhân viên — dùng cho staffs.role */
public enum StaffRole {
    QUAN_TRI("Quản trị"),
    TU_VAN("Tư vấn"),
    KE_TOAN("Kế toán"),
    QUAN_LY("Quản lý"),
    KHAC("Khác");

    private final String value;

    StaffRole(String value) { this.value = value; }

    public String getValue() { return value; }

    public static StaffRole fromValue(String v) {
        if (v == null) return KHAC;
        for (StaffRole r : values()) {
            if (r.value.equals(v)) return r;
        }
        return KHAC;
    }

    @Override
    public String toString() { return value; }
}

