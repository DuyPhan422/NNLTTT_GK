package com.company.ems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // "Cơ bản" | "Trung cấp" | "Nâng cao"   →  CourseLevel enum
    @Column(name = "level", length = 30)
    private String level;

    @Column(name = "duration")
    private Integer duration;

    // "Giờ" | "Tuần" | "Tháng"   →  DurationUnit enum
    @Column(name = "duration_unit", length = 10)
    private String durationUnit = com.company.ems.model.enums.DurationUnit.TUAN.getValue();

    @Column(name = "fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    // "Hoạt động" | "Không hoạt động"   →  ActiveStatus enum
    @Column(name = "status", nullable = false, length = 20)
    private String status = com.company.ems.model.enums.ActiveStatus.HOAT_DONG.getValue();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Class> classes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course other)) return false;
        return courseId != null && courseId.equals(other.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(courseId);
    }

    @Override
    public String toString() {
        return courseName != null ? courseName : ("Course#" + courseId);
    }
}
