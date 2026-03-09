package com.company.ems.model.enums;

/** Vai trò tài khoản — dùng cho user_accounts.role */
public enum UserRole {
    QUAN_TRI("Quản trị"),
    GIAO_VIEN("Giáo viên"),
    HOC_VIEN("Học viên"),
    NHAN_VIEN("Nhân viên");

    private final String value;

    UserRole(String value) { this.value = value; }

    public String getValue() { return value; }

    public static UserRole fromValue(String v) {
        if (v == null) return HOC_VIEN;
        for (UserRole r : values()) {
            if (r.value.equals(v)) return r;
        }
        return HOC_VIEN;
    }

    @Override
    public String toString() { return value; }
}

