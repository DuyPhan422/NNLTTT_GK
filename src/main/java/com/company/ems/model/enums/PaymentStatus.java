package com.company.ems.model.enums;

/** Trạng thái thanh toán — dùng cho payments.status */
public enum PaymentStatus {
    CHO_XU_LY("Chờ xử lý"),
    HOAN_THANH("Hoàn thành"),
    THAT_BAI("Thất bại"),
    DA_HOAN_TIEN("Đã hoàn tiền");

    private final String value;

    PaymentStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static PaymentStatus fromValue(String v) {
        if (v == null) return CHO_XU_LY;
        for (PaymentStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return CHO_XU_LY;
    }

    @Override
    public String toString() { return value; }
}

