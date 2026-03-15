package com.company.ems.stream;

import com.company.ems.model.Invoice;
import com.company.ems.model.Student;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Các truy vấn dữ liệu stream dành cho Invoice, tách biệt logic tính toán khỏi UI.
 */
public final class InvoiceStreamQueries {

    private InvoiceStreamQueries() {}

    /**
     * DTO hiển thị danh sách hóa đơn
     */
    public record InvoiceView(
            Long invoiceId,
            String invoiceCode,
            String createdDate,
            String dueDate,
            String totalAmountFormatted,
            String status
    ) {}

    // ======================================================================
    // 1) Tìm hóa đơn theo ID
    // ======================================================================
    public static Optional<Invoice> findById(List<Invoice> invoices, Long id) {
        if (invoices == null || id == null) return Optional.empty();
        return invoices.stream()
                .filter(i -> id.equals(i.getInvoiceId()))
                .findFirst();
    }

    // ======================================================================
    // 3) Gom nhóm hóa đơn theo StudentId
    // ======================================================================
    public static Map<Long, List<Invoice>> groupByStudentId(List<Invoice> invoices) {
        if (invoices == null) return Map.of();
        return invoices.stream()
                .filter(i -> i.getStudent() != null)
                .collect(Collectors.groupingBy(i -> i.getStudent().getStudentId()));
    }

    // ======================================================================
    // 4) Lọc theo trạng thái
    // ======================================================================
    public static List<Invoice> filterByStatus(List<Invoice> invoices, String status) {
        if (invoices == null) return List.of();
        return invoices.stream()
                .filter(i -> status.equals(i.getStatus()))
                .collect(Collectors.toList());
    }

    // ======================================================================
    // 5) Sắp xếp giảm dần theo ngày phát hành
    // ======================================================================
    public static List<Invoice> sortByIssueDateDesc(List<Invoice> invoices) {
        if (invoices == null) return List.of();
        return invoices.stream()
                .sorted(Comparator.comparing(Invoice::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // ======================================================================
    // 6) Tính tổng nợ chờ thanh toán
    // ======================================================================
    public static BigDecimal sumPendingDebt(List<Invoice> invoices, String pendingStatus) {
        if (invoices == null) return BigDecimal.ZERO;
        return invoices.stream()
                .filter(i -> pendingStatus.equals(i.getStatus()))
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ======================================================================
    // 7) Đếm theo trạng thái
    // ======================================================================
    public static long countByStatus(List<Invoice> invoices, String status) {
        if (invoices == null) return 0L;
        return invoices.stream()
                .filter(i -> status.equals(i.getStatus()))
                .count();
    }

    // ======================================================================
    // 8) Tìm hóa đơn đầu tiên theo trạng thái
    // ======================================================================
    public static Optional<Invoice> findFirstByStatus(List<Invoice> invoices, String status) {
        if (invoices == null) return Optional.empty();
        return invoices.stream()
                .filter(i -> status.equals(i.getStatus()))
                .findFirst();
    }
}
