package com.company.ems.model.enums;

/** Phương thức thanh toán — dùng cho payments.payment_method */
public enum PaymentMethod {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản"),
    MOMO("Momo"),
    ZALO_PAY("ZaloPay"),
    THE_NGAN_HANG("Thẻ ngân hàng"),
    KHAC("Khác");

    private final String value;

    PaymentMethod(String value) { this.value = value; }

    public String getValue() { return value; }

    public static PaymentMethod fromValue(String v) {
        if (v == null) return TIEN_MAT;
        for (PaymentMethod m : values()) {
            if (m.value.equals(v)) return m;
        }
        return TIEN_MAT;
    }

    @Override
    public String toString() { return value; }
}

