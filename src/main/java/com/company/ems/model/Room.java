package com.company.ems.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_name", nullable = false, length = 100, unique = true)
    private String roomName;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "location", length = 150)
    private String location;

    // "Hoạt động" | "Không hoạt động"   →  ActiveStatus enum
    @Column(name = "status", nullable = false, length = 20)
    private String status = com.company.ems.model.enums.ActiveStatus.HOAT_DONG.getValue();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Class> classes;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Schedule> schedules;

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
        if (!(o instanceof Room other)) return false;
        return roomId != null && roomId.equals(other.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roomId);
    }

    @Override
    public String toString() {
        return roomName != null ? roomName : ("Room#" + roomId);
    }
}
