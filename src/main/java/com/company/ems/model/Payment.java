package com.company.ems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Dùng LocalDateTime vì DB dùng DATETIME
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    // "Tiền mặt" | "Chuyển khoản" | "Momo" | "ZaloPay" | "Thẻ ngân hàng" | "Khác"   →  PaymentMethod enum
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = com.company.ems.model.enums.PaymentMethod.TIEN_MAT.getValue();

    // "Chờ xử lý" | "Hoàn thành" | "Thất bại" | "Đã hoàn tiền"   →  PaymentStatus enum
    @Column(name = "status", nullable = false, length = 20)
    private String status = com.company.ems.model.enums.PaymentStatus.HOAN_THANH.getValue();

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentDate == null) paymentDate = LocalDateTime.now();
    }
}
