package com.company.ems.model.enums;

/** Cấp độ khóa học — dùng cho courses.level */
public enum CourseLevel {
    CO_BAN("Cơ bản"),
    TRUNG_CAP("Trung cấp"),
    NANG_CAO("Nâng cao");

    private final String value;

    CourseLevel(String value) { this.value = value; }

    public String getValue() { return value; }

    public static CourseLevel fromValue(String v) {
        if (v == null) return CO_BAN;
        for (CourseLevel l : values()) {
            if (l.value.equals(v)) return l;
        }
        return CO_BAN;
    }

    @Override
    public String toString() { return value; }
}

