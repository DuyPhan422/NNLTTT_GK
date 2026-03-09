package com.company.ems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "staffs")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    // "Quản trị" | "Tư vấn" | "Kế toán" | "Quản lý" | "Khác"   →  StaffRole enum
    @Column(name = "role", nullable = false, length = 30)
    private String role = com.company.ems.model.enums.StaffRole.KHAC.getValue();

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    // "Hoạt động" | "Không hoạt động"   →  ActiveStatus enum
    @Column(name = "status", nullable = false, length = 20)
    private String status = com.company.ems.model.enums.ActiveStatus.HOAT_DONG.getValue();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "staff", fetch = FetchType.LAZY)
    private UserAccount userAccount;

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
        if (!(o instanceof Staff other)) return false;
        return staffId != null && staffId.equals(other.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(staffId);
    }

    @Override
    public String toString() {
        return fullName != null ? fullName : ("Staff#" + staffId);
    }
}
