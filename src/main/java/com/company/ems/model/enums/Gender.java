package com.company.ems.model.enums;

/** Giới tính — dùng cho students.gender */
public enum Gender {
    NAM("Nam"),
    NU("Nữ"),
    KHAC("Khác");

    private final String value;

    Gender(String value) { this.value = value; }

    public String getValue() { return value; }

    public static Gender fromValue(String v) {
        if (v == null) return KHAC;
        for (Gender g : values()) {
            if (g.value.equals(v)) return g;
        }
        return KHAC;
    }

    @Override
    public String toString() { return value; }
}

