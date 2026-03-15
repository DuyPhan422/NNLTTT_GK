package com.company.ems.stream;

import com.company.ems.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Các truy vấn dữ liệu stream dành cho Payment, tách biệt logic tính toán khỏi UI.
 *
 * Dùng trong dự án:
 *     List<Payment> payments = paymentService.findAll();
 *     BigDecimal total = PaymentStreamQueries.totalRevenuePaid(payments);
 */
public final class PaymentStreamQueries {

    private PaymentStreamQueries() {}

    // ── Record projection ────────────────────────────────────────────────────

    /** Dữ liệu KPI doanh thu tổng hợp. */
    public record RevenueKpi(BigDecimal totalRevenue, long txCount) {}

    // ── 1. Kiểm tra trạng thái thanh toán ───────────────────────────────────

    /**
     * Chấp nhận cả trạng thái cũ lẫn mới: "Đã thanh toán", "Hoàn thành", "Completed".
     */
    public static boolean isPaid(String status) {
        return "Đã thanh toán".equals(status)
                || "Hoàn thành".equals(status)
                || "Completed".equals(status);
    }

    // ── 2. Tính tổng doanh thu đã thu ────────────────────────────────────────

    /**
     * Tổng số tiền từ các payment đã hoàn thành.
     */
    public static BigDecimal totalRevenuePaid(List<Payment> payments) {
        return payments.stream()
                .filter(p -> isPaid(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── 3. Đếm số giao dịch đã hoàn thành ───────────────────────────────────

    /**
     * Số giao dịch thành công.
     */
    public static long countPaidTransactions(List<Payment> payments) {
        return payments.stream()
                .filter(p -> isPaid(p.getStatus()))
                .count();
    }

    // ── 4. KPI doanh thu tổng hợp ────────────────────────────────────────────

    /**
     * Tổng hợp KPI doanh thu: tổng tiền và số giao dịch thành công.
     */
    public static RevenueKpi buildRevenueKpi(List<Payment> payments) {
        BigDecimal total = totalRevenuePaid(payments);
        long txCount = countPaidTransactions(payments);
        return new RevenueKpi(total, txCount);
    }

    // ── 5. Doanh thu theo tháng (6 tháng gần nhất) ───────────────────────────

    /**
     * Trả về map doanh thu theo tháng dạng "yyyy/MM" → tổng tiền,
     * chỉ tính những payment đã thanh toán, trong khoảng 6 tháng gần nhất.
     */
    public static LinkedHashMap<String, BigDecimal> revenueByMonth(List<Payment> payments, int monthCount) {
        LocalDate now = LocalDate.now();
        LinkedHashMap<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (int i = monthCount - 1; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            monthly.put(m.getYear() + "/" + String.format("%02d", m.getMonthValue()), BigDecimal.ZERO);
        }
        payments.stream()
                .filter(p -> isPaid(p.getStatus()) && p.getPaymentDate() != null)
                .forEach(p -> {
                    LocalDate d = p.getPaymentDate().toLocalDate();
                    String key = d.getYear() + "/" + String.format("%02d", d.getMonthValue());
                    monthly.merge(key, p.getAmount(), BigDecimal::add);
                });
        return monthly;
    }

    // ── 6. Tổng lượng từ danh sách BigDecimal ────────────────────────────────

    /**
     * Tổng cộng một danh sách BigDecimal.
     */
    public static BigDecimal sumAmounts(List<BigDecimal> list) {
        return list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
