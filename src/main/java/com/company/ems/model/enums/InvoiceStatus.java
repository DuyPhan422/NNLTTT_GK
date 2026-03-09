package com.company.ems.model.enums;

/** Trạng thái hóa đơn — dùng cho invoices.status */
public enum InvoiceStatus {
    BAN_NHAP("Bản nháp"),
    CHO_THANH_TOAN("Chờ thanh toán"),
    DA_THANH_TOAN("Đã thanh toán"),
    DA_HUY("Đã hủy");

    private final String value;

    InvoiceStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static InvoiceStatus fromValue(String v) {
        if (v == null) return CHO_THANH_TOAN;
        for (InvoiceStatus s : values()) {
            if (s.value.equals(v)) return s;
        }
        return CHO_THANH_TOAN;
    }

    @Override
    public String toString() { return value; }
}

