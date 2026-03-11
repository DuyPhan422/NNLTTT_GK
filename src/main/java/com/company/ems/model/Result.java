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
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Class clazz;

    /** Điểm quá trình 1 (25%) */
    @Column(name = "score1", precision = 5, scale = 2)
    private BigDecimal score1;

    /** Điểm quá trình 2 (25%) */
    @Column(name = "score2", precision = 5, scale = 2)
    private BigDecimal score2;

    /** Điểm cuối kỳ (50%) */
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    /** Điểm tổng tính tự động = 0.25*score1 + 0.25*score2 + 0.50*finalScore */
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "grade", length = 10)
    private String grade;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

